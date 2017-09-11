package cn.org.cicada.jdbc.kt.api

data class Page<out T>(
        val page: Long,
        val pageSize: Int,
        val data: List<T>,
        val totalSize: Long
)
