/*
 * SPDX-License-Identifier: CC0-1.0
 */

package io.github.mfederczuk.gradle.plugin.ktlint.models

internal sealed class ErrorLimit {

	object None : ErrorLimit()

	data class Max(val n: UInt) : ErrorLimit()
}
