package fi.oph.tor.koodisto

import java.time.LocalDate

import com.typesafe.config.Config

object KoodistoCreator {
  def createKoodistotFromMockData(config: Config): Unit = {
    val kp = LowLevelKoodistoPalvelu.withoutCache(config)
    // Koodistoryhmille ei ole GETtiä, jolla voisi tsekata, onko TOR-ryhmä olemassa.
    //kp.createKoodistoRyhmä(new KoodistoRyhmä(("TOR")))
    MockKoodistoPalvelu.koodistot.foreach(koodisto => createKoodistoFromMockData(koodisto, kp))
  }

  private def createKoodistoFromMockData(koodistoUri: String, kp: LowLevelKoodistoPalvelu): Unit = {
    val koodistoViite: KoodistoViite = kp.getLatestVersion(koodistoUri).getOrElse {
      MockKoodistoPalvelu.getKoodisto(koodistoUri) match {
        case None => throw new IllegalStateException("Mock not found: " + koodistoUri)
        case Some(koodisto) =>
          kp.createKoodisto(koodisto)
          koodisto.koodistoViite
      }
    }
    val koodit = kp.getKoodistoKoodit(koodistoViite).toList.flatten
    val luotavatKoodit = MockKoodistoPalvelu.getKoodistoKoodit(koodistoViite).toList.flatten.filter { koodi: KoodistoKoodi => !koodit.find(_.koodiArvo == koodi.koodiArvo).isDefined }
    luotavatKoodit.foreach { koodi =>
      kp.createKoodi(koodistoUri, koodi.copy(voimassaAlkuPvm = Some(LocalDate.now)))
    }
  }
}
