name := "heatmap4d"

version := "0.1"

scalaVersion := "2.11.1"

scalaSource in Compile := baseDirectory.value

libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
  "org.scala-lang" % "scala-reflect" % "2.11.1"
)

