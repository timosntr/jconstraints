package gov.nasa.jpf.constraints.normalization;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.IfThenElse;
import gov.nasa.jpf.constraints.expressions.LogicalOperator;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.constraints.expressions.QuantifierExpression;

public class NormalizationUtil {

    //ToDo: renameBoundVar, minimizeScope, eliminateQuantifiers, skolemize (contains the three listed methods)
    //ToDo: normalize
    //ToDo: further normalizing methods (order, dependencies...)
    //ToDo: (optional) relabel

    public static <E> Expression<E> createCNF(Expression<E> e) {
        Expression cnf = e.accept(ConjunctionCreatorVisitor.getInstance(), null);
        return cnf;
    }

    public static <E> Expression<E> pushNegation(Expression<E> e) {
        Expression nnf = e.accept(NegatingVisitor.getInstance(), false);
        return nnf;
    }

    public static <E> Expression<E> eliminateEquivalence(Expression<E> e) {
        Expression noEquivalence = e.accept(EquivalenceRemoverVisitor.getInstance(), null);
        return noEquivalence;
    }

    public static <E> Expression<E> eliminateIfThenElse(Expression<E> e) {
        Expression noIfThenElse = e.accept(IfThenElseRemoverVisitor.getInstance(), null);
        return noIfThenElse;
    }

    public static <E> Expression<E> eliminateImplication(Expression<E> e) {
        Expression noImplication = e.accept(ImplicationRemoverVisitor.getInstance(), null);
        return noImplication;
    }

    //ToDo: decide whether check here or use the separate visitor
    public static boolean quantifierCheck(Expression<?> expr){
        if(expr instanceof QuantifierExpression){
            return true;
        }

        Expression<?>[] exprChildren = expr.getChildren();
        for(Expression i : exprChildren){
            if(quantifierCheck(i)){
                return true;
            }
        }
        return false;
    }

    public static boolean equivalenceCheck(Expression<?> expr){
        if(expr instanceof PropositionalCompound){
            LogicalOperator operator = ((PropositionalCompound) expr).getOperator();
            if(operator.equals(LogicalOperator.EQUIV)){
                return true;
            }
        }

        Expression<?>[] exprChildren = expr.getChildren();
        for(Expression i : exprChildren){
            if(equivalenceCheck(i)){
                return true;
            }
        }
        return false;
    }

    public static boolean ifThenElseCheck(Expression<?> expr){
        if(expr instanceof IfThenElse){
            return true;
        }

        Expression<?>[] exprChildren = expr.getChildren();
        for(Expression i : exprChildren){
            if(ifThenElseCheck(i)){
                return true;
            }
        }
        return false;
    }
}
