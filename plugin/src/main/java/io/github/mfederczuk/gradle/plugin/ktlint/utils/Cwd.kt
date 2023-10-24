/*
 * SPDX-License-Identifier: CC0-1.0
 */

package io.github.mfederczuk.gradle.plugin.ktlint.utils

import java.nio.file.Path
import javax.annotation.CheckReturnValue

@CheckReturnValue
internal fun getCurrentWorkingDirectoryPath(): Path {
	val str: String = System.getProperty("user.dir").orEmpty()

	check(str.isNotEmpty()) {
		"Current working directory is null".internalErrorMsg
	}

	val path: Path = Path.of(str)

	check(path.isAbsolute) {
		"Current working directory is not an absolute path".internalErrorMsg
	}

	return path
}
