package progfun

object Variance extends App {

  class Fruit {
    override def toString: String = "fruit"
  }

  class Orange extends Fruit {
    override def toString: String = "orange is a " + super.toString
  }

  def doSomethingWith(fruit: Fruit): Unit = {
    println(fruit)
  }


  doSomethingWith(new Fruit)

  // Orange <: Fruit
  doSomethingWith(new Orange)

  def doSomethingWith(fruits: List[Fruit]): Unit = {
    fruits.foreach(println)
  }

  val fruits = List(new Fruit, new Fruit)
  val oranges = List(new Orange)

  doSomethingWith(fruits)
  doSomethingWith(oranges)

  // And what types of functions?

  class A extends X
  class B

  class X
  class Y extends B

  val f: A => B = a => new B
  val g: X => Y = x => new Y

  def doSomethingWith(f: A => B): Unit = {
    println("this works!")
  }

  doSomethingWith(f)

  // will this work?
  doSomethingWith(g)

  // we need X => Y <: A => B
  // to be continued ... (Barbara Liskov)
  // `L` in SO`L`ID
}
