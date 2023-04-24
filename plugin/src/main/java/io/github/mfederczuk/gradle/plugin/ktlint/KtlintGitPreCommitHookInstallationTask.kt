/*
 * Copyright (c) 2023 Michael Federczuk
 * SPDX-License-Identifier: MPL-2.0 AND Apache-2.0
 */

package io.github.mfederczuk.gradle.plugin.ktlint

import net.swiftzer.semver.SemVer
import org.gradle.api.DefaultTask
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset
import java.time.ZonedDateTime
import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.jar.Manifest
import javax.annotation.CheckReturnValue
import javax.inject.Inject

@DisableCachingByDefault(because = "Not worth caching")
internal abstract class KtlintGitPreCommitHookInstallationTask : DefaultTask() {

	private companion object {
		const val HOOK_SCRIPT_TEMPLATE_RESOURCE_PATH_FORMAT: String =
			"/io/github/mfederczuk/gradle/plugin/ktlint/git/hooks/%s/pre-commit.template.sh"

		const val HOOK_SCRIPT_TEMPLATE_RESOURCE_PATH_PLATFORM_DIR_COMPONENT_WINDOWS = "windows"
		const val HOOK_SCRIPT_TEMPLATE_RESOURCE_PATH_PLATFORM_DIR_COMPONENT_OTHER = "other"
	}

	@get:Inject
	abstract val execOperations: ExecOperations

	@get:Inject
	abstract val projectLayout: ProjectLayout

	@get:Input
	abstract val taskName: Property<String>

	@get:InputFiles
	abstract val classpathJarFiles: Property<Iterable<File>>

	@get:Input
	abstract val projectType: Property<ProjectType>

	@get:Input
	abstract val ktlintVersion: Property<SemVer>

	@TaskAction
	fun installKtlintGitPreCommitHook() {
		val taskName: String = this.taskName.get()
		val ktlintClasspathJarFiles: Iterable<File> = this.classpathJarFiles.get()
		val projectType: ProjectType = this.projectType.get()
		val ktlintVersion: SemVer = this.ktlintVersion.get()

		val hookScript: String = this
			.loadHookScript(
				ktlintClasspathJarFiles.toList(),
				taskName,
				projectType,
				ktlintVersion,
			)

		// TODO: switch to output property? determine git dir at configuration time?
		//       this would generally be the better way to design this, but the problem is that to determine
		//       the git dir, we need to execute an external program (git itself) and i don't think that's good idea to
		//       do at configuration time...
		val gitPreCommitHookFile: File =
			determineGitPreCommitHookFilePath(
				execOperations = this.execOperations,
				projectDir = this.projectLayout.projectDirectory.asFile,
			)
		gitPreCommitHookFile.parentFile?.mkdirs()
		gitPreCommitHookFile.writeText(hookScript)
		gitPreCommitHookFile.setExecutable(true)
	}

	@CheckReturnValue
	private fun loadHookScript(
		ktlintClasspathJarFiles: List<File>,
		taskName: String,
		projectType: ProjectType,
		ktlintVersion: SemVer,
	): String {
		val platformDirComponent: String =
			if (isCurrentSystemWindows()) {
				HOOK_SCRIPT_TEMPLATE_RESOURCE_PATH_PLATFORM_DIR_COMPONENT_WINDOWS
			} else {
				HOOK_SCRIPT_TEMPLATE_RESOURCE_PATH_PLATFORM_DIR_COMPONENT_OTHER
			}

		val hookScriptResourcePath: String = HOOK_SCRIPT_TEMPLATE_RESOURCE_PATH_FORMAT.format(platformDirComponent)

		val mainClassName: String = ktlintClasspathJarFiles.first().extractJarFileMainClassName()

		val ktlintAndroidOptArg: String =
			when (projectType) {
				ProjectType.OTHER -> ""
				ProjectType.ANDROID -> {
					if (ktlintVersion >= SemVer(0, 49, 0)) {
						"--code-style=android_studio"
					} else {
						"--android"
					}
				}
			}

		return checkNotNull(this.javaClass.getResourceAsStream(hookScriptResourcePath))
			.use { hookScriptTemplateInputStream: InputStream ->
				String(hookScriptTemplateInputStream.readAllBytes(), Charset.forName("UTF-8"))
			}
			// TODO: this should be changed to a different system.
			//       instead of just using replace, scan the script for formatting placeholders in the format of:
			//           //<name>::<type>//
			//       where <type> is either: 'comment', 'quoted_string' or 'bool'.
			//       special 'comment' type so that newlines will pe prepended with '# '
			//       also detect the indentation level to correctly format
			//       e.g.:
			//           //GENERATED_DATETIME::comment//
			//           //KTLINT_CLASSPATH::quoted_string//
			//           //IS_ANDROID::bool//
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
			.replace(
				oldValue = "::KTLINT_ANDROID_OPT_ARG::",
				newValue = ktlintAndroidOptArg.quoteForPosixShell(),
			)
			.replace(
				oldValue = "::KTLINT_VERSION::",
				newValue = ktlintVersion.toString().quoteForPosixShell(),
			)
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

@CheckReturnValue
private fun determineGitPreCommitHookFilePath(execOperations: ExecOperations, projectDir: File): File {
	// this size was mostly chosen arbitrarily.
	// we need at least 22 bytes because the most expected value will be ".git/hooks/pre-commit\n".
	// absolute paths will probably where around 64 bytes
	// 128 bytes should cover most cases
	val stdout = ByteArrayOutputStream(128)

	execOperations
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

	return projectDir.resolve(gitPreCommitHookFilePath)
		.relativeToCwd()
}

@CheckReturnValue
private fun String.quoteForPosixShell(): String {
	return buildString(capacity = (this.length + 2)) {
		this@buildString.append('\'')
		this@buildString.append(this@quoteForPosixShell.replace(oldValue = "'", newValue = "'\\''"))
		this@buildString.append('\'')
	}
}
