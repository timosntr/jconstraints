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
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class ExistentionalClosureTest {

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

    Expression disjunction = PropositionalCompound.create(
            PropositionalCompound.create(e3, LogicalOperator.AND, e4),
            LogicalOperator.OR,
            b1);

    @Test(groups = {"normalization"})
    //outer forall
    public void outerTest(){
        List<Variable<?>> bound = new ArrayList<Variable<?>>();
        bound.add(x);
        Expression quantified = QuantifierExpression.create(Quantifier.FORALL, bound, con1);
        Expression expected = con1;

        Expression<Boolean> noForall = (Expression<Boolean>) quantified.accept(ForallRemoverVisitor.getInstance(), null);

        assertEquals(noForall, expected);
    }

    @Test(groups = {"normalization"})
    //inner forall
    public void innerTest(){
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

}


