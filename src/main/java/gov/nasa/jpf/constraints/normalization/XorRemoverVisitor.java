package gov.nasa.jpf.constraints.normalization;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.LogicalOperator;
import gov.nasa.jpf.constraints.expressions.Negation;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.constraints.util.DuplicatingVisitor;

public class XorRemoverVisitor extends
        DuplicatingVisitor<Void> {

    private static final XorRemoverVisitor INSTANCE = new XorRemoverVisitor();

    public static XorRemoverVisitor getInstance(){
        return INSTANCE;
    }

    @Override
    public Expression<?> visit(PropositionalCompound expression, Void data) {
        Expression<?> left = expression.getLeft();
        Expression<?> right = expression.getRight();
        LogicalOperator operator = expression.getOperator();

        if(operator.equals(LogicalOperator.XOR)){
            Expression<Boolean> partLeft = PropositionalCompound.create((Expression<Boolean>) left, LogicalOperator.OR, right);
            Expression<Boolean> partRight = PropositionalCompound.create(Negation.create((Expression<Boolean>) left), LogicalOperator.OR, Negation.create((Expression<Boolean>) right));
            Expression<Boolean> result = PropositionalCompound.create(partLeft, LogicalOperator.AND, partRight);

            return visit(result, data);
        }

        return super.defaultVisit(expression, data);
    }
}
