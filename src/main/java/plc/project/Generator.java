package plc.project;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;

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

//    @Override
//    public Void visit(Ast.Source ast) {
//        throw new UnsupportedOperationException(); //TODO
//    }

    @Override
    public Void visit(Ast.Source ast) {
        // Generate the class header
        print("public class Main {");
        newline(0); // Empty line

        // Generate fields
        for (Ast.Field field : ast.getFields()) {
            newline(1); // Indent fields at level 1
            visit(field);
        }

        // Add the static main method
        newline(0); // Empty line before main
        newline(1); // Indent main method at level 1
        print("public static void main(String[] args) {");
        newline(2); // Increase indentation for the method body
        print("System.exit(new Main().main());");
        newline(1); // Closing brace for main method
        print("}");

        // Generate methods
        for (Ast.Method method : ast.getMethods()) {
            newline(0); // Empty line between methods
            newline(1); // Indent methods at level 1
            visit(method);
        }

        // Generate the closing brace for the class
        newline(0);
        print("}");

        return null; // Void return
    }



//    @Override
//    public Void visit(Ast.Field ast) {
//        throw new UnsupportedOperationException(); //TODO
//    }

    @Override
    public Void visit(Ast.Field ast) {
        // Add indentation for fields
        newline(1);

        // Generate constant modifier if applicable
        if (ast.getConstant()) {
            print("final ");
        }

        // Generate type and name
        print(ast.getTypeName(), " ", ast.getName());

        // Generate value if present
        if (ast.getValue().isPresent()) {
            print(" = ");
            visit(ast.getValue().get());
        }

        // End the field declaration
        print(";");

        return null; // Void return
    }

//    @Override
//    public Void visit(Ast.Method ast) {
//        throw new UnsupportedOperationException(); //TODO
//    }
    @Override
    public Void visit(Ast.Method ast) {
        // Add a blank line before the method for readability
        newline(0);

        // Generate method signature
        print(ast.getReturnTypeName().orElse("void"), " ", ast.getName(), "(");

        // Generate parameters
        for (int i = 0; i < ast.getParameters().size(); i++) {
            if (i > 0) {
                print(", ");
            }
            print(ast.getParameterTypeNames().get(i), " ", ast.getParameters().get(i));
        }
        print(") {");

        // Handle method body
        if (!ast.getStatements().isEmpty()) {
            indent++; // Increase indentation for method body
            for (Ast.Statement statement : ast.getStatements()) {
                newline(indent);
                visit(statement);
            }
            indent--; // Restore original indentation
            newline(indent); // Prepare for closing brace
        }

        // Close method
        print("}");

        // Add a blank line after the method for readability
        newline(0);

        return null; // Void return
    }

//    @Override
//    public Void visit(Ast.Statement.Expression ast) {
//        throw new UnsupportedOperationException(); //TODO
//    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        // Generate the expression statement
        newline(indent); // Add a new line with the current indentation
        visit(ast.getExpression()); // Visit the expression part of the statement
        print(";"); // Append the semicolon
        return null; // Void return
    }


//    @Override
//    public Void visit(Ast.Statement.Declaration ast) {
//        throw new UnsupportedOperationException(); //TODO
//    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        // Add a new line with the current indentation
        newline(indent);

        // Print the type and name of the variable
        print(ast.getVariable().getType().getJvmName(), " ", ast.getVariable().getJvmName());

        // Check for an initialization value
        if (ast.getValue().isPresent()) {
            print(" = "); // Add the equals sign with spaces
            visit(ast.getValue().get()); // Generate the value expression
        }

        // Append the semicolon at the end
        print(";");

        return null; // Void return
    }

//    @Override
//    public Void visit(Ast.Statement.Assignment ast) {
//        throw new UnsupportedOperationException(); //TODO
//    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        // Add a new line with the current indentation
        newline(indent);

        // Generate the left-hand side (receiver)
        visit(ast.getReceiver());

        // Add the assignment operator
        print(" = ");

        // Generate the right-hand side (value)
        visit(ast.getValue());

        // Append the semicolon
        print(";");

        return null; // Void return
    }

//    @Override
//    public Void visit(Ast.Statement.If ast) {
//        throw new UnsupportedOperationException(); //TODO
//    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        // Start with the `if` keyword and the condition
        newline(indent);
        print("if (");
        visit(ast.getCondition());
        print(") {");

        // Generate the "then" block
        indent++;
        for (Ast.Statement statement : ast.getThenStatements()) {
            newline(indent);
            visit(statement);
        }
        indent--;

        // Close the "then" block
        newline(indent);
        print("}");

        // Handle the "else" block if present
        if (!ast.getElseStatements().isEmpty()) {
            newline(indent);
            print("else {");

            indent++;
            for (Ast.Statement statement : ast.getElseStatements()) {
                newline(indent);
                visit(statement);
            }
            indent--;

            // Close the "else" block
            newline(indent);
            print("}");
        }

        return null; // Void return
    }
//
//    @Override
//    public Void visit(Ast.Statement.For ast) {
//        throw new UnsupportedOperationException(); //TODO
//    }

    @Override
    public Void visit(Ast.Statement.For ast) {
        // Start the for loop signature
        newline(indent);
        print("for (");

        // Generate initialization (optional)
        if (ast.getInitialization() != null) {
            visit(ast.getInitialization());
        }
        print("; ");

        // Generate condition (optional)
        if (ast.getCondition() != null) {
            visit(ast.getCondition());
        }
        print("; ");

        // Generate increment (optional)
        if (ast.getIncrement() != null) {
            visit(ast.getIncrement());
        }
        print(") {");

        // Generate the loop body
        indent++;
        for (Ast.Statement statement : ast.getStatements()) {
            newline(indent);
            visit(statement);
        }
        indent--;

        // Close the loop
        newline(indent);
        print("}");

        return null; // Void return
    }


//    @Override
//    public Void visit(Ast.Statement.While ast) {
//        throw new UnsupportedOperationException(); //TODO
//    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        // Start the while loop with condition
        newline(indent);
        print("while (");
        visit(ast.getCondition());
        print(") {");

        // Generate the loop body
        indent++;
        for (Ast.Statement statement : ast.getStatements()) {
            newline(indent);
            visit(statement);
        }
        indent--;

        // Close the loop
        newline(indent);
        print("}");

        return null; // Void return
    }

//    @Override
//    public Void visit(Ast.Statement.Return ast) {
//        throw new UnsupportedOperationException(); //TODO
//    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        // Generate the return statement
        newline(indent);
        print("return ");
        visit(ast.getValue());
        print(";");

        return null; // Void return
    }

//    @Override
//    public Void visit(Ast.Expression.Literal ast) {
//        throw new UnsupportedOperationException(); //TODO
//    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        Object literal = ast.getLiteral();

        if (literal == null) {
            // Null literal
            print("null");
        } else if (literal instanceof Boolean) {
            // Boolean literal (true or false)
            print(literal.toString());
        } else if (literal instanceof BigInteger || literal instanceof BigDecimal) {
            // Integer or Decimal literal
            print(literal.toString());
        } else if (literal instanceof String) {
            // String literal (enclosed in double quotes)
            print("\"", literal.toString(), "\"");
        } else if (literal instanceof Character) {
            // Character literal (enclosed in single quotes)
            print("'", literal.toString(), "'");
        } else {
            throw new UnsupportedOperationException("Unsupported literal type: " + literal.getClass().getName());
        }

        return null; // Void return
    }


//    @Override
//    public Void visit(Ast.Expression.Group ast) {
//        throw new UnsupportedOperationException(); //TODO
//    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        // Print opening parenthesis
        print("(");

        // Generate the enclosed expression
        visit(ast.getExpression());

        // Print closing parenthesis
        print(")");

        return null; // Void return
    }

//    @Override
//    public Void visit(Ast.Expression.Binary ast) {
//        throw new UnsupportedOperationException(); //TODO
//    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        // Generate the left-hand expression
        visit(ast.getLeft());

        // Print the operator with spaces around it
        print(" ", ast.getOperator(), " ");

        // Generate the right-hand expression
        visit(ast.getRight());

        return null; // Void return
    }

//    @Override
//    public Void visit(Ast.Expression.Access ast) {
//        throw new UnsupportedOperationException(); //TODO
//    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        // Check if the expression has a receiver
        if (ast.getReceiver().isPresent()) {
            // Visit the receiver and add a dot (.)
            visit(ast.getReceiver().get());
            print(".");
        }

        // Print the name of the accessed field or method
        print(ast.getName());

        return null; // Void return
    }

//    @Override
//    public Void visit(Ast.Expression.Function ast) {
//        throw new UnsupportedOperationException(); //TODO
//    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        // Check if the expression has a receiver
        if (ast.getReceiver().isPresent()) {
            // Visit the receiver and add a dot (.)
            visit(ast.getReceiver().get());
            print(".");
        }

        // Print the function name
        print(ast.getName(), "(");

        // Generate and print the arguments
        for (int i = 0; i < ast.getArguments().size(); i++) {
            visit(ast.getArguments().get(i));
            if (i < ast.getArguments().size() - 1) {
                print(", ");
            }
        }

        // Close the parentheses
        print(")");

        return null; // Void return
    }

}
