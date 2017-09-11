package cn.org.cicada.jdbc.kt.api

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Entity(val name : String = "")

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Column(
        val name : String = "",
        val lob : Boolean = false,
        val length : Int = 0,
        val nullable : Boolean = true,
        val unique : Boolean = false,
        val precision : Int = 0,
        val scale : Int = 0)

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class Id(val autoincrement: Boolean = false,
                    val sequence: String = "")