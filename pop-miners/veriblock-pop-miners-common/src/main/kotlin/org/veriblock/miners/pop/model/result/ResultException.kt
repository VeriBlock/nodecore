package org.veriblock.miners.pop.model.result

open class ResultException(
    val code: String,
    override val message: String,
    val details: String
) : RuntimeException()

class OperationNotFoundException(
    details: String
) : ResultException("V404", "Operation not found", details)
