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
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class RenamingBoundVarTest {

    Variable<Integer> x = Variable.create(BuiltinTypes.SINT32, "x");
    Variable<Integer> y = Variable.create(BuiltinTypes.SINT32, "y");
    Variable<Integer> y2 = Variable.create(BuiltinTypes.SINT32, "Q.1.x");
    Variable<Integer> z = Variable.create(BuiltinTypes.SINT32, "z");
    Constant<Integer> c1 = Constant.create(BuiltinTypes.SINT32, 1);
    Expression e1 = NumericBooleanExpression.create(x, NumericComparator.LT, c1);
    Expression e2 = NumericBooleanExpression.create(y, NumericComparator.GE, c1);
    Expression e2mod = NumericBooleanExpression.create(y2, NumericComparator.GE, c1);
    Expression e3 = NumericCompound.create(x, NumericOperator.PLUS, c1);
    Expression e4 = NumericBooleanExpression.create(z, NumericComparator.LT, c1);


    @Test(groups = {"normalization"})
    //works
    public void simpleRenamingTest() {
        HashMap<String, String> data = new HashMap<>();
        List<Variable<?>> bound = new ArrayList<>();
        bound.add(x);
        Expression q = QuantifierExpression.create(Quantifier.FORALL, bound, ExpressionUtil.and(e1,e2));
        Expression<Boolean> renamed = (Expression<Boolean>) q.accept(RenamingBoundVarVisitor.getInstance(), data);
        System.out.println(q);
        System.out.println(renamed);
    }

    @Test(groups = {"normalization"})
    //works
    public void nestedRenamingTest1() {
        HashMap<String, String> data = new HashMap<>();
        List<Variable<?>> freeVars = new ArrayList<>();
        List<Variable<?>> bound = new ArrayList<>();
        bound.add(x);
        Expression q = ExpressionUtil.or(e1,
                QuantifierExpression.create(Quantifier.FORALL, bound, e1));

        Expression<Boolean> renamed = (Expression<Boolean>) q.accept(RenamingBoundVarVisitor.getInstance(), data);

        q.collectFreeVariables(freeVars);
        System.out.println("FreeVars in q: " + freeVars);
        System.out.println(q);
        freeVars.removeAll(freeVars);
        renamed.collectFreeVariables(freeVars);
        System.out.println("FreeVars in renamed: " + freeVars);
        System.out.println(renamed);
    }

    @Test(groups = {"normalization"})
    //works
    public void nestedRenamingTest2() {
        HashMap<String, String> data = new HashMap<>();
        List<Variable<?>> bound = new ArrayList<>();
        bound.add(x);
        Expression q = ExpressionUtil.or(e1, PropositionalCompound.create(
                QuantifierExpression.create(Quantifier.FORALL, bound, e1),
                LogicalOperator.AND,
                QuantifierExpression.create(Quantifier.EXISTS, bound, e1)));

        Expression<Boolean> renamed = (Expression<Boolean>) q.accept(RenamingBoundVarVisitor.getInstance(), data);
        System.out.println(q);
        System.out.println(renamed);
    }

    @Test(groups = {"normalization"})
    //works
    public void nestedRenamingTest3() {
        HashMap<String, String> data = new HashMap<>();
        List<Variable<?>> bound = new ArrayList<>();
        bound.add(x);
        Expression q = ExpressionUtil.or(ExpressionUtil.and(
                QuantifierExpression.create(Quantifier.FORALL, bound, e1)),
                QuantifierExpression.create(Quantifier.EXISTS, bound, e1), e1);

        Expression<Boolean> renamed = (Expression<Boolean>) q.accept(RenamingBoundVarVisitor.getInstance(), data);
        System.out.println(q);
        System.out.println(renamed);
    }

    @Test(groups = {"normalization"})
    //works
    public void freeVarsTest() {
        HashMap<String, String> data = new HashMap<>();
        List<Variable<?>> bound = new ArrayList<>();
        bound.add(x);
        Expression q = ExpressionUtil.or(e1, ExpressionUtil.and(
                QuantifierExpression.create(Quantifier.FORALL, bound, e1)),
                ExpressionUtil.or(e2mod, e1));

        Expression<Boolean> renamed = (Expression<Boolean>) q.accept(RenamingBoundVarVisitor.getInstance(), data);
        System.out.println(q);
        System.out.println(renamed);
    }

    @Test(groups = {"normalization"})
    //works
    public void nestedQuantifierTest() {
        HashMap<String, String> data = new HashMap<>();
        List<Variable<?>> bound1 = new ArrayList<>();
        bound1.add(x);
        List<Variable<?>> bound2 = new ArrayList<>();
        bound2.add(x);
        bound2.add(y);

        Expression<Boolean> allBound = QuantifierExpression.create(Quantifier.EXISTS, bound1,
                ExpressionUtil.or(e1, QuantifierExpression.create(Quantifier.FORALL, bound2, ExpressionUtil.and(e1, e2))));
        Expression<Boolean> renamed = (Expression<Boolean>) allBound.accept(RenamingBoundVarVisitor.getInstance(), data);

        System.out.println(allBound);
        System.out.println(renamed);
    }

    @Test(groups = {"normalization"})
    //works
    public void freeVarInOtherPathTest() {
        HashMap<String, String> data = new HashMap<>();
        List<Variable<?>> bound1 = new ArrayList<>();
        bound1.add(x);
        List<Variable<?>> bound2 = new ArrayList<>();
        bound2.add(x);
        bound2.add(y);

        Expression<Boolean> allBound = ExpressionUtil.and(e2mod, QuantifierExpression.create(Quantifier.EXISTS, bound1,
                ExpressionUtil.or(e1, QuantifierExpression.create(Quantifier.FORALL, bound2, ExpressionUtil.and(e1, e2)))));
        Expression<Boolean> renamed = (Expression<Boolean>) allBound.accept(RenamingBoundVarVisitor.getInstance(), data);

        System.out.println(allBound);
        System.out.println(renamed);
    }

    @Test(groups = {"normalization"})
    //works
    public void renamingProblemTest() {
        HashMap<String, String> data = new HashMap<>();
        List<Variable<?>> bound1 = new ArrayList<>();
        bound1.add(x);
        bound1.add(y);
        List<Variable<?>> bound2 = new ArrayList<>();
        bound2.add(x);

        Expression<Boolean> allBound = (QuantifierExpression.create(Quantifier.EXISTS, bound1,
                ExpressionUtil.or(e1, QuantifierExpression.create(Quantifier.FORALL, bound2, ExpressionUtil.and(e1, e2)))));
        Expression<Boolean> renamed = (Expression<Boolean>) allBound.accept(RenamingBoundVarVisitor.getInstance(), data);

        System.out.println(allBound);
        System.out.println(renamed);
    }

    @Test(groups = {"normalization"})
    //works
    public void renamingProblemTest2() {
        HashMap<String, String> data = new HashMap<>();
        List<Variable<?>> bound1 = new ArrayList<>();
        bound1.add(x);
        bound1.add(y);
        List<Variable<?>> bound2 = new ArrayList<>();
        bound2.add(x);
        List<Variable<?>> bound3 = new ArrayList<>();
        bound3.add(y);

        Expression<Boolean> allBound = (QuantifierExpression.create(Quantifier.EXISTS, bound1,
                ExpressionUtil.or(e1, QuantifierExpression.create(Quantifier.FORALL, bound2,
                        ExpressionUtil.and(e1, ExpressionUtil.or(QuantifierExpression.create(Quantifier.FORALL, bound3, e2), e2))))));
        Expression<Boolean> renamed = (Expression<Boolean>) allBound.accept(RenamingBoundVarVisitor.getInstance(), data);

        System.out.println(allBound);
        System.out.println(renamed);
    }

    @Test(groups = {"normalization"})
    //works
    public void renamingLetText() {
        HashMap<String, String> data = new HashMap<>();
        List<Variable<?>> bound1 = new ArrayList<>();
        bound1.add(x);
        bound1.add(y);

        Expression<Boolean> allBound = QuantifierExpression.create(Quantifier.EXISTS, bound1, ExpressionUtil.or(e1, LetExpression.create(z, e3, e4)));
        Expression<Boolean> renamed = (Expression<Boolean>) allBound.accept(RenamingBoundVarVisitor.getInstance(), data);

        System.out.println(allBound);
        System.out.println(renamed);
    }
}
