package fi.oph.koski.api

import fi.oph.koski.henkilo.HenkilötiedotSearchResponse
import fi.oph.koski.http.HttpSpecification
import fi.oph.koski.json.JsonSerializer
import fi.oph.koski.koskiuser.UserWithPassword
import fi.oph.koski.perustiedot.OpiskeluoikeudenPerustiedot
import fi.oph.koski.schema.HenkilötiedotJaOid

trait SearchTestMethods extends HttpSpecification {
  def search[T](query: String, user: UserWithPassword)(f: => T) = {
    get("api/henkilo/search", params = List(("query" -> query)), headers = authHeaders(user)) {
      f
    }
  }

  def searchForNames(query: String, user: UserWithPassword = defaultUser): List[String] = {
    searchForHenkilötiedot(query, user).map(_.kokonimi)
  }

  def searchForHenkilötiedot(query: String, user: UserWithPassword = defaultUser): List[HenkilötiedotJaOid] = {
    search(query, user) {
      verifyResponseStatusOk()
      JsonSerializer.parse[HenkilötiedotSearchResponse](body).henkilöt
    }
  }

  def searchForPerustiedot(queryParams: Map[String, String], user: UserWithPassword = defaultUser): List[OpiskeluoikeudenPerustiedot] = {
    get("api/opiskeluoikeus/perustiedot", params = queryParams, headers = authHeaders(user)) {
      readPaginatedResponse[List[OpiskeluoikeudenPerustiedot]]
    }
  }
}
