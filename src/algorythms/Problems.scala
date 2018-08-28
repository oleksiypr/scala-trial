package algorythms

import scala.annotation.tailrec

object Problems extends App {

  import scala.collection.Searching._
  
  def decompose(n: Int, a: Array[Int]): Set[Int] = {
    def floor(x: Int, to: Int): Int = a.search(x, 0, to) match {
      case Found(i)          => i
      case InsertionPoint(i) => i - 1
    } 
    
    @tailrec
    def decomposeAcc (n: Int, i: Int, acc: Set[Int]): Set[Int] =  if (n == 0) acc else {
      val j = floor(n, to = i + 1)
      if (j < 0) acc else decomposeAcc(n - a(j), j, acc + a(j))
    }       
    decomposeAcc(n, a.length - 1, Set.empty)
  }
  
  val a = Array(70, 50, 20, 15, 7, 5, 3, 2)
  val cs = decompose(100, a.sorted)
  println(cs)

  println(decompose(100, Array(120, 98).sorted))
  println(decompose(2, Array(98).sorted))
}