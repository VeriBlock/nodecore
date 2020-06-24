package org.veriblock.miners.pop.util

sealed class CheckResult {
    class Success : CheckResult()
    class Failure(val error: Throwable) : CheckResult()
}
