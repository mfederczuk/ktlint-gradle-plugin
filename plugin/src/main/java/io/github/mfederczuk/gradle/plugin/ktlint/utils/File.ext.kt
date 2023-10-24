/*
 * SPDX-License-Identifier: CC0-1.0
 */

package io.github.mfederczuk.gradle.plugin.ktlint.utils

import java.io.File
import javax.annotation.CheckReturnValue

@CheckReturnValue
internal fun File.relativeToCwd(): File {
	val cwd = File(System.getProperty("user.dir"))

	return this.relativeTo(cwd)
}
