// VeriBlock NodeCore CLI
// Copyright 2017-2019 Xenios SEZC
// All rights reserved.
// https://www.veriblock.org
// Distributed under the MIT software license, see the accompanying
// file LICENSE or http://www.opensource.org/licenses/mit-license.php.

package nodecore.cli.commands;

import nodecore.cli.Configuration;
import nodecore.cli.annotations.CommandSpec;
import nodecore.cli.annotations.CommandSpecParameter;
import nodecore.cli.contracts.Command;
import nodecore.cli.contracts.CommandDefinition;
import nodecore.cli.contracts.CommandFactory;
import nodecore.cli.contracts.CommandFactoryResult;
import nodecore.cli.contracts.Output;
import nodecore.cli.contracts.PeerEndpoint;
import org.reflections.Reflections;
import org.veriblock.core.utilities.AddressUtility;
import org.veriblock.core.utilities.Utility;
import org.veriblock.shell.core.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultCommandFactory implements CommandFactory {

    public static String FILENAME_SELECTOR =    "-o";
    public static String FORMAT_SELECTOR =      "-f";

    static Map<String, CommandDefinition> _definitions = new HashMap<>();
    private Map<String, Command> _commands;
    private Configuration _configuration;

    @SuppressWarnings("unchecked")
    static void buildFactoryCache() {
        Reflections reflections = new Reflections("nodecore.cli.commands");
        Set<Class<? extends Command>> types = reflections.getSubTypesOf(Command.class);

        for (Class type : types) {
            CommandSpec spec = (CommandSpec) type.getAnnotation(CommandSpec.class);
            if (spec != null) {
                CommandSpecParameter[] parameters = (CommandSpecParameter[]) type.getAnnotationsByType(CommandSpecParameter.class);
                _definitions.put(
                        spec.form(),
                        new CommandDefinition(type, spec, parameters));
            }
        }
    }

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

        String form = parts[0];
        form = form.toLowerCase();
        if (Aliases.containsAlias(form)) {
            form = Aliases.getCommand(form);
        }

        if (!_commands.containsKey(form) || !_definitions.containsKey(form)) {
            addUnknownCommandError(result, form);
            return result;
        }

        if(request.contains(FILENAME_SELECTOR)) {
            parts = handleSelectors(parts, result);
            if(result.didFail()) {
                return result;
            }
        }

        CommandDefinition definition = _definitions.get(form);
        result.setInstance(_commands.get(form));

        CommandSpecParameter[] params = definition.getParams();

        int requestParamsNum = parts.length - 1;

        if (requestParamsNum > params.length) {
            addSyntaxError(result, definition, "too many parameters provided");
            return result;
        }

        int index = 1;
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
                    case HASH:
                        if (!Utility.isHex(value)) {
                            addSyntaxError(
                                    result,
                                    definition,
                                    String.format("parameter '%s' must be a hexadecimal string", param.name()));
                        } else {
                            result.getParameters().put(param.name(), value);
                        }
                        break;
                    case PEER:
                        try {
                            result.getParameters().put(param.name(), new PeerEndpoint(value));
                        } catch (Exception e) {
                            addSyntaxError(
                                    result,
                                    definition,
                                    String.format("parameter '%s' must be a in the form: host:port", param.name()));
                        }
                        break;
                    case STRING:
                        result.getParameters().put(param.name(), value);
                        break;
                    case HEXSTRING:
                        if (!Utility.isHex(value)) {
                            addSyntaxError(
                                    result,
                                    definition,
                                    String.format("parameter '%s' must be a valid hexadecimal string!", param.name()));
                        } else {
                            result.getParameters().put(param.name(), value);
                        }
                        break;
                    case STANDARD_ADDRESS:
                        if (!AddressUtility.isValidStandardAddress(value)) {
                            addSyntaxError(
                                    result,
                                    definition,
                                    String.format("parameter '%s' must be a valid standard address", param.name()));
                        } else {
                            result.getParameters().put(param.name(), value);
                        }
                        break;
                    case MULTISIG_ADDRESS:
                        if (!AddressUtility.isValidMultisigAddress(value)) {
                            addSyntaxError(
                                    result,
                                    definition,
                                    String.format("parameter '%s' must be a valid multisig address", param.name()));
                        } else {
                            result.getParameters().put(param.name(), value);
                        }
                        break;
                    case STANDARD_OR_MULTISIG_ADDRESS:
                        if (!AddressUtility.isValidStandardOrMultisigAddress(value)) {
                            addSyntaxError(
                                    result,
                                    definition,
                                    String.format("parameter '%s' must be a valid standard or multisig address", param.name()));
                        } else {
                            result.getParameters().put(param.name(), value);
                        }
                        break;
                    case COMMA_SEPARATED_STANDARD_ADDRESSES:
                        String[] addresses = value.split(",");

                        boolean allValid = true;

                        if (addresses.length < 2 || addresses.length > 58) {
                            allValid = false;
                            addSyntaxError(
                                    result,
                                    definition,
                                    String.format("parameter '%s' must be comprised of between 2 and 58 standard addresses separated by commas!", param.name()));
                        }

                        if (allValid) {
                            for (String address : addresses) {
                                if (!AddressUtility.isValidStandardAddress(address)) {
                                    addSyntaxError(
                                            result,
                                            definition,
                                            String.format("parameter '%s' must be comprised of multiple standard addresses separated by commas, and '%s' is not a valid standard address", param.name(), address));
                                }
                            }
                        }

                        if (allValid) {
                            result.getParameters().put(param.name(), value);
                        }

                        break;
                    case COMMA_SEPARATED_PUBLIC_KEYS_OR_ADDRESSES:
                        String[] publicKeysOrAddresses = value.split(",");


                        allValid = true;

                        if (publicKeysOrAddresses.length < 2 || publicKeysOrAddresses.length > 58) {
                            allValid = false;
                            addSyntaxError(
                                    result,
                                    definition,
                                    String.format("parameter '%s' must be comprised of between 2 and 58 addresses or hex-encoded public keys separated by commas!", param.name()));
                        }

                        if (allValid) {
                            for (int i = 0; i < publicKeysOrAddresses.length; i++) {
                                String addressOrPublicKey = publicKeysOrAddresses[i];
                                if (!AddressUtility.isValidStandardAddress(addressOrPublicKey)) {
                                    if (!Utility.isHex(addressOrPublicKey)) {
                                        addSyntaxError(
                                                result,
                                                definition,
                                                String.format("parameter '%s' must be comprised of multiple hex-encoded public keys or addresses separated by commas, and '%s' is not a valid hex-encoded public key or standard address!", param.name(), addressOrPublicKey));
                                    }
                                    if (addressOrPublicKey.length() != 0 && addressOrPublicKey.length() != 176) {
                                        addSyntaxError(
                                                result,
                                                definition,
                                                String.format("parameter '%s' must be comprised of multiple hex-encoded public keys or addresses separated by commas, and '%s' is not a valid hex-encoded public key or standard address (should be 88 bytes, or valid standard address)!", param.name(), addressOrPublicKey));
                                    }
                                }
                            }
                        }

                        if (allValid) {
                            result.getParameters().put(param.name(), value);
                        }

                        break;
                    case COMMA_SEPARATED_SIGNATURES:
                        String[] signatures = value.split(",");

                        allValid = true;

                        if (signatures.length < 2 || signatures.length > 58) {
                            allValid = false;
                            addSyntaxError(
                                    result,
                                    definition,
                                    String.format("parameter '%s' must be comprised of between 2 and 58 hex-encoded public keys separated by commas!", param.name()));
                        }

                        if (allValid) {
                            for (String signature : signatures) {
                                if (!Utility.isHex(signature)) {
                                    addSyntaxError(
                                            result,
                                            definition,
                                            String.format("parameter '%s' must be comprised of multiple hex-encoded signatures separated by commas, and '%s' is not a valid hex-encoded signature!", param.name(), signature));
                                }
                                if (signature.length() != 0 && (signature.length() < 120 || signature.length() > 144)) {
                                    addSyntaxError(
                                            result,
                                            definition,
                                            String.format("parameter '%s' must be comprised of multiple hex-encoded public keys addresses separated by commas, and '%s' is not a valid hex-encoded public key (should be between 60 and 72 bytes, unless blank)!", param.name(), signature));
                                }
                            }
                        }

                        if (allValid) {
                            result.getParameters().put(param.name(), value);
                        }

                        break;
                    case INTEGER:
                        if (!Utility.isPositiveOrZeroInteger(value)) {
                            addSyntaxError(
                                    result,
                                    definition,
                                    String.format("parameter '%s' must be a positive 32-bit integer", param.name()));
                        } else {
                            result.getParameters().put(param.name(), Integer.parseInt(value));
                        }
                        break;
                    case LONG:
                        if (!Utility.isPositiveOrZeroLong(value)) {
                            addSyntaxError(
                                    result,
                                    definition,
                                    String.format("parameter '%s' must be a positive 64-bit integer", param.name()));
                        } else {
                            result.getParameters().put(param.name(), Long.parseLong(value));
                        }
                        break;
                    case OUTPUT: {
                        String[] outputs = value.split(",");
                        if (outputs.length == 0) {
                            addSyntaxError(
                                    result,
                                    definition,
                                    String.format("parameter '%s' must contain at least one output: address=amount", param.name()));
                        } else {
                            List<Output> list = new ArrayList<>();
                            for (String token : outputs) {
                                String[] values = token.split("=");
                                if (values.length < 2 || !Utility.isPositiveOrZeroLong(values[1])) {
                                    addSyntaxError(
                                            result,
                                            definition,
                                            "An output must be formatted as: address:STRING=amount:LONG");
                                } else {
                                    list.add(new Output(values[0], Long.parseLong(values[1])));
                                }
                            }
                            result.getParameters().put(param.name(), list);
                        }
                        break;
                    }
                    case BOOLEAN: {
                        String lowerCase = value.toLowerCase();
                        if (lowerCase.equals("true") || lowerCase.equals("t")) {
                            result.getParameters().put(param.name(), true);
                        } else if (lowerCase.equals("false") || lowerCase.equals("f")) {
                            result.getParameters().put(param.name(), false);
                        } else {
                            addSyntaxError(
                                    result,
                                    definition,
                                    String.format("parameter '%s' must be a boolean (true/false or t/f)", param.name()));
                        }
                        break;
                    }
                }
            }

            ++index;
        }

        return result;
    }

    private String[] handleSelectors(String[] parts, CommandFactoryResult result) {

        ArrayList<String> partList = new ArrayList<>();

        for(int i=0; i<parts.length; i++) {
            // process output filename
            if(parts[i].equals(FILENAME_SELECTOR)) {
                if(i<parts.length-1) {
                    result.getParameters().put(parts[i], parts[i+1]);
                    i++;
                } else {
                    addSelectorError(
                            result,
                            String.format("'-o' must be followed by filename"));
                }
            } else if(parts[i].equals(FORMAT_SELECTOR)) {
                if(i<parts.length-1) {
                    result.getParameters().put(parts[i], parts[i+1]);
                    i++;
                } else {
                    addSelectorError(
                            result,
                            String.format("'-f' must be followed by format specification (json, csv)"));
                }
            } else {
                partList.add(parts[i]);
            }
        }

        return partList.toArray(new String[partList.size()]);
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
                String.format("The command '%s' is not supported. Type 'help' to view all commands.", form),
                true);
        result.fail();
    }

    private void addSyntaxError(Result result, CommandDefinition definition, String message) {
        StringBuilder builder = new StringBuilder();
        builder.append(definition.getSpec().form());
        formatParameters(builder, definition.getParams());

        builder.append(String.format("\nERROR: %s", message));
        result.addMessage(
                "V009",
                "Syntax error",
                String.format("Usage: %s", builder.toString()),
                true);
        result.fail();
    }

    private void addSelectorError(Result result, String message) {
        StringBuilder builder = new StringBuilder();

        builder.append(String.format("\nERROR: %s", message));
        result.addMessage(
                "V010",
                "Selector parameter missing",
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
