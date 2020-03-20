// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.contracts;

public class Output {
    private String _address;
    private long _amount;

    public Output(String address, long amount) {
        _address = address;
        _amount = amount;
    }

    public String getAddress() {
        return _address;
    }

    public long getAmount() {
        return _amount;
    }
}
