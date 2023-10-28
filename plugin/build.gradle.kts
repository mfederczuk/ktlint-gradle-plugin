// SPDX-License-Identifier: CC0-1.0

import java.net.URI

plugins {
	`java-gradle-plugin`
	`kotlin-dsl`

	id("com.gradle.plugin-publish") version "1.2.1"
}

repositories {
	mavenCentral()
}

group = "io.github.mfederczuk"
version = "0.1.0-indev08"

val javaCompatibilityVersion: JavaVersion = JavaVersion.VERSION_11

java {
	sourceCompatibility = javaCompatibilityVersion
	targetCompatibility = javaCompatibilityVersion
	toolchain.languageVersion.set(JavaLanguageVersion.of(javaCompatibilityVersion.majorVersion))
}

kotlin {
	explicitApi()
	jvmToolchain(javaCompatibilityVersion.majorVersion.toInt())
}

@Suppress("SpellCheckingInspection")
dependencies {
	implementation("net.swiftzer.semver:semver:1.3.0")
}

// region publishing

val pluginName: String = "ktlint"
val pluginDisplayName: String = "ktlint Gradle Plugin"
val pluginDescription: String =
	"""
	ktlint Git-hook installation plugin
	""".trimIndent()

val pluginWebsiteUrl: URI = URI("https://github.com/mfederczuk/ktlint-gradle-plugin#readme")
val pluginRepoWebsiteUrl: URI = URI("https://github.com/mfederczuk/ktlint-gradle-plugin")
val pluginHttpsGitRepoUrl: URI = URI("https://github.com/mfederczuk/ktlint-gradle-plugin.git")
val pluginSshGitRepoUrl: URI = URI("ssh://git@github.com/mfederczuk/ktlint-gradle-plugin.git")
val pluginIssueManagementWebsiteUrl: URI = URI("https://github.com/mfederczuk/ktlint-gradle-plugin/issues")

gradlePlugin {
	website = pluginWebsiteUrl.toString()
	vcsUrl = pluginHttpsGitRepoUrl.toString()

	plugins.create(pluginName) {
		id = "${project.group}.$pluginName"
		implementationClass = "io.github.mfederczuk.gradle.plugin.ktlint.KtlintPlugin"

		displayName = pluginDisplayName
		description = pluginDescription
		tags = setOf("linting", "ktlint", "git-hooks")
	}
}

publishing {
	publications {
		create<MavenPublication>(name = "pluginMaven") {
			groupId = project.group.toString()
			artifactId = "ktlint-gradle-plugin"
			version = project.version.toString()

			pom {
				name = pluginDisplayName
				description = pluginDescription
				url = pluginWebsiteUrl.toString()
				inceptionYear = "2023"

				licenses {
					license {
						name = "MPL-2.0"
						url = "https://www.mozilla.org/media/MPL/2.0/index.txt"
						comments = "Mozilla Public License 2.0"
						distribution = "repo"
					}
					license {
						name = "Apache-2.0"
						url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
						comments = "The Apache License, Version 2.0"
						distribution = "repo"
					}
				}

				developers {
					developer {
						id = "mfederczuk"
						name = "Michael Federczuk"
						email = "federczuk.michael@protonmail.com"
						url = "https://github.com/mfederczuk"
						timezone = "Europe/Vienna"
					}
				}
				contributors {
				}

				issueManagement {
					system = "GitHub Issues"
					url = pluginIssueManagementWebsiteUrl.toString()
				}

				scm {
					connection = "scm:git:$pluginHttpsGitRepoUrl"
					developerConnection = "scm:git:$pluginSshGitRepoUrl"
					tag = "v${project.version}"
					url = pluginRepoWebsiteUrl.toString()
				}
			}
		}
	}
}

// endregion
