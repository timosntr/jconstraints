package gov.nasa.jpf.constraints.normalization;

import gov.nasa.jpf.constraints.api.Expression;
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
            Expression<Boolean> result = PropositionalCompound.create(partLeft, LogicalOperator.OR, right);

            return visit(result, data);
        }

        return super.defaultVisit(expression, data);
    }
}
