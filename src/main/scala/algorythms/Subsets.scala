package algorythms

object Subsets {

  def sub(n: Int): Set[Set[Int]] = {
    if (n == 0) Set(Set.empty[Int])
    else {
      val ss = sub(n - 1)
      ss ++ ss.map(_ + n)
    }
  }
}
