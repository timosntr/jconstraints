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
import gov.nasa.jpf.constraints.expressions.functions.Function;
import gov.nasa.jpf.constraints.expressions.functions.FunctionExpression;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.types.Type;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class SkolemizationTest {

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
    Expression e5 = NumericBooleanExpression.create(x, NumericComparator.GE, y);

    Expression con1 = PropositionalCompound.create(e1, LogicalOperator.AND, e4);

    Expression disjunction = PropositionalCompound.create(
            PropositionalCompound.create(e3, LogicalOperator.AND, e4),
            LogicalOperator.OR,
            b1);

    @Test(groups = {"normalization"})
    //only forall
    public void forallTest(){
        List<Variable<?>> args = new ArrayList<>();
        List<Variable<?>> bound = new ArrayList<>();
        bound.add(x);
        Expression quantified = QuantifierExpression.create(Quantifier.FORALL, bound, con1);

        Expression<Boolean> skolemized = (Expression<Boolean>) quantified.accept(SkolemizationVisitor.getInstance(), args);

        assertEquals(skolemized, quantified);
    }

    @Test(groups = {"normalization"})
    //only outer exists, no forall
    public void outerExistsTest1(){
        List<Variable<?>> args = new ArrayList<>();
        List<Variable<?>> bound1 = new ArrayList<Variable<?>>();
        bound1.add(x);
        List<Variable<?>> bound2 = new ArrayList<Variable<?>>();
        bound2.add(y);
        Expression quantified = QuantifierExpression.create(Quantifier.EXISTS, bound1, e1);
        Function f = Function.create("SK.constant.1.x", BuiltinTypes.SINT32);
        Variable v[] = new Variable[f.getArity()];
        FunctionExpression expr = FunctionExpression.create(f, v);
        Expression expected = NumericBooleanExpression.create(expr, NumericComparator.LT, c1);

        Expression<Boolean> skolemized = (Expression<Boolean>) quantified.accept(SkolemizationVisitor.getInstance(), args);

        System.out.println(skolemized);
        assertEquals(skolemized.toString(), expected.toString());
    }

    @Test(groups = {"normalization"})
    //only outer exists, inner forall
    public void outerExistsTest2(){
        List<Variable<?>> args = new ArrayList<>();
        List<Variable<?>> bound1 = new ArrayList<Variable<?>>();
        bound1.add(x);
        List<Variable<?>> bound2 = new ArrayList<Variable<?>>();
        bound2.add(y);
        Expression quantified = QuantifierExpression.create(Quantifier.EXISTS, bound1, QuantifierExpression.create(Quantifier.FORALL, bound2, con1));
        Function f = Function.create("SK.constant.1.x", BuiltinTypes.SINT32);
        Variable v[] = new Variable[f.getArity()];
        FunctionExpression expr = FunctionExpression.create(f, v);
        Expression expected = QuantifierExpression.create(Quantifier.FORALL, bound2, ExpressionUtil.and(NumericBooleanExpression.create(expr, NumericComparator.LT, c1),e4));

        Expression<Boolean> skolemized = (Expression<Boolean>) quantified.accept(SkolemizationVisitor.getInstance(), args);

        System.out.println(skolemized);
        assertEquals(skolemized.toString(), expected.toString());
    }

    @Test(groups = {"normalization"})
    //inner exists
    public void innerExistsTest(){
        List<Variable<?>> args = new ArrayList<>();
        List<Variable<?>> bound1 = new ArrayList<Variable<?>>();
        bound1.add(x);
        List<Variable<?>> bound2 = new ArrayList<Variable<?>>();
        bound2.add(y);
        Expression quantified = QuantifierExpression.create(Quantifier.FORALL, bound1,
                        QuantifierExpression.create(Quantifier.EXISTS, bound2, e5));

        Function f = Function.create("SK.function.0.y", BuiltinTypes.SINT32, BuiltinTypes.SINT32);
        Variable v[] = new Variable[f.getArity()];
        for (int i=0; i<v.length; i++) {
            v[i] = Variable.create(BuiltinTypes.SINT32, "x");
        }
        FunctionExpression expr = FunctionExpression.create(f, v);
        Expression expected = QuantifierExpression.create(Quantifier.FORALL, bound1,
                NumericBooleanExpression.create(x, NumericComparator.GE, expr));

        Expression<Boolean> skolemized = (Expression<Boolean>) quantified.accept(SkolemizationVisitor.getInstance(), args);

        System.out.println(skolemized);
        assertEquals(skolemized.toString(), expected.toString());
    }

    //ToDo: test nameClash

    /*@Test(groups = {"normalization"})
    //multiple exists
    public void multipleExistsTest(){
        List<Variable<?>> bound1 = new ArrayList<Variable<?>>();
        bound1.add(x);
        List<Variable<?>> bound2 = new ArrayList<Variable<?>>();
        bound2.add(y);
        Expression quantified = QuantifierExpression.create(Quantifier.FORALL, bound1,
                PropositionalCompound.create(con1, LogicalOperator.AND,
                        QuantifierExpression.create(Quantifier.FORALL, bound2, e2)));
        Expression expected = PropositionalCompound.create(con1, LogicalOperator.AND, e2);

        Expression<Boolean> noForall = (Expression<Boolean>) quantified.accept(ForallRemoverVisitor.getInstance(), null);

        assertEquals(noForall, expected);
    }

    @Test(groups = {"normalization"})
    //free Variables
    public void freeVariablesTest(){
        List<Variable<?>> bound1 = new ArrayList<Variable<?>>();
        bound1.add(x);
        List<Variable<?>> bound2 = new ArrayList<Variable<?>>();
        bound2.add(y);
        Expression quantified = QuantifierExpression.create(Quantifier.FORALL, bound1,
                PropositionalCompound.create(con1, LogicalOperator.AND,
                        QuantifierExpression.create(Quantifier.FORALL, bound2, e2)));
        Expression expected = PropositionalCompound.create(con1, LogicalOperator.AND, e2);

        Expression<Boolean> noForall = (Expression<Boolean>) quantified.accept(ForallRemoverVisitor.getInstance(), null);

        assertEquals(noForall, expected);
    }*/

}


