/*
 * SPDX-License-Identifier: CC0-1.0
 */

package io.github.mfederczuk.gradle.plugin.ktlint.models

internal sealed class CodeStyle {

	object Default : CodeStyle()

	data class Specific(val name: String) : CodeStyle()
}
