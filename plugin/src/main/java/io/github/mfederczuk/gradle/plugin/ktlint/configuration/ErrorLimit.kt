/*
 * SPDX-License-Identifier: CC0-1.0
 */

package io.github.mfederczuk.gradle.plugin.ktlint.configuration

import java.io.Serializable

internal sealed class ErrorLimit : Serializable {

	object None : ErrorLimit() {
		private fun readResolve(): Any {
			return None
		}
	}

	data class Max(val n: UInt) : ErrorLimit()
}
