package org.veriblock.miners.pop.core

import mu.KLogger

abstract class MiningOperation(
    val id: String,
    status: OperationStatus
) {
    var status = status
        protected set

    abstract val stateType: OperationStateType
}

// Utility functions for logging on an operation
inline fun KLogger.trace(operation: MiningOperation, crossinline msg: () -> Any?) = trace { "[${operation.id}] ${msg()}" }
inline fun KLogger.debug(operation: MiningOperation, crossinline msg: () -> Any?) = debug { "[${operation.id}] ${msg()}" }
inline fun KLogger.info(operation: MiningOperation, crossinline msg: () -> Any?) = info { "[${operation.id}] ${msg()}" }
inline fun KLogger.warn(operation: MiningOperation, crossinline msg: () -> Any?) = warn { "[${operation.id}] ${msg()}" }
inline fun KLogger.error(operation: MiningOperation, crossinline msg: () -> Any?) = error { "[${operation.id}] ${msg()}" }
inline fun KLogger.error(operation: MiningOperation, e: Throwable, crossinline msg: () -> Any?) = error(e) { "[${operation.id}] ${msg()}" }
