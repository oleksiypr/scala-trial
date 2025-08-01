import catigory.FunctionalState.Variant1.State

import scala.annotation.tailrec
import scala.io.StdIn._
import scala.util.Try

object PlaySimpleGame extends App with SimpleGame with LowLevelImplicits {

  override def exit(): Unit = println("Good bye")

  def makeMove(): Move = {
    println("enter move steps:")
    Move(readLine())
  }

  val gameOver = play(gameState.origin, makeMove) {
    println("'Q' to quit or 'enter' to continue")
    readLine() match {
      case "Q" | "q" => false
      case _ => true
    }
  }
  println(gameOver)
}

trait GamePlay {

  import State._

  def exit(): Unit

  @tailrec
  final def play[G, C, E](game: G, mkMove: () => C)(continued: => Boolean)(
      implicit ev: State[G, C, E]): G = if (continued) {

    val move = mkMove()
    game.validate(move) match {
      case Some(e) => play(game.updated(e), mkMove)(continued)
      case None => play(game, mkMove)(continued)
    }
  } else {
    exit()
    game
  }
}

trait SimpleGame extends GamePlay {

  import math._

  type Position = Int

  def move(p: Position, delta: Position): Position = p + delta

  def isInside(p: Position, p1: Position, p2: Position): Boolean = {
    p >= min(p1, p2) && p <= max(p1, p2)
  }

  final case class Game(position: Position, history: List[Position])

  final case class Move(cmd: String)
  final case class Moved(from: Position, to: Position)
}

trait LowLevelImplicits { self: SimpleGame =>

  implicit object gameState extends State[Game, Move, Moved] {

    private val board: (Position, Position) = (-50, +50)
    private val p0: Position = 0

    override def origin: Game = Game(p0, Nil)

    override def validate(s: Game)(mv: Move): Option[Moved] = {
      for {
        steps <- Try(mv.cmd.toInt).toOption
        p = move(s.position, steps)
        if isInside(p, board._1, board._2)
      } yield {
        Moved(s.position, p)
      }
    }

    override def updated(game: Game)(e: Moved): Game = {
      game.copy(position = e.to, history = e.from :: game.history)
    }
  }
}
