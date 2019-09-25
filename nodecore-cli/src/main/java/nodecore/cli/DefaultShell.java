// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli;

import com.google.common.base.Stopwatch;
import com.google.inject.Inject;
import nodecore.cli.annotations.CommandServiceType;
import nodecore.cli.commands.DefaultCommandContext;
import nodecore.cli.commands.rpc.GetBalanceCommand;
import nodecore.cli.commands.rpc.GetInfoCommand;
import nodecore.cli.commands.rpc.GetNewAddressCommand;
import nodecore.cli.commands.rpc.StartSoloPoolCommand;
import nodecore.cli.commands.shell.StartPoPMinerCommand;
import nodecore.cli.contracts.*;
import nodecore.cli.services.AdminServiceClient;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.InfoCmp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultShell implements Shell {
    private static final Logger _logger = LoggerFactory.getLogger(DefaultShell.class);

    private final ProtocolEndpointContainer _endpointContainer = new ProtocolEndpointContainer();
    private final AtomicBoolean connected = new AtomicBoolean(false);

    private AdminServiceClient _adminServiceClient;
    private CommandFactory _commandFactory;
    private Configuration _configuration;
    private boolean _running;
    private Terminal terminal;
    private LineReader reader;
    private CommandFactory _factory;

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
                        ((LineReaderImpl)reader).setCompleter(getCompleter());
                        reader.setOpt(LineReader.Option.DISABLE_EVENT_EXPANSION);
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
                ((LineReaderImpl)reader).setCompleter(getCompleter());
            }

            _endpointContainer.setProtocolEndpoint(null);
        } catch (Exception e) {
            _logger.error("Unhandled exception", e);
        }
    }

    private void clearScreen() {
        terminal.puts(InfoCmp.Capability.clear_screen);
        terminal.flush();
    }

    private void stopRunning() {
        _running = false;
    }

    private void startRunning() {
        _running = true;
    }

    @Inject
    public DefaultShell(
            Configuration configuration,
            CommandFactory commandFactory,
            CommandFactory factory) throws IOException {
        _commandFactory = commandFactory;
        _configuration = configuration;

        terminal = TerminalBuilder.builder()
                .system(true)
                .build();

        _factory = factory;

        reader = LineReaderBuilder.builder()
                .terminal(terminal).completer(getCompleter())
                .build();
    }

    @Override
    public Result run() {
        Result result = new DefaultResult();

        startRunning();
        Runtime.getRuntime().addShutdownHook(new Thread(this::stopRunning));

        while (_running) {
            int result_code = 200;

            String line = reader.readLine(formatPromptAsString());
            System.out.println(line);

            if (line.isEmpty())
                continue;

            Boolean clear = null;
            Result executeResult = null;
            Stopwatch stopwatch = Stopwatch.createStarted();
            if (line.equals("close")) {
                line = "quit";
            } else if (line.equals("exit")) {
                line = "quit";
            }

            CommandFactoryResult factoryResult = _commandFactory.getInstance(line);
            if (!factoryResult.didFail()) {
                try {
                    CommandContext context = new DefaultCommandContext(
                            this,
                            _adminServiceClient,
                            factoryResult.getParameters());
                    executeResult = factoryResult.getInstance().execute(context);
                    if (executeResult.didFail()) {
                        result_code = 500;
                    } else {
                        Boolean quit = context.getData("quit");
                        if (quit != null && quit)
                            stopRunning();

                        Boolean disconnect = context.getData("disconnect");
                        if (disconnect != null && disconnect) {
                            disconnect();
                        }

                        ProtocolEndpoint endpoint = context.getData("connect");
                        if (endpoint != null) {
                            if (endpoint.port() == 8500 || endpoint.port() == 8501) {
                                executeResult.addMessage("V023",
                                        "Connect Failed",
                                        "NodeCore reserves ports 8500 and 8501 for mining pool operations",
                                        true);
                                result_code = 500;
                            } else {
                                try {
                                    // TODO: Refactor all this. Shell shouldn't be responsible for establishing connection
                                    connect(endpoint, true);
                                    format(AttributedStyle.BOLD,
                                            AttributedStyle.GREEN,
                                            "Successfully connected to NodeCore!\n");

                                    List<Class<? extends Command>> suggestedCommands = new ArrayList<>(Arrays.asList(
                                            GetInfoCommand.class,
                                            GetNewAddressCommand.class,
                                            GetBalanceCommand.class,
                                            StartSoloPoolCommand.class
                                    ));
                                    if (!GraphicsEnvironment.isHeadless()) {
                                        suggestedCommands.add(StartPoPMinerCommand.class);
                                    }

                                    context.suggestCommands(suggestedCommands);
                                } catch (ConnectionFailedException connEx) {
                                    String errorMessage = connEx.getMessage() +
                                            "\n\n" +
                                            "Note: NodeCore does not begin listening for RPC connections until after " +
                                            "loading the blockchain, which may take several minutes. " +
                                            "By default, NodeCore MainNet listens on port 10500 and " +
                                            "NodeCore TestNet listens on port 10501. ";
                                    executeResult.addMessage("V023",
                                            "Connect Failed",
                                            errorMessage + "Please ensure that you have also started up NodeCore!",
                                            true);
                                    result_code = 500;
                                }
                            }
                        }

                        clear = context.getData("clear");
                    }
                } catch (IllegalArgumentException | SSLException argEx) {
                    factoryResult.addMessage(
                            "V024",
                            "TLS configuration failed",
                            argEx.getMessage(),
                            true);
                    result_code = 500;
                    _logger.error("V024: TLS configuration failed", argEx);
                    disconnect();
                } catch (Exception e) {
                    //check if connected
                    if (_adminServiceClient == null)
                    {
                        //Error from not connected yet
                        factoryResult.addMessage(
                                "V999",
                                "Not connected to NodeCore yet, please run the connect command!",
                                "Example: connect 127.0.0.1:10501",
                                true);
                        result_code = 500;
                    }
                    else
                    {
                        //Some other unhandled exception
                        factoryResult.addMessage(
                                "V999",
                                "Unhandled exception",
                                e.toString(),
                                true);
                        result_code = 500;
                        _logger.error("V999: Unhandled Exception", e);
                    }
                }
            }
            else {
                result_code = 500;
            }

            stopwatch.stop();

            formatResult(factoryResult);
            if(executeResult != null)
                formatResult(executeResult);

            switch (result_code) {
                case 200 : {
                    format(AttributedStyle.DEFAULT, AttributedStyle.GREEN, "200 success ");
                    break;
                }
                case 500 : {
                    format(AttributedStyle.DEFAULT, AttributedStyle.RED,"500 failure ");
                    break;
                }
            }

            format(AttributedStyle.DEFAULT, AttributedStyle.YELLOW, String.format("(%s)\n\n", stopwatch.toString()));

            // N.B. clear is a special case because we want it to completely clear the terminal
            if (clear != null && clear) {
                clearScreen();
            }
        }

        disconnect();

        return result;
    }

    private String formatPromptAsString() {

        String prompt;

        if (_endpointContainer.getProtocolEndpoint() == null) {

            prompt = new AttributedStringBuilder()
                    .style(AttributedStyle.BOLD)
                    .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW+8))
                    .append("(VBK_CLI ")
                    .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.RED+8))
                    .append("[NOT CONNECTED]")
                    .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW+8))
                    .append(") > ")
                    .toAnsi();
        } else {

            prompt = new AttributedStringBuilder()
                    .style(AttributedStyle.BOLD)
                    .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN+8))
                    .append("(VBK_CLI ")
                    .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN+8))
                    .append(_endpointContainer.getProtocolEndpoint().toString())
                    .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN+8))
                    .append(") > ")
                    .toAnsi();
        }

        return prompt;
    }

    private Completer getCompleter() {

        List<String> commands = new ArrayList<>();

        for (String key : _factory.getDefinitions().keySet()) {
            CommandDefinition def = _factory.getDefinitions().get(key);
            if(_endpointContainer.getProtocolEndpoint() != null ||
                    (_endpointContainer.getProtocolEndpoint() == null && def.getSpec().service() == CommandServiceType.SHELL)) {
                commands.add(def.getSpec().form());
            }
        }

        Collections.sort(commands);

        return new StringsCompleter(commands);
    }

    @Override
    public void initialize(String host) {

        printIntro();

        if(host != null) {
            ProtocolEndpoint endpoint = new ProtocolEndpoint(host, ProtocolEndpointType.RPC, null);
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

                    terminal.writer().println(msg);
                    terminal.flush();
                    reader.callWidget(LineReader.REDRAW_LINE);
                    reader.callWidget(LineReader.REDISPLAY);
                }
            } catch (Exception e) {
            }
        }

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!connected.get()) {
                    for (int i = 10500 ; i <= 10502; i++) {
                        if (bound(i)) {
                            String msg = new AttributedStringBuilder()
                                    .style(AttributedStyle.BOLD)
                                    .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
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

                            terminal.writer().println(msg);
                            terminal.flush();
                            reader.callWidget(LineReader.REDRAW_LINE);
                            reader.callWidget(LineReader.REDISPLAY);

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

    private void printIntro() {

        format(AttributedStyle.BOLD, AttributedStyle.GREEN,
                "===[ VeriBlock " + Constants.FULL_APPLICATION_NAME_VERSION + " ]===\n");

        format(AttributedStyle.BOLD, AttributedStyle.GREEN,
                "\t\thttps://www.veriblock.org/\n\n");

        format(AttributedStyle.BOLD, AttributedStyle.MAGENTA,
                "To begin, you must connect this CLI to a running instance of NodeCore.\n" +
                        "By default, NodeCore will be available on 127.0.0.1:10500.\n\n");

        format(AttributedStyle.BOLD, AttributedStyle.CYAN,
                "To connect to NodeCore type: ");

        format(AttributedStyle.BOLD, AttributedStyle.WHITE,
                "connect 127.0.0.1:10500\n");

        format(AttributedStyle.BOLD, AttributedStyle.CYAN,
                "To see available commands, type: ");

        format(AttributedStyle.BOLD, AttributedStyle.WHITE,
                "help\n\n");

        format(AttributedStyle.BOLD, AttributedStyle.MAGENTA,
                "Note: If you are using the regular \"all\" package, then the CLI can \nautomatically start many of the other VeriBlock-related services for you!\n");

        format(AttributedStyle.BOLD, AttributedStyle.CYAN,
                "  To start NodeCore, type: ");

        format(AttributedStyle.BOLD, AttributedStyle.WHITE,
                "startnodecore\n");

        format(AttributedStyle.BOLD, AttributedStyle.CYAN,
                "  To start the Proof-of-Proof (PoP) miner, type: ");

        format(AttributedStyle.BOLD, AttributedStyle.WHITE,
                "startpopminer\n");

        format(AttributedStyle.BOLD, AttributedStyle.YELLOW,
                "    Note: for the PoP miner to work, you must have already started\n    NodeCore," +
                        "and connected to it in this CLI (");

        format(AttributedStyle.BOLD, AttributedStyle.WHITE,
                "connect 127.0.0.1:10500");

        format(AttributedStyle.BOLD, AttributedStyle.YELLOW,
                ")!\n");

        format(AttributedStyle.BOLD, AttributedStyle.CYAN,
                "  To start the Proof-of-Work (PoW) CPU miner, type: ");

        format(AttributedStyle.BOLD, AttributedStyle.WHITE,
                "startcpuminer\n");

        format(AttributedStyle.BOLD, AttributedStyle.YELLOW,
                "    Note: for the PoW miner to work, you must have already started\n    NodeCore," +
                        " connected to it in this CLI (");

        format(AttributedStyle.BOLD, AttributedStyle.WHITE,
                "connect 127.0.0.1:10500");

        format(AttributedStyle.BOLD, AttributedStyle.YELLOW,
                "),\n    and have run either \"");

        format(AttributedStyle.BOLD, AttributedStyle.WHITE,
                "startpool");

        format(AttributedStyle.BOLD, AttributedStyle.YELLOW,
                "\" or \"");

        format(AttributedStyle.BOLD, AttributedStyle.WHITE,
                "startsolopool");

        format(AttributedStyle.BOLD, AttributedStyle.YELLOW,
                "\"!\n\n");

        format(AttributedStyle.BOLD, AttributedStyle.CYAN,
                "To stay up to date on the status of the VeriBlock Network and \n" +
                        "ensure you are running the latest software, frequently check:\n");

        format(AttributedStyle.BOLD, AttributedStyle.GREEN,
                "\tDiscord: ");

        format(AttributedStyle.BOLD, AttributedStyle.WHITE,
                "https://discord.gg/wJZEjry\n");

        format(AttributedStyle.BOLD, AttributedStyle.GREEN,
                "\tVeriBlock Explorer: ");

        format(AttributedStyle.BOLD, AttributedStyle.WHITE,
                "https://explore.veriblock.org\n\n");

        terminal.flush();
    }

    public void format(AttributedStyle style, int fColor, String str) {

        // workaround for high intensity color
        if(style == AttributedStyle.BOLD) {
            fColor += 8;
        }

        String msg = new AttributedStringBuilder()
                .style(style)
                .style(AttributedStyle.DEFAULT.foreground(fColor))
                .append(str)
                .toAnsi();
        terminal.writer().print(msg);
    }

    private void formatResult(Result result) {
        for (ResultMessage message : result.getMessages()) {
            String level = message.isError() ? "ERROR: " : "INFO:  ";
            int color = message.isError() ? AttributedStyle.RED : AttributedStyle.YELLOW;
            String msg = String.format("%s[%s] %s\n", level, message.getCode(), message.getMessage());
            format(AttributedStyle.BOLD, color, msg);
            String detail = message.getDetails();
            if (detail != null && detail.length() > 0) {
                format(AttributedStyle.BOLD, color, String.format("       %s\n", detail));
            }
        }

        terminal.flush();
    }

    @Override
    public ProtocolEndpointType type() {
        if (_endpointContainer.getProtocolEndpoint() == null)
            return ProtocolEndpointType.NONE;
        return _endpointContainer.getProtocolEndpoint().type();
    }

    @Override
    public void format(String fmt, Object... args) {
        terminal.writer().print(String.format(fmt, args));
        terminal.flush();
    }

    @Override
    public String passwordPrompt(String prompt) {
        Character mask = '*';
        return reader.readLine(prompt, mask);
    }
}
