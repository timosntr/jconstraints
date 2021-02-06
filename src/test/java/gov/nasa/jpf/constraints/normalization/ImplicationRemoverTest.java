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

import static org.testng.Assert.assertEquals;

public class ImplicationRemoverTest {

        Variable<Integer> x = Variable.create(BuiltinTypes.SINT32, "x");
        Constant<Integer> c1 = Constant.create(BuiltinTypes.SINT32, 1);
        Constant<Integer> c2 = Constant.create(BuiltinTypes.SINT32, 2);
        Expression e1 = NumericBooleanExpression.create(x, NumericComparator.LT, c1);
        Expression negE1 = Negation.create(e1);
        Expression e2 = NumericBooleanExpression.create(x, NumericComparator.LE, c2);

        Expression p1 = PropositionalCompound.create(e1, LogicalOperator.AND, e2);
        Expression p2 = NumericBooleanExpression.create(x, NumericComparator.EQ, c2);
        Expression p3 = PropositionalCompound.create(e1, LogicalOperator.IMPLY, e2);
        Expression negP1 = Negation.create(p1);

        Expression<Boolean> containsImply = PropositionalCompound.create(p1, LogicalOperator.IMPLY, p2);
        Expression<Boolean> containsImply2 = PropositionalCompound.create(p1, LogicalOperator.AND, p3);

        Expression<Boolean> implyFree = PropositionalCompound.create(negP1, LogicalOperator.OR, p2);

        Expression<Boolean> implyFree2 = PropositionalCompound.create(
                p1,
                LogicalOperator.AND,
                PropositionalCompound.create(negE1, LogicalOperator.OR, e2));


    @Test(groups = {"normalization"})
    public void implicationRemoverTest() {
        Expression<Boolean> result = NormalizationUtil.eliminateImplication(containsImply);
        System.out.println(result);
        assertEquals(result, implyFree);
    }

    @Test(groups = {"normalization"})
    public void nestedImplicationRemoverTest() {
        Expression<Boolean> result = NormalizationUtil.eliminateImplication(containsImply2);

        assertEquals(result, implyFree2);
    }

}
