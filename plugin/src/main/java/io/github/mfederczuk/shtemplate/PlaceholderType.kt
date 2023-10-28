/*
 * SPDX-License-Identifier: CC0-1.0
 */

package io.github.mfederczuk.shtemplate

import javax.annotation.CheckReturnValue

internal enum class PlaceholderType(private val identifier: String) {
	QUOTED_STRING("quoted_string"),
	ARGS("args"),
	COMMENT_TEXT("comment_text"),
	;

	companion object {

		@CheckReturnValue
		fun ofIdentifierOrNull(identifier: String): PlaceholderType? {
			return PlaceholderType.values()
				.firstOrNull { type: PlaceholderType ->
					type.identifier == identifier
				}
		}
	}
}
