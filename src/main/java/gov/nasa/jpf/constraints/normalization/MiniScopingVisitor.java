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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

//Creation of an anti prenex form (scope of Quantifiers should be minimized)
//Quantifiers have to be handled ahead of ConjunctionCreator
public class MiniScopingVisitor extends
        DuplicatingVisitor<Void> {

    private static final MiniScopingVisitor INSTANCE = new MiniScopingVisitor();

    public static MiniScopingVisitor getInstance() {
        return INSTANCE;
    }

    int countMiniScopeSteps;
    int operatorTransformations;

    @Override
    public Expression<?> visit(QuantifierExpression q, Void data) {

        Quantifier quantifier = q.getQuantifier();
        List<? extends Variable<?>> bound = q.getBoundVariables();
        Expression body = visit(q.getBody(), data);

        //if quantified body is not a Propositional Compound, mini scoping is done here
        //negations have to be pushed beforehand!
    /*if(body instanceof QuantifierExpression){
        return QuantifierExpression.create(quantifier, bound, body);
    }*/
        if (!(body instanceof PropositionalCompound)) {
            return q;
        }
        //if we are here, body is a Propositional Compound and there is a possibility of a smaller scope
        Expression leftChild = ((PropositionalCompound) body).getLeft();
        Expression rightChild = ((PropositionalCompound) body).getRight();
        LogicalOperator operator = ((PropositionalCompound) body).getOperator();

        //check if bound variables are only in one child of Propositional Compound
        //ArrayList<Variable> freeLeft = new ArrayList<>();
        //leftChild.collectFreeVariables(freeLeft);
        Set<Variable<?>> freeLeft = ExpressionUtil.freeVariables(leftChild);
        boolean boundInFreeLeft = false;

        //ArrayList<Variable> freeRight = new ArrayList<>();
        //rightChild.collectFreeVariables(freeRight);
        Set<Variable<?>> freeRight = ExpressionUtil.freeVariables(rightChild);
        boolean boundInFreeRight = false;

        if (freeLeft != null) {
            for (Variable v : bound) {
                for (Variable f : freeLeft) {
                    if (f.equals(v)) {
                        boundInFreeLeft = true;
                    }
                }
            }
        }

        if (freeRight != null) {
            for (Variable v : bound) {
                for (Variable f : freeRight) {
                    if (f.equals(v)) {
                        boundInFreeRight = true;
                    }
                }
            }
        }

        List<Variable<?>> newBoundLeft = new ArrayList<>();
        List<Variable<?>> newBoundRight = new ArrayList<>();

        if (!boundInFreeLeft && !boundInFreeRight) {
            //no bound variables in children
            //simplification of expression
            return body;
        } else if (!boundInFreeLeft && boundInFreeRight) {
            //no bound variables in left child of the Propositional Compound
            //TODO: added
            newBoundRight.clear();
            for (Variable b : bound) {
                for (Variable f : freeRight) {
                    if (f.equals(b) && !newBoundRight.contains(b)) {
                        newBoundRight.add(b);
                    }
                }
            }
            Expression newLeft = leftChild;
            //visit again because further miniscoping could be possible
            countMiniScopeSteps++;
            Expression newRight = visit(QuantifierExpression.create(quantifier, newBoundRight, rightChild), data);
            return PropositionalCompound.create(newLeft, operator, newRight);

        } else if (boundInFreeLeft && !boundInFreeRight) {
            //no bound variables in right child of the Propositional Compound
            //TODO: added
            newBoundLeft.clear();
            for (Variable b : bound) {
                for (Variable f : freeLeft) {
                    if (f.equals(b) && !newBoundLeft.contains(b)) {
                        newBoundLeft.add(b);
                    }
                }
            }
            //visit again because further miniscoping could be possible
            countMiniScopeSteps++;
            Expression newLeft = visit(QuantifierExpression.create(quantifier, newBoundLeft, leftChild), data);
            Expression newRight = rightChild;
            return PropositionalCompound.create(newLeft, operator, newRight);

        } else if (boundInFreeLeft && boundInFreeRight) {
            //TODO: added
            newBoundLeft.clear();
            for (Variable b : bound) {
                for (Variable f : freeLeft) {
                    if (f.equals(b) && !newBoundLeft.contains(b)) {
                        newBoundLeft.add(b);
                    }
                }
            }
            newBoundRight.clear();
            for (Variable b : bound) {
                for (Variable f : freeRight) {
                    if (f.equals(b) && !newBoundRight.contains(b)) {
                        newBoundRight.add(b);
                    }
                }
            }
            //both children of Propositional Compound contain bound variables
            if (quantifier == Quantifier.FORALL) {
                if (operator == LogicalOperator.AND) {
                    //quantifier can be pushed into the subformulas
                    //TODO: added
                    //visit again because further miniscoping could be possible
                    countMiniScopeSteps++;
                    Expression newLeft = visit(QuantifierExpression.create(quantifier, newBoundLeft, leftChild), data);
                    Expression newRight = visit(QuantifierExpression.create(quantifier, newBoundRight, rightChild), data);
                    return PropositionalCompound.create(newLeft, operator, newRight);
                }
                if (operator == LogicalOperator.OR) {
                    //FORALL is blocked by OR: try to transform body to CNF and visit again
                    Expression result = NormalizationUtil.createCNFNoQuantorHandling(body);
                    if (result instanceof PropositionalCompound) {
                        LogicalOperator newOperator = ((PropositionalCompound) result).getOperator();
                        if (newOperator == LogicalOperator.AND) {
                            operatorTransformations++;
                            return visit(QuantifierExpression.create(quantifier, bound, result));
                        }
                    }
                }
            }
            if (quantifier == Quantifier.EXISTS) {
                //BUT: Nonnengart et al. suggest not to distribute over disjunctions
                //"in order to avoid generating unnecessarily many Skolem functions"
                //ToDo: investigate further and comment this part if necessary
                if (operator == LogicalOperator.OR) {
                    //quantifier can be pushed into the subformulas
                    //TODO: added
                    countMiniScopeSteps++;
                    Expression newLeft = visit(QuantifierExpression.create(quantifier, newBoundLeft, leftChild), data);
                    Expression newRight = visit(QuantifierExpression.create(quantifier, newBoundRight, rightChild), data);
                    return PropositionalCompound.create(newLeft, operator, newRight);
                }
                if (operator == LogicalOperator.AND) {
                    //EXISTS is blocked by AND: try to transform body to DNF and visit again
                    Expression result = NormalizationUtil.createDNFNoQuantorHandling(body);
                    if (result instanceof PropositionalCompound) {
                        LogicalOperator newOperator = ((PropositionalCompound) result).getOperator();
                        if (newOperator == LogicalOperator.OR) {
                            operatorTransformations++;
                            return visit(QuantifierExpression.create(quantifier, bound, result));
                        }
                    }
                }
            }
        }
        //case: no further miniscoping possible
        return q;
    }

    @Override
    public Expression<?> visit(LetExpression expr, Void data) {
        Expression flattened = expr.flattenLetExpression();
        Expression result = visit(flattened, data);
        return result;
    }

    public <T> Expression<T> apply(Expression<T> expr, Void data) {
        return visit(expr, data).requireAs(expr.getType());
    }

    public int countMiniScopeSteps(Expression expr){
        apply(expr, null);
        return countMiniScopeSteps;
    }

    public int countMiniScopeOperatorTransformations(Expression expr){
        apply(expr, null);
        return operatorTransformations;
    }

}
