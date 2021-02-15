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
import java.util.List;

import static org.testng.Assert.assertEquals;

public class NormalizationUtilTest {

    Variable<Integer> x = Variable.create(BuiltinTypes.SINT32, "x");
    Variable<Integer> y = Variable.create(BuiltinTypes.SINT32, "y");
    Variable<Boolean> b = Variable.create(BuiltinTypes.BOOL, "b");
    Variable<Boolean> b1 = Variable.create(BuiltinTypes.BOOL, "b1");
    Variable<Boolean> b2 = Variable.create(BuiltinTypes.BOOL, "b2");
    Variable<Integer> p = Variable.create(BuiltinTypes.SINT32, "p");
    Variable<Integer> q = Variable.create(BuiltinTypes.SINT32, "q");

    Constant<Integer> c1 = Constant.create(BuiltinTypes.SINT32, 1);
    Constant<Integer> c2 = Constant.create(BuiltinTypes.SINT32, 2);

    Expression e1 = NumericBooleanExpression.create(x, NumericComparator.LT, c1);
    Expression e2 = NumericBooleanExpression.create(y, NumericComparator.LE, c2);
    Expression e3 = NumericBooleanExpression.create(p, NumericComparator.EQ, c2);
    Expression e4 = NumericBooleanExpression.create(y, NumericComparator.EQ, c2);

    Expression con1 = PropositionalCompound.create(e1, LogicalOperator.AND, e2);
    Expression dis1 = PropositionalCompound.create(e1, LogicalOperator.OR, e2);
    Expression xor2 = PropositionalCompound.create(e1, LogicalOperator.XOR, e2);
    Expression xor1 = PropositionalCompound.create(xor2, LogicalOperator.XOR, e2);
    Expression imply = PropositionalCompound.create(con1, LogicalOperator.IMPLY, e2);
    Expression negation1 = Negation.create(xor1);
    Expression negation2 = Negation.create(imply);

    @Test(groups = {"normalization"})
    public void quantifierCounterTest(){
        List<Variable<?>> bound1 = new ArrayList<>();
        bound1.add(x);
        List<Variable<?>> bound3 = new ArrayList<>();
        bound3.add(y);
        Expression quantified = ExpressionUtil.and(
                QuantifierExpression.create(Quantifier.FORALL, bound1, e1),
                QuantifierExpression.create(Quantifier.EXISTS, bound3, e2));
        int count = NormalizationUtil.countQuantifiers(quantified);

        assertEquals(count, 2);
    }

    @Test(groups = {"normalization"})
    public void iteCountTest(){
        Expression<Boolean> compound = NumericBooleanExpression.create(
                IfThenElse.create(b, IfThenElse.create(b, x, y), y),
                NumericComparator.EQ,
                NumericCompound.create(
                        IfThenElse.create(b2, p, q),
                        NumericOperator.PLUS,
                        IfThenElse.create(b2, p, q)));

        int count = NormalizationUtil.countItes(compound);

        assertEquals(count, 4);
    }

    @Test(groups = {"normalization"})
    public void iteCheckTest(){
        Expression<Boolean> compound = NumericBooleanExpression.create(
                NumericCompound.create(IfThenElse.create(b, x, y),NumericOperator.PLUS, y),
                NumericComparator.EQ,
                NumericCompound.create(
                        IfThenElse.create(b2, p, q),
                        NumericOperator.PLUS,
                        IfThenElse.create(b2, p, q)));

        boolean ite = NormalizationUtil.ifThenElseCheck(compound);

        assertEquals(ite, true);
    }

    @Test(groups = {"normalization"})
    public void equivalenceCountTest(){
        Expression p1 = PropositionalCompound.create(e1, LogicalOperator.AND, e2);
        Expression p3 = PropositionalCompound.create(e1, LogicalOperator.EQUIV, e2);

        Expression<Boolean> containsEquiv = PropositionalCompound.create(p1, LogicalOperator.EQUIV, p3);

        int count = NormalizationUtil.countEquivalences(containsEquiv);

        assertEquals(count, 2);
    }

    @Test(groups = {"normalization"})
    public void conjunctionCountTest(){
        Expression con1 = PropositionalCompound.create(e3, LogicalOperator.AND, e1);
        Expression con2 = PropositionalCompound.create(e1, LogicalOperator.AND, e2);
        Expression conjunction = PropositionalCompound.create(con1, LogicalOperator.AND, con2);

        int count = NormalizationUtil.countConjunctions(conjunction);

        assertEquals(count, 3);
    }

    @Test(groups = {"normalization"})
    public void disjunctionCountTest(){
        Expression con1 = PropositionalCompound.create(e3, LogicalOperator.AND, e1);
        Expression dis2 = PropositionalCompound.create(e1, LogicalOperator.OR, e2);
        Expression disjunction = PropositionalCompound.create(con1, LogicalOperator.OR, dis2);

        int count = NormalizationUtil.countDisjunctions(disjunction);

        assertEquals(count, 2);
    }

    @Test(groups = {"normalization"})
    public void negationCountTest(){
        Expression disjunction = Negation.create(PropositionalCompound.create(negation1, LogicalOperator.OR, negation2));

        int count = NormalizationUtil.countNegations(disjunction);

        assertEquals(count, 3);
    }

    @Test(groups = {"normalization"})
    public void xorCountTest(){
        int count = NormalizationUtil.countXORs(xor1);

        assertEquals(count, 2);
    }

    @Test(groups = {"normalization"})
    public void mixedQuantifiersTest(){
        List<Variable<?>> bound1 = new ArrayList<>();
        bound1.add(x);
        List<Variable<?>> bound3 = new ArrayList<>();
        bound3.add(y);
        Expression quantified = ExpressionUtil.and(
                QuantifierExpression.create(Quantifier.FORALL, bound1, e1),
                QuantifierExpression.create(Quantifier.EXISTS, bound3, e2));
        boolean mixed = NormalizationUtil.mixedQuantifierCheck(quantified);
        assertEquals(mixed, true);
    }

    @Test(groups = {"normalization"})
    public void maxClauseLengthTest(){
        Expression con1 = PropositionalCompound.create(e3, LogicalOperator.AND, e1);
        Expression dis2 = PropositionalCompound.create(e1, LogicalOperator.OR, e2);
        Expression dnf = PropositionalCompound.create(con1, LogicalOperator.OR, dis2);

        int max = NormalizationUtil.maxDisjunctionLength(dnf);
        assertEquals(max, 2);
    }

    @Test(groups = {"normalization"})
    public void normalizeTest1(){
        Expression disjunction8 = PropositionalCompound.create(dis1, LogicalOperator.OR, con1);
        Expression expected = PropositionalCompound.create(
                PropositionalCompound.create(dis1, LogicalOperator.OR, e1),
                LogicalOperator.AND,
                PropositionalCompound.create(dis1, LogicalOperator.OR, e2));

        Expression<Boolean> cnf = NormalizationUtil.normalize(disjunction8, "cnf");

        assertEquals(cnf, expected);
    }

    @Test(groups = {"normalization"})
    public void normalizeTest2(){
        List<Variable<?>> bound = new ArrayList<>();
        bound.add(x);
        Expression disjunction8 = PropositionalCompound.create(dis1, LogicalOperator.OR, con1);
        Expression<Boolean> quantified = QuantifierExpression.create(Quantifier.FORALL, bound, disjunction8);

        Expression<Boolean> cnf = NormalizationUtil.normalize(quantified, "cnf");

        System.out.println(quantified);
        System.out.println(cnf);
    }

    @Test(groups = {"normalization"})
    public void normalizeTest3(){
        Expression conjunction7 = PropositionalCompound.create(dis1, LogicalOperator.AND, con1);
        Expression expected = PropositionalCompound.create(
                PropositionalCompound.create(e1, LogicalOperator.AND, con1),
                LogicalOperator.OR,
                PropositionalCompound.create(e2, LogicalOperator.AND, con1));

        Expression<Boolean> dnf = NormalizationUtil.normalize(conjunction7, "dnf");

        assertEquals(dnf, expected);
    }

    @Test(groups = {"normalization"})
    public void normalizeTest4(){
        List<Variable<?>> bound = new ArrayList<>();
        bound.add(x);
        Expression disjunction8 = PropositionalCompound.create(dis1, LogicalOperator.OR, con1);
        Expression<Boolean> quantified = QuantifierExpression.create(Quantifier.FORALL, bound, disjunction8);

        Expression<Boolean> dnf = NormalizationUtil.normalize(quantified, "dnf");

        System.out.println(quantified);
        System.out.println(dnf);
    }
}
