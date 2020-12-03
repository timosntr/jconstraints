package gov.nasa.jpf.constraints.normalization;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.LogicalOperator;
import gov.nasa.jpf.constraints.expressions.Negation;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.constraints.expressions.QuantifierExpression;
import gov.nasa.jpf.constraints.util.DuplicatingVisitor;

public class EquivalenceRemoverVisitor extends
        DuplicatingVisitor<Void> {

    private static final EquivalenceRemoverVisitor INSTANCE = new EquivalenceRemoverVisitor();

    public static EquivalenceRemoverVisitor getInstance(){
        return INSTANCE;
    }

    @Override
    public Expression<?> visit(PropositionalCompound expression, Void data) {
        Expression<?> left = expression.getLeft();
        Expression<?> right = expression.getRight();
        LogicalOperator operator = expression.getOperator();

        if(operator.equals(LogicalOperator.EQUIV)){
            Expression<Boolean> partLeft = PropositionalCompound.create(Negation.create((Expression<Boolean>) left), LogicalOperator.OR, right);
            Expression<Boolean> partRight = PropositionalCompound.create(Negation.create((Expression<Boolean>) right), LogicalOperator.OR, left);
            Expression<Boolean> result = PropositionalCompound.create(
                    (Expression<Boolean>) visit(partLeft, data),
                    LogicalOperator.AND,
                    visit(partRight, data));

            return result;
        }

        Expression visitedExpr = PropositionalCompound.create(
                (Expression<Boolean>) visit(left, data),
                operator,
                visit(right, data));

        return visitedExpr;
    }
}
