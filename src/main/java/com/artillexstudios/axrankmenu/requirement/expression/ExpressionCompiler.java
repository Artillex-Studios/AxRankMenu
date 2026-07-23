package com.artillexstudios.axrankmenu.requirement.expression;

import com.artillexstudios.axrankmenu.requirement.parse.NumericParser;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class ExpressionCompiler {
    private static final int MAX_EXPRESSION_LENGTH = 4096;
    private static final int MAX_REGEX_LENGTH = 256;
    private static final Pattern UNSAFE_REGEX = Pattern.compile(
            "(?:\\\\[1-9]|\\(\\?[=!<]|\\([^)]*(?:\\*|\\+|\\{\\d+(?:,\\d*)?})[^)]*\\)(?:\\*|\\+|\\{\\d+(?:,\\d*)?}))"
    );

    private final String source;
    private final List<Token> tokens;
    private int current;

    private ExpressionCompiler(String source) {
        if (source == null || source.isBlank()) throw new ExpressionException("Expression is empty", 0);
        if (source.length() > MAX_EXPRESSION_LENGTH) throw new ExpressionException("Expression is too long", MAX_EXPRESSION_LENGTH);
        this.source = source;
        this.tokens = tokenize(source);
    }

    public static CompiledExpression compile(String source) {
        ExpressionCompiler parser = new ExpressionCompiler(source);
        Node root = parser.parseOr();
        parser.expect(Type.END, "Unexpected token");
        return new Compiled(source, root);
    }

    private Node parseOr() {
        Node node = parseAnd();
        while (match(Type.OR)) node = new Binary(node, previous(), parseAnd());
        return node;
    }

    private Node parseAnd() {
        Node node = parseComparison();
        while (match(Type.AND)) node = new Binary(node, previous(), parseComparison());
        return node;
    }

    private Node parseComparison() {
        Node node = parseUnary();
        if (match(Type.EQ, Type.NE, Type.GT, Type.GTE, Type.LT, Type.LTE)) {
            node = new Binary(node, previous(), parseUnary());
        }
        return node;
    }

    private Node parseUnary() {
        if (match(Type.NOT)) return new Unary(previous(), parseUnary());
        return parsePrimary();
    }

    private Node parsePrimary() {
        if (match(Type.NUMBER)) return new Literal(Value.number(new BigDecimal(previous().text())));
        if (match(Type.STRING)) {
            String value = previous().text();
            if (value.length() > 2 && value.startsWith("%") && value.endsWith("%") && value.indexOf('%', 1) == value.length() - 1) {
                return new Reference(value, true);
            }
            return new Literal(Value.string(value));
        }
        if (match(Type.TRUE)) return new Literal(Value.bool(true));
        if (match(Type.FALSE)) return new Literal(Value.bool(false));
        if (match(Type.PLACEHOLDER)) return new Reference(previous().text(), true);
        if (match(Type.IDENTIFIER)) {
            Token identifier = previous();
            if (match(Type.LEFT_PAREN)) {
                List<Node> arguments = new ArrayList<>();
                if (!check(Type.RIGHT_PAREN)) {
                    do {
                        arguments.add(parseOr());
                    } while (match(Type.COMMA));
                }
                expect(Type.RIGHT_PAREN, "Expected ')' after function arguments");
                validateFunction(identifier, arguments.size());
                return new Function(identifier, arguments);
            }
            return new Reference(identifier.text(), false);
        }
        if (match(Type.LEFT_PAREN)) {
            Node node = parseOr();
            expect(Type.RIGHT_PAREN, "Expected ')'");
            return node;
        }
        throw new ExpressionException("Expected value", peek().position());
    }

    private void validateFunction(Token token, int arguments) {
        String name = token.text().toLowerCase(Locale.ROOT);
        int expected = switch (name) {
            case "exists", "empty" -> 1;
            case "equals", "equalsignorecase", "contains", "startswith", "endswith", "matches" -> 2;
            default -> throw new ExpressionException("Unknown function '" + token.text() + "'", token.position());
        };
        if (arguments != expected) {
            throw new ExpressionException("Function '" + token.text() + "' expects " + expected + " arguments", token.position());
        }
    }

    private boolean match(Type... types) {
        for (Type type : types) {
            if (!check(type)) continue;
            current++;
            return true;
        }
        return false;
    }

    private boolean check(Type type) {
        return peek().type() == type;
    }

    private Token expect(Type type, String message) {
        if (check(type)) return tokens.get(current++);
        throw new ExpressionException(message, peek().position());
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private static List<Token> tokenize(String source) {
        List<Token> result = new ArrayList<>();
        int index = 0;
        while (index < source.length()) {
            char c = source.charAt(index);
            if (Character.isWhitespace(c)) {
                index++;
                continue;
            }
            int start = index;
            switch (c) {
                case '(' -> result.add(new Token(Type.LEFT_PAREN, "(", index++));
                case ')' -> result.add(new Token(Type.RIGHT_PAREN, ")", index++));
                case ',' -> result.add(new Token(Type.COMMA, ",", index++));
                case '%' -> {
                    int end = source.indexOf('%', index + 1);
                    if (end < 0) throw new ExpressionException("Unterminated placeholder", start);
                    String value = source.substring(index, end + 1);
                    if (value.length() <= 2) throw new ExpressionException("Empty placeholder", start);
                    result.add(new Token(Type.PLACEHOLDER, value, start));
                    index = end + 1;
                }
                case '\'', '"' -> {
                    char quote = c;
                    StringBuilder value = new StringBuilder();
                    index++;
                    boolean terminated = false;
                    while (index < source.length()) {
                        char next = source.charAt(index++);
                        if (next == quote) {
                            terminated = true;
                            break;
                        }
                        if (next == '\\' && index < source.length()) {
                            char escaped = source.charAt(index++);
                            value.append(switch (escaped) {
                                case 'n' -> '\n';
                                case 'r' -> '\r';
                                case 't' -> '\t';
                                default -> escaped;
                            });
                        } else {
                            value.append(next);
                        }
                    }
                    if (!terminated) throw new ExpressionException("Unterminated string", start);
                    result.add(new Token(Type.STRING, value.toString(), start));
                }
                case '&' -> {
                    if (index + 1 >= source.length() || source.charAt(index + 1) != '&')
                        throw new ExpressionException("Expected '&&'", start);
                    result.add(new Token(Type.AND, "&&", start));
                    index += 2;
                }
                case '|' -> {
                    if (index + 1 >= source.length() || source.charAt(index + 1) != '|')
                        throw new ExpressionException("Expected '||'", start);
                    result.add(new Token(Type.OR, "||", start));
                    index += 2;
                }
                case '!' -> {
                    if (index + 1 < source.length() && source.charAt(index + 1) == '=') {
                        result.add(new Token(Type.NE, "!=", start));
                        index += 2;
                    } else {
                        result.add(new Token(Type.NOT, "!", start));
                        index++;
                    }
                }
                case '=' -> {
                    if (index + 1 >= source.length() || source.charAt(index + 1) != '=')
                        throw new ExpressionException("Expected '=='", start);
                    result.add(new Token(Type.EQ, "==", start));
                    index += 2;
                }
                case '>' -> {
                    boolean equal = index + 1 < source.length() && source.charAt(index + 1) == '=';
                    result.add(new Token(equal ? Type.GTE : Type.GT, equal ? ">=" : ">", start));
                    index += equal ? 2 : 1;
                }
                case '<' -> {
                    boolean equal = index + 1 < source.length() && source.charAt(index + 1) == '=';
                    result.add(new Token(equal ? Type.LTE : Type.LT, equal ? "<=" : "<", start));
                    index += equal ? 2 : 1;
                }
                default -> {
                    if (Character.isDigit(c) || (c == '-' && index + 1 < source.length() && Character.isDigit(source.charAt(index + 1)))) {
                        index++;
                        while (index < source.length() && (Character.isDigit(source.charAt(index)) || source.charAt(index) == '.')) index++;
                        String number = source.substring(start, index);
                        try {
                            new BigDecimal(number);
                        } catch (NumberFormatException exception) {
                            throw new ExpressionException("Invalid number", start);
                        }
                        result.add(new Token(Type.NUMBER, number, start));
                    } else if (Character.isLetter(c) || c == '_') {
                        index++;
                        while (index < source.length() && (Character.isLetterOrDigit(source.charAt(index)) || source.charAt(index) == '_')) index++;
                        String word = source.substring(start, index);
                        Type type = switch (word.toLowerCase(Locale.ROOT)) {
                            case "and" -> Type.AND;
                            case "or" -> Type.OR;
                            case "not" -> Type.NOT;
                            case "true" -> Type.TRUE;
                            case "false" -> Type.FALSE;
                            default -> Type.IDENTIFIER;
                        };
                        result.add(new Token(type, word, start));
                    } else {
                        throw new ExpressionException("Invalid token '" + c + "'", start);
                    }
                }
            }
        }
        result.add(new Token(Type.END, "", source.length()));
        return result;
    }

    private enum Type {
        LEFT_PAREN, RIGHT_PAREN, COMMA, NUMBER, STRING, TRUE, FALSE, PLACEHOLDER, IDENTIFIER,
        NOT, AND, OR, EQ, NE, GT, GTE, LT, LTE, END
    }

    private record Token(Type type, String text, int position) {
    }

    private interface Node {
        Value evaluate(CompiledExpression.ValueResolver resolver, boolean strict);
    }

    private record Compiled(String source, Node root) implements CompiledExpression {
        @Override
        public boolean evaluate(ValueResolver resolver, boolean strict) {
            return root.evaluate(resolver, strict).asBoolean(strict);
        }
    }

    private record Literal(Value value) implements Node {
        @Override
        public Value evaluate(CompiledExpression.ValueResolver resolver, boolean strict) {
            return value;
        }
    }

    private record Reference(String name, boolean placeholder) implements Node {
        @Override
        public Value evaluate(CompiledExpression.ValueResolver resolver, boolean strict) {
            CompiledExpression.ResolvedValue resolved = resolver.resolve(name, placeholder);
            return resolved.present() && !resolved.value().isBlank()
                    ? Value.dynamic(resolved.value())
                    : Value.missing();
        }
    }

    private record Unary(Token operator, Node right) implements Node {
        @Override
        public Value evaluate(CompiledExpression.ValueResolver resolver, boolean strict) {
            return Value.bool(!right.evaluate(resolver, strict).asBoolean(strict));
        }
    }

    private record Binary(Node left, Token operator, Node right) implements Node {
        @Override
        public Value evaluate(CompiledExpression.ValueResolver resolver, boolean strict) {
            Value first = left.evaluate(resolver, strict);
            if (operator.type() == Type.AND && !first.asBoolean(strict)) return Value.bool(false);
            if (operator.type() == Type.OR && first.asBoolean(strict)) return Value.bool(true);
            Value second = right.evaluate(resolver, strict);
            return switch (operator.type()) {
                case AND -> Value.bool(second.asBoolean(strict));
                case OR -> Value.bool(second.asBoolean(strict));
                case EQ -> Value.bool(first.equalTo(second, strict));
                case NE -> Value.bool(!first.equalTo(second, strict));
                case GT -> Value.bool(first.compareTo(second, strict) > 0);
                case GTE -> Value.bool(first.compareTo(second, strict) >= 0);
                case LT -> Value.bool(first.compareTo(second, strict) < 0);
                case LTE -> Value.bool(first.compareTo(second, strict) <= 0);
                default -> throw new IllegalStateException("Unsupported operator " + operator.text());
            };
        }
    }

    private record Function(Token name, List<Node> arguments) implements Node {
        @Override
        public Value evaluate(CompiledExpression.ValueResolver resolver, boolean strict) {
            List<Value> values = arguments.stream().map(argument -> argument.evaluate(resolver, strict)).toList();
            String function = name.text().toLowerCase(Locale.ROOT);
            return switch (function) {
                case "exists" -> Value.bool(!values.getFirst().missing && !values.getFirst().asString(false).isBlank());
                case "empty" -> Value.bool(values.getFirst().missing || values.getFirst().asString(false).isBlank());
                case "equals" -> Value.bool(values.get(0).asString(strict).equals(values.get(1).asString(strict)));
                case "equalsignorecase" -> Value.bool(values.get(0).asString(strict).equalsIgnoreCase(values.get(1).asString(strict)));
                case "contains" -> Value.bool(values.get(0).asString(strict).contains(values.get(1).asString(strict)));
                case "startswith" -> Value.bool(values.get(0).asString(strict).startsWith(values.get(1).asString(strict)));
                case "endswith" -> Value.bool(values.get(0).asString(strict).endsWith(values.get(1).asString(strict)));
                case "matches" -> Value.bool(matches(values.get(0).asString(strict), values.get(1).asString(strict), name.position()));
                default -> throw new IllegalStateException("Unsupported function " + name.text());
            };
        }
    }

    private static boolean matches(String value, String regex, int position) {
        if (regex.length() > MAX_REGEX_LENGTH) throw new ExpressionException("Regex is too long", position);
        if (value.length() > 2048) throw new ExpressionException("Regex input is too long", position);
        if (UNSAFE_REGEX.matcher(regex).find()) throw new ExpressionException("Potentially unsafe regex", position);
        try {
            return Pattern.compile(regex).matcher(value).matches();
        } catch (PatternSyntaxException exception) {
            throw new ExpressionException("Invalid regex", position);
        }
    }

    private static final class Value {
        private final Object value;
        private final boolean missing;
        private final boolean dynamic;

        private Value(Object value, boolean missing, boolean dynamic) {
            this.value = value;
            this.missing = missing;
            this.dynamic = dynamic;
        }

        static Value number(BigDecimal value) {
            return new Value(value, false, false);
        }

        static Value string(String value) {
            return new Value(value, false, false);
        }

        static Value bool(boolean value) {
            return new Value(value, false, false);
        }

        static Value dynamic(String value) {
            return new Value(NumericParser.stripFormatting(value), false, true);
        }

        static Value missing() {
            return new Value("", true, true);
        }

        boolean asBoolean(boolean strict) {
            if (missing) {
                if (strict) throw new IllegalStateException("Required value is missing");
                return false;
            }
            if (value instanceof Boolean bool) return bool;
            String string = asString(strict).trim();
            if (string.equalsIgnoreCase("true")) return true;
            if (string.equalsIgnoreCase("false") || string.isEmpty()) return false;
            return asNumber(strict).compareTo(BigDecimal.ZERO) != 0;
        }

        String asString(boolean strict) {
            if (missing) {
                if (strict) throw new IllegalStateException("Required value is missing");
                return "";
            }
            if (value instanceof BigDecimal number) return number.stripTrailingZeros().toPlainString();
            return String.valueOf(value);
        }

        BigDecimal asNumber(boolean strict) {
            if (missing) {
                if (strict) throw new IllegalStateException("Required numeric value is missing");
                return BigDecimal.ZERO;
            }
            if (value instanceof BigDecimal number) return number;
            if (value instanceof Boolean bool) return bool ? BigDecimal.ONE : BigDecimal.ZERO;
            return NumericParser.parse(String.valueOf(value), true).orElseThrow(() ->
                    new IllegalStateException("Value is not numeric: " + value));
        }

        boolean equalTo(Value other, boolean strict) {
            if (value instanceof BigDecimal || other.value instanceof BigDecimal) {
                return asNumber(strict).compareTo(other.asNumber(strict)) == 0;
            }
            if (value instanceof Boolean || other.value instanceof Boolean) {
                return asBoolean(strict) == other.asBoolean(strict);
            }
            if (dynamic && other.dynamic) {
                var firstNumber = NumericParser.parse(asString(strict), true);
                var secondNumber = NumericParser.parse(other.asString(strict), true);
                if (firstNumber.isPresent() && secondNumber.isPresent())
                    return firstNumber.get().compareTo(secondNumber.get()) == 0;
            }
            return asString(strict).equals(other.asString(strict));
        }

        int compareTo(Value other, boolean strict) {
            return asNumber(strict).compareTo(other.asNumber(strict));
        }
    }
}
