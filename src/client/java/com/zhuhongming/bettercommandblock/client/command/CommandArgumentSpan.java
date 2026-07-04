package com.zhuhongming.bettercommandblock.client.command;

import java.util.ArrayList;
import java.util.List;

/**
 * Locates command argument spans at the cursor, including selectors, quoted strings, bracketed values
 * and coordinate triplets ({@code ~ ~ ~}, {@code 0 0 0}, {@code ^ ^ ^}).
 */
public final class CommandArgumentSpan {
    private CommandArgumentSpan() {}

    public record Range(int start, int end) {
        public int length() {
            return this.end - this.start;
        }
    }

    public static Range atCursor(String text, int cursor) {
        if (text == null || text.isEmpty()) {
            return new Range(0, 0);
        }

        List<Range> tokens = tokenize(text);
        if (tokens.isEmpty()) {
            int clamped = clamp(cursor, 0, text.length());
            return new Range(clamped, clamped);
        }

        int clamped = clamp(cursor, 0, text.length());
        for (int i = 0; i < tokens.size(); i++) {
            Range token = tokens.get(i);
            if (clamped >= token.start && clamped < token.end) {
                return token;
            }
            if (clamped == token.end) {
                return i + 1 < tokens.size() ? tokens.get(i + 1) : token;
            }
        }

        Range last = tokens.get(tokens.size() - 1);
        if (clamped > last.end) {
            return new Range(clamped, clamped);
        }
        return last;
    }

    public static int argumentIndexAt(String command, int cursor) {
        List<Range> tokens = tokenize(command);
        if (tokens.size() <= 1) {
            return -1;
        }

        Range activeToken = atCursor(command, cursor);
        for (int i = 0; i < tokens.size(); i++) {
            Range token = tokens.get(i);
            if (token.start == activeToken.start && token.end == activeToken.end) {
                return i > 0 ? i - 1 : -1;
            }
        }

        if (activeToken.length() == 0) {
            int clamped = clamp(cursor, 0, command.length());
            int lastArgumentToken = -1;
            for (int i = 1; i < tokens.size(); i++) {
                if (tokens.get(i).start <= clamped) {
                    lastArgumentToken = i;
                }
            }
            if (lastArgumentToken > 0) {
                return lastArgumentToken - 1;
            }
        }

        return -1;
    }

    public static List<Range> tokenize(String text) {
        List<Range> tokens = new ArrayList<>();
        int index = 0;
        while (index < text.length()) {
            while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
                index++;
            }
            if (index >= text.length()) {
                break;
            }
            Range token = spanAtIndex(text, index);
            if (token.end > token.start) {
                tokens.add(token);
            }
            index = Math.max(token.end, index + 1);
        }
        return tokens;
    }

    public static Range union(Range first, Range second) {
        return new Range(Math.min(first.start, second.start), Math.max(first.end, second.end));
    }

    private static Range spanAtIndex(String text, int index) {
        char first = text.charAt(index);
        if (first == '@') {
            return readSelector(text, index);
        }
        if (first == '"' || first == '\'') {
            return readQuoted(text, index, first);
        }
        if (first == '{' || first == '[') {
            return new Range(index, findMatchingBracket(text, index, first, first == '{' ? '}' : ']'));
        }
        Range coordinate = tryReadCoordinateTriplet(text, index);
        if (coordinate != null) {
            return coordinate;
        }
        int end = index + 1;
        while (end < text.length() && !Character.isWhitespace(text.charAt(end))) {
            end++;
        }
        return new Range(index, end);
    }

    /** 将 {@code origin} / {@code localVec3} 等多分量参数作为单个 token。 */
    private static Range tryReadCoordinateTriplet(String text, int index) {
        char first = text.charAt(index);
        boolean relative = first == '~' || first == '^';
        if (!relative && !isNumberStart(first)) {
            return null;
        }

        int start = index;
        int pos = index;
        int components = 0;
        while (components < 3 && pos < text.length()) {
            while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
                pos++;
            }
            if (pos >= text.length()) {
                break;
            }
            int componentEnd = endOfCoordinateComponent(text, pos);
            if (componentEnd <= pos) {
                break;
            }
            pos = componentEnd;
            components++;
        }

        if (components == 0) {
            return null;
        }
        if (relative || components == 3) {
            return new Range(start, pos);
        }
        return null;
    }

    private static int endOfCoordinateComponent(String text, int index) {
        char first = text.charAt(index);
        if (first == '~' || first == '^') {
            int pos = index + 1;
            if (pos < text.length() && isNumberStart(text.charAt(pos))) {
                while (pos < text.length()) {
                    char current = text.charAt(pos);
                    if (Character.isWhitespace(current)) {
                        break;
                    }
                    if (!isNumberChar(current)) {
                        break;
                    }
                    pos++;
                }
            }
            return pos;
        }
        if (isNumberStart(first)) {
            int pos = index + 1;
            while (pos < text.length()) {
                char current = text.charAt(pos);
                if (Character.isWhitespace(current)) {
                    break;
                }
                if (!isNumberChar(current)) {
                    break;
                }
                pos++;
            }
            return pos;
        }
        return index;
    }

    private static boolean isNumberStart(char c) {
        return c == '-' || c == '+' || c == '.' || (c >= '0' && c <= '9');
    }

    private static boolean isNumberChar(char c) {
        return isNumberStart(c) || c == 'e' || c == 'E';
    }

    private static Range readSelector(String text, int start) {
        int end = start + 1;
        while (end < text.length()) {
            char current = text.charAt(end);
            if (Character.isWhitespace(current)) {
                break;
            }
            if (current == '[') {
                end = findMatchingBracket(text, end, '[', ']');
                break;
            }
            end++;
        }
        return new Range(start, end);
    }

    private static Range readQuoted(String text, int start, char quote) {
        int end = start + 1;
        while (end < text.length()) {
            char current = text.charAt(end);
            if (current == '\\') {
                end += 2;
                continue;
            }
            if (current == quote) {
                return new Range(start, end + 1);
            }
            end++;
        }
        return new Range(start, text.length());
    }

    private static int findMatchingBracket(String text, int openIndex, char open, char close) {
        int depth = 0;
        boolean inString = false;
        char stringQuote = 0;
        for (int i = openIndex; i < text.length(); i++) {
            char current = text.charAt(i);
            if (inString) {
                if (current == '\\') {
                    i++;
                    continue;
                }
                if (current == stringQuote) {
                    inString = false;
                }
                continue;
            }
            if (current == '"' || current == '\'') {
                inString = true;
                stringQuote = current;
                continue;
            }
            if (current == open) {
                depth++;
            } else if (current == close) {
                depth--;
                if (depth == 0) {
                    return i + 1;
                }
            }
        }
        return text.length();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }
}
