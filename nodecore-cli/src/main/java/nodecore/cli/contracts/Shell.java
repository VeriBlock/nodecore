// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.contracts;

import org.jline.utils.AttributedStyle;

import javax.net.ssl.SSLException;

public interface Shell {
    Result run();

    void initialize(String host);

    public void format(AttributedStyle style, int fColor, String str);

    ProtocolEndpointType type();

    void format(String fmt, Object... args);

    public boolean connect(ProtocolEndpoint endpoint, boolean save) throws SSLException, ConnectionFailedException;

    String passwordPrompt(String prompt);
}
