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

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.*;
import gov.nasa.jpf.constraints.expressions.functions.FunctionExpression;
import gov.nasa.jpf.constraints.normalization.experimentalVisitors.ModifiedNegatingVisitor;
import gov.nasa.jpf.constraints.types.Type;

import java.util.*;

public class NormalizationUtil {

    //ToDo: further normalizing methods (order, dependencies...)
    //ToDo: normalize
    //todo: add SMTLibExportVisitor
    //ToDo: method for counting clauses

    //ToDo: fix skolemization
    public static <E> Expression<E> createCNFforMatrix(Expression<E> e) {
        Expression nnf = createNNF(e);
        if (!nnf.equals(null)) {
            //if(quantifierCheck(e)){
                Expression renamed = renameAllBoundVars(nnf);
                Expression mini = miniScope(renamed);
                Expression skolemized = skolemize(mini);
                Expression prenex = prenexing(skolemized);
                if(prenex instanceof QuantifierExpression){
                    Quantifier q = ((QuantifierExpression) prenex).getQuantifier();
                    List<? extends Variable<?>> bound = ((QuantifierExpression) prenex).getBoundVariables();
                    Expression body = ((QuantifierExpression) prenex).getBody();
                    Expression matrix = ConjunctionCreatorVisitor.getInstance().apply(body, null);
                    Expression result = QuantifierExpression.create(q, bound, matrix);
                    return result;
                } else {
                    return ConjunctionCreatorVisitor.getInstance().apply(prenex, null);
                }
            //} else {
            //    return ConjunctionCreatorVisitor.getInstance().apply(nnf, null);
            //}
        } else {
            throw new UnsupportedOperationException("Creation of NNF failed, no CNF created!");
        }
    }

    public static <E> Expression<E> createCNF(Expression<E> e) {
        Expression nnf = createNNF(e);
        if (!nnf.equals(null)) {
            /*if(quantifierCheck(e)){
                Expression renamed = renameAllBoundVars(nnf);
                Expression mini = miniScope(renamed);
                Expression skolemized = skolemize(mini);
                Expression prenex = prenexing(skolemized);
                if(prenex instanceof QuantifierExpression){
                    Quantifier q = ((QuantifierExpression) prenex).getQuantifier();
                    List<? extends Variable<?>> bound = ((QuantifierExpression) prenex).getBoundVariables();
                    Expression body = ((QuantifierExpression) prenex).getBody();
                    Expression matrix = ConjunctionCreatorVisitor.getInstance().apply(body, null);
                    Expression result = QuantifierExpression.create(q, bound, matrix);
                    return result;
                } else {
                    return ConjunctionCreatorVisitor.getInstance().apply(prenex, null);
                }
            } else {*/
                return ConjunctionCreatorVisitor.getInstance().apply(nnf, null);
        //    }
        } else {
            throw new UnsupportedOperationException("Creation of NNF failed, no CNF created!");
        }
    }

    public static <E> Expression<E> simplifyProblem(Expression<E> e) {
        return SimplifyProblemVisitor.getInstance().apply(e, null);
    }

    public static <E> Expression<E> createCNFNoQuantorHandling(Expression<E> e) {
        return ConjunctionCreatorVisitor.getInstance().apply(e, null);
    }

    //nnf has to be created beforehand
    public static <E> Expression<E> createDNF(Expression<E> e) {
        Expression nnf = createNNF(e);
        if (!nnf.equals(null)) {
            if(quantifierCheck(e)){
                Expression renamed = renameAllBoundVars(nnf);
                Expression mini = miniScope(renamed);
                Expression skolemized = skolemize(mini);
                Expression prenex = prenexing(skolemized);
                if(prenex instanceof QuantifierExpression){
                    Quantifier q = ((QuantifierExpression) prenex).getQuantifier();
                    List<? extends Variable<?>> bound = ((QuantifierExpression) prenex).getBoundVariables();
                    Expression body = ((QuantifierExpression) prenex).getBody();
                    Expression matrix = DisjunctionCreatorVisitor.getInstance().apply(body, null);
                    Expression result = QuantifierExpression.create(q, bound, matrix);
                    return result;
                } else {
                    return DisjunctionCreatorVisitor.getInstance().apply(prenex, null);
                }
            } else {
                return DisjunctionCreatorVisitor.getInstance().apply(nnf, null);
            }
        } else {
            throw new UnsupportedOperationException("Creation of NNF failed, no DNF created!");
        }
    }

    //nnf has to be created beforehand
    public static <E> Expression<E> createDNFNoQuantorHandling(Expression<E> e) {
        return DisjunctionCreatorVisitor.getInstance().apply(e, null);
    }

    public static <E> Expression<E> createNNF(Expression<E> e) {
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
                        //return NegatingVisitor.getInstance().apply(noXOR, false);
                        } else {
                            System.out.println("eliminateIfThenElse failed!");
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

    public static <E> Expression<E> simpleNegationPush(Expression<E> e) {
        return NegatingVisitor.getInstance().apply(e, false);
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

    public static <E> Expression<E> renameAllBoundVars(Expression<E> e) {
        HashMap<String, String> data = new HashMap<>();
        return RenamingBoundVarVisitor.getInstance().apply(e, data);
    }

    public static <E> Expression<E> skolemize(Expression<E> e) {
        List<Variable<?>> data = new ArrayList<>();
        return SkolemizationVisitor.getInstance().apply(e, data);
    }

    public static <E> Expression<E> prenexing(Expression<E> e) {
        return PrenexFormVisitor.getInstance().apply(e, null);
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

    /*public static boolean nameClashWithExistingNames(String name, Collection<String> existingNames) {
        if(existingNames != null) {
            for (String fName : existingNames) {
                if(fName.equals(name)){
                    return true;
                }
            }
        }
        return false;
    }*/

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
    /*public static HashMap<String, Expression> skolemizeFreeVars(Collection<Variable<?>> freeVars, int[] id) {
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
    }*/

    //checking methods
    public static boolean quantifierCheck(Expression<?> expr){
        if(expr instanceof QuantifierExpression){
            return true;
        }

        if(expr instanceof LetExpression){
            Expression flattened = ((LetExpression) expr).flattenLetExpression();
            Expression<?>[] exprChildren = flattened.getChildren();
            for(Expression i : exprChildren){
                if(quantifierCheck(i)){
                    return true;
                }
            }
        } else {
            Expression<?>[] exprChildren = expr.getChildren();
            for(Expression i : exprChildren){
                if(quantifierCheck(i)){
                    return true;
                }
            }
        }
        return false;
    }

    public static int countQuantifiers(Expression<?> expr){
        int count = 0;
        if(expr instanceof LetExpression){
            Expression flattened = ((LetExpression) expr).flattenLetExpression();
            Expression<?>[] children = flattened.getChildren();
            if(children.length != 0){
                for(Expression child : expr.getChildren()){
                    if(expr instanceof QuantifierExpression){
                        count++;
                    }
                    countQuantifiers(child);
                }
            }
        } else {
            Expression[] children = expr.getChildren();
            if(children.length != 0){
                for(Expression child : expr.getChildren()){
                    if(expr instanceof QuantifierExpression){
                        count++;
                    }
                    countQuantifiers(child);
                }
            }
        }
        return count;
    }
    //TODO: rewrite counting methods; they do not work
    public static int countItes(Expression<?> expr){
        int count = 0;
        if(expr instanceof IfThenElse){
            count++;
        }
        if(expr instanceof LetExpression){
            Expression flattened = ((LetExpression) expr).flattenLetExpression();
            Expression<?>[] children = flattened.getChildren();
            if(children.length != 0){
                for(Expression child : expr.getChildren()){
                    countItes(child);
                }
            }
        } else {
            Expression[] children = expr.getChildren();
            if(children.length != 0){
                for(Expression child : expr.getChildren()){
                    countItes(child);
                }
            }
        }
        return count;
    }

    public static int countEquivalences(Expression<?> expr){
        int count = 0;
        if(expr instanceof PropositionalCompound){
            if(((PropositionalCompound) expr).getOperator().equals(LogicalOperator.EQUIV)){
                count++;
            }
        }
        if(expr instanceof LetExpression){
            Expression flattened = ((LetExpression) expr).flattenLetExpression();
            Expression<?>[] children = flattened.getChildren();
            if(children.length != 0){
                for(Expression child : expr.getChildren()){
                    countEquivalences(child);
                }
            }
        } else {
            Expression[] children = expr.getChildren();
            if(children.length != 0){
                for(Expression child : expr.getChildren()){
                    countEquivalences(child);
                }
            }
        }
        return count;
    }

    public static int countConjunctions(Expression<?> expr){
        int count = 0;
        if(expr instanceof PropositionalCompound){
            if(((PropositionalCompound) expr).getOperator().equals(LogicalOperator.AND)){
                count++;
            }
        }
        if(expr instanceof LetExpression){
            Expression flattened = ((LetExpression) expr).flattenLetExpression();
            Expression<?>[] children = flattened.getChildren();
            if(children.length != 0){
                for(Expression child : expr.getChildren()){
                    countConjunctions(child);
                }
            }
        } else {
            Expression[] children = expr.getChildren();
            if(children.length != 0){
                for(Expression child : expr.getChildren()){
                    countConjunctions(child);
                }
            }
        }
        return count;
    }

    public static int countDisjunctions(Expression<?> expr){
        int count = 0;
        if(expr instanceof LetExpression){
            Expression flattened = ((LetExpression) expr).flattenLetExpression();
            Expression<?>[] children = flattened.getChildren();
            if(children.length != 0){
                for(Expression child : expr.getChildren()){
                    countDisjunctions(child);
                }
            }
        } else {
            Expression[] children = expr.getChildren();
            if(children.length != 0){
                for(Expression child : expr.getChildren()){
                    countDisjunctions(child);
                }
            }
        }
        return count;
    }

    public static boolean equivalenceCheck(Expression<?> expr){
        if(expr instanceof PropositionalCompound){
            LogicalOperator operator = ((PropositionalCompound) expr).getOperator();
            if(operator.equals(LogicalOperator.EQUIV)){
                return true;
            }
        }
        if(expr instanceof LetExpression){
            Expression flattened = ((LetExpression) expr).flattenLetExpression();
            Expression<?>[] exprChildren = flattened.getChildren();
            for(Expression i : exprChildren){
                if(equivalenceCheck(i)){
                    return true;
                }
            }
        } else {
            Expression<?>[] exprChildren = expr.getChildren();
            for(Expression i : exprChildren){
                if(equivalenceCheck(i)){
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean ifThenElseCheck(Expression<?> expr){
        if(expr instanceof IfThenElse){
            return true;
        }
        if(expr instanceof LetExpression){
            Expression flattened = ((LetExpression) expr).flattenLetExpression();
            Expression<?>[] exprChildren = flattened.getChildren();
            for(Expression i : exprChildren){
                if(ifThenElseCheck(i)){
                    return true;
                }
            }
        } else {
            Expression<?>[] exprChildren = expr.getChildren();
            for(Expression i : exprChildren){
                if(ifThenElseCheck(i)){
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean letExpressionCheck(Expression<?> expr){
        if(expr instanceof LetExpression){
            return true;
        }

        Expression<?>[] exprChildren = expr.getChildren();
        for(Expression i : exprChildren){
            if(letExpressionCheck(i)){
                return true;
            }
        }
        return false;
    }

}
