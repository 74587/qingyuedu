package book.model.rule

data class ContentRule(
    var content: String? = null,
    var nextContentUrl: String? = null,
    var title: String? = null,
    var webJs: String? = null,
    var sourceRegex: String? = null,
    var payAction: String? = null,
    var replaceRegex: String? = null, //替换规则
    var imageStyle: String? = null,  //默认大小居中,FULL最大宽度
    var imageDecode: String? = null, //图片bytes二次解密js, 返回解密后的bytes
)