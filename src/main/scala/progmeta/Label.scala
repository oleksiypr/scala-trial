package progmeta

trait Label[T] {
  def label(t: T): String
}

object Label {
  def apply[T](using label: Label[T]): Label[T] = label
}
