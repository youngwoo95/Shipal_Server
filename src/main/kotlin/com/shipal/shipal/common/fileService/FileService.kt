package com.shipal.shipal.common.fileService

import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.multipart.MultipartFile

interface FileService {
    suspend fun saveImageFile(image: MultipartFile, folderName: String) : String?
    suspend fun saveFile(file: MultipartFile, folderName: String) : String?
    suspend fun prepareReplaceImage(newImage: MultipartFile?, folderName: String, oldRelativePath: String?): FileReplaceWork
    suspend fun prepareReplaceFile(newFile: MultipartFile?, folderName: String, oldRelativePath: String?): FileReplaceWork
    suspend fun prepareRemove(oldRelativePath: String?): FileReplaceWork

    fun toPublicUrlFromRequest(relative: String?, req: HttpServletRequest) : String?

}