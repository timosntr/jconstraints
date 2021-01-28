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

public class SimplifyProblemVisitor extends
        DuplicatingVisitor<Void> {

    private static final SimplifyProblemVisitor INSTANCE = new SimplifyProblemVisitor();

    public static SimplifyProblemVisitor getInstance(){
        return INSTANCE;
    }

    @Override
    public Expression<?> visit(PropositionalCompound n, Void data) {
        Expression leftChild = visit(n.getLeft(), data);
        Expression rightChild = visit(n.getRight(), data);
        LogicalOperator operator = n.getOperator();

        //TODO: remove duplicates from clauses
        if(operator.equals(LogicalOperator.EQUIV)){
            if(leftChild instanceof Constant && ((Constant<?>) leftChild).getValue().equals(Boolean.TRUE)){
                return rightChild;
            }
            if(rightChild instanceof Constant && ((Constant<?>) leftChild).getValue().equals(Boolean.TRUE)){
                return leftChild;
            }
            if(leftChild instanceof Constant && ((Constant<?>) leftChild).getValue().equals(Boolean.FALSE)){
                return Negation.create(rightChild);
            }
            if(rightChild instanceof Constant && ((Constant<?>) leftChild).getValue().equals(Boolean.FALSE)){
                return Negation.create(leftChild);
            }
        }
        if(operator.equals(LogicalOperator.AND)){
            if(leftChild.equals(rightChild)){
                //or rightChild
                return leftChild;
            }
            if(leftChild instanceof Negation && rightChild.equals(((Negation) leftChild).getNegated())){
                return Constant.create(BuiltinTypes.BOOL, Boolean.FALSE);
            }
            if(rightChild instanceof Negation && leftChild.equals(((Negation) leftChild).getNegated())){
                return Constant.create(BuiltinTypes.BOOL, Boolean.FALSE);
            }
            if((leftChild instanceof Constant && ((Constant<?>) leftChild).getValue().equals(Boolean.FALSE))
            || (rightChild instanceof Constant && ((Constant<?>) rightChild).getValue().equals(Boolean.FALSE))){
                return Constant.create(BuiltinTypes.BOOL, Boolean.FALSE);
            }
            if(leftChild instanceof Constant && ((Constant<?>) leftChild).getValue().equals(Boolean.TRUE)){
                return rightChild;
            }
            if(rightChild instanceof Constant && ((Constant<?>) rightChild).getValue().equals(Boolean.TRUE)){
                return leftChild;
            }
        }
        if(operator.equals(LogicalOperator.OR)){
            if(leftChild.equals(rightChild)){
                //or rightChild
                return leftChild;
            }
            if(leftChild instanceof Negation && rightChild.equals(((Negation) leftChild).getNegated())){
                return Constant.create(BuiltinTypes.BOOL, Boolean.TRUE);
            }
            if(rightChild instanceof Negation && leftChild.equals(((Negation) leftChild).getNegated())){
                return Constant.create(BuiltinTypes.BOOL, Boolean.TRUE);
            }
            if((leftChild instanceof Constant && ((Constant<?>) leftChild).getValue().equals(Boolean.TRUE))
                    || (rightChild instanceof Constant && ((Constant<?>) rightChild).getValue().equals(Boolean.TRUE))){
                return Constant.create(BuiltinTypes.BOOL, Boolean.TRUE);
            }
            if(leftChild instanceof Constant && ((Constant<?>) leftChild).getValue().equals(Boolean.FALSE)){
                return rightChild;
            }
            if(rightChild instanceof Constant && ((Constant<?>) rightChild).getValue().equals(Boolean.FALSE)){
                return leftChild;
            }
        }
        if(operator.equals(LogicalOperator.IMPLY)){
            if(rightChild instanceof Constant && ((Constant<?>) leftChild).getValue().equals(Boolean.TRUE)){
                return Constant.create(BuiltinTypes.BOOL, Boolean.TRUE);
            }
            if(rightChild instanceof Constant && ((Constant<?>) leftChild).getValue().equals(Boolean.FALSE)){
                return Negation.create(leftChild);
            }
            if(leftChild.equals(rightChild)){
                return Constant.create(BuiltinTypes.BOOL, Boolean.TRUE);
            }
            if(leftChild instanceof Constant && ((Constant<?>) leftChild).getValue().equals(Boolean.TRUE)){
                return rightChild;
            }
            if(leftChild instanceof Constant && ((Constant<?>) leftChild).getValue().equals(Boolean.FALSE)){
                return Constant.create(BuiltinTypes.BOOL, Boolean.TRUE);
            }
        }
        return super.visit(n, data);
    }

    @Override
    public Expression<?> visit(Negation n, Void data) {
        Expression negated = n.getNegated();

        if(negated.equals(Boolean.TRUE)){
            return Constant.create(BuiltinTypes.BOOL, Boolean.FALSE);
        }
        if(negated.equals(Boolean.FALSE)){
            return Constant.create(BuiltinTypes.BOOL, Boolean.TRUE);
        }

        return super.visit(n, data);
    }

    @Override
    //Not needed if LetExpressionRemover is used beforehand
    public Expression<?> visit(LetExpression let, Void data) {
        Expression result = let.flattenLetExpression();
        return super.visit(result, data);
    }

    public <T> Expression<T> apply(Expression<T> expr, Void data) {
        return visit(expr, data).requireAs(expr.getType());
    }
}
