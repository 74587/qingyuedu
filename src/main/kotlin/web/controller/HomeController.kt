package web.controller

import org.apache.ibatis.solon.annotation.Db
import org.noear.solon.annotation.*
import org.noear.solon.core.handle.ModelAndView
import org.noear.solon.core.util.DataThrowable
import org.noear.solon.data.annotation.Tran
import org.noear.solon.data.cache.CacheService
import org.noear.solon.web.cors.annotation.CrossOrigin
import web.mapper.CodeMapper
import web.mapper.UsersMapper
import web.model.Users
import web.response.*
import web.util.admin.getMailCode
import web.util.admin.getcodes
import web.util.admin.passsign
import web.util.mail.Mail

@CrossOrigin(origins = "*")
@Controller
open class HomeController {
    @get:Get
    @get:Mapping("/regester")
    val reghtml = ModelAndView("regester.html")

    @Db("db")
    @Inject
    lateinit var usersMapper: UsersMapper

    @Db("db")
    @Inject
    lateinit var codeMapper: CodeMapper

    @Inject
    lateinit var cacheService: CacheService

    @Inject(value = "\${admin.code:}", autoRefreshed=true)
    var mycode:String=""

    @Inject(value = "\${user.allowuptxt:false}", autoRefreshed=true)
    var allowuptxt:Boolean=false

    @Inject(value = "\${user.allowcache:false}", autoRefreshed=true)
    var allowcache:Boolean=false

    @Inject(value = "\${user.allowimg:false}", autoRefreshed=true)
    var allowimg:Boolean=false


    @Inject(value = "\${user.source:0}", autoRefreshed=true)
    var source:Int =0



    @Tran
    @Post
    @Mapping("/regester")
    fun regester(@Param("username") _username:String? ,@Param("password") _password:String? ,@Param("phone") _phone:String?,@Param("email") _email:String?,@Param("code") _code:String?) = run {
        if (_username.isNullOrBlank() || _password.isNullOrBlank()  || _code.isNullOrBlank()  || _email.isNullOrBlank()  )  {
            throw DataThrowable().data(JsonResponse(false,NOT_BANK))
        }
        val user=Users().apply {
            username = _username
            password = _password
            code = _code
            phone = _phone
            email = _email
        }
        val (checkok,msg)=user.Check()
        if (!checkok) {
            throw DataThrowable().data(JsonResponse(isSuccess = false, errorMsg = msg))
        }
        val c=cacheService.get(_code,String::class.java)
        if(c != _code){
            codeMapper.getCode(_code).also {
                if(it == null) throw DataThrowable().data(JsonResponse(isSuccess = false, errorMsg = CODE_ERROR))
            }
        }
        if(usersMapper.getUserByusername(user.username?:"") != null) {
            throw DataThrowable().data(JsonResponse(isSuccess = false, errorMsg = USER_IS))
        }

        if(usersMapper.getUserByemail(_email).isNotEmpty()) {
            throw DataThrowable().data(JsonResponse(isSuccess = false, errorMsg = EMAIL_IS))
        }

        if(c == _code){
            user.code="开放注册"
        }
        user.source=source
        user.AllowCache=allowcache
        user.AllowUpTxt=allowuptxt
        user.AllowImg=allowimg

        if(user.source != 0 && user.source !=1 && user.source !=2){
            user.source=0
        }
        if(usersMapper.insert(user.create()) == 0){
            throw DataThrowable().data(JsonResponse(isSuccess = false, errorMsg = ADD_ERROR))
        }
        if(c != _code && codeMapper.deleteById(_code) == 0){
            throw DataThrowable().data(JsonResponse(isSuccess = false, errorMsg = ADD_ERROR))
        }
        JsonResponse(true)
    }

    @Mapping("/sendResetCode")
    fun sendResetCode( email: String?)=run {
        if (email.isNullOrBlank() )  {
            throw DataThrowable().data(JsonResponse(isSuccess = false, errorMsg = EMAIL_ERROR))
        }
        val c= getMailCode()
        cacheService.store("code_$email",c,60*10)
       runCatching {
            Mail.SendCode(c,email)
        }.onFailure {
            throw DataThrowable().data(JsonResponse(isSuccess = false, errorMsg = it.message?:"邮件发送失败"))
        }
        JsonResponse(true)
    }

    @Mapping("/resetPassword")
    fun  resetPassword(username:String? , password:String? , code:String? ,email:String?)=run {
        if(username.isNullOrBlank() || password.isNullOrBlank() || email.isNullOrBlank() || code.isNullOrBlank()){
            throw DataThrowable().data(JsonResponse(false,NOT_BANK))
        }
        if(password.length <6 || password.length > 15 ){
            throw DataThrowable().data(JsonResponse(false,PASS_VAIL_ERROR))
        }
        val user=usersMapper.getUserByusername(username)?:throw DataThrowable().data(JsonResponse(false,USER_NOT))
        if(user.email != email){
            throw DataThrowable().data(JsonResponse(false,EMAIL_CHECK_ERROR))
        }
        val c=cacheService.get("code_$email",String::class.java)
        if(c != code){
            throw DataThrowable().data(JsonResponse(false, CODE_CHECK_ERROR))
        }
        cacheService.remove("code_$email")
        usersMapper.changepass(user.id!!, passsign( password))

        JsonResponse(true)
    }

    @Mapping("/getcode")
    fun getcode(code: String?)=run {
        if (mycode.isBlank() )  {
            throw DataThrowable().data(JsonResponse(isSuccess = false, errorMsg = "当前未在配置文件中设置code"))
        }
        if (mycode != code )  {
            throw DataThrowable().data(JsonResponse(isSuccess = false, errorMsg = "code错误"))
        }
        val c= getcodes(1)[0]
        cacheService.store(c,c,60*10)
        JsonResponse(true).Data(c)
    }

}