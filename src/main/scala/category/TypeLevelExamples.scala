package catigory

object TypeLevelExamples extends App {

  def ipAuthUri(dev: String, ip: String): String = s"/internal/developers/$dev/$ip/authorized"
  def devUri(userId: String): String = s"/api/developers/$userId"

  final case class Developer(userId: String)
  final case class IpAuth(dev: String, ip: String)

  trait UriTemplate[R] {
    def uri(r: R): String
  }

  object UriTemplate {
    def apply[R: UriTemplate]: UriTemplate[R] = {
      implicitly[UriTemplate[R]]
    }
  }

  implicit object developerUriTemplate extends UriTemplate[Developer] {
    override def uri(d: Developer): String =   s"/api/developers/${d.userId}"
  }

  implicit object ipAuthUriTemplate extends UriTemplate[IpAuth] {
    override def uri(auth: IpAuth): String = {
      s"/internal/developers/${auth.dev}/${auth.ip}/authorized"
    }
  }

  def uri[R: UriTemplate](r: R): String = UriTemplate[R].uri(r)

  val devUri = uri(Developer("john"))
  val ipAuthUri = uri(IpAuth("dev-1", "192.168.0.1"))

  println(devUri)
  println(ipAuthUri)

  trait Currency
  case class Eur(amount: Double) extends Currency
  case class Uah(amount: Double) extends Currency
  case class Pln(amount: Double) extends Currency



  val eurs = List(Eur(10.5), Eur(20.0), Eur(8.50))
  val plns = List(Pln(27.60), Pln(15.50))
  val uahs = List(Uah(257.00))

  type Exchange[A <: Currency, B <: Currency] = A => B

  val f: Exchange[Eur, Uah] = e => Uah(e.amount / 30.00)

  def exchEurToEur(eur: Eur): Eur = eur
  def exchPlnToEur(pln: Pln): Eur = Eur(pln.amount / 1.50)
  def exchUahToEur(uah: Uah): Eur = Eur(uah.amount / 30.00)

  val totalEur = eurs.map(exchEurToEur) ++ plns.map(exchPlnToEur) ++ uahs.map(exchUahToEur)
  totalEur.fold(Eur(0.00))((a1, a2) => Eur(a1.amount + a2.amount))
}
