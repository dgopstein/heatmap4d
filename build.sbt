name := "heatmap4d"

version := "0.1"

scalaVersion := "2.9.1"

scalaSource in Compile := baseDirectory.value

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "1.6.1" % "test"
)
