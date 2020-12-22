package gov.nasa.jpf.constraints.normalization;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.*;
import gov.nasa.jpf.constraints.util.DuplicatingVisitor;

public class LetExpressionRemoverVisitor extends
        DuplicatingVisitor<Void> {

    private static final LetExpressionRemoverVisitor INSTANCE = new LetExpressionRemoverVisitor();

    public static LetExpressionRemoverVisitor getInstance(){
        return INSTANCE;
    }

    @Override
    public Expression<?> visit(LetExpression expr, Void data) {

        Expression flattened = expr.flattenLetExpression();
        Expression result = visit(flattened, data);
        return result;
    }
}
