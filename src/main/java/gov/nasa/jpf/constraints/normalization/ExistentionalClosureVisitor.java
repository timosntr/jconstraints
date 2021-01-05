package gov.nasa.jpf.constraints.normalization;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.Quantifier;
import gov.nasa.jpf.constraints.expressions.QuantifierExpression;
import gov.nasa.jpf.constraints.util.DuplicatingVisitor;

//Existentional closure for free Variables after MiniScoping and before Renaming
//has to be done before Skolemization
public class ExistentionalClosureVisitor extends
        DuplicatingVisitor<Void> {

    private static final ExistentionalClosureVisitor INSTANCE = new ExistentionalClosureVisitor();

    public static ExistentionalClosureVisitor getInstance(){
        return INSTANCE;
    }

    @Override
    public Expression<?> visit(QuantifierExpression q, Void data) {
        //ToDo
        Quantifier quantifier = q.getQuantifier();
        Expression body = q.getBody();

        if(quantifier.equals(Quantifier.EXISTS)){
            throw new UnsupportedOperationException("Unhandled EXISTS found, skolemize first!");
        }

        return visit(body, data);
    }

    public <T> Expression<T> apply(Expression<T> expr, Void data) {
        return visit(expr, data).requireAs(expr.getType());
    }
}