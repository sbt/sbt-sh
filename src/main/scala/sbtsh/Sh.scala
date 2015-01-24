package sbtsh

import sbt._
import Keys._
import java.util.Locale

object Sh {
  lazy val noPreprocess: Seq[String] => Seq[String] = identity
  lazy val defaultPreprocess: Seq[String] => Seq[String] = {
    case commands if onWindows => preprocessForWindows(commands)
    case commands              => commands
  }
  lazy val onWindows: Boolean = {
    val isCygwin = sys.env.getOrElse("OSTYPE", "").toLowerCase(Locale.ENGLISH).contains("cygwin")
    val isWindows = sys.props.getOrElse("os.name", "").toLowerCase(Locale.ENGLISH).contains("windows")
    isWindows && !isCygwin
  }
  lazy val hasShell: Boolean = sys.env.contains("SHELL")
  lazy val posixShell: Option[String] = sys.env.get("SHELL")
  lazy val binSh = "/bin/sh"
  lazy val preprocessForWindows: Seq[String] => Seq[String] = {
    case commands => "cmd" +: "/c" +: commands
  }
  def run(cmd: ShCommand, prep: Seq[String] => Seq[String], log: Logger): Seq[String] =
    {
      val p = toProcess(cmd, prep)
      cmd match {
        case AsyncCommand(_) =>
          p.run()
          Nil
        case _ =>
          try {
            val xs = p.lines(log).toList
            xs foreach { x => log.info(x) }
            xs
          } catch {
            case e: Throwable => throw new MessageOnlyException(e.getMessage)
          }
      }
    }
  def toProcess(command: ShCommand, prep: Seq[String] => Seq[String]): ProcessBuilder =
    command match {
      case AsyncCommand(cmd)                        => toProcess(cmd, prep)
      case ListCommand(cmd0, cmd1)                  => toProcess(cmd0, prep) ### toProcess(cmd1, prep)
      case LogicalCommand(cmd0, LogicalOp.||, cmd1) => toProcess(cmd0, prep) #|| toProcess(cmd1, prep)
      case LogicalCommand(cmd0, LogicalOp.&&, cmd1) => toProcess(cmd0, prep) #&& toProcess(cmd1, prep)
      case RedirectCommand(cmd, RedirectOp.>, f)    => toProcess(cmd, prep) #> f
      case RedirectCommand(cmd, RedirectOp.>>, f)   => toProcess(cmd, prep) #>> f
      case RedirectCommand(cmd, RedirectOp.<, f)    => toProcess(cmd, prep) #< f
      case PipeCommand(cmd0, cmd1)                  => toProcess(cmd0, prep) #| toProcess(cmd1, prep)
      case SimpleCommand(cmd, args)                 => Process(prep(cmd +: args))
    }
}
