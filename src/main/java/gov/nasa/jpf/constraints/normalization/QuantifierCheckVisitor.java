package gov.nasa.jpf.constraints.normalization;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.AbstractExpressionVisitor;
import gov.nasa.jpf.constraints.expressions.QuantifierExpression;


public class QuantifierCheckVisitor extends AbstractExpressionVisitor {

    private static final QuantifierCheckVisitor INSTANCE = new QuantifierCheckVisitor();

    public static QuantifierCheckVisitor getInstance(){
        return INSTANCE;
    }

    @Override
    public Object visit(QuantifierExpression q, Object data) {
        return true;
    }

    @Override
    protected Object defaultVisit(Expression expression, Object data) {
        Expression<?>[] exprChildren = expression.getChildren();
        for(Expression i : exprChildren){
            return visit(i, null);
        }
        return false;
    }

}
