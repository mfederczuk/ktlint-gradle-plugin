/*
 * Copyright (c) 2023 Michael Federczuk
 * SPDX-License-Identifier: MPL-2.0 AND Apache-2.0
 */

package io.github.mfederczuk.gradle.plugin.ktlint.configuration

import io.github.mfederczuk.gradle.plugin.ktlint.KtlintPluginExtension
import io.github.mfederczuk.gradle.plugin.ktlint.utils.isValid
import io.github.mfederczuk.gradle.plugin.ktlint.utils.zip
import net.swiftzer.semver.SemVer
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import javax.annotation.CheckReturnValue

private val KTLINT_MIN_SUPPORTED_VERSION: SemVer = SemVer(0, 50, 0)

@CheckReturnValue
internal fun KtlintPluginExtension.toConfiguration(providerFactory: ProviderFactory): Provider<PluginConfiguration> {
	return providerFactory
		.zip(
			mapVersionStringProvider(providerFactory, this.version),
			mapCodeStyleProvider(this.codeStyle),
			mapLimitProvider(this.limit),
			this.experimental,
			this.installGitPreCommitHookBeforeBuild,
		) {
				ktlintVersion: SemVer,
				codeStyle: CodeStyle,
				errorLimit: ErrorLimit,
				experimentalRulesEnabled: Boolean,
				shouldInstallGitPreCommitHookBeforeBuild: Boolean,
			->

			PluginConfiguration(
				ktlintVersion,
				codeStyle,
				errorLimit,
				experimentalRulesEnabled,
				shouldInstallGitPreCommitHookBeforeBuild,
			)
		}
}

@CheckReturnValue
private fun mapVersionStringProvider(
	providerFactory: ProviderFactory,
	versionStringProvider: Provider<String>,
): Provider<SemVer> {
	return versionStringProvider
		.map(::mapVersionString)
		.orElse(
			providerFactory.provider {
				error("No ktlint version was configured")
			},
		)
}

@CheckReturnValue
private fun mapVersionString(versionString: String): SemVer {
	val version: SemVer? = SemVer.parseOrNull(versionString)

	if (version == null) {
		val versionStringWithoutLeadingV: String = versionString.removePrefix(prefix = "v")
		if (SemVer.isValid(versionStringWithoutLeadingV)) {
			val msg: String =
				"String \"$versionString\" is not a valid semantic version.\n" +
					"Remove the leading 'v' character and " +
					"use \"$versionStringWithoutLeadingV\" instead"
			error(msg)
		}

		val msg: String = "String \"$versionString\" is not not a valid semantic version.\n" +
			"Ensure that the version was correctly copied from https://github.com/pinterest/ktlint/releases"
		error(msg)
	}

	check(version >= KTLINT_MIN_SUPPORTED_VERSION) {
		"Configured ktlint version ($version) is lower than " +
			"the minimum supported ktlint version. ($KTLINT_MIN_SUPPORTED_VERSION)"
	}

	return version
}

@CheckReturnValue
private fun mapCodeStyleProvider(codeStyleProvider: Provider<String>): Provider<CodeStyle> {
	return codeStyleProvider
		.map<CodeStyle>(CodeStyle::Specific)
		.orElse(CodeStyle.Default)
}

@CheckReturnValue
private fun mapLimitProvider(limitProvider: Provider<Int>): Provider<ErrorLimit> {
	return limitProvider
		.map<ErrorLimit> { limit: Int ->
			check(limit >= 0) {
				"Error limit must be set to a positive integer"
			}

			ErrorLimit.Max(limit.toUInt())
		}
		.orElse(ErrorLimit.None)
}
