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
        //Expression thenExpr = expr.getThen();
        //Expression elseExpr = expr.getElse();

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
            // TODO: should better not happen
            return expr;
        }
        /*Expression firstPart = PropositionalCompound.create(Negation.create(ifCond), LogicalOperator.OR, thenExpr);
        Expression secondPart = PropositionalCompound.create(ifCond, LogicalOperator.OR, elseExpr);

        Expression result = PropositionalCompound.create(firstPart, LogicalOperator.AND, secondPart);

        return result;*/

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
    public Expression<?> visit(NumericBooleanExpression n, Void data) {
        /*//Todo: hopefully safe(r) version,
        // evaluate how to flatten IfThenElses in NumericBooleanExpressions properly

        return n;*/

        Expression leftChild = n.getLeft();
        Expression rightChild = n.getRight();
        NumericComparator comparator = n.getComparator();
        boolean leftIsIte = leftChild instanceof IfThenElse;
        boolean rightIsIte = rightChild instanceof IfThenElse;

        if(!leftIsIte && !rightIsIte){
            //case: no ItE in children
            return super.visit(n, data);
        } else if(leftIsIte && !rightIsIte){
            //case: left child is ItE
            Expression ifCondition = ((IfThenElse<?>) leftChild).getIf();
            Expression thenExpression = visit(((IfThenElse<?>) leftChild).getThen(), data);
            Expression elseExpression = visit(((IfThenElse<?>) leftChild).getElse(), data);
            if(!thenExpression.getType().equals(BuiltinTypes.BOOL) || !elseExpression.getType().equals(BuiltinTypes.BOOL)){
                //case: ItE is not of Type Bool (Numeric)
                Expression flattenedThen = PropositionalCompound.create(ifCondition, LogicalOperator.AND, NumericBooleanExpression.create(thenExpression, comparator, visit(rightChild, data)));
                Expression flattenedElse = PropositionalCompound.create(Negation.create(ifCondition), LogicalOperator.AND, NumericBooleanExpression.create(elseExpression, comparator, visit(rightChild, data)));
                Expression result = PropositionalCompound.create(flattenedThen, LogicalOperator.OR, flattenedElse);
                return result;
            } else {
                //case: visit(IfThenElse) will flatten child
                return super.visit(n, data);
            }
        } else if(!leftIsIte && rightIsIte) {
            //case: left child is ItE
            Expression ifCondition = ((IfThenElse<?>) rightChild).getIf();
            Expression thenExpression = ((IfThenElse<?>) rightChild).getThen();
            Expression elseExpression = ((IfThenElse<?>) rightChild).getElse();
            if (!thenExpression.getType().equals(BuiltinTypes.BOOL) || !elseExpression.getType().equals(BuiltinTypes.BOOL)) {
                //case: ItE is not of Type Bool (Numeric)
                Expression flattenedThen = PropositionalCompound.create(ifCondition, LogicalOperator.AND, NumericBooleanExpression.create((Expression<Boolean>) visit(leftChild, data), comparator, thenExpression));
                Expression flattenedElse = PropositionalCompound.create(Negation.create(ifCondition), LogicalOperator.AND, NumericBooleanExpression.create((Expression<Boolean>) visit(leftChild, data), comparator, elseExpression));
                Expression result = PropositionalCompound.create(flattenedThen, LogicalOperator.OR, flattenedElse);
                return result;
            } else {
                //case: visit(IfThenElse) will flatten child
                return super.visit(n, data);
            }
        } else { //(leftIsIte && rightIsIte)
            Expression leftIfCondition = ((IfThenElse<?>) leftChild).getIf();
            Expression leftThenExpression = ((IfThenElse<?>) leftChild).getThen();
            Expression leftElseExpression = ((IfThenElse<?>) leftChild).getElse();
            Expression rightIfCondition = ((IfThenElse<?>) rightChild).getIf();
            Expression rightThenExpression = ((IfThenElse<?>) rightChild).getThen();
            Expression rightElseExpression = ((IfThenElse<?>) rightChild).getElse();

            if (!leftThenExpression.getType().equals(BuiltinTypes.BOOL) || !leftElseExpression.getType().equals(BuiltinTypes.BOOL)
                    || !rightThenExpression.getType().equals(BuiltinTypes.BOOL) || !rightElseExpression.getType().equals(BuiltinTypes.BOOL)) {
                //case: one or both ItE contain a part which is not of Type Bool (Numeric)
                Expression part1 = PropositionalCompound.create(leftIfCondition, LogicalOperator.AND, PropositionalCompound.create(rightIfCondition, LogicalOperator.AND,
                        NumericBooleanExpression.create(leftThenExpression, comparator, rightThenExpression)));
                Expression part2 = PropositionalCompound.create(leftIfCondition, LogicalOperator.AND, PropositionalCompound.create(Negation.create(rightIfCondition), LogicalOperator.AND,
                        NumericBooleanExpression.create(leftThenExpression, comparator, rightElseExpression)));
                Expression part3 = PropositionalCompound.create(Negation.create(leftIfCondition), LogicalOperator.AND, PropositionalCompound.create(rightIfCondition, LogicalOperator.AND,
                        NumericBooleanExpression.create(leftElseExpression, comparator, rightThenExpression)));
                Expression part4 = PropositionalCompound.create(Negation.create(leftIfCondition), LogicalOperator.AND, PropositionalCompound.create(Negation.create(rightIfCondition), LogicalOperator.AND,
                        NumericBooleanExpression.create(leftElseExpression, comparator, rightElseExpression)));

                Expression result = PropositionalCompound.create(part1, LogicalOperator.OR, PropositionalCompound.create(part2, LogicalOperator.OR, PropositionalCompound.create(part3, LogicalOperator.OR, part4)));
                return result;
            } else {
                //case: visit(IfThenElse) will flatten child
                return super.visit(n, data);
            }
        }
    }

    @Override
    public Expression<?> visit(NumericCompound n, Void data) {
        //Todo: hopefully safe(r) version,
        // evaluate how to flatten IfThenElses in NumericCompounds
        // (probably has to be done in NumericBooleanCompound)
        return n;

        /*Expression leftChild = n.getLeft();
        Expression rightChild = n.getRight();
        NumericOperator operator = n.getOperator();
        boolean leftIsIte = leftChild instanceof IfThenElse;
        boolean rightIsIte = rightChild instanceof IfThenElse;

        if(!leftIsIte && !rightIsIte){
            //case: no ItE in children
            return super.visit(n, data);
        } else if(leftIsIte && !rightIsIte){
            //case: left child is ItE
            Expression ifCondition = ((IfThenElse<?>) leftChild).getIf();
            Expression thenExpression = ((IfThenElse<?>) leftChild).getThen();
            Expression elseExpression = ((IfThenElse<?>) leftChild).getElse();
            if(!thenExpression.getType().equals(BuiltinTypes.BOOL) || !elseExpression.getType().equals(BuiltinTypes.BOOL)){
                //case: ItE is not of Type Bool (Numeric)
                Expression flattenedThen = PropositionalCompound.create(ifCondition, LogicalOperator.AND, NumericCompound.create(thenExpression, operator, visit(rightChild, data)));
                Expression flattenedElse = PropositionalCompound.create(Negation.create(ifCondition), LogicalOperator.AND, NumericCompound.create(elseExpression, operator, visit(rightChild, data)));
                Expression result = PropositionalCompound.create(flattenedThen, LogicalOperator.OR, flattenedElse);
                return result;
            } else {
                //case: visit(IfThenElse) will flatten child
                return super.visit(n, data);
            }
        } else if(!leftIsIte && rightIsIte) {
            //case: left child is ItE
            Expression ifCondition = ((IfThenElse<?>) rightChild).getIf();
            Expression thenExpression = ((IfThenElse<?>) rightChild).getThen();
            Expression elseExpression = ((IfThenElse<?>) rightChild).getElse();
            if (!thenExpression.getType().equals(BuiltinTypes.BOOL) && !elseExpression.getType().equals(BuiltinTypes.BOOL)) {
                //case: ItE is not of Type Numeric (?)
                Expression flattenedThen = PropositionalCompound.create(ifCondition, LogicalOperator.AND, NumericCompound.create(visit(leftChild, data), operator, thenExpression));
                Expression flattenedElse = PropositionalCompound.create(Negation.create(ifCondition), LogicalOperator.AND, NumericCompound.create(visit(leftChild, data), operator, elseExpression));
                Expression result = PropositionalCompound.create(flattenedThen, LogicalOperator.OR, flattenedElse);
                return result;
            } else {
                //case: visit(IfThenElse) will flatten child
                return super.visit(n, data);
            }
        } else { //(leftIsIte && rightIsIte)
            Expression leftIfCondition = ((IfThenElse<?>) leftChild).getIf();
            Expression leftThenExpression = ((IfThenElse<?>) leftChild).getThen();
            Expression leftElseExpression = ((IfThenElse<?>) leftChild).getElse();
            Expression rightIfCondition = ((IfThenElse<?>) rightChild).getIf();
            Expression rightThenExpression = ((IfThenElse<?>) rightChild).getThen();
            Expression rightElseExpression = ((IfThenElse<?>) rightChild).getElse();

            if ((leftThenExpression.getType().equals(BuiltinTypes.BOOL) || !leftElseExpression.getType().equals(BuiltinTypes.BOOL))
                    || (!rightThenExpression.getType().equals(BuiltinTypes.BOOL) || !rightElseExpression.getType().equals(BuiltinTypes.BOOL))) {
                //case: one or both ItE contain a part which is of Type Numeric (?)
                Expression part1 = PropositionalCompound.create(leftIfCondition, LogicalOperator.AND, PropositionalCompound.create(rightIfCondition, LogicalOperator.AND,
                        NumericCompound.create(leftThenExpression, operator, rightThenExpression)));
                Expression part2 = PropositionalCompound.create(leftIfCondition, LogicalOperator.AND, PropositionalCompound.create(Negation.create(rightIfCondition), LogicalOperator.AND,
                        NumericCompound.create(leftThenExpression, operator, rightElseExpression)));
                Expression part3 = PropositionalCompound.create(Negation.create(leftIfCondition), LogicalOperator.AND, PropositionalCompound.create(rightIfCondition, LogicalOperator.AND,
                        NumericCompound.create(leftElseExpression, operator, rightThenExpression)));
                Expression part4 = PropositionalCompound.create(Negation.create(leftIfCondition), LogicalOperator.AND, PropositionalCompound.create(Negation.create(rightIfCondition), LogicalOperator.AND,
                        NumericCompound.create(leftElseExpression, operator, rightElseExpression)));

                Expression result = PropositionalCompound.create(part1, LogicalOperator.OR, PropositionalCompound.create(part2, LogicalOperator.OR, PropositionalCompound.create(part3, LogicalOperator.OR, part4)));
                return result;
            } else {
                //case: visit(IfThenElse) will flatten child
                return super.visit(n, data);
            }
        }*/
    }

    @Override
    //Not needed if LetExpressionRemover is used beforehand
    public Expression<?> visit(LetExpression let, Void data) {
        return super.visit(let.flattenLetExpression(), data);
    }

    public <T> Expression<T> apply(Expression<T> expr, Void data) {
        return visit(expr, data).requireAs(expr.getType());
    }
}
