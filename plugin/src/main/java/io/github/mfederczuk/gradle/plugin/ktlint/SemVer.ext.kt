/*
 * SPDX-License-Identifier: CC0-1.0
 */

package io.github.mfederczuk.gradle.plugin.ktlint

import net.swiftzer.semver.SemVer

// <https://github.com/swiftzer/semver/pull/13>
internal fun SemVer.Companion.parseOrNull(version: String): SemVer? {
	return try {
		this.parse(version)
	} catch (_: IllegalArgumentException) {
		null
	}
}

internal fun SemVer.Companion.isValid(version: String): Boolean {
	return (this.parseOrNull(version) != null)
}
