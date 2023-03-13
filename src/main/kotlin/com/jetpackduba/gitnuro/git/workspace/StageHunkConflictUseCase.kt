package com.jetpackduba.gitnuro.git.workspace

import com.jetpackduba.gitnuro.extensions.lineDelimiter
import com.jetpackduba.gitnuro.extensions.toStatusType
import com.jetpackduba.gitnuro.git.EntryContent
import com.jetpackduba.gitnuro.git.RawFileManager
import com.jetpackduba.gitnuro.git.diff.Hunk
import com.jetpackduba.gitnuro.git.diff.Line
import com.jetpackduba.gitnuro.git.diff.LineType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import java.io.File
import java.io.FileWriter
import java.nio.ByteBuffer
import javax.inject.Inject

class StageHunkConflictUseCase @Inject constructor(
    private val rawFileManager: RawFileManager,
    private val getLinesFromRawTextUseCase: GetLinesFromRawTextUseCase,
    private val getLinesFromTextUseCase: GetLinesFromTextUseCase,
) {
    suspend operator fun invoke(git: Git, diffEntry: DiffEntry, hunk: Hunk, lines: List<Line>) =
        withContext(Dispatchers.IO) {
            val repository = git.repository
            val dirCache = repository.lockDirCache()
            val dirCacheEditor = dirCache.editor()
            var completedWithErrors = true

            try {
                val entryContent = rawFileManager.getRawContent(
                    repository = git.repository,
                    side = DiffEntry.Side.OLD,
                    entry = diffEntry,
                    oldTreeIterator = null,
                    newTreeIterator = null
                )

                if (entryContent !is EntryContent.Text)
                    return@withContext

                // The following changes are done in the GIT file
                var textLines = getLinesFromRawTextUseCase(entryContent.rawText).toMutableList()

                var hunkLines = hunk.lines.filter { it.lineType != LineType.CONTEXT }
                hunkLines = hunkLines.filter { lines.contains(it) }

                var linesAdded = 0
                for (line in hunkLines) {
                    when (line.lineType) {
                        LineType.ADDED -> {
                            textLines.add(line.oldLineNumber + linesAdded, line.text)
                            linesAdded++
                        }

                        LineType.REMOVED -> {
                            textLines.removeAt(line.oldLineNumber + linesAdded)
                            linesAdded--
                        }

                        else -> throw NotImplementedError("Line type not implemented for stage hunk")
                    }
                }

                var stagedFileText = textLines.joinToString("")
                dirCacheEditor.add(
                    HunkEdit(
                        diffEntry.newPath,
                        repository,
                        ByteBuffer.wrap(stagedFileText.toByteArray())
                    )
                )
                dirCacheEditor.commit()

                // The following changes are done in the SOURCE file
                // remove the conflicted lines that git puts in automatically
                // It would be nicer to reuse the code of ResetHunkUseCase but would need to pass lines there

                val file = File(repository.directory.parent, diffEntry.oldPath)

                val content = file.readText()
                textLines = getLinesFromTextUseCase(content, content.lineDelimiter).toMutableList()
                // get all the hunklines except the ones I did stage
                hunkLines = hunk.lines.filter { it.lineType != LineType.CONTEXT && !lines.contains(it)}

                val addedLines = hunkLines
                    .filter { it.lineType == LineType.ADDED }
                    .sortedBy { it.newLineNumber }
                val removedLines = hunkLines
                    .filter { it.lineType == LineType.REMOVED }
                    .sortedBy { it.newLineNumber }

                var linesRemoved = 0

                // Start by removing the added lines to the index
                for (line in addedLines) {
                    textLines.removeAt(line.newLineNumber + linesRemoved)
                    linesRemoved--
                }

                linesAdded = 0

                //Restore previously removed lines to the index
                for (line in removedLines) {
                    // Check how many lines before this one have been deleted
                    val previouslyRemovedLines = addedLines.count { it.newLineNumber < line.newLineNumber }
                    textLines.add(line.newLineNumber + linesAdded - previouslyRemovedLines, line.text)
                    linesAdded++
                }

                stagedFileText = textLines.joinToString("")

                FileWriter(file).use { fw ->
                    fw.write(stagedFileText)
                }

                git.add()
                    .addFilepattern(diffEntry.oldPath)
                    .setUpdate(true)
                    .call()

                completedWithErrors = false
            } finally {
                if (completedWithErrors)
                    dirCache.unlock()
            }
        }
}