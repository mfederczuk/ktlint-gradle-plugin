/*
 * SPDX-License-Identifier: CC0-1.0
 */

package io.github.mfederczuk.gradle.plugin.ktlint

import net.swiftzer.semver.SemVer
import javax.annotation.CheckReturnValue

@CheckReturnValue
internal fun SemVer.Companion.isValid(version: String): Boolean {
	return (this.parseOrNull(version) != null)
}
