package com.codesolutions.akka

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class AkkaHttpBaseSpec extends AnyWordSpec with Matchers with ScalatestRouteTest {

  "AkkaHttpBase" should {

    "return hello message" in {
      Get("/hello") ~> AkkaHttpBase.createRoute() ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] should include ("Hello from Akka HTTP base")
      }
    }

    "return enriched legacy data" in {
      Get("/legacy/123") ~> AkkaHttpBase.createRoute() ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] should include ("Enriched legacy data for id=123")
      }
    }
  }
}
