sbt-sh
======

Using the sbt-sh plugin you can invoke shell commands:

```
root> sh git status
[info] On branch wip/sequential
[info] Your branch is up-to-date with 'upstream/wip/sequential'.
[info] nothing to commit, working directory clean
```

Happiness and productivity ensues without the hassle of exiting sbt or opening multiple terminals.  

Setup
-----

To install sbt-sh, add it to your global sbt plugin list by creating `~/.sbt/0.13/plugins/sh.sbt`:

```scala
addSbtPlugin("com.eed3si9n" % "sbt-sh" % "0.1.0")
```

Usage
-----

The sbt-sh plugin introduces the `sh` task to sbt. This will execute the rest of the line as a shell command:

```
sbt-sh> sh ls
[info] LICENSE
[info] README.markdown
[info] build.sbt
[info] project
[info] src
[info] target
[success] Total time: 0 s, completed Jan 23, 2015 11:00:00 PM
```

`sh` is an input task that returns `Seq[String]` containing each line from the output.

```
sbt-sh> show sh ls
[info] LICENSE
[info] README.markdown
[info] build.sbt
[info] project
[info] src
[info] target
[info] List(LICENSE, README.markdown, build.sbt, project, src, target)
[success] Total time: 0 s, completed Jan 23, 2015 11:00:00 PM
```

Pipeline supported is limited to one level:

```
sbt-sh> sh ag case src | wc -l
[info]       34
[success] Total time: 0 s, completed Jan 23, 2015 11:00:00 PM
sbt-sh> sh find . -iname *.scala | xargs cat | wc -l
[error] ({.}/*:sh) java.lang.IllegalArgumentException: requirement failed: Piping to multiple processes is not supported.
[error] Total time: 0 s, completed Jan 23, 2015 11:00:00 PM
```

Redirect assumes rhs is a file:

```
sbt-sh> sh ls > ls.txt
[success] Total time: 0 s, completed Jan 23, 2015 11:00:00 PM
sbt-sh> sh cat < ls.txt
[info] LICENSE
[info] README.markdown
[info] build.sbt
[info] ls.txt
[info] project
[info] src
[info] target
[success] Total time: 0 s, completed Jan 23, 2015 11:00:00 PM
```

Logical operators work:

```
sbt-sh> sh cat nonexistent.txt || cat ls.txt | wc -l
[error] cat: nonexistent.txt: No such file or directory
[info]        7
[success] Total time: 0 s, completed Jan 23, 2015 11:00:00 PM
sbt-sh> sh cat nonexistent.txt && cat ls.txt | wc -l
[error] cat: nonexistent.txt: No such file or directory
[error] Nonzero exit code: 1
[error] ({.}/*:sh) Nonzero exit code: 1
[error] Total time: 0 s, completed Jan 23, 2015 11:00:00 PM
```

Background process works only at the top level:

```
sbt-sh> sh (subl . & ls | wc -l &)
[error] Expected '&&'
[error] sh (subl . & ls | wc -l &)
[error]             ^
sbt-sh> sh (subl .; ls | wc -l;) &
[success] Total time: 0 s, completed Jan 23, 2015 11:00:00 PM
       7
```

In Sublime Text pops up in the background, and it prints out 7 on shell.

Credits
-------

- On 2009-09-12, @harrah released [sbt 0.5.4][sbt054], which mentions "`sh` task for users with a unix-style shell." It uses `/bin/sh -c`. Current releases of sbt no longer supports `sh` or `exec`.
- On 2011-07-09, @steppenwells wrote the original [steppenwells/sbt-sbt][steppenwells/sbt-sbt], which adds `sh` task that simply forks commands as `args.mkString(" ") !` like `exec`.
- On 2015-01-24, @eed3si9n rewrote sbt-sbt, emulating POSIX shell-like features using sbt's parser combinator and `sbt.Process` API.

  [sbt054]: http://www.scala-sbt.org/0.7.7/docs/ChangeSummary_0_5_4.html
  [steppenwells/sbt-sbt]: https://github.com/steppenwells/sbt-sh
