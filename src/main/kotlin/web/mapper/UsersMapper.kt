package web.mapper

import com.baomidou.mybatisplus.core.mapper.BaseMapper
import org.apache.ibatis.annotations.Delete
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select
import org.apache.ibatis.annotations.Update
import web.model.Users

interface  UsersMapper : BaseMapper<Users> {
    @Select("SELECT * FROM users WHERE id = #{id}")
    fun getUser(@Param("id") id: String): Users?

    @Select("SELECT * FROM users")
    fun getAllUser(): List<Users>

    @Select("SELECT * FROM users WHERE username = #{username}" )
    fun getUserByusername(@Param("username") username: String): Users?

    @Select("SELECT * FROM users WHERE email = #{email}" )
    fun getUserByemail(@Param("email") email: String): List<Users>

    @Update("UPDATE users set password = #{password}   WHERE id = #{id}")
    fun changepass(@Param("id") id: String,@Param("password") password: String):Int

    @Delete("Delete booklist,users,usertocken  FROM booklist,users,usertocken WHERE booklist.userid = #{id} and usertocken.userid = #{id} and users.id = #{id}")
    fun delUserall(@Param("id") id: String): Int

    @Update("<script>  UPDATE users set email = #{user.email}  , phone = #{user.phone} , updatetime = #{user.updatetime} ," +
            "<if test=\"user.password != null\">" +
            "password = #{user.password}," +
            "</if> " +
            "allow_up_txt = #{user.AllowUpTxt} , comment = #{user.comment}  ,allow_cache = #{user.AllowCache} ,source  = #{user.source} " +
            "WHERE id = #{user.id}</script>")
    fun updateinfo(@Param("user") user: Users):Int


    @Delete("DROP TABLE users")
    fun Drop(): Int
}