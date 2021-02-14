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

import static org.testng.Assert.assertEquals;

public class OrderingTest {

    Variable<Integer> x = Variable.create(BuiltinTypes.SINT32, "x");
    Variable<Integer> y = Variable.create(BuiltinTypes.SINT32, "y");
    Variable<Boolean> b1 = Variable.create(BuiltinTypes.BOOL, "b1");
    Variable<Boolean> b2 = Variable.create(BuiltinTypes.BOOL, "b2");

    Constant<Integer> c1 = Constant.create(BuiltinTypes.SINT32, 1);
    Constant<Integer> c2 = Constant.create(BuiltinTypes.SINT32, 2);
    NumericCompound z = NumericCompound.create(c1, NumericOperator.MUL, x);
    NumericCompound zOrdered = NumericCompound.create(x, NumericOperator.MUL, c1);
    NumericCompound a = NumericCompound.create(c1, NumericOperator.MINUS, x);
    NumericCompound d = NumericCompound.create(c1, NumericOperator.DIV, z);

    Expression e1 = NumericBooleanExpression.create(c1, NumericComparator.NE, x);
    Expression e1ordered = NumericBooleanExpression.create(x, NumericComparator.NE, c1);
    Expression e2 = NumericBooleanExpression.create(y, NumericComparator.LE, c2);
    Expression e3 = NumericBooleanExpression.create(c1, NumericComparator.GE, x);
    Expression e4 = NumericBooleanExpression.create(y, NumericComparator.EQ, c2);
    Expression e5 = NumericBooleanExpression.create(c1, NumericComparator.LE, z);
    Expression e6 = NumericBooleanExpression.create(c1, NumericComparator.LT, a);
    Expression e7 = NumericBooleanExpression.create(c1, NumericComparator.GT, z);
    Expression e8 = NumericBooleanExpression.create(c2, NumericComparator.EQ, y);

    Expression con1 = PropositionalCompound.create(e3, LogicalOperator.AND, e4);
    Expression dis2 = PropositionalCompound.create(e1, LogicalOperator.OR, e2);
    Expression con3 = PropositionalCompound.create(e1, LogicalOperator.AND, e1);
    Expression con4 = PropositionalCompound.create(e2, LogicalOperator.AND, b1);
    Expression imply = PropositionalCompound.create(e2, LogicalOperator.IMPLY, b1);

    @Test(groups = {"normalization"})
    //LE
    public void orderingTest1(){
        Expression ordered = NormalizationUtil.orderProblem(e2);

        assertEquals(ordered, e2);
    }

    @Test(groups = {"normalization"})
    //LE
    public void orderingTest2(){
        Expression ordered = NormalizationUtil.orderProblem(e5);
        NumericCompound zOrdered = NumericCompound.create(x, NumericOperator.MUL, c1);
        Expression expected = NumericBooleanExpression.create(zOrdered, NumericComparator.GT, c1);

        assertEquals(ordered, expected);
    }

    @Test(groups = {"normalization"})
    //GE
    public void orderingTest3(){
        Expression ordered = NormalizationUtil.orderProblem(NumericBooleanExpression.create(x, NumericComparator.GE, c1));
        Expression expected = NumericBooleanExpression.create(x, NumericComparator.GE, c1);

        assertEquals(ordered, expected);
    }

    @Test(groups = {"normalization"})
    //GE
    public void orderingTest4(){
        Expression ordered = NormalizationUtil.orderProblem(con1);
        Expression expected = PropositionalCompound.create(NumericBooleanExpression.create(x, NumericComparator.LT, c1), LogicalOperator.AND, e4);

        assertEquals(ordered, expected);
    }

    @Test(groups = {"normalization"})
    //NE
    public void orderingTest5(){
        Expression ordered = NormalizationUtil.orderProblem(dis2);
        Expression expected = PropositionalCompound.create(NumericBooleanExpression.create(x, NumericComparator.NE, c1), LogicalOperator.OR, e2);

        assertEquals(ordered, expected);
    }

    @Test(groups = {"normalization"})
    //NE
    public void orderingTest6(){
        Expression ordered = NormalizationUtil.orderProblem(con3);
        Expression expected = PropositionalCompound.create(e1ordered, LogicalOperator.AND, e1ordered);

        assertEquals(ordered, expected);
    }

    @Test(groups = {"normalization"})
    //EQ
    public void orderingTest7(){
        Expression ordered = NormalizationUtil.orderProblem(e8);

        assertEquals(ordered, e4);
    }

    @Test(groups = {"normalization"})
    //EQ
    public void orderingTest8(){
        Expression ordered = NormalizationUtil.orderProblem(e4);

        assertEquals(ordered, e4);
    }

    @Test(groups = {"normalization"})
    //LT
    public void orderingTest9(){
        Expression ordered = NormalizationUtil.orderProblem(e6);
        Expression expected = NumericBooleanExpression.create(a, NumericComparator.GE, c1);
        assertEquals(ordered, expected);
    }

    @Test(groups = {"normalization"})
    //LT
    public void orderingTest10(){
        Expression ordered = NormalizationUtil.orderProblem(NumericBooleanExpression.create(a, NumericComparator.GE, c1));
        Expression expected = NumericBooleanExpression.create(a, NumericComparator.GE, c1);
        assertEquals(ordered, expected);
    }

    @Test(groups = {"normalization"})
    //GT
    public void orderingTest11(){
        Expression ordered = NormalizationUtil.orderProblem(e7);
        Expression expected = NumericBooleanExpression.create(zOrdered, NumericComparator.LE, c1);
        assertEquals(ordered, expected);
    }

    @Test(groups = {"normalization"})
    //GT
    public void orderingTest12(){
        Expression ordered = NormalizationUtil.orderProblem(NumericBooleanExpression.create(zOrdered, NumericComparator.LE, c1));
        Expression expected = NumericBooleanExpression.create(zOrdered, NumericComparator.LE, c1);
        assertEquals(ordered, expected);
    }

    @Test(groups = {"normalization"})
    public void orderingTest13(){
        Expression ordered = NormalizationUtil.orderProblem(con4);
        Expression expected = PropositionalCompound.create(b1, LogicalOperator.AND, e2);

        assertEquals(ordered, expected);
    }

    @Test(groups = {"normalization"})
    public void orderingTest14(){
        Expression ordered = NormalizationUtil.orderProblem(imply);

        assertEquals(ordered, imply);
    }

    @Test(groups = {"normalization"})
    public void orderingTest15(){
        Expression ordered = NormalizationUtil.orderProblem(d);
        Expression expected = NumericCompound.create(c1, NumericOperator.DIV, zOrdered);

        assertEquals(ordered, expected);
    }

}


