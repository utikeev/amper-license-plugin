import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.amper.plugins.Input
import org.jetbrains.amper.plugins.ModuleSources
import org.jetbrains.amper.plugins.TaskAction
import java.nio.file.FileVisitResult
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.readText
import kotlin.io.path.visitFileTree
import kotlin.io.path.writeText
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val defaultProcessStartTag = "#%L"
private const val defaultDelimiter = "%%"
private const val defaultProcessEndTag = "#L%"

@TaskAction
fun checkFileHeaders(
    @Input sources: ModuleSources,
    settings: HeaderSettings,
) {
    processFileHeaders(sources, settings, isDryRun = true)
}

@TaskAction
fun updateFileHeaders(
    @Input sources: ModuleSources,
    settings: HeaderSettings,
) {
    processFileHeaders(sources, settings, isDryRun = false)
}

@TaskAction
fun removeFileHeaders(
    @Input sources: ModuleSources,
) {
    val updatedFiles = mutableListOf<Path>()
    sources.sourceDirectories.forEach { directory ->
        directory.visitFileTree {
            onVisitFile { file, _ ->
                if (file.extension != "kt" && file.extension != "java") return@onVisitFile FileVisitResult.CONTINUE

                val oldContent = file.readText()
                var metFirstComment = false
                val existingFirstComment = buildList {
                    for (line in oldContent.lines()) {
                        val trimmedLine = line.trim()
                        if (trimmedLine.startsWith("/*")) metFirstComment = true
                        if (metFirstComment) add(line)
                        if (trimmedLine.startsWith("*/")) break
                    }
                }
                if (existingFirstComment.size < 2) return@onVisitFile FileVisitResult.CONTINUE
                val possibleStartLine = existingFirstComment[1].trim()
                if (!possibleStartLine.startsWith("* $defaultProcessStartTag")) return@onVisitFile FileVisitResult.CONTINUE
                val possibleEndLine = existingFirstComment[existingFirstComment.size - 2].trim()
                if (!possibleEndLine.startsWith("* $defaultProcessEndTag")) return@onVisitFile FileVisitResult.CONTINUE

                updatedFiles.add(file)
                val stringToDelete = existingFirstComment.joinToString("\n") + "\n"
                val newContent = oldContent.replace(stringToDelete, "")
                file.writeText(newContent)

                FileVisitResult.CONTINUE
            }
        }
    }
    if (updatedFiles.isEmpty()) println("All files have no header")
    else println("Removed header from ${updatedFiles.size} files")
}

@OptIn(ExperimentalTime::class)
private fun processFileHeaders(
    sources: ModuleSources,
    settings: HeaderSettings,
    isDryRun: Boolean,
) {
    val license = loadLicenses().firstOrNull { it.name == settings.licenseName }
    checkNotNull(license) { "License '${settings.licenseName}' not found" }

    val now = Clock.System.now()
    val currentYear = now.toLocalDateTime(TimeZone.currentSystemDefault()).date.year

    val yearString = if (currentYear == settings.inceptionYear) "$currentYear" else "${settings.inceptionYear}–${currentYear}"

    val newHeader = buildString {
        appendLine("/*")
        appendLine(" * $defaultProcessStartTag")
        appendLine(" * $defaultDelimiter")
        appendLine(" * Copyright (C) $yearString ${settings.organizationName}")
        appendLine(" * $defaultDelimiter")
        for (line in license.headerContent.lines()) appendLine(" * $line")
        appendLine(" * $defaultProcessEndTag")
        appendLine(" */")
    }.trim()
    val filesToUpdate = mutableListOf<Path>()

    sources.sourceDirectories.forEach { directory ->
        directory.visitFileTree {
            onVisitFile { file, _ ->
                if (file.extension != "kt" && file.extension != "java") return@onVisitFile FileVisitResult.CONTINUE

                val oldContent = file.readText()
                var metFirstComment = false
                val existingFirstComment = buildString {
                    for (line in oldContent.lines()) {
                        val trimmedLine = line.trim()
                        if (trimmedLine.startsWith("/*")) metFirstComment = true
                        if (metFirstComment) appendLine(line)
                        if (trimmedLine.startsWith("*/")) break
                    }
                }.trim()
                if (existingFirstComment == newHeader) return@onVisitFile FileVisitResult.CONTINUE

                filesToUpdate.add(file)
                if (!isDryRun) {
                    val newContent = newHeader + "\n" + oldContent.replace(existingFirstComment, "")
                    file.writeText(newContent)
                }

                FileVisitResult.CONTINUE
            }
        }
    }

    if (filesToUpdate.isEmpty()) {
        println("All files have correct copyright header")
        return
    } else {
        val message = buildString {
            if (isDryRun) {
                appendLine("Following ${filesToUpdate.size} files have incorrect copyright header:")
            } else {
                appendLine("Updated header in following ${filesToUpdate.size} files:")
            }
            filesToUpdate.forEach { appendLine(it) }
        }
        if (isDryRun && settings.onMissingHeader == MissingHeaderAction.Fail) {
            error(message)
        } else {
            println(message)
        }
    }
}