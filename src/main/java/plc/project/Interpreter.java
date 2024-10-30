package plc.project;
import javax.crypto.EncryptedPrivateKeyInfo;
import java.beans.Encoder;
import java.lang.reflect.Array;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.math.BigDecimal;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });

        scope.defineFunction("log", 1, args->{
            if (!(args.get(0).getValue() instanceof  BigDecimal)) {
                throw new RuntimeException("The BigDecimal Type was expected" +
                args.get(0).getValue().getClass().getName()+".");
            }
            BigDecimal firstBigDecimal = (BigDecimal) args.get(0).getValue();

            BigDecimal secondBigDecimal = requireType(BigDecimal.class, Environment.create(args.get(0).getValue()));

            BigDecimal bigDecimalResult = BigDecimal.valueOf(Math.log(secondBigDecimal.doubleValue()));

            return Environment.create(bigDecimalResult);

        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        for (int i = 0; i<ast.getFields().size();i++){
            visit(ast.getFields().get(i));
        }

        for (int i=0; i<ast.getMethods().size(); i++){
            visit(ast.getMethods().get(i));
        }
        //mafu == prifun
        Environment.Function primaryFunction = scope.lookupFunction("Primary", 0);
        return primaryFunction.invoke(List.of());




        //        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Field ast) {

        if (ast.getValue().isPresent()){
            scope.defineVariable(ast.getName(), ast.getConstant(), visit(ast.getValue().get()));

        }
        else {
            scope.defineVariable(ast.getName(), ast.getConstant(), Environment.NIL);

        }
        return Environment.NIL;






//        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Method ast) {
        scope.defineFunction(ast.getName(), ast.getParameters().size(), args-> {

                    Scope scopeHold = scope;
                    scope = new Scope(scope);
                    try {

                        for (int i = 0; i < ast.getParameters().size(); i++) {
                            scope.defineVariable(ast.getParameters().get(i), true, args.get(i));
                        }
                        for (int i = 0; i < ast.getStatements().size(); i++) {
                            visit(ast.getStatements().get(i));
                        }
                    } catch (Return returnException) {
                        return returnException.value;
                    } finally {
                        scope = scopeHold;
                    }
                    return Environment.NIL;
                }
        );
        return Environment.NIL;

//        throw new UnsupportedOperationException(); //TODO
    }
// HEREHEREHERE

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        return Environment.NIL;
        // throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast) {
        if (ast.getValue().isPresent())
        {
            scope.defineVariable(ast.getName(), false, visit(ast.getValue().get()));
        }
        else
        {
            scope.defineVariable(ast.getName(), false, Environment.NIL);
        }
        return Environment.NIL;
        //throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {

        if (!(ast.getReceiver() instanceof  Ast.Expression.Access))
        {


            throw new RuntimeException("Ast.Expression.Access instance must be reciever");
        }

        Ast.Expression.Access recieveHold = (Ast.Expression.Access) ast.getReceiver();

        if (recieveHold.getReceiver().isPresent())
        {
            Environment.PlcObject objectPLC = visit(recieveHold.getReceiver().get());
            objectPLC.setField(recieveHold.getName(), visit(ast.getValue()));
        }
        else
        {


            Environment.Variable variableEnvironment = scope.lookupVariable(recieveHold.getName());
            variableEnvironment.setValue(visit(ast.getValue()));
        }

//        throw new UnsupportedOperationException(); //TODO
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        boolean holdCondition = requireType(Boolean.class, visit(ast.getCondition()));

        Scope holdScopeStore = scope;
        scope = new Scope(scope);

        try
        {
            if (holdCondition)
            {
                for (int i=0; i<ast.getThenStatements().size();i++)
                {
                    visit(ast.getThenStatements().get(i));
                }
            }

        }
        finally {
            scope = holdScopeStore;
        }
//        throw new UnsupportedOperationException(); //TODO

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.For ast) {

        Scope holdScopeStore = scope;

        scope = new Scope(scope);

        try
        {
            visit(ast.getInitialization());
            while(requireType(Boolean.class, visit(ast.getCondition())))
            {
                for (int i=0;i<ast.getStatements().size();i++)
                {
                    visit(ast.getStatements().get(i));
                }
                visit(ast.getIncrement());
            }
        }
        finally
        {
            scope = holdScopeStore;
        }

//        throw new UnsupportedOperationException(); //TODO
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {

        Scope holdScopeStore = scope;

        scope = new Scope(scope);

        try
        {
            while (requireType(Boolean.class, visit(ast.getCondition())))
            {
                for (int i=0;i<ast.getStatements().size();i++)
                {
                    visit(ast.getStatements().get(i));
                }
            }
        }
        finally {
            scope = holdScopeStore;
        }
        return Environment.NIL;
//        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {

        throw new Return(visit(ast.getValue()));
//        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        if(ast.getLiteral()==null)
        {
            return Environment.NIL;
        }

        return Environment.create(ast.getLiteral());

        //        throw new UnsupportedOperationException(); //TODO
    }
////// 2A03 HERE NOW
    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast) {
        return visit(ast.getExpression());


        //        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        Environment.PlcObject holdLeft = visit(ast.getLeft());

        if (ast.getOperator().equals("&&")) {
            requireType(Boolean.class, holdLeft);

            if (holdLeft.getValue().equals(Boolean.FALSE)) {
                return Environment.create(false);
            }

            Environment.PlcObject holdRight = visit(ast.getRight());

            requireType(Boolean.class, holdRight);

            if (holdRight.getValue().equals(Boolean.FALSE)) {
                return Environment.create(false
                );

            }
            return Environment.create(true);

        } else if (ast.getOperator().equals("||")) {
            requireType(Boolean.class, holdLeft);
            if (holdLeft.getValue().equals(Boolean.TRUE)) {
                return Environment.create(true);
            }

            Environment.PlcObject holdRight = visit(ast.getRight());

            requireType(Boolean.class, holdLeft);

            if (holdRight.getValue().equals(Boolean.FALSE)) {
                return Environment.create(true);
            }
            return Environment.create(false);
        }
        ////////23a09
        //        throw new UnsupportedOperationException(); //TODO

        Environment.PlcObject holdRight = visit(ast.getRight());

        if (ast.getOperator().equals("<")) {
            requireType(holdLeft.getValue().getClass(), holdRight);
            if (holdLeft.getValue() instanceof BigInteger) {
                BigInteger holdGVLeft = (BigInteger) holdLeft.getValue();

                BigInteger holdGVRight = (BigInteger) holdRight.getValue();

                return Environment.create(holdGVLeft.compareTo(holdGVRight) < 0);
            } else if (holdLeft.getValue() instanceof BigDecimal) {
                BigDecimal holdGVLeft = (BigDecimal) holdLeft.getValue();

                BigDecimal holdGVRight = (BigDecimal) holdRight.getValue();

                return Environment.create(holdGVLeft.compareTo(holdGVRight) < 0);
            } else if (holdLeft.getValue() instanceof Character) {
                Character holdGVLeft = (Character) holdLeft.getValue();

                Character holdGVRight = (Character) holdRight.getValue();

                return Environment.create(holdGVLeft.compareTo(holdGVRight) < 0);
            } else if (holdLeft.getValue() instanceof String) {
                String holdGVLeft = (String) holdLeft.getValue();

                String holdGVRight = (String) holdRight.getValue();

                return Environment.create(holdGVLeft.compareTo(holdGVRight) < 0);
            } else if (holdLeft.getValue() instanceof Boolean) {
                Boolean holdGVLeft = (Boolean) holdLeft.getValue();

                Boolean holdGVRight = (Boolean) holdRight.getValue();

                return Environment.create(holdGVLeft.compareTo(holdGVRight) < 0);
            } else {
                throw new RuntimeException("Cannot compare Nil to Nil");
            }
        }
        else if (ast.getOperator().equals(">")) {
            requireType(holdLeft.getValue().getClass(), holdRight);

            if (holdLeft.getValue() instanceof BigInteger) {
                BigInteger holdGVLeft = (BigInteger) holdLeft.getValue();

                BigInteger holdGVRight = (BigInteger) holdRight.getValue();

                return Environment.create(holdGVLeft.compareTo(holdGVRight) > 0);
            } else if (holdLeft.getValue() instanceof BigDecimal) {
                BigDecimal holdGVLeft = (BigDecimal) holdLeft.getValue();

                BigDecimal holdGVRight = (BigDecimal) holdRight.getValue();

                return Environment.create(holdGVLeft.compareTo(holdGVRight) > 0);
            } else if (holdLeft.getValue() instanceof Character) {
                Character holdGVLeft = (Character) holdLeft.getValue();

                Character holdGVRight = (Character) holdRight.getValue();

                return Environment.create(holdGVLeft.compareTo(holdGVRight) > 0);
            } else if (holdLeft.getValue() instanceof String) {
                String holdGVLeft = (String) holdLeft.getValue();

                String holdGVRight = (String) holdRight.getValue();

                return Environment.create(holdGVLeft.compareTo(holdGVRight) > 0);
            } else if (holdLeft.getValue() instanceof Boolean) {
                Boolean holdGVLeft = (Boolean) holdLeft.getValue();

                Boolean holdGVRight = (Boolean) holdRight.getValue();

                return Environment.create(holdGVLeft.compareTo(holdGVRight) > 0);
            } else {
                throw new RuntimeException("Cannot compare Nil to Nil");
            }
        }
        else if (ast.getOperator().equals("<="))
        {
            requireType(holdLeft.getValue().getClass(), holdRight);

            if (holdLeft.getValue() instanceof BigInteger)
            {
                BigInteger holdGVLeft = (BigInteger) holdLeft.getValue();

                BigInteger holdGVRight = (BigInteger) holdRight.getValue();

                return Environment.create(holdGVLeft.compareTo(holdGVRight)<=0);
            }
            else if (holdLeft.getValue() instanceof BigDecimal)
            {
                BigDecimal holdGVLeft = (BigDecimal) holdLeft.getValue();

                BigDecimal holdGVRight = (BigDecimal) holdRight.getValue();

                return Environment.create(holdGVLeft.compareTo(holdGVRight)<=0);
            }
            else if (holdLeft.getValue() instanceof Character)
            {
                Character holdGVLeft = (Character) holdLeft.getValue();

                Character holdGVRight = (Character) holdRight.getValue();

                return Environment.create(holdGVLeft.compareTo(holdGVRight)<=0);
            }
            else if (holdLeft.getValue() instanceof String)
            {
                String holdGVLeft = (String) holdLeft.getValue();

                String holdGVRight = (String) holdRight.getValue();

                return Environment.create(holdGVLeft.compareTo(holdGVRight)<=0);
            }
            else if (holdLeft.getValue() instanceof Boolean)
            {
                Boolean holdGVLeft = (Boolean) holdLeft.getValue();

                Boolean holdGVRight = (Boolean) holdRight.getValue();

                return Environment.create(holdGVLeft.compareTo(holdGVRight)<=0);
            }
            else
            {
                throw new RuntimeException("Cannot compare Nil to Nil");
            }
        }
        else if (ast.getOperator().equals(">="))
        {
            requireType(holdLeft.getValue().getClass(), holdRight);

            if (holdLeft.getValue() instanceof BigInteger)
            {
                BigInteger holdGVLeft = (BigInteger) holdLeft.getValue();

                BigInteger holdGVRight = (BigInteger) holdRight.getValue();

                return Environment.create(holdGVLeft.compareTo(holdGVRight)<=0);
            }
            else if (holdLeft.getValue() instanceof BigDecimal)
            {
                BigDecimal holdGVLeft = (BigDecimal) holdLeft.getValue();

                BigDecimal holdGVRight = (BigDecimal) holdRight.getValue();

                return Environment.create(holdGVLeft.compareTo(holdGVRight)<=0);
            }
            else if (holdLeft.getValue() instanceof Character)
            {
                Character holdGVLeft = (Character) holdLeft.getValue();

                Character holdGVRight = (Character) holdRight.getValue();

                return Environment.create(holdGVLeft.compareTo(holdGVRight)<=0);
            }
            else if (holdLeft.getValue() instanceof String)
            {
                String holdGVLeft = (String) holdLeft.getValue();

                String holdGVRight = (String) holdRight.getValue();

                return Environment.create(holdGVLeft.compareTo(holdGVRight)<=0);
            }
            else
            {
                throw new RuntimeException("Cannot compare Nil to Nil");
            }
        }
        else if (ast.getOperator().equals("=="))
        {
            return Environment.create(holdLeft.getValue().equals(holdRight.getValue()));
        }
        else if (ast.getOperator().equals("!="))
        {
            return Environment.create(!(holdLeft.getValue().equals(holdRight.getValue())));
        }
        else if (ast.getOperator().equals("+"))
        {
            if(holdLeft.getValue() instanceof String||holdRight.getValue() instanceof String)
            {
                return Environment.create(holdLeft.getValue().toString()+holdRight.getValue().toString());
            }
            else if (holdLeft.getValue() instanceof BigInteger)
            {
                requireType(BigInteger.class, holdRight);

                BigInteger holdGVLeft = (BigInteger) holdLeft.getValue();

                BigInteger holdGVRight = (BigInteger) holdRight.getValue();

                return Environment.create(holdGVLeft.add(holdGVRight));
            }
            else if (holdLeft.getValue() instanceof BigDecimal)
            {
                requireType(BigDecimal.class, holdRight);

                BigDecimal holdGVLeft = (BigDecimal) holdLeft.getValue();

                BigDecimal holdGVRight = (BigDecimal) holdRight.getValue();

                return Environment.create(holdGVLeft.add(holdGVRight));
            }
        }
        else if (ast.getOperator().equals("-"))
        {
            if(holdLeft.getValue() instanceof BigInteger)
            {
                requireType(BigInteger.class, holdRight);

                BigInteger holdGVLeft = (BigInteger) holdLeft.getValue();

                BigInteger holdGVRight = (BigInteger)  holdRight.getValue();

                return Environment.create(holdGVLeft.subtract(holdGVRight));
            }
            else if(holdLeft.getValue() instanceof BigDecimal)
            {
                requireType(BigDecimal.class, holdRight);

                BigDecimal holdGVLeft = (BigDecimal) holdLeft.getValue();

                BigDecimal holdGVRight = (BigDecimal)  holdRight.getValue();

                return Environment.create(holdGVLeft.subtract(holdGVRight));
            }
        }
        else if (ast.getOperator().equals("*"))
        {
            if(holdLeft.getValue() instanceof BigInteger)
            {
                requireType(BigInteger.class, holdRight);

                BigInteger holdGVLeft = (BigInteger) holdLeft.getValue();

                BigInteger holdGVRight = (BigInteger) holdRight.getValue();

                return Environment.create(holdGVLeft.multiply(holdGVRight));
            }
            else if (holdLeft.getValue() instanceof BigDecimal)
            {
                requireType(BigDecimal.class, holdRight);

                BigDecimal holdGVLeft = (BigDecimal) holdLeft.getValue();

                BigDecimal holdGVRight = (BigDecimal) holdRight.getValue();

                return Environment.create(holdGVLeft.multiply(holdGVRight));
            }
        }
        else if (ast.getOperator().equals("/"))
        {
            if(holdLeft.getValue() instanceof  BigInteger)
            {
                requireType(BigInteger.class, holdRight);

                BigInteger holdGVLeft = (BigInteger) holdLeft.getValue();

                BigInteger holdGVRight = (BigInteger) holdRight.getValue();

                if(holdGVRight.equals(BigInteger.ZERO))
                {
                    throw new RuntimeException("CANNOT divide by zero");
                }


                return Environment.create(holdGVLeft.divide(holdGVRight));

            } else if (holdLeft.getValue() instanceof BigDecimal)
            {
                requireType(BigDecimal.class, holdRight);

                BigDecimal holdGVLeft = (BigDecimal) holdLeft.getValue();

                BigDecimal holdGVRight = (BigDecimal) holdRight.getValue();

                if(holdGVRight.equals(BigDecimal.ZERO))
                {
                    throw new RuntimeException("CANNOT divide by zero");
                }
                return Environment.create(holdGVLeft.divide(holdGVRight, RoundingMode.HALF_EVEN));


            }
        }
        throw new RuntimeException("Binary argument is Invalid");
    }
    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {

        Environment.Variable hasBeenReached;

        if(ast.getReceiver().isPresent())
        {
            Environment.PlcObject plcObjHold = ast.getReceiver().map(this::visit).orElse(null);

            hasBeenReached = plcObjHold.getField(ast.getName());

            return hasBeenReached.getValue();
        }

        hasBeenReached = scope.lookupVariable(ast.getName());

        return hasBeenReached.getValue();
//        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
//        throw new UnsupportedOperationException(); //TODO

        Environment.Function funcEnviroHold;

        if (ast.getReceiver().isPresent())
        {
            Environment.PlcObject plcObjHold = ast.getReceiver().map(this::visit).orElse(null);

            ArrayList<Environment.PlcObject> param = new ArrayList<>();

            for (int i=0;i<ast.getArguments().size();i++)
            {
                param.add(visit(ast.getArguments().get(i)));
            }

            return plcObjHold.callMethod(ast.getName(), param);
        }

        funcEnviroHold = scope.lookupFunction(ast.getName(), ast.getArguments().size());

        ArrayList<Environment.PlcObject> param = new ArrayList<>();

        for (int i=0;i<ast.getArguments().size();i++)
        {
            param.add(visit(ast.getArguments().get(i)));
        }

        return funcEnviroHold.invoke(param);
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
