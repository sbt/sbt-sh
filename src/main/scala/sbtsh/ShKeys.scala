package sbtsh

import sbt._
import Keys._

trait ShKeys {
  lazy val sh = inputKey[Seq[String]]("Runs command from shell.")
  lazy val shPreprocess = settingKey[Seq[String] => Seq[String]]("Preprocess the shell command passed to sh.")
}
