package gov.nasa.jpf.constraints.normalization;


import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.*;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.expressions.NumericCompound;
import gov.nasa.jpf.constraints.expressions.functions.FunctionExpression;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.DuplicatingVisitor;


import java.util.List;


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

    //ToDo: negation of NumericCompound should just be the same?
    @Override
    public Expression<?> visit(NumericCompound expr, Boolean shouldNegate){
       return expr;
    }

    @Override
    public Expression<?> visit(PropositionalCompound expr, Boolean shouldNegate) {
        //if shouldNegate is true, a Negation is visited
        if (shouldNegate) {
            Expression<Boolean> left = (Expression<Boolean>) visit(Negation.create(expr.getLeft()), false);
            Expression<Boolean> right = (Expression<Boolean>) visit(Negation.create(expr.getRight()), false);
            LogicalOperator operator = expr.getOperator();
            LogicalOperator negOperator = operator.invert();
            return PropositionalCompound.create(left, negOperator, right);

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
            return visit(expr.getNegated(), false);
        }
        return visit(expr.getNegated(), true);
    }

    @Override
    public Expression visit(Variable var, Boolean shouldNegate){

        if(shouldNegate){
            Negation negated = Negation.create(var);
            return negated;
        }
        return var;
    }

    @Override
    //ToDo: makes sense? how should other types of constants be negated?
    public <E> Expression<?> visit(Constant<E> c, Boolean shouldNegate) {

        if(shouldNegate){
            if (c.getType() instanceof BuiltinTypes.BoolType) {
                return Negation.create((Expression<Boolean>) c);
            }
        }
        return c;
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

    //ToDo
    @Override
    public Expression<?> visit(StringCompoundExpression n, Boolean shouldNegate) {
        return super.visit(n, shouldNegate);
    }

    @Override
    public Expression<?> visit(StringIntegerExpression n, Boolean data) {
        return super.visit(n, data);
    }

    //ToDo: test IfThenElse
    //ToDo: should be unnecessary after the IfThenElseRemover
    @Override
    public <E> Expression<?> visit(IfThenElse<E> expr, Boolean shouldNegate) {
        Expression ifCond = expr.getIf();
        Expression thenExpr = expr.getThen();
        Expression elseExpr = expr.getElse();

        Expression result = PropositionalCompound.create
                (PropositionalCompound.create(Negation.create(ifCond), LogicalOperator.OR, thenExpr), LogicalOperator.AND, PropositionalCompound.create(ifCond, LogicalOperator.OR, elseExpr));

        if(shouldNegate){
            return visit(Negation.create(result), false);
        }
        return visit(result, false);
    }

    @Override
    public <E> Expression<?> visit(FunctionExpression<E> expr, Boolean shouldNegate) {

        //FunctionExpressions are not further negated
        if(shouldNegate){
            return Negation.create((Expression<Boolean>) expr);
        }
        return expr;
    }
}
