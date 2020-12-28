package gov.nasa.jpf.constraints.normalization;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.*;
import gov.nasa.jpf.constraints.util.DuplicatingVisitor;

public class IfThenElseRemoverVisitor extends
        DuplicatingVisitor<Void> {

    private static final IfThenElseRemoverVisitor INSTANCE = new IfThenElseRemoverVisitor();

    public static IfThenElseRemoverVisitor getInstance(){
        return INSTANCE;
    }

    @Override
    public Expression<?> visit(IfThenElse expr, Void data) {
        Expression ifCond = expr.getIf();
        Expression thenExpr = expr.getThen();
        Expression elseExpr = expr.getElse();

        Expression firstPart = PropositionalCompound.create(Negation.create(ifCond), LogicalOperator.OR, thenExpr);
        Expression secondPart = PropositionalCompound.create(ifCond, LogicalOperator.OR, elseExpr);

        //visit again for finding nested IfThenElse
        Expression result = PropositionalCompound.create(
                (Expression<Boolean>) visit(firstPart, data),
                LogicalOperator.AND,
                visit(secondPart, data));

        return result;
    }

    @Override
    //Not needed if LetExpressionRemover is used beforehand
    public Expression<?> visit(LetExpression let, Void data) {
        return super.visit(let.flattenLetExpression(), data);
    }

    public <T> Expression<T> apply(Expression<T> expr, Void data) {
        return visit(expr, data).requireAs(expr.getType());
    }
}
