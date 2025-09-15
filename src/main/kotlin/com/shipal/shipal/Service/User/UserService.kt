package com.shipal.shipal.Service.User

import com.shipal.shipal.common.ResponseModel
import com.shipal.shipal.Dto.User.AddUserDto
import com.shipal.shipal.Dto.User.LoginDto
import com.shipal.shipal.Dto.User.RefreshDto
import com.shipal.shipal.Dto.User.ResponseTokenDto
import com.shipal.shipal.Dto.User.UpdateUserDto
import com.shipal.shipal.Model.UserInfo
import com.shipal.shipal.Repository.UserInfoRepository
import com.shipal.shipal.Service.comm.GetTokenValues
import com.shipal.shipal.Service.comm.JwtService
import com.shipal.shipal.VO.UserVO
import com.shipal.shipal.common.fileService.FileService
import com.shipal.shipal.common.Logger.LogService
import com.shipal.shipal.common.fileService.FileReplaceWork
import jakarta.servlet.http.HttpServletRequest
import jakarta.transaction.Transactional
import kotlinx.coroutines.runBlocking
import org.apache.coyote.Response
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.interceptor.TransactionAspectSupport
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.web.bind.annotation.ModelAttribute
import java.time.LocalDateTime
import kotlin.math.log

@Service
class UserService (
    private val userRepo: UserInfoRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val redisService: com.shipal.shipal.Service.Redis.RedisService,
    private val logService: LogService,
    private val fileService: FileService
)

{
    @Transactional
     fun updateUserService(
        dto: UpdateUserDto,
        request: HttpServletRequest) : ResponseModel<Boolean>
    {

        try
        {
            val jwt = GetTokenValues.get()
                ?: return ResponseModel(message = "로그인이 필요합니다.", data = false, code = 401)

            val tokenUserSeq: Int = jwt.uId // 필터에서 Int로 표준화

            // 3-B) (만약 DTO와 일치 체크가 꼭 필요하면) 숫자로 비교
            if (dto.userSeq != tokenUserSeq) {
                return ResponseModel(message = "일치하지 않습니다.", data = false, code = 400)
            }

            val user = userRepo.getUserInfo(dto.userSeq)
            if(user == null) {
                return ResponseModel<Boolean>(
                    message = "등록되지 않은 사용자입니다.",
                    data = null,
                    code = 400
                )
            }

            /* 넘어온 이름이 있을때. */
            if(dto.name != null)
                user.name = dto.name

            /* 넘어온 비밀번호가 있을때 */
            if(dto.loginPw != null)
            {
                val bCrypto : String = passwordEncoder.encode(dto.loginPw)
                user.loginPw = bCrypto
            }

            /* 넘어온 전화번호가 있을때. */
            if(dto.phone != null)
                user.phone = dto.phone

            /* 넘어온 닉네임이 있을 때 */
            if(dto.nickname != null)
                user.nickname = dto.nickname

            /*  파일로직  */
            var imgWork = FileReplaceWork()
            val newFile = dto.images
            if(newFile != null && !newFile.isEmpty)
            {
                val now = java.time.LocalDate.now()
                val folder = "Images/Profile/${now.year}/%02d".format(now.monthValue)

                imgWork = fileService.prepareReplaceFile(
                    newFile,
                    folder,
                    oldRelativePath = user.attach
                )

                if (imgWork.newRelativePath.isNullOrBlank()) {
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()
                    imgWork.rollback()  // 동기식
                    return ResponseModel("이미지 저장에 실패했습니다.", false, 400)
                }
                user.attach = imgWork.newRelativePath
            }


            val rows = userRepo.updateUser(user)
            if (rows <= 0) {
                // 업데이트 실패 시 파일도 롤백
                imgWork.rollback()
                return ResponseModel("업데이트에 실패했습니다.", false, 400)
            }


            if (imgWork.newRelativePath != null) {
                if (TransactionSynchronizationManager.isSynchronizationActive()) {
                    TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                        override fun afterCommit() { imgWork.commit() }
                        override fun afterCompletion(status: Int) {
                            if (status != TransactionSynchronization.STATUS_COMMITTED) imgWork.rollback()
                        }
                    })
                } else {
                    // 가드: 트랜잭션 없으면 즉시 확정 (필요시 정책대로)
                    imgWork.commit()
                }
            }

            return ResponseModel("요청이 정상 처리되었습니다.", true, 200)
        }
        catch (ex: Exception)
        {
            logService.logMessage(ex.message.toString())
            logService.consoleWarning(ex.toString())
            return ResponseModel<Boolean>(message =  "서버에서 요청을 처리하지 못하였습니다.", data = false, code =500)
        }
    }

    /*
    * 회원가입 서비스
    * */
    @Transactional
    fun addUserService(req: AddUserDto) : ResponseModel<Boolean>
    {
        // 이미지 저장
        var imageRelative: String? = null
        try
        {
            val loginId = req.loginId.trim()
            val phone = req.phone.filter { it.isDigit() } // 숫자만
            val name = req.name.trim()
            val nickname = req.nickname?.trim()

            if(userRepo.existsByLoginId(loginId) > 0) {
                return ResponseModel(message = "이미 사용중인 아이디 입니다.", data = false, code = 400)
            }

            // 비밀번호 BCrypto 암호화
            val bCrypto = passwordEncoder.encode(req.loginPw)

            if (req.images != null && !req.images.isEmpty) {
                // 폴더 규칙: 날짜 기준 (원하면 VocFolder/{siteSeq} 등으로 변경)
                val now = java.time.LocalDate.now()
                val folder = "Images/Profile/${now.year}/%02d".format(now.monthValue)

                // 화이트리스트/시그니처 검사는 FileService가 수행
                imageRelative = runBlocking { fileService.saveImageFile(req.images, folder) }
            }

            // 데이터베이스에 전달할 엔티티 생성
            val now = LocalDateTime.now()
            val userParam = UserInfo(
                userSeq = null,
                loginId = loginId,
                phone = phone,
                loginPw = bCrypto,
                name = name,
                nickname = nickname, // 널 가능
                createDt = now,
                attach = imageRelative
            )

            // 저장
            val affected = userRepo.AddUserInfo(userParam)

            if(affected <= 0 || userParam.userSeq == null)
            {
                return ResponseModel(message = "잘못된 요청입니다.", data = false, code = 400)
            }

            return ResponseModel(message = "요청이 정상 처리되었습니다.", data = true, code = 200)
        }
        catch (ex: Exception)
        {
            try
            {
                if(imageRelative != null)
                {
                    val root = getFilesRoot()
                    val full = java.nio.file.Paths.get(root).resolve(imageRelative.replace('/', java.io.File.separatorChar))
                    java.nio.file.Files.deleteIfExists(full)
                }
            }
            catch (_: Exception){}
            logService.logMessage(ex.message.toString())
            logService.consoleWarning(ex.toString())
            return ResponseModel(message = "서버에서 요청을 처리하지 못하였습니다.", data = false, code = 500)
        }
    }

    // 파일 루트 얻기 (WebConfig와 동일한 규칙)
    private fun getFilesRoot(): String {
        val envRoot = System.getProperty("fileshare.root") // 없으면 null
        return (envRoot?.takeIf { it.isNotBlank() }
            ?: java.nio.file.Paths.get(System.getProperty("user.dir")).resolve("FileShare").toString())
    }



    /**
     * 로그인 서비스
     */
    @Transactional
    fun loginService(req: LoginDto) : ResponseModel<ResponseTokenDto>{
        try {
            // 입력 정규화
            val loginId = req.loginId.trim()
            val loginPw = req.loginPw.trim()

            // 사용자 조회
            val user = userRepo.getUserLogin(loginId) ?: return ResponseModel<ResponseTokenDto>(
                message = "등록되지 않은 사용자입니다.",
                data = null,
                code = 400
            )

            // 비밀번호 암호화 검증
            val passwordChk = passwordEncoder.matches(req.loginPw, user.loginPw)
            if (!passwordChk)
                return ResponseModel<ResponseTokenDto>(message = "비밀번호가 일치하지 않습니다.", data = null, code = 400)

            val accessToken = jwtService.generateAccessToken(
                subject = user.loginId,
                roles = listOf("USER"),
                extra = mapOf(
                    "uId" to (user.userSeq ?: 0),
                    "name" to (user.name)
                )
            )

            // 2) uuid 준비 (LoginDto에 uuid가 없다면 서버에서 생성)
            val uuid = /* req.uuid ?: */ java.util.UUID.randomUUID().toString()

            // 3) Redis에 리프레시 토큰 저장 (TTL 3일, 필요시 변경)
            val pair = redisService.setRefreshToken(
                userSeq = user.userSeq ?: return ResponseModel("잘못된 사용자 정보입니다.", null, 400),
                accessToken = accessToken,
                uuid = uuid
            ) ?: return ResponseModel("세션 생성에 실패했습니다.", null, 500)

            val (_, refreshToken) = pair

            val body = ResponseTokenDto(
                accessToken = accessToken,
                refreshToken = refreshToken,
                uuid = uuid
            )

            return ResponseModel<ResponseTokenDto>(message = "요청이 정상 처리되었습니다.", data = body, code = 200)
        }
        catch (ex: Exception)
        {
            logService.logMessage(ex.message.toString())
            logService.consoleWarning(ex.toString())
            return ResponseModel<ResponseTokenDto>(message = "서버에서 요청을 처리하지 못하였습니다.", data = null, code = 500)
        }
    }

    /**
     * 토큰 재발급 (Access 재발급 + Refresh 회전)
     */
    fun refreshService(req: RefreshDto): ResponseModel<ResponseTokenDto> {
        try {
            val rotated = redisService.rotateRefreshToken(req.userSeq, req.refreshToken, req.uuid)
                ?: return ResponseModel(message = "리프레시 토큰이 유효하지 않습니다.", data = null, code = 401)

            // 2) 사용자 조회 (subject/클레임에 넣을 정보)
            val user = userRepo.getUserBySeq(req.userSeq)
                ?: return ResponseModel(message = "사용자 정보를 찾을 수 없습니다.", data = null, code = 400)

            // 3) Access Token 재발급
            val newAccess = jwtService.generateAccessToken(
                subject = user.loginId,                  // 기존 로직과 동일
                roles = listOf("USER"),
                extra = mapOf(
                    "uId" to (user.userSeq ?: 0),
                    "name" to user.name
                )
            )

            val body = ResponseTokenDto(
                accessToken = newAccess,
                refreshToken = rotated,                  // 회전되었으면 새 값, 아니면 기존 값
                uuid = req.uuid
            )
            return ResponseModel(message = "요청이 정상 처리되었습니다.", data = body, code = 200)
        }
        catch (ex: Exception){
            logService.logMessage(ex.message.toString())
            logService.consoleWarning(ex.toString())
            return ResponseModel(message = "서버에서 요청을 처리하지 못하였습니다.", data =null, code = 500)
        }
    }

    /*
     * 로그아웃
     */
    fun logoutService(): ResponseModel<Boolean> {
        try
        {
            val getJwtToken = GetTokenValues.get() ?: throw IllegalStateException("로그인이 필요합니다.")
            val ok = redisService.logout(getJwtToken.uId)

            return if (ok)
                ResponseModel(message = "요청이 정상 처리되었습니다.", data = true, code = 200)
            else
                ResponseModel(message = "세션이 존재하지 않습니다.", data = false, code = 400)
        }
        catch (ex: Exception){
            logService.logMessage(ex.message.toString())
            logService.consoleWarning(ex.toString())
            return ResponseModel(message = "서버에서 요청을 처리하지 못하였습니다.", data = null, code = 500)
        }
    }

    /*
    * 사용자 프로필 조회
    * */
    fun getProfile(req: HttpServletRequest) : ResponseModel<UserVO>
    {
        try{
            val getJwtToken = GetTokenValues.get() ?: throw IllegalStateException("로그인이 필요합니다.")

            val userSeq : Int? = getJwtToken.uId
            if(userSeq == null)
                return ResponseModel(message = "잘못된 요청입니다.", data = null, code= 400)

            var model = userRepo.getUserProfile(userSeq)
            if(model == null)
                return ResponseModel(message =  "잘못된 요청입니다.", data = null, code =400)

            model.images = fileService.toPublicUrlFromRequest(model.images, req)

            return ResponseModel(message = "요청이 정상 처리되었습니다.", data = model, code= 200)
        }
        catch (ex: Exception)
        {
            logService.logMessage(ex.message.toString())
            logService.consoleWarning(ex.toString())
            return ResponseModel(message =  "서버에서 요청을 처리하지 못하였습니다.", data = null, code =500)
        }
    }

}