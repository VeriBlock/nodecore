// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package bootstrap.downloader

object ApplicationMeta {
    private val APPLICATION_NAME: String? = ApplicationMeta::class.java.getPackage().specificationTitle
    private val APPLICATION_VERSION: String? = ApplicationMeta::class.java.getPackage().specificationVersion
    val FULL_APPLICATION_NAME_VERSION = "$APPLICATION_NAME v$APPLICATION_VERSION"
}
