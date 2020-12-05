package gov.nasa.jpf.constraints.normalization;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.*;
import gov.nasa.jpf.constraints.expressions.functions.FunctionExpression;
import gov.nasa.jpf.constraints.util.DuplicatingVisitor;
import org.apache.commons.math3.analysis.function.Exp;

//should be used after removal of equivalences, implications, ifThenElse and XOR
//negations should be handled ahead of ConjunctionCreator
public class ConjunctionCreatorVisitor extends
        DuplicatingVisitor<Void> {

    private static final ConjunctionCreatorVisitor INSTANCE = new ConjunctionCreatorVisitor();

    public static ConjunctionCreatorVisitor getInstance(){
        return INSTANCE;
    }

    @Override
    public Expression<?> visit(PropositionalCompound expr, Void data) {
        Expression leftChild = (Expression) visit(expr.getLeft());
        Expression rightChild = (Expression) visit(expr.getRight());
        LogicalOperator operator = expr.getOperator();

        boolean operatorIsOR = operator.equals(LogicalOperator.OR);
        boolean operatorIsAND = operator.equals(LogicalOperator.AND);
        boolean leftIsPropComp = leftChild instanceof PropositionalCompound;
        boolean rightIsPropComp = rightChild instanceof PropositionalCompound;

        if (leftIsPropComp && rightIsPropComp) {
            boolean leftOpIsOR = ((PropositionalCompound) leftChild).getOperator().equals(LogicalOperator.OR);
            boolean leftOpIsAND = ((PropositionalCompound) leftChild).getOperator().equals(LogicalOperator.AND);
            boolean rightOpIsOR = ((PropositionalCompound) rightChild).getOperator().equals(LogicalOperator.OR);
            boolean rightOpIsAND = ((PropositionalCompound) rightChild).getOperator().equals(LogicalOperator.AND);

            Expression leftLeft = ((PropositionalCompound) leftChild).getLeft();
            Expression leftRight = ((PropositionalCompound) leftChild).getRight();
            Expression rightLeft = ((PropositionalCompound) rightChild).getLeft();
            Expression rightRight = ((PropositionalCompound) rightChild).getRight();

            if (operatorIsOR && leftOpIsAND && rightOpIsAND) {
                //case: (A AND B) OR (C AND D)
                Expression result = PropositionalCompound.create(
                        PropositionalCompound.create(
                                PropositionalCompound.create(
                                        leftLeft,
                                        LogicalOperator.OR,
                                        rightLeft),
                                LogicalOperator.AND,
                                PropositionalCompound.create(
                                        leftLeft,
                                        LogicalOperator.OR,
                                        rightRight)),
                        LogicalOperator.AND,
                        PropositionalCompound.create(
                                PropositionalCompound.create(
                                        leftRight,
                                        LogicalOperator.OR,
                                        rightLeft),
                                LogicalOperator.AND,
                                PropositionalCompound.create(
                                        leftRight,
                                        LogicalOperator.OR,
                                        rightRight)));
                return result;

            } else if (operatorIsOR && leftOpIsAND && rightOpIsOR) {
                //case: (A AND B) OR (C OR D)
                Expression result = PropositionalCompound.create(
                        PropositionalCompound.create(
                                leftLeft,
                                LogicalOperator.OR,
                                rightChild),
                        LogicalOperator.AND,
                        PropositionalCompound.create(
                                leftRight,
                                LogicalOperator.OR,
                                rightChild));
                return result;

            } else if (operatorIsOR && leftOpIsOR && rightOpIsAND) {
                //case: (A OR B) OR (C AND D)
                Expression result = PropositionalCompound.create(
                        PropositionalCompound.create(
                                leftChild,
                                LogicalOperator.OR,
                                rightLeft),
                        LogicalOperator.AND,
                        PropositionalCompound.create(
                                leftChild,
                                LogicalOperator.OR,
                                rightRight));
                return result;

            } else if (operatorIsOR && leftOpIsOR && !rightOpIsAND) {
                //case: (A OR B) OR (C OR D)
                return expr;
            }

        } else if (leftIsPropComp && !rightIsPropComp) {
            boolean leftOpIsOR = ((PropositionalCompound) leftChild).getOperator().equals(LogicalOperator.OR);
            boolean leftOpIsAND = ((PropositionalCompound) leftChild).getOperator().equals(LogicalOperator.AND);

            Expression leftLeft = ((PropositionalCompound) leftChild).getLeft();
            Expression leftRight = ((PropositionalCompound) leftChild).getRight();

            if (operatorIsOR && leftOpIsAND) {
                //case: (A AND B) OR (C)
                Expression result = PropositionalCompound.create(
                        PropositionalCompound.create(
                                leftLeft,
                                LogicalOperator.OR,
                                rightChild),
                        LogicalOperator.AND,
                        PropositionalCompound.create(
                                leftRight,
                                LogicalOperator.OR,
                                rightChild));
                return result;

            } else if (operatorIsOR && leftOpIsOR) {
                //case: (A OR B) OR (C)
                return expr;
            }
        } else if (!leftIsPropComp && rightIsPropComp) {
            boolean rightOpIsOR = ((PropositionalCompound) rightChild).getOperator().equals(LogicalOperator.OR);
            boolean rightOpIsAND = ((PropositionalCompound) rightChild).getOperator().equals(LogicalOperator.AND);

            Expression rightLeft = ((PropositionalCompound) rightChild).getLeft();
            Expression rightRight = ((PropositionalCompound) rightChild).getRight();

            if (operatorIsOR && !leftIsPropComp && rightIsPropComp && rightOpIsAND) {
                //case: (A) OR (C AND D)
                Expression result = PropositionalCompound.create(
                        PropositionalCompound.create(
                                leftChild,
                                LogicalOperator.OR,
                                rightLeft),
                        LogicalOperator.AND,
                        PropositionalCompound.create(
                                leftChild,
                                LogicalOperator.OR,
                                rightRight));
                return result;

            } else if (operatorIsOR && !leftIsPropComp && rightIsPropComp && rightOpIsOR) {
                return expr;
            }

        } else if (!leftIsPropComp && !rightIsPropComp) {

            if (operatorIsOR || operatorIsAND) {
                return expr;
            }
        } else {
            throw new UnsupportedOperationException("Remove equivalences, implications, ifThenElse, and XOR, and handle negations first.");
        }
        return expr;
    }
}
