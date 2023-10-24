package io.github.mfederczuk.gradle.plugin.ktlint.models

internal sealed class CodeStyle {

	object Default : CodeStyle()

	data class Specific(val name: String) : CodeStyle()
}
