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
import static org.testng.Assert.assertNotEquals;

public class SimplifyTest {

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
    Expression dis1 = ExpressionUtil.or(e1, e2, e1, e3, e1, e2);
    Expression con2 = ExpressionUtil.and(e1, e2, e1);
    Expression dis2 = PropositionalCompound.create(e1, LogicalOperator.OR, e2);
    Expression con3 = PropositionalCompound.create(e1, LogicalOperator.AND, e1);

    @Test(groups = {"normalization"})
    public void duplicatesTest1(){
        Expression simplified = NormalizationUtil.simplifyProblem(con3);
        Expression expected = e1;

        assertEquals(simplified, expected);
    }
    @Test(groups = {"normalization"})
    public void duplicatesTest2(){
        Expression simplified = NormalizationUtil.simplifyProblem(con2);
        Expression expected = ExpressionUtil.and(e1, e2);

        assertEquals(simplified, expected);
    }
    @Test(groups = {"normalization"})
    public void duplicatesTest3(){
        Expression simplified = NormalizationUtil.simplifyProblem(ExpressionUtil.and(e1, e2, e1, e3, e2));

        Expression expected = ExpressionUtil.and(e1, e2, e3);

        assertEquals(simplified, expected);
    }

    @Test(groups = {"normalization"})
    public void duplicatesTest4(){
        Expression simplified = NormalizationUtil.simplifyProblem(ExpressionUtil.or(e1, e2, e1, e3, e2));

        Expression expected = ExpressionUtil.or(e1, e2, e3);

        assertEquals(simplified, expected);
    }

    @Test(groups = {"normalization"})
    public void duplicatesTest5(){
        Expression simplified = NormalizationUtil.simplifyProblem(ExpressionUtil.or(
                ExpressionUtil.and(e2, e3, e1, e3),
                (ExpressionUtil.or(e1, e2, e1))));

        Expression expected = ExpressionUtil.or(
                ExpressionUtil.and(e2, e3, e1),
                (ExpressionUtil.or(e1, e2)));

        assertEquals(simplified, expected);
    }

    @Test(groups = {"normalization"})
    public void duplicatesTest6(){
        Expression simplified = NormalizationUtil.simplifyProblem(ExpressionUtil.or(
                ExpressionUtil.and(e2, e3, e1, e3),
                (ExpressionUtil.or(e1, e2, e1)),
                ExpressionUtil.and(e3, e2, e3, e2)));

        Expression expected = ExpressionUtil.or(
                ExpressionUtil.and(e2, e3, e1),
                (ExpressionUtil.or(e1, e2)),
                ExpressionUtil.and(e3, e2));

        assertEquals(simplified, expected);
    }

    @Test(groups = {"normalization"})
    public void duplicatesTest7(){
        Expression simplified = NormalizationUtil.simplifyProblem(ExpressionUtil.or(
                ExpressionUtil.and(e2, e3, e1, e3),
                ExpressionUtil.and(e3, e2, e2)));

        Expression expected = ExpressionUtil.or(
                ExpressionUtil.and(e2, e3, e1),
                ExpressionUtil.and(e3, e2));

        assertEquals(simplified, expected);
    }

    @Test(groups = {"normalization"})
    public void duplicatesTest8(){
        Expression simplified = NormalizationUtil.simplifyProblem(ExpressionUtil.and(
                ExpressionUtil.and(e2, e3, e1, e3),
                ExpressionUtil.or(e2, e3, e1, e1),
                ExpressionUtil.or(e2, e3, e2),
                ExpressionUtil.and(e3, e2, e1, e3)));

        Expression expected = ExpressionUtil.and(
                ExpressionUtil.and(e2, e3, e1),
                ExpressionUtil.or(e2, e3, e1),
                ExpressionUtil.or(e2, e3),
                ExpressionUtil.and(e3, e2, e1));

        assertEquals(simplified, expected);
    }

    @Test(groups = {"normalization"})
    //TODO: either test 9 (if not simplified twice) or test 11 fail
    public void duplicatesTest9(){
        Expression simplified1 = NormalizationUtil.simplifyProblem(ExpressionUtil.and(
                ExpressionUtil.or(e2, e3, e3, e2),
                ExpressionUtil.or(e2, e3)));

        Expression simplified2 = NormalizationUtil.simplifyProblem(simplified1);

        Expression expected = ExpressionUtil.or(e2, e3);

        assertEquals(simplified2, expected);
    }

    @Test(groups = {"normalization"})
    //TODO: either test 9 (if not simplified twice) or test 11 fail
    public void duplicatesTest10(){
        Expression simplified1 = NormalizationUtil.simplifyProblem(ExpressionUtil.and(
                ExpressionUtil.and(e2, e3, e3, e2),
                ExpressionUtil.and(e2, e3)));

        Expression simplified2 = NormalizationUtil.simplifyProblem(simplified1);

        Expression expected = ExpressionUtil.and(e2, e3);

        assertEquals(simplified1, expected);
    }

    @Test(groups = {"normalization"})
    //TODO: either tests 10 and 9 (if not simplified twice) or test 11 fail
    public void duplicatesTest11(){
        Expression simplified = NormalizationUtil.simplifyProblem(ExpressionUtil.and(
                ExpressionUtil.and(e2, e3),
                ExpressionUtil.and(e1, e2, e3)));

        Expression expected = ExpressionUtil.and(e2, e3, e1);

        assertEquals(simplified, expected);
    }

    @Test(groups = {"normalization"})
    public void duplicatesTest12(){
        Expression simplified = NormalizationUtil.simplifyProblem(ExpressionUtil.and(
                ExpressionUtil.or(e2, e3, ExpressionUtil.and(e3, e2)),
                ExpressionUtil.or(e2, e3, e2)));

        Expression expected = ExpressionUtil.and(
                ExpressionUtil.or(e2, e3, ExpressionUtil.and(e3, e2)),
                ExpressionUtil.or(e2, e3));

        assertEquals(simplified, expected);
    }
}


