package progfun

object MapAsFunction extends App {

  type FunMap[K, V] = K => V

  def emptyMap[K, V]: FunMap[K, V] = k => ???

  def get[K, V](k: K)(map: FunMap[K, V]): V = map(k)

  def put[K, V](k: K, v: V)(map: FunMap[K, V]): FunMap[K, V] = {
    k1 => if (k == k1) v else map(k1)
  }

  val res: FunMap[Int, String] = (put(1, "A")_ andThen put(2, "B")_)(emptyMap[Int, String])
  println(res(1))
  println(res(2))

  val map  = Map(
    1 -> "A1",
    2 -> "B1"
  )

  println(get(1)(map))
  println(get(2)(map))

  val res2 = (put(1, "A2")_ andThen put(2, "B2")_)(Map.empty)
  println(res2(1))
  println(res2(2))
}
