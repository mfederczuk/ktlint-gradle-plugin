package io.github.mfederczuk.gradle.plugin.ktlint

import org.gradle.api.plugins.PluginContainer

private const val ANDROID_APPLICATION_PLUGIN_ID: String = "com.android.application"
private const val ANDROID_LIBRARY_PLUGIN_ID: String = "com.android.library"

internal fun PluginContainer.hasAndroidPlugins(): Boolean {
	return (this.hasPlugin(ANDROID_APPLICATION_PLUGIN_ID) || this.hasPlugin(ANDROID_LIBRARY_PLUGIN_ID))
}
