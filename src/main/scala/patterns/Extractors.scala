package patterns

object Extractors extends App {
  object Name {

    def unapplySeq(name: String): Option[Seq[String]] = {
      val parts = name.trim.split(" ")
      if (parts.isEmpty) None else Some(parts.toIndexedSeq)
    }
  }

  object IsCompaund {
    def unapply(name: String): Boolean = name.trim.split(" ").length > 2
  }

  def shortName(name: String): String = name match {
    case Name(first, last)               => s"${first(0)}. $last"
    case Name(first, m, last)            => s"${first(0)}. $m $last"
    case Name(first, "van", "der", last) => s"${first(0)}. van der $last"
    case Name(first, last @ _*)          => s"${first(0)}. ${last.head(0)}. ${last.tail.mkString(" ")}"
  }

  println(shortName("Martin Odersky"))
  println(shortName("Erich von Manstein"))
  println(shortName("Johannes van der Waa ls"))
  println(shortName("Johannes Diderik van der Waals"))

  println()
  def determineNationality(name: String): Unit = name match {
    case Name(first, "von", last) => println(s"${first(0)}. von $last is German")
    case Name(first, "van", "der", last) => println(s"${first(0)}. van der $last is Dutch")
    case Name(first1, first2, "van", "der", last) => println(s"${first1(0)}. ${first2(0)}. van der $last is Dutch")
    case Name(_*) => println(s"${shortName(name)} is some other")
    case _ => print("N/A")
  }

  determineNationality("Martin Odersky")
  determineNationality("Erich von Manstein")
  determineNationality("Johannes van der Waals")
  determineNationality("Johannes Diderik van der Waals")

}