/*
 * Copyright (c) 2023 Michael Federczuk
 * SPDX-License-Identifier: MPL-2.0 AND Apache-2.0
 */

package io.github.mfederczuk.gradle.plugin.ktlint.tasks

import io.github.mfederczuk.gradle.plugin.ktlint.GitService
import io.github.mfederczuk.gradle.plugin.ktlint.utils.internalErrorMsg
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.file.Path
import java.time.LocalDate
import javax.annotation.CheckReturnValue

/**
 * Task that refreshes the file that stores the path of Git pre-commit hook file.
 *
 * This is a cacheable task with no "real" inputs that are actually used by the task.
 */
@CacheableTask
internal abstract class GitPreCommitHookPathRefreshTask : DefaultTask() {

	init {
		this.description = "(internal) Saves the path of the Git pre-commit hook file"
	}

	@get:Nested
	abstract val gitService: GitService

	init {
		// refresh the hook path if the environment variable `$GIT_DIR` changes
		val gitDirEnvVarProvider: Provider<String> = this.project.providers.environmentVariable("GIT_DIR")
			.orElse("")
		this.inputs.property("gitDirEnvVar", gitDirEnvVarProvider)

		// refresh the hook path every new day
		// note: for some reason this only works if the gradle task is executed via IntelliJ IDEA.
		//       it doesn't work if the gradle wrapper script is used
		this.inputs.property("currentDate", this.project.provider { LocalDate.now().toString() })
	}

	@get:OutputFile
	val hookPathOutputFile: Provider<RegularFile> = this.project.layout.buildDirectory
		.dir("git")
		.map { dir: Directory ->
			dir.file("preCommitPath.txt")
		}

	@TaskAction
	fun refreshGitPreCommitHookPath() {
		val hookPathOutputFile: File = this.hookPathOutputFile.get().asFile

		val hookFilePath: Path = this.gitService.determinePreCommitHookFilePath()

		hookPathOutputFile
			.writeText(
				hookFilePath.toString(),
				charset = Charsets.UTF_8,
			)
	}

	@CheckReturnValue
	fun getHookFile(providerFactory: ProviderFactory): Provider<Path> {
		// this local variable is important, DON'T inline it!
		// if we were to use `this.hookPathOutputFile` inside the `.map { }` lambda, then the entire task object will be
		// captured by the lambda object and that will lead to Gradle trying to serialize the task object in
		// the configuration cache, which is not possible
		val hookPathOutputFileProvider: Provider<RegularFile> = this.hookPathOutputFile

		return providerFactory
			.fileContents(hookPathOutputFileProvider)
			.asText
			.map { hookPathStr: String ->
				val hookPath: Path = Path.of(hookPathStr)

				check(hookPath.isAbsolute) {
					"Git pre-commit hook path read from file ${hookPathOutputFileProvider.get()} is not absolute"
						.internalErrorMsg
				}

				hookPath
			}
	}
}
