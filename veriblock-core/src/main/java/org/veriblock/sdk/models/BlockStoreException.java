// VeriBlock Blockchain Project
// Copyright 2017-2018 VeriBlock, Inc
// Copyright 2018-2021 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package org.veriblock.sdk.models;

public class BlockStoreException extends RuntimeException {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

    public BlockStoreException(String message) {
        super(message);
    }

    public BlockStoreException(Throwable cause) {
        super(cause);
    }
}
