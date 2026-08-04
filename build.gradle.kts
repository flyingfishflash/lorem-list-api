import com.adarshr.gradle.testlogger.theme.ThemeType

group = "net.flyingfishflash"
description = "Simple List Management API"

repositories {
  mavenCentral()
}

plugins {
  id("java")
  id("jacoco")
  id("com.adarshr.test-logger") version "4.0.0"
  id("com.diffplug.spotless") version "7.0.4"
  id("com.github.ben-manes.versions") version "0.52.0"
  id("io.spring.dependency-management") version "1.1.7"
  id("org.sonarqube") version "6.2.0.5505"
  id("org.springframework.boot") version "3.5.0"
  id("org.springdoc.openapi-gradle-plugin") version "1.9.0"
//  id("org.graalvm.buildtools.native") version "0.9.28"
}

val ciCommit by extra { ciCommit() }
val ciPlatform by extra { ciPlatform() }
val ciPipelineId by extra { ciPipelineId() }

val flywayVersion = "11.9.1"
val jakartaValidationApiVersion = "3.1.1"
val kotlinxDateTimeVersion = "0.6.2"
val postgresqlVersion = "42.7.6"
val springDocOpenApiStarterWebmvcUiVersion = "2.8.8"

configurations { compileOnly { extendsFrom(configurations.annotationProcessor.get()) } }

dependencies {
  implementation("jakarta.validation:jakarta.validation-api:$jakartaValidationApiVersion")
//  https://github.com/flyway/flyway/releases
  implementation("org.flywaydb:flyway-core:$flywayVersion")
  implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")
  // kotlinx-datetime backs kotlinx.datetime.Instant, still used throughout the domain model.
  implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinxDateTimeVersion")
//  https://github.com/springdoc/springdoc-openapi/releases
  implementation(
    "org.springdoc:springdoc-openapi-starter-webmvc-ui:" +
      springDocOpenApiStarterWebmvcUiVersion,
  )
  implementation(
    "org.springdoc:springdoc-openapi-starter-common:" +
      springDocOpenApiStarterWebmvcUiVersion,
  )
//  https://github.com/spring-projects/spring-boot/releases
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-jdbc")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.boot:spring-boot-starter-web")
  runtimeOnly("com.h2database:h2")
  runtimeOnly("org.postgresql:postgresql:$postgresqlVersion")
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
}

jacoco { toolVersion = "0.8.15" }

java {
  sourceCompatibility = JavaVersion.toVersion(25)
  toolchain {
    languageVersion = JavaLanguageVersion.of(26)
  }
}

sonarqube {
  properties {
    property("sonar.projectVersion", project.version.toString())
    property(
      "sonar.cpd.exclusions",
      "src/main/java/net/flyingfishflash/loremlist/api/LrmItemController.java," +
        "src/main/java/net/flyingfishflash/loremlist/api/LrmListController.java,",
    )
    property(
      "sonar.coverage.exclusions",
      "src/main/java/net/flyingfishflash/loremlist/LoremListApplication.java," +
        "src/main/java/net/flyingfishflash/loremlist/**/data/**," +
        "src/main/java/net/flyingfishflash/loremlist/*/*/*Configuration.java," +
        "src/main/java/net/flyingfishflash/loremlist/core/configuration/**," +
        "src/main/java/net/flyingfishflash/loremlist/core/response/structure/ApiMessage.java," +
        "src/main/java/net/flyingfishflash/loremlist/domain/*/*Repository.java," +
        "src/main/java/net/flyingfishflash/loremlist/domain/lrmitem/LrmItem.java," +
        "src/main/java/net/flyingfishflash/loremlist/domain/lrmlist/LrmList.java," +
        "src/main/java/net/flyingfishflash/loremlist/persistence/**,",
    )
  }
}

springBoot {
  buildInfo {
    properties {
      artifact.set("lorem-list-api")
      name.set("lorem-list api")
      version.set(project.version.toString())
      additional.set(
        mapOf("ciPlatform" to ciPlatform, "ciPipelineId" to ciPipelineId, "commit" to ciCommit),
      )
    }
  }
}

spotless {
  java {
    googleJavaFormat()
    importOrder()
    removeUnusedImports()
  }

  json {
    target("*.json")
    jackson()
  }

  format("misc") {
    target("*.md", "*.xml", ".gitignore")
    trimTrailingWhitespace()
    leadingTabsToSpaces()
    endWithNewline()
  }
}

tasks {
  compileJava {
    options.release.set(25)
  }

  compileTestJava {
    options.release.set(25)
  }

  register<Sync>("explodeBootJar") {
    dependsOn(bootJar)
    from(project.zipTree(bootJar.get().archiveFile))
    into("${layout.buildDirectory.get()}/boot_jar_exploded")
  }

  register<Copy>("copyBuildInfo") {
    mustRunAfter("explodeBootJar")
    from(layout.buildDirectory.file("boot_jar_exploded/META-INF/build-info.properties"))
    into(layout.buildDirectory.dir("boot_jar_exploded/BOOT-INF/classes/META-INF/"))
  }

  test {
    ignoreFailures = false
    useJUnitPlatform()
    finalizedBy("jacocoUnitTestReport")
    filter { excludeTestsMatching("net.flyingfishflash.loremlist.integration*") }
  }

  register<Test>("integrationTests") {
    ignoreFailures = true
    findProperty("spring.profiles.active")?.let { systemProperty("spring.profiles.active", it) }
    findProperty("spring.datasource.url")?.let { systemProperty("spring.datasource.url", it) }
    findProperty("spring.datasource.username")?.let { systemProperty("spring.datasource.username", it) }
    findProperty("spring.datasource.password")?.let { systemProperty("spring.datasource.password", it) }
    findProperty("spring.datasource.platform")?.let { systemProperty("spring.datasource.platform", it) }
    useJUnitPlatform {
      filter { excludeTestsMatching("net.flyingfishflash.loremlist.unit*") }
    }
  }

  register<JacocoReport>("jacocoUnitTestReport") {
    mustRunAfter(test)
    executionData(fileTree(project.layout.buildDirectory).include("jacoco/test.exec"))
    sourceDirectories.setFrom(
      files(
        project.sourceSets.main
          .get()
          .allSource.srcDirs,
      ),
    )
    classDirectories.setFrom(
      files(
        project.sourceSets.main.get().output.asFileTree.filter { f: File ->
          !(
            f.name.equals("Application") ||
              f.name.contains("ApplicationConfiguration") ||
              f.name.contains("Configuration") ||
              f.path.contains("dto/") ||
              f.path.contains("configuration/")
          )
        },
      ),
    )
    reports {
      html.required.set(true)
      xml.required.set(true)
    }
  }

  register("writeVersionToFile") {
    doLast { File(".version").writeText(project.version.toString()) }
  }

  register("writeVersionToTagsFile") {
    doLast { File(".tags").writeText(project.version.toString()) }
  }
}

testlogger {
  showExceptions = false
  showSimpleNames = true
  showStandardStreams = true
  showExceptions = true
  showStackTraces = true
  showFullStackTraces = true
  theme = ThemeType.MOCHA
}

fun ciPlatform(): String {
  var ciPlatform = "Non-CI Build"
  if (System.getenv("CI") == "true") {
    if (System.getenv("DRONE") == "true") {
      ciPlatform = "drone"
    } else if (System.getenv("GITLAB_CI") == "true") {
      ciPlatform = "gitlab"
    }
  } else if (System.getenv("CI") == "woodpecker") {
    ciPlatform = System.getenv("CI")
  }
  return ciPlatform
}

fun ciPipelineId(): String {
  var ciPipelineId = "0"
  if (ciPlatform() == "drone") {
    ciPipelineId = System.getenv("DRONE_BUILD_NUMBER")
  } else if (ciPlatform() == "gitlab") {
    ciPipelineId = System.getenv("CI_PIPELINE_ID")
  } else if (ciPlatform() == "woodpecker") {
    ciPipelineId = System.getenv("CI_PIPELINE_NUMBER")
  }
  return ciPipelineId
}

fun ciCommit(): String {
  var ciCommit = "No Commit SHA"
  if (ciPlatform() == "drone") {
    ciCommit = System.getenv("DRONE_COMMIT_SHA").slice(0..7)
  } else if (ciPlatform() == "gitlab") {
    ciCommit = System.getenv("CI_COMMIT_SHORT_SHA")
  } else if (ciPlatform() == "woodpecker") {
    ciCommit = System.getenv("CI_COMMIT_SHA").slice(0..7)
  }
  return ciCommit
}
