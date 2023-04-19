// SPDX-License-Identifier: CC0-1.0

plugins {
	kotlin("android") version "1.8.20"
	id("com.android.application") version "8.0.0"
	id("io.github.mfederczuk.ktlint") version "0.1.0-indev01"
}

kotlin {
	jvmToolchain(8)
}

ktlint {
	version.set("0.48.2")
}

android {
	namespace = "io.github.mfederczuk.gradle.plugin.ktlint.demo.app"
	compileSdk = 33

	defaultConfig {
		applicationId = "io.github.mfederczuk.gradle.plugin.ktlint.demo.app"

		minSdk = 24
		targetSdk = 33

		versionCode = 1
		versionName = "1.0"
	}

	buildTypes {
		release {
			isMinifyEnabled = false
			proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
		}
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_1_8
		targetCompatibility = JavaVersion.VERSION_1_8
	}

	kotlinOptions {
		jvmTarget = JavaVersion.VERSION_1_8.toString()
	}
}

repositories {
	google()
	mavenCentral()
}

dependencies {
	implementation("androidx.core:core-ktx:1.12.0-alpha03")

	// keep this at version < 1.10
	// starting with version 1.10, compileSdk > 33 is required
	implementation("com.google.android.material:material:1.9.0-rc01")
}
