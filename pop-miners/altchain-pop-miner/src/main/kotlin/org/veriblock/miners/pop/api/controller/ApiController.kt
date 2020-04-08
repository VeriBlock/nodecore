// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.api.controller;

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute

interface ApiController {
    fun NormalOpenAPIRoute.registerApi()
}
