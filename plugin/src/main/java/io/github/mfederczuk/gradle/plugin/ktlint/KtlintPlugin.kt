/*
 * Copyright (c) 2023 Michael Federczuk
 * SPDX-License-Identifier: MPL-2.0 AND Apache-2.0
 */

package io.github.mfederczuk.gradle.plugin.ktlint

import io.github.mfederczuk.gradle.plugin.ktlint.configuration.CodeStyle
import io.github.mfederczuk.gradle.plugin.ktlint.configuration.ErrorLimit
import io.github.mfederczuk.gradle.plugin.ktlint.configuration.PluginConfiguration
import io.github.mfederczuk.gradle.plugin.ktlint.configuration.toConfiguration
import io.github.mfederczuk.gradle.plugin.ktlint.tasks.GitPreCommitHookPathRefreshTask
import io.github.mfederczuk.gradle.plugin.ktlint.tasks.KtlintGitPreCommitHookInstallationTask
import io.github.mfederczuk.gradle.plugin.ktlint.utils.getCurrentWorkingDirectoryPath
import io.github.mfederczuk.gradle.plugin.ktlint.utils.internalErrorMsg
import net.swiftzer.semver.SemVer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.register
import java.io.File
import java.nio.file.Path
import java.time.LocalDate
import javax.annotation.CheckReturnValue

public class KtlintPlugin : Plugin<Project> {

	private companion object {
		const val EXTENSION_NAME: String = "ktlint"

		const val TASK_GROUP_NAME: String = "ktlint"
		const val KTLINT_GIT_PRE_COMMIT_HOOK_INSTALLATION_TASK_NAME: String = "installKtlintGitPreCommitHook"
	}

	override fun apply(project: Project) {
		val configurationProvider: Provider<PluginConfiguration> = this
			.createConfiguration(
				extensionContainer = project.extensions,
				providerFactory = project.providers,
			)

		val ktlintClasspathJarFilesProvider: Provider<Iterable<File>> = configurationProvider
			.map { configuration: PluginConfiguration ->
				resolveKtlintClasspathJarFilesFromVersion(
					configuration.ktlintVersion,
					dependencyHandler = project.dependencies,
					configurationContainer = project.configurations,
				)
			}

		val gitPreCommitHookPathRefreshTaskProvider: TaskProvider<GitPreCommitHookPathRefreshTask> =
			this.registerGitPreCommitHookInfoRefreshTask(project)

		this.registerGitPreCommitHookInstallationTask(
			project,
			ktlintClasspathJarFilesProvider,
			codeStyleProvider = configurationProvider.map(PluginConfiguration::codeStyle),
			errorLimitProvider = configurationProvider.map(PluginConfiguration::errorLimit),
			experimentalRulesEnabledProvider = configurationProvider.map(PluginConfiguration::experimentalRulesEnabled),
			ktlintVersionProvider = configurationProvider.map(PluginConfiguration::ktlintVersion),
			gitPreCommitHookPathRefreshTaskProvider,
		)

		project.afterEvaluate {
			this@KtlintPlugin.afterEvaluate(project = this@afterEvaluate)
		}
	}

	@CheckReturnValue
	private fun createConfiguration(
		extensionContainer: ExtensionContainer,
		providerFactory: ProviderFactory,
	): Provider<PluginConfiguration> {
		return this.createExtension(extensionContainer)
			.toConfiguration(providerFactory)
	}

	@CheckReturnValue
	private fun createExtension(extensionContainer: ExtensionContainer): KtlintPluginExtension {
		val extension: KtlintPluginExtension = extensionContainer.create<KtlintPluginExtension>(name = EXTENSION_NAME)

		extension.experimental.convention(false)
		extension.installGitPreCommitHookBeforeBuild.convention(false)

		return extension
	}

	@CheckReturnValue
	private fun registerGitPreCommitHookInfoRefreshTask(project: Project): TaskProvider<GitPreCommitHookPathRefreshTask> {
		return project.tasks.register<GitPreCommitHookPathRefreshTask>("refreshGitPreCommitHookPath") {
			this@register.group = TASK_GROUP_NAME
			this@register.description = "(internal) Saves the path of the Git pre-commit hook file"

			this@register.gitDirEnvironmentVariableValue.set(project.provider { System.getenv("GIT_DIR").orEmpty() })
			this@register.workingDirectoryPath.set(project.provider { getCurrentWorkingDirectoryPath().toString() })
			this@register.currentDate.set(project.provider { LocalDate.now().toString() })

			val hookPathOutputFileProvider: Provider<RegularFile> = project.layout.buildDirectory
				.dir("git")
				.map { dir: Directory ->
					dir.file("preCommitPath.txt")
				}
			this@register.hookPathOutputFile.set(hookPathOutputFileProvider)
		}
	}

	private fun registerGitPreCommitHookInstallationTask(
		project: Project,
		ktlintClasspathJarFilesProvider: Provider<Iterable<File>>,
		codeStyleProvider: Provider<CodeStyle>,
		errorLimitProvider: Provider<ErrorLimit>,
		experimentalRulesEnabledProvider: Provider<Boolean>,
		ktlintVersionProvider: Provider<SemVer>,
		gitPreCommitHookPathRefreshTaskProvider: TaskProvider<GitPreCommitHookPathRefreshTask>,
	): TaskProvider<KtlintGitPreCommitHookInstallationTask> {
		@Suppress("ktlint:standard:max-line-length", "ktlint:standard:argument-list-wrapping", "LongLine")
		return project.tasks.register<KtlintGitPreCommitHookInstallationTask>(KTLINT_GIT_PRE_COMMIT_HOOK_INSTALLATION_TASK_NAME) {
			this@register.group = TASK_GROUP_NAME
			this@register.description = "Installs the ktlint Git pre-commit hook"

			this@register.dependsOn(gitPreCommitHookPathRefreshTaskProvider)

			this@register.ktlintClasspathJarFiles.set(ktlintClasspathJarFilesProvider)
			this@register.codeStyle.set(codeStyleProvider)
			this@register.errorLimit.set(errorLimitProvider)
			this@register.experimentalRulesEnabled.set(experimentalRulesEnabledProvider)
			this@register.ktlintVersion.set(ktlintVersionProvider.map(SemVer::toString))

			val gitPreCommitHookFileProvider: Provider<RegularFile> = gitPreCommitHookPathRefreshTaskProvider
				.flatMap { gitPreCommitHookPathRefreshTask: GitPreCommitHookPathRefreshTask ->
					gitPreCommitHookPathRefreshTask.getHookFile(project.providers)
				}
				.map(Path::toFile)
				.let(project.layout::file)
			this@register.gitPreCommitHookFile.set(gitPreCommitHookFileProvider)
		}
	}

	// region afterEvaluate

	private fun afterEvaluate(project: Project) {
		val extension: KtlintPluginExtension? = project.extensions.findByType<KtlintPluginExtension>()
		checkNotNull(extension) {
			"Extension of type ${KtlintPluginExtension::class.java.name} not found in $project".internalErrorMsg
		}

		this.setupAutomaticGitPreCommitHookInstallation(project, extension)
	}

	private fun setupAutomaticGitPreCommitHookInstallation(
		project: Project,
		extension: KtlintPluginExtension,
	) {
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
			project.tasks.findByName(KTLINT_GIT_PRE_COMMIT_HOOK_INSTALLATION_TASK_NAME)
		checkNotNull(gitPreCommitHookInstallationTask) {
			"Task with name \"$KTLINT_GIT_PRE_COMMIT_HOOK_INSTALLATION_TASK_NAME\" not found in $project".internalErrorMsg
		}

		targetTask.dependsOn(gitPreCommitHookInstallationTask)
	}

	// endregion
}
