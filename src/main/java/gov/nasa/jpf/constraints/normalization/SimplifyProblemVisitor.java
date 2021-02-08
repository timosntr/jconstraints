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

import java.util.LinkedList;

public class SimplifyProblemVisitor extends
        DuplicatingVisitor<LinkedList<Expression<?>>> {

    private static final SimplifyProblemVisitor INSTANCE = new SimplifyProblemVisitor();

    public static SimplifyProblemVisitor getInstance(){
        return INSTANCE;
    }

    LogicalOperator lastOperator;
    boolean firstVisit = true;

    @Override
    public Expression<?> visit(PropositionalCompound n, LinkedList data) {
        //Expression leftChild = visit(n.getLeft(), data);
        //Expression rightChild = visit(n.getRight(), data);
        Expression leftChild = visit(n.getLeft(), data);
        Expression rightChild = visit(n.getRight(), data);
        LogicalOperator operator = n.getOperator();
        if(firstVisit){
            lastOperator = operator;
            firstVisit = false;
        }

        //TODO: remove duplicates from clauses
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
                return leftChild;
            }
            /*if(operator.equals(lastOperator)){
                if(!data.isEmpty()) {
                    for (Object e : data) {
                        if (e.equals(n.getLeft())) {
                            data.add(n.getRight());
                            lastOperator = operator;
                            if(!(n.getLeft() instanceof PropositionalCompound)){
                                return rightChild;
                            }

                        }
                        if (e.equals(n.getRight())) {
                            data.add(n.getLeft());
                            lastOperator = operator;
                            if(!(n.getRight() instanceof PropositionalCompound)){
                                return leftChild;
                            }
                        }
                    }
                    data.add(n.getLeft());
                    data.add(n.getRight());
                } else {
                    data.add(n.getLeft());
                    data.add(n.getRight());
                }
            } else {
                data.clear();
                data.add(n.getLeft());
                data.add(n.getRight());
                lastOperator = operator;
            }*/

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
                return leftChild;
            }
            /*if(operator.equals(lastOperator)){
                if(!data.isEmpty()) {
                    for (Object e : data) {
                        if (e.equals(n.getLeft())) {
                            data.add(n.getRight());
                            lastOperator = operator;
                            if(!(n.getLeft() instanceof PropositionalCompound)){
                                return rightChild;
                            }

                        }
                        if (e.equals(n.getRight())) {
                            data.add(n.getLeft());
                            lastOperator = operator;
                            if(!(n.getRight() instanceof PropositionalCompound)){
                                return leftChild;
                            }
                        }
                    }
                    data.add(n.getLeft());
                    data.add(n.getRight());
                } else {
                    data.add(n.getLeft());
                    data.add(n.getRight());
                }
            } else {
                data.clear();
                data.add(n.getLeft());
                data.add(n.getRight());
                lastOperator = operator;
            }*/

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
        return PropositionalCompound.create(leftChild, operator, rightChild);
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
