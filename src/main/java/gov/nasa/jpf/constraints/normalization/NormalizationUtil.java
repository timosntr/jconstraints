package gov.nasa.jpf.constraints.normalization;

import com.google.common.base.Function;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.IfThenElse;
import gov.nasa.jpf.constraints.expressions.LogicalOperator;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.constraints.expressions.QuantifierExpression;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class NormalizationUtil {

    //ToDo: minimizeScope, eliminateQuantifiers, skolemize (contains the three listed methods)
    //ToDo: normalize
    //ToDo: further normalizing methods (order, dependencies...)
    //ToDo: (optional) relabel

    public static <E> Expression<E> createCNF(Expression<E> e) {
        Expression cnf = e.accept(ConjunctionCreatorVisitor.getInstance(), null);
        return cnf;
    }

    public static <E> Expression<E> pushNegation(Expression<E> e) {
        Expression noEquivalence = eliminateEquivalence(e);
        Expression noImplication = eliminateImplication(noEquivalence);
        Expression noXOR = eliminateXOR(noImplication);
        //optional
        Expression noIte = eliminateIfThenElse(noXOR);

        Expression nnf = (Expression) noIte.accept(NegatingVisitor.getInstance(), false);
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

    public static <E> Expression<E> eliminateXOR(Expression<E> e) {
        Expression noXOR = e.accept(XorRemoverVisitor.getInstance(), null);
        return noXOR;
    }

    public static <E> Expression<E> renameAllBoundVars(Expression<E> e) {
        Expression renamed = e.accept(RenameBoundVarVisitor.getInstance(), null);
        return renamed;
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

    public static Function<String, String> renameBoundVariables(QuantifierExpression q, int id, Collection<Variable<?>> freeVars) {

        //UUID id = UUID.randomUUID();
        List<? extends Variable<?>> boundVariables = q.getBoundVariables();
        HashMap<String, String> mappingOfNames = new HashMap<>();
        if(boundVariables != null){
            for(Variable v : boundVariables){
                String oldName = v.getName();
                String newName = "QF." + id + "." + oldName;
                while(nameClashWithFreeVars(newName, freeVars)){
                    id++;
                    newName = "QF." + id + "." + oldName;
                }
                mappingOfNames.put(oldName, newName);
            }
            return (vName) -> { return mappingOfNames.get(vName); };
        } else {
            throw new UnsupportedOperationException("No bound variables found.");
        }
    }

    public static boolean nameClashWithFreeVars(String name, Collection<Variable<?>> freeVars) {

        if(freeVars != null) {
            for (Variable v : freeVars) {
                String freeVarName = v.getName();
                if(freeVarName.equals(name)){
                    return true;
                }
            }
        }
        return false;
    }

}
