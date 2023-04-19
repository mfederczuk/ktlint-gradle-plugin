/*
 * Copyright (c) 2023 Michael Federczuk
 * SPDX-License-Identifier: MPL-2.0 AND Apache-2.0
 */

@file:Suppress("RemoveExplicitTypeArguments")

package io.github.mfederczuk.gradle.plugin.ktlint

import net.swiftzer.semver.SemVer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.register
import java.io.File

public class KtlintPlugin : Plugin<Project> {

	private companion object {
		const val EXTENSION_NAME: String = "ktlint"

		const val KTLINT_DEPENDENCY_NOTATION_WITHOUT_VERSION: String = "com.pinterest:ktlint"

		const val TASK_GROUP_NAME: String = "ktlint"
		const val GIT_PRE_COMMIT_HOOK_INSTALLATION_TASK_NAME: String = "installKtlintGitPreCommitHook"
	}

	override fun apply(project: Project) {
		val extension: KtlintPluginExtension = this.createExtension(extensionContainer = project.extensions)

		val ktlintClasspathJarFilesProvider: Provider<Iterable<File>> = extension.version
			.map<Iterable<File>> { versionString: String ->
				val requestedKtlintVersion: SemVer? = SemVer.parseOrNull(versionString)

				checkNotNull(requestedKtlintVersion) {
					"String \"$versionString\" is not not a valid semantic version.\n" +
						"Ensure that the version was correctly copied from https://github.com/pinterest/ktlint/releases"
				}

				this.resolveKtlintClasspathJarFilesFromVersion(project, requestedKtlintVersion)
			}

		val projectTypeProvider: Provider<ProjectType> = extension.android
			.map { isAndroidProject: Boolean ->
				when (isAndroidProject) {
					true -> ProjectType.ANDROID
					false -> ProjectType.OTHER
				}
			}

		this.registerGitPreCommitHookInstallationTask(
			project,
			ktlintClasspathJarFilesProvider,
			projectTypeProvider,
		)

		project.afterEvaluate {
			this@KtlintPlugin.setupAutomaticGitPreCommitHookInstallation(project = this@afterEvaluate)
		}
	}

	private fun createExtension(extensionContainer: ExtensionContainer): KtlintPluginExtension {
		val extension: KtlintPluginExtension = extensionContainer.create<KtlintPluginExtension>(name = EXTENSION_NAME)

		extension.installGitPreCommitHookBeforeBuild.convention(false)
		extension.android.convention(false)

		return extension
	}

	private fun resolveKtlintClasspathJarFilesFromVersion(project: Project, version: SemVer): Set<File> {
		val ktlintDependencyNotation = "$KTLINT_DEPENDENCY_NOTATION_WITHOUT_VERSION:$version"
		val ktlintDependency: Dependency = project.dependencies.create(ktlintDependencyNotation)

		val configuration: Configuration = project.configurations.detachedConfiguration(ktlintDependency)
		try {
			return configuration.resolve()
		} catch (_: ResolveException) {
			// don't add this exception as a cause; Gradle won't show our message but instead just the cause message

			val msg: String =
				"Could not resolve the dependency \"$ktlintDependencyNotation\".\n" +
					"Either the requested version ($version) does not exist or " +
					"Maven Central is missing from the dependency repositories.\n" +
					"If it's neither of those causes, then ...".internalErrorMsg
			error(msg)
		}
	}

	private fun registerGitPreCommitHookInstallationTask(
		project: Project,
		ktlintClasspathJarFilesProvider: Provider<Iterable<File>>,
		projectTypeProvider: Provider<ProjectType>,
	) {
		project.tasks.register<KtlintGitPreCommitHookInstallationTask>(GIT_PRE_COMMIT_HOOK_INSTALLATION_TASK_NAME) {
			this@register.group = TASK_GROUP_NAME
			this@register.description = "Installs the ktlint Git pre-commit hook"

			this@register.taskName.convention(GIT_PRE_COMMIT_HOOK_INSTALLATION_TASK_NAME)
			this@register.classpathJarFiles.convention(ktlintClasspathJarFilesProvider)
			this@register.projectType.convention(projectTypeProvider)
		}
	}

	private fun setupAutomaticGitPreCommitHookInstallation(project: Project) {
		val extension: KtlintPluginExtension? = project.extensions.findByType<KtlintPluginExtension>()
		checkNotNull(extension) {
			"Extension of type ${KtlintPluginExtension::class.java.name} not found in $project".internalErrorMsg
		}

		val installGitPreCommitHookBeforeBuild: Boolean = extension.installGitPreCommitHookBeforeBuild.get()
		if (!installGitPreCommitHookBeforeBuild) {
			return
		}

		val targetTask: Task? = project.tasks.findByName("preBuild")
			?: project.tasks.findByName("build")
		checkNotNull(targetTask) {
			"Tasks with name \"preBuild\" or \"build\" not found in $project.\n" +
				"If the plugin has been applied to a root project, either set " +
				"`$EXTENSION_NAME.${extension::installGitPreCommitHookBeforeBuild.name}` to `false`, or " +
				"apply the plugin to a non-root project"
		}

		val gitPreCommitHookInstallationTask: Task? =
			project.tasks.findByName(GIT_PRE_COMMIT_HOOK_INSTALLATION_TASK_NAME)
		checkNotNull(gitPreCommitHookInstallationTask) {
			"Task with name \"$GIT_PRE_COMMIT_HOOK_INSTALLATION_TASK_NAME\" not found in $project".internalErrorMsg
		}

		targetTask.dependsOn(gitPreCommitHookInstallationTask)
	}
}
