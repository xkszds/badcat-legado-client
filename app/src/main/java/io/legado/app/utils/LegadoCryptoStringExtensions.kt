package io.legado.app.utils

fun String.isHex(): Boolean = all { c ->
    c in '0'..'9' || c in 'A'..'F' || c in 'a'..'f'
}
