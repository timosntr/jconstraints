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
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.constraints.expressions.QuantifierExpression;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.DuplicatingVisitor;
import gov.nasa.jpf.constraints.util.RenameVarVisitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class RenameBoundVarVisitor extends
        DuplicatingVisitor<Collection<Variable<?>>> {

  private static final RenameBoundVarVisitor INSTANCE = new RenameBoundVarVisitor();
  
  public static RenameBoundVarVisitor getInstance() {
    return INSTANCE;
  }

  private boolean firstVisit = true;

  @Override
  public Function<Expression, Expression> visit(QuantifierExpression q, Collection<Variable<?>> boundVars) {

    Collection<Variable<?>> data =  new ArrayList<>();
    if(firstVisit){
      firstVisit = false;
      q.collectFreeVariables(data);
    }

    boundVars.addAll(q.getBoundVariables());

    Variable[] boundVariables = (Variable[]) boundVars.toArray();

    List<Variable> variablesToRename = new ArrayList<>();

    if(boundVariables.length >= 2) {
      for (int i = 0; i < boundVariables.length; ++i) {
        //find duplicates in boundVariables
        for (int j = 1; j < boundVariables.length; ++j) {
          if (boundVariables[i].equals(boundVariables[j])) {
            variablesToRename.add(boundVariables[j]);
          }
        }
        //find free variables with the same name like a bound variable
        if(data.contains(boundVariables[i])){
          variablesToRename.add(boundVariables[i]);
        }
      }
    }

    //nothing to do here
    if(variablesToRename.isEmpty()) {
      visit(q.getBody(), data);
    }

    Collection<Variable<?>> newBoundVariables = new ArrayList<>();

    for(Variable renamingCandidate : variablesToRename) {
      String oldName = renamingCandidate.getName();
      for(int i = 2; i < 1000; ++i) {
        String newName = oldName + i;
        //newName should be unique
        if(!data.toString().contains(newName) && !boundVariables.toString().contains(newName)) {
          for(Variable v : q.getBoundVariables()){
            if(! v.equals(renamingCandidate)){
              //ToDo: wird die ursprüngliche Reihenfolgentreue eingehalten?
              newBoundVariables.add(v);
            } else {
              Variable<?> newVariable = Variable.create(renamingCandidate.getType(), newName);
              newBoundVariables.add(newVariable);
            }
          }
        }
      }
    }
    if()
    return expression -> newVariable;

    /*  //String oldName = renamingCandidate.getName();
      //ToDo: which version is correct?
      //if(data.contains(v)) {
      //if(boundVariables.toString().contains(oldName)){
        //case1: Variable is already bound in another QuantifierExpression
        //case2: Variable exists as free Variable
        for(int i = 2; i < 1000; ++i){
          String newName = oldName + i;
          Variable<?> newVariable = Variable.create(renamingCandidate.getType(), newName);
          if(!data.toString().contains(newName) && !boundVariables.toString().contains(newName)){
            //ToDo: how to replace Variable in BoundVariables?
            for(Variable v : boundVariables){
              if(! v.equals(renamingCandidate)){
                int index = boundVariables.indexOf(v);
                newBoundVariables.add(index, v);
              } else {
                int index = boundVariables.indexOf(v);
                newBoundVariables.add(index, newVariable);
              }
            }
            Expression[] quantifiedBody = q.getBody().getChildren();
            for(Expression c : quantifiedBody){
              if(! c.equals(renamingCandidate)){
                //ToDo: how to add Expression on the right place in Body?
                visit(c, data);
              } else {
                //ToDo: how to replace Variable in Body?
                Function<String, String> rename = s -> newName;
                RenameVarVisitor.getInstance().apply(q.getBody(), rename);
              }
            }

            //Function<Expression, Expression> rename = s -> newVariable;
            //RenameVarVisitor.getInstance().apply(q.getBody(), rename);
            //add Variable with its new Name
            data.add(newVariable);
            break;
          }
        }
      } else {
        data.add(renamingCandidate);
      }
    }
    Expression result = QuantifierExpression.create(q.getQuantifier(), newBoundVariables, (Expression<Boolean>) visit(q.getBody(), data));
    return result;*/
  }

  //ToDo: does the order of visited expressions change anything?
  //ToDo: (collectFreeVariables could contain many duplicates, can it be prevented?)
  @Override
  protected <E> Expression<?> defaultVisit(Expression<E> expression, Collection<Variable<?>> data) {

    if(firstVisit){
      firstVisit = false;
      expression.collectFreeVariables(data);
    }
    return super.defaultVisit(expression, data);
  }



}
