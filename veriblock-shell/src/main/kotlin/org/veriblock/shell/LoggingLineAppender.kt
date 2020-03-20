// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.shell

import ch.qos.logback.classic.layout.TTLLLayout
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import ch.qos.logback.core.encoder.Encoder
import java.nio.charset.Charset

var currentShell: Shell? = null

class LoggingLineAppender : AppenderBase<ILoggingEvent>() {

    private val layout = TTLLLayout()

    override fun start() {
        super.start()
        layout.start()
    }

    override fun stop() {
        layout.stop()
        super.stop()
    }

    lateinit var encoder: Encoder<ILoggingEvent> // Initialized by the logger config

    override fun append(event: ILoggingEvent) {
        currentShell?.reader?.printAbove(encoder.encode(event).toString(Charset.defaultCharset()))
    }
}
