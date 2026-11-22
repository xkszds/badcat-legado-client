package io.legado.app.utils

/** JVM-hosted debug logging equivalent for the pinned upstream parser's error path. */
fun Throwable.printOnDebug() {
    printStackTrace()
}
