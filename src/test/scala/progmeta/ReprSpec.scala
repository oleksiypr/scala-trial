package progmeta

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers


object ReprSpec {
  case class Foo() derives Repr
  case class Bar(n: Int, m: Int) derives Repr
  case class Baz(n: Int, bar: Bar) derives Repr
  case class Qux(s: Option[String]) derives Repr
}

class ReprSpec extends AnyFunSuite with Matchers {
  import ReprSpec.*
  import Repr.*

  test("Repr for Foo()") {
    val foo = Foo()
    foo.repr shouldBe "Foo()"
  }

  test("Repr for Sum type") {
    val a: Option[Boolean] = Some(true)
    given R: Repr[Option[Boolean]] = Repr.derived

    a.repr shouldBe "Some(value: Boolean = true)"
    R.label shouldBe "Option"

    val b: Option[Boolean] = None
    b.repr shouldBe "None()"
  }

  test("Repr for Bar(1, 2)") {
    val bar = Bar(1, 2)
    bar.repr shouldBe "Bar(n: Int = 1, m: Int = 2)"
  }

  test("Repr for Some") {
    given R: Repr[Some[Boolean]] = Repr.derived
    given N: Repr[None.type ] = Repr.derived

    R.label shouldBe "Some"
    N.label shouldBe "None"
  }
  
  test("Repr for Baz(1, Bar(2, 3))") {
    val baz = Baz(1, Bar(2, 3))
    baz.repr shouldBe "Baz(n: Int = 1, bar: Bar = Bar(n: Int = 2, m: Int = 3))"
  }
  
  test("Repr for Qux(maybeString))") {
    Qux(Some("foo")).repr shouldBe "Qux(s: Option = Some(value: String = foo))"
    Qux(None).repr shouldBe "Qux(s: Option = None())"
  }
}
