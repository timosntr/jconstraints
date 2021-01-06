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
import gov.nasa.jpf.constraints.expressions.*;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.testng.Assert.assertEquals;


public class RenameBoundVarTest {

    Variable<Integer> x = Variable.create(BuiltinTypes.SINT32, "x");
    Variable<Integer> y = Variable.create(BuiltinTypes.SINT32, "y");
    Variable<Integer> y2 = Variable.create(BuiltinTypes.SINT32, "Q.1.x");
    Variable<Integer> z = Variable.create(BuiltinTypes.SINT32, "z");
    Constant<Integer> c1 = Constant.create(BuiltinTypes.SINT32, 1);
    Constant<Integer> c2 = Constant.create(BuiltinTypes.SINT32, 2);
    Expression e1 = NumericBooleanExpression.create(x, NumericComparator.LT, c1);
    Expression e2 = NumericBooleanExpression.create(y, NumericComparator.GE, c1);
    Expression e2mod = NumericBooleanExpression.create(y2, NumericComparator.GE, c1);


    @Test(groups = {"normalization"})
    //works
    public void simpleRenamingTest() {
        Function<String, String> data = null;
        List<Variable<?>> bound = new ArrayList<>();
        bound.add(x);
        Expression q = QuantifierExpression.create(Quantifier.FORALL, bound, ExpressionUtil.and(e1,e2));
        Expression<Boolean> renamed = (Expression<Boolean>) q.accept(RenameBoundVarVisitor.getInstance(), data);
        System.out.println(renamed);
    }

    @Test(groups = {"normalization"})
    //works
    public void nestedRenamingTest1() {
        Function<String, String> data = null;
        List<Variable<?>> bound = new ArrayList<>();
        bound.add(x);
        Expression q = ExpressionUtil.or(e1,
                QuantifierExpression.create(Quantifier.FORALL, bound, e1));

        Expression<Boolean> renamed = (Expression<Boolean>) q.accept(RenameBoundVarVisitor.getInstance(), data);
        System.out.println(renamed);
    }

    @Test(groups = {"normalization"})
    //works
    public void nestedRenamingTest2() {
        Function<String, String> data = null;
        List<Variable<?>> bound = new ArrayList<>();
        bound.add(x);
        Expression q = ExpressionUtil.or(e1, ExpressionUtil.and(
                QuantifierExpression.create(Quantifier.FORALL, bound, e1)),
                QuantifierExpression.create(Quantifier.EXISTS, bound, e1));

        Expression<Boolean> renamed = (Expression<Boolean>) q.accept(RenameBoundVarVisitor.getInstance(), data);
        System.out.println(renamed);
    }

    @Test(groups = {"normalization"})
    //works
    public void freeVarsTest() {
        Function<String, String> data = null;
        List<Variable<?>> bound = new ArrayList<>();
        bound.add(x);
        Expression q = ExpressionUtil.or(e1, ExpressionUtil.and(
                QuantifierExpression.create(Quantifier.FORALL, bound, e1)),
                ExpressionUtil.or(e2mod, e1));

        Expression<Boolean> renamed = (Expression<Boolean>) q.accept(RenameBoundVarVisitor.getInstance(), data);
        System.out.println(renamed);
    }

    @Test(groups = {"normalization"})
    //works
    public void nestedQuantifierTest() {
        Function<String, String> data = null;
        List<Variable<?>> bound1 = new ArrayList<>();
        bound1.add(x);
        List<Variable<?>> bound2 = new ArrayList<>();
        bound2.add(x);
        bound2.add(y);

        Expression<Boolean> allBound = QuantifierExpression.create(Quantifier.EXISTS, bound1,
                ExpressionUtil.or(e1, QuantifierExpression.create(Quantifier.FORALL, bound2, ExpressionUtil.and(e1, e2))));
        Expression<Boolean> renamed = (Expression<Boolean>) allBound.accept(RenameBoundVarVisitor.getInstance(), data);

        System.out.println(renamed);
    }

    @Test(groups = {"normalization"})
    //todo
    public void freeVarInOtherPathTest() {
        Function<String, String> data = null;
        List<Variable<?>> bound1 = new ArrayList<>();
        bound1.add(x);
        List<Variable<?>> bound2 = new ArrayList<>();
        bound2.add(x);
        bound2.add(y);

        Expression<Boolean> allBound = ExpressionUtil.and(e2mod, QuantifierExpression.create(Quantifier.EXISTS, bound1,
                ExpressionUtil.or(e1, QuantifierExpression.create(Quantifier.FORALL, bound2, ExpressionUtil.and(e1, e2)))));
        Expression<Boolean> renamed = (Expression<Boolean>) allBound.accept(RenameBoundVarVisitor.getInstance(), data);

        System.out.println(allBound);
        System.out.println(renamed);
    }
}
