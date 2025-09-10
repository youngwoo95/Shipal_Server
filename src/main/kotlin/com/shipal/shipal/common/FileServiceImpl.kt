package com.shipal.shipal.common

import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.multipart.MultipartFile
import java.io.File

import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files
import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.stream.ImageOutputStream


@Service
open class FileServiceImpl(
    private val log: LogService,
    @Value("\${fileshare.root:}") private val rootFromConfig: String?   // 기본값 비우고 코드에서 null 처리
) : FileService {
    private val imageRootDir: Path by lazy {
        val base: Path = rootFromConfig?.takeIf { it.isNotBlank() }?.let { Paths.get(it) }
            ?: Paths.get(System.getProperty("user.dir")).resolve("FileShare")
        Files.createDirectories(base) // 디렉토리 보장
        base
    }

    private val allowedImageExts = setOf(".jpg", ".jpeg", ".png", ".gif")
    private val allowedDocExts = setOf(".pdf", ".xlsx", ".xls", ".csv", ".docx", ".pptx", ".txt", ".zip")

    private val tsFmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")
    private val rand = SecureRandom()

    override suspend fun saveImageFile(image: MultipartFile, folderName: String): String? {
        try {
            if (image.isEmpty) return null
            val ext = extOf(image.originalFilename) ?: return null
            if (ext !in allowedImageExts) return null

            val safeFolder = normalizeFolder(folderName) ?: return null

            val safeBase  = sanitizeBaseName(baseNameOf(image.originalFilename))
            val timestamp = LocalDateTime.now().format(tsFmt)
            val suffix    = randomSuffix(8)
            val saveName  = "${timestamp}_${suffix}_${safeBase}${ext.lowercase()}"

            val relative = "$safeFolder/$saveName".replace('\\', '/')
            val dir  = imageRootDir.resolve(safeFolder)
            Files.createDirectories(dir)
            val full = dir.resolve(saveName).toFile()

            image.inputStream.use { s -> if (!isAllowedImage(s, ext)) return null }
            image.inputStream.use { s ->
                if (ext.equals(".gif", true)) {
                    // GIF는 원본 보존
                    s.copyTo(FileOutputStream(full))
                } else {
                    // JPEG/PNG 재인코딩(메타 미보존 효과)
                    reencodeImage(s, full, ext)
                }
            }
            return relative
        } catch (ex: Exception) {
            log.logMessage("saveImageFile error: ${ex.message}\n$ex")
            return null
        }
    }

    override suspend fun saveFile(file: MultipartFile, folderName: String): String? {
        try {
            if (file.isEmpty) return null
            val ext = extOf(file.originalFilename) ?: return null

            val isImage = ext in allowedImageExts
            val isDoc   = ext in allowedDocExts
            if (!isImage && !isDoc) return null

            val safeFolder = normalizeFolder(folderName) ?: return null

            val base      = sanitizeBaseName(baseNameOf(file.originalFilename))
            val ts        = LocalDateTime.now().format(tsFmt)
            val rnd       = randomSuffix(8)
            val saveName  = "${ts}_${rnd}_${base}${ext.lowercase()}"

            val relative = "$safeFolder/$saveName".replace('\\', '/')
            val dir  = imageRootDir.resolve(safeFolder)
            Files.createDirectories(dir)
            val full = dir.resolve(saveName).toFile()

            if (isImage) {
                file.inputStream.use { s -> if (!isAllowedImage(s, ext)) return null }
                file.inputStream.use { s ->
                    if (ext.equals(".gif", true)) {
                        s.copyTo(FileOutputStream(full))
                    } else {
                        reencodeImage(s, full, ext)
                    }
                }
            } else {
                file.inputStream.use { s -> if (!isAllowedDocument(s, ext)) return null }
                file.inputStream.use { s -> s.copyTo(FileOutputStream(full)) }
            }
            return relative
        } catch (ex: Exception) {
            log.logMessage("saveFile error: ${ex.message}\n$ex")
            return null
        }
    }

    override suspend fun prepareReplaceImage(
        newImage: MultipartFile?,
        folderName: String,
        oldRelativePath: String?
    ): FileReplaceWork {
        return try {
            if (newImage == null || newImage.isEmpty) {
                FileReplaceWork()
            } else {
                val newRel = saveImageFile(newImage, folderName)
                if (newRel.isNullOrBlank()) return FileReplaceWork()

                val newFull = imageRootDir.resolve(newRel.replace('/', File.separatorChar)).toFile()
                val oldFull = oldRelativePath?.let { imageRootDir.resolve(it.replace('/', File.separatorChar)).toFile() }

                FileReplaceWork(
                    newRelativePath = newRel,
                    commitAsync = {
                        try { if (oldFull != null && oldFull.exists()) oldFull.delete() } catch (_: Exception) {}
                    },
                    rollbackAsync = {
                        try { if (newFull.exists()) newFull.delete() } catch (_: Exception) {}
                    }
                )
            }
        } catch (ex: Exception) {
            log.logMessage("prepareReplaceImage error: ${ex.message}\n$ex")
            FileReplaceWork()
        }
    }

    override suspend fun prepareReplaceFile(
        newFile: MultipartFile?,
        folderName: String,
        oldRelativePath: String?
    ): FileReplaceWork {
        return try {
            if (newFile == null || newFile.isEmpty) {
                FileReplaceWork()
            } else {
                val newRel = saveFile(newFile, folderName)
                if (newRel.isNullOrBlank()) return FileReplaceWork()

                val newFull = imageRootDir.resolve(newRel.replace('/', File.separatorChar)).toFile()
                val oldFull = oldRelativePath?.let { imageRootDir.resolve(it.replace('/', File.separatorChar)).toFile() }

                FileReplaceWork(
                    newRelativePath = newRel,
                    commitAsync = {
                        try { if (oldFull != null && oldFull.exists()) oldFull.delete() } catch (_: Exception) {}
                    },
                    rollbackAsync = {
                        try { if (newFull.exists()) newFull.delete() } catch (_: Exception) {}
                    }
                )
            }
        } catch (ex: Exception) {
            log.logMessage("prepareReplaceFile error: ${ex.message}\n$ex")
            FileReplaceWork()
        }
    }

    override suspend fun prepareRemove(oldRelativePath: String?): FileReplaceWork {
        return try {
            if (oldRelativePath.isNullOrBlank()) {
                FileReplaceWork()
            } else {
                val oldFull = imageRootDir.resolve(oldRelativePath.replace('/', File.separatorChar)).toFile()
                FileReplaceWork(
                    newRelativePath = null,
                    commitAsync = { try { if (oldFull.exists()) oldFull.delete() } catch (_: Exception) {} },
                    rollbackAsync = {}
                )
            }
        } catch (ex: Exception) {
            log.logMessage("prepareRemove error: ${ex.message}\n$ex")
            FileReplaceWork()
        }
    }

    private fun extOf(filename: String?): String? {
        if (filename.isNullOrBlank()) return null
        val idx = filename.lastIndexOf('.')
        if (idx < 0) return null
        return filename.substring(idx).lowercase()
    }

    private fun baseNameOf(filename: String?): String {
        if (filename.isNullOrBlank()) return "noname"
        val justName = filename.substringBeforeLast('.', filename)
        return justName.ifBlank { "noname" }
    }

    private fun normalizeFolder(input: String?): String? {
        if (input.isNullOrBlank()) return null
        val segs = input.replace('\\', '/').split('/').filter { it.isNotBlank() }
        val safe = segs.mapNotNull { s ->
            if (s == "." || s == "..") null
            else {
                val cleaned = s.filter { it.isLetterOrDigit() || it == '-' || it == '_' }
                cleaned.ifBlank { null }
            }
        }
        return safe.joinToString("/").ifBlank { null }
    }

    private fun sanitizeBaseName(name: String): String {
        val cleaned = name.filter { it.isLetterOrDigit() || it == '-' || it == '_' || it == '.' }
            .take(60).ifBlank { "noname" }
        return cleaned
    }

    private fun randomSuffix(n: Int): String {
        val alphabet = "0123456789abcdef"
        return (1..n).map { alphabet[rand.nextInt(alphabet.length)] }.joinToString("")
    }

    private fun isAllowedImage(s: InputStream, ext: String): Boolean {
        val header = ByteArray(16)
        val read = s.read(header)
        val isJpeg = read >= 2 && header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte()
        val isPng = read >= 8 && header[0] == 0x89.toByte() && header[1] == 0x50.toByte() &&
                header[2] == 0x4E.toByte() && header[3] == 0x47.toByte() &&
                header[4] == 0x0D.toByte() && header[5] == 0x0A.toByte() &&
                header[6] == 0x1A.toByte() && header[7] == 0x0A.toByte()
        val isGif = read >= 4 && header[0] == 0x47.toByte() && header[1] == 0x49.toByte() &&
                header[2] == 0x46.toByte() && header[3] == 0x38.toByte()

        return when (ext.lowercase()) {
            ".jpg", ".jpeg" -> isJpeg
            ".png" -> isPng
            ".gif" -> isGif
            else -> false
        }
    }

    /** 문서류 매직넘버 검사 */
    private fun isAllowedDocument(s: InputStream, ext: String): Boolean {
        val header = ByteArray(8)
        val read = s.read(header)
        val isPdf = read >= 4 && header[0] == 0x25.toByte() && header[1] == 0x50.toByte() &&
                header[2] == 0x44.toByte() && header[3] == 0x46.toByte() // %PDF
        val isZip = read >= 4 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() &&
                header[2] == 0x03.toByte() && header[3] == 0x04.toByte() // PK..
        val isOle = read >= 8 && header[0] == 0xD0.toByte() && header[1] == 0xCF.toByte() &&
                header[2] == 0x11.toByte() && header[3] == 0xE0.toByte() &&
                header[4] == 0xA1.toByte() && header[5] == 0xB1.toByte() &&
                header[6] == 0x1A.toByte() && header[7] == 0xE1.toByte() // .xls OLE

        return when (ext.lowercase()) {
            ".pdf" -> isPdf
            ".xlsx", ".docx", ".pptx", ".zip" -> isZip // OOXML은 zip
            ".xls" -> isOle
            ".csv", ".txt" -> true
            else -> false
        }
    }

    /** JPEG/PNG 재인코딩 (EXIF 등 메타 미보존 효과) */
    private fun reencodeImage(inStream: InputStream, outFile: File, ext: String) {
        val img = ImageIO.read(inStream) ?: throw IllegalArgumentException("Invalid image")
        val format = if (ext.equals(".png", true)) "png" else "jpg"

        if (format == "jpg") {
            val writers = ImageIO.getImageWritersByFormatName("jpg")
            val writer = writers.next()
            FileOutputStream(outFile).use { fos ->
                val ios: ImageOutputStream = ImageIO.createImageOutputStream(fos)
                writer.output = ios
                val param = writer.defaultWriteParam
                if (param.canWriteCompressed()) {
                    param.compressionMode = ImageWriteParam.MODE_EXPLICIT
                    param.compressionQuality = 0.9f // 품질 90
                }
                writer.write(null, IIOImage(img, null, null), param)
                writer.dispose()
                ios.close()
            }
        } else {
            ImageIO.write(img, "png", outFile)
        }
    }
}