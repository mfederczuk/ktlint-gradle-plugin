/*
 * Copyright (c) 2023 Michael Federczuk
 * SPDX-License-Identifier: MPL-2.0 AND Apache-2.0
 */

package io.github.mfederczuk.shtemplate

import java.time.ZonedDateTime
import javax.annotation.CheckReturnValue

internal class ShTemplateEngine(
	private val replacementValueSuppliers: Map<PlaceholderName, ReplacementValueSupplier>,
) {

	private companion object {
		private val PLACEHOLDER_PATTERN: Regex = Regex(pattern = "//([A-Za-z0-9_]+)::([A-Za-z0-9_]+)//")
		private val LEADING_WHITESPACE_PATTERN: Regex = Regex(pattern = "^\\s*")
	}

	// TODO: at some point replace
	//               fun processString(String): String
	//       with
	//               fun processStream(InputStream): InputStream
	//       that evaluates everything lazily for better performance and memory usage

	@CheckReturnValue
	fun processString(templateString: String): String {
		val generatedDateTime: ZonedDateTime = ZonedDateTime.now()

		return templateString.lineSequence()
			.map { line: String ->
				line.replace(PLACEHOLDER_PATTERN) { match: MatchResult ->
					this.expandPlaceholderMatch(generatedDateTime, line, match)
				}
			}
			.joinToString(separator = "\n")
	}

	@CheckReturnValue
	private fun expandPlaceholderMatch(
		generatedDateTime: ZonedDateTime,
		line: String,
		match: MatchResult,
	): String {
		val placeholder: Placeholder = this.extractPlaceholderFromMatch(match)

		val replacementValueSupplier: ReplacementValueSupplier? = this.replacementValueSuppliers[placeholder.name]
		checkNotNull(replacementValueSupplier) {
			"No replacement registered for placeholder with name \"${placeholder.name}\""
		}

		return when (placeholder.type) {
			PlaceholderType.QUOTED_STRING -> {
				check(replacementValueSupplier is ReplacementValueSupplier.QuotedString) {
					"Registered replacement for placeholder with name \"${placeholder.name}\" is of the wrong type"
				}

				replacementValueSupplier.getString().quotedForSh()
			}

			PlaceholderType.ARGS -> {
				check(replacementValueSupplier is ReplacementValueSupplier.Args) {
					"Registered replacement for placeholder with name \"${placeholder.name}\" is of the wrong type"
				}

				val leadingWhitespace: String = LEADING_WHITESPACE_PATTERN.matchAt(line, index = 0)?.value.orEmpty()
				replacementValueSupplier.getArgs()
					.joinToString(
						separator = " \\\n$leadingWhitespace",
						transform = String::quotedForSh,
					)
			}

			PlaceholderType.COMMENT_TEXT -> {
				check(replacementValueSupplier is ReplacementValueSupplier.CommentText) {
					"Registered replacement for placeholder with name \"${placeholder.name}\" is of the wrong type"
				}

				val leadingWhitespace: String = LEADING_WHITESPACE_PATTERN.matchAt(line, index = 0)?.value.orEmpty()
				replacementValueSupplier.getText(generatedDateTime)
					.replace(
						oldValue = "\n",
						newValue = "\n$leadingWhitespace# ",
					)
			}
		}
	}

	@CheckReturnValue
	private fun extractPlaceholderFromMatch(match: MatchResult): Placeholder {
		return Placeholder(
			this.extractPlaceholderNameFromMatch(match),
			this.extractPlaceholderTypeFromMatch(match),
		)
	}

	@CheckReturnValue
	private fun extractPlaceholderNameFromMatch(match: MatchResult): PlaceholderName {
		val nameStr: String = match.groupValues[1]

		check(PlaceholderName.isValid(nameStr)) {
			"Placeholder name (\"$nameStr\") is invalid"
		}

		return PlaceholderName.ofString(nameStr)
	}

	@CheckReturnValue
	private fun extractPlaceholderTypeFromMatch(match: MatchResult): PlaceholderType {
		val typeIdentifier: String = match.groupValues[2]

		val type: PlaceholderType? = PlaceholderType.ofIdentifierOrNull(typeIdentifier)

		checkNotNull(type) {
			"Placeholder type with identifier \"$typeIdentifier\" does not exist"
		}

		return type
	}
}

@CheckReturnValue
private fun String.quotedForSh(): String {
	return buildString(capacity = (this.length + 2)) {
		this@buildString.append('\'')
		this@buildString.append(this@quotedForSh.replace(oldValue = "'", newValue = "'\\''"))
		this@buildString.append('\'')
	}
}
