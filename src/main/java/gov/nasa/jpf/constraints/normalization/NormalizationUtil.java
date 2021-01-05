package gov.nasa.jpf.constraints.normalization;

import com.google.common.base.Function;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.IfThenElse;
import gov.nasa.jpf.constraints.expressions.LogicalOperator;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.constraints.expressions.QuantifierExpression;
import gov.nasa.jpf.constraints.expressions.functions.FunctionExpression;
import gov.nasa.jpf.constraints.normalization.experimentalVisitors.ModifiedNegatingVisitor;
import gov.nasa.jpf.constraints.types.BuiltinTypes;

import java.util.*;

public class NormalizationUtil {

    //ToDo: minimizeScope, eliminateQuantifiers, skolemize (contains the three listed methods)
    //ToDo: normalize
    //ToDo: further normalizing methods (order, dependencies...)
    //ToDo: (optional) relabel

    public static <E> Expression<E> createCNF(Expression<E> e) {
        Expression nnf = pushNegation(e);
        //ToDo: Skolemization, if quanfifiers are present
        if (!nnf.equals(null)) {
            return ConjunctionCreatorVisitor.getInstance().apply(nnf, null);
        } else {
            throw new UnsupportedOperationException("Creation of NNF failed, no CNF created!");
        }
    }

    public static <E> Expression<E> createDNF(Expression<E> e) {
        Expression nnf = pushNegation(e);
        //ToDo: Skolemization, if quanfifiers are present
        return DisjunctionCreatorVisitor.getInstance().apply(nnf, null);
    }

    public static <E> Expression<E> pushNegation(Expression<E> e) {
        //LetExpressions have to be flattened to get children
        Expression noLet = eliminateLetExpressions(e);
        if(!noLet.equals(null)) {
            Expression noEquivalence = eliminateEquivalence(noLet);
            if (!noEquivalence.equals(null)) {
                Expression noImplication = eliminateImplication(noEquivalence);
                if (!noImplication.equals(null)) {
                    Expression noXOR = eliminateXOR(noImplication);
                    if (!noXOR.equals(null)) {
                        Expression noIte = eliminateIfThenElse(noXOR);
                        if(!noIte.equals(null)){
                            return NegatingVisitor.getInstance().apply(noIte, false);
                        } else {
                            System.out.println("eliminateIfThenElse failed!");
                            return NegatingVisitor.getInstance().apply(noXOR, false);
                        }
                    }
                    System.out.println("eliminateXOR failed!");
                }
                System.out.println("eliminateImplication failed!");
            }
            System.out.println("eliminateEquivalence failed!");
        }
        //alternativ:
        //return e;
        throw new UnsupportedOperationException("Negations were not pushed!");
    }

    public static <E> Expression<E> pushNegationModified(Expression<E> e) {
        //LetExpressions have to be flattened to get children
        Expression noLet = eliminateLetExpressions(e);
        if(!noLet.equals(null)) {
            Expression noEquivalence = eliminateEquivalence(noLet);
            if (!noEquivalence.equals(null)) {
                Expression noImplication = eliminateImplication(noEquivalence);
                if (!noImplication.equals(null)) {
                    Expression noXOR = eliminateXOR(noImplication);
                    if (!noXOR.equals(null)) {
                        Expression noIte = eliminateIfThenElse(noXOR);
                        if(!noIte.equals(null)){
                            return ModifiedNegatingVisitor.getInstance().apply(noIte, false);
                        } else {
                            System.out.println("eliminateIfThenElse failed!");
                            return NegatingVisitor.getInstance().apply(noXOR, false);
                        }
                    }
                    System.out.println("eliminateXOR failed!");
                }
                System.out.println("eliminateImplication failed!");
            }
            System.out.println("eliminateEquivalence failed!");
        }
        throw new UnsupportedOperationException("Negations were not pushed!");
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

    public static Function<String, String> renameBoundVariables(QuantifierExpression q, int[] id, Collection<Variable<?>> freeVars) {

        //UUID id = UUID.randomUUID();
        List<? extends Variable<?>> boundVariables = q.getBoundVariables();
        HashMap<String, String> mappingOfNames = new HashMap<>();
        if(boundVariables != null){
            for(Variable v : boundVariables){
                String oldName = v.getName();
                String newName = "Q." + id[0] + "." + oldName;
                while(nameClashWithExistingVars(newName, freeVars)){
                    id[0]++;
                    newName = "Q." + id[0] + "." + oldName;
                }
                mappingOfNames.put(oldName, newName);
            }
            return (vName) -> { return mappingOfNames.get(vName); };
        } else {
            throw new UnsupportedOperationException("No bound variables found.");
        }
    }

    public static Collection<String> collectFunctionNames(Expression<?> expr) {
        Collection<String> functionNames = new ArrayList<>();
        if(expr instanceof FunctionExpression){
            String name = ((FunctionExpression<?>) expr).getFunction().getName();
            functionNames.add(name);
        }

        Expression<?>[] exprChildren = expr.getChildren();
        for(Expression i : exprChildren){
            collectFunctionNames(i);
        }
        return functionNames;
    }

    public static boolean nameClashWithExistingVars(String name, Collection<Variable<?>> existingVars) {

        if(existingVars != null) {
            for (Variable v : existingVars) {
                String varName = v.getName();
                if(varName.equals(name)){
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean nameClashWithFunctions(String name, Collection<String> existingFunctions) {

        if(existingFunctions != null) {
            for (String fName : existingFunctions) {
                if(fName.equals(name)){
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
