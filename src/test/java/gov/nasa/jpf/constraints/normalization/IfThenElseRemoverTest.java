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

public class IfThenElseRemoverTest {

    Variable<Boolean> b = Variable.create(BuiltinTypes.BOOL, "b");
    Variable<Boolean> b2 = Variable.create(BuiltinTypes.BOOL, "b2");
    Variable<Integer> x = Variable.create(BuiltinTypes.SINT32, "x");
    Variable<Integer> y = Variable.create(BuiltinTypes.SINT32, "y");
    Variable<Integer> p = Variable.create(BuiltinTypes.SINT32, "p");
    Variable<Integer> q = Variable.create(BuiltinTypes.SINT32, "q");
    Constant<Integer> c1 = Constant.create(BuiltinTypes.SINT32, 1);
    Constant<Integer> c2 = Constant.create(BuiltinTypes.SINT32, 2);
    Expression e1 = NumericBooleanExpression.create(x, NumericComparator.EQ, c1);
    Expression e2 = NumericBooleanExpression.create(x, NumericComparator.EQ, c2);

    Expression iteExpression = IfThenElse.create(b, e1, e2);
    Expression first = PropositionalCompound.create(Negation.create(b), LogicalOperator.OR, e1);
    Expression second = PropositionalCompound.create(b, LogicalOperator.OR, e2);
    Expression iteFree = PropositionalCompound.create(first, LogicalOperator.AND, second);

    Expression nestedIte = PropositionalCompound.create(e1, LogicalOperator.IMPLY, iteExpression);
    Expression iteFree2 = PropositionalCompound.create(e1, LogicalOperator.IMPLY, iteFree);

    @Test(groups = {"normalization"})
    public void ifThenElseTest() {
        Expression<Boolean> result = (Expression<Boolean>) iteExpression.accept(IfThenElseRemoverVisitor.getInstance(), null);

        assertEquals(result, iteFree);
    }

    @Test(groups = {"normalization"})
    public void nestedIfThenElseTest() {
        Expression<Boolean> result = (Expression<Boolean>) nestedIte.accept(IfThenElseRemoverVisitor.getInstance(), null);

        assertEquals(result, iteFree2);
    }

    @Test(groups = {"normalization"})
    public void numericIfThenElseTest() {
        Expression<Boolean> compound = NumericBooleanExpression.create(
                IfThenElse.create(b, x, y),
                NumericComparator.EQ,
                NumericCompound.create(
                        UnaryMinus.create(c1),
                        NumericOperator.PLUS,
                        IfThenElse.create(b2, p, q)));

        Expression<Boolean> result = NormalizationUtil.eliminateIfThenElse(compound);

        System.out.println(result);
    }

}
