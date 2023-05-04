<!--
  Copyright (c) 2023 Michael Federczuk
  SPDX-License-Identifier: CC-BY-SA-4.0
-->

# ktlint Gradle Plugin #

[version_shield]: https://img.shields.io/badge/version-0.1.0--indev04-informational.svg
[release_page]: https://github.com/mfederczuk/ktlint-gradle-plugin/releases/tag/v0.1.0-indev04 "Release v0.1.0-indev04"
[![version: 0.1.0-indev04][version_shield]][release_page]
[![Changelog](https://img.shields.io/badge/-Changelog-informational.svg)](CHANGELOG.md "Changelog")

## About ##

This is a simple [Gradle] plugin for installing a custom [ktlint] Git pre-commit hook script.

The advantages over using ktlint's built-in `installGitPreCommitHook` command are:

* The `ktlint` JAR is downloaded by the plugin from the Maven Central repository, which means that the JAR doesn't need
  to be pre-installed on the system

* The custom hook script will first stash away any unstaged changes before running `ktlint`, so that only the changes
  that will actually be committed will be inspected (the official `git-stash` command is *not* used for this)

* If committing via IntelliJ IDEA / Android Studio, then absolute paths are printed instead of relative ones.
  This results in those paths being clickable in the IDE

&#8203;

This plugin's custom hook script was greatly inspired by [JLLeitschuh/ktlint-gradle].  
The main advantages/fixes of this plugin's hook are:

* The script doesn't execute Gradle, it executes the `ktlint` JAR directly, which (depending on the project's size) can
  potentially shave off multiple seconds of the hook's execution time

* The script handles stashing binary files correctly

* The script supports the config `diff.noprefix = true`

&#8203;

> Note that the hook script requires at least Git version `2.35.0`.  
> Check your installed Git version with:
>
> ```sh
> git --version
> ```

[Gradle]: <https://gradle.org> "Gradle Build Tool"
[ktlint]: <https://github.com/pinterest/ktlint> "pinterest/ktlint: An anti-bikeshedding Kotlin linter with built-in formatter"
[JLLeitschuh/ktlint-gradle]: <https://github.com/JLLeitschuh/ktlint-gradle> "JLLeitschuh/ktlint-gradle: A ktlint gradle plugin"

## Configuration ##

The minimum supported ktlint version is `0.48.0`.

<!-- markdownlint-disable no-inline-html -->

<details open>
<summary>Kotlin script & plugins DSL</summary>

```kotlin
plugins {
	id("io.github.mfederczuk.ktlint") version "0.1.0-indev04"
}

repositories {
	// make sure that Maven Central is configured as an artifact repository
	mavenCentral()
}

ktlint {
	version.set("0.49.0") // set the version of ktlint

	// the following configuration properties are optional
	codeStyle.set(AndroidStudio) // add the --code-style=android_studio flag to ktlint
	limit.set(5) // add the --limit=5 flag to ktlint
	installGitPreCommitHookBeforeBuild.set(true) // automatically installs the hook every time before a build is started
}
```

</details>

<details>
<summary>Kotlin Script & legacy plugin application</summary>

```kotlin
buildscript {
	repositories {
		maven("https://plugins.gradle.org/m2/")
	}
	dependencies {
		classpath("io.github.mfederczuk:ktlint-gradle-plugin:0.1.0-indev04")
	}
}

apply(plugin = "io.github.mfederczuk.ktlint")

repositories {
	// make sure that Maven Central is configured as an artifact repository
	mavenCentral()
}

ktlint {
	version.set("0.49.0") // set the version of ktlint

	// the following configuration properties are optional
	codeStyle.set(AndroidStudio) // add the --code-style=android_studio flag to ktlint
	limit.set(5) // add the --limit=5 flag to ktlint
	installGitPreCommitHookBeforeBuild.set(true) // automatically installs the hook every time before a build is started
}
```

</details>

<details>
<summary>Groovy & plugins DSL</summary>

```groovy
plugins {
	id 'io.github.mfederczuk.ktlint' version '0.1.0-indev04'
}

repositories {
	// make sure that Maven Central is configured as an artifact repository
	mavenCentral()
}

ktlint {
	version = '0.49.0' // set the version of ktlint

	// the following configuration properties are optional
	codeStyle = 'android_studio' // add the --code-style=android_studio flag to ktlint
	limit = 5 // add the --limit=5 flag to ktlint
	installGitPreCommitHookBeforeBuild = true // automatically installs the hook every time before a build is started
}
```

</details>

<details>
<summary>Groovy & legacy plugin application</summary>

```groovy
buildscript {
	repositories {
		maven { url 'https://plugins.gradle.org/m2/' }
	}
	dependencies {
		classpath 'io.github.mfederczuk:ktlint-gradle-plugin:0.1.0-indev04'
	}
}

apply plugin: 'io.github.mfederczuk.ktlint'

repositories {
	// make sure that Maven Central is configured as an artifact repository
	mavenCentral()
}

ktlint {
	version = '0.49.0' // set the version of ktlint

	// the following configuration properties are optional
	codeStyle = 'android_studio' // add the --code-style=android_studio flag to ktlint
	limit = 5 // add the --limit=5 flag to ktlint
	installGitPreCommitHookBeforeBuild = true // automatically installs the hook every time before a build is started
}
```

</details>

<!-- markdownlint-enable no-inline-html -->

## To-Do List ##

* [x] Configuration property to add the `--android` flag to `ktlint`
* [x] Configuration property to add the `--code-style=<codeStyle>` flag to `ktlint`
* [ ] Automatically detecting Android projects and adding the `--android` / `--code-style=android_studio` flag if that
      is the case
* [ ] Configuration property to add the `--disabled_rules=<disabledRules>` flag to `ktlint`
* [x] Configuration property to add the `--limit=<limit>` flag to `ktlint`
* [ ] Configuration property to add the `--reporter=<reporterConfiguration>` flag to `ktlint`
* [ ] Configuration property to add the `--ruleset=<rulesetJarPaths>` flag to `ktlint`
* [ ] Configuration property to add the `--editorconfig=<editorConfigPath>` flag to `ktlint`
* [x] Configuration property to add the `--experimental` flag to `ktlint`
* [ ] Configuration property to add the `--baseline` flag to `ktlint`
* [ ] Configuration property to add the `--log-level=<minLogLevel>` flag to `ktlint`
* [ ] Gradle task to run ktlint manually
* [ ] Configuration property to run ktlint over all Kotlin files, not just the ones that are staged

## Contributing ##

Read through the [Contribution Guidelines](CONTRIBUTING.md) if you want to contribute to this project.

## License ##

**ktlint GitHub Gradle Plugin** is licensed under both the [**Mozilla Public License 2.0**](LICENSES/MPL-2.0.txt) AND
the [**Apache License 2.0**](LICENSES/Apache-2.0.txt).  
For more information about copying and licensing, see the [`COPYING.txt`](COPYING.txt) file.

_(note that this project is **not** affiliated with ktlint or Pinterest Inc.)_
