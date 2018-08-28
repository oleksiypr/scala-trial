package features

object InfiniteStreamTrial {
  def integers(z: Int)(step: Int): Stream[Int] = z #:: integers(z + step)(step)
  val fromZero: Int => Stream[Int] = integers(0)_

  println(fromZero(4).take(10).force)
  println(fromZero(3).take(7).force)

  val fromZeroByOne = fromZero(1)
  println(fromZeroByOne.take(3).size)

}
