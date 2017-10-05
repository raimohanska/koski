package fi.oph.koski.perustiedot

import java.time.LocalDate

import fi.oph.koski.db.{HenkilöRow, OpiskeluoikeusRow}
import fi.oph.koski.json.JsonSerializer
import fi.oph.koski.koskiuser.KoskiSession
import fi.oph.koski.json.JsonSerializer.extract
import fi.oph.koski.schema._
import fi.oph.scalaschema.annotation.Description
import org.json4s.{JArray, JValue}

trait WithId {
  def id: Int
}

case class OpiskeluoikeudenPerustiedot(
  id: Int,
  henkilö: NimitiedotJaOid,
  oppilaitos: Oppilaitos,
  sisältyyOpiskeluoikeuteen: Option[SisältäväOpiskeluoikeus],
  @Description("Opiskelijan opiskeluoikeuden alkamisaika joko tutkintotavoitteisessa koulutuksessa tai tutkinnon osa tavoitteisessa koulutuksessa. Muoto YYYY-MM-DD")
  alkamispäivä: Option[LocalDate],
  päättymispäivä: Option[LocalDate],
  tyyppi: Koodistokoodiviite,
  suoritukset: List[SuorituksenPerustiedot],
  @KoodistoUri("virtaopiskeluoikeudentila")
  @KoodistoUri("koskiopiskeluoikeudentila")
  tilat: Option[List[OpiskeluoikeusJaksonPerustiedot]], // Optionality can be removed after re-indexing
  @Description("Luokan tai ryhmän tunniste, esimerkiksi 9C")
  luokka: Option[String]
) extends WithId

case class NimitiedotJaOid(oid: String, etunimet: String, kutsumanimi: String, sukunimi: String)
case class Henkilötiedot(id: Int, henkilö: NimitiedotJaOid) extends WithId

case class OpiskeluoikeusJaksonPerustiedot(
  alku: LocalDate,
  loppu: Option[LocalDate],
  tila: Koodistokoodiviite
)

object OpiskeluoikeudenPerustiedot {
  def makePerustiedot(row: OpiskeluoikeusRow, henkilöRow: HenkilöRow): OpiskeluoikeudenPerustiedot = {
    makePerustiedot(row.id, row.data, row.luokka, henkilöRow.toHenkilötiedot)
  }

  def makePerustiedot(id: Int, oo: Opiskeluoikeus, henkilö: TäydellisetHenkilötiedot): OpiskeluoikeudenPerustiedot = {
    makePerustiedot(id, JsonSerializer.serializeWithUser(KoskiSession.untrustedUser)(oo), oo.luokka.orElse(oo.ryhmä), henkilö)
  }

  def makePerustiedot(id: Int, data: JValue, luokka: Option[String], henkilö: TäydellisetHenkilötiedot): OpiskeluoikeudenPerustiedot = {
    val suoritukset: List[SuorituksenPerustiedot] = (data \ "suoritukset").asInstanceOf[JArray].arr
      .map { suoritus =>
        SuorituksenPerustiedot(
          extract[Koodistokoodiviite](suoritus \ "tyyppi"),
          (KoulutusmoduulinPerustiedot(extract[Koodistokoodiviite](suoritus \ "koulutusmoduuli" \ "tunniste"))), // TODO: voi olla paikallinen koodi
          extract[Option[List[Koodistokoodiviite]]](suoritus \ "osaamisala"),
          extract[Option[List[Koodistokoodiviite]]](suoritus \ "tutkintonimike"),
          extract[OidOrganisaatio](suoritus \ "toimipiste", ignoreExtras = true)
        )
      }
      .filter(_.tyyppi.koodiarvo != "perusopetuksenvuosiluokka")
    OpiskeluoikeudenPerustiedot(
      id,
      toNimitiedotJaOid(henkilö),
      extract[Oppilaitos](data \ "oppilaitos"),
      extract[Option[SisältäväOpiskeluoikeus]](data \ "sisältyyOpiskeluoikeuteen"),
      extract[Option[LocalDate]](data \ "alkamispäivä"),
      extract[Option[LocalDate]](data \ "päättymispäivä"),
      extract[Koodistokoodiviite](data \ "tyyppi"),
      suoritukset,
      Some(fixTilat(extract[List[OpiskeluoikeusJaksonPerustiedot]](data \ "tila" \ "opiskeluoikeusjaksot", ignoreExtras = true))),
      luokka)
  }

  private def fixTilat(tilat: List[OpiskeluoikeusJaksonPerustiedot]) = {
    tilat.zip(tilat.drop(1)).map { case (tila, next) =>
      tila.copy(loppu = Some(next.alku))
    } ++ List(tilat.last)
  }

  def toNimitiedotJaOid(henkilötiedot: TäydellisetHenkilötiedot): NimitiedotJaOid =
    NimitiedotJaOid(henkilötiedot.oid, henkilötiedot.etunimet, henkilötiedot.kutsumanimi, henkilötiedot.sukunimi)
}

case class SuorituksenPerustiedot(
  @Description("Suorituksen tyyppi, jolla erotellaan eri koulutusmuotoihin (perusopetus, lukio, ammatillinen...) ja eri tasoihin (tutkinto, tutkinnon osa, kurssi, oppiaine...) liittyvät suoritukset")
  @KoodistoUri("suorituksentyyppi")
  @Hidden
  tyyppi: Koodistokoodiviite,
  koulutusmoduuli: KoulutusmoduulinPerustiedot,
  @Description("Tieto siitä mihin osaamisalaan/osaamisaloihin oppijan tutkinto liittyy")
  @KoodistoUri("osaamisala")
  @OksaUri(tunnus = "tmpOKSAID299", käsite = "osaamisala")
  osaamisala: Option[List[Koodistokoodiviite]] = None,
  @Description("Tieto siitä mihin tutkintonimikkeeseen oppijan tutkinto liittyy")
  @KoodistoUri("tutkintonimikkeet")
  @OksaUri("tmpOKSAID588", "tutkintonimike")
  tutkintonimike: Option[List[Koodistokoodiviite]] = None,
  toimipiste: OidOrganisaatio
)

case class KoulutusmoduulinPerustiedot(
  tunniste: KoodiViite
)