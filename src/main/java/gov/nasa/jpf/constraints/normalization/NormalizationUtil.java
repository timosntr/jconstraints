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
import gov.nasa.jpf.constraints.normalization.experimentalVisitors.ModifiedIfThenElseRemoverVisitor;
import gov.nasa.jpf.constraints.normalization.experimentalVisitors.ModifiedNegatingVisitor;

import java.util.*;

public class NormalizationUtil {

    public static <E> Expression<E> normalize(Expression<E> e, String form){
        boolean buildCnf = false;
        boolean buildDnf = false;
        if(form.equalsIgnoreCase("cnf")){
            buildCnf = true;
        } else if(form.equalsIgnoreCase("dnf")){
            buildDnf = true;
        } else {
            throw new UnsupportedOperationException("Parameter 'cnf' or 'dnf' needed!");
        }

        if(quantifierCheck(e)){
            if(buildCnf){
                Expression cnf = createCNFforMatrix(e);
                return cnf;
            } else if(buildDnf){
                Expression dnf = createDNF(e);
                return dnf;
            } else {
                throw new UnsupportedOperationException("Parameter 'cnf' or 'dnf' needed!");
            }
        } else {
            if(buildCnf){
                Expression cnf = createCNF(e);
                return cnf;
            } else if(buildDnf){
                Expression nnf = createNNF(e);
                Expression dnf = createDNFNoQuantorHandling(nnf);
                return dnf;
            } else {
                throw new UnsupportedOperationException("Parameter 'cnf' or 'dnf' needed!");
            }
        }
    }

    //TODO: order first after all removers?
    public static <E> Expression<E> createNNF(Expression<E> e) {
        //LetExpressions have to be flattened to get children
        Expression noLet = eliminateLetExpressions(e);
        //TODO: find errors
        //Expression simplified = simplifyProblem(noLet);
        //Expression ordered = orderProblem(simplified);

        Expression noEquivalence = eliminateEquivalence(noLet);
        Expression noImplication = eliminateImplication(noEquivalence);
        Expression noXOR = eliminateXOR(noImplication);
        Expression noIte = eliminateIfThenElse(noXOR);
        return NegatingVisitor.getInstance().apply(noIte, false);
    }

    public static <E> Expression<E> simpleNegationPush(Expression<E> e) {
        return NegatingVisitor.getInstance().apply(e, false);
    }

    //TODO: order first after all removers?
    public static <E> Expression<E> pushNegationModified(Expression<E> e) {
        //LetExpressions have to be flattened to get children
        Expression noLet = eliminateLetExpressions(e);
        //TODO: find errors
        //Expression simplified = simplifyProblem(noLet);
        //Expression ordered = orderProblem(simplified);

        Expression noEquivalence = eliminateEquivalence(noLet);
        Expression noImplication = eliminateImplication(noEquivalence);
        Expression noXOR = eliminateXOR(noImplication);
        Expression noIte = eliminateIfThenElse(noXOR);
        return ModifiedNegatingVisitor.getInstance().apply(noIte, false);

    }

    public static <E> Expression<E> createCNFforMatrix(Expression<E> e) {
        //Expression simplified = simplifyProblem(e);
        Expression nnf = createNNF(e);
        if (!nnf.equals(null)) {
            Expression renamed = renameAllBoundVars(nnf);
            //miniscoping is only senseful if there is at least an EXISTS after creation of nnf
            Expression beforeSkolemization;
            if(checkForExists(renamed)){
                beforeSkolemization = miniScope(renamed);
            } else {
                beforeSkolemization = renamed;
            }
            Expression skolemized = skolemize(beforeSkolemization);
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
        } else {
            throw new UnsupportedOperationException("Creation of NNF failed, no CNF created!");
        }
    }

    public static <E> Expression<E> createCNF(Expression<E> e) {
        Expression nnf = createNNF(e);
        if (!nnf.equals(null)) {
            return ConjunctionCreatorVisitor.getInstance().apply(nnf, null);
        } else {
            throw new UnsupportedOperationException("Creation of NNF failed, no CNF created!");
        }
    }

    public static <E> Expression<E> createCNFNoQuantorHandling(Expression<E> e) {
        return ConjunctionCreatorVisitor.getInstance().apply(e, null);
    }

    //nnf has to be created beforehand
    public static <E> Expression<E> createDNF(Expression<E> e) {
        Expression nnf = createNNF(e);
        if (!nnf.equals(null)) {
            if(quantifierCheck(nnf)){
                Expression renamed = renameAllBoundVars(nnf);
                //miniscoping is only senseful if there is at least an EXISTS after creation of nnf
                Expression beforeSkolemization;
                if(checkForExists(renamed)){
                    beforeSkolemization = miniScope(renamed);
                } else {
                    beforeSkolemization = renamed;
                }
                Expression skolemized = skolemize(beforeSkolemization);
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

    //todo: simplify does not remove duplicates completely yet
    public static <E> Expression<E> simplifyProblem(Expression<E> e) {
        LinkedList<Expression<?>> data = new LinkedList<>();
        return SimplifyProblemVisitor.getInstance().apply(e, data);
    }

    public static <E> Expression<E> orderProblem(Expression<E> e) {
        return OrderingVisitor.getInstance().apply(e, null);
    }

    public static <E> Expression<E> eliminateEquivalence(Expression<E> e) {
        return EquivalenceRemoverVisitor.getInstance().apply(e, null);
    }

    public static <E> Expression<E> eliminateIfThenElse(Expression<E> e) {
        return IfThenElseRemoverVisitor.getInstance().apply(e, null);
    }

    public static <E> Expression<E> eliminatePropositionalIfThenElse(Expression<E> e) {
        return ModifiedIfThenElseRemoverVisitor.getInstance().apply(e, null);
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

    //checking and counting methods
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

    public static boolean checkForForall(Expression<?> expr){
        if(expr instanceof QuantifierExpression){
            if(((QuantifierExpression) expr).getQuantifier().equals(Quantifier.FORALL)){
                return true;
            }
        }

        if(expr instanceof LetExpression){
            Expression flattened = ((LetExpression) expr).flattenLetExpression();
            Expression<?>[] exprChildren = flattened.getChildren();
            for(Expression i : exprChildren){
                if(checkForForall(i)){
                    return true;
                }
            }
        } else {
            Expression<?>[] exprChildren = expr.getChildren();
            for(Expression i : exprChildren){
                if(checkForForall(i)){
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean checkForExists(Expression<?> expr){
        if(expr instanceof QuantifierExpression){
            if(((QuantifierExpression) expr).getQuantifier().equals(Quantifier.EXISTS)){
                return true;
            }
        }

        if(expr instanceof LetExpression){
            Expression flattened = ((LetExpression) expr).flattenLetExpression();
            Expression<?>[] exprChildren = flattened.getChildren();
            for(Expression i : exprChildren){
                if(checkForExists(i)){
                    return true;
                }
            }
        } else {
            Expression<?>[] exprChildren = expr.getChildren();
            for(Expression i : exprChildren){
                if(checkForExists(i)){
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean mixedQuantifierCheck(Expression<?> expr){
        return (checkForExists(expr) && checkForForall(expr));
    }

    public static int countQuantifiers(Expression<?> expr){
        //return QuantifierCounterVisitor.getInstance().apply(expr);
        int count = 0;
        if(expr instanceof LetExpression){
            Expression flattened = ((LetExpression) expr).flattenLetExpression();
            Expression<?>[] children = flattened.getChildren();
            if(children.length != 0){
                for(Expression child : children){
                    if(expr instanceof QuantifierExpression){
                        count++;
                    }
                    count += countQuantifiers(child);
                }
            }
        } else {
            Expression[] children = expr.getChildren();
            if(children.length != 0){
                for(Expression child : children){
                    if(expr instanceof QuantifierExpression){
                        count++;
                    }
                    count += countQuantifiers(child);
                }
            }
        }
        return count;
    }

    public static int countItes(Expression<?> expr){
        //return IfThenElseCounterVisitor.getInstance().apply(expr);
        int count = 0;
        if(expr instanceof IfThenElse){
            count++;
        }
        if(expr instanceof LetExpression) {
            Expression flattened = ((LetExpression) expr).flattenLetExpression();
            count += countItes(flattened);
        } else if(expr instanceof QuantifierExpression){
            Expression body = ((QuantifierExpression) expr).getBody();
                count += countItes(body);
        } else {
            Expression[] children = expr.getChildren();
            if(children.length != 0){
                for(Expression child : children){
                    count += countItes(child);
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
                for(Expression child : children){
                    count += countEquivalences(child);
                }
            }
        } else {
            Expression[] children = expr.getChildren();
            if(children.length != 0){
                for(Expression child : children){
                    count += countEquivalences(child);
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
                for(Expression child : children){
                    count += countConjunctions(child);
                }
            }
        } else {
            Expression[] children = expr.getChildren();
            if(children.length != 0){
                for(Expression child : children){
                    count += countConjunctions(child);
                }
            }
        }
        return count;
    }

    public static int countDisjunctions(Expression<?> expr){
        int count = 0;
        if(expr instanceof PropositionalCompound){
            if(((PropositionalCompound) expr).getOperator().equals(LogicalOperator.OR)){
                count++;
            }
        }
        if(expr instanceof LetExpression){
            Expression flattened = ((LetExpression) expr).flattenLetExpression();
            Expression<?>[] children = flattened.getChildren();
            if(children.length != 0){
                for(Expression child : children){
                    count += countDisjunctions(child);
                }
            }
        } else {
            Expression[] children = expr.getChildren();
            if(children.length != 0){
                for(Expression child : children){
                    count += countDisjunctions(child);
                }
            }
        }
        return count;
    }

    public static int maxDisjunctionLength(Expression<?> expr){
        int count = 0;
        int maxLength = 0;
        if(expr instanceof PropositionalCompound){
            if(((PropositionalCompound) expr).getOperator().equals(LogicalOperator.OR)){
                count++;
            }
            if(((PropositionalCompound) expr).getOperator().equals(LogicalOperator.AND)){
                count=0;
            }
        }
        if(expr instanceof LetExpression){
            Expression flattened = ((LetExpression) expr).flattenLetExpression();
            Expression<?>[] children = flattened.getChildren();
            if(children.length != 0){
                for(Expression child : children){
                    count += maxDisjunctionLength(child);
                    if(count > maxLength){
                        maxLength = count;
                    }
                }
            }
        } else {
            Expression[] children = expr.getChildren();
            if(children.length != 0){
                for(Expression child : children){
                    count += maxDisjunctionLength(child);
                    if(count > maxLength){
                        maxLength = count;
                    }
                }
            }
        }
        return maxLength;
    }

    public static int maxConjunctionLength(Expression<?> expr){
        int count = 0;
        int maxLength = 0;
        if(expr instanceof PropositionalCompound){
            if(((PropositionalCompound) expr).getOperator().equals(LogicalOperator.AND)){
                count++;
            }
            if(((PropositionalCompound) expr).getOperator().equals(LogicalOperator.OR)){
                count=0;
            }
        }
        if(expr instanceof LetExpression){
            Expression flattened = ((LetExpression) expr).flattenLetExpression();
            Expression<?>[] children = flattened.getChildren();
            if(children.length != 0){
                for(Expression child : children){
                    count += maxConjunctionLength(child);
                    if(count > maxLength){
                        maxLength = count;
                    }
                }
            }
        } else {
            Expression[] children = expr.getChildren();
            if(children.length != 0){
                for(Expression child : children){
                    count += maxConjunctionLength(child);
                    if(count > maxLength){
                        maxLength = count;
                    }
                }
            }
        }
        return maxLength;
    }

    public static int countNegations(Expression<?> expr){
        int count = 0;
        if(expr instanceof Negation){
            count++;
        }
        if(expr instanceof LetExpression){
            Expression flattened = ((LetExpression) expr).flattenLetExpression();
            Expression<?>[] children = flattened.getChildren();
            if(children.length != 0){
                for(Expression child : children){
                    count += countNegations(child);
                }
            }
        } else {
            Expression[] children = expr.getChildren();
            if(children.length != 0){
                for(Expression child : children){
                    count += countNegations(child);
                }
            }
        }
        return count;
    }

    public static int countXORs(Expression<?> expr){
        int count = 0;
        if(expr instanceof PropositionalCompound){
            if(((PropositionalCompound) expr).getOperator().equals(LogicalOperator.XOR)){
                count++;
            }
        }
        if(expr instanceof LetExpression){
            Expression flattened = ((LetExpression) expr).flattenLetExpression();
            Expression<?>[] children = flattened.getChildren();
            if(children.length != 0){
                for(Expression child : children){
                    count += countXORs(child);
                }
            }
        } else {
            Expression[] children = expr.getChildren();
            if(children.length != 0){
                for(Expression child : children){
                    count += countXORs(child);
                }
            }
        }
        return count;
    }

    public static boolean equivalenceCheck(Expression<?> expr){
        boolean check = false;
        if(expr instanceof PropositionalCompound){
            LogicalOperator operator = ((PropositionalCompound) expr).getOperator();
            if(operator.equals(LogicalOperator.EQUIV)){
                check = true;
                return check;
            }
        }
        if(expr instanceof LetExpression){
            Expression flattened = ((LetExpression) expr).flattenLetExpression();
            Expression<?>[] exprChildren = flattened.getChildren();
            for(Expression i : exprChildren){
                if(equivalenceCheck(i)){
                    check = true;
                    return check;
                }
            }
        } else {
            Expression<?>[] exprChildren = expr.getChildren();
            for(Expression i : exprChildren){
                if(equivalenceCheck(i)){
                    check = true;
                    return check;
                }
            }
        }
        return check;
    }

    public static boolean ifThenElseCheck(Expression<?> expr){
        boolean check = false;
        if(expr instanceof IfThenElse){
            check = true;
            return check;
        }
        if(expr instanceof LetExpression){
            Expression flattened = ((LetExpression) expr).flattenLetExpression();
            Expression<?>[] exprChildren = flattened.getChildren();
            for(Expression i : exprChildren){
                if(ifThenElseCheck(i)){
                    check = true;
                    return check;
                }
            }
        } else {
            Expression<?>[] exprChildren = expr.getChildren();
            for(Expression i : exprChildren){
                if(ifThenElseCheck(i)){
                    check = true;
                    return check;
                }
            }
        }
        return check;
    }

    public static boolean letExpressionCheck(Expression<?> expr){
        boolean check = false;
        if(expr instanceof LetExpression){
            check = true;
            return check;
        }

        Expression<?>[] exprChildren = expr.getChildren();
        for(Expression i : exprChildren){
            if(letExpressionCheck(i)){
                check = true;
                return check;
            }
        }
        return check;
    }

    //stepcounter methods
    //TODO: anpassen an jeweiligen Schritt im Algorithmus (simplify und order fehlen)
    public static int countPushsIntoLogic(Expression e) {
        //make sure, that the same transformation as in reality is transformed
        Expression noLet = eliminateLetExpressions(e);
        Expression noEquivalence = eliminateEquivalence(noLet);
        Expression noImplication = eliminateImplication(noEquivalence);
        Expression noXOR = eliminateXOR(noImplication);
        Expression noIte = eliminateIfThenElse(noXOR);
        NegatingVisitor n = new NegatingVisitor();
        n.apply(noIte, false);
        int countNumBoolNegations = n.getCountLogicalNegations();
        /*int[] arr =  n.countNegationSteps(noIte);
        int countNumBoolNegations = arr[0];*/
        return countNumBoolNegations;
    }

    public static int countAllNegationPushs(Expression e) {
        //make sure, that the same transformation as in reality is transformed
        Expression noLet = eliminateLetExpressions(e);
        Expression noEquivalence = eliminateEquivalence(noLet);
        Expression noImplication = eliminateImplication(noEquivalence);
        Expression noXOR = eliminateXOR(noImplication);
        Expression noIte = eliminateIfThenElse(noXOR);
        NegatingVisitor n = new NegatingVisitor();
        n.apply(noIte, false);
        int countAllNegationPushs = n.getcountAllNegationPushs();
        /*int[] arr =  n.countNegationSteps(noIte);
        int countAllNegationPushs = arr[1];*/
        return countAllNegationPushs;
    }

    public static int countMiniScopingSteps(Expression e) {
        //make sure, that the same transformation as in reality is transformed
        Expression nnf = createNNF(e);
        int countMiniScopingSteps = 0;
        if(quantifierCheck(nnf)) {
            Expression renamed = renameAllBoundVars(nnf);
            //miniscoping is only senseful if there is at least an EXISTS after creation of nnf
            Expression beforeSkolemization;
            if (checkForExists(renamed)) {
                beforeSkolemization = miniScope(renamed);
                MiniScopingVisitor m = new MiniScopingVisitor();
                countMiniScopingSteps =  m.countMiniScopeSteps(beforeSkolemization);
            }
        }
        return countMiniScopingSteps;
    }

    public static int countMiniScopingOperationTransformations(Expression e) {
        //make sure, that the same transformation as in reality is transformed
        Expression nnf = createNNF(e);
        int operatorTransformations = 0;
        if(quantifierCheck(nnf)) {
            Expression renamed = renameAllBoundVars(nnf);
            //miniscoping is only senseful if there is at least an EXISTS after creation of nnf
            Expression beforeSkolemization;
            if (checkForExists(renamed)) {
                beforeSkolemization = miniScope(renamed);
                MiniScopingVisitor m = new MiniScopingVisitor();
                operatorTransformations =  m.countMiniScopeOperatorTransformations(beforeSkolemization);
            }
        }
        return operatorTransformations;
    }


    public static int countCNFSteps(Expression e) {
        //make sure, that the same transformation as in reality is transformed
        Expression nnf = createNNF(e);
        ConjunctionCreatorVisitor c = new ConjunctionCreatorVisitor();
        int countCNFSteps =  c.countCNFSteps(nnf);
        return countCNFSteps;
    }

    public static int countCNFStepsInMatrix(Expression e) {
        //make sure, that the same transformation as in reality is transformed
        Expression nnf = createNNF(e);
        Expression renamed = renameAllBoundVars(nnf);
        //miniscoping is only senseful if there is at least an EXISTS after creation of nnf
        Expression beforeSkolemization;
        if(checkForExists(renamed)){
            beforeSkolemization = miniScope(renamed);
        } else {
            beforeSkolemization = renamed;
        }
        Expression skolemized = skolemize(beforeSkolemization);
        Expression prenex = prenexing(skolemized);
        if(prenex instanceof QuantifierExpression){
            Expression body = ((QuantifierExpression) prenex).getBody();
            ConjunctionCreatorVisitor c = new ConjunctionCreatorVisitor();
            int countCNFSteps =  c.countCNFSteps(body);
            return countCNFSteps;
        } else {
            ConjunctionCreatorVisitor c = new ConjunctionCreatorVisitor();
            int countCNFSteps = c.countCNFSteps(prenex);
            return countCNFSteps;
        }
    }

    public static int countDNFSteps(Expression e) {
        Expression nnf = createNNF(e);
        DisjunctionCreatorVisitor d = new DisjunctionCreatorVisitor();
        int countDNFSteps =  d.countDNFSteps(nnf);
        return countDNFSteps;
    }
}
