/*
 * SPDX-License-Identifier: CC0-1.0
 */

package io.github.mfederczuk.gradle.plugin.ktlint.models

import java.io.Serializable

internal sealed class CodeStyle : Serializable {

	object Default : CodeStyle() {
		private fun readResolve(): Any {
			return Default
		}
	}

	data class Specific(val name: String) : CodeStyle()
}
