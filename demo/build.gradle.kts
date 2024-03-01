import io.github.mfederczuk.gradle.plugin.ktlint.tasks.KtlintFormattingTask

// SPDX-License-Identifier: CC0-1.0

plugins {
	kotlin("jvm") version "1.9.22"
	application
	id("io.github.mfederczuk.ktlint") version "0.1.0-indev08"
}

repositories {
	mavenCentral()
}

ktlint {
	version = "1.0.1"
	// codeStyle = AndroidStudio
	// limit = 5
	// enableExperimental()
	installGitPreCommitHookBeforeBuild = true
}

tasks.register<KtlintFormattingTask>("format") {
	inputDir = project.layout.projectDirectory.dir("src/main/java")
}

application.mainClass = "io.github.mfederczuk.gradle.plugin.ktlint.demo.Main"
