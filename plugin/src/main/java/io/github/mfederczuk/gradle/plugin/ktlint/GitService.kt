/*
 * Copyright (c) 2023 Michael Federczuk
 * SPDX-License-Identifier: MPL-2.0 AND Apache-2.0
 */

package io.github.mfederczuk.gradle.plugin.ktlint

import org.gradle.api.file.ProjectLayout
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset
import javax.annotation.CheckReturnValue
import javax.inject.Inject

internal abstract class GitService {

	@get:Inject
	abstract val execOperations: ExecOperations

	@get:Inject
	abstract val projectLayout: ProjectLayout

	@CheckReturnValue
	fun determinePreCommitHookFilePath(): File {
		return this.resolveGirDirPath(path = "hooks/pre-commit")
	}

	@CheckReturnValue
	private fun resolveGirDirPath(path: String): File {
		// this size was mostly chosen arbitrarily.
		// we need at least 22 bytes because the most expected value will be ".git/hooks/pre-commit\n".
		// absolute paths will probably where around 64 bytes
		// 128 bytes should cover most cases
		val stdout = ByteArrayOutputStream(128)

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

				commandLine = listOf("git", "--no-pager", "rev-parse", "--git-path", path)
				standardInput = InputStream.nullInputStream()
				standardOutput = stdout
				errorOutput = System.err
			}
			.assertNormalExitValue()
			.rethrowFailure()

		// TODO: does Windows need UTF-16 here?
		val gitPreCommitHookFilePath: String = stdout.toString(Charset.forName("UTF-8"))
			.trimEnd() // remove trailing newlines

		return this.projectLayout.projectDirectory.asFile.resolve(gitPreCommitHookFilePath)
			.relativeToCwd()
	}
}
