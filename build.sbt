import org.scalajs.jsenv.selenium._
import xerial.sbt.Sonatype._
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import ReleaseTransformations._

ThisBuild / organization := "dev.guillaumebogard"
ThisBuild / organizationName := "Guillaume Bogard"
ThisBuild / startYear := Some(2021)
ThisBuild / licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt"))
ThisBuild / homepage := Some(url("https://(your project url)"))
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/gbogard/scalajs-idb"),
    "scm:git@github.com:gbogard/scalajs-idb.git"
  )
)
ThisBuild / developers += Developer(
  "gbogard",
  "Guillaume Bogard",
  "hey@guillaumebogard.dev",
  url("https://guillaumebogard.dev")
)
ThisBuild / scalaVersion := "3.0.1"
ThisBuild / scalacOptions ++= Seq("-Yexplicit-nulls")

lazy val testSettings = Seq(
  libraryDependencies ++= Seq(
    "com.lihaoyi" %%% "utest" % "0.7.10" % "test"
  ),
  testFrameworks += new TestFramework("utest.runner.Framework"),
  Test / jsEnv := new SeleniumJSEnv(
    new org.openqa.selenium.firefox.FirefoxOptions().setHeadless(true),
    SeleniumJSEnv.Config().withKeepAlive(false)
  )
)

lazy val publishSettings = Seq(
  sonatypeCredentialHost := "s01.oss.sonatype.org",
  versionScheme := Some("semver-spec"),
  publishTo := sonatypePublishToBundle.value,
  publishMavenStyle := true,
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    releaseStepCommandAndRemaining("publishSigned"),
    releaseStepCommand("sonatypeBundleRelease"),
    setNextVersion,
    commitNextVersion,
    pushChanges
  )
)

lazy val root = project
  .in(file("."))
  .aggregate(core, catsEffect, scalaJavaTime)
  .settings(
    publish / skip := true
  )

lazy val core = project
  .in(file("core"))
  .settings(
    name := "scalajs-idb-core",
    testSettings,
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core" % "2.6.1",
      "org.typelevel" %%% "cats-free" % "2.6.1"
    )
  )
  .enablePlugins(ScalaJSPlugin)

lazy val catsEffect = project
  .in(file("cats-effect"))
  .settings(
    name := "scalajs-idb-cats-effect",
    testSettings,
    publishSettings,
    libraryDependencies += "org.typelevel" %%% "cats-effect" % "3.2.9"
  )
  .dependsOn(core % "provided;compile->compile;test->test")
  .enablePlugins(ScalaJSPlugin)

lazy val scalaJavaTime = project
  .in(file("java-time"))
  .settings(
    name := "scalajs-idb-java-time",
    testSettings,
    publishSettings,
    libraryDependencies += "io.github.cquiroz" %%% "scala-java-time" % "2.3.0"
  )
  .dependsOn(core % "provided;compile->compile;test->test")
  .enablePlugins(ScalaJSPlugin)
