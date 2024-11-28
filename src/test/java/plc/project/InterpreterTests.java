package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

final class InterpreterTests {

    @ParameterizedTest
    @MethodSource
    void testSource(String test, Ast.Source ast, Object expected) {
        test(ast, expected, new Scope(null));
    }

    private static Stream<Arguments> testSource() {
        return Stream.of(
                Arguments.of("Main", new Ast.Source(
                        Arrays.asList(),
                        Arrays.asList(new Ast.Method("main", Arrays.asList(), Arrays.asList(
                                new Ast.Statement.Return(new Ast.Expression.Literal(BigInteger.ZERO)))
                        ))
                ), BigInteger.ZERO),
                Arguments.of("Fields & No Return", new Ast.Source(
                        Arrays.asList(
                                new Ast.Field("x", false, Optional.of(new Ast.Expression.Literal(BigInteger.ONE))),
                                new Ast.Field("y", false, Optional.of(new Ast.Expression.Literal(BigInteger.TEN)))
                        ),
                        Arrays.asList(new Ast.Method("main", Arrays.asList(), Arrays.asList(
                                new Ast.Statement.Expression(new Ast.Expression.Binary("+",
                                        new Ast.Expression.Access(Optional.empty(), "x"),
                                        new Ast.Expression.Access(Optional.empty(), "y")
                                ))
                        )))
                ), Environment.NIL.getValue())
        );
    }


    @ParameterizedTest
    @MethodSource
    void testField(String test, Ast.Field ast, Object expected) {
        Scope scope = test(ast, Environment.NIL.getValue(), new Scope(null));
        Assertions.assertEquals(expected, scope.lookupVariable(ast.getName()).getValue().getValue());
    }

    private static Stream<Arguments> testField() {
        return Stream.of(
                Arguments.of("Declaration", new Ast.Field("name", false, Optional.empty()), Environment.NIL.getValue()),
                Arguments.of("Initialization", new Ast.Field("name", false, Optional.of(new Ast.Expression.Literal(BigInteger.ONE))), BigInteger.ONE),
                Arguments.of("Initialization BigDecimal", new Ast.Field("value", false, Optional.of(new Ast.Expression.Literal(new BigDecimal("3.14")))), new BigDecimal("3.14"))
        );
    }

    @ParameterizedTest
    @MethodSource
    void testMethod(String test, Ast.Method ast, List<Environment.PlcObject> args, Object expected) {
        Scope scope = test(ast, Environment.NIL.getValue(), new Scope(null));
        Assertions.assertEquals(expected, scope.lookupFunction(ast.getName(), args.size()).invoke(args).getValue());
    }

    private static Stream<Arguments> testMethod() {
        return Stream.of(
                Arguments.of("Main",
                        new Ast.Method("main", Arrays.asList(), Arrays.asList(
                                new Ast.Statement.Return(new Ast.Expression.Literal(BigInteger.ZERO)))
                        ),
                        Arrays.asList(),
                        BigInteger.ZERO
                ),
                Arguments.of("Arguments",
                        new Ast.Method("main", Arrays.asList("x"), Arrays.asList(
                                new Ast.Statement.Return(new Ast.Expression.Binary("*",
                                        new Ast.Expression.Access(Optional.empty(), "x"),
                                        new Ast.Expression.Access(Optional.empty(), "x")
                                ))
                        )),
                        Arrays.asList(Environment.create(BigInteger.TEN)),
                        BigInteger.valueOf(100)
                ),
                Arguments.of("Double Argument Addition",
                        new Ast.Method("sum", Arrays.asList("x", "y"), Arrays.asList(
                                new Ast.Statement.Return(new Ast.Expression.Binary("+",
                                        new Ast.Expression.Access(Optional.empty(), "x"),
                                        new Ast.Expression.Access(Optional.empty(), "y")
                                ))
                        )),
                        Arrays.asList(Environment.create(BigInteger.TEN), Environment.create(BigInteger.valueOf(5))),
                        BigInteger.valueOf(15)
                )
        );
    }

    @Test
    void testExpressionStatement() {
        PrintStream sysout = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        try {
            test(new Ast.Statement.Expression(
                    new Ast.Expression.Function(Optional.empty(), "print", Arrays.asList(new Ast.Expression.Literal("Hello, World!")))
            ), Environment.NIL.getValue(), new Scope(null));
            Assertions.assertEquals("Hello, World!" + System.lineSeparator(), out.toString());
        } finally {
            System.setOut(sysout);
        }
    }

    @ParameterizedTest
    @MethodSource
    void testDeclarationStatement(String test, Ast.Statement.Declaration ast, Object expected) {
        Scope scope = test(ast, Environment.NIL.getValue(), new Scope(null));
        Assertions.assertEquals(expected, scope.lookupVariable(ast.getName()).getValue().getValue());
    }

    private static Stream<Arguments> testDeclarationStatement() {
        return Stream.of(
                Arguments.of("Declaration", new Ast.Statement.Declaration("name", Optional.empty()), Environment.NIL.getValue()),
                Arguments.of("Initialization", new Ast.Statement.Declaration("name", Optional.of(new Ast.Expression.Literal(BigInteger.ONE))), BigInteger.ONE)
        );
    }

    @Test
    void testVariableAssignmentStatement() {
        Scope scope = new Scope(null);
        scope.defineVariable("variable", false, Environment.create("variable"));
        test(new Ast.Statement.Assignment(
                new Ast.Expression.Access(Optional.empty(),"variable"),
                new Ast.Expression.Literal(BigInteger.ONE)
        ), Environment.NIL.getValue(), scope);
        Assertions.assertEquals(BigInteger.ONE, scope.lookupVariable("variable").getValue().getValue());
    }

    @Test
    void testFieldAssignmentStatement() {
        Scope scope = new Scope(null);
        Scope object = new Scope(null);
        object.defineVariable("field", false, Environment.create("object.field"));
        scope.defineVariable("object", false, new Environment.PlcObject(object, "object"));
        test(new Ast.Statement.Assignment(
                new Ast.Expression.Access(Optional.of(new Ast.Expression.Access(Optional.empty(), "object")),"field"),
                new Ast.Expression.Literal(BigInteger.ONE)
        ), Environment.NIL.getValue(), scope);
        Assertions.assertEquals(BigInteger.ONE, object.lookupVariable("field").getValue().getValue());
    }

    @ParameterizedTest
    @MethodSource
    void testIfStatement(String test, Ast.Statement.If ast, Object expected) {
        Scope scope = new Scope(null);
        scope.defineVariable("num", false, Environment.NIL);
        test(ast, Environment.NIL.getValue(), scope);
        Assertions.assertEquals(expected, scope.lookupVariable("num").getValue().getValue());
    }

    private static Stream<Arguments> testIfStatement() {
        return Stream.of(
                Arguments.of("True Condition",
                        new Ast.Statement.If(
                                new Ast.Expression.Literal(true),
                                Arrays.asList(new Ast.Statement.Assignment(new Ast.Expression.Access(Optional.empty(),"num"), new Ast.Expression.Literal(BigInteger.ONE))),
                                Arrays.asList()
                        ),
                        BigInteger.ONE
                ),
                Arguments.of("False Condition",
                        new Ast.Statement.If(
                                new Ast.Expression.Literal(false),
                                Arrays.asList(),
                                Arrays.asList(new Ast.Statement.Assignment(new Ast.Expression.Access(Optional.empty(),"num"), new Ast.Expression.Literal(BigInteger.TEN)))
                        ),
                        BigInteger.TEN
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testIfStatementWithMixedTypes(String test, Ast.Statement.If ast, Object expected) {
        Scope scope = new Scope(null);
        scope.defineVariable("result", false, Environment.NIL);
        test(ast, Environment.NIL.getValue(), scope);
        Assertions.assertEquals(expected, scope.lookupVariable("result").getValue().getValue());
    }

    private static Stream<Arguments> testIfStatementWithMixedTypes() {
        return Stream.of(
                Arguments.of("If with BigDecimal condition",
                        new Ast.Statement.If(
                                new Ast.Expression.Binary(">=",
                                        new Ast.Expression.Literal(new BigDecimal("1.5")),
                                        new Ast.Expression.Literal(BigDecimal.ONE)
                                ),
                                Arrays.asList(new Ast.Statement.Assignment(
                                        new Ast.Expression.Access(Optional.empty(), "result"),
                                        new Ast.Expression.Literal(BigInteger.TEN)
                                )),
                                Arrays.asList()
                        ),
                        BigInteger.TEN
                ),
                Arguments.of("If with BigInteger condition",
                        new Ast.Statement.If(
                                new Ast.Expression.Binary("<",
                                        new Ast.Expression.Literal(BigInteger.ZERO),
                                        new Ast.Expression.Literal(BigInteger.ONE)
                                ),
                                Arrays.asList(new Ast.Statement.Assignment(
                                        new Ast.Expression.Access(Optional.empty(), "result"),
                                        new Ast.Expression.Literal(BigInteger.ONE)
                                )),
                                Arrays.asList(new Ast.Statement.Assignment(
                                        new Ast.Expression.Access(Optional.empty(), "result"),
                                        new Ast.Expression.Literal(BigInteger.TEN)
                                ))
                        ),
                        BigInteger.ONE
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testLogFunction(String test, Ast.Expression.Function ast, Object expected) {
        Scope scope = new Scope(null);
        scope.defineFunction("log", 1, args -> {
            Object value = args.get(0).getValue();
            if (value instanceof BigInteger) {
                value = new BigDecimal((BigInteger) value);
            }
            if (!(value instanceof BigDecimal)) {
                throw new RuntimeException("The BigDecimal Type was expected but received " + value.getClass().getName() + ".");
            }
            BigDecimal input = (BigDecimal) value;
            BigDecimal result = BigDecimal.valueOf(Math.log(input.doubleValue()));
            return Environment.create(result);
        });
        test(ast, expected, scope);
    }

    private static Stream<Arguments> testLogFunction() {
        return Stream.of(
                Arguments.of("Log BigDecimal",
                        new Ast.Expression.Function(Optional.empty(), "log", Arrays.asList(new Ast.Expression.Literal(new BigDecimal("10")))),
                        BigDecimal.valueOf(Math.log(10))
                ),
                Arguments.of("Log BigInteger",
                        new Ast.Expression.Function(Optional.empty(), "log", Arrays.asList(new Ast.Expression.Literal(BigInteger.TEN))),
                        BigDecimal.valueOf(Math.log(10))
                ),
                Arguments.of("Log Invalid Type",
                        new Ast.Expression.Function(Optional.empty(), "log", Arrays.asList(new Ast.Expression.Literal("InvalidType"))),
                        null // Expected to throw an exception
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testFunctionWithInvalidArguments(String test, Ast.Expression.Function ast, Class<? extends Throwable> expectedException) {
        Scope scope = new Scope(null);
        scope.defineFunction("multiply", 2, args -> {
            BigDecimal left = requireType(BigDecimal.class, args.get(0));
            BigDecimal right = requireType(BigDecimal.class, args.get(1));
            return Environment.create(left.multiply(right));
        });
        Assertions.assertThrows(expectedException, () -> test(ast, null, scope));
    }

    private static Stream<Arguments> testFunctionWithInvalidArguments() {
        return Stream.of(
                Arguments.of("Multiply with BigDecimal and BigInteger",
                        new Ast.Expression.Function(Optional.empty(), "multiply",
                                Arrays.asList(
                                        new Ast.Expression.Literal(new BigDecimal("10.5")),
                                        new Ast.Expression.Literal(BigInteger.TEN)
                                )),
                        RuntimeException.class
                ),
                Arguments.of("Multiply with String argument",
                        new Ast.Expression.Function(Optional.empty(), "multiply",
                                Arrays.asList(
                                        new Ast.Expression.Literal("InvalidType"),
                                        new Ast.Expression.Literal(new BigDecimal("5.0"))
                                )),
                        RuntimeException.class
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    void testBinaryExpression(String test, Ast ast, Object expected) {
        test(ast, expected, new Scope(null));
    }

    private static Stream<Arguments> testBinaryExpression() {
        return Stream.of(
                Arguments.of("And",
                        new Ast.Expression.Binary("&&",
                                new Ast.Expression.Literal(true),
                                new Ast.Expression.Literal(false)
                        ),
                        false
                ),
                Arguments.of("Or (Short Circuit)",
                        new Ast.Expression.Binary("||",
                                new Ast.Expression.Literal(true),
                                new Ast.Expression.Access(Optional.empty(), "undefined")
                        ),
                        true
                ),
                Arguments.of("Less Than",
                        new Ast.Expression.Binary("<",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        true
                ),
                Arguments.of("Greater Than or Equal",
                        new Ast.Expression.Binary(">=",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        false
                ),
                Arguments.of("Equal",
                        new Ast.Expression.Binary("==",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        false
                ),
                Arguments.of("Concatenation",
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Literal("a"),
                                new Ast.Expression.Literal("b")
                        ),
                        "ab"
                ),
                Arguments.of("Addition",
                        new Ast.Expression.Binary("+",
                                new Ast.Expression.Literal(BigInteger.ONE),
                                new Ast.Expression.Literal(BigInteger.TEN)
                        ),
                        BigInteger.valueOf(11)
                ),
                Arguments.of("Division",
                        new Ast.Expression.Binary("/",
                                new Ast.Expression.Literal(new BigDecimal("1.2")),
                                new Ast.Expression.Literal(new BigDecimal("3.4"))
                        ),
                        new BigDecimal("0.4")
                )
        );
    }

    private static Scope test(Ast ast, Object expected, Scope scope) {
        Interpreter interpreter = new Interpreter(scope);
        if (expected != null) {
            Assertions.assertEquals(expected, interpreter.visit(ast).getValue());
        } else {
            Assertions.assertThrows(RuntimeException.class, () -> interpreter.visit(ast));
        }
        return interpreter.getScope();
    }

    // Local requireType helper function
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }
}
