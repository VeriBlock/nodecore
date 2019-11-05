// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.shell;

import com.diogonunes.jcdp.color.ColoredPrinter;
import com.diogonunes.jcdp.color.api.Ansi;
import com.google.common.base.Strings;
import com.google.common.eventbus.Subscribe;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import nodecore.miners.pop.Constants;
import nodecore.miners.pop.InternalEventBus;
import nodecore.miners.pop.contracts.*;
import nodecore.miners.pop.events.PoPMinerReadyEvent;
import nodecore.miners.pop.events.PoPMiningOperationStateChangedEvent;
import nodecore.miners.pop.events.ShellCompletedEvent;
import nodecore.miners.pop.events.WalletSeedAgreementMissingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class DefaultShell implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(DefaultShell.class);

    private final Configuration configuration;
    private final CommandFactory commandFactory;
    private final MessageService messageService;
    private final ColoredPrinter printer;
    private final Scanner scanner;
    private final Object lock = new Object();
    private final SimpleDateFormat dateFormatter;
    private final PoPMiner popMiner;

    private CompletableFuture<Void> messageHandler;
    private boolean running = false;
    private boolean awaitingInput = false;
    private boolean mustAcceptWalletSeed = false;

    @Inject
    public DefaultShell(Configuration configuration,
                        PoPMiner poPMiner,
                        CommandFactory commandFactory,
                        MessageService messageService) {
        this.configuration = configuration;
        this.popMiner = poPMiner;
        this.commandFactory = commandFactory;
        this.messageService = messageService;
        this.scanner = new Scanner(System.in);
        this.printer = new ColoredPrinter.Builder(1, false)
                .foreground(Ansi.FColor.WHITE)
                .background(Ansi.BColor.NONE)
                .build();

        this.dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        InternalEventBus.getInstance().register(this);
    }

    private void clear() {
        synchronized (lock) {
            printer.clear();
            printer.print("\033[2J");
            printer.print("\033[H");
        }
    }

    private void prompt() {
        synchronized (lock) {
            printer.print(" > ", Ansi.Attribute.BOLD, Ansi.FColor.GREEN, Ansi.BColor.NONE);
            printer.clear();
        }
    }

    private void printLines(List<StatusOutputMessage> messages) {
        if (messages.size() == 0) return;

        synchronized (lock) {
            if (awaitingInput) {
                printer.print("\r");
            }

            for (StatusOutputMessage msg : messages) {
                printer.clear();
                printer.print(Strings.padStart(msg.getLevel().toString(), 7, ' '),
                        Ansi.Attribute.BOLD,
                        msg.getColor(),
                        Ansi.BColor.NONE);
                printer.clear();
                printer.print(": (" + dateFormatter.format(new Date()) + ") " + msg.getMessage() + "\n");

                for (String detail : msg.getDetails()) {
                    printer.println("         " + detail);
                }
            }
        }

        if (awaitingInput) {
            prompt();
        }
    }

    private void formatResult(Result result) {
        List<StatusOutputMessage> formatted = new ArrayList<>();
        for (ResultMessage msg : result.getMessages()) {
            if (msg.isError()) {
                logger.warn(String.format("[%s] %s", msg.getCode(), msg.getMessage()));
            } else {
                logger.info(String.format("[%s] %s", msg.getCode(), msg.getMessage()));
            }
            formatted.add(new StatusOutputMessage(msg.isError() ? MessageEvent.Level.ERROR : MessageEvent.Level.INFO,
                    String.format("[%s] %s", msg.getCode(), msg.getMessage()),
                    msg.getDetails() != null ? msg.getDetails().toArray(new String[msg.getDetails().size()]) : null));
        }

        printLines(formatted);
    }

    private void formatMessages(List<MessageEvent> messages) {
        List<StatusOutputMessage> formatted = new ArrayList<>();
        for (MessageEvent msg : messages) {
            if (MessageEvent.Level.ERROR.equals(msg.getLevel())) {
                logger.error(msg.getMessage());
            } else if (MessageEvent.Level.WARN.equals(msg.getLevel())) {
                logger.warn(msg.getMessage());
            } else {
                logger.info(msg.getMessage());
            }
            formatted.add(new StatusOutputMessage(msg.getLevel(), msg.getMessage()));
        }

        printLines(formatted);
    }

    private String readLine() {
        prompt();

        awaitingInput = true;
        String input = scanner.nextLine();
        awaitingInput = false;

        return input;
    }

    private void startRunning() {
        watchMessages();
        running = true;
    }

    private void stopRunning() {
        if (messageHandler != null)
            messageHandler.complete(null);

        running = false;
    }

    private void watchMessages() {
        messageHandler = CompletableFuture.supplyAsync(messageService::getMessages)
                .thenAccept(this::formatMessages)
                .thenRun(this::watchMessages);
    }

    private void runOnce() {
        if (mustAcceptWalletSeed) {
            List<String> walletSeed = popMiner.getWalletSeed();
            if (walletSeed != null) {
                print(Collections.singletonList("This application contains a Bitcoin wallet. The seed words which can be used to recover this wallet will be displayed below. Press 'y' to continue..."));
                int counter = 0;
                while (!readLine().toUpperCase().equals("Y")) {
                    counter++;
                    if (counter >= 3) {
                        System.exit(1);
                    }
                    print(Collections.singletonList("This application contains a Bitcoin wallet. The seed words which can be used to recover this wallet will be displayed below. Press 'y' to continue..."));
                }

                print(Collections.singletonList("WALLET CREATION TIME:"));
                print(Collections.singletonList(String.format("\t%s", walletSeed.get(0))));
                print(Collections.singletonList("SEED WORDS:"));
                print(walletSeed.subList(1, walletSeed.size()).stream().map(s -> String.format("\t%s", s)).collect(Collectors.toList()));

                print(Collections.singletonList("\rThis information will not be displayed again. Please make sure you have recorded them securely. Press 'y' to continue..."));
                counter = 0;
                while (!readLine().toUpperCase().equals("Y")) {
                    counter++;
                    if (counter >= 3) {
                        System.exit(1);
                    }
                    print(Collections.singletonList("This information will not be displayed again. Please make sure you have recorded them securely. Press 'y' to continue..."));
                }
            }
        }
    }

    public void initialize() {
        clear();
        printer.print("===[ " + Constants.FULL_APPLICATION_NAME_VERSION + " ]===\n");
        printer.print("https://www.veriblock.org/\n\n");

        Object objDiagnostics = org.veriblock.core.utilities.DiagnosticUtility.getDiagnosticInfo();
        String strDiagnostics = (new GsonBuilder().setPrettyPrinting().create().toJson(objDiagnostics));
        logger.info(strDiagnostics);

        format(Ansi.Attribute.BOLD, Ansi.FColor.YELLOW, Ansi.BColor.NONE,
                "WARNING: This miner maintains a lightweight BTC wallet for the purpose of creating PoP transactions only. " +
                        "Please deposit minimal amounts of BTC sufficient for mining.\n\n");
    }

    private static final long MINING_DELAY_MS = 6000;
    public void run() {
        runOnce();

        startRunning();

        while (running) {
            String input = readLine();
            if (input.isEmpty())
                continue;

            Boolean clear = null;
            Result executeResult = null;
            CommandFactoryResult factoryResult = commandFactory.getInstance(input);
            if (!factoryResult.didFail()) {
                try {
                    CommandContext context = new DefaultCommandContext(this, factoryResult.getParameters());
                    executeResult = factoryResult.getInstance().execute(context);

                    if (!executeResult.didFail()) {
                        Boolean quit = context.getData("quit");
                        if (quit != null && quit)
                            stopRunning();

                        clear = context.getData("clear");
                    }
                } catch (Exception e) {
                    factoryResult.addMessage(
                            "V999",
                            "Unhandled exception",
                            e.toString(),
                            true);

                    logger.error("V999: Unhandled Exception", e);
                }
            }

            formatResult(factoryResult);
            if (executeResult != null)
                formatResult(executeResult);


            if (clear != null && clear) {
                clear();
            }
        }

        InternalEventBus.getInstance().post(new ShellCompletedEvent());
    }

    public void print(List<String> text) {
        synchronized (lock) {
            if (awaitingInput) {
                printer.print("\r");
            }

            printer.clear();

            for (String s : text) {
                printer.println(s);
            }
        }

        if (awaitingInput) {
            prompt();
        }
    }

    public void renderFromThrowable(Throwable t) {
        format(Ansi.Attribute.BOLD, Ansi.FColor.YELLOW, Ansi.BColor.NONE, "%s\n\n", t.getMessage());
    }

    @Subscribe public void onPoPMinerReady(PoPMinerReadyEvent event) {
        try {
            List<String> output = new ArrayList<>();
            output.add("**********************************************************************************************");
            output.add("* Ready to start mining. Type 'help' to see available commands. Type 'mine' to start mining. *");
            output.add("**********************************************************************************************");

            print(output);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Subscribe public void onPoPMiningOperationStateChanged(PoPMiningOperationStateChangedEvent event) {
        try {
            String operationId = event.getState().getOperationId();
            List<StatusOutputMessage> messages = new ArrayList<>();

            for (String s : event.getMessages()) {
                StatusOutputMessage statusOutputMessage = new StatusOutputMessage(MessageEvent.Level.MINER, String.format("[%s] %s", operationId, s));
                logger.info(statusOutputMessage.getMessage());

                messages.add(statusOutputMessage);
            }

            printLines(messages);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Subscribe public void onWalletSeedAgreementMissing(WalletSeedAgreementMissingEvent event) {
        this.mustAcceptWalletSeed = true;
    }

    public void format(
            Ansi.Attribute attribute,
            Ansi.FColor foregroundColor,
            Ansi.BColor backgroundColor,
            String fmt,
            Object... args) {
        printer.print(String.format(fmt, args), attribute, foregroundColor, backgroundColor);
        printer.clear();
    }
}
