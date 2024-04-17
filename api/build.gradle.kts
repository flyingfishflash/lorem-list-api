import com.adarshr.gradle.testlogger.theme.ThemeType

plugins {
  id("java")
  id("jacoco")
  id("com.adarshr.test-logger") version "4.0.0"
  id("com.diffplug.spotless")
  id("io.spring.dependency-management") version "1.1.4"
//  id("org.graalvm.buildtools.native") version "0.9.28"
  id("org.springframework.boot") version "3.2.3"
  id("org.springdoc.openapi-gradle-plugin") version "1.8.0"
  kotlin("jvm") version "1.9.22"
  kotlin("plugin.jpa") version "1.9.22"
  kotlin("plugin.serialization")
  kotlin("plugin.spring") version "1.9.22"
}

group = "net.flyingfishflash"
description = "lorem-list api"

val ciCommit: String by rootProject.extra
val ciPlatform: String by rootProject.extra
val ciPipelineId: String by rootProject.extra

val exposedVersion = "0.48.0"
val flywayVersion = "10.10.0"
val kotestVersion = "5.8.1"
val kotestExtensionsSpringVersion = "1.1.3"
val kotlinLoggingVersion = "6.0.9"
val kotlinxDateTimeVersion = "0.5.0"
val kotlinxSerializationJson = "1.6.3"
val postgresqlVersion = "42.7.3"
val springDocOpenApiStarterWebmvcUiVersion = "2.4.0"
val springmockkVersion = "4.0.2"

configurations { compileOnly { extendsFrom(configurations.annotationProcessor.get()) } }

dependencies {
//  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
//  https://github.com/oshai/kotlin-logging/releases
  implementation("io.github.oshai:kotlin-logging-jvm:$kotlinLoggingVersion")
  implementation("jakarta.validation:jakarta.validation-api")
  implementation("org.flywaydb:flyway-core:$flywayVersion")
  implementation("org.jetbrains.exposed:exposed-spring-boot-starter:$exposedVersion")
  implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
  implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposedVersion")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinxDateTimeVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationJson")
  implementation(
    "org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocOpenApiStarterWebmvcUiVersion",
  )
  implementation(
    "org.springdoc:springdoc-openapi-starter-common:$springDocOpenApiStarterWebmvcUiVersion",
  )
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
    // TODO: no matter what value I place here sonarqube key/project name remains as lorem-list
    property("sonar.projectKey", "lorem-list-api")
    property("sonar.projectName", "lorem-list-api")
    property("sonar.projectVersion", rootProject.version.toString())
    property(
      "sonar.coverage.exclusions",
      "src/main/kotlin/net/flyingfishflash/loremlist/LoremListApplication.kt," +
        "src/main/kotlin/net/flyingfishflash/loremlist/core/configuration/**," +
        "src/main/kotlin/net/flyingfishflash/loremlist/**/dto/*," +
        "src/main/kotlin/net/flyingfishflash/loremlist/*/*/*Configuration.kt," +
        "src/main/kotlin/net/flyingfishflash/loremlist/domain/*/data/*Repository.kt,",
    )
  }
}

springBoot {
  buildInfo {
    properties {
      artifact.set("lorem-list-api")
      name.set("lorem-list api")
      version.set(rootProject.version.toString())
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
    kotlinOptions {
      freeCompilerArgs += "-Xjsr305=strict"
      jvmTarget = "17"
    }
  }

  test {
    ignoreFailures = true
    useJUnitPlatform()
    finalizedBy("jacocoUnitTestReport")
    filter { excludeTestsMatching("net.flyingfishflash.loremlist.integration*") }
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
}

testlogger {
  showExceptions = false
  showSimpleNames = true
  theme = ThemeType.MOCHA
}
