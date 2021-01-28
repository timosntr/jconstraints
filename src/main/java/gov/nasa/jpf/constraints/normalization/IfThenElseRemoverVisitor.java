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
import gov.nasa.jpf.constraints.util.DuplicatingVisitor;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import org.apache.commons.math3.analysis.function.Exp;

//only removes IfThenElse Expressions up to the first level of NumericCompounds;
//an IfThenElse in an IfThenElse in a NumericBooleanExpression is not removed
//not sure yet how to remove them from deeper levels properly
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
                    (Expression<Boolean>) firstPart,
                    LogicalOperator.AND,
                    secondPart);

            return result;
        } else {
            //a numeric IfThenElse in a numeric IfThenElse will return here unflattened
            return expr;
        }
    }

    /*@Override
    public Expression<?> visit(NumericBooleanExpression n, Void data) {
        return n;
    }*/

    //ToDo: something is wrong -> but fails first together with renaming
    // in z3 (after nnf in cvc4)
    @Override
    public Expression<?> visit(NumericBooleanExpression n, Void data) {
        //Todo: not optimized for cnf yet, probably too expensive
        //TODO: handle inner IfThenElse?
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

        if(leftChildIsIte){
            Expression leftIfCondition = ((IfThenElse<?>) leftChild).getIf();
            Expression leftThenExpression = ((IfThenElse<?>) leftChild).getThen();
            Expression leftElseExpression = ((IfThenElse<?>) leftChild).getElse();

            if(rightChildIsIte){//leftChildIsIte && rightChildIsIte
                //case: both children are Ite
                Expression rightIfCondition = ((IfThenElse<?>) rightChild).getIf();
                Expression rightThenExpression = ((IfThenElse<?>) rightChild).getThen();
                Expression rightElseExpression = ((IfThenElse<?>) rightChild).getElse();

                Expression part1 = ExpressionUtil.and(leftIfCondition, rightIfCondition, NumericBooleanExpression.create(leftThenExpression, comparator, rightThenExpression));
                Expression part2 = ExpressionUtil.and(leftIfCondition, Negation.create(rightIfCondition), NumericBooleanExpression.create(leftThenExpression, comparator, rightElseExpression));
                Expression part3 = ExpressionUtil.and(Negation.create(leftIfCondition), rightIfCondition, NumericBooleanExpression.create(leftElseExpression, comparator, rightThenExpression));
                Expression part4 = ExpressionUtil.and(Negation.create(leftIfCondition), Negation.create(rightIfCondition), NumericBooleanExpression.create(leftElseExpression, comparator, rightElseExpression));

                Expression result = ExpressionUtil.or(part1, part2, part3, part4);
                return result;
            } else {//leftChildIsIte && !rightChildIsIte
                if(!rightChildIsNum){
                    Expression<Boolean> result = ExpressionUtil.or(
                            ExpressionUtil.and(leftIfCondition, NumericBooleanExpression.create(leftThenExpression, comparator, rightChild)),
                            ExpressionUtil.and(Negation.create(leftIfCondition), NumericBooleanExpression.create(leftElseExpression, comparator, rightChild)));
                    return result;
                } else { //leftChildIsIte && rightChildIsNum
                    Expression rightLeft = ((NumericCompound<?>) rightChild).getLeft();
                    NumericOperator opRight = ((NumericCompound<?>) rightChild).getOperator();
                    Expression rightRight = ((NumericCompound<?>) rightChild).getRight();

                    boolean rightLeftIsIte = rightLeft instanceof IfThenElse;
                    boolean rightRightIsIte = rightRight instanceof IfThenElse;

                    if(!rightLeftIsIte){
                        if(!rightRightIsIte) {//leftChildIsIte && !rightLeftIsIte && !rightRightIsIte
                            Expression<Boolean> result = ExpressionUtil.or(
                                    ExpressionUtil.and(leftIfCondition, NumericBooleanExpression.create(leftThenExpression, comparator, rightChild)),
                                    ExpressionUtil.and(Negation.create(leftIfCondition), NumericBooleanExpression.create(leftElseExpression, comparator, rightChild)));
                            return result;
                        } else {//leftChildIsIte && !rightLeftIsIte && rightRightIsIte
                            Expression ifRight = ((IfThenElse<?>) rightRight).getIf();
                            Expression thenRight = ((IfThenElse<?>) rightRight).getThen();
                            Expression elseRight = ((IfThenElse<?>) rightRight).getElse();

                            Expression numericPart1 = NumericCompound.create(rightLeft, opRight, thenRight);
                            Expression numericPart2 = NumericCompound.create(rightLeft, opRight, elseRight);

                            Expression comparatorPart1 = NumericBooleanExpression.create(leftThenExpression, comparator, numericPart1);
                            Expression comparatorPart2 = NumericBooleanExpression.create(leftThenExpression, comparator, numericPart2);
                            Expression comparatorPart3 = NumericBooleanExpression.create(leftElseExpression, comparator, numericPart1);
                            Expression comparatorPart4 = NumericBooleanExpression.create(leftElseExpression, comparator, numericPart2);

                            Expression propositionalPart1 = ExpressionUtil.and(leftIfCondition, ifRight, comparatorPart1);
                            Expression propositionalPart2 = ExpressionUtil.and(leftIfCondition, Negation.create(ifRight), comparatorPart2);
                            Expression propositionalPart3 = ExpressionUtil.and(Negation.create(leftIfCondition), ifRight, comparatorPart3);
                            Expression propositionalPart4 = ExpressionUtil.and(Negation.create(leftIfCondition), Negation.create(ifRight), comparatorPart4);

                            Expression<Boolean> result = ExpressionUtil.or(propositionalPart1, propositionalPart2, propositionalPart3, propositionalPart4);
                            return result;
                        }
                    } else {//leftChildIsIte && rightLeftIsIte
                        Expression ifLeft = ((IfThenElse<?>) rightLeft).getIf();
                        Expression thenLeft = ((IfThenElse<?>) rightLeft).getThen();
                        Expression elseLeft = ((IfThenElse<?>) rightLeft).getElse();

                        if(!rightRightIsIte) {//leftChildIsIte && rightLeftIsIte && !rightRightIsIte
                            Expression numericPart1 = NumericCompound.create(thenLeft, opRight, rightRight);
                            Expression numericPart2 = NumericCompound.create(elseLeft, opRight, rightRight);

                            Expression comparatorPart1 = NumericBooleanExpression.create(leftThenExpression, comparator, numericPart1);
                            Expression comparatorPart2 = NumericBooleanExpression.create(leftThenExpression, comparator, numericPart2);
                            Expression comparatorPart3 = NumericBooleanExpression.create(leftElseExpression, comparator, numericPart1);
                            Expression comparatorPart4 = NumericBooleanExpression.create(leftElseExpression, comparator, numericPart2);

                            Expression propositionalPart1 = ExpressionUtil.and(leftIfCondition, ifLeft, comparatorPart1);
                            Expression propositionalPart2 = ExpressionUtil.and(leftIfCondition, Negation.create(ifLeft), comparatorPart2);
                            Expression propositionalPart3 = ExpressionUtil.and(Negation.create(leftIfCondition), ifLeft, comparatorPart3);
                            Expression propositionalPart4 = ExpressionUtil.and(Negation.create(leftIfCondition), Negation.create(ifLeft), comparatorPart4);

                            Expression<Boolean> result = ExpressionUtil.or(propositionalPart1, propositionalPart2, propositionalPart3, propositionalPart4);
                            return result;

                        } else {//leftChildIsIte && rightLeftIsIte && rightRightIsIte
                            Expression ifRight = ((IfThenElse<?>) rightRight).getIf();
                            Expression thenRight = ((IfThenElse<?>) rightRight).getThen();
                            Expression elseRight = ((IfThenElse<?>) rightRight).getElse();

                            Expression numericPart1= NumericCompound.create(thenLeft, opRight, thenRight);
                            Expression numericPart2 = NumericCompound.create(thenLeft, opRight, elseRight);
                            Expression numericPart3= NumericCompound.create(elseLeft, opRight, thenRight);
                            Expression numericPart4= NumericCompound.create(elseLeft, opRight, elseRight);

                            Expression comparatorPart1 = NumericBooleanExpression.create(leftThenExpression, comparator, numericPart1);
                            Expression comparatorPart2 = NumericBooleanExpression.create(leftThenExpression, comparator, numericPart2);
                            Expression comparatorPart3 = NumericBooleanExpression.create(leftThenExpression, comparator, numericPart3);
                            Expression comparatorPart4 = NumericBooleanExpression.create(leftThenExpression, comparator, numericPart4);
                            Expression comparatorPart5 = NumericBooleanExpression.create(leftElseExpression, comparator, numericPart1);
                            Expression comparatorPart6 = NumericBooleanExpression.create(leftElseExpression, comparator, numericPart2);
                            Expression comparatorPart7 = NumericBooleanExpression.create(leftElseExpression, comparator, numericPart3);
                            Expression comparatorPart8 = NumericBooleanExpression.create(leftElseExpression, comparator, numericPart4);

                            Expression propositionalPart1 = ExpressionUtil.and(leftIfCondition, ifLeft, ifRight, comparatorPart1);
                            Expression propositionalPart2 = ExpressionUtil.and(leftIfCondition, ifLeft, Negation.create(ifRight), comparatorPart2);
                            Expression propositionalPart3 = ExpressionUtil.and(leftIfCondition, Negation.create(ifLeft), ifRight, comparatorPart3);
                            Expression propositionalPart4 = ExpressionUtil.and(leftIfCondition, Negation.create(ifLeft), Negation.create(ifRight), comparatorPart4);
                            Expression propositionalPart5 = ExpressionUtil.and(Negation.create(leftIfCondition), ifLeft, ifRight, comparatorPart5);
                            Expression propositionalPart6 = ExpressionUtil.and(Negation.create(leftIfCondition), ifLeft, Negation.create(ifRight), comparatorPart6);
                            Expression propositionalPart7 = ExpressionUtil.and(Negation.create(leftIfCondition), Negation.create(ifLeft), ifRight, comparatorPart7);
                            Expression propositionalPart8 = ExpressionUtil.and(Negation.create(leftIfCondition), Negation.create(ifLeft), Negation.create(ifRight), comparatorPart8);

                            Expression<Boolean> result = ExpressionUtil.or(propositionalPart1, propositionalPart2, propositionalPart3,
                                    propositionalPart4, propositionalPart5, propositionalPart6,
                                    propositionalPart7, propositionalPart8);
                            return result;
                        }
                    }
                }
            }
        } else { //!leftChildIsIte
            if(rightChildIsIte){//!leftChildIsIte && rightChildIsIte
                Expression rightIfCondition = ((IfThenElse<?>) rightChild).getIf();
                Expression rightThenExpression = ((IfThenElse<?>) rightChild).getThen();
                Expression rightElseExpression = ((IfThenElse<?>) rightChild).getElse();

                if(!leftChildIsNum){//!leftChildIsNum && rightChildIsIte
                    Expression result = ExpressionUtil.or(
                            ExpressionUtil.and(rightIfCondition, NumericBooleanExpression.create(leftChild, comparator, rightThenExpression)),
                            ExpressionUtil.and(Negation.create(rightIfCondition), NumericBooleanExpression.create(leftChild, comparator, rightElseExpression)));
                    return result;
                } else { //leftChildIsNum && rightChildIsIte
                    Expression leftLeft = ((NumericCompound<?>) leftChild).getLeft();
                    NumericOperator opLeft = ((NumericCompound<?>) leftChild).getOperator();
                    Expression leftRight = ((NumericCompound<?>) leftChild).getRight();

                    boolean leftLeftIsIte = leftLeft instanceof IfThenElse;
                    boolean leftRightIsIte = leftRight instanceof IfThenElse;

                    if (!leftLeftIsIte){
                        if (!leftRightIsIte){//!leftLeftIsIte && !leftRightIsIte && rightChildIsIte
                            Expression<Boolean> result = ExpressionUtil.or(
                                    ExpressionUtil.and(rightIfCondition, NumericBooleanExpression.create(leftChild, comparator, rightThenExpression)),
                                    ExpressionUtil.and(Negation.create(rightIfCondition), NumericBooleanExpression.create(leftChild, comparator, rightElseExpression)));
                            return result;
                        } else { //!leftLeftIsIte && leftRightIsIte && rightChildIsIte
                            Expression ifRight = ((IfThenElse<?>) leftRight).getIf();
                            Expression thenRight = ((IfThenElse<?>) leftRight).getThen();
                            Expression elseRight = ((IfThenElse<?>) leftRight).getElse();

                            Expression numericPart1 = NumericCompound.create(leftLeft, opLeft, thenRight);
                            Expression numericPart2 = NumericCompound.create(leftLeft, opLeft, elseRight);

                            Expression comparatorPart1 = NumericBooleanExpression.create(numericPart1, comparator, rightThenExpression);
                            Expression comparatorPart2 = NumericBooleanExpression.create(numericPart2, comparator, rightThenExpression);
                            Expression comparatorPart3 = NumericBooleanExpression.create(numericPart1, comparator, rightElseExpression);
                            Expression comparatorPart4 = NumericBooleanExpression.create(numericPart2, comparator, rightElseExpression);

                            Expression propositionalPart1 = ExpressionUtil.and(rightIfCondition, ifRight, comparatorPart1);
                            Expression propositionalPart2 = ExpressionUtil.and(rightIfCondition, Negation.create(ifRight), comparatorPart2);
                            Expression propositionalPart3 = ExpressionUtil.and(Negation.create(rightIfCondition), ifRight, comparatorPart3);
                            Expression propositionalPart4 = ExpressionUtil.and(Negation.create(rightIfCondition), Negation.create(ifRight), comparatorPart4);

                            Expression<Boolean> result = ExpressionUtil.or(propositionalPart1, propositionalPart2, propositionalPart3, propositionalPart4);
                            return result;
                        }
                    } else {//leftLeftIsIte
                        Expression ifLeft = ((IfThenElse<?>) leftLeft).getIf();
                        Expression thenLeft = ((IfThenElse<?>) leftLeft).getThen();
                        Expression elseLeft = ((IfThenElse<?>) leftLeft).getElse();

                        if(!leftRightIsIte){//leftLeftIsIte && !leftRightIsIte && rightChildIsIte
                            Expression numericPart1 = NumericCompound.create(thenLeft, opLeft, leftRight);
                            Expression numericPart2 = NumericCompound.create(elseLeft, opLeft, leftRight);

                            Expression comparatorPart1 = NumericBooleanExpression.create(numericPart1, comparator, rightThenExpression);
                            Expression comparatorPart2 = NumericBooleanExpression.create(numericPart2, comparator, rightThenExpression);
                            Expression comparatorPart3 = NumericBooleanExpression.create(numericPart1, comparator, rightElseExpression);
                            Expression comparatorPart4 = NumericBooleanExpression.create(numericPart2, comparator, rightElseExpression);

                            Expression propositionalPart1 = ExpressionUtil.and(rightIfCondition, ifLeft, comparatorPart1);
                            Expression propositionalPart2 = ExpressionUtil.and(rightIfCondition, Negation.create(ifLeft), comparatorPart2);
                            Expression propositionalPart3 = ExpressionUtil.and(Negation.create(rightIfCondition), ifLeft, comparatorPart3);
                            Expression propositionalPart4 = ExpressionUtil.and(Negation.create(rightIfCondition), Negation.create(ifLeft), comparatorPart4);

                            Expression<Boolean> result = ExpressionUtil.or(propositionalPart1, propositionalPart2, propositionalPart3, propositionalPart4);
                            return result;
                        } else {//leftLeftIsIte && leftRightIsIte && rightChildIsIte
                            Expression ifRight = ((IfThenElse<?>) leftRight).getIf();
                            Expression thenRight = ((IfThenElse<?>) leftRight).getThen();
                            Expression elseRight = ((IfThenElse<?>) leftRight).getElse();

                            Expression numericPart1= NumericCompound.create(thenLeft, opLeft, thenRight);
                            Expression numericPart2 = NumericCompound.create(thenLeft, opLeft, elseRight);
                            Expression numericPart3= NumericCompound.create(elseLeft, opLeft, thenRight);
                            Expression numericPart4= NumericCompound.create(elseLeft, opLeft, elseRight);

                            Expression comparatorPart1 = NumericBooleanExpression.create(numericPart1, comparator, rightThenExpression);
                            Expression comparatorPart2 = NumericBooleanExpression.create(numericPart2, comparator, rightThenExpression);
                            Expression comparatorPart3 = NumericBooleanExpression.create(numericPart3, comparator, rightThenExpression);
                            Expression comparatorPart4 = NumericBooleanExpression.create(numericPart4, comparator, rightThenExpression);
                            Expression comparatorPart5 = NumericBooleanExpression.create(numericPart1, comparator, rightElseExpression);
                            Expression comparatorPart6 = NumericBooleanExpression.create(numericPart2, comparator, rightElseExpression);
                            Expression comparatorPart7 = NumericBooleanExpression.create(numericPart3, comparator, rightElseExpression);
                            Expression comparatorPart8 = NumericBooleanExpression.create(numericPart4, comparator, rightElseExpression);

                            Expression propositionalPart1 = ExpressionUtil.and(rightIfCondition, ifLeft, ifRight, comparatorPart1);
                            Expression propositionalPart2 = ExpressionUtil.and(rightIfCondition, ifLeft, Negation.create(ifRight), comparatorPart2);
                            Expression propositionalPart3 = ExpressionUtil.and(rightIfCondition, Negation.create(ifLeft), ifRight, comparatorPart3);
                            Expression propositionalPart4 = ExpressionUtil.and(rightIfCondition, Negation.create(ifLeft), Negation.create(ifRight), comparatorPart4);
                            Expression propositionalPart5 = ExpressionUtil.and(Negation.create(rightIfCondition), ifLeft, ifRight, comparatorPart5);
                            Expression propositionalPart6 = ExpressionUtil.and(Negation.create(rightIfCondition), ifLeft, Negation.create(ifRight), comparatorPart6);
                            Expression propositionalPart7 = ExpressionUtil.and(Negation.create(rightIfCondition), Negation.create(ifLeft), ifRight, comparatorPart7);
                            Expression propositionalPart8 = ExpressionUtil.and(Negation.create(rightIfCondition), Negation.create(ifLeft), Negation.create(ifRight), comparatorPart8);

                            Expression result = ExpressionUtil.or(propositionalPart1, propositionalPart2, propositionalPart3, propositionalPart4,
                                    propositionalPart5, propositionalPart6, propositionalPart7, propositionalPart8);
                            return result;
                        }

                    }
                }
            } else {// !leftChildIsIte && !rightChildIsIte
                if(leftChildIsNum){
                    Expression leftLeft = ((NumericCompound<?>) leftChild).getLeft();
                    NumericOperator opLeft = ((NumericCompound<?>) leftChild).getOperator();
                    Expression leftRight = ((NumericCompound<?>) leftChild).getRight();

                    boolean leftLeftIsIte = leftLeft instanceof IfThenElse;
                    boolean leftRightIsIte = leftRight instanceof IfThenElse;

                    if(!rightChildIsNum){
                        if(!leftLeftIsIte){
                            if(!leftRightIsIte){//!leftLeftIsIte && !leftRightIsIte && !rightChildIsNum
                                return n;
                            } else {//!leftLeftIsIte && leftRightIsIte && !rightChildIsNum
                                Expression ifLeftRight = ((IfThenElse<?>) leftRight).getIf();
                                Expression thenLeftRight = ((IfThenElse<?>) leftRight).getThen();
                                Expression elseLeftRight = ((IfThenElse<?>) leftRight).getElse();

                                Expression numericPart5 = NumericCompound.create(leftLeft, opLeft, thenLeftRight);
                                Expression numericPart6 = NumericCompound.create(leftLeft, opLeft, elseLeftRight);

                                Expression comparatorPart1 = NumericBooleanExpression.create(numericPart5, comparator, rightChild);
                                Expression comparatorPart2 = NumericBooleanExpression.create(numericPart6, comparator, rightChild);

                                Expression propositionalPart1 = ExpressionUtil.and(ifLeftRight, comparatorPart1);
                                Expression propositionalPart2 = ExpressionUtil.and(Negation.create(ifLeftRight), comparatorPart2);

                                Expression result = ExpressionUtil.or(propositionalPart1, propositionalPart2);
                                return result;
                            }
                        } else {//leftLeftIsIte && !rightChildIsNum
                            Expression ifLeftLeft = ((IfThenElse<?>) leftLeft).getIf();
                            Expression thenLeftLeft = ((IfThenElse<?>) leftLeft).getThen();
                            Expression elseLeftLeft = ((IfThenElse<?>) leftLeft).getElse();
                            if(!leftRightIsIte){//leftLeftIsIte && !leftRightIsIte && !rightChildIsNum

                                Expression numericPart5 = NumericCompound.create(thenLeftLeft, opLeft, leftRight);
                                Expression numericPart6 = NumericCompound.create(elseLeftLeft, opLeft, leftRight);

                                Expression comparatorPart1 = NumericBooleanExpression.create(numericPart5, comparator, rightChild);
                                Expression comparatorPart2 = NumericBooleanExpression.create(numericPart6, comparator, rightChild);

                                Expression propositionalPart1 = ExpressionUtil.and(ifLeftLeft, comparatorPart1);
                                Expression propositionalPart2 = ExpressionUtil.and(Negation.create(ifLeftLeft), comparatorPart2);

                                Expression result = ExpressionUtil.or(propositionalPart1, propositionalPart2);
                                return result;
                            } else {//leftLeftIsIte && leftRightIsIte && !rightChildIsNum
                                Expression ifLeftRight = ((IfThenElse<?>) leftRight).getIf();
                                Expression thenLeftRight = ((IfThenElse<?>) leftRight).getThen();
                                Expression elseLeftRight = ((IfThenElse<?>) leftRight).getElse();

                                Expression numericPart1 = NumericCompound.create(thenLeftLeft, opLeft, thenLeftRight);
                                Expression numericPart2 = NumericCompound.create(thenLeftLeft, opLeft, elseLeftRight);
                                Expression numericPart3 = NumericCompound.create(elseLeftLeft, opLeft, thenLeftRight);
                                Expression numericPart4 = NumericCompound.create(elseLeftLeft, opLeft, elseLeftRight);

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
                        }
                    } else {//leftChildIsNum && rightChildIsNum
                        //case: both children are NumericCompounds; they may contain further IfThenElses as children
                        Expression rightLeft = ((NumericCompound<?>) rightChild).getLeft();
                        NumericOperator opRight = ((NumericCompound<?>) rightChild).getOperator();
                        Expression rightRight = ((NumericCompound<?>) rightChild).getRight();

                        boolean rightLeftIsIte = rightLeft instanceof IfThenElse;
                        boolean rightRightIsIte = rightRight instanceof IfThenElse;

                        if(!leftLeftIsIte){
                            if(!leftRightIsIte) {//!leftLeftIsIte && !leftRightIsIte && rightChildIsNum
                                if (!rightLeftIsIte) {
                                    if (!rightRightIsIte) {//!leftLeftIsIte && !leftRightIsIte && !rightLeftIsIte && !rightRightIsIte
                                        return n;
                                    } else {//!leftLeftIsIte && !leftRightIsIte && !rightLeftIsIte && rightRightIsIte
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
                                } else {
                                    Expression ifRightLeft = ((IfThenElse<?>) rightLeft).getIf();
                                    Expression thenRightLeft = ((IfThenElse<?>) rightLeft).getThen();
                                    Expression elseRightLeft = ((IfThenElse<?>) rightLeft).getElse();
                                    if (!rightRightIsIte) {//!leftLeftIsIte && !leftRightIsIte && rightLeftIsIte && !rightRightIsIte
                                        Expression numericPart5 = NumericCompound.create(thenRightLeft, opRight, rightRight);
                                        Expression numericPart6 = NumericCompound.create(elseRightLeft, opRight, rightRight);

                                        Expression comparatorPart1 = NumericBooleanExpression.create(leftChild, comparator, numericPart5);
                                        Expression comparatorPart2 = NumericBooleanExpression.create(leftChild, comparator, numericPart6);

                                        Expression propositionalPart1 = ExpressionUtil.and(ifRightLeft, comparatorPart1);
                                        Expression propositionalPart2 = ExpressionUtil.and(Negation.create(ifRightLeft), comparatorPart2);

                                        Expression result = ExpressionUtil.or(propositionalPart1, propositionalPart2);
                                        return result;
                                    } else {//!leftLeftIsIte && !leftRightIsIte && rightLeftIsIte && rightRightIsIte
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
                            } else {//!leftLeftIsIte && leftRightIsIte && rightChildIsNum
                                Expression ifLeftRight = ((IfThenElse<?>) leftRight).getIf();
                                Expression thenLeftRight = ((IfThenElse<?>) leftRight).getThen();
                                Expression elseLeftRight = ((IfThenElse<?>) leftRight).getElse();

                                Expression numericPart5 = NumericCompound.create(leftLeft, opLeft, thenLeftRight);
                                Expression numericPart6 = NumericCompound.create(leftLeft, opLeft, elseLeftRight);
                                if(!rightLeftIsIte){
                                    if(!rightRightIsIte){//!leftLeftIsIte && leftRightIsIte && !rightLeftIsIte && !rightRightIsIte
                                        Expression comparatorPart1 = NumericBooleanExpression.create(numericPart5, comparator, rightChild);
                                        Expression comparatorPart2 = NumericBooleanExpression.create(numericPart6, comparator, rightChild);

                                        Expression propositionalPart1 = ExpressionUtil.and(ifLeftRight, comparatorPart1);
                                        Expression propositionalPart2 = ExpressionUtil.and(Negation.create(ifLeftRight), comparatorPart2);

                                        Expression result = ExpressionUtil.or(propositionalPart1, propositionalPart2);
                                        return result;
                                    } else {//!leftLeftIsIte && leftRightIsIte && !rightLeftIsIte && rightRightIsIte
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

                                } else {//rightLeftIsIte
                                    Expression ifRightLeft = ((IfThenElse<?>) rightLeft).getIf();
                                    Expression thenRightLeft = ((IfThenElse<?>) rightLeft).getThen();
                                    Expression elseRightLeft = ((IfThenElse<?>) rightLeft).getElse();

                                    if(!rightRightIsIte){//!leftLeftIsIte && leftRightIsIte && rightLeftIsIte && !rightRightIsIte
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
                                    } else {//!leftLeftIsIte && leftRightIsIte && rightLeftIsIte && rightRightIsIte
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
                            }
                        } else {//leftLeftIsIte
                            Expression ifLeftLeft = ((IfThenElse<?>) leftLeft).getIf();
                            Expression thenLeftLeft = ((IfThenElse<?>) leftLeft).getThen();
                            Expression elseLeftLeft = ((IfThenElse<?>) leftLeft).getElse();

                            if(!leftRightIsIte){
                                Expression numericPart5 = NumericCompound.create(thenLeftLeft, opLeft, leftRight);
                                Expression numericPart6 = NumericCompound.create(elseLeftLeft, opLeft, leftRight);

                                if(!rightLeftIsIte){
                                    if(!rightRightIsIte){//leftLeftIsIte && !leftRightIsIte && !rightLeftIsIte && !rightRightIsIte
                                        Expression comparatorPart1 = NumericBooleanExpression.create(numericPart5, comparator, rightChild);
                                        Expression comparatorPart2 = NumericBooleanExpression.create(numericPart6, comparator, rightChild);

                                        Expression propositionalPart1 = ExpressionUtil.and(ifLeftLeft, comparatorPart1);
                                        Expression propositionalPart2 = ExpressionUtil.and(Negation.create(ifLeftLeft), comparatorPart2);

                                        Expression result = ExpressionUtil.or(propositionalPart1, propositionalPart2);
                                        return result;
                                    } else {//leftLeftIsIte && !leftRightIsIte && !rightLeftIsIte && rightRightIsIte
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
                                } else {
                                    Expression ifRightLeft = ((IfThenElse<?>) rightLeft).getIf();
                                    Expression thenRightLeft = ((IfThenElse<?>) rightLeft).getThen();
                                    Expression elseRightLeft = ((IfThenElse<?>) rightLeft).getElse();

                                    if(!rightRightIsIte){//leftLeftIsIte && !leftRightIsIte && rightLeftIsIte && !rightRightIsIte
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
                                    } else {//leftLeftIsIte && !leftRightIsIte && rightLeftIsIte && rightRightIsIte
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
                            } else {//leftLeftIsIte && leftRightIsIte
                                Expression ifLeftRight = ((IfThenElse<?>) leftRight).getIf();
                                Expression thenLeftRight = ((IfThenElse<?>) leftRight).getThen();
                                Expression elseLeftRight = ((IfThenElse<?>) leftRight).getElse();

                                Expression numericPart1 = NumericCompound.create(thenLeftLeft, opLeft, thenLeftRight);
                                Expression numericPart2 = NumericCompound.create(thenLeftLeft, opLeft, elseLeftRight);
                                Expression numericPart3 = NumericCompound.create(elseLeftLeft, opLeft, thenLeftRight);
                                Expression numericPart4 = NumericCompound.create(elseLeftLeft, opLeft, elseLeftRight);
                                if(!rightLeftIsIte){
                                    if(!rightRightIsIte){//leftLeftIsIte && leftRightIsIte && !rightLeftIsIte && !rightRightIsIte
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
                                    } else {//leftLeftIsIte && leftRightIsIte && !rightLeftIsIte && rightRightIsIte
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
                                } else {
                                    Expression ifRightLeft = ((IfThenElse<?>) rightLeft).getIf();
                                    Expression thenRightLeft = ((IfThenElse<?>) rightLeft).getThen();
                                    Expression elseRightLeft = ((IfThenElse<?>) rightLeft).getElse();
                                    if(!rightRightIsIte){//leftLeftIsIte && leftRightIsIte && rightLeftIsIte && !rightRightIsIte
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
                                    } else {//leftLeftIsIte && leftRightIsIte && rightLeftIsIte && rightRightIsIte
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
                            }
                        }
                    }
                } else {//!leftChildIsNum
                    if (rightChildIsNum){//!leftChildIsNum && rightChildIsNum
                        Expression rightLeft = ((NumericCompound<?>) rightChild).getLeft();
                        NumericOperator opRight = ((NumericCompound<?>) rightChild).getOperator();
                        Expression rightRight = ((NumericCompound<?>) rightChild).getRight();

                        boolean rightLeftIsIte = rightLeft instanceof IfThenElse;
                        boolean rightRightIsIte = rightRight instanceof IfThenElse;

                        if (!rightLeftIsIte){
                            if (!rightRightIsIte){//!leftChildIsNum && !rightLeftIsIte && !rightRightIsIte
                                return n;
                            } else {//!leftChildIsNum && !rightLeftIsIte && rightRightIsIte
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
                        } else {//!leftChildIsNum && rightLeftIsIte
                            Expression ifRightLeft = ((IfThenElse<?>) rightLeft).getIf();
                            Expression thenRightLeft = ((IfThenElse<?>) rightLeft).getThen();
                            Expression elseRightLeft = ((IfThenElse<?>) rightLeft).getElse();
                            if(!rightRightIsIte){//!leftChildIsNum && rightLeftIsIte && !rightRightIsIte
                                Expression numericPart5 = NumericCompound.create(thenRightLeft, opRight, rightRight);
                                Expression numericPart6 = NumericCompound.create(elseRightLeft, opRight, rightRight);

                                Expression comparatorPart1 = NumericBooleanExpression.create(leftChild, comparator, numericPart5);
                                Expression comparatorPart2 = NumericBooleanExpression.create(leftChild, comparator, numericPart6);

                                Expression propositionalPart1 = ExpressionUtil.and(ifRightLeft, comparatorPart1);
                                Expression propositionalPart2 = ExpressionUtil.and(Negation.create(ifRightLeft), comparatorPart2);

                                Expression result = ExpressionUtil.or(propositionalPart1, propositionalPart2);
                                return result;

                            } else {//!leftChildIsNum && rightLeftIsIte && rightRightIsIte
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
                    } else {//!leftChildIsNum && !rightChildIsNum
                        return n;
                    }
                }
            }
        }
    }

    /*@Override
    public Expression<?> visit(NumericCompound n, Void data) {
        return n;
    }*/

    @Override
    //Not needed if LetExpressionRemover is used beforehand
    public Expression<?> visit(LetExpression let, Void data) {
        return visit(let.flattenLetExpression(), data);
    }

    public <T> Expression<T> apply(Expression<T> expr, Void data) {
        return visit(expr, data).requireAs(expr.getType());
    }
}
