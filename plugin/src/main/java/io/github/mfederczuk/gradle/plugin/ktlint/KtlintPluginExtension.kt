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
	 * Whether or not to install the Git pre-commit hook before a build.
	 *
	 * This works by making either the "`pre-build`" or the "`build`" task dependent on the hook installation task.
	 * If the project that the plugin is applied to neither has a "`pre-build`" nor a "`build`" task, configuration will
	 * fail.
	 *
	 * The default value is `false`.
	 */
	public val installGitPreCommitHookBeforeBuild: Property<Boolean>
}
