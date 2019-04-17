Secure Connections
==================

To connect to a NodeCore instance running in secure mode, use the following connect command (port 10501 is used for TestNet, port 10500 will be used for MainNet)

    (no connection) > connect https://localhost:10501 [password]
    
The host name (`localhost` in the above example) needs to match the common name (CN) of your server certificate

The above example will only work if your nodecore instance is using a certificate generated from a trusted certificate authority (Thawte, VeriSign, etc.).

If you are using a self-signed certificate or one generated from a untrusted/internal CA, you will need to configure the cli so it knows where to load your root ca cert from. 

By default, the cli looks for a file named ca.crt in it's working directory. To override this behavior, add the following property to your nodecore-cli.properties file

    rpc.security.cert.chain.path = /path/to/ca.crt

