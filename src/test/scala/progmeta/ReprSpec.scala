package progmeta

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.should.Matchers.shouldBe

object ReprSpec {

  case class Foo(x: Int) derives Repr
}

class ReprSpec extends AnyFunSuite with Matchers {

  test("Repr type-class") {
    val repr = Repr[Foo]
    repr.repr(Foo(1)) shouldBe "Foo(x: Int = 1)"
  }
}
