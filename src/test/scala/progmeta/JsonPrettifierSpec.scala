package progmeta

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import JsonPrettifier.{given, *}

class JsonPrettifierSpec extends AnyFunSuite with Matchers {
  
  test("prettify Foo JSON") {
    val foo = Foo(42)
    foo.pretty shouldBe 
    """
        |{
        | "x": 42
        |}""".stripMargin
  }
}