package algorythms

object NubmerSpell {
  val SpecialCases = Map (
    0 -> "zero",
    1 -> "one",
    2 -> "two",
    3 -> "three",
    4 -> "four",
    5 -> "five",
    6 -> "six",
    7 -> "seven",
    8 -> "eight",
    9 -> "nine",
    10 -> "ten",
    11 -> "eleven",
    12 -> "twelve",
    13 -> "thirteen",
    15 -> "fifteen",
    18 -> "eighteen",
    20 -> "twenty",
    30 -> "thirty",
    50 -> "fifty",
    80 -> "eighty"
  )
  val DecadesNames = Map (
          100 -> "hundred",
         1000 -> "thousand",
      1000000 -> "million",
   1000000000 -> "billion"
  )
  val Decades = DecadesNames.keys.toArray.sorted
  
  def spell(n: Int): String = {
    def remain(r: Int) = if (r == 0) "" else " " + spell(r)
    
    if (SpecialCases.contains(n)) SpecialCases(n) else
    if (n > 13 && n < 20) spell(n % 10) + "teen" else 
    if (n < 100) SpecialCases.get((n /10) * 10) match {
      case Some(v) => v + remain(n % 10)  
      case None => SpecialCases(n / 10) + "ty" + remain(n % 10)
    } else {
      val dec = decade(n)
      val rem = n % dec
      spell(n / dec) + " " + DecadesNames(dec) + remain(rem)
    }     
  }
  
  def decade(n: Int): Int = Decades.toList.takeWhile( _ <= n).last
}