import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ExampleTest extends AnyFunSuite with Matchers {
  
  test("basic test to verify structure") {
    val result = 2 + 2
    result shouldBe 4
  }
  
  test("string test") {
    "hello" should startWith("he")
  }
} 