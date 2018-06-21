organization := "ch.unibas.cs.gravis"

name := "summerschool18"

version := "0.1"

scalaVersion := "2.12.6"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers += Resolver.jcenterRepo

resolvers += Resolver.bintrayRepo("unibas-gravis", "maven")

resolvers += "Statismo (public)" at "https://statismo.cs.unibas.ch/repository/public/"

resolvers += Opts.resolver.sonatypeSnapshots

libraryDependencies += "ch.unibas.cs.gravis" %% "scalismo-faces" % "0.9.1"

// uncomment if you need to click landmarks on faces
// libraryDependencies += "ch.unibas.cs.gravis" %% "landmarks-clicker" % "0.2.0"

libraryDependencies += "ch.unibas.cs.gravis" %% "scalismo-ui" % "0.12.0"
