package progmeta

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.should.Matchers.shouldBe

object ReprSpec {
  case class Qux() derives Repr
}

class ReprSpec extends AnyFunSuite with Matchers {
  
  import ReprSpec.*
  
  test("Repr for Qux()") {
    val repr = Repr[Qux]
    val qux = Qux()
    repr.repr(qux) shouldBe "Qux()"
  }
}
