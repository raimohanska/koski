package fi.oph.koski.userdirectory

import fi.oph.koski.koskiuser.MockUsers

object MockDirectoryClient extends DirectoryClient {
  def findUser(username: String) = {
    MockUsers.users.find(_.username == username).map(_.ldapUser)
  }
  def authenticate(userid: String, password: String) = findUser(userid).isDefined && userid == password
}
