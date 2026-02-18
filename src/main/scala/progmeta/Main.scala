package progmeta

object Main {

  private case class Foo(x: Int) {
    infix def +(y: Foo): Foo = Foo(x + y.x)
  }


  @main def run(): Unit = {
    Debug.included(false) {
      val x = 10
      val y = 5
      Debug.debug(x + y)
      Debug.debug(x * y)
      Debug.debug(x / y)
      Debug.debug(math.pow(x, y))
      Debug.debug(4 + 7)
      Debug.debug(Foo(42))
      Debug.debug(Foo(42) + Foo(1))
    }

    Debug.included(true) {
      val x = 2.0
      square(x)
      qube(x)
      ()
    }
  }
}
