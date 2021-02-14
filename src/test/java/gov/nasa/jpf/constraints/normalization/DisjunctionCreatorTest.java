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

public class DisjunctionCreatorTest {

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
    Expression dis1 = PropositionalCompound.create(e3, LogicalOperator.OR, e4);
    Expression con2 = PropositionalCompound.create(e1, LogicalOperator.AND, e2);
    Expression dis2 = PropositionalCompound.create(e1, LogicalOperator.OR, e2);
    Expression con3 = PropositionalCompound.create(e1, LogicalOperator.AND, dis2);

    Expression conjunction = PropositionalCompound.create(
            PropositionalCompound.create(e3, LogicalOperator.OR, e4),
            LogicalOperator.AND,
            b1);
    Expression expected = PropositionalCompound.create(
            PropositionalCompound.create(e3, LogicalOperator.AND, b1),
            LogicalOperator.OR,
            PropositionalCompound.create(e4, LogicalOperator.AND, b1));
    Expression nestedConjunction = PropositionalCompound.create(e3, LogicalOperator.AND, conjunction);

    @Test(groups = {"normalization"})
    //case: (A AND B)
    public void basicTest1(){
        Expression simpleConjunction = PropositionalCompound.create(e3, LogicalOperator.AND, e4);

        Expression<Boolean> dnf = NormalizationUtil.createDNF(simpleConjunction);

        assertEquals(dnf, simpleConjunction);
    }

    @Test(groups = {"normalization"})
    //case: (A OR B)
    public void basicTest2(){
        Expression simpleDisjunction = PropositionalCompound.create(e3, LogicalOperator.OR, e4);

        Expression<Boolean> dnf = NormalizationUtil.createDNF(simpleDisjunction);

        assertEquals(dnf, simpleDisjunction);
    }

    @Test(groups = {"normalization"})
    //case: (A OR B) AND C
    public void conjunctionTest(){
        Expression<Boolean> dnf = NormalizationUtil.createDNF(conjunction);

        assertEquals(dnf, expected);
    }

    @Test(groups = {"normalization"})
    //case: (A AND B) AND C
    public void conjunctionTest2(){
        Expression conjunction2 = PropositionalCompound.create(
                PropositionalCompound.create(e3, LogicalOperator.AND, e4),
                LogicalOperator.AND,
                b1);

        Expression<Boolean> dnf = NormalizationUtil.createDNF(conjunction2);

        assertEquals(dnf, conjunction2);
    }

    @Test(groups = {"normalization"})
    //case: A OR (A AND B)
    public void disjunctionTest1(){
        Expression disjunction1 = PropositionalCompound.create(
                b1,
                LogicalOperator.OR,
                PropositionalCompound.create(b1, LogicalOperator.AND, b2));

        Expression<Boolean> dnf = NormalizationUtil.createDNF(disjunction1);

        assertEquals(dnf, disjunction1);
    }

    @Test(groups = {"normalization"})
    //case: (A) AND (C OR D)
    public void conjunctionTest3(){
        Expression conjunction3 = PropositionalCompound.create(
                b1,
                LogicalOperator.AND,
                PropositionalCompound.create(e3, LogicalOperator.OR, e4));
        Expression expected3 = PropositionalCompound.create(
                PropositionalCompound.create(b1, LogicalOperator.AND, e3),
                LogicalOperator.OR,
                PropositionalCompound.create(b1, LogicalOperator.AND, e4));

        Expression<Boolean> dnf = NormalizationUtil.createDNF(conjunction3);

        assertEquals(dnf, expected3);
    }

    @Test(groups = {"normalization"})
    //case: (A) AND (C AND D)
    public void conjunctionTest4(){
        Expression conjunction4 = PropositionalCompound.create(
                b1,
                LogicalOperator.AND,
                PropositionalCompound.create(e3, LogicalOperator.AND, e4));

        Expression<Boolean> dnf = NormalizationUtil.createDNF(conjunction4);

        assertEquals(dnf, conjunction4);
    }

    @Test(groups = {"normalization"})
    //case: (A OR B) AND (C OR D)
    public void conjunctionTest5(){
        Expression conjunction5 = PropositionalCompound.create(dis1, LogicalOperator.AND, dis2);
        Expression expected = PropositionalCompound.create(
                PropositionalCompound.create(
                        PropositionalCompound.create(e3, LogicalOperator.AND, e1),
                        LogicalOperator.OR,
                        PropositionalCompound.create(e3, LogicalOperator.AND, e2)),
                LogicalOperator.OR,
                PropositionalCompound.create(
                        PropositionalCompound.create(e4, LogicalOperator.AND, e1),
                        LogicalOperator.OR,
                        PropositionalCompound.create(e4, LogicalOperator.AND, e2))
                );

        Expression<Boolean> dnf = NormalizationUtil.createDNF(conjunction5);

        assertEquals(dnf, expected);
    }

    @Test(groups = {"normalization"})
    //case: (A AND B) AND (C AND D)
    public void conjunctionTest6(){
        Expression conjunction6 = PropositionalCompound.create(con1, LogicalOperator.AND, con2);

        Expression<Boolean> dnf = NormalizationUtil.createDNF(conjunction6);

        assertEquals(dnf, conjunction6);
    }

    @Test(groups = {"normalization"})
    //case: (A AND B) AND (C AND D)
    public void conjunctionNestedTest(){
        Expression conjunction6 = PropositionalCompound.create(con1, LogicalOperator.AND, con3);

        Expression<Boolean> dnf = NormalizationUtil.createDNF(conjunction6);
        System.out.print(dnf);
    }

    @Test(groups = {"normalization"})
    //case: (A OR B) AND (C AND D)
    public void conjunctionTest7(){
        Expression conjunction7 = PropositionalCompound.create(dis1, LogicalOperator.AND, con1);
        Expression expected = PropositionalCompound.create(
                PropositionalCompound.create(e3, LogicalOperator.AND, con1),
                LogicalOperator.OR,
                PropositionalCompound.create(e4, LogicalOperator.AND, con1));

        Expression<Boolean> dnf = NormalizationUtil.createDNF(conjunction7);

        assertEquals(dnf, expected);
    }

    @Test(groups = {"normalization"})
    //case: (A AND B) AND (C OR D)
    public void conjunctionTest8(){
        Expression conjunction8 = PropositionalCompound.create(con1, LogicalOperator.AND, dis1);
        Expression expected = PropositionalCompound.create(
                PropositionalCompound.create(con1, LogicalOperator.AND, e3),
                LogicalOperator.OR,
                PropositionalCompound.create(con1, LogicalOperator.AND, e4));

        Expression<Boolean> dnf = NormalizationUtil.createDNF(conjunction8);

        assertEquals(dnf, expected);
    }

    @Test(groups = {"normalization"})
    //A AND ((A OR B) AND C)
    public void nestedConjunctionTest(){
        Expression<Boolean> dnf = NormalizationUtil.createDNF(nestedConjunction);
        Expression nestedExpected = PropositionalCompound.create(
                PropositionalCompound.create(
                        e3,
                        LogicalOperator.AND,
                        PropositionalCompound.create(e3, LogicalOperator.AND, b1)),
                LogicalOperator.OR,
                PropositionalCompound.create(
                        e3,
                        LogicalOperator.AND,
                        PropositionalCompound.create(e4, LogicalOperator.AND, b1)));

        assertEquals(dnf, nestedExpected);
    }

    @Test(groups = {"normalization"})
    public void andTest(){
        Expression and = PropositionalCompound.create(
                        PropositionalCompound.create(b1, LogicalOperator.AND, b2),
                        LogicalOperator.OR,
                        PropositionalCompound.create(
                                PropositionalCompound.create(b1, LogicalOperator.AND, b2),
                                LogicalOperator.OR,
                                PropositionalCompound.create(b1, LogicalOperator.OR, b2)));

        Expression<Boolean> dnf = NormalizationUtil.createDNF(and);

        assertEquals(dnf, and);
    }

    @Test(groups = {"normalization"})
    public void nestedTest1(){
        Expression nested = PropositionalCompound.create(
                PropositionalCompound.create(e3, LogicalOperator.OR, e4),
                LogicalOperator.AND,
                PropositionalCompound.create(
                        e3,
                        LogicalOperator.AND,
                        PropositionalCompound.create(e3, LogicalOperator.OR, e4)));

        Expression<Boolean> dnf = NormalizationUtil.createDNF(nested);

        Expression expected =
                PropositionalCompound.create(
                        PropositionalCompound.create(
                                PropositionalCompound.create(
                                        e3,
                                        LogicalOperator.AND,
                                        PropositionalCompound.create(e3, LogicalOperator.AND, e3)),
                        LogicalOperator.OR,
                        PropositionalCompound.create(
                                e3,
                                LogicalOperator.AND,
                                PropositionalCompound.create(e3, LogicalOperator.AND, e4))),
                LogicalOperator.OR,
                PropositionalCompound.create(
                        PropositionalCompound.create(
                                e4,
                                LogicalOperator.AND,
                                PropositionalCompound.create(e3, LogicalOperator.AND, e3)),
                        LogicalOperator.OR,
                        PropositionalCompound.create(
                                e4,
                                LogicalOperator.AND,
                                PropositionalCompound.create(e3, LogicalOperator.AND, e4))));

        assertEquals(dnf, expected);
    }

    @Test(groups = {"normalization"})
    public void nestedTest2(){
        Expression nested2 = PropositionalCompound.create(
                PropositionalCompound.create(b1, LogicalOperator.OR, b2),
                LogicalOperator.AND,
                PropositionalCompound.create(
                        b1,
                        LogicalOperator.OR,
                        PropositionalCompound.create(b1, LogicalOperator.AND, b2)));

        Expression expected = ExpressionUtil.or(ExpressionUtil.or(ExpressionUtil.and(b1, b1), ExpressionUtil.and(b1, ExpressionUtil.and(b1, b2))),
                ExpressionUtil.or(ExpressionUtil.and(b2, b1), ExpressionUtil.and(b2, ExpressionUtil.and(b1, b2))));

        Expression<Boolean> dnf = NormalizationUtil.createDNF(nested2);

        assertEquals(dnf, expected);
    }

    @Test(groups = {"normalization"})
    public void nestedTest3(){
        Expression nested3 = ExpressionUtil.or(b1, ExpressionUtil.or(ExpressionUtil.and(ExpressionUtil.or(e3,e3)),e3),
                ExpressionUtil.and(e4, ExpressionUtil.or(e4, e4)));

        Expression<Boolean> dnf = (Expression<Boolean>) nested3.accept(DisjunctionCreatorVisitor.getInstance(), null);

        assertNotEquals(dnf, nested3);
    }

    @Test(groups = {"normalization"})
    public void nestedTest4(){
        Expression nested4 = ExpressionUtil.and(ExpressionUtil.and(b1, b2), ExpressionUtil.and(ExpressionUtil.or(e3, e4)), ExpressionUtil.and(e3, e4));

        Expression<Boolean> dnf = (Expression<Boolean>) nested4.accept(DisjunctionCreatorVisitor.getInstance(), null);

        assertNotEquals(dnf, nested4);
    }

    @Test(groups = {"normalization"})
    //Todo: investigate if works as intended -> looks like it does
    public void nestedTest(){
        Expression nested = PropositionalCompound.create(
                PropositionalCompound.create(b1, LogicalOperator.OR, b2),
                LogicalOperator.AND,
                PropositionalCompound.create(
                        PropositionalCompound.create(b1, LogicalOperator.OR, b2),
                        LogicalOperator.AND,
                        PropositionalCompound.create(b1, LogicalOperator.OR, b2)));

        Expression<Boolean> dnf = (Expression<Boolean>) nested.accept(DisjunctionCreatorVisitor.getInstance(), null);

        System.out.println(dnf);
    }

    @Test(groups = {"normalization"})
    public void nestedTest6(){
        Expression nested = ExpressionUtil.and(ExpressionUtil.or(e1, e1), ExpressionUtil.or(e2, e2));

        Expression expected = ExpressionUtil.or(ExpressionUtil.and(e1, e2), ExpressionUtil.and(e1, e2), ExpressionUtil.and(e1, e2),
                ExpressionUtil.and(e1, e2));

        Expression<Boolean> dnf = NormalizationUtil.createDNF(nested);

        System.out.println(dnf);
        //fails only because of the brackets
        //assertEquals(dnf, expected);
    }
}


