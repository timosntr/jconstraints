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

//this visitor should ensure the ordering: Variable (operator/comparator) Constant
public class OrderingVisitor extends
        DuplicatingVisitor<Void> {

    private static final OrderingVisitor INSTANCE = new OrderingVisitor();

    public static OrderingVisitor getInstance(){
        return INSTANCE;
    }

    @Override
    public Expression<?> visit(NumericBooleanExpression n, Void data) {
        Expression left = visit(n.getLeft(), data);
        Expression right = visit(n.getRight(), data);
        NumericComparator comparator = n.getComparator();

        if(left instanceof Constant && (right instanceof Variable || right instanceof NumericCompound)){
            if(comparator.equals(NumericComparator.LT) || comparator.equals(NumericComparator.LE) || comparator.equals(NumericComparator.GT) || comparator.equals(NumericComparator.GE)){
                return NumericBooleanExpression.create(right, comparator.not(), left);
            } else {
                //EQ and NE should not be changed
                return NumericBooleanExpression.create(right, comparator, left);
            }
        } else {
            return NumericBooleanExpression.create(left, comparator, right);
        }
    }

    @Override
    public <E> Expression<?> visit(NumericCompound<E> n, Void data) {
        Expression left = visit(n.getLeft(), data);
        Expression right = visit(n.getRight(), data);
        NumericOperator operator = n.getOperator();

        if(left instanceof Constant && (right instanceof Variable || right instanceof NumericCompound)){
            if(operator.equals(NumericOperator.PLUS) || operator.equals(NumericOperator.MUL)){
                return NumericCompound.create(right, operator, left);
            } else {
                //no change of order if NumericOperator is Minus, REM or DIV (transformation here more complex)
                return NumericCompound.create(left, operator, right);
            }

        } else {
            return NumericCompound.create(left, operator, right);
        }
    }

    @Override
    public Expression<?> visit(PropositionalCompound n, Void data) {
        Expression left = visit(n.getLeft(), data);
        Expression right = visit(n.getRight(), data);
        LogicalOperator operator = n.getOperator();
        if(operator.equals(LogicalOperator.IMPLY)){
            return super.visit(n, data);
        }
        if((left instanceof Constant && !(right instanceof Constant)) || (!(left instanceof Variable) && right instanceof Variable)) {
            return PropositionalCompound.create(right, operator, left);
        } else {
            return PropositionalCompound.create(left, operator, right);
        }
    }

    @Override
    //Not needed if LetExpressionRemover is used beforehand
    public Expression<?> visit(LetExpression let, Void data) {
        return visit(let.flattenLetExpression(), data);
    }

    public <T> Expression<T> apply(Expression<T> expr, Void data) {
        return visit(expr, data).requireAs(expr.getType());
    }
}
