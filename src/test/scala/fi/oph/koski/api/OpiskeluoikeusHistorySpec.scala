package fi.oph.koski.api

import java.time.LocalDate

import fi.oph.koski.documentation.AmmatillinenOldExamples
import fi.oph.koski.henkilo.MockOppijat
import fi.oph.koski.history.OpiskeluoikeusHistory
import fi.oph.koski.http.KoskiErrorCategory
import fi.oph.koski.json.Json
import fi.oph.koski.koskiuser.MockUsers
import fi.oph.koski.log.AuditLogTester
import fi.oph.koski.schema.{Henkilö, Opiskeluoikeus}
import org.scalatest.FreeSpec

class OpiskeluoikeusHistorySpec extends FreeSpec with LocalJettyHttpSpecification with OpiskeluoikeusTestMethodsAmmatillinen {
  val uusiOpiskeluoikeus = defaultOpiskeluoikeus
  val oppija = MockOppijat.tyhjä

  "Muutoshistoria" - {
    "Luotaessa uusi opiskeluoikeus" - {
      "Luodaan historiarivi" in {
        val opiskeluoikeus = createOpiskeluoikeus(oppija, uusiOpiskeluoikeus, resetFixtures = true)
        verifyHistory(oppija, opiskeluoikeus, List(1))
      }

      "osasuorituksilla" in {
        val opiskeluoikeus = createOpiskeluoikeus(oppija, AmmatillinenOldExamples.full.opiskeluoikeudet(0), resetFixtures = true)
        verifyHistory(oppija, opiskeluoikeus, List(1))
      }
    }
    "Päivitettäessä" - {
      "Luodaan uusi versiorivi" in {
        val opiskeluoikeus = createOpiskeluoikeus(oppija, uusiOpiskeluoikeus, resetFixtures = true)
        val modified: Opiskeluoikeus = createOrUpdate(oppija, opiskeluoikeus.copy(arvioituPäättymispäivä = Some(LocalDate.now)))
        verifyHistory(oppija, modified, List(1, 2))
      }

      "Jos mikään ei ole muuttunut" - {
        "Ei luoda uutta versioriviä" in {
          val opiskeluoikeus = createOpiskeluoikeus(oppija, uusiOpiskeluoikeus, resetFixtures = true)
          val modified: Opiskeluoikeus = createOrUpdate(oppija, opiskeluoikeus)
          verifyHistory(oppija, modified, List(1))
        }
      }

      "Kun syötteessä annetaan versionumero" - {
        "Versionumero sama kuin viimeisin" - {
          "Päivitys hyväksytään" in {
            val opiskeluoikeus = createOpiskeluoikeus(oppija, uusiOpiskeluoikeus, resetFixtures = true)
            val modified: Opiskeluoikeus = createOrUpdate(oppija, opiskeluoikeus.copy(arvioituPäättymispäivä = Some(LocalDate.now), versionumero = Some(1)))
            verifyHistory(oppija, modified, List(1, 2))
          }
        }

        "Versionumero ei täsmää" - {
          "Päivitys hylätään" in {
            val opiskeluoikeus = createOpiskeluoikeus(oppija, uusiOpiskeluoikeus, resetFixtures = true)
            val modified: Opiskeluoikeus = createOrUpdate(oppija, opiskeluoikeus.copy(arvioituPäättymispäivä = Some(LocalDate.now), versionumero = Some(3)), {
              verifyResponseStatus(409, KoskiErrorCategory.conflict.versionumero("Annettu versionumero 3 <> 1"))
            })
            verifyHistory(oppija, modified, List(1))
          }
        }
      }
    }

    "Käyttöoikeudet" - {
      "Kun haetaan historiaa opiskeluoikeudelle, johon käyttäjällä ei oikeuksia" - {
        "Palautetaan 404" in {
          val opiskeluoikeus = createOpiskeluoikeus(oppija, uusiOpiskeluoikeus)
          authGet("api/opiskeluoikeus/historia/" + opiskeluoikeus.oid.get, MockUsers.omniaPalvelukäyttäjä) {
            verifyResponseStatus(404, KoskiErrorCategory.notFound.opiskeluoikeuttaEiLöydyTaiEiOikeuksia())
          }
        }
      }
    }

    "Tuntematon id" - {
      "Palautetaan HTTP 400" in {
        authGet("api/opiskeluoikeus/historia/123456789") {
          verifyResponseStatus(404, KoskiErrorCategory.notFound.opiskeluoikeuttaEiLöydyTaiEiOikeuksia("Opiskeluoikeutta ei löydy annetulla oid:llä tai käyttäjällä ei ole siihen oikeuksia"))
        }
      }
    }

    "Versiohistorian hakeminen" - {
      "Onnistuu ja tuottaa auditlog-merkinnän" in {
        val opiskeluoikeus = createOpiskeluoikeus(oppija, uusiOpiskeluoikeus, resetFixtures = true)
        authGet("api/opiskeluoikeus/historia/" + opiskeluoikeus.oid.get) {
          getHistory(opiskeluoikeus.oid.get)
          AuditLogTester.verifyAuditLogMessage(Map("operaatio" -> "MUUTOSHISTORIA_KATSOMINEN"))
        }
      }
    }

    "Yksittäisen version hakeminen" - {
      "Onnistuu ja tuottaa auditlog-merkinnän" in {
        val opiskeluoikeus = createOpiskeluoikeus(oppija, uusiOpiskeluoikeus, resetFixtures = true)
        authGet("api/opiskeluoikeus/historia/" + opiskeluoikeus.oid.get + "/1") {
          verifyResponseStatus(200)
          val versio = readOpiskeluoikeus
          versio should equal(opiskeluoikeus)
          AuditLogTester.verifyAuditLogMessage(Map("operaatio" -> "MUUTOSHISTORIA_KATSOMINEN"))
        }
      }
      "Tuntematon versionumero" - {
        "Palautetaan 404" in {
          val opiskeluoikeus = createOpiskeluoikeus(oppija, uusiOpiskeluoikeus, resetFixtures = true)
          authGet("api/opiskeluoikeus/historia/" + opiskeluoikeus.oid.get + "/2") {
            verifyResponseStatus(404, KoskiErrorCategory.notFound.versiotaEiLöydy("""Versiota 2 ei löydy opiskeluoikeuden [^ ]+ historiasta.""".r))
          }
        }
      }
    }
  }

  def getHistory(opiskeluoikeusOid: String): List[OpiskeluoikeusHistory] = {
    authGet("api/opiskeluoikeus/historia/" + opiskeluoikeusOid) {
      verifyResponseStatus(200)
      Json.read[List[OpiskeluoikeusHistory]](body)
    }
  }

  def verifyHistory(oppija: Henkilö, opiskeluoikeus: Opiskeluoikeus, versions: List[Int]): Unit = {
    val historia: List[OpiskeluoikeusHistory] = getHistory(opiskeluoikeus.oid.get)
    historia.map(_.versionumero) should equal(versions)

    markup("Validoidaan versiohistorian eheys")

    authGet("api/opiskeluoikeus/validate/" + opiskeluoikeus.oid.get) {
      // Validates version history integrity by applying all history patches on top of first version and comparing to stored final value.
      verifyResponseStatus(200)
    }
  }
}
