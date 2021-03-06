package fi.oph.koski.util

import org.scalatest.{FreeSpec, Matchers}

class InvocationSpec extends FreeSpec with Matchers {
  "Invocation.toString" in {
    Invocation(classOf[String].getMethods()(0), List("hello", List(1,2,3)), "some target object").toString should equal("equals(\"hello\", _)")
  }
}
