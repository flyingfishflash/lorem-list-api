import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("java")
  id("org.springframework.boot") version "3.2.1"
  id("io.spring.dependency-management") version "1.1.4"
  id("org.hibernate.orm") version "6.4.1.Final"
  id("org.graalvm.buildtools.native") version "0.9.28"
  id("com.diffplug.spotless")
  id("jacoco")
  kotlin("jvm") version "1.9.21"
  kotlin("plugin.spring") version "1.9.21"
  kotlin("plugin.jpa") version "1.9.21"
}

group = "net.flyingfishflash"

description = "Listx API"

val ciCommit: String by rootProject.extra
val ciPlatform: String by rootProject.extra
val ciPipelineId: String by rootProject.extra

java { sourceCompatibility = JavaVersion.VERSION_17 }

configurations { compileOnly { extendsFrom(configurations.annotationProcessor.get()) } }

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("org.flywaydb:flyway-core")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  runtimeOnly("com.h2database:h2")
  runtimeOnly("org.postgresql:postgresql")
  annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.security:spring-security-test")
}

jacoco { toolVersion = "0.8.11" }

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
            "src/main/java/net/flyingfishflash/ledger/*/*/*Configuration.java")
  }
}

tasks.withType<KotlinCompile> {
  kotlinOptions {
    freeCompilerArgs += "-Xjsr305=strict"
    jvmTarget = "17"
  }
}

tasks.withType<Test> { useJUnitPlatform() }

hibernate { enhancement { enableAssociationManagement.set(true) } }

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
  kotlinGradle { ktfmt() }
  kotlin { ktfmt() }

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
