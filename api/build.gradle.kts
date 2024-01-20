import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("java")
  id("jacoco")
  id("com.diffplug.spotless")
  id("io.spring.dependency-management") version "1.1.4"
//  id("org.graalvm.buildtools.native") version "0.9.28"
  id("org.hibernate.orm") version "6.4.2.Final"
  id("org.springframework.boot") version "3.2.2"
  kotlin("jvm") version "1.9.22"
  kotlin("plugin.jpa") version "1.9.22"
  kotlin("plugin.serialization") version "1.9.22"
  kotlin("plugin.spring") version "1.9.22"
}

group = "net.flyingfishflash"
description = "lorem-list api"

val ciCommit: String by rootProject.extra
val ciPlatform: String by rootProject.extra
val ciPipelineId: String by rootProject.extra

val kotestVersion = "5.8.0"
val kotestExtensionsSpringVersion = "1.1.3"
val kotlinxDateTimeVersion = "0.5.0"
val kotlinxSerializationJson = "1.6.2"
val springDocOpenApiStarterWebmvcUiVersion = "2.3.0"
val springmockkVersion = "4.0.2"

java { sourceCompatibility = JavaVersion.VERSION_17 }

configurations { compileOnly { extendsFrom(configurations.annotationProcessor.get()) } }

dependencies {
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("jakarta.validation:jakarta.validation-api")
  implementation("org.flywaydb:flyway-core")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinxDateTimeVersion")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationJson")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocOpenApiStarterWebmvcUiVersion")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  //  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.boot:spring-boot-starter-web")
  runtimeOnly("com.h2database:h2")
  runtimeOnly("org.postgresql:postgresql")
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

allOpen {
  annotations("jakarta.persistence.Entity", "jakarta.persistence.Embeddable", "jakarta.persistence.MappedSuperclass")
}

sonarqube {
  properties {
    property("sonar.projectKey", rootProject.name)
    property("sonar.projectVersion", rootProject.version.toString())
    property(
      "sonar.coverage.exclusions",
      "src/main/java/net/flyingfishflash/ledger/core/CustomCommandLineRunner.java," +
        "src/main/java/net/flyingfishflash/ledger/core/configuration/**," +
        "src/main/java/net/flyingfishflash/ledger/core/multitenancy/TenantService.java," +
        "src/main/java/net/flyingfishflash/ledger/**/dto/*," +
        "src/main/java/net/flyingfishflash/ledger/*/*/*Configuration.java",
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

tasks.withType<KotlinCompile> {
  kotlinOptions {
    freeCompilerArgs += "-Xjsr305=strict"
    jvmTarget = "17"
  }
}

tasks.withType<Test> {
  useJUnitPlatform()
  testLogging { events("passed", "skipped", "failed") }
}

hibernate { enhancement { enableAssociationManagement.set(true) } }

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
  kotlinGradle { ktlint() }
  kotlin { ktlint() }

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
