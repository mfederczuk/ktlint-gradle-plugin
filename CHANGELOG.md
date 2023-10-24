<!--
  Copyright (c) 2023 Michael Federczuk
  SPDX-License-Identifier: CC-BY-SA-4.0
-->

<!-- markdownlint-disable no-duplicate-heading -->

# Changelog #

All notable changes to this project will be documented in this file.
The format is based on [**Keep a Changelog v1.0.0**](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [**Semantic Versioning v2.0.0**](https://semver.org/spec/v2.0.0.html).

## Unreleased ##

### Added ###

* The task `installKtlintGitPreCommitHook` is now cacheable
* The option `--color` is added to the ktlint invocation (can be disabled with [`NO_COLOR`](https://no-color.org/))

### Changed ###

* The minimum supported version of ktlint is now **`0.50.0`**

### Removed ###

* The `android` configuration property (which was deprecated in [version 0.1.0-indev04](#v010-indev04---2023-05-04))
  has been removed

## [v0.1.0-indev06] - 2023-09-11 ##

[v0.1.0-indev06]: <https://github.com/mfederczuk/ktlint-github-gradle-plugin/releases/tag/v0.1.0-indev06>

### Added ###

* Support for ktlint version `1.0.0`+

## [v0.1.0-indev05] - 2023-06-14 ##

[v0.1.0-indev05]: <https://github.com/mfederczuk/ktlint-github-gradle-plugin/releases/tag/v0.1.0-indev05>

### Added ###

* New `experimentalRulesEnabled` configuration property for adding the `--experimental` flag to the ktlint invocation
  * Convenience function `enableExperimentalRules()`

### Fixed ###

* The Java compatibility is now explicitly set to `11` (before it was implicitly `17`)

## [v0.1.0-indev04] - 2023-05-04 ##

[v0.1.0-indev04]: <https://github.com/mfederczuk/ktlint-github-gradle-plugin/releases/tag/v0.1.0-indev04>

### Added ###

* The hook will now also print the configured ktlint version
* New `limit` configuration property for adding the `--limit=<limit>` flag to the ktlint invocation
* New `codeStyle` configuration property for adding the `--code-stye=<codeStyle>` flag to the ktlint invocation

### Deprecated ###

* The `android` configuration property to mirror the deprecation of the `--android` flag

## [v0.1.0-indev03] - 2023-04-19 ##

[v0.1.0-indev03]: <https://github.com/mfederczuk/ktlint-github-gradle-plugin/releases/tag/v0.1.0-indev03>

### Added ###

* New `android` boolean configuration property ([`ad2a302`](https://github.com/mfederczuk/ktlint-gradle-plugin/commit/ad2a302d6f56993ae766cfe61a4414159f48bf4c))

## [v0.1.0-indev02] - 2023-04-19 ##

[v0.1.0-indev02]: <https://github.com/mfederczuk/ktlint-github-gradle-plugin/releases/tag/v0.1.0-indev02>

### Fixed ###

* Fixed the `installKtlintGitPreCommitHook` task ([`4db95eb`](https://github.com/mfederczuk/ktlint-gradle-plugin/commit/4db95ebbb7ab24837b4a82b8a0cd4374fd0ce98d))

## [v0.1.0-indev01] - 2023-04-14 ##

[v0.1.0-indev01]: <https://github.com/mfederczuk/ktlint-github-gradle-plugin/releases/tag/v0.1.0-indev01>

Initial Release
