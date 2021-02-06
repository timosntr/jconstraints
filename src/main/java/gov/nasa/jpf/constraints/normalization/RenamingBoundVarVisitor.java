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

import java.util.*;

public class RenamingBoundVarVisitor extends
        DuplicatingVisitor<HashMap<String, String>> {
  
  private static final RenamingBoundVarVisitor INSTANCE = new RenamingBoundVarVisitor();
  
  public static RenamingBoundVarVisitor getInstance() {
    return INSTANCE;
  }

  private int[] id = {0};
  Set<Variable<?>> freeVars = new HashSet<>();

  @Override
  public <E> Expression<?> visit(Variable<E> v, HashMap<String, String> data) {

    if(data.containsKey(v.getName())){
      String newName = data.get(v.getName());
      return Variable.create(v.getType(), newName);
    } else {
      return v;
    }
  }

  @Override
  public Expression<?> visit(QuantifierExpression q, HashMap<String, String> data){
    List<Variable<?>> boundVariables = (List<Variable<?>>) q.getBoundVariables();
    id[0]++;
    LinkedList<Variable<?>> renamedBoundVariables = new LinkedList<>();

    if(boundVariables != null) {
      for (Variable v : boundVariables) {
        String oldName = v.getName();
        String newName = "Q." + id[0] + "." + oldName;
        while (NormalizationUtil.nameClashWithExistingFreeVars(newName, freeVars)) {
          id[0]++;
          newName = "Q." + id[0] + "." + oldName;
        }
        if(data.containsKey(v.getName())){
          data.replace(v.getName(), newName);
          renamedBoundVariables.add(Variable.create(v.getType(), newName));
        } else {
          data.put(v.getName(), newName);
          renamedBoundVariables.add(Variable.create(v.getType(), newName));
        }
      }
    }

    assert boundVariables.size() == renamedBoundVariables.size();
    //rename variables in expression
    Expression renamedExpr = visit(q.getBody(), data);
    return QuantifierExpression.create(q.getQuantifier(), renamedBoundVariables, renamedExpr);
  }

  @Override
  public Expression<?> visit(PropositionalCompound n, HashMap<String, String> data) {
    //renamings only relevant in the left path should not be used in the right path
    HashMap<String, String> leftMap = (HashMap<String, String>) data.clone();
    HashMap<String, String> rightMap = (HashMap<String, String>) data.clone();

    Expression leftChild = visit(n.getLeft(), leftMap);

    Expression rightChild = visit(n.getRight(), rightMap);
    return PropositionalCompound.create(leftChild, n.getOperator(), rightChild);
  }

  @Override
  protected <E> Expression<?> defaultVisit(Expression<E> expression, HashMap<String, String> data) {
    return super.defaultVisit(expression, data);
  }

  @Override
  public Expression<?> visit(LetExpression expr, HashMap<String, String> data) {

    Expression flattened = expr.flattenLetExpression();
    Expression result = (Expression) visit(flattened, data);
    return result;
  }

  public <T> Expression<T> apply(Expression<T> expr, HashMap<String, String> data) {
    freeVars = ExpressionUtil.freeVariables(expr);
    return visit(expr, data).requireAs(expr.getType());
  }

}
