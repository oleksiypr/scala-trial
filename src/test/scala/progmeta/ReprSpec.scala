package progmeta

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.should.Matchers.shouldBe

object ReprSpec {
  case class Qux() derives Repr
  case class Bar(m: Double) derives Repr
  
  case class Baz(n: Int, m: Double)
  case class Foo(n: Int, bar: Bar) derives Repr
  case class Quux(s: String, b: Option[Boolean]) derives Repr
  
  enum Foobar:
    case A(s: String)
    case B(i: Int)
    case C(b: Boolean)
}

class ReprSpec extends AnyFunSuite with Matchers {
  
  import ReprSpec.*
  import Repr.*
  
  test("Repr for Qux()") {
    val qux = Qux()
    qux.repr shouldBe "Qux()"
  }

  test("Repr for Bar(2.0)") {
    val bar = Bar(2.0)
    bar.repr shouldBe "Bar(m: Double = 2.0)"
  }

  test("Repr for Baz(1, 2.0)") {
    val bar = Baz(1, 2.0)
    bar.repr shouldBe "Baz(n: Int = 1, m: Double = 2.0)"
  }
  
  test("Repr for Foo(1, Bar(2.0))") {
    val foo = Foo(1, Bar(2))
    foo.repr shouldBe "Foo(n: Int = 1, bar: Bar = Bar(m: Double = 2.0))"
  }
  
  test("Repr for Option[Boolean]") {
    Some(true).repr shouldBe "Some(value: Boolean = true)"
    None.repr shouldBe "None()"
  }
  
  test("Repr for Quux('hello', maybe)") {
    Quux("hello", Some(true)).repr shouldBe "Quux(s: String = hello, b: Option = Some(value: Boolean = true))"
    Quux("buy", None).repr shouldBe "Quux(s: String = buy, b: Option = None())"
  }
  
  test("how erasedValue works") {
    getErasedValue[List[Int]] shouldBe List("::", "Nil")
    getErasedValue[Baz] shouldBe List("Int", "Double")
    getErasedValue[(String, Boolean)] shouldBe List("String", "Boolean")
    getErasedValue[Foobar] shouldBe List("A", "B", "C")
  }
}
