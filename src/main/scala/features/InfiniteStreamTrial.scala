package features

object InfiniteStreamTrial {
  def integers(z: Int)(step: Int): LazyList[Int] = z #:: integers(z + step)(step)
  val fromZero: Int => LazyList[Int] = integers(0)

  println(fromZero(4).take(10).force)
  println(fromZero(3).take(7).force)

  val fromZeroByOne = fromZero(1)
  println(fromZeroByOne.take(3).size)

}
