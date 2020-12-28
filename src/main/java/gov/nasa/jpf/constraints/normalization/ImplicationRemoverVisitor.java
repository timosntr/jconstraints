package gov.nasa.jpf.constraints.normalization;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.LetExpression;
import gov.nasa.jpf.constraints.expressions.LogicalOperator;
import gov.nasa.jpf.constraints.expressions.Negation;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.constraints.util.DuplicatingVisitor;

public class ImplicationRemoverVisitor extends
        DuplicatingVisitor<Void> {

    private static final ImplicationRemoverVisitor INSTANCE = new ImplicationRemoverVisitor();

    public static ImplicationRemoverVisitor getInstance(){
        return INSTANCE;
    }

    @Override
    public Expression<?> visit(PropositionalCompound expression, Void data) {
        Expression<?> left = expression.getLeft();
        Expression<?> right = expression.getRight();
        LogicalOperator operator = expression.getOperator();

        if(operator.equals(LogicalOperator.IMPLY)){
            Expression<Boolean> partLeft = Negation.create((Expression<Boolean>) left);
            Expression<Boolean> result = PropositionalCompound.create(
                    (Expression<Boolean>) visit(partLeft, data),
                    LogicalOperator.OR,
                    visit(right, data));

            return result;
        }
        Expression visitedExpr = PropositionalCompound.create(
                (Expression<Boolean>) visit(left, data),
                operator,
                visit(right, data));

        return visitedExpr;
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
