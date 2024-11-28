package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Method method;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        for (Ast.Field field : ast.getFields()) {
            visit(field);


        }

        for (Ast.Method method : ast.getMethods()) {
            visit(method);

        }

        boolean hasMainMethod = ast.getMethods().stream()

                .anyMatch(m -> m.getName().equals("main") && m.getParameters().isEmpty() && m.getReturnTypeName().map("Integer"::equals).orElse(false));

        if (!hasMainMethod) {

            throw new RuntimeException("No params and int type required.");

        }

        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        Environment.Type type = Environment.getType(ast.getTypeName());
        scope.defineVariable(ast.getName(), ast.getName(), type, ast.getConstant(), Environment.NIL);

        if (ast.getValue().isPresent()) {

            visit(ast.getValue().get());
            requireAssignable(type, ast.getValue().get().getType());

        }

        ast.setVariable(scope.lookupVariable(ast.getName()));
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        List<Environment.Type> parameterTypes = ast.getParameterTypeNames().stream()
                .map(Environment::getType)
                .toList();
        Environment.Type returnType = ast.getReturnTypeName().map(Environment::getType).orElse(Environment.Type.NIL);

        scope.defineFunction(ast.getName(), ast.getName(), parameterTypes, returnType, args -> Environment.NIL);

        method = ast;

        scope = new Scope(scope);
        for (int i = 0; i < ast.getParameters().size(); i++) {

            scope.defineVariable(ast.getParameters().get(i), ast.getParameters().get(i), parameterTypes.get(i), false, Environment.NIL);

        }

        for (Ast.Statement statement : ast.getStatements()) {

            visit(statement);

        }

        scope = scope.getParent();

        ast.setFunction(scope.lookupFunction(ast.getName(), ast.getParameters().size()));

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());

        if (!(ast.getExpression() instanceof Ast.Expression.Function)) {


            throw new RuntimeException("Function call for expression statement");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        Environment.Type typeEnv = ast.getTypeName().map(Environment::getType).orElse(Environment.Type.ANY);

        if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());

            typeEnv = ast.getValue().get().getType();

        } else if (ast.getTypeName().isEmpty()) {
            throw new RuntimeException("If no value, type must be specified");
        }

        scope.defineVariable(ast.getName(), ast.getName(), typeEnv, false, Environment.NIL);
        ast.setVariable(scope.lookupVariable(ast.getName()));
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        if (!(ast.getReceiver() instanceof Ast.Expression.Access)) {
            throw new RuntimeException("Access expression for receiver");
        }

        visit(ast.getReceiver());
        visit(ast.getValue());
        requireAssignable(ast.getReceiver().getType(), ast.getValue().getType());

        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {

        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());

        if (ast.getThenStatements().isEmpty()) {

            throw new RuntimeException("No empty then statements allowed");

        }

        scope = new Scope(scope);
        for (Ast.Statement statement : ast.getThenStatements()) {

            visit(statement);

        }
        scope = scope.getParent();

        if (!ast.getElseStatements().isEmpty()) {

            scope = new Scope(scope);
            for (Ast.Statement statement : ast.getElseStatements()) {
                visit(statement);
            }
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.For ast) {
        // Initialization, condition, and increment are handled separately


        if (ast.getInitialization() != null) {

            visit(ast.getInitialization());

        }
        if (ast.getCondition() != null) {

            visit(ast.getCondition());

            requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
        }
        if (ast.getIncrement() != null) {
            visit(ast.getIncrement());

        }

        scope = new Scope(scope);

        for (Ast.Statement statement : ast.getStatements()) {
            visit(statement);
        }
        scope = scope.getParent();
        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        visit(ast.getCondition());

        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());

        scope = new Scope(scope);

        for (Ast.Statement statement : ast.getStatements()) {
            visit(statement);
        }
        scope = scope.getParent();
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        visit(ast.getValue());

        requireAssignable(method.getReturnTypeName().map(Environment::getType).orElse(Environment.Type.NIL),

                ast.getValue().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        Object litObj = ast.getLiteral();
        if (litObj instanceof Boolean) {
            ast.setType(Environment.Type.BOOLEAN);
        } else if (litObj instanceof BigInteger) {
            BigInteger value = (BigInteger) litObj;
            if (value.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0 || value.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0) {

                throw new RuntimeException("Not within the range of ints");

            }
            ast.setType(Environment.Type.INTEGER);

        } else if (litObj instanceof BigDecimal) {

            ast.setType(Environment.Type.DECIMAL);
        } else if (litObj instanceof String) {

            ast.setType(Environment.Type.STRING);
        } else if (litObj == Environment.NIL) {

            ast.setType(Environment.Type.NIL);
        } else {
            throw new RuntimeException("Not a literal supported type");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        visit(ast.getExpression());
        if (!(ast.getExpression() instanceof Ast.Expression.Binary)) {
            throw new RuntimeException("Binary expression must be in group");
        }
        ast.setType(ast.getExpression().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        visit(ast.getLeft());
        visit(ast.getRight());

        switch (ast.getOperator()) {
            case "&&":

            case "||":

                requireAssignable(Environment.Type.BOOLEAN, ast.getLeft().getType());
                requireAssignable(Environment.Type.BOOLEAN, ast.getRight().getType());

                ast.setType(Environment.Type.BOOLEAN);

                break;
            case "<":

            case "<=":

            case ">":

            case ">=":

            case "==":

            case "!=":

                requireAssignable(Environment.Type.COMPARABLE, ast.getLeft().getType());
                requireAssignable(Environment.Type.COMPARABLE, ast.getRight().getType());
                ast.setType(Environment.Type.BOOLEAN);
                break;

            case "+":

                if (ast.getLeft().getType() == Environment.Type.STRING || ast.getRight().getType() == Environment.Type.STRING) {
                    ast.setType(Environment.Type.STRING);
                } else if (ast.getLeft().getType() == ast.getRight().getType()) {
                    ast.setType(ast.getLeft().getType());
                } else {
                    throw new RuntimeException("These two types are not compatible for addition");
                }
                break;

            case "-":

            case "*":

            case "/":

                if (ast.getLeft().getType() != ast.getRight().getType()) {
                    throw new RuntimeException("These two types are not compatible for this operation");
                }
                ast.setType(ast.getLeft().getType());
                break;

            default:

                throw new RuntimeException("Binary operator not supported");

        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {

        if (ast.getReceiver().isPresent()) {

            visit(ast.getReceiver().get());
            Environment.Variable variable = scope.lookupVariable(ast.getName());
            ast.setVariable(variable);

        } else {

            ast.setVariable(scope.lookupVariable(ast.getName()));

        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());

            Environment.Function function = scope.lookupFunction(ast.getName(), ast.getArguments().size());
            for (int i = 0; i < ast.getArguments().size(); i++) {

                visit(ast.getArguments().get(i));
                requireAssignable(function.getParameterTypes().get(i), ast.getArguments().get(i).getType());
            }
            ast.setFunction(function);

        } else {
            Environment.Function function = scope.lookupFunction(ast.getName(), ast.getArguments().size());
            for (int i = 0; i < ast.getArguments().size(); i++) {
                visit(ast.getArguments().get(i));

                requireAssignable(function.getParameterTypes().get(i), ast.getArguments().get(i).getType());
            }
            ast.setFunction(function);
        }
        return null;
    }


    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if (target != type && target != Environment.Type.ANY && target != Environment.Type.COMPARABLE) {

            throw new RuntimeException("These types are not compatible");
        }
    }
}
