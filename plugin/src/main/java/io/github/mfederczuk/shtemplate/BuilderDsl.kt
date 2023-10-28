/*
 * Copyright (c) 2023 Michael Federczuk
 * SPDX-License-Identifier: MPL-2.0 AND Apache-2.0
 */

package io.github.mfederczuk.shtemplate

import javax.annotation.CheckReturnValue

internal class ShTemplateEngineBuilderDsl(builder: ShTemplateEngineBuilder) {

	class Replace(private val builder: ShTemplateEngineBuilder) {

		class Placeholder(
			private val builder: ShTemplateEngineBuilder,
			private val name: PlaceholderName,
		) {

			object QuotedStringType

			object CommentTextType

			class OfTypeQuotedString(
				private val builder: ShTemplateEngineBuilder,
				private val placeholderName: PlaceholderName,
			) {

				infix fun with(string: String) {
					this.builder.quotedString(this.placeholderName, string)
				}
			}

			class OfTypeCommentText(
				private val builder: ShTemplateEngineBuilder,
				private val placeholderName: PlaceholderName,
			) {

				object GeneratedDateTime

				infix fun with(
					@Suppress("UNUSED_PARAMETER") generatedDateTime: GeneratedDateTime,
				) {
					this.builder.commentTextGeneratedDateTime(this.placeholderName)
				}
			}

			infix fun ofType(
				@Suppress("UNUSED_PARAMETER") type: QuotedStringType,
			): OfTypeQuotedString {
				return OfTypeQuotedString(this.builder, this.name)
			}

			infix fun ofType(
				@Suppress("UNUSED_PARAMETER") type: CommentTextType,
			): OfTypeCommentText {
				return OfTypeCommentText(this.builder, this.name)
			}
		}

		infix fun placeholder(name: String): Placeholder {
			return Placeholder(this.builder, PlaceholderName.ofString(name))
		}
	}

	val replace: Replace = Replace(builder)

	val quotedString: Replace.Placeholder.QuotedStringType = Replace.Placeholder.QuotedStringType
	val commentText: Replace.Placeholder.CommentTextType = Replace.Placeholder.CommentTextType

	val generatedDateTime: Replace.Placeholder.OfTypeCommentText.GeneratedDateTime =
		Replace.Placeholder.OfTypeCommentText.GeneratedDateTime
}

@CheckReturnValue
internal fun buildShTemplateEngine(dslBlock: ShTemplateEngineBuilderDsl.() -> Unit): ShTemplateEngine {
	val builder = ShTemplateEngineBuilder()

	dslBlock(ShTemplateEngineBuilderDsl(builder))

	return builder.build()
}
