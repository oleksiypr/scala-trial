object Trial {

  def acceptNonEmpty[T](xs: ::[T]): Unit = {
    println("head is: " + xs.head)
  }

  acceptNonEmpty(::(1, List(2, 4, 5)))
  //acceptNonEmpty(List())
  //acceptNonEmpty(Nil)


  def atLeastOne(x: Int, xs: Int*): Unit = {
    println("we will have at least one: " + x)
    xs.foreach(x => print("but maybe this one to :" + x))
  }

  atLeastOne(1, 2, 3)

  case class Greeting(default: String, options: String*)

  val single = Greeting("hi")
  val many = Greeting("hi", "hello", "good morning")

  val greetings = List("hello", "good morning", "good day", "good evening")
  val more = Greeting("hi", greetings: _*)

  def handle(greeting: Greeting): Unit = greeting match {
    case Greeting(default) => println(s"just say :$default")
    case Greeting(default, options@ _*) =>
      println(s"you may chose: $default or ${options mkString " or "}")
  }

  handle(single)
  handle(many)
  handle(more)
}

