import AssemblyKeys._ // put this at the top of the file

assemblySettings

mainClass in assembly := Some("se.sics.caracaldb.driver.Main")

mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
  {
    case PathList("com", "google", "common", xs @ _*)         => MergeStrategy.first
    case PathList("com", "typesafe", "config", xs @ _*)         => MergeStrategy.first
    case PathList("org", "apache", "commons", xs @ _*)         => MergeStrategy.first
    case PathList("org", "codehaus", "jackson", xs @ _*)		=> MergeStrategy.first
    case PathList("javassist", xs @ _*)				=> MergeStrategy.first
    case "log4j.properties"							=> MergeStrategy.filterDistinctLines
    case x => old(x)
  }
}