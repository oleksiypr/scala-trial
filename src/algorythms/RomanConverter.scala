package algorythms

import scala.collection.Searching._

object RomanConverter extends App {
  val Romans = Map (
       1 -> "I",
       5 -> "V",
      10 -> "X",
      50 -> "L",
     100 -> "C",
     500 -> "D",
    1000 -> "M"
  )
  val Base = Romans.keys.toArray.sorted
  
  def toRoman(n: Int): String = {
    if (n > 1000) throw new IllegalArgumentException("No more then 1000")    
    if (n == 0) "" else { 
      val (floor, ceiling) = range(n)
      val times = n / floor
      val remain = n % floor
      val sub = lowDecade(ceiling)
      
      if (n >= ceiling - sub && n < ceiling) Romans(sub) + Romans(ceiling) + toRoman(n + sub - ceiling) else
      if (remain == 0) List.fill(times)(Romans(floor)) mkString "" 
      else List.fill(times)(Romans(floor)).mkString("") + toRoman(remain)   
    }
  }
  
  def range(n: Int): (Int, Int) = Base.search(n) match {
    case Found(i) => (Base(i), Base(i))
    case InsertionPoint(i) => (Base(i - 1), Base(i))
  }
  def lowDecade(base: Int) = if (base == 1) 1 else Base.search(base) match {
    case Found(i) if i % 2 == 0 => base / 10
    case Found(i) => base / 5
    case _ => throw new IllegalArgumentException("Must belong to a base of roman digits")
  }
  
  (1 to 100).toList foreach (i => println(i + ": " + toRoman(i))) 
  
  val test = 224
  println(test + ": " + toRoman(test))
}