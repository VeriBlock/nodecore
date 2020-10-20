package org.veriblock.spv.service

class StatefulIterable<out T>(wrapped: Sequence<T>) : Iterable<T> {
    private val iterator = wrapped.iterator()
    override fun iterator() = iterator
}

// make sequence remember its last state. e.g. sequential .take(...) will not be overlapping
fun <T> Sequence<T>.asStateful(): Sequence<T> = StatefulIterable(this).asSequence()
