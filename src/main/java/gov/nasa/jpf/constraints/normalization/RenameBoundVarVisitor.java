/*
 * Copyright (C) 2015, United States Government, as represented by the 
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 *
 * The PSYCO: A Predicate-based Symbolic Compositional Reasoning environment 
 * platform is licensed under the Apache License, Version 2.0 (the "License"); you 
 * may not use this file except in compliance with the License. You may obtain a 
 * copy of the License at http://www.apache.org/licenses/LICENSE-2.0. 
 *
 * Unless required by applicable law or agreed to in writing, software distributed 
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR 
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the 
 * specific language governing permissions and limitations under the License.
 */
package gov.nasa.jpf.constraints.normalization;

import com.google.common.base.Function;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.QuantifierExpression;
import gov.nasa.jpf.constraints.util.RenameVarVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class RenameBoundVarVisitor extends
        RenameVarVisitor {
  
  private static final RenameBoundVarVisitor INSTANCE = new RenameBoundVarVisitor();
  
  public static RenameBoundVarVisitor getInstance() {
    return INSTANCE;
  }

  private boolean nestedQuantifier = false;
  private int[] id = {0};
  Collection<Variable<?>> freeVars = new ArrayList<>();

  @Override
  public <E> Expression<?> visit(Variable<E> v, Function<String, String> data) {

    String newName = "";
    if(data != null){
      String renaming = data.apply(v.getName());
      if(renaming != null){
        newName = data.apply(v.getName());
      } else {
        newName = v.getName();
      }
    } else {
      newName = v.getName();
    }

    return Variable.create(v.getType(), newName);
  }

  @Override
  public Expression<?> visit(QuantifierExpression q, Function<String, String> data){
    List<Variable<?>> boundVariables = (List<Variable<?>>) q.getBoundVariables();
    id[0]++;
    q.collectFreeVariables(freeVars);

    if(nestedQuantifier) {
      //Quantifier in a Quantifier: data may already contain a renaming, so it has to be changed!
      for (Variable v : boundVariables) {
        if(data != null){
          String renaming = data.apply(v.getName());
          if (renaming != null) {
            //data contains a renaming for another bound variable with the same name
            data = NormalizationUtil.renameBoundVariables(q, id, freeVars);
          }
        }
      }
    }
    LinkedList<Variable<?>> renamedBoundVariables = new LinkedList<>();

    data = NormalizationUtil.renameBoundVariables(q, id, freeVars);
    //rename boundVariables if they are in data
    for (Variable v : boundVariables) {
      String renaming = data.apply(v.getName());
      if (renaming != null) {
        renamedBoundVariables.add(Variable.create(v.getType(), renaming));
      } else {
        renamedBoundVariables.add(v);
      }
    }

    nestedQuantifier = true;
    //rename variables in expression
    Expression renamedExpr = visit(q.getBody(), data);
    return QuantifierExpression.create(q.getQuantifier(), renamedBoundVariables, renamedExpr);
  }

  @Override
  protected <E> Expression<?> defaultVisit(Expression<E> expression, Function<String, String> data) {

    expression.collectFreeVariables(freeVars);
    return super.defaultVisit(expression, data);
  }

  public <T> Expression<T> apply(Expression<T> expr, Function<String,String> rename) {
    return visit(expr, rename).requireAs(expr.getType());
  }

}
