/*
 * SPDX-License-Identifier: CC0-1.0
 */

package io.github.mfederczuk.gradle.plugin.ktlint

internal enum class ProjectType {
	OTHER,
	ANDROID,
}

internal fun ProjectType.isAndroid(): Boolean {
	return when (this) {
		ProjectType.OTHER -> false
		ProjectType.ANDROID -> true
	}
}
