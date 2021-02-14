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
import org.smtlib.logic.Logic;

import java.util.LinkedList;

public class SimplifyProblemVisitor extends
        DuplicatingVisitor<LinkedList<Expression<?>>> {

    private static final SimplifyProblemVisitor INSTANCE = new SimplifyProblemVisitor();

    public static SimplifyProblemVisitor getInstance(){
        return INSTANCE;
    }

    LogicalOperator lastOperator;

    @Override
    public Expression<?> visit(PropositionalCompound n, LinkedList data) {
        LogicalOperator operator = n.getOperator();
        //Expression leftChild = visit(n.getLeft(), data);
        /*if(leftChild instanceof PropositionalCompound && n.getRight() instanceof PropositionalCompound){
            if(!(((PropositionalCompound) leftChild).getOperator().equals(operator) && ((PropositionalCompound) n.getRight()).getOperator().equals(operator))){
                data.clear();
            }
        } else if(leftChild instanceof PropositionalCompound){
            if(!((PropositionalCompound) leftChild).getOperator().equals(operator)){
                data.clear();
            }
        } else if (n.getRight() instanceof  PropositionalCompound){
            if(!((PropositionalCompound) n.getRight()).getOperator().equals(operator)){
                data.clear();
            }
        }*/
        //data.clear();
        //Expression rightChild = visit(n.getRight(), data);

        Expression leftChild = n.getLeft();
        Expression rightChild = n.getRight();

        boolean leftChildIsProp = leftChild instanceof PropositionalCompound;
        boolean rightChildIsProp = rightChild instanceof PropositionalCompound;

        boolean removeLeft = false;
        boolean removeRight = false;

        if(operator.equals(LogicalOperator.EQUIV)){
            if(leftChild instanceof Constant){
                if(((Constant<?>) leftChild).getValue().equals(Boolean.TRUE)) {
                    return rightChild;
                }
            }
            if(rightChild instanceof Constant){
                if(((Constant<?>) rightChild).getValue().equals(Boolean.TRUE)) {
                    return leftChild;
                }
            }
            if(leftChild instanceof Constant){
                if(((Constant<?>) leftChild).getValue().equals(Boolean.FALSE)) {
                    return Negation.create(rightChild);
                }
            }
            if(rightChild instanceof Constant){
                if(((Constant<?>) rightChild).getValue().equals(Boolean.FALSE)) {
                    return Negation.create(leftChild);
                }
            }
        }
        if(operator.equals(LogicalOperator.AND)){
            if(leftChild.equals(rightChild)){
                //or rightChild
                return visit(leftChild, data);
            }
            /*if (data.contains(leftChild) && data.contains(rightChild)) {
                data.add(n);
            } else if(data.contains(leftChild)){
                return rightChild;
            } else if(data.contains(rightChild)){
                return leftChild;
            }*/
            if(leftChildIsProp && rightChildIsProp) {
                LogicalOperator leftOp = ((PropositionalCompound) leftChild).getOperator();
                LogicalOperator rightOp = ((PropositionalCompound) rightChild).getOperator();
                if(operator.equals(leftOp) && operator.equals(rightOp)){
                    return PropositionalCompound.create((Expression<Boolean>) visit(leftChild, data), operator, visit(rightChild, data));
                } else {
                    Expression newLeft = visit(leftChild, data);
                    data.clear();
                    Expression newRight = visit(rightChild, data);
                    return PropositionalCompound.create(newLeft, operator, newRight);
                }
            }
            if(leftChildIsProp && !rightChildIsProp) {
                LogicalOperator leftOp = ((PropositionalCompound) leftChild).getOperator();
                if(operator.equals(leftOp)){
                    Expression newLeft = visit(leftChild, data);
                    for (Object e : data) {
                        if (e.equals(rightChild)) {
                            removeRight = true;
                        }
                    }
                    if(removeRight){
                        return newLeft;
                    } else {
                        data.add(rightChild);
                        return PropositionalCompound.create(newLeft, operator, rightChild);
                    }
                } else {
                    Expression newLeft = visit(leftChild, data);
                    data.clear();
                    return PropositionalCompound.create(newLeft, operator, rightChild);
                }
            }
            if(!leftChildIsProp && rightChildIsProp){
                LogicalOperator rightOp = ((PropositionalCompound) rightChild).getOperator();
                if(operator.equals(rightOp)){
                    Expression newRight = visit(rightChild, data);
                    for (Object e : data) {
                        if (e.equals(leftChild)) {
                            removeLeft = true;
                        }
                    }
                    if(removeLeft){
                        return newRight;
                    } else {
                        data.add(leftChild);
                        return PropositionalCompound.create(leftChild, operator, newRight);
                    }
                } else {
                    Expression newRight = visit(rightChild, data);
                    data.clear();
                    return PropositionalCompound.create(leftChild, operator, newRight);
                }
            }
            if(!leftChildIsProp && !rightChildIsProp){
                if(leftChild.equals(rightChild)){
                    //or rightChild
                    data.add(leftChild);
                    return leftChild;
                } else {
                    for (Object e : data) {
                        if (e.equals(leftChild)) {
                            removeLeft = true;
                        }
                        if (e.equals(rightChild)) {
                            removeRight = true;
                        }
                    }
                    if(removeLeft && removeRight){
                        //TODO: how to return nothing? here, actually we need to return on the level above

                    } else if(removeLeft){
                        data.add(rightChild);
                        return rightChild;
                    } else if(removeRight){
                        data.add(leftChild);
                        return leftChild;
                    } else {
                        data.add(leftChild);
                        data.add(rightChild);
                        return n;
                    }
                }
            }

            if(leftChild instanceof Negation){
                if(rightChild.equals(((Negation) leftChild).getNegated())){
                    return Constant.create(BuiltinTypes.BOOL, Boolean.FALSE);
                }
            }
            if(rightChild instanceof Negation){
                if(leftChild.equals(((Negation) rightChild).getNegated())){
                    return Constant.create(BuiltinTypes.BOOL, Boolean.FALSE);
                }
            }
            if(leftChild instanceof Constant){
                if(((Constant<?>) leftChild).getValue().equals(Boolean.FALSE)) {
                    return Constant.create(BuiltinTypes.BOOL, Boolean.FALSE);
                }
            }
            if(rightChild instanceof Constant){
                if(((Constant<?>) rightChild).getValue().equals(Boolean.FALSE)){
                    return Constant.create(BuiltinTypes.BOOL, Boolean.FALSE);
                }
            }
            if(leftChild instanceof Constant){
                if(((Constant<?>) leftChild).getValue().equals(Boolean.TRUE)) {
                    return rightChild;
                }
            }
            if(rightChild instanceof Constant){
                if(((Constant<?>) rightChild).getValue().equals(Boolean.TRUE)){
                    return leftChild;
                }
            }
        }
        if(operator.equals(LogicalOperator.OR)){
            if(leftChild.equals(rightChild)){
                //or rightChild
                return visit(leftChild, data);
            }
            if(leftChildIsProp && rightChildIsProp) {
                LogicalOperator leftOp = ((PropositionalCompound) leftChild).getOperator();
                LogicalOperator rightOp = ((PropositionalCompound) rightChild).getOperator();
                if(operator.equals(leftOp) && operator.equals(rightOp)){
                    return PropositionalCompound.create((Expression<Boolean>) visit(leftChild, data), operator, visit(rightChild, data));
                } else {
                    Expression newLeft = visit(leftChild, data);
                    data.clear();
                    Expression newRight = visit(rightChild, data);
                    return PropositionalCompound.create(newLeft, operator, newRight);
                }
            }
            if(leftChildIsProp && !rightChildIsProp) {
                LogicalOperator leftOp = ((PropositionalCompound) leftChild).getOperator();
                if(operator.equals(leftOp)){
                    Expression newLeft = visit(leftChild, data);
                    for (Object e : data) {
                        if (e.equals(rightChild)) {
                            removeRight = true;
                        }
                    }
                    if(removeRight){
                        return newLeft;
                    } else {
                        data.add(rightChild);
                        return PropositionalCompound.create(newLeft, operator, rightChild);
                    }
                } else {
                    Expression newLeft = visit(leftChild, data);
                    data.clear();
                    return PropositionalCompound.create(newLeft, operator, rightChild);
                }
            }
            if(!leftChildIsProp && rightChildIsProp){
                LogicalOperator rightOp = ((PropositionalCompound) rightChild).getOperator();
                if(operator.equals(rightOp)){
                    Expression newRight = visit(rightChild, data);
                    for (Object e : data) {
                        if (e.equals(leftChild)) {
                            removeLeft = true;
                        }
                    }
                    if(removeLeft){
                        return newRight;
                    } else {
                        data.add(leftChild);
                        return PropositionalCompound.create(leftChild, operator, newRight);
                    }
                } else {
                    Expression newRight = visit(rightChild, data);
                    data.clear();
                    return PropositionalCompound.create(leftChild, operator, newRight);
                }
            }
            if(!leftChildIsProp && !rightChildIsProp){
                if(leftChild.equals(rightChild)){
                    //or rightChild
                    data.add(leftChild);
                    return leftChild;
                } else {
                    for (Object e : data) {
                        if (e.equals(leftChild)) {
                            removeLeft = true;
                        }
                        if (e.equals(rightChild)) {
                            removeRight = true;
                        }
                    }
                    if(removeLeft && removeRight){
                        //TODO: how to return nothing? here, actually we need to return on the level above

                    } else if(removeLeft){
                        data.add(rightChild);
                        return rightChild;
                    } else if(removeRight){
                        data.add(leftChild);
                        return leftChild;
                    } else {
                        data.add(leftChild);
                        data.add(rightChild);
                        return n;
                    }
                }
            }

            if(leftChild instanceof Negation) {
                if(rightChild.equals(((Negation) leftChild).getNegated())) {
                    return Constant.create(BuiltinTypes.BOOL, Boolean.TRUE);
                }
            }
            if(rightChild instanceof Negation){
                if(leftChild.equals(((Negation) rightChild).getNegated())) {
                    return Constant.create(BuiltinTypes.BOOL, Boolean.TRUE);
                }
            }
            if(leftChild instanceof Constant) {
                if (((Constant<?>) leftChild).getValue().equals(Boolean.TRUE)) {
                    return Constant.create(BuiltinTypes.BOOL, Boolean.TRUE);
                }
            }
            if(rightChild instanceof Constant) {
                if(((Constant<?>) rightChild).getValue().equals(Boolean.TRUE)) {
                    return Constant.create(BuiltinTypes.BOOL, Boolean.TRUE);
                }
            }
            if(leftChild instanceof Constant) {
                if(((Constant<?>) leftChild).getValue().equals(Boolean.FALSE)) {
                    return rightChild;
                }
            }
            if(rightChild instanceof Constant) {
                if(((Constant<?>) rightChild).getValue().equals(Boolean.FALSE)) {
                    return leftChild;
                }
            }
        }
        if(operator.equals(LogicalOperator.IMPLY)){
            if(rightChild instanceof Constant) {
                if(((Constant<?>) rightChild).getValue().equals(Boolean.TRUE)) {
                    return Constant.create(BuiltinTypes.BOOL, Boolean.TRUE);
                }
            }
            if(rightChild instanceof Constant) {
                if(((Constant<?>) rightChild).getValue().equals(Boolean.FALSE)) {
                    return Negation.create(leftChild);
                }
            }
            if(leftChild.equals(rightChild)){
                return Constant.create(BuiltinTypes.BOOL, Boolean.TRUE);
            }
            if(leftChild instanceof Constant){
                if(((Constant<?>) leftChild).getValue().equals(Boolean.TRUE)) {
                    return rightChild;
                }
            }
            if(leftChild instanceof Constant) {
                if(((Constant<?>) leftChild).getValue().equals(Boolean.FALSE)) {
                    return Constant.create(BuiltinTypes.BOOL, Boolean.TRUE);
                }
            }
        }
        return n;
    }

    @Override
    public Expression<?> visit(Negation n, LinkedList data) {
        Expression negated = n.getNegated();

        if(negated.equals(Boolean.TRUE)){
            return Constant.create(BuiltinTypes.BOOL, Boolean.FALSE);
        }
        if(negated.equals(Boolean.FALSE)){
            return Constant.create(BuiltinTypes.BOOL, Boolean.TRUE);
        }

        return n;
    }

    @Override
    //Not needed if LetExpressionRemover is used beforehand
    public Expression<?> visit(LetExpression let, LinkedList data) {
        Expression result = let.flattenLetExpression();
        return visit(result, data);
    }

    public <T> Expression<T> apply(Expression<T> expr, LinkedList data) {
        return visit(expr, data).requireAs(expr.getType());
    }
}
