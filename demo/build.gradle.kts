// SPDX-License-Identifier: CC0-1.0

plugins {
	kotlin("jvm") version "1.9.10"
	application
	id("io.github.mfederczuk.ktlint") version "0.1.0-indev07"
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

application.mainClass = "io.github.mfederczuk.gradle.plugin.ktlint.demo.Main"
