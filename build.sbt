import org.scalajs.jsenv.selenium._

val scala3Version = "3.0.1"
val scala213Version = "2.13.6"
val supportedVersions = List(scala3Version, scala213Version)

ThisBuild / version := "0.1.0"
ThisBuild / organization := "dev.guillaumebogard"
ThisBuild / scalaVersion := scala3Version
ThisBuild / scalacOptions ++= Seq("-Yexplicit-nulls")

lazy val testSettings = Seq(
  libraryDependencies += "com.lihaoyi" %%% "utest" % "0.7.10" % "test",
  testFrameworks += new TestFramework("utest.runner.Framework"),
  Test / jsEnv := new SeleniumJSEnv(
    new org.openqa.selenium.firefox.FirefoxOptions(),
    SeleniumJSEnv.Config()
  )
)

lazy val root = project
  .in(file("."))
  .aggregate(core)
  .settings(
    crossScalaVersions := Nil,
    publish / skip := true
  )

lazy val core = project
  .in(file("core"))
  .settings(
    name := "scala3-idb-core",
    crossScalaVersions := supportedVersions,
    testSettings
  )
  .enablePlugins(ScalaJSPlugin)
