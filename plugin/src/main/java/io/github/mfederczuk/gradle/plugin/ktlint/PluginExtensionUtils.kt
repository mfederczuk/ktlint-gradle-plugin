/*
 * Copyright (c) 2023 Michael Federczuk
 * SPDX-License-Identifier: MPL-2.0 AND Apache-2.0
 */

package io.github.mfederczuk.gradle.plugin.ktlint

import io.github.mfederczuk.gradle.plugin.ktlint.utils.internalErrorMsg
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import javax.annotation.CheckReturnValue

internal object PluginExtensionUtils {

	const val EXTENSION_NAME: String = "ktlint"

	@CheckReturnValue
	fun createExtension(project: Project): KtlintPluginExtension {
		val extension: KtlintPluginExtension = project.extensions.create<KtlintPluginExtension>(name = EXTENSION_NAME)

		extension.experimental.convention(false)
		extension.installGitPreCommitHookBeforeBuild.convention(false)

		return extension
	}

	@CheckReturnValue
	fun getExtension(project: Project): KtlintPluginExtension {
		val extension: Any? = project.extensions.findByName(EXTENSION_NAME)

		checkNotNull(extension) {
			"Extension with name \"$EXTENSION_NAME\" not found in $project".internalErrorMsg
		}

		check(extension is KtlintPluginExtension) {
			"Extension with name \"$EXTENSION_NAME\" in $project is not of type ${KtlintPluginExtension::class.java}"
				.internalErrorMsg
		}

		return extension
	}
}
