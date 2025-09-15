plugins {
	val kotlinVersion = "1.9.25"
	id("org.springframework.boot") version "3.5.5"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("jvm") version kotlinVersion
	kotlin("plugin.spring") version kotlinVersion
	kotlin("plugin.jpa") version kotlinVersion
}

group = "com.shipal"
version = "0.0.1-SNAPSHOT"
description = "Demo project for Spring Boot"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// Kotlin Coroutines
	implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.8.1"))
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

	// -- Spring / Web / Validation
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-data-redis")
	developmentOnly("org.springframework.boot:spring-boot-devtools")

	// -- Persistence / MyBatis
	implementation("org.mybatis.spring.boot:mybatis-spring-boot-starter:3.0.3")
	runtimeOnly("com.mysql:mysql-connector-j")

	// -- Jsckson / Kotlin
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	// -- OpenAPI
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

	// -- JWT
	implementation("io.jsonwebtoken:jjwt-api:0.13.0")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
}

noArg{
	annotation("jakarta.persistence.Entity")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
