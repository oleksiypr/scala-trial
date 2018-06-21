
object Main extends App {

  sealed trait Compressed[+A]
  case class Single[A](element: A) extends Compressed[A]
  case class Repeat[A](count: Int, element: A) extends Compressed[A]


  val xs = List(3, 2, 1, 1, 2)
  println(xs.scanLeft(0)((a, b) => a + b))

  val ys = Vector(Repeat(3, "A"), Repeat(2, "B"), Single("C"), Single("D"), Repeat(2, "E"))
  val ns = (ys map {
    case Repeat(n, _) => n
    case Single(_) => 1
  }).scanLeft(0)(_+_).drop(1)

  println(ns)


  import scala.collection.Searching._

  for (index <- 0 to 8) {
    println(index)

    ns.search(index + 1) match {
      case Found(i) => println(ys(i))
      case InsertionPoint(i) =>  println(ys(i))
    }
  }
}
