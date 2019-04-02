package progfun

object FunctionsAsData extends App {

  // 0. Functions and compositions.

  val f: String => Int = _.size
  val g: Int => Boolean = _ % 2 == 0

  val u: String => Boolean = f andThen g
  g(f("xx"))
  val x: Boolean = u("sss")

  println(x)

  // 1. Set as a function

  type FunSet[A] = A => Boolean

  def empty[A]: FunSet[A] = (x: A) => false

  def pure[A](a: A): FunSet[A] = (x: A) => a == x

  def union[A](s1: FunSet[A], s2: FunSet[A]): FunSet[A] = (x: A) => s1(x) || s2(x)

  def intersect[A](s1: FunSet[A], s2: FunSet[A]): FunSet[A] = (x: A) => s1(x) && s2(x)

  val twoItems = union(pure(1), pure(2))
  val S = union(twoItems, pure(3))

  println("Set as a function: ")
  println("Is 1 in {1, 2, 3} ? " + S(1))
  println("Is 2 in {1, 2, 3} ? " + S(2))
  println("Is 3 in {1, 2, 3} ? " + S(3))
  println("Is 0 in {1, 2, 3} ? " + S(0))
  println("Is 4 in {1, 2, 3} ? " + S(4))
  println("x belong to empty: " + empty[Int](3))
  println()

  // Lets play same game but for intersection of sets
  // {1, 2, 3} with empty
  // {1, 2, 3} with {2, 3}

  // 2. Can we use standard Scala `Set` for the case above?
  val s1: FunSet[Int] = Set(1, 2)
  val s2: FunSet[Int] = Set(3)

  val set = union(s1, s2)
  val set2 = intersect(s1, s2)

  println("Standard Scala Set is still function: ")
  println("Is 1 in {1, 2, 3} ? " + set(1))
  println("Is 2 in {1, 2, 3} ? " + set(2))
  println("Is 3 in {1, 2, 3} ? " + set(3))
  println("Is 0 in {1, 2, 3} ? " + set(0))


  // 3. What about Map?
  // play with this on your own

  type FunMap[K, V] = K => V

  def emptyMap[K, V]: FunMap[K, V] = (k:K) => throw new Error()

  def get[K, V](map: FunMap[K, V])(k: K): V = map(k)

  def put[K, V](k: K, v: V)(map: FunMap[K, V]): FunMap[K, V] = k1 => {
    if(k1==k) v
    else map(k1)
  }


  val res = (put(1, "A") _ andThen put(2, "B") _) (emptyMap[Int, String])
  println("Set as a function: ")
//   This is not compile
   println(res(1))
   println(res(2))
  println(res(3))

  // 4. Last trick here the same as above. What about standard Scala Map?
  val map = Map(
    1 -> "A",
    2 -> "B"
  )

  println("!!!"  + (put(1, "A") _ andThen put(2, "B") _) (Map.empty)(5))
}
