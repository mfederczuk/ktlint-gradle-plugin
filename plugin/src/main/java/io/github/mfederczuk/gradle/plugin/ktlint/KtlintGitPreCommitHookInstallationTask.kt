/*
 * Copyright (c) 2023 Michael Federczuk
 * SPDX-License-Identifier: MPL-2.0 AND Apache-2.0
 */

package io.github.mfederczuk.gradle.plugin.ktlint

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset
import java.time.ZonedDateTime
import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.jar.Manifest

@DisableCachingByDefault(because = "Not worth caching")
internal abstract class KtlintGitPreCommitHookInstallationTask : DefaultTask() {

	private companion object {
		const val HOOK_SCRIPT_TEMPLATE_RESOURCE_PATH_FORMAT: String =
			"/io/github/mfederczuk/gradle/plugin/ktlint/git/hooks/%s/pre-commit.template.sh"

		const val HOOK_SCRIPT_TEMPLATE_RESOURCE_PATH_PLATFORM_DIR_COMPONENT_WINDOWS = "windows"
		const val HOOK_SCRIPT_TEMPLATE_RESOURCE_PATH_PLATFORM_DIR_COMPONENT_OTHER = "other"
	}

	@get:Input
	abstract val taskName: Property<String>

	@get:InputFiles
	abstract val classpathJarFiles: Property<Iterable<File>>

	@TaskAction
	fun installKtlintGitPreCommitHook() {
		val taskName: String = this.taskName.get()
		val ktlintClasspathJarFiles: Iterable<File> = this.classpathJarFiles.get()

		val hookScript: String = this.loadHookScript(ktlintClasspathJarFiles.toList(), taskName)

		val gitPreCommitHookFile: File = determineGitPreCommitHookFilePath(project)
		gitPreCommitHookFile.parentFile?.mkdirs()
		gitPreCommitHookFile.writeText(hookScript)
		gitPreCommitHookFile.setExecutable(true)
	}

	private fun loadHookScript(ktlintClasspathJarFiles: List<File>, taskName: String): String {
		val platformDirComponent: String =
			if (isCurrentSystemWindows()) {
				HOOK_SCRIPT_TEMPLATE_RESOURCE_PATH_PLATFORM_DIR_COMPONENT_WINDOWS
			} else {
				HOOK_SCRIPT_TEMPLATE_RESOURCE_PATH_PLATFORM_DIR_COMPONENT_OTHER
			}

		val hookScriptResourcePath: String = HOOK_SCRIPT_TEMPLATE_RESOURCE_PATH_FORMAT.format(platformDirComponent)

		val mainClassName: String = ktlintClasspathJarFiles.first().extractJarFileMainClassName()

		return checkNotNull(this.javaClass.getResourceAsStream(hookScriptResourcePath))
			.use { hookScriptTemplateInputStream: InputStream ->
				String(hookScriptTemplateInputStream.readAllBytes(), Charset.forName("UTF-8"))
			}
			.replace(oldValue = "::GENERATED_DATETIME::", newValue = ZonedDateTime.now().toString())
			.replace(
				oldValue = "::KTLINT_CLASSPATH::",
				newValue = ktlintClasspathJarFiles
					.joinToString(separator = File.pathSeparator)
					.quoteForPosixShell(),
			)
			.replace(oldValue = "::KTLINT_MAIN_CLASS_NAME::", newValue = mainClassName.quoteForPosixShell())
			.replace(
				oldValue = "::HOOK_INSTALLATION_TASK_NAME::",
				newValue = taskName.quoteForPosixShell(),
			)
	}
}

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

private fun isCurrentSystemWindows(): Boolean {
	return ("win" in System.getProperty("os.name").lowercase())
}

private fun determineGitPreCommitHookFilePath(project: Project): File {
	// this size was mostly chosen arbitrarily.
	// we need at least 22 bytes because the most expected value will be ".git/hooks/pre-commit\n".
	// absolute paths will probably where around 64 bytes
	// 128 bytes should cover most cases
	val stdout = ByteArrayOutputStream(128)

	project
		.exec {
			// TODO: does Windows need git.exe here?

			// Git fucking spoiled us with the `rev-parse --git-path` command.
			//
			// It handles the following things for us, so we don't need to worry about it:
			//
			//  * the environment variable $GIT_DIR
			//  * being in a worktree
			//  * not being in the top level directory
			//  * the config `core.hooksPath`
			//
			// Doesn't matter what is configured or what the current working directory is; this command *should* always
			// return the correct pathname to the pre-commit file.
			// What an absolute luxury.

			commandLine = listOf("git", "--no-pager", "rev-parse", "--git-path", "hooks/pre-commit")
			standardInput = InputStream.nullInputStream()
			standardOutput = stdout
			errorOutput = System.err
		}
		.assertNormalExitValue()
		.rethrowFailure()

	// TODO: does Windows need UTF-16 here?
	val gitPreCommitHookFilePath: String = stdout.toString(Charset.forName("UTF-8"))
		.trimEnd() // remove trailing newlines

	return project.projectDir.resolve(gitPreCommitHookFilePath)
		.relativeToCwd()
}

private fun String.quoteForPosixShell(): String {
	return buildString(capacity = (this.length + 2)) {
		this@buildString.append('\'')
		this@buildString.append(this@quoteForPosixShell.replace(oldValue = "'", newValue = "'\\''"))
		this@buildString.append('\'')
	}
}
