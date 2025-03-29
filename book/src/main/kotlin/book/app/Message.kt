package book.app

data class WebMessage (
    val msg: String,
    val url: String,
    val html: String = "",
    val title: String,
    val header:String="",
    val id: String
)

data class ToastMessage (
    val msg: String,
    val str: String,
)