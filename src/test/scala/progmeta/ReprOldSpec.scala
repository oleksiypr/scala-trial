package progmeta

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

object ReprOldSpec {
  case class Qux() derives ReprOld
  case class Bar(m: Double) derives ReprOld
  
  case class Baz(n: Int, m: Double) derives ReprOld
  case class Foo(n: Int, bar: Bar) derives ReprOld
  
  enum Foobar:
    case A(s: String)
    case B(i: Int)
    case C(b: Boolean)
}

class ReprOldSpec extends AnyFunSuite with Matchers {
  
  import ReprOld.*
  import ReprOldSpec.*
  
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

  test("Repr for Sum type") {
    val a: Option[Boolean] = Some(true)
    given ReprOld[Option[Boolean]] = ReprOld.derived
    a.repr shouldBe "Some(value: Boolean = true)"
  }

}
