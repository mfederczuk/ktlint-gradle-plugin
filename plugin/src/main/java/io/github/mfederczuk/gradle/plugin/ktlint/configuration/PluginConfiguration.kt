/*
 * SPDX-License-Identifier: CC0-1.0
 */

package io.github.mfederczuk.gradle.plugin.ktlint.configuration

import net.swiftzer.semver.SemVer

internal data class PluginConfiguration(
	val ktlintVersion: SemVer,
	val codeStyle: CodeStyle,
	val errorLimit: ErrorLimit,
	val experimentalRulesEnabled: Boolean,
	val shouldInstallGitPreCommitHookBeforeBuild: Boolean,
)
