/*
 * SPDX-License-Identifier: CC0-1.0
 */

package io.github.mfederczuk.gradle.plugin.ktlint

import org.gradle.api.provider.Property

public interface KtlintPluginExtension {

	/**
	 * The version of ktlint to use.
	 * This string must be a valid [semantic version](https://semver.org/spec/v2.0.0.html).
	 *
	 * This property doesn't have a default value — a version must be explicitly set.
	 */
	public val version: Property<String>

	// region ktlint CLI flags

	// region --code-style=<codeStyle>

	/**
	 * Constant for the code style "`ktlint_official`".
	 *
	 * @see codeStyle
	 */
	@Suppress("PropertyName")
	public val KtlintOfficial: String get() = "ktlint_official"

	/**
	 * Constant for the code style "`intellij_idea`".
	 *
	 * @see codeStyle
	 */
	@Suppress("PropertyName")
	public val IntellijIdea: String get() = "intellij_idea"

	/**
	 * Constant for the code style "`android_studio`".
	 *
	 * @see codeStyle
	 */
	@Suppress("PropertyName")
	public val AndroidStudio: String get() = "android_studio"

	/**
	 * Defines the code style ("`ktlint_official`", "`intellij_idea`" or "`android_studio`") to be used for linting
	 * the code.
	 * It is advised to define the EditorConfig property `ktlint_code_style` instead of using this property.
	 *
	 * The default behavior — if no value is set — is dependent on the used ktlint version.
	 * Since version 1.0.0 the default code style is "`ktlint_official`",
	 * for versions below 1.0.0 it is "`intellij_idea`".
	 *
	 * If set, then the flag `--code-style=<codeStyle>` is added to the ktlint invocation.
	 *
	 * @see KtlintOfficial
	 * @see IntellijIdea
	 * @see AndroidStudio
	 */
	public val codeStyle: Property<String>

	// endregion

	/**
	 * The maximum number of errors to show.
	 * This value must be positive. (0 is excluded)
	 *
	 * The default behavior — if no value is set — is to show all errors.
	 *
	 * If set, then the flag `--limit=<limit>` is added to the ktlint invocation.
	 */
	public val limit: Property<Int>

	// region --experimental

	/**
	 * Whether to enable the experimental rules.
	 *
	 * The default value is `false`.
	 *
	 * If set to `true`, then the flag `--experimental` is added to the ktlint invocation.
	 *
	 * @see enableExperimentalRules
	 */
	public val experimentalRulesEnabled: Property<Boolean>

	/**
	 * Enables the experimental rules.
	 *
	 * Adds the flag `--experimental` to the ktlint invocation.
	 *
	 * This is a convenience function for setting the property [experimentalRulesEnabled] to `true`.
	 */
	public fun enableExperimentalRules() {
		this.experimentalRulesEnabled.set(true)
	}

	// endregion

	// endregion

	/**
	 * Whether to install the Git pre-commit hook before a build.
	 *
	 * This is implemented by making either the task "`preBuild`" or the task "`build`" dependent on
	 * the hook installation task.
	 * If the project that the plugin is applied to neither has a "`preBuild`" nor a "`build`" task, then
	 * the gradle configuration will fail.
	 *
	 * The default value is `false`.
	 */
	public val installGitPreCommitHookBeforeBuild: Property<Boolean>
}
