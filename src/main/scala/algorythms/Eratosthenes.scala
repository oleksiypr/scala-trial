package algorythms

object Eratosthenes extends App {
  def sieve(n: Int): LazyList[Int] = n #:: sieve(n + 1).filter(_ % n != 0)

  println(sieve(2).take(4).toList)
}
