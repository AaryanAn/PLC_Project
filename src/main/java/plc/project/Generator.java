package plc.project;

import java.io.PrintWriter;


public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        print("public class Main {");
        newline(0);
        indent++;

        // Fields
        for (Ast.Field field : ast.getFields()) {

            newline(indent);
            visit(field);

        }
        if (!ast.getFields().isEmpty()) {

            newline(0);
            // add one blank line after the fields
        }


        newline(indent);

        print("public static void main(String[] args) {");

        indent++;

        newline(indent);

        print("System.exit(new Main().main());");

        indent--;

        newline(indent);

        print("}");

        newline(0);


        // Methods

        for (Ast.Method method : ast.getMethods()) {

            newline(indent);

            visit(method);

        }

        indent--;
        newline(indent);
        print("\n}");

        return null;
    }



    @Override
    public Void visit(Ast.Field ast) {
        if (ast.getConstant()) {

            print("final ");

        }
        print(getTypeName(ast.getTypeName()), " ", ast.getName());

        if (ast.getValue().isPresent()) {
            print(" = ");
            visit(ast.getValue().get());
        }
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        print(getTypeName(ast.getReturnTypeName().orElse("void")), " ", ast.getName(), "(");
        for (int i = 0; i < ast.getParameters().size(); i++) {
            if (i > 0) {

                print(", ");
            }
            print(getTypeName(ast.getParameterTypeNames().get(i)), " ", ast.getParameters().get(i));
        }
        print(") {");

        indent++;
        for (Ast.Statement statement : ast.getStatements()) {
            newline(indent);
            visit(statement);
        }
        indent--;
        newline(indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        print(getTypeName(ast.getVariable().getType().getJvmName()), " ", ast.getVariable().getJvmName());
        if (ast.getValue().isPresent()) {
            print(" = ");
            visit(ast.getValue().get());
        }
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        visit(ast.getReceiver());
        print(" = ");
        visit(ast.getValue());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        print("if (");
        visit(ast.getCondition());
        print(") {");
        indent++;
        for (Ast.Statement statement : ast.getThenStatements()) {
            newline(indent);
            visit(statement);
        }
        indent--;
        newline(indent);
        print("}");
        if (!ast.getElseStatements().isEmpty()) {
            print(" else {");
            indent++;
            for (Ast.Statement statement : ast.getElseStatements()) {
                newline(indent);
                visit(statement);
            }
            indent--;
            newline(indent);
            print("}");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.For ast) {
        print("for ( ");

        // Handle initialization
        if (ast.getInitialization() != null) {
            visit(ast.getInitialization());
        } else {
            print(";"); // Print an empty semicolon for missing initialization
        }

        // Handle condition
        print(" ");
        if (ast.getCondition() != null) {
            visit(ast.getCondition());
        }
        print(";");

//        // Handle increment
//        if (ast.getIncrement() != null) {
//            print(" ");
//            visit(ast.getIncrement()); // extra semicolon issue at end is here because getincrement places one
//        } else {
//            print(" "); // If there is no final condition for increment still put a space
//        }

        // Handle increment
        if (ast.getIncrement() != null) {
            print(" ");
            if (ast.getIncrement() instanceof Ast.Statement.Assignment) {
                // Assignment handling
                Ast.Statement.Assignment reassignCast = (Ast.Statement.Assignment) ast.getIncrement();
                visit(reassignCast.getReceiver()); // Reciever Visit
                print(" = ");
                visit(reassignCast.getValue()); // Value visit
            } else {
                visit(ast.getIncrement());
            }
            print(" ");
        } else {
            print(" "); // Missing increment should still have space
        }


        print(") {"); // close loop

        // Loop body
        indent++;
        for (Ast.Statement statement : ast.getStatements()) {
            newline(indent);

            // change print to "System.out.println"
            if (statement instanceof Ast.Statement.Expression) {
                Ast.Statement.Expression exprStmt = (Ast.Statement.Expression) statement;
                if (exprStmt.getExpression() instanceof Ast.Expression.Function) {
                    Ast.Expression.Function functionExpr = (Ast.Expression.Function) exprStmt.getExpression();
                    if ("print".equals(functionExpr.getName())) {
                        print("System.out.println(");
                        for (int i = 0; i < functionExpr.getArguments().size(); i++) {
                            visit(functionExpr.getArguments().get(i));
                            if (i < functionExpr.getArguments().size() - 1) {
                                print(", ");
                            }
                        }
                        print(");");
                        continue;
                    }
                }
            }
            visit(statement);
            // other statements visit
        }
        indent--;

// end loop
        newline(indent);
        print("}");
        return null;
    }



    @Override
    public Void visit(Ast.Statement.While ast) {
        print("while (");
        visit(ast.getCondition());
        print(") {");

        indent++;
        for (Ast.Statement statement : ast.getStatements()) {
            newline(indent);
            visit(statement);
        }
        indent--;
        newline(indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        print("return ");
        visit(ast.getValue());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        Object literal = ast.getLiteral();
        if (literal instanceof String) {

            print("\"", literal, "\"");
        } else {
            print(literal.toString());

        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        print("(");
        visit(ast.getExpression());
        print(")");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        visit(ast.getLeft());
        print(" ", ast.getOperator(), " ");
        visit(ast.getRight());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
            print(".");
        }
        print(ast.getName());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        //            // change print to "System.out.println"
        if ("print".equals(ast.getName())) {
            print("System.out.println(");
            for (int i = 0; i < ast.getArguments().size(); i++) {
                visit(ast.getArguments().get(i));
                if (i < ast.getArguments().size() - 1) {
                    print(", ");
                }
            }
            print(")");
        } else {

            if (ast.getReceiver().isPresent()) {
                visit(ast.getReceiver().get());
                print(".");
            }
            print(ast.getName(), "(");
            for (int i = 0; i < ast.getArguments().size(); i++) {
                visit(ast.getArguments().get(i));
                if (i < ast.getArguments().size() - 1) {

                    print(", ");
                }
            }
            print(")");
        }
        return null;
    }

    private String getTypeName(String type) {
        switch (type) {
            case "Integer":
                return "int";
            case "Decimal":
                return "double";
            case "Boolean":
                return "boolean";
            case "Character":

                return "char";
            case "String":
                return "String";
            default:
                return type;
        }
    }
}
