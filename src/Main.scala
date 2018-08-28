
object Main extends App {

// 1
// 1 1
// 1 2 1
// 1 3 3 1
// 1 4 6 4 1


  def pascal(n: Int): List[Int] = {
    if (n == 1) List(1) else {
      1 :: next(pascal(n - 1))
    }
  }

  def next(ns: List[Int]): List[Int] = ns match {
    case Nil => Nil
    case h1 :: h2 :: tail =>  (h1 + h2) :: next(h2 :: tail)
    case h :: tail =>  h :: next(tail)
  }

  //println(next(List(1, 3, 3, 1)))


  println(pascal(1))
}
