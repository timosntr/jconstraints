package gov.nasa.jpf.constraints.normalization;

import com.google.common.base.Function;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
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

    public <T> Expression<T> apply(Expression<T> expr, Void data) {
        return visit(expr, data).requireAs(expr.getType());
    }

}
