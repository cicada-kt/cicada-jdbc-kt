# cicada-jdbc-kt

[![Kotlin 1.1.3-2](https://img.shields.io/badge/Kotlin-1.1-blue.svg)](http://kotlinlang.org)
[![MIT License](https://img.shields.io/github/license/cicada-kt/cicada-jdbc-kt.svg)](https://github.com/cicada-kt/cicada-jdbc-kt/blob/master/LICENSE)
[![GitHub issues](https://img.shields.io/github/issues/cicada-kt/cicada-jdbc-kt.svg)](https://github.com/cicada-kt/cicada-jdbc-kt/issues)

Cicada Jdbc for Kotlin
===========================

cicada-jdbc-kt is a very simple and very useful sql builder. it is very easy to use and configure.

cicada-jdbc-kt allow you:

- Mapping a database table to an entity class
- Insert / Delete and Update a row data without sql
- Easily build sql and parameter list

An example is always better than a thousand words:

Define entity:

```kotlin
@Entity
data class Foo(
        @Id(autoincrement = true) @Column var fooId: Int,
        @Column(length = 32) var fooName: String,
        @Column(length = 32) var fooCode: String
)
```

Build a JdbcDao to begin

```kotlin
val dao = JdbcDao.Builder {
    mysql()
    provider = SpringJdbcDaoProvider(JdbcTemplate(DriverManagerDataSource(
            "jdbc:mysql://localhost:3306/chuanner",
            "root", "root")))
}.build()
```

```kotlin
dao {
    val foo = Foo(null, "name", "code")
    insert(foo) // insert a row to table foo
    foo.fooName = "New Name"
    update(foo) // update all fields in foo by foo's id
    delete(foo) // delete a foo row by foo's id
    delete<Foo>(1) // delete a foo row by id = 1
    list<Foo> { // query a list result by a sql 
        + "select ${cols<Foo>().removeIdCols()} " // all fields except id in the foo 
        + "from ${table<Foo>()} as c "
        + "where c.${col(Foo::fooId)} = ${param(1)} " // add parameter
        + { "and c.${col(Foo::fooName)} = ${param(null!!)} " } // when a parameter is null, the part in this block will not in sql and parameter list
    }
}
```
