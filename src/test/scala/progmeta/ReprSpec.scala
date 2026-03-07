package progmeta

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.should.Matchers.shouldBe

object ReprSpec {
  case class Qux() derives Repr
  case class Bar(m: Double) derives Repr
  case class Foo(n: Int, bar: Bar) derives Repr
}

class ReprSpec extends AnyFunSuite with Matchers {
  
  import ReprSpec.*
  import Repr.*
  
  test("Repr for Qux()") {
    val qux = Qux()
    qux.repr shouldBe "Qux()"
  }
  
  test("Repr for Foo(1, Bar(2.0))") {
    val foo = Foo(1, Bar(2))
    foo.repr shouldBe "Foo(n: Int = 1, bar: Bar = Bar(m: Double = 2.0))"
  }
}
