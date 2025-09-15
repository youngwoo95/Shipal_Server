package com.shipal.shipal.Service.Post

import com.shipal.shipal.common.Logger.LogService
import com.shipal.shipal.common.ResponseModel
import com.shipal.shipal.common.fileService.FileService
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class PostService (
    private val logService: LogService, /* 로그 저장 공통로직 */
    private val fileService: FileService /* 파일 서비스 공통로직 */
)

{
    @Transactional
    fun addPostService(): ResponseModel<Boolean>
    {
        // 이미지 저장
        var imageRelative: String? = null

        try
        {


            return ResponseModel(message = "잘못된 요청입니다.", data = false, code = 400)
        }
        catch (ex: Exception)
        {
            logService.logMessage(ex.message.toString())
            logService.consoleWarning(ex.toString())
            return ResponseModel(message = "서버에서 요청을 처리하지 못하였습니다.", data = false, code = 500)
        }
    }
}