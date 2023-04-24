/*
 * SPDX-License-Identifier: CC0-1.0
 */

package io.github.mfederczuk.gradle.plugin.ktlint

import javax.annotation.CheckReturnValue

private const val REPORT_SENTENCE: String =
	"Please report it at https://github.com/mfederczuk/ktlint-gradle-plugin/issues"

internal val String.internalErrorMsg: String
	@CheckReturnValue
	get() {
		return buildString(capacity = (this.length + 29 + REPORT_SENTENCE.length)) {
			this@buildString.append(this@internalErrorMsg)

			if (this@buildString.endsWith(suffix = "...")) {
				this@buildString
					.replace(
						this@buildString.length - 3,
						this@buildString.length,
						"this is an internal error. ",
					)
			} else {
				require(!(this@buildString.endsWith(suffix = "."))) {
					"Source string must not end with a period.\n" +
						"This is an internal error. $REPORT_SENTENCE"
				}

				this@buildString.append(".\nThis is an internal error. ")
			}

			this@buildString.append(REPORT_SENTENCE)
		}
	}
