package progmeta

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import scala.quoted.staging.*
import scala.quoted.*

class DebugSpec extends AnyFunSuite with Matchers {

  given Compiler = Compiler.make(getClass.getClassLoader)

  test("meta") {
    given Compiler = Compiler.make(getClass.getClassLoader)

    withQuotes {
      Debug.debugImpl('{1}).show shouldBe
        '{ val value = 1; println("1" + " = " + value); value }.show
    }
  }
}
