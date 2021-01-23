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
import gov.nasa.jpf.constraints.util.ExpressionUtil;
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
        //Todo: not optimized for cnf yet, probably too expensive
        //visit first -> inner ItEs can be hopefully flattened
        Expression leftChild = visit(n.getLeft(), data);
        Expression rightChild = visit(n.getRight(), data);
        NumericComparator comparator = n.getComparator();

        boolean leftChildIsNum = leftChild instanceof NumericCompound;
        boolean rightChildIsNum = rightChild instanceof NumericCompound;
        boolean leftChildIsIte = leftChild instanceof IfThenElse;
        boolean rightChildIsIte = rightChild instanceof IfThenElse;

        if(!leftChildIsNum && !rightChildIsNum && !leftChildIsIte && !rightChildIsIte){
            return n;
        }

        if(leftChildIsIte && rightChildIsIte) {
            //case: both children are Ite
            Expression leftIfCondition = ((IfThenElse<?>) leftChild).getIf();
            Expression leftThenExpression = ((IfThenElse<?>) leftChild).getThen();
            Expression leftElseExpression = ((IfThenElse<?>) leftChild).getElse();

            Expression rightIfCondition = ((IfThenElse<?>) rightChild).getIf();
            Expression rightThenExpression = ((IfThenElse<?>) rightChild).getThen();
            Expression rightElseExpression = ((IfThenElse<?>) rightChild).getElse();

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
        }

        if(leftChildIsIte && !rightChildIsIte){
            //case: left child is ItE
            Expression ifCondition = ((IfThenElse<?>) leftChild).getIf();
            //TODO: or visit again?
            Expression thenExpression = ((IfThenElse<?>) leftChild).getThen();
            Expression elseExpression = ((IfThenElse<?>) leftChild).getElse();

            if(!rightChildIsNum){
                Expression result = ExpressionUtil.or(
                        ExpressionUtil.and(ifCondition, NumericBooleanExpression.create(thenExpression, comparator, rightChild)),
                        ExpressionUtil.and(Negation.create(ifCondition), NumericBooleanExpression.create(elseExpression, comparator, rightChild)));
                return result;
            } else { //(rightChildIsNum)
                Expression rightLeft = ((NumericCompound<?>) rightChild).getLeft();
                NumericOperator opRight = ((NumericCompound<?>) rightChild).getOperator();
                Expression rightRight = ((NumericCompound<?>) rightChild).getRight();

                boolean rightLeftIsIte = rightLeft instanceof IfThenElse;
                boolean rightRightIsIte = rightRight instanceof IfThenElse;

                if(!rightLeftIsIte && !rightRightIsIte){
                    Expression result = ExpressionUtil.or(
                            ExpressionUtil.and(ifCondition, NumericBooleanExpression.create(thenExpression, comparator, rightChild)),
                            ExpressionUtil.and(Negation.create(ifCondition), NumericBooleanExpression.create(elseExpression, comparator, rightChild)));
                    return result;
                }

                if(!rightLeftIsIte && rightRightIsIte){
                    Expression ifRight = ((IfThenElse<?>) rightRight).getIf();
                    Expression thenRight = ((IfThenElse<?>) rightRight).getThen();
                    Expression elseRight = ((IfThenElse<?>) rightRight).getElse();

                    Expression numericPart1 = NumericCompound.create(rightLeft, opRight, thenRight);
                    Expression numericPart2 = NumericCompound.create(rightLeft, opRight, elseRight);

                    Expression comparatorPart1 = NumericBooleanExpression.create(thenExpression, comparator, numericPart1);
                    Expression comparatorPart2 = NumericBooleanExpression.create(thenExpression, comparator, numericPart2);
                    Expression comparatorPart3 = NumericBooleanExpression.create(elseExpression, comparator, numericPart1);
                    Expression comparatorPart4 = NumericBooleanExpression.create(elseExpression, comparator, numericPart2);

                    Expression propositionalPart1 = ExpressionUtil.and(ifCondition, ExpressionUtil.and(ifRight, comparatorPart1));
                    Expression propositionalPart2 = ExpressionUtil.and(ifCondition, ExpressionUtil.and(Negation.create(ifRight), comparatorPart2));
                    Expression propositionalPart3 = ExpressionUtil.and(Negation.create(ifCondition), ExpressionUtil.and(ifRight, comparatorPart3));
                    Expression propositionalPart4 = ExpressionUtil.and(Negation.create(ifCondition), ExpressionUtil.and(Negation.create(ifRight), comparatorPart4));

                    Expression result = ExpressionUtil.or(
                            propositionalPart1, ExpressionUtil.or(propositionalPart2, ExpressionUtil.or(propositionalPart3, propositionalPart4)));
                    return result;
                }

                if(rightLeftIsIte && !rightRightIsIte){
                    Expression ifLeft = ((IfThenElse<?>) rightLeft).getIf();
                    Expression thenLeft = ((IfThenElse<?>) rightLeft).getThen();
                    Expression elseLeft = ((IfThenElse<?>) rightLeft).getElse();

                    Expression numericPart1 = NumericCompound.create(thenLeft, opRight, rightRight);
                    Expression numericPart2 = NumericCompound.create(elseLeft, opRight, rightRight);

                    Expression comparatorPart1 = NumericBooleanExpression.create(thenExpression, comparator, numericPart1);
                    Expression comparatorPart2 = NumericBooleanExpression.create(thenExpression, comparator, numericPart2);
                    Expression comparatorPart3 = NumericBooleanExpression.create(elseExpression, comparator, numericPart1);
                    Expression comparatorPart4 = NumericBooleanExpression.create(elseExpression, comparator, numericPart2);

                    Expression propositionalPart1 = ExpressionUtil.and(ifCondition, ExpressionUtil.and(ifLeft, comparatorPart1));
                    Expression propositionalPart2 = ExpressionUtil.and(ifCondition, ExpressionUtil.and(Negation.create(ifLeft), comparatorPart2));
                    Expression propositionalPart3 = ExpressionUtil.and(Negation.create(ifCondition), ExpressionUtil.and(ifLeft, comparatorPart3));
                    Expression propositionalPart4 = ExpressionUtil.and(Negation.create(ifCondition), ExpressionUtil.and(Negation.create(ifLeft), comparatorPart4));

                    Expression result = ExpressionUtil.or(
                            propositionalPart1, ExpressionUtil.or(propositionalPart2, ExpressionUtil.or(propositionalPart3, propositionalPart4)));
                    return result;
                }

                if(rightLeftIsIte && rightRightIsIte){
                    Expression ifLeft = ((IfThenElse<?>) rightLeft).getIf();
                    Expression thenLeft = ((IfThenElse<?>) rightLeft).getThen();
                    Expression elseLeft = ((IfThenElse<?>) rightLeft).getElse();

                    Expression ifRight = ((IfThenElse<?>) rightRight).getIf();
                    Expression thenRight = ((IfThenElse<?>) rightRight).getThen();
                    Expression elseRight = ((IfThenElse<?>) rightRight).getElse();

                    Expression numericPart1= NumericCompound.create(thenLeft, opRight, thenRight);
                    Expression numericPart2 = NumericCompound.create(thenLeft, opRight, elseRight);
                    Expression numericPart3= NumericCompound.create(elseLeft, opRight, thenRight);
                    Expression numericPart4= NumericCompound.create(elseLeft, opRight, elseRight);

                    Expression comparatorPart1 = NumericBooleanExpression.create(thenExpression, comparator, numericPart1);
                    Expression comparatorPart2 = NumericBooleanExpression.create(thenExpression, comparator, numericPart2);
                    Expression comparatorPart3 = NumericBooleanExpression.create(thenExpression, comparator, numericPart3);
                    Expression comparatorPart4 = NumericBooleanExpression.create(thenExpression, comparator, numericPart4);
                    Expression comparatorPart5 = NumericBooleanExpression.create(elseExpression, comparator, numericPart1);
                    Expression comparatorPart6 = NumericBooleanExpression.create(elseExpression, comparator, numericPart2);
                    Expression comparatorPart7 = NumericBooleanExpression.create(elseExpression, comparator, numericPart3);
                    Expression comparatorPart8 = NumericBooleanExpression.create(elseExpression, comparator, numericPart4);

                    Expression propositionalPart1 = ExpressionUtil.and(ifCondition, ExpressionUtil.and(ifLeft, ExpressionUtil.and(ifRight, comparatorPart1)));
                    Expression propositionalPart2 = ExpressionUtil.and(ifCondition, ExpressionUtil.and(ifLeft, ExpressionUtil.and(Negation.create(ifRight), comparatorPart2)));
                    Expression propositionalPart3 = ExpressionUtil.and(ifCondition, ExpressionUtil.and(Negation.create(ifLeft), ExpressionUtil.and(ifRight, comparatorPart3)));
                    Expression propositionalPart4 = ExpressionUtil.and(ifCondition, ExpressionUtil.and(Negation.create(ifLeft), ExpressionUtil.and(Negation.create(ifRight), comparatorPart4)));
                    Expression propositionalPart5 = ExpressionUtil.and(Negation.create(ifCondition), ExpressionUtil.and(ifLeft, ExpressionUtil.and(ifRight, comparatorPart5)));
                    Expression propositionalPart6 = ExpressionUtil.and(Negation.create(ifCondition), ExpressionUtil.and(ifLeft, ExpressionUtil.and(Negation.create(ifRight), comparatorPart6)));
                    Expression propositionalPart7 = ExpressionUtil.and(Negation.create(ifCondition), ExpressionUtil.and(Negation.create(ifLeft), ExpressionUtil.and(ifRight, comparatorPart7)));
                    Expression propositionalPart8 = ExpressionUtil.and(Negation.create(ifCondition), ExpressionUtil.and(Negation.create(ifLeft), ExpressionUtil.and(Negation.create(ifRight), comparatorPart8)));

                    Expression result = ExpressionUtil.or(
                            propositionalPart1, ExpressionUtil.or(propositionalPart2, ExpressionUtil.or(propositionalPart3,
                                    ExpressionUtil.or(propositionalPart4, ExpressionUtil.or(propositionalPart5, ExpressionUtil.or(propositionalPart6,
                                            ExpressionUtil.or(propositionalPart7, propositionalPart8)))))));
                    return result;
                }
            }
        }
        if(!leftChildIsIte && rightChildIsIte){
            //case: right child is ItE
            Expression ifCondition = ((IfThenElse<?>) rightChild).getIf();
            Expression thenExpression = ((IfThenElse<?>) rightChild).getThen();
            Expression elseExpression = ((IfThenElse<?>) rightChild).getElse();

            if(!leftChildIsNum){
                Expression result = ExpressionUtil.or(
                        ExpressionUtil.and(ifCondition, NumericBooleanExpression.create(thenExpression, comparator, rightChild)),
                        ExpressionUtil.and(Negation.create(ifCondition), NumericBooleanExpression.create(elseExpression, comparator, rightChild)));
                return result;
            } else { //(leftChildIsNum)
                Expression leftLeft = ((NumericCompound<?>) leftChild).getLeft();
                NumericOperator opLeft = ((NumericCompound<?>) leftChild).getOperator();
                Expression leftRight = ((NumericCompound<?>) leftChild).getRight();

                boolean leftLeftIsIte = leftLeft instanceof IfThenElse;
                boolean leftRightIsIte = leftRight instanceof IfThenElse;

                if(!leftLeftIsIte && !leftRightIsIte){
                    Expression result = ExpressionUtil.or(
                            ExpressionUtil.and(ifCondition, NumericBooleanExpression.create(thenExpression, comparator, rightChild)),
                            ExpressionUtil.and(Negation.create(ifCondition), NumericBooleanExpression.create(elseExpression, comparator, rightChild)));
                    return result;
                }

                if(!leftLeftIsIte && leftRightIsIte){
                    Expression ifRight = ((IfThenElse<?>) leftRight).getIf();
                    Expression thenRight = ((IfThenElse<?>) leftRight).getThen();
                    Expression elseRight = ((IfThenElse<?>) leftRight).getElse();

                    Expression numericPart1 = NumericCompound.create(leftLeft, opLeft, thenRight);
                    Expression numericPart2 = NumericCompound.create(leftLeft, opLeft, elseRight);

                    Expression comparatorPart1 = NumericBooleanExpression.create(numericPart1, comparator, thenExpression);
                    Expression comparatorPart2 = NumericBooleanExpression.create(numericPart2, comparator, thenExpression);
                    Expression comparatorPart3 = NumericBooleanExpression.create(numericPart1, comparator, elseExpression);
                    Expression comparatorPart4 = NumericBooleanExpression.create(numericPart2, comparator, elseExpression);

                    Expression propositionalPart1 = ExpressionUtil.and(ifCondition, ExpressionUtil.and(ifRight, comparatorPart1));
                    Expression propositionalPart2 = ExpressionUtil.and(ifCondition, ExpressionUtil.and(Negation.create(ifRight), comparatorPart2));
                    Expression propositionalPart3 = ExpressionUtil.and(Negation.create(ifCondition), ExpressionUtil.and(ifRight, comparatorPart3));
                    Expression propositionalPart4 = ExpressionUtil.and(Negation.create(ifCondition), ExpressionUtil.and(Negation.create(ifRight), comparatorPart4));

                    Expression result = ExpressionUtil.or(
                            propositionalPart1, ExpressionUtil.or(propositionalPart2, ExpressionUtil.or(propositionalPart3, propositionalPart4)));
                    return result;
                }

                if(leftLeftIsIte && !leftRightIsIte){
                    Expression ifLeft = ((IfThenElse<?>) leftLeft).getIf();
                    Expression thenLeft = ((IfThenElse<?>) leftLeft).getThen();
                    Expression elseLeft = ((IfThenElse<?>) leftLeft).getElse();

                    Expression numericPart1 = NumericCompound.create(thenLeft, opLeft, leftLeft);
                    Expression numericPart2 = NumericCompound.create(elseLeft, opLeft, leftLeft);

                    Expression comparatorPart1 = NumericBooleanExpression.create(numericPart1, comparator, thenExpression);
                    Expression comparatorPart2 = NumericBooleanExpression.create(numericPart2, comparator, thenExpression);
                    Expression comparatorPart3 = NumericBooleanExpression.create(numericPart1, comparator, elseExpression);
                    Expression comparatorPart4 = NumericBooleanExpression.create(numericPart2, comparator, elseExpression);

                    Expression propositionalPart1 = ExpressionUtil.and(ifCondition, ExpressionUtil.and(ifLeft, comparatorPart1));
                    Expression propositionalPart2 = ExpressionUtil.and(ifCondition, ExpressionUtil.and(Negation.create(ifLeft), comparatorPart2));
                    Expression propositionalPart3 = ExpressionUtil.and(Negation.create(ifCondition), ExpressionUtil.and(ifLeft, comparatorPart3));
                    Expression propositionalPart4 = ExpressionUtil.and(Negation.create(ifCondition), ExpressionUtil.and(Negation.create(ifLeft), comparatorPart4));

                    Expression result = ExpressionUtil.or(
                            propositionalPart1, ExpressionUtil.or(propositionalPart2, ExpressionUtil.or(propositionalPart3, propositionalPart4)));
                    return result;

                }

                if(leftLeftIsIte && leftRightIsIte){
                    Expression ifLeft = ((IfThenElse<?>) leftLeft).getIf();
                    Expression thenLeft = ((IfThenElse<?>) leftLeft).getThen();
                    Expression elseLeft = ((IfThenElse<?>) leftLeft).getElse();

                    Expression ifRight = ((IfThenElse<?>) leftRight).getIf();
                    Expression thenRight = ((IfThenElse<?>) leftRight).getThen();
                    Expression elseRight = ((IfThenElse<?>) leftRight).getElse();

                    Expression numericPart1= NumericCompound.create(thenLeft, opLeft, thenRight);
                    Expression numericPart2 = NumericCompound.create(thenLeft, opLeft, elseRight);
                    Expression numericPart3= NumericCompound.create(elseLeft, opLeft, thenRight);
                    Expression numericPart4= NumericCompound.create(elseLeft, opLeft, elseRight);

                    Expression comparatorPart1 = NumericBooleanExpression.create(numericPart1, comparator, thenExpression);
                    Expression comparatorPart2 = NumericBooleanExpression.create(numericPart2, comparator, thenExpression);
                    Expression comparatorPart3 = NumericBooleanExpression.create(numericPart3, comparator, thenExpression);
                    Expression comparatorPart4 = NumericBooleanExpression.create(numericPart4, comparator, thenExpression);
                    Expression comparatorPart5 = NumericBooleanExpression.create(numericPart1, comparator, elseExpression);
                    Expression comparatorPart6 = NumericBooleanExpression.create(numericPart2, comparator, elseExpression);
                    Expression comparatorPart7 = NumericBooleanExpression.create(numericPart3, comparator, elseExpression);
                    Expression comparatorPart8 = NumericBooleanExpression.create(numericPart4, comparator, elseExpression);

                    Expression propositionalPart1 = ExpressionUtil.and(ifCondition, ExpressionUtil.and(ifLeft, ExpressionUtil.and(ifRight, comparatorPart1)));
                    Expression propositionalPart2 = ExpressionUtil.and(ifCondition, ExpressionUtil.and(ifLeft, ExpressionUtil.and(Negation.create(ifRight), comparatorPart2)));
                    Expression propositionalPart3 = ExpressionUtil.and(ifCondition, ExpressionUtil.and(Negation.create(ifLeft), ExpressionUtil.and(ifRight, comparatorPart3)));
                    Expression propositionalPart4 = ExpressionUtil.and(ifCondition, ExpressionUtil.and(Negation.create(ifLeft), ExpressionUtil.and(Negation.create(ifRight), comparatorPart4)));
                    Expression propositionalPart5 = ExpressionUtil.and(Negation.create(ifCondition), ExpressionUtil.and(ifLeft, ExpressionUtil.and(ifRight, comparatorPart5)));
                    Expression propositionalPart6 = ExpressionUtil.and(Negation.create(ifCondition), ExpressionUtil.and(ifLeft, ExpressionUtil.and(Negation.create(ifRight), comparatorPart6)));
                    Expression propositionalPart7 = ExpressionUtil.and(Negation.create(ifCondition), ExpressionUtil.and(Negation.create(ifLeft), ExpressionUtil.and(ifRight, comparatorPart7)));
                    Expression propositionalPart8 = ExpressionUtil.and(Negation.create(ifCondition), ExpressionUtil.and(Negation.create(ifLeft), ExpressionUtil.and(Negation.create(ifRight), comparatorPart8)));

                    Expression result = ExpressionUtil.or(propositionalPart1, propositionalPart2, propositionalPart3, propositionalPart4,
                            propositionalPart5, propositionalPart6, propositionalPart7, propositionalPart8);
                    return result;
                }
            }
        }

        //case: both children are NumericCompounds; they may contain further IfThenElses as children
        assert rightChildIsNum && leftChildIsNum;
        Expression leftLeft = ((NumericCompound<?>) leftChild).getLeft();
        NumericOperator opLeft = ((NumericCompound<?>) leftChild).getOperator();
        Expression leftRight = ((NumericCompound<?>) leftChild).getRight();

        Expression rightLeft = ((NumericCompound<?>) rightChild).getLeft();
        NumericOperator opRight = ((NumericCompound<?>) rightChild).getOperator();
        Expression rightRight = ((NumericCompound<?>) rightChild).getRight();

        boolean rightLeftIsIte = rightLeft instanceof IfThenElse;
        boolean rightRightIsIte = rightRight instanceof IfThenElse;
        boolean leftLeftIsIte = leftLeft instanceof IfThenElse;
        boolean leftRightIsIte = leftRight instanceof IfThenElse;

        if(!leftLeftIsIte && !leftRightIsIte){
            if(!rightLeftIsIte && !rightRightIsIte){
                return n;
            }
            if(!rightLeftIsIte && rightRightIsIte){
                Expression ifRightRight = ((IfThenElse<?>) rightRight).getIf();
                Expression thenRightRight = ((IfThenElse<?>) rightRight).getThen();
                Expression elseRightRight = ((IfThenElse<?>) rightRight).getElse();

                Expression numericPart5 = NumericCompound.create(rightLeft, opRight, thenRightRight);
                Expression numericPart6 = NumericCompound.create(rightLeft, opRight, elseRightRight);

                Expression comparatorPart1 = NumericBooleanExpression.create(leftChild, comparator, numericPart5);
                Expression comparatorPart2 = NumericBooleanExpression.create(leftChild, comparator, numericPart6);

                Expression propositionalPart1 = ExpressionUtil.and(ifRightRight, comparatorPart1);
                Expression propositionalPart2 = ExpressionUtil.and(Negation.create(ifRightRight), comparatorPart2);

                Expression result = ExpressionUtil.or(propositionalPart1, propositionalPart2);
                return result;
            }
            if(rightLeftIsIte && !rightRightIsIte){
                Expression ifRightLeft = ((IfThenElse<?>) rightLeft).getIf();
                Expression thenRightLeft = ((IfThenElse<?>) rightLeft).getThen();
                Expression elseRightLeft = ((IfThenElse<?>) rightLeft).getElse();

                Expression numericPart5 = NumericCompound.create(thenRightLeft, opRight, rightRight);
                Expression numericPart6 = NumericCompound.create(elseRightLeft, opRight, rightRight);

                Expression comparatorPart1 = NumericBooleanExpression.create(leftChild, comparator, numericPart5);
                Expression comparatorPart2 = NumericBooleanExpression.create(leftChild, comparator, numericPart6);

                Expression propositionalPart1 = ExpressionUtil.and(ifRightLeft, comparatorPart1);
                Expression propositionalPart2 = ExpressionUtil.and(Negation.create(ifRightLeft), comparatorPart2);

                Expression result = ExpressionUtil.or(propositionalPart1, propositionalPart2);
                return result;
            }
            if(rightLeftIsIte && rightRightIsIte){
                Expression ifRightLeft = ((IfThenElse<?>) rightLeft).getIf();
                Expression thenRightLeft = ((IfThenElse<?>) rightLeft).getThen();
                Expression elseRightLeft = ((IfThenElse<?>) rightLeft).getElse();
                Expression ifRightRight = ((IfThenElse<?>) rightRight).getIf();
                Expression thenRightRight = ((IfThenElse<?>) rightRight).getThen();
                Expression elseRightRight = ((IfThenElse<?>) rightRight).getElse();

                Expression numericPart1 = NumericCompound.create(thenRightLeft, opRight, thenRightRight);
                Expression numericPart2 = NumericCompound.create(thenRightLeft, opRight, elseRightRight);
                Expression numericPart3 = NumericCompound.create(elseRightLeft, opRight, thenRightRight);
                Expression numericPart4 = NumericCompound.create(elseRightLeft, opRight, elseRightRight);

                Expression comparatorPart1 = NumericBooleanExpression.create(leftChild, comparator, numericPart1);
                Expression comparatorPart2 = NumericBooleanExpression.create(leftChild, comparator, numericPart2);
                Expression comparatorPart3 = NumericBooleanExpression.create(leftChild, comparator, numericPart3);
                Expression comparatorPart4 = NumericBooleanExpression.create(leftChild, comparator, numericPart4);

                Expression propositionalPart1 = ExpressionUtil.and(ifRightLeft, ifRightRight, comparatorPart1);
                Expression propositionalPart2 = ExpressionUtil.and(ifRightLeft, Negation.create(ifRightRight), comparatorPart2);
                Expression propositionalPart3 = ExpressionUtil.and(Negation.create(ifRightLeft), ifRightRight, comparatorPart3);
                Expression propositionalPart4 = ExpressionUtil.and(Negation.create(ifRightLeft), Negation.create(ifRightRight), comparatorPart4);

                Expression result = ExpressionUtil.or(propositionalPart1, propositionalPart2, propositionalPart3, propositionalPart4);
                return result;
            }
        }

        if(!leftLeftIsIte && leftRightIsIte){
            Expression ifLeftRight = ((IfThenElse<?>) leftRight).getIf();
            Expression thenLeftRight = ((IfThenElse<?>) leftRight).getThen();
            Expression elseLeftRight = ((IfThenElse<?>) leftRight).getElse();

            Expression numericPart5 = NumericCompound.create(leftLeft, opLeft, thenLeftRight);
            Expression numericPart6 = NumericCompound.create(leftLeft, opLeft, elseLeftRight);

            if(!rightLeftIsIte && !rightRightIsIte){
                Expression comparatorPart1 = NumericBooleanExpression.create(numericPart5, comparator, rightChild);
                Expression comparatorPart2 = NumericBooleanExpression.create(numericPart6, comparator, rightChild);

                Expression propositionalPart1 = ExpressionUtil.and(ifLeftRight, comparatorPart1);
                Expression propositionalPart2 = ExpressionUtil.and(Negation.create(ifLeftRight), comparatorPart2);

                Expression result = ExpressionUtil.or(propositionalPart1, propositionalPart2);
                return result;
            }
            if(!rightLeftIsIte && rightRightIsIte){
                Expression ifRightRight = ((IfThenElse<?>) rightRight).getIf();
                Expression thenRightRight = ((IfThenElse<?>) rightRight).getThen();
                Expression elseRightRight = ((IfThenElse<?>) rightRight).getElse();

                Expression numericPart7 = NumericCompound.create(rightLeft, opRight, thenRightRight);
                Expression numericPart8 = NumericCompound.create(rightLeft, opRight, elseRightRight);

                Expression comparatorPart1 = NumericBooleanExpression.create(numericPart5, comparator, numericPart7);
                Expression comparatorPart2 = NumericBooleanExpression.create(numericPart6, comparator, numericPart7);
                Expression comparatorPart3 = NumericBooleanExpression.create(numericPart5, comparator, numericPart8);
                Expression comparatorPart4 = NumericBooleanExpression.create(numericPart6, comparator, numericPart8);

                Expression propositionalPart1 = ExpressionUtil.and(ifLeftRight, ifRightRight, comparatorPart1);
                Expression propositionalPart2 = ExpressionUtil.and(Negation.create(ifLeftRight), ifRightRight, comparatorPart2);
                Expression propositionalPart3 = ExpressionUtil.and(ifLeftRight, Negation.create(ifRightRight), comparatorPart3);
                Expression propositionalPart4 = ExpressionUtil.and(Negation.create(ifLeftRight), Negation.create(ifRightRight), comparatorPart4);

                Expression result = ExpressionUtil.or(propositionalPart1, propositionalPart2, propositionalPart3, propositionalPart4);
                return result;
            }
            if(rightLeftIsIte && !rightRightIsIte){
                Expression ifRightLeft = ((IfThenElse<?>) rightLeft).getIf();
                Expression thenRightLeft = ((IfThenElse<?>) rightLeft).getThen();
                Expression elseRightLeft = ((IfThenElse<?>) rightLeft).getElse();

                Expression numericPart7 = NumericCompound.create(thenRightLeft, opRight, rightRight);
                Expression numericPart8 = NumericCompound.create(elseRightLeft, opRight, rightRight);

                Expression comparatorPart1 = NumericBooleanExpression.create(numericPart5, comparator, numericPart7);
                Expression comparatorPart2 = NumericBooleanExpression.create(numericPart6, comparator, numericPart7);
                Expression comparatorPart3 = NumericBooleanExpression.create(numericPart5, comparator, numericPart8);
                Expression comparatorPart4 = NumericBooleanExpression.create(numericPart6, comparator, numericPart8);

                Expression propositionalPart1 = ExpressionUtil.and(ifLeftRight, ifRightLeft, comparatorPart1);
                Expression propositionalPart2 = ExpressionUtil.and(Negation.create(ifLeftRight), ifRightLeft, comparatorPart2);
                Expression propositionalPart3 = ExpressionUtil.and(ifLeftRight, Negation.create(ifRightLeft), comparatorPart3);
                Expression propositionalPart4 = ExpressionUtil.and(Negation.create(ifLeftRight), Negation.create(ifRightLeft), comparatorPart4);

                Expression result = ExpressionUtil.or(propositionalPart1, propositionalPart2, propositionalPart3, propositionalPart4);
                return result;
            }
            if(rightLeftIsIte && rightRightIsIte){
                Expression ifRightLeft = ((IfThenElse<?>) rightLeft).getIf();
                Expression thenRightLeft = ((IfThenElse<?>) rightLeft).getThen();
                Expression elseRightLeft = ((IfThenElse<?>) rightLeft).getElse();
                Expression ifRightRight = ((IfThenElse<?>) rightRight).getIf();
                Expression thenRightRight = ((IfThenElse<?>) rightRight).getThen();
                Expression elseRightRight = ((IfThenElse<?>) rightRight).getElse();

                Expression numericPart1 = NumericCompound.create(thenRightLeft, opRight, thenRightRight);
                Expression numericPart2 = NumericCompound.create(thenRightLeft, opRight, elseRightRight);
                Expression numericPart3 = NumericCompound.create(elseRightLeft, opRight, thenRightRight);
                Expression numericPart4 = NumericCompound.create(elseRightLeft, opRight, elseRightRight);

                Expression comparatorPart1 = NumericBooleanExpression.create(numericPart5, comparator, numericPart1);
                Expression comparatorPart2 = NumericBooleanExpression.create(numericPart6, comparator, numericPart1);
                Expression comparatorPart3 = NumericBooleanExpression.create(numericPart5, comparator, numericPart2);
                Expression comparatorPart4 = NumericBooleanExpression.create(numericPart6, comparator, numericPart2);
                Expression comparatorPart5 = NumericBooleanExpression.create(numericPart5, comparator, numericPart3);
                Expression comparatorPart6 = NumericBooleanExpression.create(numericPart6, comparator, numericPart3);
                Expression comparatorPart7 = NumericBooleanExpression.create(numericPart5, comparator, numericPart4);
                Expression comparatorPart8 = NumericBooleanExpression.create(numericPart6, comparator, numericPart4);

                Expression propositionalPart1 = ExpressionUtil.and(ifLeftRight, ifRightLeft, ifRightRight, comparatorPart1);
                Expression propositionalPart2 = ExpressionUtil.and(Negation.create(ifLeftRight), ifRightLeft, ifRightRight, comparatorPart2);
                Expression propositionalPart3 = ExpressionUtil.and(ifLeftRight, ifRightLeft, Negation.create(ifRightRight), comparatorPart3);
                Expression propositionalPart4 = ExpressionUtil.and(Negation.create(ifLeftRight), ifRightLeft, Negation.create(ifRightRight), comparatorPart4);
                Expression propositionalPart5 = ExpressionUtil.and(ifLeftRight, Negation.create(ifRightLeft), ifRightRight, comparatorPart5);
                Expression propositionalPart6 = ExpressionUtil.and(Negation.create(ifLeftRight), Negation.create(ifRightLeft), ifRightRight, comparatorPart6);
                Expression propositionalPart7 = ExpressionUtil.and(ifLeftRight, Negation.create(ifRightLeft), Negation.create(ifRightRight), comparatorPart7);
                Expression propositionalPart8 = ExpressionUtil.and(Negation.create(ifLeftRight), Negation.create(ifRightLeft), Negation.create(ifRightRight), comparatorPart8);

                Expression result = ExpressionUtil.or(propositionalPart1, propositionalPart2, propositionalPart3, propositionalPart4,
                        propositionalPart5, propositionalPart6, propositionalPart7, propositionalPart8);
                return result;
            }
        }

        if(leftLeftIsIte && !leftRightIsIte){
            Expression ifLeftLeft = ((IfThenElse<?>) leftLeft).getIf();
            Expression thenLeftLeft = ((IfThenElse<?>) leftLeft).getThen();
            Expression elseLeftLeft = ((IfThenElse<?>) leftLeft).getElse();

            Expression numericPart5 = NumericCompound.create(thenLeftLeft, opLeft, leftRight);
            Expression numericPart6 = NumericCompound.create(elseLeftLeft, opLeft, leftRight);

            if(!rightLeftIsIte && !rightRightIsIte){
                Expression comparatorPart1 = NumericBooleanExpression.create(numericPart5, comparator, rightChild);
                Expression comparatorPart2 = NumericBooleanExpression.create(numericPart6, comparator, rightChild);

                Expression propositionalPart1 = ExpressionUtil.and(ifLeftLeft, comparatorPart1);
                Expression propositionalPart2 = ExpressionUtil.and(Negation.create(ifLeftLeft), comparatorPart2);

                Expression result = ExpressionUtil.or(propositionalPart1, propositionalPart2);
                return result;
            }
            if(!rightLeftIsIte && rightRightIsIte){
                Expression ifRightRight = ((IfThenElse<?>) rightRight).getIf();
                Expression thenRightRight = ((IfThenElse<?>) rightRight).getThen();
                Expression elseRightRight = ((IfThenElse<?>) rightRight).getElse();

                Expression numericPart7 = NumericCompound.create(rightLeft, opRight, thenRightRight);
                Expression numericPart8 = NumericCompound.create(rightLeft, opRight, elseRightRight);

                Expression comparatorPart1 = NumericBooleanExpression.create(numericPart5, comparator, numericPart7);
                Expression comparatorPart2 = NumericBooleanExpression.create(numericPart6, comparator, numericPart7);
                Expression comparatorPart3 = NumericBooleanExpression.create(numericPart5, comparator, numericPart8);
                Expression comparatorPart4 = NumericBooleanExpression.create(numericPart6, comparator, numericPart8);

                Expression propositionalPart1 = ExpressionUtil.and(ifLeftLeft, ifRightRight, comparatorPart1);
                Expression propositionalPart2 = ExpressionUtil.and(Negation.create(ifLeftLeft), ifRightRight, comparatorPart2);
                Expression propositionalPart3 = ExpressionUtil.and(ifLeftLeft, Negation.create(ifRightRight), comparatorPart3);
                Expression propositionalPart4 = ExpressionUtil.and(Negation.create(ifLeftLeft), Negation.create(ifRightRight), comparatorPart4);

                Expression result = ExpressionUtil.or(propositionalPart1, propositionalPart2, propositionalPart3, propositionalPart4);
                return result;
            }
            if(rightLeftIsIte && !rightRightIsIte){
                Expression ifRightLeft = ((IfThenElse<?>) rightLeft).getIf();
                Expression thenRightLeft = ((IfThenElse<?>) rightLeft).getThen();
                Expression elseRightLeft = ((IfThenElse<?>) rightLeft).getElse();

                Expression numericPart7 = NumericCompound.create(thenRightLeft, opRight, rightRight);
                Expression numericPart8 = NumericCompound.create(elseRightLeft, opRight, rightRight);

                Expression comparatorPart1 = NumericBooleanExpression.create(numericPart5, comparator, numericPart7);
                Expression comparatorPart2 = NumericBooleanExpression.create(numericPart6, comparator, numericPart7);
                Expression comparatorPart3 = NumericBooleanExpression.create(numericPart5, comparator, numericPart8);
                Expression comparatorPart4 = NumericBooleanExpression.create(numericPart6, comparator, numericPart8);

                Expression propositionalPart1 = ExpressionUtil.and(ifLeftLeft, ifRightLeft, comparatorPart1);
                Expression propositionalPart2 = ExpressionUtil.and(Negation.create(ifLeftLeft), ifRightLeft, comparatorPart2);
                Expression propositionalPart3 = ExpressionUtil.and(ifLeftLeft, Negation.create(ifRightLeft), comparatorPart3);
                Expression propositionalPart4 = ExpressionUtil.and(Negation.create(ifLeftLeft), Negation.create(ifRightLeft), comparatorPart4);

                Expression result = ExpressionUtil.or(propositionalPart1, propositionalPart2, propositionalPart3, propositionalPart4);
                return result;
            }
            if(rightLeftIsIte && rightRightIsIte){
                Expression ifRightLeft = ((IfThenElse<?>) rightLeft).getIf();
                Expression thenRightLeft = ((IfThenElse<?>) rightLeft).getThen();
                Expression elseRightLeft = ((IfThenElse<?>) rightLeft).getElse();
                Expression ifRightRight = ((IfThenElse<?>) rightRight).getIf();
                Expression thenRightRight = ((IfThenElse<?>) rightRight).getThen();
                Expression elseRightRight = ((IfThenElse<?>) rightRight).getElse();

                Expression numericPart1 = NumericCompound.create(thenRightLeft, opRight, thenRightRight);
                Expression numericPart2 = NumericCompound.create(thenRightLeft, opRight, elseRightRight);
                Expression numericPart3 = NumericCompound.create(elseRightLeft, opRight, thenRightRight);
                Expression numericPart4 = NumericCompound.create(elseRightLeft, opRight, elseRightRight);

                Expression comparatorPart1 = NumericBooleanExpression.create(numericPart5, comparator, numericPart1);
                Expression comparatorPart2 = NumericBooleanExpression.create(numericPart6, comparator, numericPart1);
                Expression comparatorPart3 = NumericBooleanExpression.create(numericPart5, comparator, numericPart2);
                Expression comparatorPart4 = NumericBooleanExpression.create(numericPart6, comparator, numericPart2);
                Expression comparatorPart5 = NumericBooleanExpression.create(numericPart5, comparator, numericPart3);
                Expression comparatorPart6 = NumericBooleanExpression.create(numericPart6, comparator, numericPart3);
                Expression comparatorPart7 = NumericBooleanExpression.create(numericPart5, comparator, numericPart4);
                Expression comparatorPart8 = NumericBooleanExpression.create(numericPart6, comparator, numericPart4);

                Expression propositionalPart1 = ExpressionUtil.and(ifLeftLeft, ifRightLeft, ifRightRight, comparatorPart1);
                Expression propositionalPart2 = ExpressionUtil.and(Negation.create(ifLeftLeft), ifRightLeft, ifRightRight, comparatorPart2);
                Expression propositionalPart3 = ExpressionUtil.and(ifLeftLeft, ifRightLeft, Negation.create(ifRightRight), comparatorPart3);
                Expression propositionalPart4 = ExpressionUtil.and(Negation.create(ifLeftLeft), ifRightLeft, Negation.create(ifRightRight), comparatorPart4);
                Expression propositionalPart5 = ExpressionUtil.and(ifLeftLeft, Negation.create(ifRightLeft), ifRightRight, comparatorPart5);
                Expression propositionalPart6 = ExpressionUtil.and(Negation.create(ifLeftLeft), Negation.create(ifRightLeft), ifRightRight, comparatorPart6);
                Expression propositionalPart7 = ExpressionUtil.and(ifLeftLeft, Negation.create(ifRightLeft), Negation.create(ifRightRight), comparatorPart7);
                Expression propositionalPart8 = ExpressionUtil.and(Negation.create(ifLeftLeft), Negation.create(ifRightLeft), Negation.create(ifRightRight), comparatorPart8);

                Expression result = ExpressionUtil.or(propositionalPart1, propositionalPart2, propositionalPart3, propositionalPart4,
                        propositionalPart5, propositionalPart6, propositionalPart7, propositionalPart8);
                return result;
            }
        }

        if(leftLeftIsIte && leftRightIsIte) {
            Expression ifLeftLeft = ((IfThenElse<?>) leftLeft).getIf();
            Expression thenLeftLeft = ((IfThenElse<?>) leftLeft).getThen();
            Expression elseLeftLeft = ((IfThenElse<?>) leftLeft).getElse();
            Expression ifLeftRight = ((IfThenElse<?>) leftRight).getIf();
            Expression thenLeftRight = ((IfThenElse<?>) leftRight).getThen();
            Expression elseLeftRight = ((IfThenElse<?>) leftRight).getElse();

            Expression numericPart1 = NumericCompound.create(thenLeftLeft, opLeft, thenLeftRight);
            Expression numericPart2 = NumericCompound.create(thenLeftLeft, opLeft, elseLeftRight);
            Expression numericPart3 = NumericCompound.create(elseLeftLeft, opLeft, thenLeftRight);
            Expression numericPart4 = NumericCompound.create(elseLeftLeft, opLeft, elseLeftRight);

            if(!rightLeftIsIte && !rightRightIsIte) {
                Expression comparatorPart1 = NumericBooleanExpression.create(numericPart1, comparator, rightChild);
                Expression comparatorPart2 = NumericBooleanExpression.create(numericPart2, comparator, rightChild);
                Expression comparatorPart3 = NumericBooleanExpression.create(numericPart3, comparator, rightChild);
                Expression comparatorPart4 = NumericBooleanExpression.create(numericPart4, comparator, rightChild);

                Expression propositionalPart1 = ExpressionUtil.and(ifLeftLeft, ifLeftRight, comparatorPart1);
                Expression propositionalPart2 = ExpressionUtil.and(ifLeftLeft, Negation.create(ifLeftRight), comparatorPart2);
                Expression propositionalPart3 = ExpressionUtil.and(Negation.create(ifLeftLeft), ifLeftRight, comparatorPart3);
                Expression propositionalPart4 = ExpressionUtil.and(Negation.create(ifLeftLeft), Negation.create(ifLeftRight), comparatorPart4);

                Expression result = ExpressionUtil.or(propositionalPart1, propositionalPart2, propositionalPart3, propositionalPart4);
                return result;
            }

            if(!rightLeftIsIte && rightRightIsIte){
                Expression ifRightRight = ((IfThenElse<?>) rightRight).getIf();
                Expression thenRightRight = ((IfThenElse<?>) rightRight).getThen();
                Expression elseRightRight = ((IfThenElse<?>) rightRight).getElse();

                Expression numericPart5 = NumericCompound.create(rightLeft, opRight, thenRightRight);
                Expression numericPart6 = NumericCompound.create(rightLeft, opRight, elseRightRight);

                Expression comparatorPart1 = NumericBooleanExpression.create(numericPart1, comparator, numericPart5);
                Expression comparatorPart2 = NumericBooleanExpression.create(numericPart2, comparator, numericPart5);
                Expression comparatorPart3 = NumericBooleanExpression.create(numericPart3, comparator, numericPart5);
                Expression comparatorPart4 = NumericBooleanExpression.create(numericPart4, comparator, numericPart5);
                Expression comparatorPart5 = NumericBooleanExpression.create(numericPart1, comparator, numericPart6);
                Expression comparatorPart6 = NumericBooleanExpression.create(numericPart2, comparator, numericPart6);
                Expression comparatorPart7 = NumericBooleanExpression.create(numericPart3, comparator, numericPart6);
                Expression comparatorPart8 = NumericBooleanExpression.create(numericPart4, comparator, numericPart6);

                Expression propositionalPart1 = ExpressionUtil.and(ifLeftLeft, ifLeftRight, ifRightRight, comparatorPart1);
                Expression propositionalPart2 = ExpressionUtil.and(ifLeftLeft, Negation.create(ifLeftRight), ifRightRight, comparatorPart2);
                Expression propositionalPart3 = ExpressionUtil.and(Negation.create(ifLeftLeft), ifLeftRight, ifRightRight, comparatorPart3);
                Expression propositionalPart4 = ExpressionUtil.and(Negation.create(ifLeftLeft), Negation.create(ifLeftRight), ifRightRight, comparatorPart4);
                Expression propositionalPart5 = ExpressionUtil.and(ifLeftLeft, ifLeftRight, Negation.create(ifRightRight), comparatorPart5);
                Expression propositionalPart6 = ExpressionUtil.and(ifLeftLeft, Negation.create(ifLeftRight), Negation.create(ifRightRight), comparatorPart6);
                Expression propositionalPart7 = ExpressionUtil.and(Negation.create(ifLeftLeft), ifLeftRight, Negation.create(ifRightRight), comparatorPart7);
                Expression propositionalPart8 = ExpressionUtil.and(Negation.create(ifLeftLeft), Negation.create(ifLeftRight), Negation.create(ifRightRight), comparatorPart8);

                Expression result = ExpressionUtil.or(propositionalPart1, propositionalPart2, propositionalPart3, propositionalPart4,
                        propositionalPart5, propositionalPart6, propositionalPart7, propositionalPart8);
                return result;
            }

            if(rightLeftIsIte && !rightRightIsIte){
                Expression ifRightLeft = ((IfThenElse<?>) rightLeft).getIf();
                Expression thenRightLeft = ((IfThenElse<?>) rightLeft).getThen();
                Expression elseRightLeft = ((IfThenElse<?>) rightLeft).getElse();

                Expression numericPart5 = NumericCompound.create(thenRightLeft, opRight, rightRight);
                Expression numericPart6 = NumericCompound.create(elseRightLeft, opRight, rightRight);

                Expression comparatorPart1 = NumericBooleanExpression.create(numericPart1, comparator, numericPart5);
                Expression comparatorPart2 = NumericBooleanExpression.create(numericPart2, comparator, numericPart5);
                Expression comparatorPart3 = NumericBooleanExpression.create(numericPart3, comparator, numericPart5);
                Expression comparatorPart4 = NumericBooleanExpression.create(numericPart4, comparator, numericPart5);
                Expression comparatorPart5 = NumericBooleanExpression.create(numericPart1, comparator, numericPart6);
                Expression comparatorPart6 = NumericBooleanExpression.create(numericPart2, comparator, numericPart6);
                Expression comparatorPart7 = NumericBooleanExpression.create(numericPart3, comparator, numericPart6);
                Expression comparatorPart8 = NumericBooleanExpression.create(numericPart4, comparator, numericPart6);

                Expression propositionalPart1 = ExpressionUtil.and(ifLeftLeft, ifLeftRight, ifRightLeft, comparatorPart1);
                Expression propositionalPart2 = ExpressionUtil.and(ifLeftLeft, Negation.create(ifLeftRight), ifRightLeft, comparatorPart2);
                Expression propositionalPart3 = ExpressionUtil.and(Negation.create(ifLeftLeft), ifLeftRight, ifRightLeft, comparatorPart3);
                Expression propositionalPart4 = ExpressionUtil.and(Negation.create(ifLeftLeft), Negation.create(ifLeftRight), ifRightLeft, comparatorPart4);
                Expression propositionalPart5 = ExpressionUtil.and(ifLeftLeft, ifLeftRight, Negation.create(ifRightLeft), comparatorPart5);
                Expression propositionalPart6 = ExpressionUtil.and(ifLeftLeft, Negation.create(ifLeftRight), Negation.create(ifRightLeft), comparatorPart6);
                Expression propositionalPart7 = ExpressionUtil.and(Negation.create(ifLeftLeft), ifLeftRight, Negation.create(ifRightLeft), comparatorPart7);
                Expression propositionalPart8 = ExpressionUtil.and(Negation.create(ifLeftLeft), Negation.create(ifLeftRight), Negation.create(ifRightLeft), comparatorPart8);

                Expression result = ExpressionUtil.or(propositionalPart1, propositionalPart2, propositionalPart3, propositionalPart4,
                        propositionalPart5, propositionalPart6, propositionalPart7, propositionalPart8);
                return result;
            }

            if (rightLeftIsIte && rightRightIsIte) {
                Expression ifRightLeft = ((IfThenElse<?>) rightLeft).getIf();
                Expression thenRightLeft = ((IfThenElse<?>) rightLeft).getThen();
                Expression elseRightLeft = ((IfThenElse<?>) rightLeft).getElse();
                Expression ifRightRight = ((IfThenElse<?>) rightRight).getIf();
                Expression thenRightRight = ((IfThenElse<?>) rightRight).getThen();
                Expression elseRightRight = ((IfThenElse<?>) rightRight).getElse();

                Expression numericPart5 = NumericCompound.create(thenRightLeft, opRight, thenRightRight);
                Expression numericPart6 = NumericCompound.create(thenRightLeft, opRight, elseRightRight);
                Expression numericPart7 = NumericCompound.create(elseRightLeft, opRight, thenRightRight);
                Expression numericPart8 = NumericCompound.create(elseRightLeft, opRight, elseRightRight);

                Expression comparatorPart1 = NumericBooleanExpression.create(numericPart1, comparator, numericPart5);
                Expression comparatorPart2 = NumericBooleanExpression.create(numericPart2, comparator, numericPart5);
                Expression comparatorPart3 = NumericBooleanExpression.create(numericPart3, comparator, numericPart5);
                Expression comparatorPart4 = NumericBooleanExpression.create(numericPart4, comparator, numericPart5);
                Expression comparatorPart5 = NumericBooleanExpression.create(numericPart1, comparator, numericPart6);
                Expression comparatorPart6 = NumericBooleanExpression.create(numericPart2, comparator, numericPart6);
                Expression comparatorPart7 = NumericBooleanExpression.create(numericPart3, comparator, numericPart6);
                Expression comparatorPart8 = NumericBooleanExpression.create(numericPart4, comparator, numericPart6);
                Expression comparatorPart9 = NumericBooleanExpression.create(numericPart1, comparator, numericPart7);
                Expression comparatorPart10 = NumericBooleanExpression.create(numericPart2, comparator, numericPart7);
                Expression comparatorPart11 = NumericBooleanExpression.create(numericPart3, comparator, numericPart7);
                Expression comparatorPart12 = NumericBooleanExpression.create(numericPart4, comparator, numericPart7);
                Expression comparatorPart13 = NumericBooleanExpression.create(numericPart1, comparator, numericPart8);
                Expression comparatorPart14 = NumericBooleanExpression.create(numericPart2, comparator, numericPart8);
                Expression comparatorPart15 = NumericBooleanExpression.create(numericPart3, comparator, numericPart8);
                Expression comparatorPart16 = NumericBooleanExpression.create(numericPart4, comparator, numericPart8);

                Expression propositionalPart1 = ExpressionUtil.and(ifLeftLeft, ifLeftRight, ifRightLeft, ifRightRight, comparatorPart1);
                Expression propositionalPart2 = ExpressionUtil.and(ifLeftLeft, Negation.create(ifLeftRight), ifRightLeft, ifRightRight, comparatorPart2);
                Expression propositionalPart3 = ExpressionUtil.and(Negation.create(ifLeftLeft), ifLeftRight, ifRightLeft, ifRightRight, comparatorPart3);
                Expression propositionalPart4 = ExpressionUtil.and(Negation.create(ifLeftLeft), Negation.create(ifLeftRight), ifRightLeft, ifRightRight, comparatorPart4);
                Expression propositionalPart5 = ExpressionUtil.and(ifLeftLeft, ifLeftRight, ifRightLeft, Negation.create(ifRightRight), comparatorPart5);
                Expression propositionalPart6 = ExpressionUtil.and(ifLeftLeft, Negation.create(ifLeftRight), ifRightLeft, Negation.create(ifRightRight), comparatorPart6);
                Expression propositionalPart7 = ExpressionUtil.and(Negation.create(ifLeftLeft), ifLeftRight, ifRightLeft, Negation.create(ifRightRight), comparatorPart7);
                Expression propositionalPart8 = ExpressionUtil.and(Negation.create(ifLeftLeft), Negation.create(ifLeftRight), ifRightLeft, Negation.create(ifRightRight), comparatorPart8);
                Expression propositionalPart9 = ExpressionUtil.and(ifLeftLeft, ifLeftRight, Negation.create(ifRightLeft), ifRightRight, comparatorPart9);
                Expression propositionalPart10 = ExpressionUtil.and(ifLeftLeft, Negation.create(ifLeftRight), Negation.create(ifRightLeft), ifRightRight, comparatorPart10);
                Expression propositionalPart11 = ExpressionUtil.and(Negation.create(ifLeftLeft), ifLeftRight, Negation.create(ifRightLeft), ifRightRight, comparatorPart11);
                Expression propositionalPart12 = ExpressionUtil.and(Negation.create(ifLeftLeft), Negation.create(ifLeftRight), Negation.create(ifRightLeft), ifRightRight, comparatorPart12);
                Expression propositionalPart13 = ExpressionUtil.and(ifLeftLeft, ifLeftRight, Negation.create(ifRightLeft), Negation.create(ifRightRight), comparatorPart13);
                Expression propositionalPart14 = ExpressionUtil.and(ifLeftLeft, Negation.create(ifLeftRight), Negation.create(ifRightLeft), Negation.create(ifRightRight), comparatorPart14);
                Expression propositionalPart15 = ExpressionUtil.and(Negation.create(ifLeftLeft), ifLeftRight, Negation.create(ifRightLeft), Negation.create(ifRightRight), comparatorPart15);
                Expression propositionalPart16 = ExpressionUtil.and(Negation.create(ifLeftLeft), Negation.create(ifLeftRight), Negation.create(ifRightLeft), Negation.create(ifRightRight), comparatorPart16);

                Expression result = ExpressionUtil.or(propositionalPart1, propositionalPart2, propositionalPart3, propositionalPart4,
                        propositionalPart5, propositionalPart6, propositionalPart7, propositionalPart8,
                        propositionalPart9, propositionalPart10, propositionalPart11, propositionalPart12,
                        propositionalPart13, propositionalPart14, propositionalPart15, propositionalPart16);
                return result;
            }
        }
        throw new UnsupportedOperationException("Missing or wrong case in handling of Numeric IfThenElse.");
    }

    @Override
    public Expression<?> visit(NumericCompound n, Void data) {
        return n;
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
