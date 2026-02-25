package progmeta

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import JsonPrettifier.{*, given}

object JsonPrettifierSpec {

  case class Bar(a: String, foo: Foo)

  object Bar {
    given JsonPrettifier[Bar] with
      override def pretty(value: Bar): String =
        s"""|{
            |  "a": ${value.a.pretty},
            |  "foo": {
            |    "x": ${value.foo.x.pretty}
            |  }
            |}""".stripMargin
  }
}

class JsonPrettifierSpec extends AnyFunSuite with Matchers {

  import JsonPrettifierSpec.*

  test("prettify Foo JSON") {
    val foo = Foo(42)
    foo.pretty shouldBe
    """|{
       |  "x": 42
       |}""".stripMargin
  }

  test("prettify List JSON") {
    val bar = Bar(a = "A", foo = Foo(42))
    val expected =
      """|{
         |  "a": "A",
         |  "foo": {
         |    "x": 42
         |  }
         |}""".stripMargin
    bar.pretty shouldBe expected
  }
}