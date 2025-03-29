package web.util.admin

import org.noear.solon.core.handle.Context
import web.model.Code
import web.util.hash.Sha256
import java.util.*

fun getRandomString(length: Int) : String {
    val charset = "ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz0123456789"
    return (1..length)
        .map { charset.random() }
        .joinToString("")
}
fun loginok(username:String, ctx: Context){
    ctx.sessionSet("username", username)
}

fun logout(ctx: Context) {
    ctx.sessionRemove("username")
}

fun islogin(ctx: Context) :Boolean =run{
    if(ctx.session("username") != null && ctx.session("username").toString().isNotBlank()){
        true
    }else{
        false
    }
}

fun getcodes(num:Int):MutableList<String>{
    var codes = mutableListOf<String>()
    var date=  Date().time
    for(i in 1..num){
        val randoms = getRandomString(6)
        val code= "$date$randoms"
        codes.add(code)
    }
    return codes
}


fun passsign(password: String):String{
    return Sha256(password)
}