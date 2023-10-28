/*
 * Copyright (c) 2023 Michael Federczuk
 * SPDX-License-Identifier: MPL-2.0 AND Apache-2.0
 */

package io.github.mfederczuk.shtemplate

import java.time.ZonedDateTime
import javax.annotation.CheckReturnValue

internal class ShTemplateEngineBuilder {

	private val replacementValueSuppliers: MutableMap<PlaceholderName, ReplacementValueSupplier> = mutableMapOf()

	fun quotedString(
		placeholderName: PlaceholderName,
		string: String,
	) {
		this.replacementValueSuppliers[placeholderName] = ReplacementValueSupplier.QuotedString { string }
	}

	fun commentTextGeneratedDateTime(placeholderName: PlaceholderName) {
		this.replacementValueSuppliers[placeholderName] = ReplacementValueSupplier
			.CommentText { generatedDateTime: ZonedDateTime ->
				generatedDateTime.toString()
			}
	}

	@CheckReturnValue
	fun build(): ShTemplateEngine {
		return ShTemplateEngine(this.replacementValueSuppliers)
	}
}
