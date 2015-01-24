val root = (project in file(".")).
  settings(
    sbtPlugin := true,
    organization := "com.eed3si9n",
    name := "sbt-sh",
    version := "0.1.0-SNAPSHOT",
    description := "sbt plugin to invoke shell commands",
    licenses := Seq("MIT License" -> url("https://github.com/sbt/sbt-sh/blob/master/LICENSE"))
  ).
  settings(
    publishMavenStyle := false,
    publishTo := {
      if (isSnapshot.value) Some(Resolver.sbtPluginRepo("snapshots"))
      else Some(Resolver.sbtPluginRepo("releases"))
    },
    credentials += Credentials(Path.userHome / ".ivy2" / ".sbtcredentials")
  ).
  settings(ScriptedPlugin.scriptedSettings: _*).
  settings(
    scriptedLaunchOpts := { scriptedLaunchOpts.value ++
      Seq("-Xmx1024M", "-XX:MaxPermSize=256M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false
  )
