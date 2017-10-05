package fi.oph.koski.api

import java.time.LocalDate.{of => date}

import fi.oph.koski.documentation.ExampleData._
import fi.oph.koski.documentation.LukioExampleData.nuortenOpetussuunnitelma
import fi.oph.koski.documentation.YleissivistavakoulutusExampleData.jyväskylänNormaalikoulu
import fi.oph.koski.localization.LocalizedStringImplicits._
import fi.oph.koski.schema._

trait OpiskeluoikeusTestMethodsLukio extends PutOpiskeluoikeusTestMethods[LukionOpiskeluoikeus]{
  def tag = implicitly[reflect.runtime.universe.TypeTag[LukionOpiskeluoikeus]]

  override def defaultOpiskeluoikeus = OpiskeluoikeusTestMethodsLukio.lukionOpiskeluoikeus
}

object OpiskeluoikeusTestMethodsLukio {
  val vahvistus = Some(HenkilövahvistusPaikkakunnalla(päivä = date(2016, 6, 4), jyväskylä, myöntäjäOrganisaatio = jyväskylänNormaalikoulu, myöntäjäHenkilöt = List(Organisaatiohenkilö("Reijo Reksi", "rehtori", jyväskylänNormaalikoulu))))

  val päättötodistusSuoritus = LukionOppimääränSuoritus(
    koulutusmoduuli = LukionOppimäärä(perusteenDiaarinumero = Some("60/011/2015")),
    oppimäärä = nuortenOpetussuunnitelma,
    suorituskieli = suomenKieli,
    toimipiste = jyväskylänNormaalikoulu,
    vahvistus = vahvistus,
    osasuoritukset = None
  )

  def lukionOpiskeluoikeus = LukionOpiskeluoikeus(
    oppilaitos = Some(jyväskylänNormaalikoulu),
    suoritukset = List(päättötodistusSuoritus),
    alkamispäivä = Some(longTimeAgo),
    tila = LukionOpiskeluoikeudenTila(List(LukionOpiskeluoikeusjakso(longTimeAgo, opiskeluoikeusLäsnä)))
  )
}