import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
	java
	id("org.springframework.boot") version "3.5.13"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.sisibibi"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// 메인 애플리케이션 라이브러리
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-web")

	// Lombok 설정
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	// 개발 도구
	developmentOnly("org.springframework.boot:spring-boot-devtools")

	// 테스트 통합 라이브러리 (data-jpa, security, validation, webmvc 4개의 starter를 하나로 대체)
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	runtimeOnly("com.mysql:mysql-connector-j")
	runtimeOnly("com.h2database:h2")

	// 테스트용 Lombok 및 런처 설정
	testCompileOnly("org.projectlombok:lombok")
	testAnnotationProcessor("org.projectlombok:lombok")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	implementation("org.springframework.boot:spring-boot-starter-actuator")
	runtimeOnly("io.micrometer:micrometer-registry-prometheus")
}

tasks.withType<Test> {
	useJUnitPlatform()

	testLogging {
		events = setOf(
			TestLogEvent.SKIPPED,
			TestLogEvent.FAILED
		)
		exceptionFormat = TestExceptionFormat.FULL
		showExceptions = true
		showCauses = true
		showStackTraces = true
	}

	val concurrencySummaryFile = layout.buildDirectory.file("test-results/concurrency-summary.txt")

	systemProperty(
		"concurrency.summary.file",
		concurrencySummaryFile.get().asFile.absolutePath
	)

	doFirst {
		val summaryFile = concurrencySummaryFile.get().asFile
		if (summaryFile.exists()) {
			summaryFile.delete()
		}
		summaryFile.parentFile.mkdirs()
	}

	addTestListener(object : TestListener {
		override fun beforeSuite(suite: TestDescriptor) = Unit

		override fun beforeTest(testDescriptor: TestDescriptor) = Unit

		override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {
			logger.lifecycle(
				"[TEST ${result.resultType}] ${testDescriptor.className}.${testDescriptor.name}"
			)
		}

		override fun afterSuite(suite: TestDescriptor, result: TestResult) {
			if (suite.parent == null) {
				val summaryFile = concurrencySummaryFile.get().asFile
				if (summaryFile.exists()) {
					logger.lifecycle("[CONCURRENCY TEST SUMMARY]")
					summaryFile.readLines()
						.filter { it.isNotBlank() }
						.forEach { logger.lifecycle(it) }
				}

				logger.lifecycle(
					"[TEST SUMMARY] total=${result.testCount}, passed=${result.successfulTestCount}, " +
						"failed=${result.failedTestCount}, skipped=${result.skippedTestCount}"
				)
			}
		}
	})
}

tasks.named<Test>("test") {
	exclude("**/*IT.class")
}

tasks.register<Test>("rdbLimitMysqlTest") {
	description = "Runs opt-in RDB limit tests against local MySQL."
	group = "verification"

	testClassesDirs = sourceSets.test.get().output.classesDirs
	classpath = sourceSets.test.get().runtimeClasspath

	useJUnitPlatform()
	include("**/*IT.class")
	systemProperty("rdb.limit.mysql.enabled", "true")

	shouldRunAfter(tasks.named("test"))
}
