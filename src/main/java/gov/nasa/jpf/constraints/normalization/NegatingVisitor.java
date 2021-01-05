package gov.nasa.jpf.constraints.normalization;


import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.*;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.expressions.functions.FunctionExpression;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.DuplicatingVisitor;
import gov.nasa.jpf.constraints.util.ExpressionUtil;


import java.util.Collection;
import java.util.List;
import java.util.Map;


public class NegatingVisitor extends
        DuplicatingVisitor<Boolean> {

    private static final NegatingVisitor INSTANCE = new NegatingVisitor();

    public static NegatingVisitor getInstance(){
        return INSTANCE;
    }

    @Override
    public Expression<?> visit(NumericBooleanExpression expr, Boolean shouldNegate){

        if(shouldNegate){
            Expression<?> left = expr.getLeft();
            Expression<?> right = expr.getRight();
            NumericComparator comparator = expr.getComparator();
            return NumericBooleanExpression.create(left, comparator.not(), right);
        }

        return expr;
    }


    @Override
    public Expression<?> visit(PropositionalCompound expr, Boolean shouldNegate) {
        //if shouldNegate is true, a Negation is visited
        if (shouldNegate) {
            Expression<Boolean> left = expr.getLeft();
            Expression<Boolean> right = expr.getRight();
            LogicalOperator operator = expr.getOperator();
            LogicalOperator negOperator = operator.invert();
            Expression<Boolean> newLeft = left;
            Expression<Boolean> newRight = right;

            //if an equivalence or an xor is negated, only the operator is inverted
            //otherwise not only the operator has to be inverted but also the children have to bei negated
            if(operator.equals(LogicalOperator.EQUIV) || operator.equals(LogicalOperator.XOR)){
                newLeft = (Expression<Boolean>) visit(left, false);
            } else {
                if(left.getType().equals(BuiltinTypes.BOOL)) {
                    newLeft = (Expression<Boolean>) visit(Negation.create(left), false);
                } else {
                    newLeft = (Expression<Boolean>) visit(left, false);
                }
            }
            if(operator.equals(LogicalOperator.EQUIV) || operator.equals(LogicalOperator.XOR)) {
                newRight = (Expression<Boolean>) visit(right, false);
            } else {
                if(right.getType().equals(BuiltinTypes.BOOL)) {
                    newRight = (Expression<Boolean>) visit(Negation.create(right), false);
                } else {
                    newRight = (Expression<Boolean>) visit(right, false);
                }
            }

            return PropositionalCompound.create(newLeft, negOperator, newRight);

        } else {
            Expression<Boolean> left = (Expression<Boolean>) visit(expr.getLeft(), false);
            Expression<Boolean> right = (Expression<Boolean>) visit(expr.getRight(), false);
            LogicalOperator operator = expr.getOperator();
            return PropositionalCompound.create(left, operator, right);
        }
    }

    @Override
    public Expression<?> visit(Negation expr, Boolean shouldNegate){

        if(shouldNegate){
            //negation of a negation
            if(expr.getType().equals(BuiltinTypes.BOOL)){
                return visit(expr.getNegated(), false);
            } else {
                //Todo: not sure about this part;
                // should be unnecessary as a Negation should always be boolean typed
                return expr;
            }
        } else {
            if (expr.getType().equals(BuiltinTypes.BOOL)) {
                return visit(expr.getNegated(), true);
            } else {
                //Todo: not sure about this part;
                // should be unnecessary as a Negation should always be boolean typed
                return expr;
            }
        }
    }

    @Override
    public Expression visit(Variable var, Boolean shouldNegate){

        if(shouldNegate){
            if (var.getType().equals(BuiltinTypes.BOOL)) {
                Negation negated = Negation.create(var);
                return negated;
            }
        }
        return var;
    }

    public Expression<?> visit(QuantifierExpression quantified, Boolean shouldNegate) {
        Quantifier q = quantified.getQuantifier();
        List<? extends Variable<?>> vars = quantified.getBoundVariables();
        Expression<Boolean> body = quantified.getBody();

        if(shouldNegate){
            QuantifierExpression qExpr = QuantifierExpression.create(q.negate(), vars, (Expression<Boolean>) visit(Negation.create(body), false));
            return qExpr;
        }
        Expression expr = QuantifierExpression.create(q, vars, (Expression<Boolean>) visit(body, false));
        return expr;
    }

    @Override
    public Expression<?> visit(StringBooleanExpression expr, Boolean shouldNegate) {

        if(shouldNegate) {
            StringBooleanOperator operator = expr.getOperator();
            Expression<?> left = expr.getLeft();
            Expression<?> right = expr.getRight();

            if (operator.equals(StringBooleanOperator.EQUALS)){
                return StringBooleanExpression.createNotEquals(left, right);
            } else if (operator.equals(StringBooleanOperator.NOTEQUALS)){
                return StringBooleanExpression.createEquals(left, right);
            } else {
                //other negations of operators not implemented
                return Negation.create(expr);
            }
        }
        return expr;
    }

    //should be unnecessary after the IfThenElseRemover
    @Override
    public <E> Expression<?> visit(IfThenElse<E> expr, Boolean shouldNegate) {

        Expression result = expr.flattenIfThenElse();

        if(shouldNegate){
            if(result.getType().equals(BuiltinTypes.BOOL)){
                return visit(Negation.create(result), false);
            } else {
                //ToDo: not sure if this is the right solution for IfThenElse with
                // non-boolean typed children
                Expression newIte = IfThenElse.create(expr.getIf(), Negation.create((Expression<Boolean>) expr.getThen()), Negation.create((Expression<Boolean>) expr.getElse()));
                return visit(newIte, false);
            }
        }
        return visit(expr, false);
    }

    @Override
    public <E> Expression<?> visit(FunctionExpression<E> expr, Boolean shouldNegate) {

        //FunctionExpressions are not further negated
        if(shouldNegate){
            return Negation.create((Expression<Boolean>) expr);
        }
        return expr;
    }

    @Override
    public Expression<?> visit(RegExBooleanExpression expr, Boolean shouldNegate) {
        if(shouldNegate){
            return Negation.create(expr);
        }
        return expr;
    }

    @Override
    public <E> Expression<?> visit(Constant<E> c, Boolean shouldNegate) {
        if(shouldNegate){
            if (c.getType().equals(BuiltinTypes.BOOL)) {
                return Negation.create((Expression<Boolean>) c);
            }
        }
        return c;
    }

    @Override
    public <E> Expression<?> visit(UnaryMinus<E> expr, Boolean shouldNegate) {
        if(shouldNegate){
            if(expr.getType().equals(BuiltinTypes.BOOL)){
                return expr.getNegated();
            }
        }
        return expr;
    }

    @Override
    public <E> Expression<?> visit(BitvectorExpression<E> expr, Boolean shouldNegate) {
        if(shouldNegate){
            Expression left = expr.getLeft();
            Expression right = expr.getRight();
            BitvectorOperator operator = expr.getOperator();

            if(operator.equals(BitvectorOperator.AND)){
                return BitvectorExpression.create(left, BitvectorOperator.OR, right);
            } else if(operator.equals(BitvectorOperator.OR)){
                return BitvectorExpression.create(left, BitvectorOperator.AND, right);
            }
            return Negation.create((Expression<Boolean>) expr);
        }
        return expr;
    }

    @Override
    //should be unnecessary after the LetExpressionRemover
    public Expression<?> visit(LetExpression expr, Boolean shouldNegate) {
        Expression flattened = expr.flattenLetExpression();
        if(shouldNegate){
            if(flattened.getType().equals(BuiltinTypes.BOOL)){
                return visit(Negation.create(flattened), false);
            }
        }
        return visit(flattened, false);
    }

    //defaultVisit for CastExpression, NumericCompound, StringIntegerExpression,
    //StringCompoundExpression, RegexCompoundExpression, RegexOperatorExpression,
    //RegExBooleanExpression, BitvectorNegation
    @Override
    protected <E> Expression<?> defaultVisit(Expression<E> expression, Boolean shouldNegate) {
        return super.defaultVisit(expression, shouldNegate);
    }

    public <T> Expression<T> apply(Expression<T> expr, Boolean shouldNegate) {
        return visit(expr, shouldNegate).requireAs(expr.getType());
    }
}
