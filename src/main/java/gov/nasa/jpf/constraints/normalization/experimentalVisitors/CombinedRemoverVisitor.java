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
package gov.nasa.jpf.constraints.normalization.experimentalVisitors;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.*;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.DuplicatingVisitor;

//this is a combined remover, which could be used before creating a NNF
//because of modularity it should only be used for experimental reasons
public class CombinedRemoverVisitor extends
        DuplicatingVisitor<Void> {

    private static final CombinedRemoverVisitor INSTANCE = new CombinedRemoverVisitor();

    public static CombinedRemoverVisitor getInstance(){
        return INSTANCE;
    }

    @Override
    public Expression<?> visit(PropositionalCompound expression, Void data) {
        Expression<?> left = expression.getLeft();
        Expression<?> right = expression.getRight();
        LogicalOperator operator = expression.getOperator();
        boolean leftIsIte = left instanceof IfThenElse;
        boolean rightIsIte = right instanceof IfThenElse;

        if(!leftIsIte && !rightIsIte){
            //case: no ItE in children
            if(operator.equals(LogicalOperator.XOR)) {
                Expression<Boolean> partLeft = PropositionalCompound.create((Expression<Boolean>) left, LogicalOperator.OR, right);
                Expression<Boolean> partRight = PropositionalCompound.create(Negation.create((Expression<Boolean>) left), LogicalOperator.OR, Negation.create((Expression<Boolean>) right));
                Expression<Boolean> result = PropositionalCompound.create(
                        (Expression<Boolean>) visit(partLeft, data),
                        LogicalOperator.AND,
                        visit(partRight, data));

                return result;
            } else if(operator.equals(LogicalOperator.EQUIV)) {
                Expression<Boolean> partLeft = PropositionalCompound.create(Negation.create((Expression<Boolean>) left), LogicalOperator.OR, right);
                Expression<Boolean> partRight = PropositionalCompound.create((Expression<Boolean>) left, LogicalOperator.OR, Negation.create((Expression<Boolean>) right));
                Expression<Boolean> result = PropositionalCompound.create(
                        (Expression<Boolean>) visit(partLeft, data),
                        LogicalOperator.AND,
                        visit(partRight, data));

                return result;
            } else if(operator.equals(LogicalOperator.IMPLY)){
                Expression<Boolean> partLeft = Negation.create((Expression<Boolean>) left);
                Expression<Boolean> result = PropositionalCompound.create(
                        (Expression<Boolean>) visit(partLeft, data),
                        LogicalOperator.OR,
                        visit(right, data));

                return result;
            } else {
                Expression visitedExpr = PropositionalCompound.create(
                        (Expression<Boolean>) visit(left, data),
                        operator,
                        visit(right, data));

                return visitedExpr;
            }
        } else if(leftIsIte && !rightIsIte){
            //case: left child is ItE
            Expression ifCondition = ((IfThenElse<?>) left).getIf();
            Expression thenExpression = ((IfThenElse<?>) left).getThen();
            Expression elseExpression = ((IfThenElse<?>) left).getElse();
            if(!thenExpression.getType().equals(BuiltinTypes.BOOL) || !elseExpression.getType().equals(BuiltinTypes.BOOL)){
                //case: ItE is not of Type Bool (most probably Numeric)
                Expression flattenedThen = PropositionalCompound.create(ifCondition, LogicalOperator.AND, PropositionalCompound.create(thenExpression, operator, visit(right, data)));
                Expression flattenedElse = PropositionalCompound.create(Negation.create(ifCondition), LogicalOperator.AND, PropositionalCompound.create(elseExpression, operator, visit(right, data)));
                Expression result = PropositionalCompound.create(flattenedThen, LogicalOperator.OR, flattenedElse);
                return result;
            } else {
                //case: visit(IfThenElse) will flatten child
                return super.visit(expression, data);
            }
        } else if(!leftIsIte && rightIsIte) {
            //case: left child is ItE
            Expression ifCondition = ((IfThenElse<?>) right).getIf();
            Expression thenExpression = ((IfThenElse<?>) right).getThen();
            Expression elseExpression = ((IfThenElse<?>) right).getElse();
            if (!thenExpression.getType().equals(BuiltinTypes.BOOL) || !elseExpression.getType().equals(BuiltinTypes.BOOL)) {
                //case: ItE is not of Type Bool (most probably Numeric)
                Expression flattenedThen = PropositionalCompound.create(ifCondition, LogicalOperator.AND, PropositionalCompound.create((Expression<Boolean>) visit(left, data), operator, thenExpression));
                Expression flattenedElse = PropositionalCompound.create(Negation.create(ifCondition), LogicalOperator.AND, PropositionalCompound.create((Expression<Boolean>) visit(left, data), operator, elseExpression));
                Expression result = PropositionalCompound.create(flattenedThen, LogicalOperator.OR, flattenedElse);
                return result;
            } else {
                //case: visit(IfThenElse) will flatten child
                return super.visit(expression, data);
            }
        } else { //(leftIsIte && rightIsIte)
            Expression leftIfCondition = ((IfThenElse<?>) left).getIf();
            Expression leftThenExpression = ((IfThenElse<?>) left).getThen();
            Expression leftElseExpression = ((IfThenElse<?>) left).getElse();
            Expression rightIfCondition = ((IfThenElse<?>) right).getIf();
            Expression rightThenExpression = ((IfThenElse<?>) right).getThen();
            Expression rightElseExpression = ((IfThenElse<?>) right).getElse();

            if ((!leftThenExpression.getType().equals(BuiltinTypes.BOOL) || !leftElseExpression.getType().equals(BuiltinTypes.BOOL))
                    || (!rightThenExpression.getType().equals(BuiltinTypes.BOOL) || !rightElseExpression.getType().equals(BuiltinTypes.BOOL))) {
                //case: one or both ItE contain a part which is not of Type Bool (most probably Numeric)
                Expression part1 = PropositionalCompound.create(leftIfCondition, LogicalOperator.AND, PropositionalCompound.create(rightIfCondition, LogicalOperator.AND,
                        PropositionalCompound.create(leftThenExpression, operator, rightThenExpression)));
                Expression part2 = PropositionalCompound.create(leftIfCondition, LogicalOperator.AND, PropositionalCompound.create(Negation.create(rightIfCondition), LogicalOperator.AND,
                        PropositionalCompound.create(leftThenExpression, operator, rightElseExpression)));
                Expression part3 = PropositionalCompound.create(Negation.create(leftIfCondition), LogicalOperator.AND, PropositionalCompound.create(rightIfCondition, LogicalOperator.AND,
                        PropositionalCompound.create(leftElseExpression, operator, rightThenExpression)));
                Expression part4 = PropositionalCompound.create(Negation.create(leftIfCondition), LogicalOperator.AND, PropositionalCompound.create(Negation.create(rightIfCondition), LogicalOperator.AND,
                        PropositionalCompound.create(leftElseExpression, operator, rightElseExpression)));

                Expression result = PropositionalCompound.create(part1, LogicalOperator.OR, PropositionalCompound.create(part2, LogicalOperator.OR, PropositionalCompound.create(part3, LogicalOperator.OR, part4)));
                return result;
            } else {
                //case: visit(IfThenElse) will flatten child
                return super.visit(expression, data);
            }
        }
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
    }

    @Override
    //Not needed if LetExpressionRemover is used beforehand
    public Expression<?> visit(LetExpression let, Void data) {
        Expression flattened = let.flattenLetExpression();
        Expression result = visit(flattened, data);
        return result;
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
