Introduction
============
The NodeCore CLI tool allows you connect to the RPC protocol.

Because of a known bug with gradle, to run the CLI from source you shoud execute:

    gradle --no-daemon --console=plain build run
    
Using the gradle daemon or the color output options causes the CLI not to function properly.

Debugging of the process can be bit tricky. Depending on how well your IDE supports ANSI terminals you
may be able to start the process from within the IDE.  Alternatively, you may need to connect to the process
once you've started it from a command shell.


Connections
===========
When the CLI first starts there is no active connection.  If you execute `help` you will only
see commands associated to the shell itself.

The first thing you'll want to do is connect to an endpoint (TestNet uses port 10501, MainNet will use port 10500):

    (no connection) > connect 127.0.0.1:10501
    
This will connect to the RPC protocol at the specified endpoint.  

Executing the `help` command while connected to an endpoint will
display help for the shell commands _and_ the commands applicable to the connected
protocol.


Secure Connections
==================
See [SECURITY.MD](SECURITY.MD) for connecting to a nodecore instance running in secure mode