package fi.oph.koski.api

import fi.oph.koski.henkilo.MockOppijat
import fi.oph.koski.http.KoskiErrorCategory
import fi.oph.koski.koskiuser.{MockUser, MockUsers}
import fi.oph.koski.organisaatio.MockOrganisaatiot
import fi.oph.koski.schema.{AmmatillinenOpiskeluoikeus, OidOrganisaatio, Oppilaitos, SisältäväOpiskeluoikeus}
import org.scalatest.{FreeSpec, Matchers}
import fi.oph.koski.documentation.AmmatillinenExampleData._

class SisältyväOpiskeluoikeusSpec extends FreeSpec with Matchers with OpiskeluoikeusTestMethodsAmmatillinen with LocalJettyHttpSpecification {
  "Sisältyvä opiskeluoikeus" - {
    lazy val fixture = new {
      resetFixtures
      val original: AmmatillinenOpiskeluoikeus = createOpiskeluoikeus(defaultHenkilö, defaultOpiskeluoikeus, user = MockUsers.stadinAmmattiopistoTallentaja)

      val sisältyvä: AmmatillinenOpiskeluoikeus = defaultOpiskeluoikeus.copy(
        oppilaitos = Some(Oppilaitos(MockOrganisaatiot.omnia)),
        sisältyyOpiskeluoikeuteen = Some(SisältäväOpiskeluoikeus(original.id.get, original.oppilaitos.get)),
        suoritukset = List(autoalanPerustutkinnonSuoritus(OidOrganisaatio(MockOrganisaatiot.omnia)))
      )
    }

    "Kun sisältävä opiskeluoikeus löytyy Koskesta" - {
      lazy val sisältyvä = createOpiskeluoikeus(defaultHenkilö, fixture.sisältyvä, user = MockUsers.omniaTallentaja)
      "Lisäys onnistuu" in {
        sisältyvä.id.isDefined should equal(true)
      }

      "Sisältävän opiskeluoikeuden organisaatiolla on katseluoikeudet sisältyvään opiskeluoikeuteen" in {
        val ids = getOpiskeluoikeudet(MockOppijat.eero.oid, MockUsers.stadinAmmattiopistoTallentaja).flatMap(_.id)
        ids should contain(fixture.original.id.get)
        ids should contain(sisältyvä.id.get)
      }

      "Sisältyvän opiskeluoikeuden organisaatiolla ei ole oikeuksia sisältävään opiskeluoikeuteen" in {
        val ids = getOpiskeluoikeudet(MockOppijat.eero.oid, MockUsers.omniaKatselija).flatMap(_.id)
        ids should contain(sisältyvä.id.get)
        ids should not contain(fixture.original.id)
      }

      "Sisältävän opiskeluoikeuden organisaatiolla ei ole kirjoitusoikeuksia sisältyvään opiskeluoikeuteen" in {
        putOpiskeluoikeus(sisältyvä, headers = authHeaders(MockUsers.stadinAmmattiopistoTallentaja) ++ jsonContent) {
          verifyResponseStatus(403)
        }
        putOpiskeluoikeus(sisältyvä, headers = authHeaders(MockUsers.omniaTallentaja) ++ jsonContent) {
          verifyResponseStatus(200)
        }
      }
    }

    "Kun sisältävä opiskeluoikeus ei löydy Koskesta -> HTTP 400" in {
      putOpiskeluoikeus(fixture.sisältyvä.copy( sisältyyOpiskeluoikeuteen = Some(SisältäväOpiskeluoikeus(66666666, fixture.original.oppilaitos.get)))) {
        verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.sisältäväOpiskeluoikeus.eiLöydy("Sisältävää opiskeluoikeutta ei löydy id-arvolla 66666666"))
      }
    }

    "Kun sisältävän opiskeluoikeuden organisaatio ei täsmää -> HTTP 400" in {
      putOpiskeluoikeus(fixture.sisältyvä.copy( sisältyyOpiskeluoikeuteen = Some(SisältäväOpiskeluoikeus(fixture.original.id.get, Oppilaitos(MockOrganisaatiot.omnia))))) {
        verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.sisältäväOpiskeluoikeus.vääräOppilaitos())
      }
     }

    "Kun sisältävän opiskeluoikeuden henkilötieto ei täsmää -> HTTP 400" in {
      putOpiskeluoikeus(fixture.sisältyvä, henkilö = MockOppijat.eerola.vainHenkilötiedot) {
        verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.sisältäväOpiskeluoikeus.henkilöTiedot())
      }
    }
  }
}