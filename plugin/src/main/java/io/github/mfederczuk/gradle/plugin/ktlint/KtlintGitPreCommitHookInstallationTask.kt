/*
 * Copyright (c) 2023 Michael Federczuk
 * SPDX-License-Identifier: MPL-2.0 AND Apache-2.0
 */

package io.github.mfederczuk.gradle.plugin.ktlint

import io.github.mfederczuk.gradle.plugin.ktlint.posixshtemplateengine.PosixShTemplateEngine
import io.github.mfederczuk.gradle.plugin.ktlint.posixshtemplateengine.buildPosixShTemplateEngine
import net.swiftzer.semver.SemVer
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset
import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.jar.Manifest
import javax.annotation.CheckReturnValue

@DisableCachingByDefault(because = "Not worth caching")
internal abstract class KtlintGitPreCommitHookInstallationTask : DefaultTask() {

	private companion object {
		const val HOOK_SCRIPT_TEMPLATE_RESOURCE_PATH_FORMAT: String =
			"/io/github/mfederczuk/gradle/plugin/ktlint/git/hooks/%s/pre-commit.template.sh"

		const val HOOK_SCRIPT_TEMPLATE_RESOURCE_PATH_PLATFORM_DIR_COMPONENT_WINDOWS = "windows"
		const val HOOK_SCRIPT_TEMPLATE_RESOURCE_PATH_PLATFORM_DIR_COMPONENT_OTHER = "other"
	}

	@get:Nested
	abstract val gitService: GitService

	@get:InputFiles
	abstract val ktlintClasspathJarFiles: Property<Iterable<File>>

	@get:Input
	abstract val taskName: Property<String>

	@get:Input
	abstract val codeStyle: Property<CodeStyle>

	@get:Input
	abstract val projectType: Property<ProjectType>

	@get:Input
	abstract val errorLimit: Property<ErrorLimit>

	@get:Input
	abstract val experimentalRulesEnabled: Property<Boolean>

	@get:Input
	abstract val ktlintVersion: Property<SemVer>

	@TaskAction
	fun installKtlintGitPreCommitHook() {
		val ktlintClasspathJarFiles: Iterable<File> = this.ktlintClasspathJarFiles.get()
		val taskName: String = this.taskName.get()
		val codeStyle: CodeStyle = this.codeStyle.get()
		val projectType: ProjectType = this.projectType.get()
		val errorLimit: ErrorLimit = this.errorLimit.get()
		val experimentalRulesEnabled: Boolean = this.experimentalRulesEnabled.get()
		val ktlintVersion: SemVer = this.ktlintVersion.get()

		val hookScript: String = this
			.loadHookScript(
				ktlintClasspathJarFiles.toList(),
				taskName,
				codeStyle,
				projectType,
				errorLimit,
				experimentalRulesEnabled,
				ktlintVersion,
			)

		// TODO: switch to output property? determine git dir at configuration time?
		//       this would generally be the better way to design this, but the problem is that to determine
		//       the git dir, we need to execute an external program (git itself) and i don't think that's good idea to
		//       do at configuration time...
		val hookFile: File = this.gitService.determinePreCommitHookFilePath()
		hookFile.parentFile?.mkdirs()
		hookFile.writeText(hookScript)
		hookFile.setExecutable(true)
	}

	@CheckReturnValue
	private fun loadHookScript(
		ktlintClasspathJarFiles: List<File>,
		taskName: String,
		codeStyle: CodeStyle,
		projectType: ProjectType,
		errorLimit: ErrorLimit,
		experimentalRulesEnabled: Boolean,
		ktlintVersion: SemVer,
	): String {
		val engine: PosixShTemplateEngine = buildPosixShTemplateEngine {
			replace placeholder "GENERATED_DATETIME" with generatedDateTime

			replace placeholder "KTLINT_CLASSPATH" with run {
				ktlintClasspathJarFiles.joinToString(separator = File.pathSeparator)
			}

			replace placeholder "KTLINT_MAIN_CLASS_NAME" with run {
				ktlintClasspathJarFiles.first().extractJarFileMainClassName()
			}

			replace placeholder "HOOK_INSTALLATION_TASK_NAME" with taskName

			replace placeholder "KTLINT_CODE_STYLE_OPT_ARG" with run {
				if (ktlintVersion >= SemVer(0, 49, 0)) {
					when (codeStyle) {
						is CodeStyle.Default -> ""
						is CodeStyle.Specific -> "--code-style=${codeStyle.name}"
					}
				} else {
					when (projectType) {
						ProjectType.OTHER -> ""
						ProjectType.ANDROID -> "--android"
					}
				}
			}

			replace placeholder "KTLINT_LIMIT_OPT_ARG" with when (errorLimit) {
				is ErrorLimit.None -> ""
				is ErrorLimit.Max -> "--limit=${errorLimit.n}"
			}

			replace placeholder "KTLINT_EXPERIMENTAL_OPT_ARG" with run {
				if (experimentalRulesEnabled) {
					"--experimental"
				} else {
					""
				}
			}

			replace placeholder "KTLINT_VERSION" with ktlintVersion.toString()
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
