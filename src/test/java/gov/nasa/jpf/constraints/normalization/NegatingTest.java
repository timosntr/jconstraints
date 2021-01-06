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
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.*;
import gov.nasa.jpf.constraints.expressions.functions.Function;
import gov.nasa.jpf.constraints.expressions.functions.FunctionExpression;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import org.testng.annotations.Test;


import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;

public class NegatingTest {

    Variable<Integer> x = Variable.create(BuiltinTypes.SINT32, "x");
    Variable<Integer> y = Variable.create(BuiltinTypes.SINT32, "y");
    Variable<Boolean> b1 = Variable.create(BuiltinTypes.BOOL, "b1");

    Constant<Integer> c1 = Constant.create(BuiltinTypes.SINT32, 1);
    Constant<Integer> c2 = Constant.create(BuiltinTypes.SINT32, 2);

    Expression e1 = NumericCompound.create(x, NumericOperator.PLUS, c1);
    Expression e2 = NumericCompound.create(y, NumericOperator.MINUS, c2);

    Expression e3 = NumericBooleanExpression.create(x, NumericComparator.GE, c1);
    Expression e4 = NumericBooleanExpression.create(y, NumericComparator.EQ, c2);
    Expression negE3 = NumericBooleanExpression.create(x, NumericComparator.LT, c1);
    Expression negE4 = NumericBooleanExpression.create(y, NumericComparator.NE, c2);

    Expression e5 = NumericBooleanExpression.create(e1, NumericComparator.GE, c1);
    Expression e6 = NumericBooleanExpression.create(e2, NumericComparator.LT, c2);
    Expression negE5 = NumericBooleanExpression.create(e1, NumericComparator.LT, c1);
    Expression negE6 = NumericBooleanExpression.create(e2, NumericComparator.GE, c2);

    @Test(groups = {"normalization"})
    public void simpleNegationTest(){
        Expression<Boolean> negConjunction = Negation.create(ExpressionUtil.and(e3,e4));
        Expression<Boolean> disjunction = ExpressionUtil.or(negE3, negE4);
        Expression<Boolean> nnfFormula = (Expression<Boolean>) negConjunction.accept(NegatingVisitor.getInstance(), false);

        assertEquals(nnfFormula, disjunction);
    }

    @Test(groups = {"normalization"})
    public void operatorNegationTest(){
        Expression<Boolean> conj = Negation.create(Negation.create(ExpressionUtil.and(e3,e4)));
        Expression<Boolean> nnf1 = (Expression<Boolean>) conj.accept(NegatingVisitor.getInstance(), false);

        assertEquals(nnf1, ExpressionUtil.and(e3,e4));

        Expression<Boolean> disj = Negation.create(Negation.create(ExpressionUtil.or(e3,e4)));
        Expression<Boolean> nnf2 = (Expression<Boolean>) disj.accept(NegatingVisitor.getInstance(), false);

        assertEquals(nnf2, ExpressionUtil.or(e3,e4));

        Expression<Boolean> equiv = Negation.create(Negation.create(PropositionalCompound.create(e3,LogicalOperator.EQUIV, e4)));
        Expression<Boolean> nnf3 = (Expression<Boolean>) equiv.accept(NegatingVisitor.getInstance(), false);

        assertEquals(nnf3, PropositionalCompound.create(e3,LogicalOperator.EQUIV, e4));

        Expression<Boolean> impl = Negation.create(Negation.create(PropositionalCompound.create(e3,LogicalOperator.IMPLY, e4)));
        Expression<Boolean> nnf4 = (Expression<Boolean>) impl.accept(NegatingVisitor.getInstance(), false);

        assertEquals(nnf4, PropositionalCompound.create(e3,LogicalOperator.IMPLY, e4));
    }

    //Test for boolean variable
    @Test(groups = {"normalization"})
    public void negateBoolTest1(){
        Negation neg = Negation.create(b1);
        Valuation init = new Valuation();
        //ValuationEntry val1 = new ValuationEntry<>(b1, true);
        //init.addEntry(val1);
        init.setValue(b1, true);
        Expression<Boolean> nnf = (Expression<Boolean>) neg.accept(NegatingVisitor.getInstance(), false);


        assertFalse(nnf.evaluate(init));
        //should be both !b
        assertEquals(nnf, neg);
    }

    @Test(groups = {"normalization"})
    public void negateBoolTest2(){
        Negation neg = Negation.create(Negation.create(b1));
        //!! is omitted
        Expression<Boolean> nnf = (Expression<Boolean>) neg.accept(NegatingVisitor.getInstance(), false);
        assertEquals(nnf, b1);
    }

    @Test(groups = {"normalization"})
    public void negateBoolTest3(){
        Negation neg = Negation.create(Negation.create(Negation.create(b1)));
        //!!!b should become !b
        Expression<Boolean> nnf = (Expression<Boolean>) neg.accept(NegatingVisitor.getInstance(), false);
        assertEquals(nnf, Negation.create(b1));
    }

    @Test(groups = {"normalization"})
    public void negateNegationOfExpr1(){
        Expression<Boolean> negE3 = Negation.create(e3);
        Expression<Boolean> nnf = (Expression<Boolean>) negE3.accept(NegatingVisitor.getInstance(), false);
        assertEquals(nnf, NumericBooleanExpression.create(x, NumericComparator.LT, c1));
    }

    @Test(groups = {"normalization"})
    public void negateNegationOfExpr2(){
        Negation neg = Negation.create(Negation.create(e3));
        Expression<Boolean> nnf = (Expression<Boolean>) neg.accept(NegatingVisitor.getInstance(), false);
        assertEquals(nnf, e3);
    }

    @Test(groups = {"normalization"})
    public void negateNegationOfExpr3(){
        Negation neg = Negation.create(Negation.create(Negation.create(e3)));
        Expression<Boolean> nnf = (Expression<Boolean>) neg.accept(NegatingVisitor.getInstance(), false);
        assertEquals(nnf, NumericBooleanExpression.create(x, NumericComparator.LT, c1));
    }

    @Test(groups = {"normalization"})
    public void bigConstraintTest() {
        Expression<Boolean> constraint = PropositionalCompound.create(
                (Negation.create(PropositionalCompound.create(e5, LogicalOperator.AND, e6))), LogicalOperator.OR, NumericBooleanExpression.create(e1, NumericComparator.EQ, c2));
        Expression<Boolean> constraint2 = PropositionalCompound.create(
                (PropositionalCompound.create(e5, LogicalOperator.AND, e6)), LogicalOperator.OR, Negation.create(NumericBooleanExpression.create(e1, NumericComparator.EQ, c2)));

        Expression<Boolean> negation = PropositionalCompound.create(
                (PropositionalCompound.create(negE5, LogicalOperator.OR, negE6)), LogicalOperator.OR, NumericBooleanExpression.create(e1, NumericComparator.EQ, c2));
        Expression<Boolean> negation2 = PropositionalCompound.create(
                (PropositionalCompound.create(e5, LogicalOperator.AND, e6)), LogicalOperator.OR, NumericBooleanExpression.create(e1, NumericComparator.NE, c2));

        Expression<Boolean> nnf = (Expression<Boolean>) constraint.accept(NegatingVisitor.getInstance(), false);
        assertEquals(nnf, negation);

        Expression<Boolean> nnf2 = (Expression<Boolean>) constraint2.accept(NegatingVisitor.getInstance(), false);
        assertEquals(nnf2, negation2);
    }

    @Test(groups = {"normalization"})
    public void quantifierTest() {
        List<Variable<?>> bound = new ArrayList<Variable<?>>();
        bound.add(x);
        Expression<Boolean> quantified = Negation.create(QuantifierExpression.create(Quantifier.EXISTS, bound, e3));
        Expression<Boolean> nnf = (Expression<Boolean>) quantified.accept(NegatingVisitor.getInstance(), false);
        Expression<Boolean> expected = QuantifierExpression.create(Quantifier.FORALL, bound, negE3);

        assertEquals(nnf, expected);
    }

    @Test(groups = {"normalization"})
    public void stringTest() {
        Variable x = Variable.create(BuiltinTypes.STRING, "string1");
        Constant c = Constant.create(BuiltinTypes.STRING, "W");
        StringBooleanExpression notEquals = StringBooleanExpression.createNotEquals(x, c);
        Expression neg = Negation.create(notEquals);

        Expression<Boolean> nnf = (Expression<Boolean>) neg.accept(NegatingVisitor.getInstance(), false);


        Valuation val = new Valuation();
        val.setValue(x, "a");

        assertFalse(nnf.evaluate(val));

        Valuation val1 = new Valuation();
        val1.setValue(x, "W");

        assertTrue(nnf.evaluate(val1));
    }

    @Test(groups = {"normalization"})
    public void constantTest1(){
        Constant<Boolean> b = Constant.create(BuiltinTypes.BOOL, true);
        Variable<Boolean> v = Variable.create(BuiltinTypes.BOOL, "var");
        Expression<Boolean> expr = PropositionalCompound.create(v, LogicalOperator.EQUIV, b);
        Expression<Boolean> neg = Negation.create(expr);
        Expression<Boolean> expr2 = PropositionalCompound.create(v, LogicalOperator.EQUIV, ExpressionUtil.TRUE);
        Expression<Boolean> neg2 = Negation.create(expr2);

        Expression<Boolean> nnf = (Expression<Boolean>) neg.accept(NegatingVisitor.getInstance(), false);
        Expression<Boolean> result = PropositionalCompound.create(v, LogicalOperator.XOR, b);

        assertEquals(nnf, result);

        Expression<Boolean> nnf2 = (Expression<Boolean>) neg2.accept(NegatingVisitor.getInstance(), false);
        assertEquals(nnf2, result);
    }

    @Test(groups = {"normalization"})
    public void constantTest2(){
        Constant<Boolean> v = Constant.create(BuiltinTypes.BOOL, true);
        Expression<Boolean> expr = PropositionalCompound.create(v, LogicalOperator.AND, e3);
        Expression<Boolean> neg = Negation.create(expr);

        Expression<Boolean> nnf = (Expression<Boolean>) neg.accept(NegatingVisitor.getInstance(), false);
        Expression<Boolean> result = PropositionalCompound.create(Negation.create(v), LogicalOperator.OR, negE3);

        assertEquals(nnf, result);
    }

    @Test(groups = {"normalization"})
    public void unaryMinusTest(){
        Expression<Boolean> u = UnaryMinus.create(b1);
        Expression<Boolean> neg = Negation.create(u);
        Valuation val = new Valuation();
        val.setValue(b1, true);

        Expression<Boolean> nnf = (Expression<Boolean>) neg.accept(NegatingVisitor.getInstance(), false);

        //minus is omitted
        assertEquals(nnf, b1);
        //evaluation should be the same
        assertTrue(b1.evaluate(val));
    }

    @Test(groups = {"normalization"})
    public void bitvectorTest(){
        NumericCompound computation1 =
                NumericCompound.create(x, NumericOperator.MUL, Constant.create(BuiltinTypes.SINT32, 2));
        BitvectorExpression bvAnd =
                BitvectorExpression.create(computation1, BitvectorOperator.AND, Constant.create(BuiltinTypes.SINT32, 3));
        Expression<Boolean> neg = Negation.create(bvAnd);

        Expression<Boolean> nnf = (Expression<Boolean>) neg.accept(NegatingVisitor.getInstance(), false);
        Expression<Boolean> expected = BitvectorExpression.create(computation1, BitvectorOperator.OR, Constant.create(BuiltinTypes.SINT32, 3));

        assertEquals(nnf, expected);
    }

    @Test(groups = {"normalization"})
    public void letExpressionTest(){
        Variable x1 = Variable.create(BuiltinTypes.SINT32, "x1");
        Variable x2 = Variable.create(BuiltinTypes.SINT32, "x2");
        Constant c2 = Constant.create(BuiltinTypes.SINT32, 2);
        Constant c4 = Constant.create(BuiltinTypes.SINT32, 4);
        NumericBooleanExpression partA = NumericBooleanExpression.create(x1, NumericComparator.LE, c4);
        NumericCompound replacement = NumericCompound.create(x2, NumericOperator.PLUS, c2);

        Expression<Boolean> neg = Negation.create(LetExpression.create(x1, replacement, partA));

        Expression expected = NumericBooleanExpression.create(replacement, NumericComparator.GT, c4);

        Expression<Boolean> nnf = (Expression<Boolean>) neg.accept(NegatingVisitor.getInstance(), false);

        assertEquals(nnf, expected);
    }


    @Test(groups = {"normalization"})
    public void functionTest(){
        // f (Int) Int
        Function f = Function.create("f", BuiltinTypes.SINT32, BuiltinTypes.SINT32);
        Variable v[] = new Variable[f.getArity()];
        for (int i=0; i<v.length; i++) {
            v[i] = Variable.create(BuiltinTypes.SINT32, "v");
        }
        FunctionExpression expr = FunctionExpression.create(f, v);
        Expression<Boolean> neg = Negation.create(expr);

        Expression<Boolean> nnf = (Expression<Boolean>) neg.accept(NegatingVisitor.getInstance(), false);

        assertEquals(nnf, neg);
    }

}
