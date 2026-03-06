package progmeta

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.should.Matchers.shouldBe

object LabelSpec {
  case class Foo(x: Int)
}

class LabelSpec extends AnyFunSuite with Matchers {
  test("Label type-class") {
    given Label[Foo] = foo => s"Foo(x: Int = ${foo.x})"
    val label = Label[Foo]
    label.label(Foo(1)) shouldBe "Foo(x: Int = 1)"
  }
}
