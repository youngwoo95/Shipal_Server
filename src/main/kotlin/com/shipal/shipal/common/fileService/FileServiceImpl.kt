package com.shipal.shipal.common.fileService

import com.shipal.shipal.common.Logger.LogService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
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

    private val allowedImageExts = setOf(".jpg", ".jpeg", ".png", ".gif",".webp")
    private val allowedDocExts = setOf(".pdf", ".xlsx", ".xls", ".csv", ".docx", ".pptx", ".txt", ".zip")

    private val tsFmt = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")
    private val rand = SecureRandom()

    override fun saveImageFile(image: MultipartFile, folderName: String): String? {
        try {
            if (image.isEmpty) return null
            val ext = extOf(image.originalFilename) ?: return null
            if (ext !in allowedImageExts) return null

            val safeFolder = normalizeFolder(folderName) ?: return null
            val safeBase   = sanitizeBaseName(baseNameOf(image.originalFilename))
            val timestamp  = LocalDateTime.now().format(tsFmt)
            val suffix     = randomSuffix(8)
            val saveName   = "${timestamp}_${suffix}_${safeBase}${ext.lowercase()}"

            val relative = "$safeFolder/$saveName".replace('\\', '/')
            val dir  = imageRootDir.resolve(safeFolder)
            Files.createDirectories(dir)
            val full = dir.resolve(saveName).toFile()

            // 1) 형식 검사
            image.inputStream.use { s -> if (!isAllowedImage(s, ext)) return null }

            // 2) 저장 (gif/webp 원본 복사, jpg/png 재인코딩)
            image.inputStream.use { s ->
                when {
                    ext.equals(".gif", true) || ext.equals(".webp", true) -> {
                        writeAtomic(full) { t ->
                            FileOutputStream(t).use { fos -> s.copyTo(fos) }
                        }
                    }
                    else -> {
                        writeAtomic(full) { t ->
                            reencodeImage(s, t, ext)
                        }
                    }
                }
            }

            return relative
        } catch (ex: Exception) {
            log.logMessage("saveImageFile error: ${ex.message}\n$ex")
            return null
        }
    }

    override fun saveFile(file: MultipartFile, folderName: String): String? {
        try {
            if (file.isEmpty) return null
            val ext = extOf(file.originalFilename) ?: return null

            val isImage = ext in allowedImageExts
            val isDoc   = ext in allowedDocExts
            if (!isImage && !isDoc) return null

            val safeFolder = normalizeFolder(folderName) ?: return null
            val base       = sanitizeBaseName(baseNameOf(file.originalFilename))
            val ts         = LocalDateTime.now().format(tsFmt)
            val rnd        = randomSuffix(8)
            val saveName   = "${ts}_${rnd}_${base}${ext.lowercase()}"

            val relative = "$safeFolder/$saveName".replace('\\', '/')
            val dir  = imageRootDir.resolve(safeFolder)
            Files.createDirectories(dir)
            val full = dir.resolve(saveName).toFile()

            if (isImage) {
                // 형식 검사
                file.inputStream.use { s -> if (!isAllowedImage(s, ext)) return null }
                // 저장
                file.inputStream.use { s ->
                    when {
                        ext.equals(".gif", true) || ext.equals(".webp", true) -> {
                            writeAtomic(full) { t ->
                                FileOutputStream(t).use { fos -> s.copyTo(fos) }
                            }
                        }
                        else -> {
                            writeAtomic(full) { t ->
                                reencodeImage(s, t, ext)
                            }
                        }
                    }
                }
            } else {
                // 문서 시그니처 검사
                file.inputStream.use { s -> if (!isAllowedDocument(s, ext)) return null }
                file.inputStream.use { s ->
                    writeAtomic(full) { t ->
                        FileOutputStream(t).use { fos -> s.copyTo(fos) }
                    }
                }
            }

            return relative
        } catch (ex: Exception) {
            log.logMessage("saveFile error: ${ex.message}\n$ex")
            return null
        }
    }

    override fun prepareReplaceImage(
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
                    // 동기식이나 기존 시그니처 유지
                    commit = {
                        try { if (oldFull != null && oldFull.exists()) oldFull.delete() } catch (_: Exception) {}
                    },
                    rollback = {
                        try { if (newFull.exists()) newFull.delete() } catch (_: Exception) {}
                    }
                )
            }
        } catch (ex: Exception) {
            log.logMessage("prepareReplaceImage error: ${ex.message}\n$ex")
            FileReplaceWork()
        }
    }

    override fun prepareReplaceFile(
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
                    commit = {
                        try { if (oldFull != null && oldFull.exists()) oldFull.delete() } catch (_: Exception) {}
                    },
                    rollback = {
                        try { if (newFull.exists()) newFull.delete() } catch (_: Exception) {}
                    }
                )
            }
        } catch (ex: Exception) {
            log.logMessage("prepareReplaceFile error: ${ex.message}\n$ex")
            FileReplaceWork()
        }
    }

    override fun prepareRemove(oldRelativePath: String?): FileReplaceWork {
        return try {
            if (oldRelativePath.isNullOrBlank()) {
                FileReplaceWork()
            } else {
                val oldFull = imageRootDir.resolve(oldRelativePath.replace('/', File.separatorChar)).toFile()
                FileReplaceWork(
                    newRelativePath = null,
                    commit = { try { if (oldFull.exists()) oldFull.delete() } catch (_: Exception) {} },
                    rollback = {}
                )
            }
        } catch (ex: Exception) {
            log.logMessage("prepareRemove error: ${ex.message}\n$ex")
            FileReplaceWork()
        }
    }

    override fun toPublicUrlFromRequest(relative: String?, req: HttpServletRequest): String? {
        if (relative.isNullOrBlank()) return null
        val segments = relative.replace('\\','/').trimStart('/').split('/')
        return ServletUriComponentsBuilder.fromRequest(req)
            .replacePath(null)
            .path("/images/")
            .pathSegment(*segments.toTypedArray())
            .build()
            .toUriString()
    }

    // -------------------- Helpers --------------------

    private fun writeAtomic(target: File, writer: (File) -> Unit) {
        val tmp = File(target.parentFile, target.name + ".tmp")
        writer(tmp)
        try {
            // 가능하면 원자 이동
            Files.move(tmp.toPath(), target.toPath(), java.nio.file.StandardCopyOption.ATOMIC_MOVE)
        } catch (_: Exception) {
            // 파일시스템이 ATOMIC_MOVE 미지원이면 대체
            Files.move(tmp.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
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
        return name.filter { it.isLetterOrDigit() || it == '-' || it == '_' || it == '.' }
            .take(60).ifBlank { "noname" }
    }

    private fun randomSuffix(n: Int): String {
        val alphabet = "0123456789abcdef"
        return (1..n).map { alphabet[rand.nextInt(alphabet.length)] }.joinToString("")
    }

    private fun isAllowedImage(s: InputStream, ext: String): Boolean {
        // WebP 판별 위해 12바이트까지 읽음 (RIFF....WEBP)
        val header = ByteArray(12)
        val read = s.read(header)
        val isJpeg = read >= 2  && header[0] == 0xFF.toByte() && header[1] == 0xD8.toByte()
        val isPng  = read >= 8  && header[0] == 0x89.toByte() && header[1] == 0x50.toByte() &&
                header[2] == 0x4E.toByte() && header[3] == 0x47.toByte() &&
                header[4] == 0x0D.toByte() && header[5] == 0x0A.toByte() &&
                header[6] == 0x1A.toByte() && header[7] == 0x0A.toByte()
        val isGif  = read >= 4  && header[0] == 0x47.toByte() && header[1] == 0x49.toByte() &&
                header[2] == 0x46.toByte() && header[3] == 0x38.toByte()
        val isWebp = read >= 12 && header[0] == 'R'.code.toByte() && header[1] == 'I'.code.toByte() &&
                header[2] == 'F'.code.toByte() && header[3] == 'F'.code.toByte() &&
                header[8] == 'W'.code.toByte() && header[9] == 'E'.code.toByte() &&
                header[10] == 'B'.code.toByte() && header[11] == 'P'.code.toByte()

        return when (ext.lowercase()) {
            ".jpg", ".jpeg" -> isJpeg
            ".png"          -> isPng
            ".gif"          -> isGif
            ".webp"         -> isWebp
            else            -> false
        }
    }

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
            ".pdf"                      -> isPdf
            ".xlsx", ".docx", ".pptx",
            ".zip"                      -> isZip
            ".xls"                      -> isOle
            ".csv", ".txt"              -> true  // 필요 시 MIME 검증/강제 다운로드 권장
            else                        -> false
        }
    }

    /** JPEG/PNG 재인코딩 (EXIF 등 메타 미보존 효과). 웹브라우저 호환 위해 jpg/png만 처리 */
    private fun reencodeImage(inStream: InputStream, outFile: File, ext: String) {
        val img = ImageIO.read(inStream) ?: throw IllegalArgumentException("Invalid image")
        val format = if (ext.equals(".png", true)) "png" else "jpg"

        if (format == "jpg") {
            val writers = ImageIO.getImageWritersByFormatName("jpg")
            require(writers.hasNext()) { "No JPEG writer available" }
            val writer = writers.next()
            FileOutputStream(outFile).use { fos ->
                ImageIO.createImageOutputStream(fos).use { ios: ImageOutputStream ->
                    writer.output = ios
                    val param = writer.defaultWriteParam
                    if (param.canWriteCompressed()) {
                        param.compressionMode = ImageWriteParam.MODE_EXPLICIT
                        param.compressionQuality = 0.9f
                    }
                    writer.write(null, IIOImage(img, null, null), param)
                }
            }
            writer.dispose()
        } else {
            ImageIO.write(img, "png", outFile)
        }
    }
}