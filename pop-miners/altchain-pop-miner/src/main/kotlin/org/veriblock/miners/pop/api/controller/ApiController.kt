// VeriBlock NodeCore
// Copyright 2017-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.miners.pop.api.controller;

import com.papsign.ktor.openapigen.route.path.auth.OpenAPIAuthenticatedRoute
import io.ktor.auth.UserIdPrincipal

interface ApiController {
    fun OpenAPIAuthenticatedRoute<UserIdPrincipal>.registerApi()
}
