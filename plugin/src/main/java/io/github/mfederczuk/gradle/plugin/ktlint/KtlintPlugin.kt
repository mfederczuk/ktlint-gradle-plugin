/*
 * Copyright (c) 2023 Michael Federczuk
 * SPDX-License-Identifier: MPL-2.0 AND Apache-2.0
 */

@file:Suppress("RemoveExplicitTypeArguments")

package io.github.mfederczuk.gradle.plugin.ktlint

import io.github.mfederczuk.gradle.plugin.ktlint.models.CodeStyle
import io.github.mfederczuk.gradle.plugin.ktlint.models.ErrorLimit
import io.github.mfederczuk.gradle.plugin.ktlint.models.PluginConfiguration
import io.github.mfederczuk.gradle.plugin.ktlint.models.toConfiguration
import io.github.mfederczuk.gradle.plugin.ktlint.tasks.GitPreCommitHookPathRefreshTask
import io.github.mfederczuk.gradle.plugin.ktlint.tasks.KtlintGitPreCommitHookInstallationTask
import io.github.mfederczuk.gradle.plugin.ktlint.utils.getCurrentWorkingDirectoryPath
import io.github.mfederczuk.gradle.plugin.ktlint.utils.internalErrorMsg
import net.swiftzer.semver.SemVer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ResolveException
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

		val KTLINT_NEW_MAVEN_COORDS_VERSION: SemVer = SemVer(1, 0, 0)

		const val KTLINT_DEPENDENCY_NOTATION_WITHOUT_VERSION_OLD: String = "com.pinterest:ktlint"
		const val KTLINT_DEPENDENCY_NOTATION_WITHOUT_VERSION_NEW: String = "com.pinterest.ktlint:ktlint-cli"

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
			.map<Iterable<File>> { configuration: PluginConfiguration ->
				this.resolveKtlintClasspathJarFilesFromVersion(project, configuration.ktlintVersion)
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

		extension.installGitPreCommitHookBeforeBuild.convention(false)
		extension.experimentalRulesEnabled.convention(false)

		return extension
	}

	@CheckReturnValue
	private fun resolveKtlintClasspathJarFilesFromVersion(
		project: Project,
		ktlintVersion: SemVer,
	): Set<File> {
		val ktlintDependencyNotationWithoutVersion: String =
			if (ktlintVersion >= KTLINT_NEW_MAVEN_COORDS_VERSION) {
				KTLINT_DEPENDENCY_NOTATION_WITHOUT_VERSION_NEW
			} else {
				KTLINT_DEPENDENCY_NOTATION_WITHOUT_VERSION_OLD
			}
		val ktlintDependencyNotation = "$ktlintDependencyNotationWithoutVersion:$ktlintVersion"
		val ktlintDependency: Dependency = project.dependencies.create(ktlintDependencyNotation)

		val configuration: Configuration = project.configurations.detachedConfiguration(ktlintDependency)
		try {
			return configuration.resolve()
		} catch (_: ResolveException) {
			// don't add this exception as a cause; Gradle won't show our message but instead just the cause message

			val msg: String =
				"Could not resolve the dependency \"$ktlintDependencyNotation\".\n" +
					"Either the requested version ($ktlintVersion) does not exist or " +
					"Maven Central is missing from the dependency repositories.\n" +
					"If it's neither of those causes, then ...".internalErrorMsg
			error(msg)
		}
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
			this@register.taskName.set(KTLINT_GIT_PRE_COMMIT_HOOK_INSTALLATION_TASK_NAME)
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
