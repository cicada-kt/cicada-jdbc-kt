package cn.org.cicada.jdbc.kt.query

import cn.org.cicada.jdbc.kt.dao.JdbcDao
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

class Stmt(val sql : String, val params: List<Any?>) {

    companion object {
        fun build(jdbcDao: JdbcDao, f: Builder.()->Unit) : Stmt {
            return Builder(jdbcDao, f).build()
        }
    }

    class Builder(val jdbcDao: JdbcDao, f: Builder.()->Unit) {
        private val sql = StringBuilder(256)
        private val params = mutableListOf<Any?>()

        internal fun build(): Stmt = Stmt(sql.toString(), params)

        operator fun String.unaryPlus() {
            sql.append(this)
        }

        operator fun (()->String).unaryPlus() {
            try {
                sql.append(this()).append(" ")
            } catch (ex : NullPointerException) { }
        }

        inline fun <reified E:Any> cols(alias: String = "") : ColumnList<E> = ColumnList(jdbcDao, alias, E::class)
        inline fun <E:Any> cols(entityClass:KClass<E>, alias: String = "") : ColumnList<E> {
            return ColumnList(jdbcDao, alias, entityClass)
        }

        inline fun <reified E:Any> cols(vararg properties: KProperty1<E,*>) : String  = cols("", *properties)
        inline fun <reified E:Any> cols(alias: String, vararg properties: KProperty1<E,*>) : String {
            val prefix = if (alias.isEmpty()) "" else "$alias."
            return properties.joinToString {
                "$prefix${col(it)}"
            }
        }

        fun param(p: Any?): String {
            params.add(p)
            return "?"
        }

        fun params(vararg p: Any?): String {
            p.forEach {
                params.add(it)
            }
            return p.joinToString { "?" }
        }

        inline fun <reified E:Any> col(property: KProperty1<E, *>): String {
            return jdbcDao {
                val column = colomn(property)
                if (column !== null) {
                    quote(column.name)
                } else {
                    toString()
                }
            }
        }

        inline fun <reified T:Any> table() : String = table(T::class)

        fun quote(string: String) : String = jdbcDao.queryFactory.quote(string)

        fun table(entityClass: KClass<*>): String {
            val table = jdbcDao.table(entityClass)
            return if (table !== null) {
                quote(table.name)
            } else {
                toString()
            }
        }

        inner class ColumnList<E:Any>(jdbcDao: JdbcDao, val alias: String, entityClass: KClass<E>) {
            val table = jdbcDao.table(entityClass)!!
            val columns = mutableSetOf(*table.columns.values.toTypedArray())

            fun removeIdCols() : ColumnList<E> {
                columns.removeIf { it.primaryKey }
                return this
            }

            fun removeAutoincrement() : ColumnList<E> {
                columns.removeIf { it.autoincrement }
                return this
            }

            fun idCols() : ColumnList<E> {
                columns.removeIf { ! it.primaryKey }
                return this
            }

            operator fun minus(property: KProperty1<*, *>) : ColumnList<E> {
                columns.remove(table.columns[property] as JdbcDao.Column<E>)
                return this
            }

            fun join(separator: CharSequence = ",", prefix: CharSequence = "", postfix: CharSequence = "",
                     limit: Int = -1, truncated: CharSequence = "...",
                     transform: ((JdbcDao.Column<E>) -> CharSequence)?) {
                columns.joinTo(sql, separator, prefix, postfix, limit, truncated, transform)
            }

            override fun toString(): String {
                val prefix = if (alias.isEmpty()) "" else "$alias."
                return columns.joinToString { "$prefix${quote(it.name)}" }
            }
        }

        init {
            this.f()
        }
    }


}