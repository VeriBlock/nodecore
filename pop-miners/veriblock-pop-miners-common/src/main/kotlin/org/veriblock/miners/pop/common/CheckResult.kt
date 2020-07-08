package org.veriblock.miners.pop.common

sealed class CheckResult {
    class Success : CheckResult()
    class Failure(val error: Throwable) : CheckResult()
}
