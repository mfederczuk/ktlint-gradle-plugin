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
	 * A version must be explicitly set, otherwise attempting to run tasks will fail.
	 */
	public val version: Property<String>

	/**
	 * Whether or not this project is an android project.
	 *
	 * If set to `true`, prior to ktlint version `0.49.0`, then the flag `--android` will be added to the `ktlint`
	 * invocation. For ktlint version `0.49.0` and onwards, the option `--code-style=android_studio` is added instead.
	 *
	 * The default value is `false`.
	 */
	@Deprecated(
		message = "Since ktlint version 0.49.0, the flag '--android' is deprecated in favor of " +
			"'--code-style=android_studio', though it is recommended to instead set " +
			"the EditorConfig property 'ktlint_code_style=android_studio' all together. " +
			"If the configured version is before 0.49.0, then this warning can be ignored",
	)
	public val android: Property<Boolean>

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
