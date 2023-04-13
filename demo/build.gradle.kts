// SPDX-License-Identifier: CC0-1.0

plugins {
	kotlin("jvm") version "1.8.20"
	application
	id("io.github.mfederczuk.ktlint") version "0.1.0-indev01"
}

repositories {
	mavenCentral()
}

ktlint {
	version.set("0.48.2")
	installGitPreCommitHookBeforeBuild.set(true)
}

application.mainClass.set("io.github.mfederczuk.gradle.plugin.ktlint.demo.Main")
