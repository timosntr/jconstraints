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
import gov.nasa.jpf.constraints.util.DuplicatingVisitor;
//ToDo: evtl entfernen
public class ReplaceVarVisitor extends
        DuplicatingVisitor<Function<Expression, Expression>> {
  
  private static final ReplaceVarVisitor INSTANCE = new ReplaceVarVisitor();
  
  public static ReplaceVarVisitor getInstance() {
    return INSTANCE;
  }

  @Override
  public <E> Expression<?> visit(Variable<E> v, Function<Expression, Expression> data) {
    Expression newExpression = data.apply(v);
    return newExpression;
  }
  
  public <T> Expression<T> apply(Expression<T> expr, Function<Expression, Expression> replace) {
    return visit(expr, replace).requireAs(expr.getType());
  }

}