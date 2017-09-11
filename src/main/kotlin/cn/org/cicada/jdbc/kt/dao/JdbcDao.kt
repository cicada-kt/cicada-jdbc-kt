package cn.org.cicada.jdbc.kt.dao

import cn.org.cicada.jdbc.kt.api.Entity
import cn.org.cicada.jdbc.kt.query.QueryFactory
import cn.org.cicada.jdbc.kt.query.Stmt
import cn.org.cicada.jdbc.kt.query.toUnderline
import java.io.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaField

open class JdbcDao private constructor(
        val schemas : Map<String,Schema>,
        val defaultSchema : Schema,
        val queryFactory: QueryFactory,
        val provider: DaoProvider) {

    private constructor(builder: Builder) : this(
            builder.schemas,
            builder.defaultSchema ?: Schema(""),
            builder.queryFactory!!,
            builder.provider!!)

    companion object {
        fun build(f : Builder.()->Unit) : JdbcDao = Builder(f).build()
    }

    private val tables = mutableMapOf<KClass<*>,Table<*>>()

    operator fun <T> invoke(f: JdbcDao.() -> T) : T {
        (provider as? TransactionalDaoProvider).let {
            try {
                it?.beginTransaction()
                val result = this.f()
                it?.commitTransaction()
                return result
            } catch (ex : Exception) {
                it?.rollbackTransaction()
                throw ex
            }
        }
    }

    inline fun <reified T:Any> insert(entity: T) {
        val stmt = queryFactory.buildInsertSql(this@JdbcDao, entity)
        println(stmt.sql)
        println(stmt.params)
        provider.update(stmt.sql, *stmt.params.toTypedArray())
    }

    inline fun <reified T:Any> update(entity: T) {
        val stmt = queryFactory.buildUpdateSql(this@JdbcDao, entity)
        println(stmt.sql)
        println(stmt.params)
        provider.update(stmt.sql, *stmt.params.toTypedArray())
    }

    inline fun <reified T:Any> delete(entity: T) {
        val stmt = queryFactory.buildDeleteSql(this@JdbcDao, entity)
        println(stmt.sql)
        println(stmt.params)
        provider.update(stmt.sql, *stmt.params.toTypedArray())
    }

    inline fun <reified T:Any> delete(vararg ids : Serializable) {
        val stmt = queryFactory.buildDeleteSql(this@JdbcDao, T::class, *ids)
        println(stmt.sql)
        println(stmt.params)
        provider.update(stmt.sql, *stmt.params.toTypedArray())
    }

    inline fun <reified T:Any> list(noinline f: Stmt.Builder.()->Unit) : List<T> {
        val stmt = Stmt.build(this, f)
        println(stmt.sql)
        println(stmt.params)
        return provider.query(T::class.java, stmt.sql, stmt.params.toTypedArray())
    }

    inline fun <reified T:Any> uniqueResult(noinline f: Stmt.Builder.()->Unit) : T {
        val stmt = Stmt.build(this, f)
        println(stmt.sql)
        println(stmt.params)
        val list = provider.query(T::class.java, stmt.sql, stmt.params.toTypedArray())
        when (list.size) {
            1 -> return list.first()
            0 -> throw EmptyResultException("Unique result not found")
            else -> throw NotUniqueException("Unique result but found ${list.size} rows.")
        }
    }

    inline fun <reified T:Any> firstResult(noinline f: Stmt.Builder.()->Unit) : T? {
        val stmt = Stmt.build(this, f)
        println(stmt.sql)
        println(stmt.params)
        val list = provider.query(T::class.java, stmt.sql, stmt.params.toTypedArray())
        return if (list.isEmpty()) null else list.first()
    }

    class Builder(f : Builder.()->Unit) {
        init {
            this.f()
        }

        val schemas = mutableMapOf<String,Schema>()
        var defaultSchema : Schema? = null
        var queryFactory : QueryFactory? = null
        var provider: DaoProvider? = null

        fun bind(pkgName: String, schema: Schema, default: Boolean = false) {
            schemas[pkgName] = schema
            if (default) {
                defaultSchema = schema
            }
        }

        fun build() : JdbcDao = JdbcDao(this)
    }

    protected fun schemaOf(entityClass: KClass<*>) : Schema = schemaOf(entityClass.java.`package`.name)

    protected fun schemaOf(pkgName: String) : Schema {
        val schema = schemas[pkgName]
        if (schema === null) {
            val p = pkgName.lastIndexOf('.')
            if (p < 0) return defaultSchema
            return schemaOf(pkgName.substring(0, p))
        }
        return schema
    }

    private fun <E:Any> tableOf(entityClass: KClass<E>) : Table<E>? {
        val entity = entityClass.java.getAnnotation(Entity::class.java)
        if (entity === null) return null
        val schema = schemaOf(entityClass)
        val tablename = if (entity.name == "") {
            schema.tableName(entityClass.simpleName!!)
        } else {
            entity.name
        }
        val columns = LinkedHashMap<KProperty1<E,*>,Column<E>>()
        val table = Table(tablename, schema, columns)
        entityClass.declaredMemberProperties.forEach {
            val field = it.javaField
            field ?: return@forEach
            val col = field.getAnnotation(cn.org.cicada.jdbc.kt.api.Column::class.java)
            col ?: return@forEach
            val columnName = if (col.name == "") it.name.toUnderline(schema.lowercaseColumnName) else col.name
            val id = field.getAnnotation(cn.org.cicada.jdbc.kt.api.Id::class.java)
            columns[it] = Column(it, columnName, table,
                    col.length, col.precision, col.scale,
                    col.nullable, col.unique,
                    id !== null,
                    id !== null && id.autoincrement)
        }
        return table
    }

    inline fun <reified T:Any> table() : Table<T>? = table(T::class)
    fun <E:Any> table(entityClass: KClass<E>) : Table<E>? {
        synchronized(tables) {
            val table = tables[entityClass]
            @Suppress("UNCHECKED_CAST")
            if (table !== null) return table as Table<E>
            return tableOf(entityClass)
        }
    }

    inline fun <reified E:Any> colomn(property: KProperty1<E,*>) : Column<E>? {
        val field = property.javaField
        field ?: return null
        val table = table<E>()
        table ?: return null
        return table.columns[property]
    }

    data class Schema(val name: String,
                      val lowercaseTableName: Boolean = true,
                      val lowercaseColumnName: Boolean = true,
                      val tablePrefix: String = "",
                      val tableSuffix: String = "") {
        fun tableName(entityClassName: String) : String {
            val schemaPrefix = if (name == "") "" else "$name."
            val simpleName = entityClassName.toUnderline(lowercaseTableName)
            return "$schemaPrefix$tablePrefix$simpleName$tableSuffix"
        }
    }

    data class Table<E>(val name: String,
                     val schema: Schema,
                     val columns : Map<KProperty1<E,*>,Column<E>>) {
        override fun hashCode(): Int = name.hashCode() + schema.hashCode()
        override fun equals(other: Any?): Boolean {
            other ?: return false
            other as? Table<*> ?: return false
            return name == other.name && schema == other.schema
        }
    }

    data class Column<E>(
            val property: KProperty1<E,*>,
            val name: String,
            val table: Table<E>,
            val length : Int = 0,
            val precision : Int = 0,
            val scale : Int = 0,
            val nullable : Boolean = true,
            val unique : Boolean = false,
            val primaryKey: Boolean,
            val autoincrement: Boolean)

}
