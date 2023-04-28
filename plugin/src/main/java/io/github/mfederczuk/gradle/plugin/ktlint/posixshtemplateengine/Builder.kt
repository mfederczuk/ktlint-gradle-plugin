/*
 * Copyright (c) 2023 Michael Federczuk
 * SPDX-License-Identifier: MPL-2.0 AND Apache-2.0
 */

package io.github.mfederczuk.gradle.plugin.ktlint.posixshtemplateengine

import javax.annotation.CheckReturnValue

private class PosixShTemplateEngineBuilderImpl : PosixShTemplateEngineBuilder {

	private val placeholderReplacerMap: MutableMap<String, PlaceholderReplacer> = HashMap()

	private inner class ReplaceImpl : PosixShTemplateEngineBuilder.Replace {

		private inner class PlaceholderImpl(private val name: String) : PosixShTemplateEngineBuilder.Replace.Placeholder {

			override fun with(value: String) {
				this@PlaceholderImpl.addReplacer { value }
			}

			override fun with(lazy: Lazy<String>) {
				this@PlaceholderImpl.addReplacer(lazy::value)
			}

			private fun addReplacer(block: () -> String) {
				this@PosixShTemplateEngineBuilderImpl.placeholderReplacerMap[this.name] = PlaceholderReplacer(block)
			}
		}

		@CheckReturnValue
		override fun placeholder(name: String): PosixShTemplateEngineBuilder.Replace.Placeholder {
			require(name.isNotEmpty()) {
				"Placeholder name must not be empty"
			}

			require(PosixShTemplateEngine.isValidPlaceholderName(name)) {
				"Invalid placeholder name \"$name\""
			}

			return PlaceholderImpl(name)
		}
	}

	override val replace: PosixShTemplateEngineBuilder.Replace = ReplaceImpl()

	@CheckReturnValue
	fun build(): PosixShTemplateEngine {
		return PosixShTemplateEngine(this.placeholderReplacerMap)
	}
}

internal interface PosixShTemplateEngineBuilder {

	interface Replace {
		interface Placeholder {
			infix fun with(value: String)
			infix fun with(lazy: Lazy<String>)
		}

		@CheckReturnValue
		infix fun placeholder(name: String): Placeholder
	}

	val replace: Replace
}

@CheckReturnValue
internal fun buildPosixShTemplateEngine(
	block: PosixShTemplateEngineBuilder.() -> Unit,
): PosixShTemplateEngine {
	val builder = PosixShTemplateEngineBuilderImpl()
	with(builder, block)
	return builder.build()
}
