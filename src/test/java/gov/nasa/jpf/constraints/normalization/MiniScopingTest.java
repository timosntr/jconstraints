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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

public class MiniScopingTest {

    Variable<Integer> x = Variable.create(BuiltinTypes.SINT32, "x");
    Variable<Integer> y = Variable.create(BuiltinTypes.SINT32, "y");
    Variable<Boolean> b1 = Variable.create(BuiltinTypes.BOOL, "b1");
    Variable<Boolean> b2 = Variable.create(BuiltinTypes.BOOL, "b2");

    Constant<Integer> c1 = Constant.create(BuiltinTypes.SINT32, 1);
    Constant<Integer> c2 = Constant.create(BuiltinTypes.SINT32, 2);

    Expression e1 = NumericBooleanExpression.create(x, NumericComparator.LT, c1);
    Expression e2 = NumericBooleanExpression.create(y, NumericComparator.LE, c2);
    Expression e3 = NumericBooleanExpression.create(x, NumericComparator.GE, c1);
    Expression e4 = NumericBooleanExpression.create(y, NumericComparator.EQ, c2);

    Expression con1 = PropositionalCompound.create(e3, LogicalOperator.AND, e4);
    Expression dis1 = PropositionalCompound.create(e2, LogicalOperator.OR, e4);
    Expression con2 = PropositionalCompound.create(e1, LogicalOperator.AND, e2);
    Expression dis2 = PropositionalCompound.create(e1, LogicalOperator.OR, e2);
    Expression dis3 = PropositionalCompound.create(e1, LogicalOperator.OR, con2);

    Expression disjunction = PropositionalCompound.create(
            PropositionalCompound.create(e3, LogicalOperator.AND, e4),
            LogicalOperator.OR,
            b1);
    Expression expected = PropositionalCompound.create(
            PropositionalCompound.create(e3, LogicalOperator.OR, b1),
            LogicalOperator.AND,
            PropositionalCompound.create(e4, LogicalOperator.OR, b1));
    Expression nestedDisjunction = PropositionalCompound.create(e3, LogicalOperator.OR, disjunction);

    @Test(groups = {"normalization"})
    //free vars left
    public void leftTest(){
        List<Variable<?>> bound = new ArrayList<>();
        bound.add(x);
        Expression q = ExpressionUtil.or(e1,
                QuantifierExpression.create(Quantifier.FORALL, bound, ExpressionUtil.or(e2, e1)));

        Expression<Boolean> minimized = (Expression<Boolean>) q.accept(MiniScopingVisitor.getInstance(), null);
        System.out.println(minimized);
    }

    @Test(groups = {"normalization"})
    //free vars right
    public void rightTest(){
        List<Variable<?>> bound = new ArrayList<>();
        bound.add(x);
        Expression q = ExpressionUtil.or(QuantifierExpression.create(Quantifier.FORALL, bound, ExpressionUtil.and(e1, e2)), dis1);

        Expression<Boolean> minimized = (Expression<Boolean>) q.accept(MiniScopingVisitor.getInstance(), null);
        System.out.println(minimized);
    }

    @Test(groups = {"normalization"})
    //free vars in both
    public void existsTest(){
        List<Variable<?>> bound = new ArrayList<>();
        bound.add(x);
        Expression q = QuantifierExpression.create(Quantifier.EXISTS, bound, ExpressionUtil.and(ExpressionUtil.or(e1, e2), con1));

        Expression<Boolean> minimized = (Expression<Boolean>) q.accept(MiniScopingVisitor.getInstance(), null);
        System.out.println(q);
        System.out.println(minimized);
    }

    @Test(groups = {"normalization"})
    //free vars in both
    public void forallTest(){
        List<Variable<?>> bound = new ArrayList<>();
        bound.add(x);
        Expression q = QuantifierExpression.create(Quantifier.FORALL, bound, ExpressionUtil.or(ExpressionUtil.and(e1, e2), con1));

        Expression<Boolean> minimized = (Expression<Boolean>) q.accept(MiniScopingVisitor.getInstance(), null);
        System.out.println(q);
        System.out.println(minimized);
    }

    @Test(groups = {"normalization"})
    //free vars in both
    public void multipleQuantifierTest(){
        List<Variable<?>> bound1 = new ArrayList<>();
        bound1.add(x);
        List<Variable<?>> bound2 = new ArrayList<>();
        bound2.add(y);
        Expression q = QuantifierExpression.create(Quantifier.EXISTS, bound1,
                QuantifierExpression.create(Quantifier.FORALL, bound2, ExpressionUtil.and(ExpressionUtil.or(e1, e2), con1)));

        Expression<Boolean> minimized = (Expression<Boolean>) q.accept(MiniScopingVisitor.getInstance(), null);
        System.out.println(q);
        System.out.println(minimized);
    }

    @Test(groups = {"normalization"})
    //free vars in both
    public void notMixedQuantifierTest1(){
        List<Variable<?>> bound1 = new ArrayList<>();
        bound1.add(x);
        List<Variable<?>> bound2 = new ArrayList<>();
        bound2.add(y);
        Expression q = QuantifierExpression.create(Quantifier.FORALL, bound1, ExpressionUtil.and(e1, QuantifierExpression.create(Quantifier.FORALL, bound2, ExpressionUtil.and(e1, e2))));

        Expression<Boolean> minimized = (Expression<Boolean>) q.accept(MiniScopingVisitor.getInstance(), null);
        System.out.println(q);
        System.out.println(minimized);
    }

    @Test(groups = {"normalization"})
    //gemischte quantifier dürfen einander nicht überspringen
    public void mixedQuantifierTest1(){
        List<Variable<?>> bound1 = new ArrayList<>();
        bound1.add(x);
        List<Variable<?>> bound2 = new ArrayList<>();
        bound2.add(y);
        Expression q = QuantifierExpression.create(Quantifier.FORALL, bound1, PropositionalCompound.create(e1, LogicalOperator.AND, QuantifierExpression.create(Quantifier.EXISTS, bound2, PropositionalCompound.create(e1, LogicalOperator.AND, e2))));

        Expression<Boolean> minimized = (Expression<Boolean>) q.accept(MiniScopingVisitor.getInstance(), null);
        System.out.println(q);
        System.out.println(minimized);
    }
}


