// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.contracts;

import java.io.InputStream;
import java.io.OutputStream;

public interface Configuration {
    void load();

    void save();

    void clearProperties();

    boolean isDebugEnabled();

    void load(InputStream inputStream);

    void save(OutputStream outputStream);

    String getPrivateKeyPath();

    String getCertificateChainPath();
}
