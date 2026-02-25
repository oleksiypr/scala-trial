package progmeta

trait JsonPrettifier[T] {
  def pretty(value: T): String
}

object JsonPrettifier {
  
  extension [T](value: T)
    def pretty(using prettifier: JsonPrettifier[T]): String = 
      prettifier.pretty(value)
  
  given JsonPrettifier[Int] with {
    def pretty(value: Int): String = value.toString
  }

  given JsonPrettifier[String] with {
    def pretty(value: String): String = "\"" + value + "\""
  }

  given [T](using ev: JsonPrettifier[T]): JsonPrettifier[List[T]] with {
    def pretty(value: List[T]): String =
      value.map(ev.pretty).mkString("[", ", ", "]")
  }
}