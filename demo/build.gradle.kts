// SPDX-License-Identifier: CC0-1.0

plugins {
	kotlin("jvm") version "1.8.20"
	application
	id("io.github.mfederczuk.ktlint") version "0.1.0-indev04"
}

repositories {
	mavenCentral()
}

ktlint {
	version.set("0.49.1")
	// codeStyle.set(AndroidStudio)
	// limit.set(5)
	installGitPreCommitHookBeforeBuild.set(true)
}

application.mainClass.set("io.github.mfederczuk.gradle.plugin.ktlint.demo.Main")
