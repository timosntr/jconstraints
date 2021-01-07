/**
 * Copyright 2020, TU Dortmund, Malte Mues (@mmuesly)
 *
 * <p>This is a derived version of JConstraints original located at:
 * https://github.com/psycopaths/jconstraints
 *
 * <p>Until commit: https://github.com/tudo-aqua/jconstraints/commit/876e377 the original license
 * is: Copyright (C) 2015, United States Government, as represented by the Administrator of the
 * National Aeronautics and Space Administration. All rights reserved.
 *
 * <p>The PSYCO: A Predicate-based Symbolic Compositional Reasoning environment platform is licensed
 * under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>Modifications and new contributions are Copyright by TU Dortmund 2020, Malte Mues under Apache
 * 2.0 in alignment with the original repository license.
 */
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
import gov.nasa.jpf.constraints.types.Type;

import java.util.*;

public class NormalizationUtil {

    //ToDo: further normalizing methods (order, dependencies...)
    //ToDo: normalize

    public static <E> Expression<E> createCNF(Expression<E> e) {
        Expression nnf = pushNegation(e);
        if (!nnf.equals(null)) {
            if(quantifierCheck(e)){
                Expression skolemized = skolemize(e);
                Expression noQuantifiers = dropForallQuantifiers(skolemized);
                if(!noQuantifiers.equals(null)){
                    return ConjunctionCreatorVisitor.getInstance().apply(nnf, null);
                } else {
                    throw new UnsupportedOperationException("Handling of Quantifiers failed!");
                }
            } else {
                return ConjunctionCreatorVisitor.getInstance().apply(nnf, null);
            }
        } else {
            throw new UnsupportedOperationException("Creation of NNF failed, no CNF created!");
        }
    }

    public static <E> Expression<E> createDNF(Expression<E> e) {
        Expression nnf = pushNegation(e);
        if (!nnf.equals(null)) {
            if(quantifierCheck(e)){
                Expression skolemized = skolemize(e);
                Expression noQuantifiers = dropForallQuantifiers(skolemized);
                if(!noQuantifiers.equals(null)){
                    return DisjunctionCreatorVisitor.getInstance().apply(nnf, null);
                } else {
                    throw new UnsupportedOperationException("Handling of Quantifiers failed!");
                }
            } else {
                return DisjunctionCreatorVisitor.getInstance().apply(nnf, null);
            }
        } else {
            throw new UnsupportedOperationException("Creation of NNF failed, no DNF created!");
        }
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
    public static <E> Expression<E> miniScope(Expression<E> e) {
        return MiniScopingVisitor.getInstance().apply(e, null);
    }

    public static <E> Expression<E> makeBoundVarsUnique(Expression<E> e) {
        Function<String, String> data = null;
        return RenameBoundVarVisitor.getInstance().apply(e, data);
    }

    public static <E> Expression<E> skolemize(Expression<E> e) {
        Expression mini = miniScope(e);
        Expression unique = makeBoundVarsUnique(e);
        List<Variable<?>> data = new ArrayList<>();
        return SkolemizationVisitor.getInstance().apply(unique, data);
    }

    public static <E> Expression<E> dropForallQuantifiers(Expression<E> e) {
        return ForallRemoverVisitor.getInstance().apply(e, null);
    }

    /*public static <E> Expression<E> renameAllBoundVars(Expression<E> e) {
        return RenameBoundVarVisitor.getInstance().apply(e, null);
    }*/

    public static Function<String, String> renameBoundVariables(QuantifierExpression q, int[] id, Collection<Variable<?>> freeVars) {

        //UUID id = UUID.randomUUID();
        List<? extends Variable<?>> boundVariables = q.getBoundVariables();
        HashMap<String, String> mappingOfNames = new HashMap<>();
        if(boundVariables != null){
            for(Variable v : boundVariables){
                String oldName = v.getName();
                String newName = "Q." + id[0] + "." + oldName;
                while(nameClashWithExistingFreeVars(newName, freeVars)){
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

    /*public static Collection<String> collectVariableNames(Expression<?> expr) {
        Collection<String> variableNames = new ArrayList<>();
        if(expr instanceof Variable){
            String name = ((Variable<?>) expr).getName();
            variableNames.add(name);
        }

        Expression<?>[] exprChildren = expr.getChildren();
        for(Expression i : exprChildren){
            collectFunctionNames(i);
        }
        return variableNames;
    }*/

    public static boolean nameClashWithExistingFreeVars(String name, Collection<Variable<?>> existingVars) {
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

    public static boolean nameClashWithExistingNames(String name, Collection<String> existingNames) {
        if(existingNames != null) {
            for (String fName : existingNames) {
                if(fName.equals(name)){
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean containsDuplicateNames(Collection<Variable<?>> vars) {
        Collection<Variable> existing = new ArrayList<>();
        if(vars != null) {
            for (Variable v : vars) {
                if(existing.contains(v)){
                    return true;
                }
                existing.add(v);
            }
        }
        return false;
    }

    //free Variables are implicitly existentially quantified
    //could possibly be replaced by a separate ExistentionalClosure
    public static HashMap<String, Expression> skolemizeFreeVars(Collection<Variable<?>> freeVars, int[] id) {
        HashMap<String, Expression> functionNames = new HashMap<>();
        if(!freeVars.isEmpty()){
            for(Variable var : freeVars){
                String name = var.getName();
                String nameConstant = "SK.f.constant." + id[0] + "." + name;
                while(functionNames.containsKey(nameConstant)){
                    id[0]++;
                    nameConstant = "SK.f.constant." + id[0] + "." + name;
                }
                Type type = var.getType();
                gov.nasa.jpf.constraints.expressions.functions.Function f = gov.nasa.jpf.constraints.expressions.functions.Function.create(nameConstant, type);
                Variable v[] = new Variable[f.getArity()];
                FunctionExpression expr = FunctionExpression.create(f, v);
                functionNames.put(name, expr);
            }
        }
        return functionNames;
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
