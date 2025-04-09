package web.model


import com.baomidou.mybatisplus.annotation.FieldFill
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.google.gson.Gson
import org.dromara.autotable.annotation.AutoTable
import org.dromara.autotable.annotation.ColumnType
import org.dromara.autotable.annotation.PrimaryKey
import org.noear.snack.annotation.ONodeAttr
import java.time.LocalDateTime
import book.model.BookSource as Booksource

@AutoTable(value = "book_source")
class BookSource {
    @TableId
    @PrimaryKey
     var bookSourceUrl: String? = null           // 地址，包括 http/https
     var bookSourceName: String? = null           // 名称
     var bookSourceGroup: String? = null
     var sourceorder: Int? = null
     var bookSourceType: Int? = null         // 类型，0 文本，1 音频
    @ColumnType(value = "LONGTEXT")
    var exploreUrl: String? = null                 // 发现url
    var enabled: Boolean? = null         // 是否启用
        get() {
            if(field == null) {
                return false
            }
            return field
        }
    var enabledExplore: Boolean? = null     //启用发现
        get() {
            if(field == null) {
                return false
            }
            return field
        }
    @ColumnType(value = "LONGTEXT")
    var bookSourceComment: String? = null           // 注释
    var lastUpdateTime: Long? = null            // 最后更新时间，用于排序
    //var tag: String? = null
    var signkey:String? = null
    @ColumnType(value = "LONGTEXT")
    var json: String? = null                 // 发现url

    @ONodeAttr(format = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT,value = "createtime")
    var createtime: LocalDateTime? = null

    fun jsontomodel(bookSource: Booksource) :BookSource {
        this.bookSourceName = bookSource.bookSourceName
        this.bookSourceGroup = bookSource.bookSourceGroup
        this.bookSourceType = bookSource.bookSourceType
        this.bookSourceUrl = bookSource.bookSourceUrl
        this.exploreUrl = bookSource.exploreUrl
        this.enabled = true
        this.enabledExplore = bookSource.enabledExplore
        this.bookSourceComment = bookSource.bookSourceComment
        this.lastUpdateTime = bookSource.lastUpdateTime
        //this.tag = bookSource.getTag()
        this.createtime = LocalDateTime.now()
        this.json= Gson().toJson(bookSource)
        return this
    }

    fun toBaseSource(): BaseSource {
        return BaseSource(
            bookSourceUrl = this.bookSourceUrl?:"",
            bookSourceName = this.bookSourceName?:"",
            bookSourceGroup = this.bookSourceGroup,
            bookSourceType = this.bookSourceType,
            sourceorder = this.sourceorder,
            enabledExplore = this.enabledExplore,
            enabled = this.enabled?:false,
            json = this.json?:"{}"
        )
    }
}