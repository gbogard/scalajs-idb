import org.scalajs.jsenv.selenium._

ThisBuild / version := "0.1.0"
ThisBuild / organization := "dev.guillaumebogard"
ThisBuild / organizationName := "Guillaume Bogard"
ThisBuild / startYear := Some(2021) 
ThisBuild / licenses += ("Apache-2.0", new URL("https://www.apache.org/licenses/LICENSE-2.0.txt"))
ThisBuild / scalaVersion := "3.0.1"
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
    testSettings
  )
  .enablePlugins(ScalaJSPlugin)
