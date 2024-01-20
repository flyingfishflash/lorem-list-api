plugins { id("com.mooltiverse.oss.nyx") version "2.5.2" }

rootProject.name = "lorem-list"

include("api")

// include("client")

System.setProperty("sonar.gradle.skipCompile", "true")

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories { mavenCentral() }
}
