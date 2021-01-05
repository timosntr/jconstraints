package gov.nasa.jpf.constraints.normalization;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.Quantifier;
import gov.nasa.jpf.constraints.expressions.QuantifierExpression;
import gov.nasa.jpf.constraints.util.DuplicatingVisitor;

import java.util.ArrayList;
import java.util.List;

//Removing of Quantifier.FORALL after Skolemization
//Quantifiers have to be handled ahead of ConjunctionCreator
public class ForallRemoverVisitor extends
        DuplicatingVisitor<Void> {

    private static final ForallRemoverVisitor INSTANCE = new ForallRemoverVisitor();

    public static ForallRemoverVisitor getInstance(){
        return INSTANCE;
    }

    @Override
    public Expression<?> visit(QuantifierExpression q, Void data) {

        Quantifier quantifier = q.getQuantifier();
        Expression body = q.getBody();

        if(quantifier.equals(Quantifier.EXISTS)){
            throw new UnsupportedOperationException("Unhandled EXISTS found, skolemize first!");
        }

        return visit(body, data);
    }

    @Override
    public Expression<?> visit(NumericBooleanExpression n, Void data) {
        return n;
    }

    public <T> Expression<T> apply(Expression<T> expr, Void data) {
        return visit(expr, data).requireAs(expr.getType());
    }
}