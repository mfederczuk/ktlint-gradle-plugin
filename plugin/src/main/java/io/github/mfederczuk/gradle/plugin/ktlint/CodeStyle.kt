package io.github.mfederczuk.gradle.plugin.ktlint

internal sealed class CodeStyle {

	object Default : CodeStyle()

	data class Specific(val name: String) : CodeStyle()
}
