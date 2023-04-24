/*
 * Copyright (c) 2023 Michael Federczuk
 * SPDX-License-Identifier: MPL-2.0 AND Apache-2.0
 */

package io.github.mfederczuk.gradle.plugin.ktlint.posixshtemplateengine

import javax.annotation.CheckReturnValue

private class PosixShTemplateEngineBuilderImpl : PosixShTemplateEngineBuilder {

	private val placeholderReplacerMap: MutableMap<String, PlaceholderReplacer> = HashMap()

	private inner class ReplaceImpl(private val placeholderName: String) : PosixShTemplateEngineBuilder.Replace {

		override fun with(value: String) {
			this@PosixShTemplateEngineBuilderImpl.placeholderReplacerMap[this.placeholderName] =
				PlaceholderReplacer { value }
		}

		override fun with(lazy: Lazy<String>) {
			this@PosixShTemplateEngineBuilderImpl.placeholderReplacerMap[this.placeholderName] =
				PlaceholderReplacer(lazy::value)
		}
	}

	@CheckReturnValue
	override fun replace(placeholderName: String): PosixShTemplateEngineBuilder.Replace {
		require(placeholderName.isNotEmpty()) {
			"Placeholder name must not be empty"
		}

		require(PosixShTemplateEngine.isValidPlaceholderName(placeholderName)) {
			"Invalid placeholder name \"$placeholderName\""
		}

		return ReplaceImpl(placeholderName)
	}

	@CheckReturnValue
	fun build(): PosixShTemplateEngine {
		return PosixShTemplateEngine(this.placeholderReplacerMap)
	}
}

internal interface PosixShTemplateEngineBuilder {

	interface Replace {
		infix fun with(value: String)
		infix fun with(lazy: Lazy<String>)
	}

	@CheckReturnValue
	fun replace(placeholderName: String): Replace
}

@CheckReturnValue
internal fun buildPosixShTemplateEngine(
	block: PosixShTemplateEngineBuilder.() -> Unit,
): PosixShTemplateEngine {
	val builder = PosixShTemplateEngineBuilderImpl()
	with(builder, block)
	return builder.build()
}
