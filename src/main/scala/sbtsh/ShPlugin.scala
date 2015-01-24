package sbtsh

import sbt._
import Keys._

object ShPlugin extends AutoPlugin {
  override def requires = plugins.CorePlugin
  override def trigger = allRequirements

  object autoImport extends ShKeys {}

  override def buildSettings = baseShSettings
  import autoImport._

  lazy val baseShSettings: Seq[sbt.Def.Setting[_]] = Seq(
    sh := {
      val log = (streams in sh).value.log
      val cmd = ShCommand.asyncCommand.parsed
      Sh.run(cmd, shPreprocess.value, log)
    },
    shPreprocess := Sh.defaultPreprocess,
    aggregate in sh := false
  )
}
