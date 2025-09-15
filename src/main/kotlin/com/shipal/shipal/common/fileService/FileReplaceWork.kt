package com.shipal.shipal.common.fileService

data class FileReplaceWork (
    val newRelativePath: String? = null,
    val commit:  () -> Unit = {},
    val rollback: () -> Unit = {}
)