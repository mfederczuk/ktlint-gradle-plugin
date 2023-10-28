/*
 * SPDX-License-Identifier: CC0-1.0
 */

package io.github.mfederczuk.shtemplate

import java.time.ZonedDateTime
import javax.annotation.CheckReturnValue

internal sealed interface ReplacementValueSupplier {

	@FunctionalInterface
	fun interface QuotedString : ReplacementValueSupplier {

		@CheckReturnValue
		fun getString(): String
	}

	@FunctionalInterface
	fun interface Args : ReplacementValueSupplier {

		@CheckReturnValue
		fun getArgs(): List<String>
	}

	@FunctionalInterface
	fun interface CommentText : ReplacementValueSupplier {

		@CheckReturnValue
		fun getText(generatedDateTime: ZonedDateTime): String
	}
}
