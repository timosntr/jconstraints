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
import gov.nasa.jpf.constraints.expressions.functions.Function;
import gov.nasa.jpf.constraints.expressions.functions.FunctionExpression;
import gov.nasa.jpf.constraints.types.Type;
import gov.nasa.jpf.constraints.util.DuplicatingVisitor;
import gov.nasa.jpf.constraints.util.ExpressionUtil;

import java.util.*;

//ToDo: refactor code
//Creation of an anti prenex form (scope of Quantifiers should be minimized)
//Quantifiers have to be handled ahead of ConjunctionCreator
public class SkolemizationVisitor extends
        DuplicatingVisitor<List<Variable<?>>> {

    private static final SkolemizationVisitor INSTANCE = new SkolemizationVisitor();

    public static SkolemizationVisitor getInstance(){
        return INSTANCE;
    }

    private int[] id = {0};
    HashMap<String, Expression> toSkolemize = new HashMap<>();
    Collection<Variable<?>> boundInOtherPath = new ArrayList<>();
    Collection<String> functionNames = new ArrayList<>();
    Set<Variable<?>> freeVars = new HashSet<>();

    @Override
    public Expression<?> visit(QuantifierExpression q, List<Variable<?>> data) {

        Quantifier quantifier = q.getQuantifier();
        List<? extends Variable<?>> bound = q.getBoundVariables();
        Expression body = q.getBody();

        //case: FORALL
        if(quantifier.equals(Quantifier.FORALL)){
            //add bound variables to data
            data.addAll(bound);
            return QuantifierExpression.create(quantifier, bound, (Expression<Boolean>) visit(body, data));

        } else {
            //case: EXISTS

            //case: EXISTS not in scope of a FORALL -> new FunctionExpression with arity 0 for each bound var
            if(data.isEmpty()){
                for(Variable var : bound){
                    String name = var.getName();
                    String nameConstant = "SK.constant." + id[0] + "." + name;
                    while(NormalizationUtil.nameClashWithExistingNames(nameConstant, functionNames)){
                        id[0]++;
                        nameConstant = "SK.constant." + id[0] + "." + name;
                    }
                    Type type = var.getType();
                    Function f = Function.create(nameConstant, type);
                    //arity here = 0
                    Variable v[] = new Variable[f.getArity()];
                    //Variable v[] = new Variable[0];
                    FunctionExpression expr = FunctionExpression.create(f, v);

                    toSkolemize.put(name, expr);
                    functionNames.add(nameConstant);
                }

            } else {
                //case: EXISTS in scope of a FORALL -> new FunctionExpression for each bound var
                for(Variable var : bound){
                    String name = var.getName();
                    String nameFunction = "SK.function." + id[0] + "." + name;
                    while(NormalizationUtil.nameClashWithExistingNames(nameFunction, functionNames)){
                        id[0]++;
                        nameFunction = "SK.function." + id[0] + "." + name;
                    }
                    Type outputType = var.getType();
                    Type<?>[] paramTypes = new Type[data.toArray().length];
                    for(int i = 0; i < paramTypes.length; i++){
                        paramTypes[i] = data.get(i).getType();
                    }
                    Function f = Function.create(nameFunction, outputType, paramTypes);
                    Variable v[] = new Variable[f.getArity()];
                    for (int i=0; i<v.length; i++) {
                        v[i] = Variable.create(data.get(i).getType(), data.get(i).getName());
                    }
                    FunctionExpression expr = FunctionExpression.create(f, v);

                    toSkolemize.put(name, expr);
                    functionNames.add(nameFunction);
                }
            }
        }
        return visit(body, data);
    }

    @Override
    public Expression<?> visit(PropositionalCompound n, List<Variable<?>> data) {
        //path-wise collection of data
        Expression leftChild = visit(n.getLeft(), data);
        //remove boundVars of leftChild from data
        n.getLeft().collectBoundVariables(boundInOtherPath);
        if(!boundInOtherPath.isEmpty()){
            data.removeAll(boundInOtherPath);
        }
        Expression rightChild = visit(n.getRight(), data);
        return PropositionalCompound.create(leftChild, n.getOperator(), rightChild);

    }

    //TODO: visit or is simple returning of expression sufficient?
    @Override
    public <E> Expression<?> visit(Variable<E> v, List<Variable<?>> data) {
        if(toSkolemize.containsKey(v.getName())){
            return toSkolemize.get(v.getName());
        }
        return super.visit(v, data);
    }

    @Override
    protected <E> Expression<?> defaultVisit(Expression<E> expression, List<Variable<?>> data) {
        return super.defaultVisit(expression, data);
    }

    @Override
    public Expression<?> visit(LetExpression expr, List<Variable<?>> data) {
        /*List<Variable> par = expr.getParameters();
        List<Variable> newPar = new ArrayList<>();
        Map<Variable, Expression> map = expr.getParameterValues();
        Map<Variable, Expression> newMap = new HashMap<>();
        //Todo: problem: Expression instead of Variable -> probably won't work without proper flattening
        for(Variable v : par){
            if(toSkolemize.containsKey(v.getName())){
                //Expression newParameter = toSkolemize.get(v.getName());
                //newPar.add(newParameter);
                newPar.add(v);
                if(map.containsKey(v.getName())){
                    Expression val = map.get(v.getName());
                    Expression newVal = visit(val, data);
                    newMap.put(v, newVal);
                    //newMap.put(newVar, newVal);
                }
            } else {
                newPar.add(v);
                newMap.put(v, map.get(v.getName()));
            }
        }
        Expression newMain = visit(expr.getMainValue(), data);

        return LetExpression.create(newPar, newMap, newMain).flattenLetExpression();*/
        Expression flattened = expr.flattenLetExpression();
        Expression result = visit(flattened, data);
        return result;
    }

    @Override
    public <E> Expression<?> visit(IfThenElse<E> n, List<Variable<?>> data) {

        Expression newIfCond = visit(n.getIf(), data);
        Expression newThenExpr = visit(n.getThen(), data);
        Expression newElseExpr = visit(n.getElse(), data);

        return IfThenElse.create(newIfCond, newThenExpr, newElseExpr);
    }

    public <T> Expression<T> apply(Expression<T> expr, List<Variable<?>> data) {
        functionNames = NormalizationUtil.collectFunctionNames(expr);
        freeVars = ExpressionUtil.freeVariables(expr);

        //free Variables are implicitly existentially quantified
        //could possibly be replaced by a separate ExistentionalClosure
        if(!freeVars.isEmpty()){
            toSkolemize = NormalizationUtil.skolemizeFreeVars(freeVars, id);
            Set<String> skolemizedFreeVars = toSkolemize.keySet();
            for(String s : skolemizedFreeVars){
                while(NormalizationUtil.nameClashWithExistingNames(s, functionNames)){
                    id[0]++;
                    s = "SK.f.constant." + id[0] + "." + s;
                }
                functionNames.add(s);
            }
        }
        return visit(expr, data).requireAs(expr.getType());
    }
}