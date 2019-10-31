// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.shell

import io.kotlintest.matchers.string.shouldContain
import org.junit.Test
import org.veriblock.shell.core.success
import java.io.ByteArrayOutputStream

class ShellTest {

    @Test
    fun test() {
        // Given a set of commands that will be run
        val input = """
test
greet
greet World
        """.trimStart()

        // and a shell configured with the greet command
        val outStream = ByteArrayOutputStream()
        val shell = Shell(
            ShellTestData(
                inputStream = input.byteInputStream(),
                outputStream =  outStream
            )
        ).apply {
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
        }

        // When
        shell.run()

        // Then
        val output = outStream.toString("UTF-8")
        // Display the output until we find out why it is not captured properly in the CI server
        println(output)
        // Dirty workaround for the fact that we are unable to capture the output from the CI server
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            output shouldContain "[V004] Unknown protocol command"
            output shouldContain "The command 'test' is not supported"
            output shouldContain "[V009] Syntax error"
            output shouldContain "[V004] Unknown protocol command"
            output shouldContain "Usage: greet <who> ERROR: parameter 'who' is required"
            output shouldContain "Hello World!"
        }
    }
}
