/*
 * Copyright (c) 2023 Michael Federczuk
 * SPDX-License-Identifier: MPL-2.0 AND Apache-2.0
 */

package io.github.mfederczuk.gradle.plugin.ktlint

import io.github.mfederczuk.gradle.plugin.ktlint.configuration.PluginConfiguration
import io.github.mfederczuk.gradle.plugin.ktlint.configuration.toConfiguration
import io.github.mfederczuk.gradle.plugin.ktlint.tasks.GitPreCommitHookPathRefreshTask
import io.github.mfederczuk.gradle.plugin.ktlint.tasks.KtlintGitPreCommitHookInstallationTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

public class KtlintPlugin : Plugin<Project> {

	private companion object {
		const val TASK_GROUP_NAME: String = "ktlint"
	}

	override fun apply(project: Project) {
		PluginExtensionUtils.createExtension(project)

		this.registerTasks(project)

		project.afterEvaluate {
			this@KtlintPlugin.setupAutomaticGitPreCommitHookInstallation(project = this@afterEvaluate)
		}
	}

	private fun registerTasks(project: Project) {
		GitPreCommitHookPathRefreshTask.registerIn(project, groupName = TASK_GROUP_NAME)

		KtlintGitPreCommitHookInstallationTask.registerIn(project, groupName = TASK_GROUP_NAME)
	}

	private fun setupAutomaticGitPreCommitHookInstallation(project: Project) {
		val extension: KtlintPluginExtension = PluginExtensionUtils.getExtension(project)

		val shouldInstallGitPreCommitHookBeforeBuild: Boolean = extension.toConfiguration(project.providers)
			.map(PluginConfiguration::shouldInstallGitPreCommitHookBeforeBuild)
			.get()

		if (!shouldInstallGitPreCommitHookBeforeBuild) {
			return
		}

		val targetTask: Task? = project.tasks.findByName("preBuild")
			?: project.tasks.findByName("build")
		checkNotNull(targetTask) {
			"Tasks with name \"preBuild\" or \"build\" not found in $project.\n" +
				"If the plugin has been applied to a root project, either set " +
				"`${PluginExtensionUtils.EXTENSION_NAME}.${extension::installGitPreCommitHookBeforeBuild.name}` " +
				"to `false`, or apply the plugin to a non-root project"
		}

		val gitPreCommitHookInstallationTask: Task = KtlintGitPreCommitHookInstallationTask.getFrom(project)

		targetTask.dependsOn(gitPreCommitHookInstallationTask)
	}
}
