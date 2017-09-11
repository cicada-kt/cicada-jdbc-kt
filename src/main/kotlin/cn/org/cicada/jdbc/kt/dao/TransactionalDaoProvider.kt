package cn.org.cicada.jdbc.kt.dao

interface TransactionalDaoProvider : DaoProvider {
    fun beginTransaction()
    fun commitTransaction()
    fun rollbackTransaction()
}
