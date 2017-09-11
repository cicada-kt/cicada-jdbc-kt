package cn.org.cicada.jdbc.kt.mysql

import cn.org.cicada.jdbc.kt.dao.JdbcDao

fun JdbcDao.Builder.mysql() {
    try {
        Class.forName("com.mysql.cj.jdbc.Driver")
    } catch (ex : ClassNotFoundException) {
        Class.forName("com.mysql.jdbc.Driver")
    }
    queryFactory = MysqlQueryFactory()
}
