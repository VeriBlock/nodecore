// VeriBlock PoP Miner
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.miners.pop.shell;


import com.google.inject.Inject;
import nodecore.miners.pop.common.Utility;
import nodecore.miners.pop.shell.annotations.CommandSpec;
import nodecore.miners.pop.shell.annotations.CommandSpecParameter;
import nodecore.miners.pop.contracts.*;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;

public class DefaultCommandFactory implements CommandFactory {
    private static final Logger logger = LoggerFactory.getLogger(DefaultCommandFactory.class);
    static Map<String, CommandDefinition> _definitions = new HashMap<>();
    private Map<String, Command> _commands;
    private Configuration _configuration;

    @SuppressWarnings("unchecked")
    static void buildFactoryCache() {
        Reflections reflections = new Reflections("nodecore.miners.pop.shell.commands");
        Set<Class<? extends Command>> types = reflections.getSubTypesOf(Command.class);

        for (Class type : types) {
            CommandSpec spec = (CommandSpec) type.getAnnotation(CommandSpec.class);
            if (spec != null) {
                CommandSpecParameter[] parameters = (CommandSpecParameter[]) type.getAnnotationsByType(
                        CommandSpecParameter.class);
                String[] aliases = spec.form().split("\\|");
                for (String alias : aliases) {
                    _definitions.put(
                            alias,
                            new CommandDefinition(type, spec, parameters));
                }
            }
        }
    }

    @Inject
    public DefaultCommandFactory(
            Configuration configuration,
            Map<String, Command> commands) {
        _commands = commands;
        _configuration = configuration;
    }

    @Override
    public Map<String, CommandDefinition> getDefinitions() {
        return _definitions;
    }

    @Override
    public CommandFactoryResult getInstance(String request) {
        CommandFactoryResult result = new CommandFactoryResult();

        if (request == null || request.length() == 0) {
            addMalformedCommandError(result);
            return result;
        }

        String[] parts = request.split(" ");
        if (parts.length == 0) {
            addMalformedCommandError(result);
            return result;
        }

        StringBuilder loggedCommand = new StringBuilder();

        String form = parts[0];
        loggedCommand.append(form);
        if (!_commands.containsKey(form) || !_definitions.containsKey(form)) {
            addUnknownCommandError(result, form);
            return result;
        }

        CommandDefinition definition = _definitions.get(form);
        result.setInstance(_commands.get(form));

        int index = 1;
        CommandSpecParameter[] params = definition.getParams();

        if (parts.length - 1 > params.length) {
            addSyntaxError(result, definition, "too many parameters provided");
            return result;
        }

        for (CommandSpecParameter param : params) {
            if (param.required() && parts.length < index + 1) {
                addSyntaxError(
                        result,
                        definition,
                        String.format("parameter '%s' is required", param.name()));
                break;
            }

            if (index < parts.length) {
                String value = parts[index];
                switch (param.type()) {
                    case STRING:
                        loggedCommand.append(" ").append(value);
                        result.getParameters().put(param.name(), value);
                        break;
                    case LONG:
                        if (!Utility.isPositiveLong(value)) {
                            addSyntaxError(
                                    result,
                                    definition,
                                    String.format("parameter '%s' must be a positive 64-bit integer", param.name()));
                        } else {
                            loggedCommand.append(" ").append(value);
                            result.getParameters().put(param.name(), Long.parseLong(value));
                        }
                        break;
                    case INTEGER:
                        if (!Utility.isPositiveInteger(value)) {
                            addSyntaxError(
                                    result,
                                    definition,
                                    String.format("parameter '%s' must be a positive 32-bit integer", param.name()));
                        } else {
                            loggedCommand.append(" ").append(value);
                            result.getParameters().put(param.name(), Integer.parseInt(value));
                        }
                        break;
                    case AMOUNT:
                        try {
                            BigDecimal amount = new BigDecimal(value);
                            loggedCommand.append(" ").append(value);
                            result.getParameters().putIfAbsent(param.name(), amount);
                        } catch (NumberFormatException e) {
                            addSyntaxError(result, definition, String.format("parameter '%s' must be an amount in BTC to send. e.g. 0.1", param.name()));
                        }
                        break;
                    case PASSWORD:
                        loggedCommand.append(" *****");
                        result.getParameters().put(param.name(), value);
                        break;
                }
            }

            ++index;
        }

        logger.info("USER COMMAND: {}", loggedCommand.toString());

        return result;
    }

    private void addMalformedCommandError(Result result) {
        result.addMessage(
                "V005",
                "Malformed command string",
                "The passed command string cannot be parsed.",
                true);
        result.fail();
    }

    private void addUnknownCommandError(Result result, String form) {
        result.addMessage(
                "V004",
                "Unknown protocol command",
                String.format("The command '%s' is not supported", form),
                true);
        result.fail();
    }

    private void addSyntaxError(Result result, CommandDefinition definition, String message) {
        StringBuilder builder = new StringBuilder();
        builder.append(definition.getSpec().form());
        formatParameters(builder, definition.getParams());

        builder.append(String.format("ERROR: %s", message));
        result.addMessage(
                "V009",
                "Syntax error",
                String.format("Usage: %s", builder.toString()),
                true);
        result.fail();
    }

    private void formatParameters(StringBuilder builder, CommandSpecParameter[] params) {
        if (params.length > 0) {
            builder.append(' ');
            for (CommandSpecParameter param : params) {
                if (param.required()) {
                    builder.append(String.format("<%s> ", param.name()));
                } else {
                    builder.append(String.format("[%s] ", param.name()));
                }
            }
        }
    }
}
