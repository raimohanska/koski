package fi.oph.koski.opiskeluoikeus

import fi.oph.koski.config.KoskiApplication
import fi.oph.koski.db.{HenkilöRow, OpiskeluoikeusRow}
import fi.oph.koski.henkilo.HenkilöRepository
import fi.oph.koski.history.OpiskeluoikeusHistoryRepository
import fi.oph.koski.http._
import fi.oph.koski.json.JsonDiff.jsonDiff
import fi.oph.koski.json.JsonSerializer.serialize
import fi.oph.koski.koskiuser.{AccessType, KoskiSession, RequiresAuthentication}
import fi.oph.koski.log.Logging
import fi.oph.koski.schema.annotation.SensitiveData
import fi.oph.koski.schema.{Henkilö, Opiskeluoikeus}
import fi.oph.koski.servlet.{ApiServlet, NoCache, ObservableSupport}
import fi.oph.koski.validation.KoskiValidator
import org.json4s._
import org.scalatra._
import rx.lang.scala.Observable

class OpiskeluoikeusValidationServlet(implicit val application: KoskiApplication) extends ApiServlet with RequiresAuthentication with Logging with NoCache with ObservableSupport with ContentEncodingSupport {
  get("/") {
    val errorsOnly = params.get("errorsOnly").map(_.toBoolean).getOrElse(false)
    val validateHistory = params.get("history").map(_.toBoolean).getOrElse(false)
    val validateHenkilö = params.get("henkilö").map(_.toBoolean).getOrElse(false)
    val extractOnly = params.get("extractOnly").map(_.toBoolean).getOrElse(false)

    val context = ValidateContext(application.validator, application.historyRepository, application.henkilöRepository)
    def validate(row: OpiskeluoikeusRow): ValidationResult = {
      var result = if (extractOnly) {
        context.extractOpiskeluoikeus(row)
      } else {
        context.validateOpiskeluoikeus(row)
      }
      if (validateHistory) result = result + context.validateHistory(row)
      if (validateHenkilö) result = result + context.validateHenkilö(row)
      result
    }

    OpiskeluoikeusQueryFilter.parse(params.filterKeys(!List("errorsOnly", "history", "henkilö", "extractOnly").contains(_)).toList)(application.koodistoViitePalvelu, application.organisaatioRepository, koskiSession) match {
      case Right(filters) =>
        val rows: Observable[(OpiskeluoikeusRow, HenkilöRow, Option[HenkilöRow])] = application.opiskeluoikeusQueryRepository.opiskeluoikeusQuery(filters, None, None)(koskiSession)
        streamResponse[ValidationResult](rows.map(_._1).map(validate).filter(result => !(errorsOnly && result.isOk)))

      case Left(status) =>
        haltWithStatus(status)
    }
  }

  get("/:oid") {
    val context = ValidateContext(application.validator, application.historyRepository, application.henkilöRepository)
    renderEither(application.opiskeluoikeusRepository.findByOid(getStringParam("oid"))(koskiSession).map(context.validateAll))
  }
}

/**
  *  Operating context for data validation. Operates outside the lecixal scope of OpiskeluoikeusServlet to ensure that none of the
  *  Scalatra threadlocals are used. This must be done because in batch mode, we are running in several threads.
  */
case class ValidateContext(validator: KoskiValidator, historyRepository: OpiskeluoikeusHistoryRepository, henkilöRepository: HenkilöRepository)(implicit user: KoskiSession) {
  def validateHistory(row: OpiskeluoikeusRow): ValidationResult = {
    try {
      val opiskeluoikeus = row.toOpiskeluoikeus
      (historyRepository.findVersion(row.oid, row.versionumero)(user) match {
        case Right(latestVersion) =>
          HttpStatus.validate(latestVersion == opiskeluoikeus) {
            KoskiErrorCategory.internalError(JsonErrorMessage(HistoryInconsistency(row + " versiohistoria epäkonsistentti", jsonDiff(serialize(row), serialize(latestVersion)))))
          }
        case Left(error) => error
      }) match {
        case HttpStatus.ok => ValidationResult(row.oppijaOid, row.oid, Nil)
        case status: HttpStatus => ValidationResult(row.oppijaOid, row.oid, status.errors)
      }
    } catch {
      case e: MappingException =>
        ValidationResult(row.oppijaOid, row.oid, List(ErrorDetail("deserializationFailed", s"Opiskeluoikeuden ${row.oid} deserialisointi epäonnistui")))
    }
  }

  def extractOpiskeluoikeus(row: OpiskeluoikeusRow): ValidationResult = {
    renderValidationResult(row, validator.extractOpiskeluoikeus(row.data))
  }

  def validateOpiskeluoikeus(row: OpiskeluoikeusRow): ValidationResult = {
    renderValidationResult(row, validator.extractAndValidateOpiskeluoikeus(row.data)(user, AccessType.read))
  }

  private def renderValidationResult(row: OpiskeluoikeusRow, validationResult: Either[HttpStatus, Opiskeluoikeus]) = {
    validationResult match {
      case Right(oppija) =>
        ValidationResult(row.oppijaOid, row.oid, Nil)
      case Left(status) =>
        ValidationResult(row.oppijaOid, row.oid, status.errors)
    }
  }

  def validateHenkilö(row: OpiskeluoikeusRow): ValidationResult = {
    henkilöRepository.findByOid(row.oppijaOid) match {
      case Some(h) => ValidationResult(row.oppijaOid, row.oid, Nil)
      case None => ValidationResult(row.oppijaOid, row.oid, List(ErrorDetail("oppijaaEiLöydy", s"Oppijaa ${row.oppijaOid} ei löydy henkilöpalvelusta")))
    }
  }

  def validateAll(row: OpiskeluoikeusRow): ValidationResult = {
    validateOpiskeluoikeus(row) + validateHistory(row) + validateHenkilö(row)
  }
}

case class ValidationResult(henkilöOid: Henkilö.Oid, opiskeluoikeusOid: String, errors: List[ErrorDetail]) {
  def isOk = errors.isEmpty
  def +(other: ValidationResult) = ValidationResult(henkilöOid, opiskeluoikeusOid, errors ++ other.errors)
}

case class HistoryInconsistency(message: String, @SensitiveData diff: JValue)
