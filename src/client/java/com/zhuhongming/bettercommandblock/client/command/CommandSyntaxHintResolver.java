package com.zhuhongming.bettercommandblock.client.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.context.SuggestionContext;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.commands.SharedSuggestionProvider;

public final class CommandSyntaxHintResolver {
    private static final Pattern SYNTAX_ELEMENT_PATTERN = Pattern.compile("<[^<>]+>");

    private CommandSyntaxHintResolver() {}

    public record State(String usageLine, List<String> syntaxElements, int activeArgumentIndex) {
        public static State empty() {
            return new State("", List.of(), -1);
        }

        public boolean isPresent() {
            return !this.usageLine.isEmpty();
        }
    }

    public static State resolve(
            String command,
            int cursor,
            CommandDispatcher<SharedSuggestionProvider> dispatcher,
            SharedSuggestionProvider provider) {
        if (command == null || command.isBlank() || dispatcher == null || provider == null) {
            return State.empty();
        }

        int clampedCursor = Math.max(0, Math.min(cursor, command.length()));
        try {
            ParseResults<SharedSuggestionProvider> parse = parseCommand(command, dispatcher, provider);
            SuggestionContext<SharedSuggestionProvider> context =
                    findSuggestionContextSafely(parse, clampedCursor);
            if (context == null) {
                return State.empty();
            }

            String usageLine =
                    resolveUsageLine(dispatcher, context, provider, command, clampedCursor, parse);
            if (usageLine.isEmpty()) {
                return State.empty();
            }

            List<String> syntaxElements = extractSyntaxElements(usageLine);
            int activeIndex = resolveActiveArgumentIndex(parse, context, clampedCursor);
            if (activeIndex < 0 || activeIndex >= syntaxElements.size()) {
                activeIndex = -1;
            }

            return new State(usageLine, syntaxElements, activeIndex);
        } catch (RuntimeException ignored) {
            return State.empty();
        }
    }

    /** 仅解析当前光标所在的语法占位符序号（相对 suggestion parent 的参数，与 Brigadier 一致）。 */
    public static int resolveActiveIndexOnly(
            String command,
            int cursor,
            CommandDispatcher<SharedSuggestionProvider> dispatcher,
            SharedSuggestionProvider provider) {
        if (command == null || command.isBlank() || dispatcher == null || provider == null) {
            return -1;
        }
        int clampedCursor = Math.max(0, Math.min(cursor, command.length()));
        try {
            ParseResults<SharedSuggestionProvider> parse = parseCommand(command, dispatcher, provider);
            SuggestionContext<SharedSuggestionProvider> context =
                    findSuggestionContextSafely(parse, clampedCursor);
            if (context == null) {
                return -1;
            }
            return resolveActiveArgumentIndex(parse, context, clampedCursor);
        } catch (RuntimeException ignored) {
            return -1;
        }
    }

    private static ParseResults<SharedSuggestionProvider> parseCommand(
            String command,
            CommandDispatcher<SharedSuggestionProvider> dispatcher,
            SharedSuggestionProvider provider) {
        StringReader reader = new StringReader(command);
        if (reader.canRead() && reader.peek() == '/') {
            reader.skip();
        }
        return dispatcher.parse(reader, provider);
    }

    private static SuggestionContext<SharedSuggestionProvider> findSuggestionContextSafely(
            ParseResults<SharedSuggestionProvider> parse, int cursor) {
        int maxCursor = parse.getReader().getTotalLength();
        int[] candidates = new int[] {cursor, Math.max(0, cursor - 1), Math.min(maxCursor, cursor + 1), maxCursor};
        for (int candidate : candidates) {
            try {
                return parse.getContext().findSuggestionContext(candidate);
            } catch (IllegalStateException ignored) {
                // Brigadier throws when no node exists before the cursor position.
            }
        }
        return null;
    }

    private static String resolveUsageLine(
            CommandDispatcher<SharedSuggestionProvider> dispatcher,
            SuggestionContext<SharedSuggestionProvider> context,
            SharedSuggestionProvider provider,
            String command,
            int cursor,
            ParseResults<SharedSuggestionProvider> parse) {
        if (context.parent == null) {
            return "";
        }

        Map<CommandNode<SharedSuggestionProvider>, String> usages =
                dispatcher.getSmartUsage(context.parent, provider);
        if (usages.isEmpty()) {
            return "";
        }
        if (usages.size() == 1) {
            return firstNonEmptyUsage(usages);
        }

        String chosen = selectUsageForAmbiguousParent(usages, command, cursor, context, parse);
        return chosen != null ? chosen : firstNonEmptyUsage(usages);
    }

    private static String firstNonEmptyUsage(
            Map<CommandNode<SharedSuggestionProvider>, String> usages) {
        for (Map.Entry<CommandNode<SharedSuggestionProvider>, String> entry : usages.entrySet()) {
            if (entry.getKey() instanceof LiteralCommandNode) {
                continue;
            }
            String usage = entry.getValue();
            if (usage != null && !usage.isEmpty()) {
                return usage;
            }
        }
        for (String usage : usages.values()) {
            if (usage != null && !usage.isEmpty()) {
                return usage;
            }
        }
        return "";
    }

    private static String selectUsageForAmbiguousParent(
            Map<CommandNode<SharedSuggestionProvider>, String> usages,
            String command,
            int cursor,
            SuggestionContext<SharedSuggestionProvider> context,
            ParseResults<SharedSuggestionProvider> parse) {
        String fragment = readArgumentFragment(command, context.startPos, cursor).toLowerCase(Locale.ROOT);

        if (prefersPrefixUsage(fragment)) {
            return longestUsage(usages);
        }
        if (prefersDedicatedUsage(fragment)) {
            return shortestUsage(usages);
        }

        CommandNode<SharedSuggestionProvider> target = findTargetChildNode(parse, context);
        if (target != null) {
            String direct = usages.get(target);
            if (direct != null && !direct.isEmpty()) {
                return direct;
            }
        }

        return null;
    }

    private static CommandNode<SharedSuggestionProvider> findTargetChildNode(
            ParseResults<SharedSuggestionProvider> parse,
            SuggestionContext<SharedSuggestionProvider> context) {
        List<ParsedCommandNode<SharedSuggestionProvider>> nodes = parse.getContext().getNodes();
        if (nodes.isEmpty()) {
            return null;
        }
        ParsedCommandNode<SharedSuggestionProvider> last = nodes.get(nodes.size() - 1);
        if (last.getNode() == context.parent && last.getRange().getEnd() <= context.startPos) {
            for (CommandNode<SharedSuggestionProvider> child : context.parent.getChildren()) {
                if (child instanceof ArgumentCommandNode<?, ?> arg
                        && arg.getName().equals(fragmentNameAt(context.startPos, parse))) {
                    return child;
                }
            }
        }
        return null;
    }

    private static String fragmentNameAt(
            int startPos, ParseResults<SharedSuggestionProvider> parse) {
        for (ParsedCommandNode<SharedSuggestionProvider> node : parse.getContext().getNodes()) {
            if (node.getRange().getStart() == startPos && node.getNode() instanceof ArgumentCommandNode<?, ?> arg) {
                return arg.getName();
            }
        }
        return "";
    }

    private static boolean prefersPrefixUsage(String fragment) {
        if (fragment.isEmpty()) {
            return true;
        }
        if ("-".startsWith(fragment)) {
            return true;
        }
        if (fragment.indexOf(':') >= 0) {
            return true;
        }
        if ("minecraft".startsWith(fragment)) {
            return true;
        }
        return fragment.startsWith("~") || fragment.startsWith("^");
    }

    private static boolean prefersDedicatedUsage(String fragment) {
        if (fragment.isEmpty()) {
            return false;
        }
        char first = fragment.charAt(0);
        return first == '"' || first == '\'' || Character.isDigit(first) || first == '.';
    }

    private static String longestUsage(Map<CommandNode<SharedSuggestionProvider>, String> usages) {
        return usages.values().stream()
                .filter(usage -> usage != null && !usage.isEmpty())
                .max(Comparator.comparingInt(String::length))
                .orElse("");
    }

    private static String shortestUsage(Map<CommandNode<SharedSuggestionProvider>, String> usages) {
        return usages.values().stream()
                .filter(usage -> usage != null && !usage.isEmpty())
                .min(Comparator.comparingInt(String::length))
                .orElse("");
    }

    private static String readArgumentFragment(String command, int startPos, int cursor) {
        int end = Math.max(startPos, Math.min(cursor, command.length()));
        if (startPos >= end) {
            return "";
        }
        return command.substring(startPos, end).trim();
    }

    /**
     * 按 Brigadier 已解析参数范围计数（{@code origin} / {@code direction} 等多 token 参数只占 1 格）。
     */
    static int resolveActiveArgumentIndex(
            ParseResults<SharedSuggestionProvider> parse,
            SuggestionContext<SharedSuggestionProvider> context,
            int cursor) {
        List<ParsedCommandNode<SharedSuggestionProvider>> nodes = parse.getContext().getNodes();
        int parentIndex = findParentIndex(nodes, context.parent);
        List<ParsedCommandNode<SharedSuggestionProvider>> argsAfterParent = new ArrayList<>();
        for (int i = parentIndex + 1; i < nodes.size(); i++) {
            if (nodes.get(i).getNode() instanceof ArgumentCommandNode<?, ?>) {
                argsAfterParent.add(nodes.get(i));
            }
        }

        for (int i = 0; i < argsAfterParent.size(); i++) {
            ParsedCommandNode<SharedSuggestionProvider> arg = argsAfterParent.get(i);
            int start = arg.getRange().getStart();
            int end = arg.getRange().getEnd();
            if (cursor >= start && cursor <= end) {
                return i;
            }
        }

        int completed = 0;
        for (ParsedCommandNode<SharedSuggestionProvider> arg : argsAfterParent) {
            if (arg.getRange().getEnd() < cursor) {
                completed++;
            } else if (arg.getRange().getStart() <= cursor && cursor <= arg.getRange().getEnd()) {
                return completed;
            } else {
                break;
            }
        }

        if (cursor >= context.startPos) {
            return completed;
        }
        return Math.max(0, completed);
    }

    private static int findParentIndex(
            List<ParsedCommandNode<SharedSuggestionProvider>> nodes,
            CommandNode<SharedSuggestionProvider> parent) {
        for (int i = nodes.size() - 1; i >= 0; i--) {
            if (nodes.get(i).getNode() == parent) {
                return i;
            }
        }
        return Math.max(0, nodes.size() - 1);
    }

    private static List<String> extractSyntaxElements(String usage) {
        List<String> elements = new ArrayList<>();
        Matcher matcher = SYNTAX_ELEMENT_PATTERN.matcher(usage);
        while (matcher.find()) {
            elements.add(matcher.group());
        }
        return elements;
    }
}
