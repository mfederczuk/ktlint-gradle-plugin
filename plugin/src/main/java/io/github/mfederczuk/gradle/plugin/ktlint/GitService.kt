/*
 * Copyright (c) 2023 Michael Federczuk
 * SPDX-License-Identifier: MPL-2.0 AND Apache-2.0
 */

package io.github.mfederczuk.gradle.plugin.ktlint

import io.github.mfederczuk.gradle.plugin.ktlint.utils.getCurrentWorkingDirectoryPath
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.file.Path
import javax.annotation.CheckReturnValue
import javax.inject.Inject

internal abstract class GitService {

	@get:Inject
	abstract val execOperations: ExecOperations

	@CheckReturnValue
	fun determinePreCommitHookFilePath(): Path {
		// this size was mostly chosen arbitrarily.
		// we need at least 22 bytes because the most expected value will be ".git/hooks/pre-commit\n".
		// absolute paths will probably where around 64 bytes
		// 128 bytes should cover most cases
		val stdout = ByteArrayOutputStream(128)

		val currentWorkingDirectoryPath: Path = getCurrentWorkingDirectoryPath()

		this.execOperations
			.exec {
				// TODO: does Windows need git.exe here?

				// Git fucking spoiled us with the `rev-parse --git-path` command.
				//
				// It handles the following things for us, so we don't need to worry about it:
				//
				//  * environment variables like $GIT_DIR, $GIT_OBJECT_DIRECTORY, ...
				//  * being in a worktree
				//  * not being in the top level directory
				//  * configs like `core.hooksPath`, ...
				//
				// Doesn't matter what is configured or what the current working directory is; this command *should*
				// return the correct pathname to the requested file.
				// What an absolute luxury.

				this@exec.workingDir = currentWorkingDirectoryPath.toFile()

				this@exec.commandLine = listOf("git", "--no-pager", "rev-parse", "--git-path", "hooks/pre-commit")

				this@exec.standardInput = InputStream.nullInputStream()
				this@exec.standardOutput = stdout
				this@exec.errorOutput = System.err
			}
			.assertNormalExitValue()
			.rethrowFailure()

		// TODO: does Windows need UTF-16 here?
		val relativeHookFilePath: Path = stdout.toString(Charsets.UTF_8)
			.trimEnd() // remove trailing newlines
			.let(Path::of)

		return currentWorkingDirectoryPath.resolve(relativeHookFilePath)
	}
}
