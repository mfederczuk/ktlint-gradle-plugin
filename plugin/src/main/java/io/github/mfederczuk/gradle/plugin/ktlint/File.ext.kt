/*
 * SPDX-License-Identifier: CC0-1.0
 */

package io.github.mfederczuk.gradle.plugin.ktlint

import java.io.File

internal fun File.relativeToCwd(): File {
	val cwd = File(System.getProperty("user.dir"))

	return this.relativeTo(cwd)
}
