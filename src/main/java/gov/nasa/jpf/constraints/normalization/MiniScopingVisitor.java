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
import gov.nasa.jpf.constraints.expressions.functions.FunctionExpression;
import gov.nasa.jpf.constraints.util.DuplicatingVisitor;

import java.util.ArrayList;
import java.util.List;

//Creation of an anti prenex form (scope of Quantifiers should be minimized)
//Quantifiers have to be handled ahead of ConjunctionCreator
public class MiniScopingVisitor extends
        DuplicatingVisitor<Void> {

    private static final MiniScopingVisitor INSTANCE = new MiniScopingVisitor();

    public static MiniScopingVisitor getInstance(){
        return INSTANCE;
    }

    @Override
    public Expression<?> visit(QuantifierExpression q, Void data) {

        Quantifier quantifier = q.getQuantifier();
        List<? extends Variable<?>> bound = q.getBoundVariables();
        //todo: do not miniscope inner quantifier first?
        Expression body = visit(q.getBody(), data);
        //if quantified body is not a Propositional Compound, mini scoping is done here
        //negations have to be pushed beforehand!
        if(body instanceof QuantifierExpression){
            Expression innerQuantifier = visit(body, data);
            return visit(QuantifierExpression.create(quantifier, bound, innerQuantifier));
        }
        //TODO: actually they should all be flattened
        if(body instanceof LetExpression){
            return visit(((LetExpression) body).flattenLetExpression(), data);
        }
        if(!(body instanceof PropositionalCompound)){
            return q;
        }
        //if we are here, body is a Propositional Compound and there is a possibility of a smaller scope
        Expression leftChild = ((PropositionalCompound) body).getLeft();
        Expression rightChild = ((PropositionalCompound) body).getRight();
        LogicalOperator operator = ((PropositionalCompound) body).getOperator();

        //check if bound variables are only in one child of Propositional Compound
        ArrayList<Variable> freeLeft = new ArrayList<>();
        leftChild.collectFreeVariables(freeLeft);
        boolean boundInFreeLeft = false;

        ArrayList<Variable> freeRight = new ArrayList<>();
        rightChild.collectFreeVariables(freeRight);
        boolean boundInFreeRight = false;

        if(freeLeft != null){
            for(Variable v : bound){
                for(Variable f : freeLeft){
                    if(f.equals(v)){
                        boundInFreeLeft = true;
                    }
                }
            }
        }

        if(freeRight != null){
            for(Variable v : bound){
                for(Variable f : freeRight){
                    if(f.equals(v)){
                        boundInFreeRight = true;
                    }
                }
            }
        }

        if(!boundInFreeLeft && boundInFreeRight){
            if(leftChild instanceof QuantifierExpression){
                if(!((QuantifierExpression) leftChild).getQuantifier().equals(quantifier)){
                    Expression result = QuantifierExpression.create(quantifier, bound, (Expression<Boolean>) visit(body, data));
                    return result;
                }
            }
            if(rightChild instanceof QuantifierExpression){
                if(!((QuantifierExpression) rightChild).getQuantifier().equals(quantifier)){
                    Expression result = QuantifierExpression.create(quantifier, bound, (Expression<Boolean>) visit(body, data));
                    return result;
                }
            }
            //no bound variables in left child of the Propositional Compound
            Expression newLeft = visit(leftChild, data);
            Expression newRight = visit(QuantifierExpression.create(quantifier, bound, rightChild), data);
            return PropositionalCompound.create(newLeft, operator, newRight);

        } else if(boundInFreeLeft && !boundInFreeRight){
            if(leftChild instanceof QuantifierExpression){
                if(!((QuantifierExpression) leftChild).getQuantifier().equals(quantifier)){
                    Expression result = QuantifierExpression.create(quantifier, bound, (Expression<Boolean>) visit(body, data));
                    return result;
                }
            }
            if(rightChild instanceof QuantifierExpression){
                if(!((QuantifierExpression) rightChild).getQuantifier().equals(quantifier)){
                    //TODO: visit whole expression again in case further miniscoping is possible?
                    Expression result = QuantifierExpression.create(quantifier, bound, (Expression<Boolean>) visit(body, data));
                    return result;
                }
            }

            //no bound variables in right child of the Propositional Compound
            Expression newLeft = visit(QuantifierExpression.create(quantifier, bound, leftChild), data);
            Expression newRight = visit(rightChild, data);
            return PropositionalCompound.create(newLeft, operator, newRight);

        } else if(boundInFreeLeft && boundInFreeRight){
            if(leftChild instanceof QuantifierExpression){
                if(!((QuantifierExpression) leftChild).getQuantifier().equals(quantifier)){
                    Expression result = QuantifierExpression.create(quantifier, bound, (Expression<Boolean>) visit(body, data));
                    return result;
                }
            }
            if(rightChild instanceof QuantifierExpression){
                if(!((QuantifierExpression) rightChild).getQuantifier().equals(quantifier)){
                    Expression result = QuantifierExpression.create(quantifier, bound, (Expression<Boolean>) visit(body, data));
                    return result;
                }
            }
            //both children of Propositional Compound contain bound variables
            if(quantifier == Quantifier.FORALL){
                if(operator == LogicalOperator.AND){
                    //quantifier can be pushed into the subformulas
                    Expression newLeft = visit(QuantifierExpression.create(quantifier, (List<? extends Variable<?>>) bound, leftChild), data);
                    Expression newRight = visit(QuantifierExpression.create(quantifier, (List<? extends Variable<?>>) bound, rightChild), data);
                    return PropositionalCompound.create(newLeft, operator, newRight);
                }
                if(operator == LogicalOperator.OR){
                    //FORALL is blocked by OR: try to transform body to CNF and visit again
                    Expression result = NormalizationUtil.createCNFNoQuantorHandling(body);
                    if(result instanceof PropositionalCompound){
                        LogicalOperator newOperator = ((PropositionalCompound) result).getOperator();
                        if(newOperator == LogicalOperator.AND){
                            return visit(QuantifierExpression.create(quantifier, (List<? extends Variable<?>>) bound, result));
                        }
                    }
                }
            }
            if(quantifier == Quantifier.EXISTS){
                //BUT: Nonnengart et al. suggest not to distribute over disjunctions
                //"in order to avoid generating unnecessarily many Skolem functions"
                //ToDo: investigate further and comment this part if necessary
                if(operator == LogicalOperator.OR){
                    //quantifier can be pushed into the subformulas
                    Expression newLeft = visit(QuantifierExpression.create(quantifier, (List<? extends Variable<?>>) bound, leftChild), data);
                    Expression newRight = visit(QuantifierExpression.create(quantifier, (List<? extends Variable<?>>) bound, rightChild), data);
                    return PropositionalCompound.create(newLeft, operator, newRight);
                }
                if(operator == LogicalOperator.AND){
                    //EXISTS is blocked by AND: try to transform body to DNF and visit again
                    Expression result = NormalizationUtil.createDNFNoQuantorHandling(body);
                    if(result instanceof PropositionalCompound){
                        LogicalOperator newOperator = ((PropositionalCompound) result).getOperator();
                        if(newOperator == LogicalOperator.OR){
                            return visit(QuantifierExpression.create(quantifier, (List<? extends Variable<?>>) bound, result));
                        }
                    }
                }
            }
        }
        //no bound variables in children
        return q;

        //old version
        /*if(!boundInFreeLeft && boundInFreeRight){
            //no bound variables in left child of the Propositional Compound
            Expression newLeft = visit(leftChild, data);
            Expression newRight = visit(QuantifierExpression.create(quantifier, (List<? extends Variable<?>>) bound, rightChild), data);
            return PropositionalCompound.create(newLeft, operator, newRight);

        } else if(boundInFreeLeft && !boundInFreeRight){
            //no bound variables in right child of the Propositional Compound
            Expression newLeft = visit(QuantifierExpression.create(quantifier, (List<? extends Variable<?>>) bound, leftChild), data);
            Expression newRight = visit(rightChild, data);
            return PropositionalCompound.create(newLeft, operator, newRight);

        } else if(boundInFreeLeft && boundInFreeRight){
            //both children of Propositional Compound contain bound variables
            if(quantifier == Quantifier.FORALL){
                if(operator == LogicalOperator.AND){
                    //quantifier can be pushed into the subformulas
                    Expression newLeft = visit(QuantifierExpression.create(quantifier, (List<? extends Variable<?>>) bound, leftChild), data);
                    Expression newRight = visit(QuantifierExpression.create(quantifier, (List<? extends Variable<?>>) bound, rightChild), data);
                    return PropositionalCompound.create(newLeft, operator, newRight);
                }
                if(operator == LogicalOperator.OR){
                    //FORALL is blocked by OR: try to transform body to CNF and visit again
                    Expression result = NormalizationUtil.createCNFNoQuantorHandling(body);
                    if(result instanceof PropositionalCompound){
                        LogicalOperator newOperator = ((PropositionalCompound) result).getOperator();
                        if(newOperator == LogicalOperator.AND){
                            return visit(QuantifierExpression.create(quantifier, (List<? extends Variable<?>>) bound, result));
                        }
                    }
                }
            }
            if(quantifier == Quantifier.EXISTS){
                //BUT: Nonnengart et al. suggest not to distribute over disjunctions
                //"in order to avoid generating unnecessarily many Skolem functions"
                //ToDo: investigate further and comment this part if necessary
                if(operator == LogicalOperator.OR){
                    //quantifier can be pushed into the subformulas
                    Expression newLeft = visit(QuantifierExpression.create(quantifier, (List<? extends Variable<?>>) bound, leftChild), data);
                    Expression newRight = visit(QuantifierExpression.create(quantifier, (List<? extends Variable<?>>) bound, rightChild), data);
                    return PropositionalCompound.create(newLeft, operator, newRight);
                }
                if(operator == LogicalOperator.AND){
                    //EXISTS is blocked by AND: try to transform body to DNF and visit again
                    Expression result = NormalizationUtil.createDNFNoQuantorHandling(body);
                    if(result instanceof PropositionalCompound){
                        LogicalOperator newOperator = ((PropositionalCompound) result).getOperator();
                        if(newOperator == LogicalOperator.OR){
                            return visit(QuantifierExpression.create(quantifier, (List<? extends Variable<?>>) bound, result));
                        }
                    }
                }
            }
        }
        //no bound variables in children
        return q;*/
    }

    @Override
    public Expression<?> visit(LetExpression expr, Void data) {
        Expression flattened = expr.flattenLetExpression();
        Expression result = visit(flattened, data);
        return result;
    }

    @Override
    public <E> Expression<?> visit(IfThenElse<E> n, Void data) {
        return super.visit(n, data);
    }

    public <T> Expression<T> apply(Expression<T> expr, Void data) {
        return visit(expr, data).requireAs(expr.getType());
    }

    /*@Override
    public Expression<?> visit(IfThenElse expr, Void data) {
        Expression ifCond = expr.getIf();
        Expression thenExpr = visit(expr.getThen(), data);
        Expression elseExpr = visit(expr.getElse(), data);

        Expression firstPart = PropositionalCompound.create(Negation.create(ifCond), LogicalOperator.OR, thenExpr);
        Expression secondPart = PropositionalCompound.create(ifCond, LogicalOperator.OR, elseExpr);

        Expression result = PropositionalCompound.create(firstPart, LogicalOperator.AND, secondPart);

        return result;
    }*/
}