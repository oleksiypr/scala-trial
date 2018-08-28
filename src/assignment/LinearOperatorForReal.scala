package assignment

object LinearOperatorForReal extends App {

  case class LinReal(as: Array[Double]) {
    def apply(xs: Array[Double]): Double = {
      (xs zip as).foldLeft(0.0) { (s, ax) =>
        ???
      }
    }
  }

  val as = Array(1.0, 2.0, 3.0)
  val xs = Array(3.0, 2.0, 1.0)

  val Lr = LinReal(as)
  println(Lr(xs))
}
