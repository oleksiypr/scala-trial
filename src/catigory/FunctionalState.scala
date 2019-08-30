package catigory

object FunctionalState extends App {

  object Variant1 {

    trait State[S, C, E] {

      def origin: S
      def validate(s: S)(cmd: C): Option[E]
      def updated(s: S)(e: E): S
    }

    object State {

      implicit class StateOps[S, C, E](val s: S) extends AnyVal {

        def validate(cmd: C)(
          implicit ev: State[S, C, E]): Option[E] = ev.validate(s)(cmd)

        def updated(e: E)(implicit ev: State[S, C, E]): S = ev.updated(s)(e)
      }

      @scala.annotation.tailrec
      def program[S, C, E](r: () => C)(s: S)(
          implicit ev: State[S, C, E]
        ): Unit = {
        val cmd = r()
        s.validate(cmd) match {
          case Some(e) => program(r)(s.updated(e))
          case None => program(r)(s)
        }
      }
    }
  }

  object Variant2 {

    trait State { self =>

      type S
      type Event
      type Command

      def origin: S
      def validate(s: S)(cmd: Command): Option[Event]
      def updated(s: S)(e: Event): S

      implicit class StateOp(val s: S) {

        def validate(cmd: Command): Option[Event] = self.validate(s)(cmd)
        def updated(e: Event): S = self.updated(s)(e)
      }
    }

    trait Program {

      val state: State
      import state._

      def expectCommand(): state.Command

      def run(s: S): Unit = {
        val cmd = expectCommand()
        s.validate(cmd) match {
          case Some(e) => run(s.updated(e))
          case None => run(s)
        }
      }
    }
  }
}
