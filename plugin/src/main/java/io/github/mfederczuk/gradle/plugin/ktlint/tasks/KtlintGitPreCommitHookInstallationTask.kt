/*
 * Copyright (c) 2023 Michael Federczuk
 * SPDX-License-Identifier: MPL-2.0 AND Apache-2.0
 */

package io.github.mfederczuk.gradle.plugin.ktlint.tasks

import io.github.mfederczuk.gradle.plugin.ktlint.PluginExtensionUtils
import io.github.mfederczuk.gradle.plugin.ktlint.configuration.CodeStyle
import io.github.mfederczuk.gradle.plugin.ktlint.configuration.ErrorLimit
import io.github.mfederczuk.gradle.plugin.ktlint.configuration.PluginConfiguration
import io.github.mfederczuk.gradle.plugin.ktlint.configuration.toConfiguration
import io.github.mfederczuk.gradle.plugin.ktlint.resolveKtlintClasspathJarFilesFromVersion
import io.github.mfederczuk.gradle.plugin.ktlint.utils.internalErrorMsg
import io.github.mfederczuk.shtemplate.ShTemplateEngine
import io.github.mfederczuk.shtemplate.buildShTemplateEngine
import net.swiftzer.semver.SemVer
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.file.Path
import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.jar.Manifest
import javax.annotation.CheckReturnValue

@CacheableTask
internal abstract class KtlintGitPreCommitHookInstallationTask : DefaultTask() {

	companion object {

		private const val TASK_NAME: String = "installKtlintGitPreCommitHook"

		private const val HOOK_SCRIPT_TEMPLATE_RESOURCE_PATH_FORMAT: String =
			"/io/github/mfederczuk/gradle/plugin/ktlint/git/hooks/%s/pre-commit.template.sh"

		private const val HOOK_SCRIPT_TEMPLATE_RESOURCE_PATH_PLATFORM_DIR_COMPONENT_WINDOWS = "windows"
		private const val HOOK_SCRIPT_TEMPLATE_RESOURCE_PATH_PLATFORM_DIR_COMPONENT_OTHER = "other"

		fun registerIn(
			project: Project,
			groupName: String,
		) {
			project.tasks.register<KtlintGitPreCommitHookInstallationTask>(TASK_NAME) {
				this@register.group = groupName
			}
		}

		@CheckReturnValue
		fun getFrom(project: Project): KtlintGitPreCommitHookInstallationTask {
			val task: Any? = project.tasks.findByName(TASK_NAME)

			checkNotNull(task) {
				"Task with name \"$TASK_NAME\" not found in $project".internalErrorMsg
			}

			check(task is KtlintGitPreCommitHookInstallationTask) {
				"Task with name \"$TASK_NAME\" in $project" +
					" is not of type ${KtlintGitPreCommitHookInstallationTask::class.java}".internalErrorMsg
			}

			return task
		}
	}

	@get:InputFiles
	@get:Classpath
	val ktlintClasspathJarFiles: Provider<Iterable<File>>

	@get:Input
	val codeStyle: Provider<CodeStyle>

	@get:Input
	val errorLimit: Provider<ErrorLimit>

	@get:Input
	val experimentalRulesEnabled: Provider<Boolean>

	@get:Input
	val ktlintVersion: Provider<String>

	@get:OutputFile
	val gitPreCommitHookFile: Provider<RegularFile>

	init {
		this.description = "Installs the ktlint Git pre-commit hook"

		val gitPreCommitHookPathRefreshTaskProvider: TaskProvider<GitPreCommitHookPathRefreshTask> =
			GitPreCommitHookPathRefreshTask.getFrom(this.project)

		this.dependsOn(gitPreCommitHookPathRefreshTaskProvider)

		// region inputs

		val configurationProvider: Provider<PluginConfiguration> = PluginExtensionUtils.getExtension(this.project)
			.toConfiguration(this.project.providers)

		this.ktlintClasspathJarFiles = configurationProvider
			.map { configuration: PluginConfiguration ->
				resolveKtlintClasspathJarFilesFromVersion(
					configuration.ktlintVersion,
					dependencyHandler = this.project.dependencies,
					configurationContainer = this.project.configurations,
				)
			}

		this.codeStyle = configurationProvider
			.map(PluginConfiguration::codeStyle)

		this.errorLimit = configurationProvider
			.map(PluginConfiguration::errorLimit)

		this.experimentalRulesEnabled = configurationProvider
			.map(PluginConfiguration::experimentalRulesEnabled)

		this.ktlintVersion = configurationProvider
			.map { configuration: PluginConfiguration ->
				configuration.ktlintVersion.toString()
			}

		// endregion

		this.gitPreCommitHookFile = gitPreCommitHookPathRefreshTaskProvider
			.flatMap { gitPreCommitHookPathRefreshTask: GitPreCommitHookPathRefreshTask ->
				gitPreCommitHookPathRefreshTask.getHookFile(this.project.providers)
			}
			.map(Path::toFile)
			.let(this.project.layout::file)
	}

	@TaskAction
	fun installKtlintGitPreCommitHook() {
		val ktlintClasspathJarFiles: Iterable<File> = this.ktlintClasspathJarFiles.get()
		val codeStyle: CodeStyle = this.codeStyle.get()
		val errorLimit: ErrorLimit = this.errorLimit.get()
		val experimentalRulesEnabled: Boolean = this.experimentalRulesEnabled.get()
		val ktlintVersion: SemVer = SemVer.parse(this.ktlintVersion.get())
		val hookFile: File = this.gitPreCommitHookFile.get().asFile

		val hookScript: String = this
			.loadHookScript(
				ktlintClasspathJarFiles.toList(),
				codeStyle,
				errorLimit,
				experimentalRulesEnabled,
				ktlintVersion,
			)

		hookFile.parentFile?.mkdirs()
		hookFile.writeText(hookScript)
		hookFile.setExecutable(true)
	}

	@CheckReturnValue
	private fun loadHookScript(
		ktlintClasspathJarFiles: List<File>,
		codeStyle: CodeStyle,
		errorLimit: ErrorLimit,
		experimentalRulesEnabled: Boolean,
		ktlintVersion: SemVer,
	): String {
		val engine: ShTemplateEngine = buildShTemplateEngine {
			replace placeholder "GENERATED_DATETIME" ofType commentText with generatedDateTime

			replace placeholder "KTLINT_CLASSPATH" ofType quotedString with
				ktlintClasspathJarFiles.joinToString(separator = File.pathSeparator)

			replace placeholder "KTLINT_MAIN_CLASS_NAME" ofType quotedString with
				ktlintClasspathJarFiles.first().extractJarFileMainClassName()

			replace placeholder "HOOK_INSTALLATION_TASK_NAME" ofType quotedString with
				this@KtlintGitPreCommitHookInstallationTask.name

			replace placeholder "KTLINT_CODE_STYLE_OPT_ARG" ofType quotedString with
				when (codeStyle) {
					is CodeStyle.Default -> ""
					is CodeStyle.Specific -> "--code-style=${codeStyle.name}"
				}

			replace placeholder "KTLINT_LIMIT_OPT_ARG" ofType quotedString with
				when (errorLimit) {
					is ErrorLimit.None -> ""
					is ErrorLimit.Max -> "--limit=${errorLimit.n}"
				}

			replace placeholder "KTLINT_EXPERIMENTAL_OPT_ARG" ofType quotedString with
				if (experimentalRulesEnabled) {
					"--experimental"
				} else {
					""
				}

			replace placeholder "KTLINT_VERSION" ofType quotedString with ktlintVersion.toString()
		}

		val hookScriptTemplate: String = this.loadHookScriptTemplate()

		return engine.processString(hookScriptTemplate)
	}

	@CheckReturnValue
	private fun loadHookScriptTemplate(): String {
		val platformDirComponent: String =
			if (isCurrentSystemWindows()) {
				HOOK_SCRIPT_TEMPLATE_RESOURCE_PATH_PLATFORM_DIR_COMPONENT_WINDOWS
			} else {
				HOOK_SCRIPT_TEMPLATE_RESOURCE_PATH_PLATFORM_DIR_COMPONENT_OTHER
			}

		val hookScriptResourcePath: String = HOOK_SCRIPT_TEMPLATE_RESOURCE_PATH_FORMAT.format(platformDirComponent)

		return checkNotNull(this.javaClass.getResourceAsStream(hookScriptResourcePath))
			.use { hookScriptTemplateInputStream: InputStream ->
				String(hookScriptTemplateInputStream.readAllBytes(), Charset.forName("UTF-8"))
			}
	}
}

@CheckReturnValue
private fun File.extractJarFileMainClassName(): String {
	val jarFile = JarFile(this)

	val manifest: Manifest? = jarFile.manifest
	checkNotNull(manifest) {
		"JAR $this has no manifest".internalErrorMsg
	}

	val mainClassName: String? = manifest.mainAttributes.getValue(Attributes.Name.MAIN_CLASS)
	checkNotNull(mainClassName) {
		"JAR $this has no main class".internalErrorMsg
	}

	return mainClassName
}

@CheckReturnValue
private fun isCurrentSystemWindows(): Boolean {
	return ("win" in System.getProperty("os.name").lowercase())
}
