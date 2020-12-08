package gov.nasa.jpf.constraints.normalization;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.*;
import gov.nasa.jpf.constraints.expressions.functions.FunctionExpression;
import gov.nasa.jpf.constraints.util.DuplicatingVisitor;

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
                                PropositionalCompound.create(leftLeft, LogicalOperator.OR, rightLeft),
                                LogicalOperator.AND,
                                PropositionalCompound.create(leftLeft, LogicalOperator.OR, rightRight)),
                        LogicalOperator.AND,
                        PropositionalCompound.create(
                                PropositionalCompound.create(leftRight, LogicalOperator.OR, rightLeft),
                                LogicalOperator.AND,
                                PropositionalCompound.create(leftRight, LogicalOperator.OR, rightRight)));
                return result;

            } else if (operatorIsOR && leftOpIsAND && rightOpIsOR) {
                //case: (A AND B) OR (C OR D)
                Expression result = PropositionalCompound.create(
                        PropositionalCompound.create(leftLeft, LogicalOperator.OR, rightChild),
                        LogicalOperator.AND,
                        PropositionalCompound.create(leftRight, LogicalOperator.OR, rightChild));
                return result;

            } else if (operatorIsOR && leftOpIsOR && rightOpIsAND) {
                //case: (A OR B) OR (C AND D)
                Expression result = PropositionalCompound.create(
                        PropositionalCompound.create(leftChild, LogicalOperator.OR, rightLeft),
                        LogicalOperator.AND,
                        PropositionalCompound.create(leftChild, LogicalOperator.OR, rightRight));
                return result;

            } else if (operatorIsOR && leftOpIsOR && !rightOpIsAND) {
                //case: (A OR B) OR (C OR D)
                /*Expression result = PropositionalCompound.create(leftChild, LogicalOperator.OR, rightChild);
                return result;*/
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
                        PropositionalCompound.create(leftLeft, LogicalOperator.OR, rightChild),
                        LogicalOperator.AND,
                        PropositionalCompound.create(leftRight, LogicalOperator.OR, rightChild));
                return result;

            } else if (operatorIsOR && leftOpIsOR) {
                //case: (A OR B) OR (C)
                /*Expression result = PropositionalCompound.create(leftChild, LogicalOperator.OR, rightChild);
                return result;*/
                return expr;
            }
        } else if (!leftIsPropComp && rightIsPropComp) {
            boolean rightOpIsOR = ((PropositionalCompound) rightChild).getOperator().equals(LogicalOperator.OR);
            boolean rightOpIsAND = ((PropositionalCompound) rightChild).getOperator().equals(LogicalOperator.AND);

            Expression rightLeft = ((PropositionalCompound) rightChild).getLeft();
            Expression rightRight = ((PropositionalCompound) rightChild).getRight();

            if (operatorIsOR && rightOpIsAND) {
                //case: (A) OR (C AND D)
                Expression result = PropositionalCompound.create(
                        PropositionalCompound.create(leftChild, LogicalOperator.OR, rightLeft),
                        LogicalOperator.AND,
                        PropositionalCompound.create(leftChild, LogicalOperator.OR, rightRight));
                return result;

            } else if (operatorIsOR && rightOpIsOR) {
                //case: (A) OR (C OR D)
                /*Expression result = PropositionalCompound.create(leftChild, LogicalOperator.OR, rightChild);
                return result;*/
                return expr;
            }

        } else if (!leftIsPropComp && !rightIsPropComp) {
            //cases: (A) OR (B); (A) AND (B)
            if (operatorIsOR || operatorIsAND) {
                return expr;
            }
        } else {
            throw new UnsupportedOperationException("Remove equivalences, implications, ifThenElse, and XOR, and handle negations first.");
        }
        //if we are here, there are no conjunctions under disjunctions in the tree (anymore)
        Expression result = PropositionalCompound.create(leftChild, operator, rightChild);
        return result;
    }

    @Override
    public Expression<?> visit(QuantifierExpression q, Void data) {
        return super.visit(q, data);
    }

    @Override
    public <E> Expression<?> visit(FunctionExpression<E> f, Void data) {
        return super.visit(f, data);
    }

    @Override
    public <E> Expression<?> visit(Variable<E> v, Void data) {
        return v;
    }

    @Override
    public <E> Expression<?> visit(Constant<E> c, Void data) {
        return c;
    }

    @Override
    public Expression<?> visit(Negation n, Void data) {
        return n;
    }

    @Override
    public Expression<?> visit(NumericBooleanExpression n, Void data) {
        return n;
    }

    @Override
    public Expression<?> visit(RegExBooleanExpression n, Void data) {
        return n;
    }

    @Override
    public Expression<?> visit(StringBooleanExpression n, Void data) {
        return n;
    }

    @Override
    public Expression<?> visit(StringIntegerExpression n, Void data) {
        return n;
    }

    @Override
    public Expression<?> visit(StringCompoundExpression n, Void data) {
        return n;
    }

    @Override
    public Expression<?> visit(RegexCompoundExpression n, Void data) {
        return n;
    }

    @Override
    public Expression<?> visit(RegexOperatorExpression n, Void data) {
        return n;
    }

    @Override
    public <F, E> Expression<?> visit(CastExpression<F, E> cast, Void data) {
        return cast;
    }

    @Override
    public <E> Expression<?> visit(NumericCompound<E> n, Void data) {
        return n;
    }

    @Override
    public <E> Expression<?> visit(UnaryMinus<E> n, Void data) {
        return n;
    }

    @Override
    public <E> Expression<?> visit(BitvectorExpression<E> bv, Void data) {
        return bv;
    }

    @Override
    public <E> Expression<?> visit(BitvectorNegation<E> n, Void data) {
        return n;
    }

}