package com.shipal.shipal.common.fileService

data class FileReplaceWork (
    val newRelativePath: String? = null,
    val commitAsync: suspend () -> Unit = {},
    val rollbackAsync: suspend () -> Unit = {}
)