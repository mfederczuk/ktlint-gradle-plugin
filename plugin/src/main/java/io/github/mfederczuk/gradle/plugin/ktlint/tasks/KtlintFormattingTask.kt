package io.github.mfederczuk.gradle.plugin.ktlint.tasks

import io.github.mfederczuk.gradle.plugin.ktlint.KtlintUtils
import io.github.mfederczuk.gradle.plugin.ktlint.PluginExtensionUtils
import io.github.mfederczuk.gradle.plugin.ktlint.configuration.CodeStyle
import io.github.mfederczuk.gradle.plugin.ktlint.configuration.ErrorLimit
import io.github.mfederczuk.gradle.plugin.ktlint.configuration.PluginConfiguration
import io.github.mfederczuk.gradle.plugin.ktlint.configuration.toConfiguration
import io.github.mfederczuk.gradle.plugin.ktlint.utils.internalErrorMsg
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.File
import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.jar.Manifest
import javax.annotation.CheckReturnValue
import javax.inject.Inject

private data class KotlinFile(
	val source: File,
	val intermediate: File,
	val target: File,
)

/**
 * A gradle task that formats Kotlin source files using ktlint.
 *
 * The directory where the input source files are found is configured via the [inputDir] property.
 * The directory where the formatted output files are to be written to is configured via the [outputDir] property.
 */
public abstract class KtlintFormattingTask : DefaultTask() {

	@get:Inject
	internal abstract val execOperations: ExecOperations

	@get:Inject
	internal abstract val projectLayout: ProjectLayout

	init {
		this.description = "Formats Kotlin source files using ktlint"
	}

	@get:InputFiles
	@get:Classpath
	internal val ktlintClasspath: Provider<FileCollection>

	@get:Input
	internal val codeStyle: Provider<CodeStyle>

	@get:Input
	internal val errorLimit: Provider<ErrorLimit>

	@get:Input
	internal val experimentalRulesEnabled: Provider<Boolean>

	init {
		val configurationProvider: Provider<PluginConfiguration> = PluginExtensionUtils.getExtension(this.project)
			.toConfiguration(this.project.providers)

		this.ktlintClasspath = configurationProvider
			.map { configuration: PluginConfiguration ->
				KtlintUtils.resolveKtlintClasspath(configuration.ktlintVersion, this.project)
			}

		this.codeStyle = configurationProvider
			.map(PluginConfiguration::codeStyle)

		this.errorLimit = configurationProvider
			.map(PluginConfiguration::errorLimit)

		this.experimentalRulesEnabled = configurationProvider
			.map(PluginConfiguration::experimentalRulesEnabled)
	}

	@get:InputDirectory
	@get:SkipWhenEmpty
	@get:IgnoreEmptyDirectories
	internal val inputDirectory: DirectoryProperty = this.project.objects.directoryProperty()

	@get:OutputDirectory
	internal val outputDirectory: DirectoryProperty = this.project.objects.directoryProperty()

	init {
		this.outputDirectory.convention(this.inputDirectory)
	}

	@get:InputFiles
	@get:SkipWhenEmpty
	internal val kotlinFileTree: FileTree = this.inputDirectory.asFileTree
		.matching {
			this@matching.include("**/*.kt", "**/*.kts")
		}

	/**
	 * Sets the input directory where the Kotlin source files are located.
	 *
	 * @see [Project.file]
	 */
	@get:Internal
	public var inputDir: Any
		get() {
			return this.inputDirectory.get().asFile.toString()
		}
		set(value) {
			this.inputDirectory.set(this.project.file(value))
		}

	/**
	 * Sets the output directory where the formatted Kotlin files are copied to.
	 *
	 * The default values is the same that is set to [inputDir].
	 *
	 * @see [Project.file]
	 */
	@get:Internal
	public var outputDir: Any
		get() {
			return this.outputDirectory.get().asFile.toString()
		}
		set(value) {
			this.outputDirectory.set(this.project.file(value))
		}

	@TaskAction
	internal fun formatFiles() {
		val ktlintClasspath: FileCollection = this.ktlintClasspath.get()
		val codeStyle: CodeStyle = this.codeStyle.get()
		val errorLimit: ErrorLimit = this.errorLimit.get()
		val experimentalRulesEnabled: Boolean = this.experimentalRulesEnabled.get()

		val inputDirectory: File = this.inputDirectory.asFile.get()
		val outputDirectory: File = this.outputDirectory.asFile.get()

		val intermediateDirectory: File = this.projectLayout.buildDirectory
			.dir("ktlint")
			.map { buildKtlintDir: Directory ->
				buildKtlintDir
					.dir("formatting")
					.dir("intermediate")
			}
			.get()
			.asFile

		if (this.kotlinFileTree.isEmpty) {
			return
		}

		val files: List<KotlinFile> = this.kotlinFileTree
			.map { sourceFile: File ->
				val relative: File = sourceFile.relativeTo(inputDirectory)
				KotlinFile(
					sourceFile,
					intermediate = intermediateDirectory.resolve(relative),
					target = outputDirectory.resolve(relative),
				)
			}

		for (kotlinFile: KotlinFile in files) {
			kotlinFile.source.copyTo(kotlinFile.intermediate, overwrite = true)
		}

		this.execOperations
			.javaexec {
				this@javaexec.classpath = ktlintClasspath
				this@javaexec.mainClass.set(ktlintClasspath.files.extractClasspathMainClassName())

				this@javaexec.standardInput = files
					.joinToString(separator = "\u0000") { it.intermediate.toString() }
					.byteInputStream(charset = Charsets.UTF_8)

				this@javaexec.standardOutput = System.err
				this@javaexec.errorOutput = System.err

				this@javaexec.args =
					buildList {
						when (codeStyle) {
							is CodeStyle.Default -> Unit
							is CodeStyle.Specific -> this@buildList.add("--code-style=${codeStyle.name}")
						}

						when (errorLimit) {
							is ErrorLimit.None -> Unit
							is ErrorLimit.Max -> this@buildList.add("--limit=${errorLimit.n}")
						}

						if (experimentalRulesEnabled) {
							this@buildList.add("--experimental")
						}

						this@buildList.add("--patterns-from-stdin=''")
						this@buildList.add("--format")
					}
			}
			.assertNormalExitValue()
			.rethrowFailure()

		for (kotlinFile: KotlinFile in files) {
			kotlinFile.intermediate.copyTo(kotlinFile.target, overwrite = true)
		}
	}
}

@CheckReturnValue
private fun Iterable<File>.extractClasspathMainClassName(): String {
	var classpathMainClassName: String? = null

	for (file: File in this) {
		val fileMainClassName: String = file.extractJarFileMainClassName()
			?: break

		check(classpathMainClassName == null) {
			"Classpath $this contains multiple main classes".internalErrorMsg
		}

		classpathMainClassName = fileMainClassName
	}

	checkNotNull(classpathMainClassName) {
		"Classpath $this contains no main class".internalErrorMsg
	}

	return classpathMainClassName
}

@CheckReturnValue
private fun File.extractJarFileMainClassName(): String? {
	val jarFile = JarFile(this)

	val manifest: Manifest? = jarFile.manifest
	checkNotNull(manifest) {
		"JAR $this has no manifest".internalErrorMsg
	}

	return manifest.mainAttributes.getValue(Attributes.Name.MAIN_CLASS)
}
