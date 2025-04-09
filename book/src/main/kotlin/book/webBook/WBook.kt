package book.webBook



import book.model.Book
import book.model.BookChapter
import book.model.BookSource
import book.model.SearchBook
import book.util.AppPattern.JS_PATTERN
import book.util.http.StrResponse
import book.webBook.analyzeRule.AnalyzeUrl
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger: Logger = LoggerFactory.getLogger(WBook::class.java)

class WBook (val bookSource: BookSource, val debugLog: Boolean = true, var debugLogger: DebugLog? = null,val userid:String?=null,val usertocken:String?=null){

    constructor(bookSourceString: String, _userid:String, _usertocken:String?=null, debugLog: Boolean = true) : this(BookSource.fromJson(bookSourceString).getOrNull() ?: BookSource(), debugLog,userid=_userid,usertocken=_usertocken)

    init {
        bookSource.userid = userid
        bookSource.usertocken = usertocken
    }

    val sourceUrl: String
        get() = bookSource.bookSourceUrl


    val debugger: DebugLog?
        get() {
            if (debugLogger != null) {
                return debugLogger
            }
            if (debugLog) {
                return Debug
            }
            return null
        }

//    fun getexploreUrl():String = run{
//        if(bookSource.exploreUrl == null){
//            return  ""
//        }else{
//            val variableBook = SearchBook()
//            val analyzeUrl = AnalyzeUrl(
//                mUrl = bookSource.exploreUrl!!,
//                baseUrl = bookSource.bookSourceUrl,
//                source = bookSource,
//                ruleData = variableBook,
//                headerMapF = bookSource.getHeaderMap(true),
//                needanalyzeUrl = false
//            )
//            return analyzeUrl.ruleUrl
//        }
//    }

    fun getexploreUrl():String = run{
        val jsMatcher = JS_PATTERN.matcher(bookSource.exploreUrl!!)
        if(bookSource.exploreUrl == null){
            return  ""
        }else if (jsMatcher.find()) {

            val jsEval = bookSource.evalJS(jsMatcher.group(2) ?: jsMatcher.group(1))
            when {
                jsEval is String -> return@run jsEval
                jsEval is Double && jsEval % 1.0 == 0.0 -> return@run  String.format("%.0f", jsEval)
                else -> return@run  jsEval.toString()
            }
        }else{
            return bookSource.exploreUrl!!
        }
    }

    suspend fun searchBook(key: String, page: Int? = 1): List<SearchBook> {
        val variableBook = SearchBook(origin = sourceUrl)
        variableBook.userid=userid?:""
        variableBook.searchinit()
        return bookSource.searchUrl?.let {searchUrl->
            val analyzeUrl = AnalyzeUrl(
                mUrl = searchUrl,
                key = key,
                page = page,
                baseUrl = bookSource.bookSourceUrl,
                source = bookSource,
                ruleData = variableBook,
                headerMapF = bookSource.getHeaderMap(true),
            )

            var res = analyzeUrl.getStrResponseAwait()

            if(debugger != null){
                debugger?.log("搜索源码Qwq${res.body}");
            }

            bookSource.loginCheckJs?.let { checkJs ->
                if (checkJs.isNotBlank()) {
                    res = analyzeUrl.evalJS(checkJs, res) as StrResponse
                }
            }

            BookList.analyzeBookList(
                res.body,
                bookSource,
                analyzeUrl,
                res.url,
                variableBook,
                true,
                debugLog = debugger
            ).map {
                it.tocHtml = ""
                it.infoHtml = ""
                it
            }
        }?:arrayListOf()
    }

    /**
     * 发现
     */
    suspend fun exploreBook(
        url: String,
        page: Int? = 1
    ): List<SearchBook> {
        val variableBook =  SearchBook(origin = sourceUrl)
        variableBook.userid=userid?:""
        variableBook.searchinit()
        val analyzeUrl = AnalyzeUrl(
            mUrl = url,
            page = page,
            baseUrl = bookSource.bookSourceUrl,
            source = bookSource,
            ruleData = variableBook,
            headerMapF = bookSource.getHeaderMap(true)
        )
        var res = analyzeUrl.getStrResponseAwait()
        if(debugger != null){
            debugger?.log("发现源码Qwq${res.body}");
        }
        //检测书源是否已登录
        bookSource.loginCheckJs?.let { checkJs ->
            if (checkJs.isNotBlank()) {
                res = analyzeUrl.evalJS(checkJs, result = res) as StrResponse
            }
        }
        return BookList.analyzeBookList(
            res.body,
            bookSource,
            analyzeUrl,
            res.url,
            variableBook,
            false,
            debugLog = debugger
        )
    }

    /**
     * 书籍信息
     */
    suspend fun getBookInfo(book: Book, canReName: Boolean = true): Book {
        book.type = bookSource.bookSourceType
        if (!book.infoHtml.isNullOrEmpty()) {
            book.userid=userid?:""
            BookInfo.analyzeBookInfo(
                book,
                book.infoHtml,
                bookSource,
                book.bookUrl,
                book.bookUrl,
                canReName
            )
            return book
        } else {
            return getBookInfo(book.bookUrl, canReName)
        }
    }

    /**
     * 书籍信息
     */
    suspend fun getBookInfo(bookUrl: String, canReName: Boolean = true): Book {
        val book = Book()
        book.bookUrl = bookUrl
        book.userid=userid?:""
        book.origin = bookSource.bookSourceUrl
        book.originName = bookSource.bookSourceName
        book.originOrder = bookSource.customOrder
        book.type = bookSource.bookSourceType
        val analyzeUrl = AnalyzeUrl(
            mUrl = book.bookUrl,
            baseUrl = bookSource.bookSourceUrl,
            source = bookSource,
            ruleData = book,
            headerMapF = bookSource.getHeaderMap(true)
        )
        var res = analyzeUrl.getStrResponseAwait()
        if(debugger != null){
            debugger?.log("书籍源码Qwq${res.body}");
        }
        //检测书源是否已登录
        bookSource.loginCheckJs?.let { checkJs ->
            if (checkJs.isNotBlank()) {
                res = analyzeUrl.evalJS(checkJs, result = res) as StrResponse
            }
        }

        BookInfo.analyzeBookInfo(book, res.body, bookSource, book.bookUrl, res.url, canReName, debugLog = debugger)
        book.tocHtml = null
        return book
    }

    /**
     * 目录
     */
    suspend fun getChapterList(
        book: Book
    ): List<BookChapter> {
        book.userid=userid?:""
        book.type = bookSource.bookSourceType
        return if (book.bookUrl == book.tocUrl && !book.tocHtml.isNullOrEmpty()) {
            BookChapterList.analyzeChapterList(
                book,
                book.tocHtml,
                bookSource,
                book.tocUrl,
                book.tocUrl
            )
        } else {
            val analyzeUrl = AnalyzeUrl(
                mUrl = book.tocUrl,
                baseUrl = book.bookUrl,
                source = bookSource,
                ruleData = book,
                headerMapF = bookSource.getHeaderMap(true)
            )
            var res = analyzeUrl.getStrResponseAwait()
            if(debugger != null){
                debugger?.log("目录源码Qwq${res.body}");
            }
            //检测书源是否已登录
            bookSource.loginCheckJs?.let { checkJs ->
                if (checkJs.isNotBlank()) {
                    res = analyzeUrl.evalJS(checkJs, result = res) as StrResponse
                }
            }
            return BookChapterList.analyzeChapterList(book, res.body, bookSource, book.tocUrl, res.url, debugLog = debugger)
        }
    }

    /**
     * 章节内容
     */
    suspend fun getBookContent(
        book: Book,
        bookChapter: BookChapter,
        // bookChapterUrl:String,
        nextChapterUrl: String? = null
    ): String {
        book.userid=userid?:""
        //println(bookSource.ruleContent?.content)
        //println(GSON.toJson(bookChapter))
        if (bookSource.getContentRule().content.isNullOrEmpty()) {
            debugger?.log(bookSource.bookSourceUrl, "⇒正文规则为空,使用章节链接: ${bookChapter.url}")
            return bookChapter.url
        }
        if (bookChapter.isVolume && bookChapter.url.startsWith(bookChapter.title)) {
            debugger?.log(bookSource.bookSourceUrl, "⇒一级目录正文不解析规则")
            return bookChapter.tag ?: ""
        }
//        val body = if (book != null && bookChapter.url == book.bookUrl && !book.tocHtml.isNullOrEmpty()) {
//            book.tocHtml
//        } else {
        logger.info("bookChapterUrl: {}", bookChapter.url, bookChapter.getAbsoluteURL())
        val analyzeUrl = AnalyzeUrl(
            mUrl = bookChapter.getAbsoluteURL(),
            baseUrl = book.tocUrl,
            source = bookSource,
            ruleData = book,
            chapter = bookChapter,
            headerMapF = bookSource.getHeaderMap(true)
        )
        var res = analyzeUrl.getStrResponseAwait(
            jsStr = bookSource.getContentRule().webJs,
            sourceRegex = bookSource.getContentRule().sourceRegex,
        )
        if(debugger != null){
            debugger?.log("正文源码Qwq${res.body}");
        }
        return BookContent.analyzeContent(
            res.body,
            book,
            bookChapter,
            bookSource,
            bookChapter.url,
            res.url,
            nextChapterUrl,
            debugLog = debugger
        )
    }
}