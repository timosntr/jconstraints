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
        Expression nnf = pushNegation(e);
        //ToDo: Skolemization, if quanfifiers are present
        return ConjunctionCreatorVisitor.getInstance().apply(nnf, null);
    }

    public static <E> Expression<E> createDNF(Expression<E> e) {
        Expression nnf = pushNegation(e);
        //ToDo: Skolemization, if quanfifiers are present
        return DisjunctionCreatorVisitor.getInstance().apply(nnf, null);
    }

    public static <E> Expression<E> pushNegation(Expression<E> e) {

        Expression noLet = eliminateLetExpressions(e);
        Expression noEquivalence = eliminateEquivalence(noLet);
        Expression noImplication = eliminateImplication(noEquivalence);
        Expression noXOR = eliminateXOR(noImplication);
        Expression noIte = eliminateIfThenElse(noXOR);

        return NegatingVisitor.getInstance().apply(noIte, false);
    }

    public static <E> Expression<E> eliminateEquivalence(Expression<E> e) {
        return EquivalenceRemoverVisitor.getInstance().apply(e, null);
    }

    public static <E> Expression<E> eliminateIfThenElse(Expression<E> e) {
        return IfThenElseRemoverVisitor.getInstance().apply(e, null);
    }

    public static <E> Expression<E> eliminateLetExpressions(Expression<E> e) {
        return LetExpressionRemoverVisitor.getInstance().apply(e, null);
    }

    public static <E> Expression<E> eliminateImplication(Expression<E> e) {
        return ImplicationRemoverVisitor.getInstance().apply(e, null);
    }

    public static <E> Expression<E> eliminateXOR(Expression<E> e) {
        return XorRemoverVisitor.getInstance().apply(e, null);
    }

    //Methods for handling of quantifiers
    public static <E> Expression<E> renameAllBoundVars(Expression<E> e) {
        return RenameBoundVarVisitor.getInstance().apply(e, null);
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

    //checking methods
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
