// VeriBlock NodeCore CLI
// Copyright 2017-2020 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli;

import com.google.common.collect.Sets;
import nodecore.cli.annotations.CommandServiceType;
import nodecore.cli.contracts.AdminService;
import nodecore.cli.contracts.ConnectionFailedException;
import nodecore.cli.contracts.EndpointTransportType;
import nodecore.cli.contracts.ProtocolEndpoint;
import nodecore.cli.contracts.ProtocolEndpointType;
import nodecore.cli.models.ModeType;
import nodecore.cli.services.AdminServiceClient;
import org.jetbrains.annotations.NotNull;
import org.jline.reader.LineReader;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.veriblock.shell.Command;
import org.veriblock.shell.CommandContext;
import org.veriblock.shell.CommandFactory;
import org.veriblock.shell.Shell;
import org.veriblock.shell.core.Result;
import org.veriblock.shell.core.ResultMessage;
import veriblock.SpvContext;
import veriblock.conf.NetworkParameters;
import veriblock.model.DownloadStatusResponse;
import veriblock.net.BootstrapPeerDiscovery;
import veriblock.net.PeerDiscovery;

import javax.net.ssl.SSLException;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class CliShell extends Shell {
    private static final Logger _logger = LoggerFactory.getLogger(CliShell.class);

    private final SpvContext spvContext = new SpvContext();
    private final ProtocolEndpointContainer _endpointContainer = new ProtocolEndpointContainer();
    private final AtomicBoolean connected = new AtomicBoolean(false);

    private AdminServiceClient _adminServiceClient;
    private Configuration _configuration;
    private Runnable disconnectCallBack;

    private ModeType modeType = ModeType.STANDARD;

    public void onStart() {
    }

    public void onStop() {
        disconnect();
    }

    public boolean connect(ProtocolEndpoint endpoint, boolean save) throws SSLException, ConnectionFailedException {
        if (save) {
            disconnect();
        }

        switch (endpoint.type()) {
            case NONE:
                return false;
            case RPC:
                if (save) {
                    _adminServiceClient = new AdminServiceClient(endpoint.address(), endpoint.port(), endpoint.transportType(), _configuration, endpoint.password());
                    try {
                        _adminServiceClient.connect();
                        _endpointContainer.setProtocolEndpoint(endpoint);
                        refreshCompleter();
                        connected.set(true);

                        return true;
                    } catch (Exception x) {
                        disconnect();
                        throw new ConnectionFailedException(String.format("Unable to connect to the endpoint %s", endpoint.toString()), x);
                    }
                } else {
                    AdminServiceClient temp = new AdminServiceClient(endpoint.address(), endpoint.port(), endpoint.transportType(), _configuration, endpoint.password());
                    boolean result = false;
                    try {
                        temp.connect();
                        result = true;
                    } catch (Exception e) {
                        // do nothing
                    } finally {
                        try {
                            temp.shutdown();
                        } catch (InterruptedException e) {
                            // do nothing
                        }
                    }
                    return result;
                }
        }

        return false;
    }

    private void disconnect() {
        try {
            if (_adminServiceClient != null) {
                _adminServiceClient.shutdown();
                _adminServiceClient = null;
                refreshCompleter();

                if(this.disconnectCallBack != null){
                    this.disconnectCallBack.run();
                    this.disconnectCallBack = null;
                }
            }

            _endpointContainer.setProtocolEndpoint(null);
        } catch (Exception e) {
            _logger.error("Unhandled exception", e);
        }
    }

    public CliShell(CommandFactory commandFactory, Configuration configuration) {
        super(commandFactory, null);
        _configuration = configuration;
    }

    @Override
    protected String getPrompt() {
        if (_endpointContainer.getProtocolEndpoint() == null) {
            return new AttributedStringBuilder()
                .style(AttributedStyle.BOLD)
                .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW+8))
                .append("(VBK_CLI ")
                .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED+8))
                .append("[NOT CONNECTED]")
                .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW+8))
                .append(") > ")
                .toAnsi();
        } else {
            return new AttributedStringBuilder()
                .style(AttributedStyle.BOLD)
                .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN+8))
                .append("(VBK_CLI ")
                .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN+8))
                .append(_endpointContainer.getProtocolEndpoint().toString())
                .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN+8))
                .append(") > ")
                .toAnsi();
        }
    }

    @Override
    protected Result handleResult(CommandContext context, Result result) {
        boolean failed = result.isFailed();

        try {
            Runnable disconnectCallBack = context.getExtraData("disconnectCallBack");
            if(disconnectCallBack != null){
                this.disconnectCallBack = disconnectCallBack;
            }

            Boolean disconnect = context.getExtraData("disconnect");
            if (disconnect != null && disconnect) {
                disconnect();
            }

            ProtocolEndpoint endpoint = context.getExtraData("connect");
            if (endpoint != null) {
                if (endpoint.port() == 8500 || endpoint.port() == 8501) {
                    result.addMessage("V023", "Connect Failed", "NodeCore reserves ports 8500 and 8501 for mining pool operations", true);
                    failed = true;
                } else {
                    try {
                        // TODO: Refactor all this. Shell shouldn't be responsible for establishing connection
                        connect(endpoint, true);
                        printStyled("Successfully connected to NodeCore!", AttributedStyle.BOLD.foreground(AttributedStyle.GREEN));
                    } catch (ConnectionFailedException connEx) {
                        String errorMessage =
                            connEx.getMessage() + "\n\n" + "Note: NodeCore does not begin listening for RPC connections until after " +
                                "loading the blockchain, which may take several minutes. " + "By default, NodeCore MainNet listens on port 10500 and " +
                                "NodeCore TestNet listens on port 10501. ";
                        result.addMessage("V023",
                            "Connect Failed",
                            errorMessage + "Please ensure that you have also started up NodeCore!",
                            true);
                        failed = true;
                    }
                }
            }
        } catch (IllegalArgumentException | SSLException argEx) {
            result.addMessage(
                "V024",
                "TLS configuration failed",
                argEx.getMessage(),
                true);
            failed = true;
            _logger.error("V024: TLS configuration failed", argEx);
            disconnect();
        } catch (Exception e) {
            //check if connected
            if (_adminServiceClient == null) {
                //Error from not connected yet
                result.addMessage(
                    "V999",
                    "Not connected to NodeCore yet, please run the connect command!",
                    "Example: connect 127.0.0.1:10501",
                    true);
                failed = true;
            }
            else {
                //Some other unhandled exception
                result.addMessage(
                    "V999",
                    "Unhandled exception",
                    e.toString(),
                    true);
                failed = true;
                _logger.error("V999: Unhandled Exception", e);
            }
        }

        if (failed) {
            Result failure = new Result(true);
            for (ResultMessage rm : result.getMessages()) {
                failure.addMessage(rm);
            }
            return failure;
        }

        return result;
    }

    @Override
    protected Result handleException(Exception exception) {
        Result result = super.handleException(exception);
        if (_adminServiceClient == null) {
            //Error from not connected yet
            result.addMessage(
                "V999",
                "Not connected to NodeCore yet, please run the connect command!",
                "Example: connect 127.0.0.1:10501",
                true
            );
        }
        return result;
    }

    @Override
    protected boolean shouldAutoComplete(@NotNull Command command) {
        String extraData = command.getExtraData();
        if (getModeType() == ModeType.SPV) {
            return getCommandsSpv().contains(command.getForm().split("\\|")[0]);
        }
        return (_endpointContainer != null && _endpointContainer.getProtocolEndpoint() != null) || (
            extraData != null && extraData.equals(CommandServiceType.SHELL.name())
        );
    }

    //TODO Implement it with adding available mods for commands.
    public Set<String> getCommandsSpv() {
        return Sets.newHashSet("getbalance", "getstateinfo", "send", "lockwallet", "unlockwallet", "decryptwallet", "encryptwallet", "importwallet",
            "importprivatekey", "backupwallet", "getnewaddress", "disconnect", "exit"
        );
    }

    public void initialize(ProgramOptions programOptions) {
        ProtocolEndpoint endpoint = null;
        String host = null;

        if (programOptions.getSpvNetworkParameters() != null) {
            printIntroSpv();
        } else {
            printIntroStandard();
        }

        if(programOptions.getSpvNetworkParameters() != null){
            try {
                endpoint = startSPV(programOptions.getSpvNetworkParameters(), new BootstrapPeerDiscovery(programOptions.getSpvNetworkParameters()));
                host = programOptions.getSpvNetworkParameters().getAdminHost() + ":" + programOptions.getSpvNetworkParameters().getAdminPort();

                disconnectCallBack = spvContext::shutdown;
            } catch (Exception e) {
                printResultWithFormat(handleException(e));
            }
        } else if(programOptions.getConnect() != null) {
            host = programOptions.getConnect();
            endpoint = new ProtocolEndpoint(host, ProtocolEndpointType.RPC, null);
        }

        if(endpoint != null && host != null){
            try {
                connect(endpoint, true);

                if(!connected.get()) {
                } else {
                    String msg = new AttributedStringBuilder()
                            .style(AttributedStyle.BOLD)
                            .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                            .append("\n\nConnected to node on ")
                            .append(host)
                            .append("\n\n")
                            .toAnsi();

                    printInfo(msg);
                }
            } catch (Exception e) {
            }
        }

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!connected.get() && ModeType.SPV != getModeType()) {
                    for (int i = 10500 ; i <= 10502; i++) {
                        if (bound(i)) {
                            String msg = new AttributedStringBuilder()
                                    .style(AttributedStyle.BOLD.foreground(AttributedStyle.GREEN))
                                    .append("\n\nA local NodeCore " + (i == 10500 ? ("MainNet") : (i == 10501 ? ("TestNet") : ("AlphaNet"))) + " Instance is present on 127.0.0.1:")
                                    .append(Integer.toString(i))
                                    .append("!\n ")
                                    .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN))
                                    .append("To connect to NodeCore " + (i == 10500 ? ("MainNet") : (i == 10501 ? ("TestNet") : ("AlphaNet"))) +  ", type: ")
                                    .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.WHITE))
                                    .append("connect 127.0.0.1:")
                                    .append(Integer.toString(i))
                                    .append("\n\n")
                                    .toAnsi();

                            printInfo(msg);
                            getReader().callWidget(LineReader.REDRAW_LINE);
                            getReader().callWidget(LineReader.REDISPLAY);

                            return;
                        }
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (Exception ignore) {}
                }
            }

            private boolean bound(int port) {
                try {
                    ProtocolEndpoint endpoint = new ProtocolEndpoint("127.0.0.1:" + port, ProtocolEndpointType.RPC, null);
                    return connect(endpoint, false);
                } catch (Exception ignored) {
                    return false;
                }
            }
        });

        t.start();
    }

    public ProtocolEndpoint startSPV(NetworkParameters net, PeerDiscovery peerDiscovery) throws ExecutionException, InterruptedException {
        this.setModeType(ModeType.SPV);

        spvContext.init(net, peerDiscovery, true);
        spvContext.getPeerTable().start();

        while (true) {
            DownloadStatusResponse status = spvContext.getPeerTable().getDownloadStatus();
            if (status.getDownloadStatus().isDiscovering()) {
                printInfo("Waiting for peers response.");
            } else if (status.getDownloadStatus().isDownloading()) {
                printInfo("Blockchain is downloading. " + status.getCurrentHeight() + " / " + status.getBestHeight());
            } else {
                printInfo("Blockchain is ready. Current height " + status.getCurrentHeight());
                break;
            }

            Thread.sleep(5000L);
        }

        return new ProtocolEndpoint(net.getAdminHost(), (short) net.getAdminPort(), ProtocolEndpointType.RPC, EndpointTransportType.HTTP);
    }

    private void printIntroStandard() {
        printStyled(
            "===[ VeriBlock " + Constants.FULL_APPLICATION_NAME_VERSION + " ]===",
            AttributedStyle.BOLD.foreground(AttributedStyle.GREEN)
        );
        printStyled(
            "\t\thttps://www.veriblock.org/\n",
            AttributedStyle.BOLD.foreground(AttributedStyle.GREEN)
        );
        printStyled(
            "To begin, you must connect this CLI to a running instance of NodeCore.\n" +
                "By default, NodeCore will be available on 127.0.0.1:10500.\n",
            AttributedStyle.BOLD.foreground(AttributedStyle.MAGENTA)
        );
        printStyled(
            "To connect to NodeCore type: ",
            AttributedStyle.BOLD.foreground(AttributedStyle.CYAN),
            false
        );
        printStyled(
            "connect 127.0.0.1:10500",
            AttributedStyle.BOLD.foreground(AttributedStyle.WHITE)
        );
        printStyled(
            "To see available commands, type: ",
            AttributedStyle.BOLD.foreground(AttributedStyle.CYAN),
            false
        );
        printStyled(
            "help\n",
            AttributedStyle.BOLD.foreground(AttributedStyle.WHITE)
        );
        printStyled(
            "Note: If you are using the regular \"all\" package, then the CLI can \nautomatically start many of the other VeriBlock-related services for you!",
            AttributedStyle.BOLD.foreground(AttributedStyle.MAGENTA)
        );
        printStyled(
            "  To start NodeCore, type: ",
            AttributedStyle.BOLD.foreground(AttributedStyle.CYAN),
            false
        );
        printStyled(
                "startnodecore",
            AttributedStyle.BOLD.foreground(AttributedStyle.WHITE)
        );
        printStyled(
            "  To start the Proof-of-Proof (PoP) miner, type: ",
            AttributedStyle.BOLD.foreground(AttributedStyle.CYAN),
            false
        );
        printStyled(
            "startpopminer",
            AttributedStyle.BOLD.foreground(AttributedStyle.WHITE)
        );
        printStyled(
            "    Note: for the PoP miner to work, you must have already started\n    NodeCore," +
                "and connected to it in this CLI (",
            AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW),
            false
        );
        printStyled(
            "connect 127.0.0.1:10500",
            AttributedStyle.BOLD.foreground(AttributedStyle.WHITE),
            false
        );
        printStyled(
            ")!",
            AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW)
        );
        printStyled(
            "  To start the Proof-of-Work (PoW) CPU miner, type: ",
            AttributedStyle.BOLD.foreground(AttributedStyle.CYAN),
            false
        );
        printStyled(
            "startcpuminer",
            AttributedStyle.BOLD.foreground(AttributedStyle.WHITE)
        );
        printStyled(
            "    Note: for the PoW miner to work, you must have already started\n    NodeCore," +
                " connected to it in this CLI (",
            AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW),
            false
        );
        printStyled(
            "connect 127.0.0.1:10500",
            AttributedStyle.BOLD.foreground(AttributedStyle.WHITE),
            false
        );
        printStyled(
            "),\n    and have run either \"",
            AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW),
            false
        );
        printStyled(
            "startpool",
            AttributedStyle.BOLD.foreground(AttributedStyle.WHITE),
            false
        );
        printStyled(
            "\" or \"",
            AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW),
            false
        );
        printStyled(
            "startsolopool",
            AttributedStyle.BOLD.foreground(AttributedStyle.WHITE),
            false
        );
        printStyled(
            "\"!\n",
            AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW),
            true
        );

        introFooter();
    }


    private void printIntroSpv(){
        printStyled(
            "===[ VeriBlock NodeCore SPV " + " v"+ Constants.APPLICATION_VERSION + " ]===",
            AttributedStyle.BOLD.foreground(AttributedStyle.GREEN)
        );
        printStyled(
            "\t\thttps://www.veriblock.org/\n",
            AttributedStyle.BOLD.foreground(AttributedStyle.GREEN)
        );
        printStyled(
            "To see available commands, type: ",
            AttributedStyle.BOLD.foreground(AttributedStyle.CYAN),
            false
        );
        printStyled(
            "help\n",
            AttributedStyle.BOLD.foreground(AttributedStyle.WHITE)
        );

        introFooter();
    }

    private void introFooter(){
        printStyled(
            "To stay up to date on the status of the VeriBlock Network and \n" +
                "ensure you are running the latest software, frequently check:",
            AttributedStyle.BOLD.foreground(AttributedStyle.CYAN)
        );
        printStyled(
            "\tDiscord: ",
            AttributedStyle.BOLD.foreground(AttributedStyle.GREEN),
            false
        );
        printStyled(
            "https://discord.gg/wJZEjry",
            AttributedStyle.BOLD.foreground(AttributedStyle.WHITE),
            true
        );
        printStyled(
            "\tVeriBlock Explorer: ",
            AttributedStyle.BOLD.foreground(AttributedStyle.GREEN),
            false
        );
        printStyled(
                "https://explore.veriblock.org\n\n",
                AttributedStyle.BOLD.foreground(AttributedStyle.WHITE)
        );
    }

    public ProtocolEndpointType getProtocolType() {
        if (_endpointContainer.getProtocolEndpoint() == null)
            return ProtocolEndpointType.NONE;
        return _endpointContainer.getProtocolEndpoint().type();
    }

    public AdminService getAdminService() {
        return _adminServiceClient;
    }

    public boolean isConnected() {
        return _adminServiceClient != null;
    }

    public SpvContext getSpvContext() {
        return spvContext;
    }

    public ModeType getModeType() {
        return modeType;
    }

    public void setModeType(ModeType modeType) {
        this.modeType = modeType;
    }
}
