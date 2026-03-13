package progmeta

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers


object ReprSpec {
  case class Foo() derives Repr
}


class ReprSpec extends AnyFunSuite with Matchers {
  import ReprSpec.*
  import Repr.*

  test("Repr for Foo()") {
    val qux = Foo()
    qux.repr shouldBe "Foo()"
  }

  test("Repr for Sum type") {
    val a: Option[Boolean] = Some(true)
    given R: Repr[Option[Boolean]] = Repr.derived
    a.repr shouldBe "Some(value: Boolean = true)"
  }

}
