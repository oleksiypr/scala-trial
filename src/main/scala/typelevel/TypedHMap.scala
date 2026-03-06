package typelevel

import scala.language.dynamics
import scala.util.NotGiven

type =!=[A, B] = NotGiven[A =:= B]

object TypedHMap {

  opaque type M[T <: Tuple] = Map[Any, Any]

  type GetValue[Tup <: Tuple, K] = Tup match
    case EmptyTuple                     => Nothing
    case (K *: v *: EmptyTuple) *: ?    => v
    case (? *: ? *: EmptyTuple) *: rest => GetValue[rest, K]

  type DropKey[Tup, K] <: Tuple = Tup match
    case EmptyTuple => EmptyTuple
    case (K *: ? *: EmptyTuple) *: rest => rest
    case (k *: t *: EmptyTuple) *: rest => (k *: t *: EmptyTuple) *: DropKey[rest, K]

  trait UpdateHandler[T <: Tuple, K <: Singleton, V]:
    type Out <: Tuple
    def apply(m: M[T], k: K, v: V): M[Out]

  trait DropHandler[T <: Tuple, K <: Singleton]:
    type Out <: DropKey[T, K]
    def apply(m: M[T], k: K): M[Out]

  given UpdateExisting[T <: Tuple, K <: Singleton, V](
      using ev: GetValue[T, K] =:= V
    ): UpdateHandler[T, K, V] with
    type Out = T
    def apply(m: M[T], k: K, v: V): M[Out] = m + (k -> v)

  given AddNew[T <: Tuple, K <: Singleton, V](
      using ev: GetValue[T, K] =:= Nothing
    ): UpdateHandler[T, K, V] with
    type Out = (K, V) *: T
    def apply(m: M[T], k: K, v: V): M[Out] =m + (k -> v)

  given Drop[T <: Tuple, K <: Singleton](
      using ev: GetValue[T, K] =!= Nothing
    ): DropHandler[T, K] with
    type Out = DropKey[T, K]
    def apply(m: M[T], k: K): M[Out] = m - k

  val Empty: M[EmptyTuple] = Map.empty

  extension [T <: Tuple](m: M[T])
    def add[K <: Singleton, V](k: K, v: V)(
      using GetValue[T, K] =:= Nothing
    ): M[(K, V) *: T] = m + (k -> v)

    def get[K <: Singleton](k: K)(using GetValue[T, K] =!= Nothing): GetValue[T, K] =
      m(k).asInstanceOf[GetValue[T, K]]

    def mkString: String = m.map((k, v) => s"$k: $v").mkString(", ")
}

class DynamicRecord[T <: Tuple](hmap: TypedHMap.M[T]) extends Dynamic {

  import TypedHMap.*

  def selectDynamic[K <: Singleton](k: K)(
    using ev: GetValue[T, K] =!= Nothing
  ): GetValue[T, K] = hmap.get(k)(using ev)

  def updateDynamic[K <: Singleton, V](k: K)(v: V)(
    using
    handler: UpdateHandler[T, K, V]
  ): DynamicRecord[handler.Out] = new DynamicRecord(handler(hmap, k, v))

  def remove: DynamicRecord[T] = new DynamicRecord(hmap) {
    def selectDynamic[K <: Singleton](k: K)(
      using handler: DropHandler[T, K]
    ): DynamicRecord[handler.Out] = new DynamicRecord(handler(hmap, k))
  }

  override def toString: String = hmap.mkString
}

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

    println(s"host           : $host")
    println(s"port           : $port")
    println(s"secure         : $secure")

    // Add a new config key
    val updatedConfig = configRecord.timeout = 5000
    println(s"updated timeout: ${updatedConfig.timeout}") // 5000

    val timeoutRemoved = updatedConfig.remove
    // This line would cause a compile-time error since "timeout
    // timeoutRemoved.timeout
    println(s"host           : ${timeoutRemoved.host}")
    println(s"port           : ${timeoutRemoved.port}")
    println(s"secure         : ${timeoutRemoved.secure}")
}
