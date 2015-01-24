package sbtsh

import sbt._
import sbt.complete._
import sbt.complete.Parser._
import Keys._

// see http://pubs.opengroup.org/onlinepubs/009695399/utilities/xcu_chap02.html#tag_02_09_02

sealed trait ShCommand
object ShCommand extends Parsers {
  lazy val isSpecialChar: Char => Boolean = {
    case '\'' | '\"' | '|' | '&' | '>' | '<' | '(' | ')' | '{' | '}' | ';' => true
    case _ => false
  }
  lazy val SimpleCharClass = charClass(c => !isSpecialChar(c) && !c.isWhitespace, "simple character")
  lazy val SimpleString = SimpleCharClass.+.string
  lazy val QString = StringVerbatim | StringEscapable | SimpleString

  lazy val asyncCommand: Parser[ShCommand] =
    ((commandList <~ token("&") <~ SpaceClass.*) map { case cmd => AsyncCommand(cmd) }) |
    command
  lazy val command: Parser[ShCommand] =
    commandList <~ token(";").? <~ SpaceClass.*
  lazy val commandList: Parser[ShCommand] =
    (logicalCommand flatMap { cmd0 =>
      token(";") ~> command map { case cmd1 => ListCommand(cmd0, cmd1) }
    }) | logicalCommand
  lazy val logicalCommand = orCommand | pipeline
  lazy val orCommand =
    pipeline flatMap { cmd0 =>
      logicalOp ~ command map { case op ~ cmd1 => LogicalCommand(cmd0, op, cmd1) }
    }
  def pipeline: Parser[ShCommand] =
    (redirectCommand flatMap { cmd0 =>
      token("|") ~> command map { case cmd1 => PipeCommand(cmd0, cmd1) }
    }) | redirectCommand
  def redirectCommand: Parser[ShCommand] =
    ((command0 ~ redirectOp ~ fileTarget) map { case cmd ~ op ~ f =>
      RedirectCommand(cmd, op, new File(f))
    }) | command0
  def command0: Parser[ShCommand] = OptSpace ~> (simpleCommand | subshell | braceGroup) <~ SpaceClass.*
  def simpleCommand =
    (token(QString).examples("<cmd>") <~ token(OptSpace)) ~
    repsep(token(QString).examples("<arg>"), token(Space)) map {
      case x ~ args => SimpleCommand(x, args)
    }
  def braceGroup: Parser[ShCommand] =
    ((token("{") <~ OptSpace) flatMap { _ =>
      command <~ (OptSpace ~> token("}"))
    })
  def subshell: Parser[ShCommand] =
    ((token("(") <~ OptSpace) flatMap { _ =>
      command <~ (OptSpace ~> token(")"))
    })
  lazy val redirectOp =
    (token(">") map { _ => RedirectOp.> }) |
    (token(">>") map { _ => RedirectOp.>> }) |
    (token("<") map { _ => RedirectOp.< })
  lazy val logicalOp =
    (token("||") map { _ => LogicalOp.|| }) |
    (token("&&") map { _ => LogicalOp.&& })
  def fileTarget = SpaceClass.* ~> token(QString).examples("<file>") <~ SpaceClass.*
}

case class SimpleCommand(command: String, args: Seq[String]) extends ShCommand
case class RedirectCommand(command: ShCommand, op: RedirectOp, f: File) extends ShCommand
case class PipeCommand(command0: ShCommand, command1: ShCommand) extends ShCommand
case class LogicalCommand(command0: ShCommand, op: LogicalOp, command1: ShCommand) extends ShCommand
// POSIX would allow ( cmd; cmd; ) and ( cmd & cmd & ), but ProcessBuilder doesn't have #&
case class ListCommand(command0: ShCommand, command1: ShCommand) extends ShCommand
case class AsyncCommand(command: ShCommand) extends ShCommand
sealed trait LogicalOp
object LogicalOp {
  case object && extends LogicalOp
  case object || extends LogicalOp
}
sealed trait RedirectOp
object RedirectOp {
  case object > extends RedirectOp
  case object >> extends RedirectOp
  case object < extends RedirectOp
}
