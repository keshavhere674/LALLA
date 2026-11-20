package app.lalla.data.tasks.executors

import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import java.io.File

object FileSearchTaskExecutor {

    fun execute(context: Context, query: String?): TaskExecutionResult {
        val cleanQuery = query?.trim()?.removePrefix("search ")?.removePrefix("find ")
            ?.removeSuffix(" file")?.removeSuffix(" folder")?.trim()

        if (cleanQuery.isNullOrBlank()) {
            return TaskExecutionResult(
                isSuccess = false,
                method = "Validation",
                spokenFeedback = "Kripya batayein kaunsi file ya folder dhoondhna hai.",
                failureReason = "Empty search query"
            )
        }

        // Primary: Query MediaStore indexed file catalog
        val mediaStoreResults = searchMediaStore(context, cleanQuery)
        if (mediaStoreResults.isNotEmpty()) {
            val count = mediaStoreResults.size
            val fileNames = mediaStoreResults.take(3).joinToString(", ") { it.first }
            val firstLocation = mediaStoreResults.first().second

            return TaskExecutionResult(
                isSuccess = true,
                method = "MediaStore Index Search",
                spokenFeedback = "Mujhe $count file mili: $fileNames. Location: $firstLocation"
            )
        }

        // Fallback: File system search in standard public directories
        val fsResults = searchPublicDirectories(cleanQuery)
        if (fsResults.isNotEmpty()) {
            val count = fsResults.size
            val fileNames = fsResults.take(3).joinToString(", ") { it.name }
            val firstLocation = fsResults.first().parent ?: "Storage"

            return TaskExecutionResult(
                isSuccess = true,
                method = "FileSystem Public Dirs (Fallback)",
                spokenFeedback = "Public storage mein $count file mili: $fileNames. Location: $firstLocation",
                fallbackAttempted = true,
                failureReason = "MediaStore index returned 0 results"
            )
        }

        return TaskExecutionResult(
            isSuccess = true,
            method = "FileSearch (Both checked)",
            spokenFeedback = "\"$cleanQuery\" naam ki koi file ya folder nahi mila.",
            fallbackAttempted = true
        )
    }

    private fun searchMediaStore(context: Context, query: String): List<Pair<String, String>> {
        val results = mutableListOf<Pair<String, String>>()
        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.RELATIVE_PATH
        )
        val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$query%")

        try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val pathIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.RELATIVE_PATH)

                while (cursor.moveToNext() && results.size < 10) {
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "File" else "File"
                    val path = if (pathIdx >= 0) cursor.getString(pathIdx) ?: "Storage" else "Storage"
                    results.add(name to path)
                }
            }
        } catch (_: Exception) {}

        return results
    }

    private fun searchPublicDirectories(query: String): List<File> {
        val results = mutableListOf<File>()
        val publicDirs = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        )

        val lowerQuery = query.lowercase()
        for (dir in publicDirs) {
            if (dir != null && dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    if (file.name.lowercase().contains(lowerQuery)) {
                        results.add(file)
                        if (results.size >= 10) return results
                    }
                }
            }
        }
        return results
    }
}
