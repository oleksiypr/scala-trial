package progmeta

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

object ReprSpec {
  case class Foo() derives Repr  
}


class ReprSpec extends AnyFunSuite with Matchers {
  

}
