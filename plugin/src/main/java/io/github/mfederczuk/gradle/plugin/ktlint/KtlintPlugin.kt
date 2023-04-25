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
import javax.annotation.CheckReturnValue

public class KtlintPlugin : Plugin<Project> {

	private companion object {
		const val EXTENSION_NAME: String = "ktlint"

		// flag --patterns-from-stdin (which is required for the hook) was introduced in 0.48.0
		val KTLINT_MIN_SUPPORTED_VERSION: SemVer = SemVer(0, 48, 0)

		const val KTLINT_DEPENDENCY_NOTATION_WITHOUT_VERSION: String = "com.pinterest:ktlint"

		const val TASK_GROUP_NAME: String = "ktlint"
		const val KTLINT_GIT_PRE_COMMIT_HOOK_INSTALLATION_TASK_NAME: String = "installKtlintGitPreCommitHook"
	}

	override fun apply(project: Project) {
		val extension: KtlintPluginExtension = this.createExtension(extensionContainer = project.extensions)

		val ktlintVersionProvider: Provider<SemVer> = extension.version
			.map<SemVer> { versionString: String ->
				val requestedKtlintVersion: SemVer? = SemVer.parseOrNull(versionString)

				if ((requestedKtlintVersion == null) && SemVer.isValid(versionString.removePrefix(prefix = "v"))) {
					val msg: String =
						"String \"$versionString\" is not a valid semantic version.\n" +
							"Remove the leading 'v' character and " +
							"use \"${versionString.removePrefix(prefix = "v")}\" instead"
					error(msg)
				}

				checkNotNull(requestedKtlintVersion) {
					"String \"$versionString\" is not not a valid semantic version.\n" +
						"Ensure that the version was correctly copied from https://github.com/pinterest/ktlint/releases"
				}

				check(requestedKtlintVersion >= KTLINT_MIN_SUPPORTED_VERSION) {
					"Configured ktlint version ($requestedKtlintVersion) is lower than " +
						"minimum supported ktlint version $KTLINT_MIN_SUPPORTED_VERSION"
				}

				requestedKtlintVersion
			}

		val ktlintClasspathJarFilesProvider: Provider<Iterable<File>> = ktlintVersionProvider
			.map<Iterable<File>> { version: SemVer ->
				this.resolveKtlintClasspathJarFilesFromVersion(project, version)
			}

		val projectTypeProvider: Provider<ProjectType> = extension.android
			.map { isAndroidProject: Boolean ->
				when (isAndroidProject) {
					true -> ProjectType.ANDROID
					false -> ProjectType.OTHER
				}
			}

		val errorLimitProvider: Provider<ErrorLimit> = extension.limit
			.map<ErrorLimit> { n: Int ->
				check(n >= 0) {
					"Limit must be set to a positive integer"
				}

				ErrorLimit.Max(n.toUInt())
			}
			.orElse(ErrorLimit.None)

		this.registerGitPreCommitHookInstallationTask(
			project,
			ktlintClasspathJarFilesProvider,
			projectTypeProvider,
			errorLimitProvider,
			ktlintVersionProvider,
		)

		project.afterEvaluate {
			this@KtlintPlugin.afterEvaluate(project = this@afterEvaluate)
		}
	}

	@CheckReturnValue
	private fun createExtension(extensionContainer: ExtensionContainer): KtlintPluginExtension {
		val extension: KtlintPluginExtension = extensionContainer.create<KtlintPluginExtension>(name = EXTENSION_NAME)

		extension.installGitPreCommitHookBeforeBuild.convention(false)
		extension.android.convention(false)

		return extension
	}

	@CheckReturnValue
	private fun resolveKtlintClasspathJarFilesFromVersion(project: Project, ktlintVersion: SemVer): Set<File> {
		val ktlintDependencyNotation = "$KTLINT_DEPENDENCY_NOTATION_WITHOUT_VERSION:$ktlintVersion"
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

	private fun registerGitPreCommitHookInstallationTask(
		project: Project,
		ktlintClasspathJarFilesProvider: Provider<Iterable<File>>,
		projectTypeProvider: Provider<ProjectType>,
		errorLimitProvider: Provider<ErrorLimit>,
		ktlintVersionProvider: Provider<SemVer>,
	) {
		project.tasks.register<KtlintGitPreCommitHookInstallationTask>(KTLINT_GIT_PRE_COMMIT_HOOK_INSTALLATION_TASK_NAME) {
			this@register.group = TASK_GROUP_NAME
			this@register.description = "Installs the ktlint Git pre-commit hook"

			this@register.ktlintClasspathJarFiles.set(ktlintClasspathJarFilesProvider)
			this@register.taskName.set(KTLINT_GIT_PRE_COMMIT_HOOK_INSTALLATION_TASK_NAME)
			this@register.projectType.set(projectTypeProvider)
			this@register.errorLimit.set(errorLimitProvider)
			this@register.ktlintVersion.set(ktlintVersionProvider)
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

	private fun setupAutomaticGitPreCommitHookInstallation(project: Project, extension: KtlintPluginExtension) {
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
