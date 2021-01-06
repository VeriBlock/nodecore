// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.
package nodecore.cli

object Constants {
    private val APPLICATION_NAME: String? = Constants::class.java.getPackage().specificationTitle
    private val APPLICATION_VERSION: String? = Constants::class.java.getPackage().specificationVersion

    val FULL_APPLICATION_NAME_VERSION = "$APPLICATION_NAME v$APPLICATION_VERSION"
    const val DEFAULT_PROPERTIES = "nodecore-cli-default.properties"
}
