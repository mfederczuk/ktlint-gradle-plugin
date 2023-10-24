/*
 * SPDX-License-Identifier: CC0-1.0
 */

package io.github.mfederczuk.gradle.plugin.ktlint

import org.gradle.api.provider.Property

public interface KtlintPluginExtension {

	/**
	 * The version of ktlint to use.
	 *
	 * This property doesn't have a default value.
	 * A version must be explicitly set, otherwise configuration will fail.
	 */
	public val version: Property<String>

	// region ktlint CLI flags

	// region --code-style=<codeStyle>

	/**
	 * Code style "`ktlint_official`".
	 *
	 * @see codeStyle
	 */
	@Suppress("PropertyName")
	public val KtlintOfficial: String get() = "ktlint_official"

	/**
	 * Code style "`intellij_idea`".
	 *
	 * @see codeStyle
	 */
	@Suppress("PropertyName")
	public val IntellijIdea: String get() = "intellij_idea"

	/**
	 * Code style "`android_studio`".
	 *
	 * @see codeStyle
	 */
	@Suppress("PropertyName")
	public val AndroidStudio: String get() = "android_studio"

	/**
	 *
	 * Adds the flag `--code-style=<codeStyle>` to the ktlint invocation.
	 *
	 * The default is no explicit code style / whatever ktlint uses as the default.
	 *
	 * @see KtlintOfficial
	 * @see IntellijIdea
	 * @see AndroidStudio
	 */
	public val codeStyle: Property<String>

	// endregion

	/**
	 * The maximum number of errors to show.
	 *
	 * If set, then the flag `--limit=<limit>` will be added to the `ktlint` invocation.
	 *
	 * The default is no limit.
	 */
	public val limit: Property<Int>

	// region --experimental

	/**
	 * Whether or not to enable experimental rules.
	 *
	 * If set to `true`, then the flag `--experimental` will be added to the `ktlint` invocation.
	 *
	 * The default value is `false`.
	 *
	 * @see enableExperimentalRules
	 */
	public val experimentalRulesEnabled: Property<Boolean>

	/**
	 * Enable experimental rules.
	 *
	 * Adds the flag `--experimental` to the `ktlint` invocation.
	 *
	 * This is a convenience function for
	 *
	 * ```
	 * experimentalRulesEnabled.set(true)
	 * ```
	 *
	 * @see experimentalRulesEnabled
	 */
	public fun enableExperimentalRules() {
		this.experimentalRulesEnabled.set(true)
	}

	// endregion

	// endregion

	/**
	 * Whether or not to install the Git pre-commit hook before a build.
	 *
	 * This works by making either the "`preBuild`" or the "`build`" task dependent on the hook installation task.
	 * If the project that the plugin is applied to neither has a "`preBuild`" nor a "`build`" task, configuration will
	 * fail.
	 *
	 * The default value is `false`.
	 */
	public val installGitPreCommitHookBeforeBuild: Property<Boolean>
}
