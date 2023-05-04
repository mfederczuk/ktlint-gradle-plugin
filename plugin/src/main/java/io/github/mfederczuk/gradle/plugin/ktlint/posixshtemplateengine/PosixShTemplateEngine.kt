/*
 * Copyright (c) 2023 Michael Federczuk
 * SPDX-License-Identifier: MPL-2.0 AND Apache-2.0
 */

package io.github.mfederczuk.gradle.plugin.ktlint.posixshtemplateengine

import java.time.ZonedDateTime
import javax.annotation.CheckReturnValue

@FunctionalInterface
internal fun interface PlaceholderReplacer {

	data class Context(
		val generatedDateTime: ZonedDateTime,
	)

	@CheckReturnValue
	fun get(context: Context): String
}

internal class PosixShTemplateEngine(
	private val placeholderReplacerMap: Map<String, PlaceholderReplacer>,
) {

	companion object {
		private val PLACEHOLDER_NAME_PATTERN: Regex = Regex(pattern = "^[A-Z]([A-Z0-9_]*)*$")
		private val TEMPLATE_PATTERN: Regex = Regex(pattern = "//(.*)::(.*)//")
		private val LEADING_WHITESPACE_PATTERN: Regex = Regex(pattern = "^\\s*")

		fun isValidPlaceholderName(placeholderName: String): Boolean {
			return PLACEHOLDER_NAME_PATTERN.matches(placeholderName)
		}
	}

	private enum class PlaceholderType {
		QUOTED_STRING,
		COMMENT,
	}

	init {
		for (placeholderName: String in this.placeholderReplacerMap.keys) {
			require(placeholderName.isNotEmpty()) {
				"Placeholder name must not be empty"
			}

			require(isValidPlaceholderName(placeholderName)) {
				"Invalid placeholder name \"$placeholderName\""
			}
		}
	}

	@CheckReturnValue
	fun processString(templateString: String): String {
		val generatedDateTime: ZonedDateTime = ZonedDateTime.now()

		return templateString.lineSequence()
			.map { line: String ->
				line
					.replace(TEMPLATE_PATTERN) { matchResult: MatchResult ->
						this.replaceMatch(generatedDateTime, line, matchResult)
					}
			}
			.joinToString(separator = "\n")
	}

	@CheckReturnValue
	private fun replaceMatch(generatedDateTime: ZonedDateTime, line: String, matchResult: MatchResult): String {
		val placeholderName: String = matchResult.groupValues[1]

		check(placeholderName.isNotEmpty()) {
			"Placeholder name must not be empty"
		}

		check(isValidPlaceholderName(placeholderName)) {
			"Invalid placeholder name \"$placeholderName\""
		}

		val placeholderType: PlaceholderType =
			when (val placeHolderTypeString: String = matchResult.groupValues[2]) {
				"" -> error("Placeholder type must not be empty")
				"quoted_string" -> PlaceholderType.QUOTED_STRING
				"comment" -> PlaceholderType.COMMENT
				else -> error("Invalid placeholder type \"$placeHolderTypeString\"")
			}

		val placeholderReplacer: PlaceholderReplacer? = placeholderReplacerMap[placeholderName]

		checkNotNull(placeholderReplacer) {
			"No placeholder replacer registered for placeholder with name \"$placeholderName\""
		}

		val context = PlaceholderReplacer
			.Context(
				generatedDateTime,
			)

		val value: String = placeholderReplacer.get(context)

		return when (placeholderType) {
			PlaceholderType.QUOTED_STRING -> value.quoteForPosixSh()

			PlaceholderType.COMMENT -> {
				val leadingWhitespace: String = LEADING_WHITESPACE_PATTERN.matchAt(line, index = 0)?.value.orEmpty()
				value
					.replace(
						oldValue = "\n",
						newValue = "\n$leadingWhitespace# ",
					)
			}
		}
	}
}

@CheckReturnValue
private fun String.quoteForPosixSh(): String {
	return buildString(capacity = (this.length + 2)) {
		this@buildString.append('\'')
		this@buildString.append(this@quoteForPosixSh.replace(oldValue = "'", newValue = "'\\''"))
		this@buildString.append('\'')
	}
}
