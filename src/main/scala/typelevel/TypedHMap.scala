package typelevel

import scala.language.dynamics

object TypedHMap {
  opaque type M[T <: Tuple] = Map[Any, Any]

  type KeyMatch[Tup <: Tuple, K] = Tup match
    case EmptyTuple => false
    case (K *: ? *: EmptyTuple) *: ? => true
    case (? *: ? *: EmptyTuple) *: rest => KeyMatch[rest, K]

  type KeyExists[Tup <: Tuple, K] = KeyMatch[Tup, K] =:= true
  type KeyNotExists[Tup <: Tuple, K] = KeyMatch[Tup, K] =:= false

  type GetValue[Tup <: Tuple, K] = Tup match
    case EmptyTuple => Nothing
    case (K *: v *: EmptyTuple) *: ? => v
    case (? *: ? *: EmptyTuple) *: rest => GetValue[rest, K]

  type DropKey[Tup, K] <: Tuple = Tup match
    case EmptyTuple => EmptyTuple
    case (K *: ? *: EmptyTuple) *: rest => rest
    case (k *: t *: EmptyTuple) *: rest => (k *: t *: EmptyTuple) *: DropKey[rest, K]

  trait UpdateHandler[T <: Tuple, K <: Singleton, V]:
    type Out <: Tuple
    def apply(m: M[T], k: K, v: V): M[Out]

  given UpdateExisting[T <: Tuple, K <: Singleton, V](
      using
        ev1: KeyExists[T, K],
        ev2: GetValue[T, K] =:= V
    ): UpdateHandler[T, K, V] with
    type Out = T
    def apply(m: M[T], k: K, v: V): M[T] = m + (k -> v)

  given AddNew[T <: Tuple, K <: Singleton, V](
      using ev: KeyNotExists[T, K]
    ): UpdateHandler[T, K, V] with
    type Out = (K, V) *: T
    def apply(m: M[T], k: K, v: V): M[(K, V) *: T] = m.add(k, v)

  val Empty: M[EmptyTuple] = Map.empty

  extension [T <: Tuple](m: M[T])
    def add[K <: Singleton, V](k: K, v: V)(
      using KeyNotExists[T, K]
    ): M[(K, V) *: T] = m + (k -> v)

    def get[K <: Singleton](k: K)(using KeyExists[T, K]): GetValue[T, K] =
      m(k).asInstanceOf[GetValue[T, K]]
}

class Foo extends Dynamic {
  private var map: Map[String, Int] = Map.empty
  def selectDynamic(name: String): Int = map(name)
  def updateDynamic(name: String)(value: Int): Unit = map += (name -> value)
}

class DynamicRecord[T <: Tuple](hmap: TypedHMap.M[T]) extends Dynamic:

  def selectDynamic[K <: Singleton](k: K)(
    using ev: TypedHMap.KeyExists[T, K]
  ): TypedHMap.GetValue[T, K] = hmap.get(k)(using ev)

  def updateDynamic[K <: Singleton, V](k: K)(v: V)(
    using
    handler: TypedHMap.UpdateHandler[T, K, V]
  ): DynamicRecord[handler.Out] = new DynamicRecord(handler(hmap, k, v))

object Main {

  /*
    1. selectDynamic: Enables syntax like record.name, ensuring at compile time that the key exists.
    2. updateDynamic: Supports assignment like record.age = 31, either updating an existing key or adding a new one,
        returning a new record with the updated type.
  */
  @main def run(): Unit =
    val config = TypedHMap.Empty
      .add("host", "localhost")
      .add("port", 8080)
      .add("secure", false)

    val configRecord = new DynamicRecord(config)
    val host: String    = configRecord.host
    val port: Int       = configRecord.port
    val secure: Boolean = configRecord.secure

    println(host)
    println(port)
    println(secure)

    // Add a new config key
    val updatedConfig = configRecord.timeout = 5000
    println(updatedConfig.timeout) // 5000\
}
