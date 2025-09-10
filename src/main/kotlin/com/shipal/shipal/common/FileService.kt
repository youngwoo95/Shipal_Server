package com.shipal.shipal.common

import org.springframework.web.multipart.MultipartFile

interface FileService {
    suspend fun saveImageFile(image: MultipartFile, folderName: String) : String?
    suspend fun saveFile(file: MultipartFile, folderName: String) : String?
    suspend fun prepareReplaceImage(newImage: MultipartFile?, folderName: String, oldRelativePath: String?): FileReplaceWork
    suspend fun prepareReplaceFile(newFile: MultipartFile?, folderName: String, oldRelativePath: String?): FileReplaceWork
    suspend fun prepareRemove(oldRelativePath: String?): FileReplaceWork
}