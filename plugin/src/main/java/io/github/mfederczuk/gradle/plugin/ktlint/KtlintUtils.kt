/*
 * Copyright (c) 2023 Michael Federczuk
 * SPDX-License-Identifier: MPL-2.0 AND Apache-2.0
 */

package io.github.mfederczuk.gradle.plugin.ktlint

import io.github.mfederczuk.gradle.plugin.ktlint.utils.internalErrorMsg
import net.swiftzer.semver.SemVer
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Provider
import java.io.File
import javax.annotation.CheckReturnValue

internal object KtlintUtils {

	private val KTLINT_NEW_COORDINATES_VERSION: SemVer = SemVer(1, 0, 0)

	private const val KTLINT_OLD_GROUP_AND_ARTIFACT_COORDINATES_STRING: String = "com.pinterest:ktlint"
	private const val KTLINT_NEW_GROUP_AND_ARTIFACT_COORDINATES_STRING: String = "com.pinterest.ktlint:ktlint-cli"

	@CheckReturnValue
	internal fun resolveKtlintClasspath(
		ktlintVersion: SemVer,
		project: Project,
	): FileCollection {
		val ktlintCoordinatesString: String = this.makeCoordinatesString(ktlintVersion)

		val configuration: Configuration = this.createDependencyConfiguration(ktlintCoordinatesString, project)
		val filesSetProvider: Provider<Set<File>> = project
			.provider {
				try {
					configuration.resolve()
				} catch (_: ResolveException) {
					// don't add this caught exception as a cause of another exception; Gradle won't show the message of
					// the parent exception but instead only the message of the cause exceptions

					val msg: String = "Could not resolve the dependency \"$ktlintCoordinatesString\".\n" +
						"Either the requested version ($ktlintVersion) does not exist or " +
						"Maven Central is missing from the dependency repositories.\n" +
						"If it's neither of those causes, then ...".internalErrorMsg
					error(msg)
				}
			}

		return project.files(filesSetProvider)
	}

	@CheckReturnValue
	private fun makeCoordinatesString(ktlintVersion: SemVer): String {
		val ktlintGroupAndArtifactCoordinatesString: String =
			if (ktlintVersion >= KTLINT_NEW_COORDINATES_VERSION) {
				KTLINT_NEW_GROUP_AND_ARTIFACT_COORDINATES_STRING
			} else {
				KTLINT_OLD_GROUP_AND_ARTIFACT_COORDINATES_STRING
			}

		return "$ktlintGroupAndArtifactCoordinatesString:$ktlintVersion"
	}

	@CheckReturnValue
	private fun createDependencyConfiguration(
		ktlintCoordinatesString: String,
		project: Project,
	): Configuration {
		val ktlintDependency: Dependency = project.dependencies.create(ktlintCoordinatesString)
		return project.configurations.detachedConfiguration(ktlintDependency)
	}
}
