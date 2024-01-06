plugins { id("com.mooltiverse.oss.nyx") version "2.5.1" }

rootProject.name = "listx"

include("api")

// include("client")

System.setProperty("sonar.gradle.skipCompile", "true")

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories { mavenCentral() }
}
