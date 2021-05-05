package org.veriblock.spv

object SpvConstants {
    val PROGRAM_NAME = SpvConstants::class.java.getPackage().specificationTitle
    val PROGRAM_VERSION = SpvConstants::class.java.getPackage().implementationVersion
    val FULL_PROGRAM_NAME_VERSION = "$PROGRAM_NAME v$PROGRAM_VERSION"
    val PLATFORM = System.getProperty("os.name") + " | " + System.getProperty("java.version")
}
