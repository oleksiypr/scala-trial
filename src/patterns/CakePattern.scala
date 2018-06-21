package patterns

object CakePattern extends App {

  case class User(name: String)

  trait UserDao {
    def save(u: User): Unit
    def find(name: String): User
  }

  class DbUserDao extends UserDao {
    def save(u: User): Unit = println(s"Saved to data base: ${u.name}")
    def find(name: String): User = {
      println(s"search for $name in db")
      User(name)
    }
  }

  trait Repository {
    val userDao: UserDao
  }

  trait DbRepository extends Repository {
    val userDao = new DbUserDao
  }

  trait FakeRepository extends Repository {
    val userDao: UserDao = new UserDao {
      def save(u: User): Unit = println(s"saved to fake db: ${u.name}")
      def find(name: String): User =  {
        println(s"search for $name in fake db")
        User(name)
      }
    }
  }

  trait Service {
    this: Repository =>

    def create(name: String) {
      println(s"Do some stuff and preparation to create $name")
      val u = User(name)
      userDao.save(u)
    }

    def getUser(name: String): User = {
      println(s"Do some stuff and preparation to find $name")
      userDao.find(name)
    }
  }

  object Application extends Service with DbRepository
  Application.create("John Doe")

  object TestApplication extends Service with FakeRepository
  TestApplication.create("Fake User")
}
