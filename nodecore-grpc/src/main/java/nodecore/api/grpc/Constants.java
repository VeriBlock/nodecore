// VeriBlock NodeCore
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.api.grpc;

import io.grpc.Metadata;

public class Constants {
    public static final Metadata.Key<String> RPC_PASSWORD_HEADER_NAME = Metadata.Key.of("X-VBK-RPC-PASSWORD", Metadata.ASCII_STRING_MARSHALLER);
}
