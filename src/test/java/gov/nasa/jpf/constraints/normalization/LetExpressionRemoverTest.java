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

public class LetExpressionRemoverTest {

    Variable x1 = Variable.create(BuiltinTypes.SINT32, "x1");
    Variable x2 = Variable.create(BuiltinTypes.SINT32, "x2");
    Constant c2 = Constant.create(BuiltinTypes.SINT32, 2);
    Constant c4 = Constant.create(BuiltinTypes.SINT32, 4);
    NumericBooleanExpression partA = NumericBooleanExpression.create(x1, NumericComparator.LE, c4);
    NumericBooleanExpression partB = NumericBooleanExpression.create(x2, NumericComparator.GE, c2);
    NumericCompound replacementA = NumericCompound.create(x2, NumericOperator.PLUS, c2);
    NumericCompound replacementB = NumericCompound.create(x2, NumericOperator.MINUS, c2);

    Expression<Boolean> letExpression = LetExpression.create(x1, replacementA, partA);
    Expression letFree = NumericBooleanExpression.create(replacementA, NumericComparator.LE, c4);
    Expression<Boolean> nestedLet = PropositionalCompound.create(
            LetExpression.create(x2, replacementB, partB), LogicalOperator.AND, letExpression);
    Expression letFree2 = PropositionalCompound.create(
            NumericBooleanExpression.create(replacementB, NumericComparator.GE, c2), LogicalOperator.AND, letFree);



    @Test(groups = {"normalization"})
    public void letTest() {
        Expression<Boolean> result = NormalizationUtil.eliminateLetExpressions(letExpression);

        assertEquals(result, letFree);
    }

    @Test(groups = {"normalization"})
    public void nestedLetTest() {
        Expression<Boolean> result = NormalizationUtil.eliminateLetExpressions(nestedLet);

        assertEquals(result, letFree2);
        System.out.println(result);
    }

    @Test(groups = {"normalization"})
    public void quantifiedLetTest() {
        List<Variable<?>> bound = new ArrayList<Variable<?>>();
        bound.add(x1);
        Expression quantified = QuantifierExpression.create(Quantifier.EXISTS, bound, letExpression);
        Expression<Boolean> result = NormalizationUtil.eliminateLetExpressions(quantified);

        System.out.println(result);
    }

    @Test(groups = {"normalization"})
    public void innerLetTest() {
        Expression multipleLets = ExpressionUtil.or(nestedLet, letExpression);
        Expression<Boolean> result = NormalizationUtil.eliminateLetExpressions(multipleLets);

        System.out.println(result);
    }

}
