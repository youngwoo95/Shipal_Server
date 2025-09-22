package com.shipal.shipal.Service.Post

import com.shipal.shipal.Dto.Post.AddPostDto
import com.shipal.shipal.Dto.Post.Cmd.CountryUpsertCmd
import com.shipal.shipal.Model.CountryInfo
import com.shipal.shipal.Repository.CountryRepository
import com.shipal.shipal.common.Logger.LogService
import com.shipal.shipal.common.ResponseModel
import com.shipal.shipal.common.fileService.FileService
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class PostService (
    private val logService: LogService, /* 로그 저장 공통로직 */
    private val fileService: FileService, /* 파일 서비스 공통로직 */
    private val CountryRepository: CountryRepository
)

{
    private  fun findOrInsertCountry(name: String): CountryInfo{
        CountryRepository.selCountryName(name)?.let { return it }

        val cmd = CountryUpsertCmd(countryName = name)
        CountryRepository.upsertAndFillId(cmd)
        return CountryInfo(countrySeq = cmd.countrySeq!!, countryName =  name)
    }

    @Transactional
    fun addPostService(req: AddPostDto): ResponseModel<Boolean>
    {
        // 이미지 저장
        var imageRelative: String? = null

        try
        {
            val fromCountry = findOrInsertCountry(req.fromCountryName)

            println(fromCountry.countrySeq)

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