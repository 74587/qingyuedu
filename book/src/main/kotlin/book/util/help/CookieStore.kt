@file:Suppress("unused")
package book.util.help


import book.util.MD5Utils
import book.util.NetworkUtils
import book.util.TextUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import okhttp3.Cookie
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Connection
import java.net.IDN
import java.net.URI



// TODO 处理cookie
class CookieStore(val userid:String) : CookieManager {
    companion object{
        val Stores:HashMap<String,CookieStore> = HashMap();
    }
    private val logger: Logger = LoggerFactory.getLogger(CookieStore::class.java)

    private val mainpath="storage"
    private val cookiepath="$mainpath/cookies"
    private val mycookiepath="$cookiepath/$userid"

    init {
        checkfile(mainpath)
        checkfile(cookiepath)
        checkfile(mycookiepath)
    }

    fun checkfile(path:String){
        val file = File(path)
        if (!file.exists()){
            file.mkdirs()
        }else{
            if (!file.isDirectory){
                file.delete()
                file.mkdirs()
            }
        }
    }

    fun clear(){
        val file = File(mycookiepath)
        if(file.exists()){
            file.deleteRecursively()
        }
        checkfile(mycookiepath)
    }

    fun loadRequest(request: Request): Request {
        var url = request.url.toString()
        val pos = url.indexOf('?')
        if (pos != -1) {
            url = url.substring(0, pos)
        }

        val cookie = getCookie(url)
        val requestCookie = request.header("Cookie")

        val newCookie = mergeCookies(requestCookie, cookie) ?: return request
        kotlin.runCatching {
            return request.newBuilder()
                .header("Cookie", newCookie)
                .build()
        }.onFailure {
            this.removeCookie(url)
            //val msg = "设置cookie出错，已清除cookie $domain cookie:$newCookie\n$it"
           // AppLog.put(msg, it)
        }

        return request
    }

    fun savejsonResponse(response: Connection.Response) {
        var url = response.url().toString()
        val pos = url.indexOf('?')
        if (pos != -1) {
            url = url.substring(0, pos)
        }

        val cookieString = mapToCookie(  response.cookies())
        if(cookieString != null){
            replaceCookie(url, cookieString)
        }
        //println("savejsonResponse $url, $cookieString")
    }

    fun saveResponse(response: Response) {
        val url = response.request.url
        val headers = response.headers
        saveCookiesFromHeaders(url, headers)
    }

    private fun saveCookiesFromHeaders(url: HttpUrl, headers: Headers) {
        val cookies = Cookie.parseAll(url, headers)
        var url=url.toString()
        val pos = url.indexOf('?')
        if (pos != -1) {
            url = url.substring(0, pos)
        }
        //val sessionCookie = cookies.filter { !it.persistent }.getString()
        //println(sessionCookie)
        val cookieString = cookies.getString()
        replaceCookie(url, cookieString)
    }

    fun List<Cookie>.getString() = buildString {
        this@getString.forEachIndexed { index, cookie ->
            if (index > 0) append("; ")
            append(cookie.name).append('=').append(cookie.value)
        }
    }

    private fun getkey(url: String) :String {
        return  NetworkUtils.getSubDomain(url)
    }

    override fun setCookie(url: String, cookie: String?) {
        logger.info("setCookie url:$url")
        val key=getkey(url)
        File("$mycookiepath/$key").writeText(cookie?:"")
    }

    override fun replaceCookie(url: String, cookie: String) {
        if (TextUtils.isEmpty(url) || TextUtils.isEmpty(cookie)) {
            return
        }
        val oldCookie = getCookie(url)
        if (TextUtils.isEmpty(oldCookie)) {
            setCookie(url, cookie)
        } else {
            val cookieMap = cookieToMap(oldCookie)
            cookieMap.putAll(cookieToMap(cookie))
            val newCookie = mapToCookie(cookieMap)
            setCookie(url, newCookie)
        }
    }

    fun getKey(tag: String, key: String? = null): String {
        val cookie = this.getCookie(tag)
        val cookieMap = this.cookieToMap(cookie)
        logger.info("tag:$tag, key:$key ")
        return if (key != null) {
            cookieMap[key] ?: ""
        } else {
            cookie
        }
    }


    fun mergeCookies(vararg cookies: String?): String? {
        val cookieMap = mergeCookiesToMap(*cookies)
        return mapToCookie(cookieMap)
    }

    fun mergeCookiesToMap(vararg cookies: String?): MutableMap<String, String> {
        return cookies.filterNotNull().map {
           cookieToMap(it)
        }.reduce { acc, cookieMap ->
            acc.apply { putAll(cookieMap) }
        }
    }


    private fun encodeChineseUrl(url: String): URI {
        val regex = Regex("^(https?://)([^/]+)(.*)")
        val match = regex.find(url) ?: throw IllegalArgumentException("Invalid URL")

        val (protocol, rawHost, path) = match.destructured
        val encodedHost = IDN.toASCII(rawHost) // 编码域名部分

        return URI("$protocol$encodedHost$path")
    }

    private fun getcookie(url: String):String {
        val key=getkey(url)
        var ck=""
        runCatching {
            ck =  File("$mycookiepath/$key").readText()
        }
        return ck
    }


    override fun getCookie(url: String): String {
        logger.info("getCookie:$url")
        var ck=getcookie(url)
        val cookieMap=cookieToMap(ck)
        while (ck.length > 4096) {
            val removeKey = cookieMap.keys.random()
            removeCookie(url, removeKey)
            cookieMap.remove(removeKey)
            ck = mapToCookie(cookieMap) ?: ""
        }
        return ck
    }

    fun removeCookie(url: String,key: String) {
        var ck=getcookie(url)
        if(ck.isEmpty()){
            return
        }
        val cookieMap=cookieToMap(ck)
        cookieMap.remove(key)
        setCookie(url,mapToCookie(cookieMap))
    }

    override fun removeCookie(url: String) {
        logger.info("removeCookie :$url")
        val key=getkey(url)
        val file =  File("$mycookiepath/$key")
        if (file.exists()) {
            file.delete()
        }
    }

    override fun cookieToMap(cookie: String): MutableMap<String, String> {
        val cookieMap = mutableMapOf<String, String>()
        if (cookie.isBlank()) {
            return cookieMap
        }
        val pairArray = cookie.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (pair in pairArray) {
            val pairs = pair.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (pairs.size == 1) {
                continue
            }
            val pos = pair.indexOf('=')
            val value =if (pos != -1 && pos+1 != pair.length ) pair.substring(pos+1, pair.length) else ""
            val key = if (pos != -1 && pos != 0 ) pair.substring(0, pos) else ""
            if (value.isNotBlank() || value.trim { it <= ' ' } == "null") {
                cookieMap[key] = value.trim { it <= ' ' }
            }
        }
        return cookieMap
    }

    override fun mapToCookie(cookieMap: Map<String, String>?): String? {
        if (cookieMap == null || cookieMap.isEmpty()) {
            return null
        }
        val builder = StringBuilder()
        var isadd=false;
        for (key in cookieMap.keys) {
            val value = cookieMap[key]
            if(key.trim() == "path"){
                continue
            }
            if (value?.isNotBlank() == true) {
                isadd=true;
                builder.append(key)
                    .append("=")
                    .append(value)
                    .append(";")
            }
        }
        if(!isadd){return  null}
        return builder.deleteCharAt(builder.lastIndexOf(";")).toString()
    }

}