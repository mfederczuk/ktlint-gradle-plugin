/*
 * Copyright (c) 2023 Michael Federczuk
 * SPDX-License-Identifier: MPL-2.0 AND Apache-2.0
 */

package io.github.mfederczuk.gradle.plugin.ktlint.tasks

import io.github.mfederczuk.gradle.plugin.ktlint.GitService
import io.github.mfederczuk.gradle.plugin.ktlint.utils.internalErrorMsg
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.file.Path
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

	/** Refresh the hook path if the environment variable `$GIT_DIR` changes */
	@get:Input
	abstract val gitDirEnvironmentVariableValue: Property<String>

	/** Refresh the hook path if the working directory changes */
	@get:Input
	abstract val workingDirectoryPath: Property<String>

	/** Refresh the hook path every new day */
	@get:Input
	abstract val currentDate: Property<String>

	@get:OutputFile
	abstract val hookPathOutputFile: RegularFileProperty

	@TaskAction
	fun refreshGitPreCommitHookPath() {
		this.gitDirEnvironmentVariableValue.get()
		this.workingDirectoryPath.get()
		this.currentDate.get()
		val hookPathOutputFile: File = this.hookPathOutputFile.asFile.get()

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
