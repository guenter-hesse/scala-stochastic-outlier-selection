
name := "Scala Stochastic Outlier Selection"
version := "0.2.0"

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

lazy val core = (project in file("."))
  .settings(organization := "com.github.gnni")

pomIncludeRepository := { _ => false }

scalaVersion := "2.12.8"

libraryDependencies += "org.scalanlp" %% "breeze" % "0.13.2"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.4" % Test
