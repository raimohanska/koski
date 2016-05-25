# Koski

Koski (aiemmin TOR) tulee toimimaan kattavana opetustoimialan tietovarantona, joka tarjoaa
tutkintoon johtavat suoritustiedot eri koulutusasteilta. Yleinen Koski-dokumentaatio kootaan CSC:n wikiin: https://confluence.csc.fi/display/OPHPALV/Koski

Tässä git-repositoriossa on Koski-järjestelmän ohjelmakoodi, tietokannan rakennuslausekkeet ja tekninen dokumentaatio ohjelmistokehitystä varten.

Koski on [EUPL](LICENSE.txt)-lisensoitu sovellus, josta on mahdollista käynnistää kehitysinstanssi omalla työasemalla, alla olevien kehitysohjeiden mukaisesti. Koski-sovellus on alustariippumaton, sillä se pyörii Java-virtuaalikoneella. Kehitysympäristö toimii sellaisenaan ainakin Linux ja OSX-käyttöjärjestelmissä.

Huom! Koski-järjestelmän kehitys on vielä alkuvaiheessa ja kaikki tässä dokumentissa kuvattu voi vielä muuttua matkan varrella.

## Käsitteet

Keskeiset entiteetit, ja järjestelmät, joihin nämä tallennetaan.

| käsite         | selite                                       | tunniste         | tallennuspaikka        |
|----------------|----------------------------------------------|------------------|------------------------|
| Koodisto       | Kooditus objekteille, esim tutkintonimikkeet | id (tekstiä)     | Koodistopalvelu        |
| Koodi          | Yksittäisen objektin koodi koodistossa       | id (tekstiä)     | Koodistopalvelu        |
| Oppija         | Opiskelija, oppilas.                         | henkilöOid       | Henkilöpalvelu         |
| Organisaatio   | Oppilaitos, kunta, eri rooleissa             | organisaatioOid  | Organisaatiopalvelu    |
| Opinto-oikeus  | Oppijan suhde oppilaitokseen ja suoritettavaan tutkintoon (tutkinto, oppija, oppilaitos, voimassaoloaika, läsnäolotiedot...) | id (numeerinen)  | Koski        |
| Peruste        | Tutkinnon tai tutkinnon osan peruste         | diaarinumero     | ePerusteet             |
| Suoritus       | Oppijan suoritus (tutkinto, oppija, oppilaitos, aika...) | id (numeerinen)  | Koski        |
| Tutkinto       | Tutkinnon kuvaus (tutkintotunnus, nimi...)   | tutkintotunnus   | Koodisto               |

## Teknologiat

Nämä ovat keskeiset Koski-järjestelmässä käytettävät teknologiat. Lista kuvaa järjestelmän nykytilaa ja muuttuu matkan varrella
tarpeen mukaan.

- PostgreSQL 9.4 -tietokanta
- Palvelinteknologiat
  - Scala 2.11.4 ohjelmointikieli ja kääntäjä
  - Scalatra web framework
  - Slick (http://slick.typesafe.com/doc/3.0.1/index.html) relaatiokantakirjasto
  - Flyway migraatiotyökalu kannan skeeman rakentamiseen ja päivittämiseen kehityksessä ja tuotannossa
  - Maven build-työkalu kehityskäyttöön ja asennettavan paketin rakentamiseen
  - Mvn-riippuvuuksien lataus Jitpackilla, jolloin voidaan viitata suoraan Github-repoihin, eikä tarvitse itse buildata jar-artifaktoja
  - Integraatiot Opintopolku-palveluihin, kuten organisaatio- ja henkilöpalveluun REST-rajpinnoilla, käyttäen http4s-clienttiä
- Web-sovelluksen frontend-teknologiat
  - NPM riippuvuuksien hakuun   
  - Webpack frontend bundlen rakentamiseen
  - React
  - Bacon.js
  - LESS
  - Selainyhteensopivuus IE10-selaimesta moderneihin selaimiin
- Koko järjestelmän buildaus Make-työkalulla, joka delegoi yksittäiset toiminnot eri työkaluille, kuten Maven ja NPM

## Kehitystyökalut

Minimissään tarvitset nämä:

- Git (osx, linux sisältää tämän, komentorivillä `git`)
- GNU Make (osx, linux sisältää tämän, komentorivillä `make`)
- Java 8
- Maven 3 (osx: `brew install maven`)
- Postgres 9.4 (osx: `brew install postgresql94`)
- Node.js ja NPM (osx: `brew install node`)
- Tekstieditori (kehitystiimi käyttää IntelliJ IDEA 14/15)

## Buildi ja ajaminen

Koski:n buildiin kuuluu frontin buildaus (npm / webpack) ja serverin buildaus Mavenilla. Tätä helpottamaan on otettu käyttöön `make`, jonka avulla
eri taskit on helppo suorittaa. Ks `Makefile`-tiedosto.

Buildaa koko systeemi

    make build    

Buildaa frontti, ja päivitä automaattisesti kun tiedostoja muokataan:

    make watch
    
Staattinen analyysi [ScalaStyle](http://www.scalastyle.org/) ja [ESLint](http://eslint.org/)

    make lint

### Koski-sovelluksen ajaminen kehitystyöasemalla

Aja JettyLauncher-luokka IDEAsta/Eclipsestä, tai käynnistä Koski vaihtoehtoisesti komentoriviltä

    make build
    make run

Avaa selaimessa

    http://localhost:7021/tor

Suoritus-testidatat näkyy

    http://localhost:7021/tor/suoritus/

### Ajaminen paikallisesti käyttäen ulkoisia palveluja (esim henkilöpalvelu)

Ilman parametrejä ajettaessa Koski käyttää mockattuja ulkoisia riippuvuuksia.

Ottaaksesi käyttöön ulkoiset integraatiot, kuten henkilpalvelun, voit antaa Koski:lle käynnistysparametrinä käytettävän konfiguraatiotiedoston sijainnin. Esimerkiksi

    -Dconfig.resource=qa.conf

Tällä asetuksella käytetään tiedostoa `src/main/resources/qa.conf`. Tämä tiedosto ei ole versionhallinnassa, koska se sisältää ei-julkista tietoa.

## Paikallinen PostgreSQL-tietokanta

Kehityskäyttöön tarvitaan paikallinen PostgreSQL-tietokanta. Koski-sovellus luo paikallisen kannan, skeeman ja käyttäjän
automaattisesti ja käynnistää myös tarvittavan PostgreSQL-serveriprosessin.

Paikallisen kannan konfiguraatio on tiedostossa `postgresql/postgresql.conf` ja tietokannan datahakemisto on `postgresql/data`.

Jos haluat pitää Postgresin käynnissä erikseen, voit käynnistää sen komentoriviltä komennolla

    make postgres

PostgreSQL jää pyörimään konsoliin ja voit sammuttaa sen painamalla ctrl-c.

Käynnistyessään Koski-sovellus huomaa, jos tietokanta on jo käynnissä, eikä siinä tapauksessa yritä käynnistää sitä.

Kehityksessä käytetään kahta kantaa: `tor` jota käytetään normaalisti ja `tortest` jota käytetään automaattisissa
testeissä (tämä kanta tyhjennetään aina testiajon alussa). Molemmat kannat sisältävät `tor` -skeeman, ja sijaitsevat
fyysisesti samassa datahakemistossa.


### SQL-yhteys paikalliseen kantaan

Jos ja kun haluat tarkastella paikallisen kehityskannan tilaa SQL-työkalulla, se onnistuu esimerkiksi Postgren omalla komentorivityökalulla `psql`:

    psql -h localhost tor tor
    psql -h localhost tortest tor

Peruskomennot

    \dt    listaa taulut
    \q     poistuu psql:stä

Sitten vaikka

    select * from arviointi;

### Kantamigraatiot

Tietokannan rakenne luodaan ja päivitetään Flywayllä migraatioskripteillä, jotka ovat hakemistossa `src/main/resources/db/migration`.

Koski-sovellus ajaa migraatiot automaattisesti käynnistyessään.

### Testit

Buildaa ja aja kaikki testit

    make test

Kun applikaatio pyörii paikallisesti (ks. ohjeet yllä), voi Mocha-testit ajaa selaimessa osoitteessa

    http://localhost:7021/tor/test/runner.html

Mocha-testit voi ajaa myös nopeasti komentoriviltä

    make fronttest

## CI-palvelin

Koski:n Jenkins CI-palvelin palvelee osoitteessa http://86.50.170.109:8080/, jonne pääsy on rajattu käyttäjätunnuksella ja salasanalla.

CI-palvelimella sovellus testataan jokaisen commitin yhteydessä. Paikallisten testien lisäksi ajetaan pieni määrä integraatiotestejä testiympäristön REST-rajapintoja vasten.
 
Myös staattinen analyysi [ScalaStyle](http://www.scalastyle.org/) ja [ESLint](http://eslint.org/) -työkaluilla ajetaan joka commitille.

Suorituskykytestit ajetaan joka aamu. 

CI-palvelimen konfiguraatio synkronoidaan [Github-repositorioon](https://github.com/Opetushallitus/koski-ci-configuration) Jenkins SCM sync congiguration [pluginilla](https://wiki.jenkins-ci.org/display/JENKINS/SCM+Sync+configuration+plugin).

## Loggaus

Koski loggaa tapahtumia kolmeen erilliseen logitiedostoon

1. `koski-audit.log` eli Audit-logi sisältää kaikki tärkeät käyttäjätapahtumat, kuten login, tietojen haut hakuehtoineen, tietojen katselu, lisäys ja muokkaus. Logista ilmenee aina käyttäjän OID ja IP-osoite.
2. `koski-access.log` eli Access-logi sisältää kaikki palvelimen käsittelemät HTTP-pyynnöt polkuineen, paluukoodeineen ja käsittelyaikoineen.
3. `koski.log` eli "sovelluslogi" sisältää kehitys- ja diagnostiikkatietoa, kuten kaikki virheilmoitukset

Kaikkien logien tapahtumat siirretään testiympäristön palvelimilta Filebeat-agentilla Elasticsearch -tietokantaan, josta ne ovat katseltavissa Kibana-käyttöliittymän avulla.

Loggaus on konfiguroitu tiedostoilla `log4j.properties` ja `log4j-cloud.properties`, joista edellinen määrittää loggausksen kehitysympäristössä ja jälkimmäinen tuotantoympäristössä. Tämän konfiguraatiotiedoston avulla määritellään esimerkiksi se, mitä logataan mihin tiedostoon. Kuten konfiguraatiotiedostosta ilmenee, tapahtuu access-loggaus ohjaamalla Jettyn `RequestLog`-luokan logitus `koski-access.log` -tiedostoon. Vastaavasti `fi.vm.sade.auditlog.Audit` -luokan loggaus ohjataan tiedostoon `koski-audit.log` ja kaikki muu sovelluslogiin `koski.log`.

Koski-sovelluskoodissa audit-loggaus tehdään `AuditLog`-luokan kautta ja sovellusloggaus käyttäen `Logging`-luokkaa, jolta sovelluskoodi saa käyttöönsä loggerin, joka automaattisesti liittää logiviesteihin käyttäjä- ja IP-osoitetiedot.

## Testiympäristö (CSC:n ePouta-pilvessä)

### URLit

Testiympäristön Koski löytyy täältä:

    http://tordev.tor.oph.reaktor.fi/tor/

Ympäristöön kuuluvat Opintopolku-palvelun osat täällä:

    https://virkailija.tordev.tor.oph.reaktor.fi/

Esimerkiksi henkilöpalvelu:

    https://virkailija.tordev.tor.oph.reaktor.fi/authentication-service/swagger/index.html

Testiympäristö käyttää tuotannon ePerusteet-palvelua

    https://eperusteet.opintopolku.fi/

Koodistopalvelua käytetään toistaiseksi Opintopolun QA-ympäristöstä

    https://testi.virkailija.opintopolku.fi/koodisto-ui/html/index.html#/etusivu

### Sovelluksen asennus pilveen (CSC:n ePouta)

Ennakkovaatimukset:

1. Sinulla on tunnus CSC:n cPouta-ympäristöön
2. Poudan ympäristö(muuttuja)määrittely: https://pouta.csc.fi/dashboard/project/access_and_security/api_access/openrc/ on ladattu ja käytössä (`source Project_2000079-openrc.sh`)
3. Sinun julkinen ssh avain on lisättynä tänne: https://github.com/reaktor/oph-poutai-env/tree/master/roles/ssh.init/files/public_keys (ja koneiden konfiguraatio on päivitetty)
4. Käytössäsi Homebrew:n openssl ja ruby, koska OSX:n mukana tuleva OpenSSL 0.9.8zg ei toimi Poudan kanssa:
  * `brew install openssl`
  * `brew install ruby`

Tämän jälkeen voit pushata uuden version Koski:sta ajamalla,

    make deploy

Lokien katsominen onnistuu komennolla:

    make tail
    
Serverille pääsee myös ssh:lla kätevästi:

    make ssh
    less /home/git/logs/tor.log

## Toteutus ja integraatiot

### Konfigurointi

Sovellus käyttää konfigurointiin [Typesafe Config](https://github.com/typesafehub/config) -kirjastoa, 
jonka avulla tarvittavat asetukset haetaan tiedostoista ja/tai komentoriviltä.

Sovelluksen oletusasetukset ovat tiedostossa [src/main/resources/reference.conf]. 
Kun sovellus käynnistetään ilman ulkoisia parametrejä, käynnistyy se näillä asetuksilla 
ja toimii "kehitysmoodissa", eli käynnistää paikallisen tietokannan, 
eikä ota yhteyttä ulkoisiin järjestelmiin.

Tuotantokäytössä ja testiympäristössä käytetään asetuksia, joilla Koski saadaan ottamaan yhteys ulkoisiin
järjestelmiin. Pilviympäristössä käytetään tällä hetkellä [cloud/restart.sh] -skriptiä, jolla annetaan
tarvittavat asetukset.

Kehityskäytössä voit käyttää erilaisia asetuksia tekemällä asetustiedostoja, kuten vaikkapa [src/main/resources/tordev.conf]
(ei versionhallinnassa, koska sisältää luottamuksellista tietoa) ja antaa käytettävän tiedoston nimi käynnistysparametrina, 
esim. `-Dconfig.resource=tordev.conf`. Valmiita asetustiedostoja voi pyytää kehitystiimiltä.

### Henkilötiedot

Koski ei tallenna henkilötietoja omaan kantaansa, vaan hakee/tallentaa ne Opintopolun [henkilöpalveluun](https://github.com/Opetushallitus/henkilo). [toteutus](src/main/scala/fi/oph/tor/oppija/OppijaRepository.scala)

Kun TORissa haetaan henkilön tietoja esimerkiksi sukunimellä, haetaan lista mahdollisista henkilöistä ensin henkilöpalvelusta, jonka jälkeen se [suodatetaan](src/main/scala/fi/oph/tor/opiskeluoikeus/OpiskeluOikeusRepository.scala#L8)
TORissa olevien opinto-oikeuksien perusteella.

Käyttäjä voi nähdä vain ne opinto-oikeudet, jotka liittyvät oppilaitokseen, johon hänellä on käyttöoikeus. Henkilön organisaatioliitokset ja käyttöoikeudet haetaan [henkilöpalvelusta](https://github.com/Opetushallitus/henkilo) ja [organisaatiopalvelusta](https://github.com/Opetushallitus/organisaatio). [toteutus](src/main/scala/fi/oph/tor/user/RemoteUserRepository.scala)

Esimerkkihaku: haetaan organisaatiopuurakenne.

    https://testi.virkailija.opintopolku.fi:443/organisaatio-service/rest/organisaatio/v2/hierarkia/hae?aktiiviset=true&suunnitellut=true&lakkautetut=false&&&&&&oid=1.2.246.562.10.50822930082&
    
Henkilöpalvelun swagger:

    https://virkailija.tordev.tor.oph.reaktor.fi/authentication-service/swagger/index.html

### ePerusteet

Tällä hetkellä TORiin voi tallentaa vain [ePerusteista](https://eperusteet.opintopolku.fi/) löytyvien tutkintojen tietoja. Opinto-oikeutta lisättäessa lista mahdollisista tutkinnoista haetaan
ePerusteista ja [Opinto-oikeuden](src/main/scala/fi/oph/tor/opiskeluoikeus/OpiskeluOikeus.scala) sisältämään [tutkinto](src/main/scala/fi/oph/tor/tutkinto/Tutkinto.scala)-osioon tallennetaan tieto ePerusteet-linkityksestä.

EPerusteista haetaan myös tutkinnon hierarkkinen [rakenne](src/main/scala/fi/oph/tor/tutkinto/TutkintoRakenne.scala), joka kuvaa, mistä tutkinnon osista tutkinto koostuu. [toteutus](https://github.com/Opetushallitus/koski/blob/master/src/main/scala/fi/oph/tor/eperusteet/RemoteEPerusteetRepository.scala)

EPerusteiden Swagger-dokumentaatio:

    https://eperusteet.opintopolku.fi/eperusteet-service/

Pari testiurlia:

    https://eperusteet.opintopolku.fi/eperusteet-service/api/perusteet?nimi=Ty%C3%B6njoh
    https://eperusteet.opintopolku.fi/eperusteet-service/api/perusteet/1013059
    https://eperusteet.opintopolku.fi/eperusteet-service/api/perusteet/1013059/kaikki
    https://eperusteet.opintopolku.fi/eperusteet-service/api/perusteet/diaari?diaarinumero=104/011/2014

### Koodistopalvelu

Koski käyttää [Koodistopalvelua](https://github.com/Opetushallitus/koodisto) mm. tutkintoihin liittyvien arviointiasteikkojen hakemiseen.

Koodistopalvelun Swagger-dokumentaatio:

    https://testi.virkailija.opintopolku.fi/koodisto-service/swagger/index.html

Pari testiurlia Koodistopalveluun:

    https://testi.virkailija.opintopolku.fi/koodisto-service/rest/codes/arviointiasteikkoammatillinenhyvaksyttyhylatty/1
    https://testi.virkailija.opintopolku.fi/koodisto-service/rest/codeelement/codes/arviointiasteikkoammatillinenhyvaksyttyhylatty/1
    
Koski osaa tarvittaessa luoda käytettävät koodistot ja koodistopalveluun. Käynnistä parametrillä `-Dkoodisto.create=true`. 

### LDAP

TORin käyttäjäautentikaatio on toteutettu Opintopolku-järjestelmän LDAPia vasten. LDAP-palvelimen osoite ja tunnukset konfiguroidaan `ldap.host`, `ldap.userdn` ja `ldap.password` -asetuksilla.
Mon Apr 18 14:18:11 EEST 2016
