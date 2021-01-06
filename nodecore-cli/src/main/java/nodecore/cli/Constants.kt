// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli

object Constants {
    val APPLICATION_NAME = Constants::class.java.getPackage().specificationTitle
    @JvmField
    val APPLICATION_VERSION = Constants::class.java.getPackage().specificationVersion
    @JvmField
    val FULL_APPLICATION_NAME_VERSION = APPLICATION_NAME + " v" + APPLICATION_VERSION
    const val DEFAULT_PROPERTIES = "nodecore-cli-default.properties"
}
