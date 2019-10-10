// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.shell

import io.kotlintest.matchers.string.shouldContain
import org.junit.Ignore
import org.junit.Test
import org.veriblock.shell.core.success
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class ShellTest {
    private val outStream = ByteArrayOutputStream()

    @Test
    @Ignore
    fun test() {
        val input = """
test
greet
greet World
        """.trimStart()

        Shell(input.byteInputStream(), PrintStream(outStream)).apply {
            command(
                name = "Greet",
                form = "greet",
                description = "Greets someone",
                parameters = listOf(
                    CommandParameter("who", CommandParameterType.STRING)
                )
            ) {
                val who: String = getParameter("who")
                printInfo("Hello $who!")
                success()
            }
        }.run()

        val output = outStream.toString("UTF-8")
        output shouldContain "[V004] Unknown protocol command"
        output shouldContain "The command 'test' is not supported"
        output shouldContain "[V009] Syntax error"
        output shouldContain "[V004] Unknown protocol command"
        output shouldContain "Usage: greet <who> ERROR: parameter 'who' is required"
        output shouldContain "Hello World!"
    }
}
