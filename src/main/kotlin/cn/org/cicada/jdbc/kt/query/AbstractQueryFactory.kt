package cn.org.cicada.jdbc.kt.query

import cn.org.cicada.jdbc.kt.dao.JdbcDao
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.jvm.javaField

abstract class AbstractQueryFactory : QueryFactory {

    override fun buildInsertSql(jdbcDao: JdbcDao, entity: Any) : Stmt {
        val entityClass = entity.javaClass.kotlin
        return Stmt.build(jdbcDao) {
            val columns = cols(entityClass).removeAutoincrement()
            + "insert into ${table(entityClass)}($columns) values ("
            columns.join {
                param(it.property.get(entity))
            }
            + ")"
        }
    }

    override fun buildUpdateSql(jdbcDao: JdbcDao, entity: Any) : Stmt {
        val entityClass = entity.javaClass.kotlin
        return Stmt.build(jdbcDao) {
            val columns = cols(entityClass).removeIdCols()
            + "update ${table(entityClass)} set "
            columns.join {
                "`${it.name}` = ${param(it.property.get(entity))}"
            }
            + ")"
        }
    }

    override fun buildDeleteSql(jdbcDao: JdbcDao, entity: Any): Stmt {
        val entityClass = entity.javaClass.kotlin
        return Stmt.build(jdbcDao) {
            val idCols = cols(entityClass).idCols()
            + "delete from ${table(entityClass)} where "
            idCols.join(" and ") {
                "`${it.name}` = ${param(it.property.get(entity))}"
            }
        }
    }

    override fun <E:Any> buildDeleteSql(jdbcDao: JdbcDao, entityClass: KClass<E>, vararg ids: Serializable) : Stmt {
        return Stmt.build(jdbcDao) {
            val idCols = cols(entityClass).idCols()
            + "delete from ${table(entityClass)} where "
            var i = 0
            idCols.join(" and ") {
                "`${it.name}` = ${param(ids[i++])}"
            }
        }
    }

    override fun quote(string: String): String = "`$string`"

    fun columnType(property: KProperty<*>) : String {
        val column = property.javaField!!.getAnnotation(cn.org.cicada.jdbc.kt.api.Column::class.java)
        val type = property.returnType
        @Suppress("UNCHECKED_CAST")
        if (isEnum(type)) return enumType(type as KClass<out Enum<*>>)
        return when(type) {
            String::class -> if (column.lob) textType() else varcharType(column.length)
            Int::class, Long::class -> intType(column.length)
            Double::class -> doubleType()
            Float::class -> floatType()
            BigDecimal::class -> decimalType(column.precision, column.scale)
            LocalDateTime::class -> datetimeType()
            LocalDate::class -> dateType()
            LocalTime::class -> timeType()
            Array<Byte>::class -> if (column.lob) blobType() else throw IllegalArgumentException("column bytes type must be a lob")
            else -> throw IllegalArgumentException("Unsupported column type: $type")
        }
    }

    private fun isEnum(type: KType) = type is KClass<*> && type.java.isEnum

    protected open fun varcharType(length: Int) = "VARCHAR($length)"
    protected open fun textType() = "TEXT"
    protected open fun intType(length: Int) = "INT($length)"
    protected open fun floatType() = "FLOAT"
    protected open fun doubleType() = "DOUBLE"
    protected open fun decimalType(precision: Int, scale: Int) = "DECIMAL($precision, $scale)"
    protected open fun datetimeType() = "DATETIME"
    protected open fun dateType() = "DATE"
    protected open fun timeType() = "TIME"
    protected open fun enumType(enumClass: KClass<out Enum<*>>) = "ENUM($enumClass)"
    protected open fun blobType() = "BLOB"

}