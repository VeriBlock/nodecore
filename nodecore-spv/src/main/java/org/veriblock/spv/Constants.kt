package org.veriblock.spv

object Constants {
    val PROGRAM_NAME = Constants::class.java.getPackage().specificationTitle
    val PROGRAM_VERSION = Constants::class.java.getPackage().implementationVersion
    val FULL_PROGRAM_NAME_VERSION = "$PROGRAM_NAME v$PROGRAM_VERSION"
    val PLATFORM = System.getProperty("os.name") + " | " + System.getProperty("java.version")
}
