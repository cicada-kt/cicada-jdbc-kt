package cn.org.cicada.jdbc.kt.dao

interface DaoProvider {
    fun update(sql: String, vararg params: Any?)
    fun <T> query(type: Class<T>, sql: String, vararg params: Any?) : List<T>
}
