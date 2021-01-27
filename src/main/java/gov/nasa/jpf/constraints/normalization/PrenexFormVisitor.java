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
import gov.nasa.jpf.constraints.util.DuplicatingVisitor;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

import java.util.*;

//needed for creation of cnfs or dnfs in presence of quantifiers
public class PrenexFormVisitor extends
        DuplicatingVisitor<List<Variable<?>>> {

    private static final PrenexFormVisitor INSTANCE = new PrenexFormVisitor();

    public static PrenexFormVisitor getInstance(){
        return INSTANCE;
    }

    Set<Variable<?>> boundVarsForall = new HashSet<>();

    @Override
    public Expression<?> visit(QuantifierExpression q, List<Variable<?>> data) {

        if (q.getQuantifier().equals(Quantifier.EXISTS)) {
            throw new UnsupportedOperationException("EXISTS detected, skolemize first!");
        }
        Expression body = q.getBody();
        return visit(body, data);
    }

    @Override
    public Expression<?> visit(LetExpression expr, List<Variable<?>> data) {
        Expression flattened = expr.flattenLetExpression();
        Expression result = visit(flattened, data);
        return result;
    }


    public <T> Expression<T> apply(Expression<T> expr, List<Variable<?>> data) {
        boundVarsForall = ExpressionUtil.boundVariables(expr);
        List<Variable<?>> bound = new ArrayList<>();
        bound.addAll(boundVarsForall);
        Expression matrix = visit(expr, data).requireAs(expr.getType());
        if(!bound.isEmpty()){
            Expression result = QuantifierExpression.create(Quantifier.FORALL, bound, matrix);
            return result;
        } else {
            return expr;
        }
    }
}