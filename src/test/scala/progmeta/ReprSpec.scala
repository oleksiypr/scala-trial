package progmeta

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers


object ReprSpec {
  case class Foo() derives Repr
  case class Bar(n: Int) derives Repr
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

  test("Repr for Bar(1)") {
    val bar = Bar(1)
    bar.repr shouldBe "Bar(n: Int = 1)"
  }
}
