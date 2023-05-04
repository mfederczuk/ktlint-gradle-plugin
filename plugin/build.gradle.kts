// SPDX-License-Identifier: CC0-1.0

import java.net.URI

plugins {
	`java-gradle-plugin`
	`kotlin-dsl`

	id("com.gradle.plugin-publish") version "1.2.0"
}

repositories {
	mavenCentral()
}

group = "io.github.mfederczuk"
version = "0.1.0-indev04"

kotlin {
	explicitApi()
}

@Suppress("SpellCheckingInspection")
dependencies {
	implementation("net.swiftzer.semver:semver:1.2.0")
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
	website.set(pluginWebsiteUrl.toString())
	vcsUrl.set(pluginHttpsGitRepoUrl.toString())

	plugins.create(pluginName) {
		id = "${project.group}.$pluginName"
		implementationClass = "io.github.mfederczuk.gradle.plugin.ktlint.KtlintPlugin"

		displayName = pluginDisplayName
		description = pluginDescription
		tags.set(setOf("linting", "ktlint", "git-hooks"))
	}
}

publishing {
	publications {
		create<MavenPublication>(name = "pluginMaven") {
			groupId = project.group.toString()
			artifactId = "ktlint-gradle-plugin"
			version = project.version.toString()

			pom {
				name.set(pluginDisplayName)
				description.set(pluginDescription)
				url.set(pluginWebsiteUrl.toString())
				inceptionYear.set("2023")

				licenses {
					license {
						name.set("MPL-2.0")
						url.set("https://www.mozilla.org/media/MPL/2.0/index.txt")
						comments.set("Mozilla Public License 2.0")
						distribution.set("repo")
					}
					license {
						name.set("Apache-2.0")
						url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
						comments.set("The Apache License, Version 2.0")
						distribution.set("repo")
					}
				}

				developers {
					developer {
						id.set("mfederczuk")
						name.set("Michael Federczuk")
						email.set("federczuk.michael@protonmail.com")
						url.set("https://github.com/mfederczuk")
						timezone.set("Europe/Vienna")
					}
				}
				contributors {
				}

				issueManagement {
					system.set("GitHub Issues")
					url.set(pluginIssueManagementWebsiteUrl.toString())
				}

				scm {
					connection.set("scm:git:$pluginHttpsGitRepoUrl")
					developerConnection.set("scm:git:$pluginSshGitRepoUrl")
					tag.set("v${project.version}")
					url.set(pluginRepoWebsiteUrl.toString())
				}
			}
		}
	}
}

// endregion
