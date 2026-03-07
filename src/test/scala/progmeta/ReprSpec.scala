package progmeta

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.should.Matchers.shouldBe

object ReprSpec {
  case class Foo(n: Int, s: String, bar: Bar) derives Repr
  case class Bar(x: Double, b: Boolean) derives Repr
  case class Baz(n: Int) derives Repr
}

class ReprSpec extends AnyFunSuite with Matchers {
  
  import ReprSpec.*

  ignore("Repr Foo") {
    val repr = Repr[Foo]
    val foo = Foo(1, "hi", Bar(3.14, true))
    repr.repr(foo) shouldBe 
      "Foo(n: Int = 1, s: String = hi, bar: Bar = Bar(x: Double = 3.14, b: Boolean = true)"
  }
  
  test("Repr Baz(1)") {
    val repr = Repr[Baz]
    val baz = Baz(1)
    repr.repr(baz) shouldBe "Baz(n: Int = 1)"
  }
}
