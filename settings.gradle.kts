// SPDX-License-Identifier: CC0-1.0

rootProject.name = "ktlint Gradle Plugin"

includeBuild("plugin")
includeBuild("demo")

val ideaPlatformPrefix: String? = System.getProperty("idea.platform.prefix")
if ((ideaPlatformPrefix == null) || (ideaPlatformPrefix == "AndroidStudio")) {
	includeBuild("demo/app") {
		name = ":demo:app"
	}
}
