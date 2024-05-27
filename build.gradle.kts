import com.adarshr.gradle.testlogger.theme.ThemeType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

group = "net.flyingfishflash"
description = "Simple List Management API"

repositories {
  mavenCentral()
}

plugins {
  id("java")
  id("jacoco")
  id("com.adarshr.test-logger") version "4.0.0"
  id("com.diffplug.spotless") version "6.25.0"
  id("com.github.ben-manes.versions") version "0.51.0"
  id("io.spring.dependency-management") version "1.1.5"
  id("org.sonarqube") version "5.0.0.4638"
  id("org.springframework.boot") version "3.3.0"
  id("org.springdoc.openapi-gradle-plugin") version "1.8.0"
  kotlin("jvm") version "2.0.0"
  kotlin("plugin.jpa") version "2.0.0"
  kotlin("plugin.serialization") version "2.0.0"
  kotlin("plugin.spring") version "2.0.0"
//  id("org.graalvm.buildtools.native") version "0.9.28"
}

val ciCommit by extra { ciCommit() }
val ciPlatform by extra { ciPlatform() }
val ciPipelineId by extra { ciPipelineId() }

val exposedVersion = "0.50.1"
val flywayVersion = "10.13.0"
val jakartaValidationApiVersion = "3.1.0"
val kotestVersion = "5.9.0"
val kotestExtensionsSpringVersion = "1.1.3"
val kotlinLoggingVersion = "6.0.9"
val kotlinxDateTimeVersion = "0.6.0"
val kotlinxSerializationJson = "1.6.3"
val postgresqlVersion = "42.7.3"
val springDocOpenApiStarterWebmvcUiVersion = "2.5.0"
val springmockkVersion = "4.0.2"

configurations { compileOnly { extendsFrom(configurations.annotationProcessor.get()) } }

dependencies {
//  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
//  https://github.com/oshai/kotlin-logging/releases
  implementation("io.github.oshai:kotlin-logging-jvm:$kotlinLoggingVersion")
  implementation("jakarta.validation:jakarta.validation-api:$jakartaValidationApiVersion")
//  https://github.com/flyway/flyway/releases
  implementation("org.flywaydb:flyway-core:$flywayVersion")
  implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")
//  https://github.com/JetBrains/Exposed/releases
  implementation("org.jetbrains.exposed:exposed-spring-boot-starter:$exposedVersion")
  implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
  implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposedVersion")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinxDateTimeVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationJson")
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
  //  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.boot:spring-boot-starter-web") {
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-json")
  }
  runtimeOnly("com.h2database:h2")
  runtimeOnly("org.postgresql:postgresql:$postgresqlVersion")
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
  testImplementation("com.ninja-squad:springmockk:$springmockkVersion")
  testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
  testImplementation("io.kotest:kotest-assertions-json:$kotestVersion")
  testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
  testImplementation("io.kotest.extensions:kotest-extensions-spring:$kotestExtensionsSpringVersion")
  //  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.springframework.boot:spring-boot-starter-test") {
    exclude(group = "org.assertj", module = "assertj-core")
    exclude(group = "org.hamcrest", module = "hamcrest")
    exclude(group = "org.mockito", module = "mockito-core")
    exclude(group = "org.mockito", module = "mockito-junit-jupiter")
  }
}

jacoco { toolVersion = "0.8.11" }

java { sourceCompatibility = JavaVersion.VERSION_17 }

sonarqube {
  properties {
    property("sonar.projectVersion", project.version.toString())
    property(
      "sonar.cpd.exclusions",
      "src/main/kotlin/net/flyingfishflash/loremlist/domain/lrmitem/LrmItemController.kt," +
        "src/main/kotlin/net/flyingfishflash/loremlist/domain/lrmlist/LrmListController.kt,",
    )
    property(
      "sonar.coverage.exclusions",
      "src/main/kotlin/net/flyingfishflash/loremlist/LoremListApplication.kt," +
        "src/main/kotlin/net/flyingfishflash/loremlist/**/data/*," +
        "src/main/kotlin/net/flyingfishflash/loremlist/*/*/*Configuration.kt," +
        "src/main/kotlin/net/flyingfishflash/loremlist/core/configuration/**," +
        "src/main/kotlin/net/flyingfishflash/loremlist/core/response/structure/ApiMessage.kt," +
        "src/main/kotlin/net/flyingfishflash/loremlist/domain/EntityModel.kt," +
        "src/main/kotlin/net/flyingfishflash/loremlist/domain/*/*Repository.kt," +
        "src/main/kotlin/net/flyingfishflash/loremlist/domain/common/Association.kt," +
        "src/main/kotlin/net/flyingfishflash/loremlist/domain/common/data/ItemToListAssociationUpdateRequest.kt," +
        "src/main/kotlin/net/flyingfishflash/loremlist/domain/lrmitem/LrmItem.kt," +
        "src/main/kotlin/net/flyingfishflash/loremlist/domain/lrmitem/data/LrmItemRequest.kt," +
        "src/main/kotlin/net/flyingfishflash/loremlist/domain/lrmitem/data/LrmItemDeleteResponse.kt," +
        "src/main/kotlin/net/flyingfishflash/loremlist/domain/lrmlist/LrmList.kt," +
        "src/main/kotlin/net/flyingfishflash/loremlist/domain/lrmlist/data/LrmListRequest.kt," +
        "src/main/kotlin/net/flyingfishflash/loremlist/domain/lrmlist/data/LrmListSuccinct.kt,",
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
  kotlinGradle { ktlint() }

  kotlin {
    ktlint()
      .editorConfigOverride(
        mapOf(
          "indent_size" to 2,
          "ktlint_code_style" to "intellij_idea",
          "max_line_length" to 140,
        ),
      )
  }

  json {
    target("*.json")
    jackson()
  }

  format("misc") {
    target("*.md", "*.xml", ".gitignore")
    trimTrailingWhitespace()
    indentWithSpaces()
    endWithNewline()
  }
}

tasks {
  compileKotlin {
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_17)
      freeCompilerArgs.add("-Xjsr305=strict")
    }
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
    sourceDirectories.setFrom(files(project.sourceSets.main.get().allSource.srcDirs))
    classDirectories.setFrom(
      files(
        project.sourceSets.main.get().output.asFileTree.filter { f: File ->
          !(
            f.path.contains("/kotlin/main/net/flyingfishflash/loremlist") &&
              (
                f.name.equals("Application") ||
                  f.name.contains("ApplicationConfiguration") ||
                  f.name.contains("Configuration") ||
                  f.path.contains("dto/") ||
                  f.path.contains("configuration/")
              )
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
  }
  return ciPlatform
}

fun ciPipelineId(): String {
  var ciPipelineId = "0"
  if (ciPlatform() == "drone") {
    ciPipelineId = System.getenv("DRONE_BUILD_NUMBER")
  } else if (ciPlatform() == "gitlab") {
    ciPipelineId = System.getenv("CI_PIPELINE_ID")
  }
  return ciPipelineId
}

fun ciCommit(): String {
  var ciCommit = "No Commit SHA"
  if (ciPlatform() == "drone") {
    ciCommit = System.getenv("DRONE_COMMIT_SHA").slice(0..7)
  } else if (ciPlatform() == "gitlab") {
    ciCommit = System.getenv("CI_COMMIT_SHORT_SHA")
  }
  return ciCommit
}
