package fi.oph.koski.koskiuser

import fi.oph.koski.userdirectory.DirectoryUser

case class AuthenticationUser(oid: String, username: String, name: String, serviceTicket: Option[String], kansalainen: Boolean = false) extends UserWithUsername with UserWithOid

object AuthenticationUser {
  def fromDirectoryUser(username: String, user: DirectoryUser) = AuthenticationUser(user.oid, username, user.etunimet + " " + user.sukunimi, None)
}