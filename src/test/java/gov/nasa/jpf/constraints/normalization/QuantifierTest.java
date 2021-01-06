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


public class QuantifierTest {

    Variable<Integer> x = Variable.create(BuiltinTypes.SINT32, "x");
    Constant<Integer> c1 = Constant.create(BuiltinTypes.SINT32, 1);
    Expression e3 = NumericBooleanExpression.create(x, NumericComparator.GE, c1);
    Expression negE3 = NumericBooleanExpression.create(x, NumericComparator.LT, c1);


    @Test(groups = {"normalization"})
    public void quantifierCheckTest() {
        List<Variable<?>> bound = new ArrayList<Variable<?>>();
        bound.add(x);
        Expression<Boolean> quantified = Negation.create(QuantifierExpression.create(Quantifier.EXISTS, bound, e3));
        boolean checkExpression = NormalizationUtil.quantifierCheck(quantified);

        assertEquals(checkExpression, true);
    }

    @Test(groups = {"normalization"})
    public void quantifierCheckVisitorTest() {
        List<Variable<?>> bound = new ArrayList<Variable<?>>();
        bound.add(x);
        Expression<Boolean> quantified = Negation.create(PropositionalCompound.create(QuantifierExpression.create(Quantifier.EXISTS, bound, e3), LogicalOperator.OR, negE3));
        boolean checkExpression = (boolean) quantified.accept(QuantifierCheckVisitor.getInstance(), null);

        assertEquals(checkExpression, true);
    }
}
