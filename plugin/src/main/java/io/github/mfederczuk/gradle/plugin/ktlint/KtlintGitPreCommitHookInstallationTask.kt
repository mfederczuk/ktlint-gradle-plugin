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
import java.time.ZonedDateTime
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

	@get:Input
	abstract val taskName: Property<String>

	@get:InputFiles
	abstract val classpathJarFiles: Property<Iterable<File>>

	@get:Input
	abstract val projectType: Property<ProjectType>

	@get:Input
	abstract val limit: Property<ErrorLimit>

	@get:Input
	abstract val ktlintVersion: Property<SemVer>

	@TaskAction
	fun installKtlintGitPreCommitHook() {
		val taskName: String = this.taskName.get()
		val ktlintClasspathJarFiles: Iterable<File> = this.classpathJarFiles.get()
		val projectType: ProjectType = this.projectType.get()
		val limit: ErrorLimit = this.limit.get()
		val ktlintVersion: SemVer = this.ktlintVersion.get()

		val hookScript: String = this
			.loadHookScript(
				ktlintClasspathJarFiles.toList(),
				taskName,
				projectType,
				limit,
				ktlintVersion,
			)

		// TODO: switch to output property? determine git dir at configuration time?
		//       this would generally be the better way to design this, but the problem is that to determine
		//       the git dir, we need to execute an external program (git itself) and i don't think that's good idea to
		//       do at configuration time...
		val gitPreCommitHookFile: File = this.gitService.determinePreCommitHookFilePath()
		gitPreCommitHookFile.parentFile?.mkdirs()
		gitPreCommitHookFile.writeText(hookScript)
		gitPreCommitHookFile.setExecutable(true)
	}

	@CheckReturnValue
	private fun loadHookScript(
		ktlintClasspathJarFiles: List<File>,
		taskName: String,
		projectType: ProjectType,
		limit: ErrorLimit,
		ktlintVersion: SemVer,
	): String {
		val engine: PosixShTemplateEngine = buildPosixShTemplateEngine {
			replace("GENERATED_DATETIME") with lazy {
				ZonedDateTime.now().toString()
			}

			replace("KTLINT_CLASSPATH") with ktlintClasspathJarFiles.joinToString(separator = File.pathSeparator)

			replace("KTLINT_MAIN_CLASS_NAME") with ktlintClasspathJarFiles.first().extractJarFileMainClassName()

			replace("HOOK_INSTALLATION_TASK_NAME") with taskName

			replace("KTLINT_ANDROID_OPT_ARG") with when (projectType) {
				ProjectType.OTHER -> ""
				ProjectType.ANDROID -> {
					if (ktlintVersion >= SemVer(0, 49, 0)) {
						"--code-style=android_studio"
					} else {
						"--android"
					}
				}
			}

			replace("KTLINT_LIMIT_OPT_ARG") with when (limit) {
				is ErrorLimit.None -> ""
				is ErrorLimit.Max -> "--limit=${limit.n}"
			}

			replace("KTLINT_VERSION") with ktlintVersion.toString()
		}

		val platformDirComponent: String =
			if (isCurrentSystemWindows()) {
				HOOK_SCRIPT_TEMPLATE_RESOURCE_PATH_PLATFORM_DIR_COMPONENT_WINDOWS
			} else {
				HOOK_SCRIPT_TEMPLATE_RESOURCE_PATH_PLATFORM_DIR_COMPONENT_OTHER
			}

		val hookScriptResourcePath: String = HOOK_SCRIPT_TEMPLATE_RESOURCE_PATH_FORMAT.format(platformDirComponent)

		val hookScriptTemplate: String = checkNotNull(this.javaClass.getResourceAsStream(hookScriptResourcePath))
			.use { hookScriptTemplateInputStream: InputStream ->
				String(hookScriptTemplateInputStream.readAllBytes(), Charset.forName("UTF-8"))
			}

		return engine.processString(hookScriptTemplate)
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
