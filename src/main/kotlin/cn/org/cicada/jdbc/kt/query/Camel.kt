package cn.org.cicada.jdbc.kt.query

fun String.toCamel(firstUppercase: Boolean = true) : String {
    val text = StringBuilder(this.length)
    var uppercase = firstUppercase
    for (i in this.indices) {
        if (this[i] === '_') {
            uppercase = true
            continue
        }
        text.append(if (uppercase) this[i].toUpperCase() else this[i].toLowerCase())
        uppercase = false
    }
    return text.toString()
}

fun String.toUnderline(lowercase: Boolean = true) : String {
    val text = StringBuilder(this.length * 2)
    for (i in this.indices) {
        if (this[i] in 'A'..'Z') {
            if (text.isNotEmpty()) {
                text.append('_')
            }
            text.append(if (lowercase) this[i].toLowerCase() else this[i])
        } else {
            text.append(if (lowercase) this[i] else this[i].toUpperCase())
        }
    }
    return text.toString()
}
