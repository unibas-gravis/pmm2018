organization := "ch.unibas.cs.gravis"

name := "pmm2018"

version := "0.1"

scalaVersion := "2.12.6"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers += Resolver.jcenterRepo

resolvers += Resolver.bintrayRepo("unibas-gravis", "maven")

resolvers += Opts.resolver.sonatypeSnapshots

libraryDependencies += "ch.unibas.cs.gravis" %% "scalismo-ui" % "0.12.0"


