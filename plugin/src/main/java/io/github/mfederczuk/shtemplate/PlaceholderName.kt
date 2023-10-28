/*
 * Copyright (c) 2023 Michael Federczuk
 * SPDX-License-Identifier: MPL-2.0 AND Apache-2.0
 */

package io.github.mfederczuk.shtemplate

import javax.annotation.CheckReturnValue

@JvmInline
internal value class PlaceholderName private constructor(private val string: String) {

	companion object {

		private val NAME_PATTERN: Regex = Regex(pattern = "^[A-Z][A-Z0-9_]*$")

		@CheckReturnValue
		fun isValid(nameString: String): Boolean {
			return NAME_PATTERN.matches(nameString)
		}

		@CheckReturnValue
		fun ofString(nameString: String): PlaceholderName {
			require(isValid(nameString)) {
				"Placeholder name string must not be invalid"
			}

			return PlaceholderName(nameString)
		}
	}

	init {
		require(isValid(this.string)) {
			"Placeholder name must not be invalid"
		}
	}

	override fun toString(): String {
		return this.string
	}
}
