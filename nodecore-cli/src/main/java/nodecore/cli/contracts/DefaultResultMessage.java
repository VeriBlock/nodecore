// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.contracts;

public class DefaultResultMessage implements ResultMessage {
    private String _code;
    private String _message;
    private String _details;
    private boolean _isError;

    public DefaultResultMessage(String code, String message, String details, boolean error) {
        _code = code;
        _message = message;
        _details = details;
        _isError = error;
    }

    @Override
    public String getCode() {
        return _code;
    }

    @Override
    public String getMessage() {
        return _message;
    }

    @Override
    public String getDetails() {
        return _details;
    }

    @Override
    public boolean isError() {
        return _isError;
    }
}
