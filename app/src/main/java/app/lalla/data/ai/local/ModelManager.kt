package app.lalla.data.ai.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

enum class ModelStatus {
    NOT_FOUND,
    VALIDATING,
    READY,
    ERROR
}

data class ModelInfo(
    val fileName: String,
    val filePath: String,
    val fileSizeMb: Long,
    val status: ModelStatus,
    val errorMessage: String? = null
)

@Singleton
class ModelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val DEFAULT_MODEL_NAME = "qwen2.5-0.5b-instruct-q4_k_m.gguf"
        private const val GGUF_MAGIC = "GGUF"
    }

    /**
     * Resolves the app-specific model directory.
     * Uses getExternalFilesDir("models") or falls back to internal filesDir/models.
     */
    fun getModelsDirectory(): File {
        val internalDir = File(context.filesDir, "models")
        if (!internalDir.exists()) internalDir.mkdirs()
        return internalDir
    }

    /**
     * Returns the active model file if it exists and passes validation.
     */
    fun getModelFile(modelName: String = DEFAULT_MODEL_NAME): File? {
        // 1. Check app private internal filesDir/models/ first (safest, zero permission restrictions)
        val internalFile = File(File(context.filesDir, "models"), modelName)
        if (internalFile.exists() && validateGguf(internalFile)) {
            return internalFile
        }

        // 2. Check external files dir as fallback
        val externalDir = context.getExternalFilesDir("models")
        if (externalDir != null && externalDir.exists()) {
            val externalFile = File(externalDir, modelName)
            if (externalFile.exists() && validateGguf(externalFile)) {
                return externalFile
            }
        }

        return null
    }

    /**
     * Inspects the model status.
     */
    fun getModelInfo(modelName: String = DEFAULT_MODEL_NAME): ModelInfo {
        val file = getModelFile(modelName)
        return if (file != null && file.exists()) {
            val sizeMb = file.length() / (1024 * 1024)
            ModelInfo(
                fileName = file.name,
                filePath = file.absolutePath,
                fileSizeMb = sizeMb,
                status = ModelStatus.READY
            )
        } else {
            val targetFile = File(getModelsDirectory(), modelName)
            val exists = targetFile.exists()
            val errorMsg = if (exists) "File exists but is not a valid GGUF binary" else null
            ModelInfo(
                fileName = modelName,
                filePath = targetFile.absolutePath,
                fileSizeMb = if (exists) targetFile.length() / (1024 * 1024) else 0L,
                status = if (exists) ModelStatus.ERROR else ModelStatus.NOT_FOUND,
                errorMessage = errorMsg
            )
        }
    }

    /**
     * Validates that the file has the standard 'GGUF' 4-byte magic header and size > 20MB.
     */
    fun validateGguf(file: File): Boolean {
        if (!file.exists() || file.length() < 20 * 1024 * 1024) return false
        return try {
            FileInputStream(file).use { stream ->
                val header = ByteArray(4)
                val read = stream.read(header)
                read == 4 && String(header, Charsets.US_ASCII) == GGUF_MAGIC
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Copies an imported model stream (e.g. from SAF or Uri) into app-specific storage.
     */
    fun importModel(inputStream: InputStream, targetName: String = DEFAULT_MODEL_NAME): Result<File> {
        return try {
            val targetFile = File(getModelsDirectory(), targetName)
            val tempFile = File(getModelsDirectory(), "$targetName.tmp")

            inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (!validateGguf(tempFile)) {
                tempFile.delete()
                return Result.failure(IllegalArgumentException("Provided file is not a valid GGUF model"))
            }

            if (targetFile.exists()) targetFile.delete()
            tempFile.renameTo(targetFile)
            Result.success(targetFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
