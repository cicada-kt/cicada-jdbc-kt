package cn.org.cicada.jdbc.kt.dao

open class JdbcException(message: String, cause: Throwable? = null) :
        RuntimeException(message, cause)

class NotUniqueException(message: String, cause: Throwable? = null) :
        JdbcException(message, cause)

class EmptyResultException(message: String, cause: Throwable? = null) :
        JdbcException(message, cause)