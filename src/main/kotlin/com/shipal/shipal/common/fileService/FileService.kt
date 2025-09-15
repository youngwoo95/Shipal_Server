package com.shipal.shipal.common.fileService

import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.multipart.MultipartFile

interface FileService {
    fun saveImageFile(image: MultipartFile, folderName: String) : String?
    fun saveFile(file: MultipartFile, folderName: String) : String?
    fun prepareReplaceImage(newImage: MultipartFile?, folderName: String, oldRelativePath: String?): FileReplaceWork
    fun prepareReplaceFile(newFile: MultipartFile?, folderName: String, oldRelativePath: String?): FileReplaceWork
     fun prepareRemove(oldRelativePath: String?): FileReplaceWork

    fun toPublicUrlFromRequest(relative: String?, req: HttpServletRequest) : String?

}