package cn.org.cicada.jdbc.kt.query

import cn.org.cicada.jdbc.kt.dao.JdbcDao
import java.io.Serializable
import kotlin.reflect.KClass

interface QueryFactory {
    fun quote(string: String) : String
    fun buildInsertSql(jdbcDao: JdbcDao, entity: Any) : Stmt
    fun buildUpdateSql(jdbcDao: JdbcDao, entity: Any) : Stmt
    fun buildDeleteSql(jdbcDao: JdbcDao, entity: Any) : Stmt
    fun <E:Any> buildDeleteSql(jdbcDao: JdbcDao, entityClass: KClass<E>, vararg ids: Serializable) : Stmt
}
