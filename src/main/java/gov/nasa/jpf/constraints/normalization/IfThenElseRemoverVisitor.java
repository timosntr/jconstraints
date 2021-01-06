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
import gov.nasa.jpf.constraints.expressions.*;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.DuplicatingVisitor;
import org.apache.commons.math3.analysis.function.Exp;

public class IfThenElseRemoverVisitor extends
        DuplicatingVisitor<Void> {

    private static final IfThenElseRemoverVisitor INSTANCE = new IfThenElseRemoverVisitor();

    public static IfThenElseRemoverVisitor getInstance(){
        return INSTANCE;
    }

    @Override
    public Expression<?> visit(IfThenElse expr, Void data) {
        Expression ifCond = expr.getIf();
        Expression thenExpr = visit(expr.getThen(), data);
        Expression elseExpr = visit(expr.getElse(), data);

        Expression firstPart = PropositionalCompound.create(Negation.create(ifCond), LogicalOperator.OR, thenExpr);
        Expression secondPart = PropositionalCompound.create(ifCond, LogicalOperator.OR, elseExpr);

        Expression result = PropositionalCompound.create(firstPart, LogicalOperator.AND, secondPart);

        return result;

        //ToDo: this is an older version, which showed some outliers
        // which are not existing anymore in the version above -> investigation needed!

        /*
        * Expression ifCond = expr.getIf();
        Expression thenExpr = expr.getThen();
        Expression elseExpr = expr.getElse();

        assert ifCond.getType().equals(BuiltinTypes.BOOL);
        if(thenExpr.getType().equals(BuiltinTypes.BOOL) && elseExpr.getType().equals(BuiltinTypes.BOOL)){
            Expression firstPart = PropositionalCompound.create(Negation.create(ifCond), LogicalOperator.OR, thenExpr);
            Expression secondPart = PropositionalCompound.create(ifCond, LogicalOperator.OR, elseExpr);

            //visit again for finding nested IfThenElse
            Expression result = PropositionalCompound.create(
                    (Expression<Boolean>) visit(firstPart, data),
                    LogicalOperator.AND,
                    visit(secondPart, data));

            return result;
        } else {
            return expr;
        }
        * */
    }

    @Override
    //Not needed if LetExpressionRemover is used beforehand
    public Expression<?> visit(LetExpression let, Void data) {
        return super.visit(let.flattenLetExpression(), data);
    }

    //maybe more efficient
    @Override
    public Expression<?> visit(NumericBooleanExpression n, Void data) {
        return n;
    }

    public <T> Expression<T> apply(Expression<T> expr, Void data) {
        return visit(expr, data).requireAs(expr.getType());
    }
}
