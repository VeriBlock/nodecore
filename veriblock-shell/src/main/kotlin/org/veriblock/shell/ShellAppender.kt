// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.shell

import org.apache.logging.log4j.core.Filter
import org.apache.logging.log4j.core.Layout
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.appender.AbstractAppender
import org.apache.logging.log4j.core.config.Property
import org.apache.logging.log4j.core.config.plugins.Plugin
import org.apache.logging.log4j.core.config.plugins.PluginAttribute
import org.apache.logging.log4j.core.config.plugins.PluginElement
import org.apache.logging.log4j.core.config.plugins.PluginFactory
import org.apache.logging.log4j.core.layout.PatternLayout
import java.io.Serializable
import java.nio.charset.Charset

var currentShell: Shell? = null

@Plugin(name = "shell", category = "Core", elementType = "appender", printObject = true)
class ShellAppender private constructor(
    name: String,
    filter: Filter?,
    layout: Layout<out Serializable>,
    ignoreExceptions: Boolean
) : AbstractAppender(name, filter, layout, ignoreExceptions, Property.EMPTY_ARRAY) {

    override fun append(event: LogEvent) {
        val message = layout.toByteArray(event).toString(Charset.defaultCharset())
        val currentShell = currentShell
        if (currentShell != null) {
            if (!currentShell.logsMuted || event.loggerName == "shell-printing") { // TODO extract to constant
                currentShell.reader.printAbove(message)
            }
        } else {
            print(message)
        }
    }

    companion object {
        @JvmStatic
        @PluginFactory
        fun createAppender(
            @PluginAttribute("name") name: String?,
            @PluginElement("filters") filter: Filter?,
            @PluginElement("layout") layout: Layout<out Serializable>?,
            @PluginAttribute("ignoreExceptions") ignoreExceptions: Boolean
        ): ShellAppender? {
            if (name == null) {
                LOGGER.error("No name provided for StubAppender")
                return null
            }
            return ShellAppender(name, filter, layout ?: PatternLayout.createDefaultLayout(), ignoreExceptions)
        }
    }
}
