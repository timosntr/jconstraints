package gov.nasa.jpf.constraints.normalization;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.LogicalOperator;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.constraints.expressions.Quantifier;
import gov.nasa.jpf.constraints.expressions.QuantifierExpression;
import gov.nasa.jpf.constraints.expressions.functions.Function;
import gov.nasa.jpf.constraints.expressions.functions.FunctionExpression;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.types.Type;
import gov.nasa.jpf.constraints.util.DuplicatingVisitor;

import java.util.*;

//Creation of an anti prenex form (scope of Quantifiers should be minimized)
//Quantifiers have to be handled ahead of ConjunctionCreator
public class SkolemizationVisitor extends
        DuplicatingVisitor<List<Variable<?>>> {

    private static final SkolemizationVisitor INSTANCE = new SkolemizationVisitor();

    public static SkolemizationVisitor getInstance(){
        return INSTANCE;
    }

    private int id = 0;
    HashMap<String, Expression> toSkolemize = new HashMap<>();
    Collection<String> functionNames;
    boolean firstVisit = true;
    boolean inQuantifierExpression = false;

    @Override
    public Expression<?> visit(QuantifierExpression q, List<Variable<?>> data) {
        List<Variable<?>> freeVars = new ArrayList<>();
        if(firstVisit){
            functionNames = NormalizationUtil.collectFunctionNames(q);
            q.collectFreeVariables(freeVars);
            firstVisit = false;
        }
        Quantifier quantifier = q.getQuantifier();
        List<? extends Variable<?>> bound = q.getBoundVariables();
        Expression body = q.getBody();
        inQuantifierExpression = true;

        //case: FORALL
        if(quantifier.equals(Quantifier.FORALL)){
            data.addAll(bound);
            return QuantifierExpression.create(quantifier, bound, (Expression<Boolean>) visit(body, data));
        } else {
            //case: EXISTS

            //case: EXISTS not in scope of a FORALL -> new FunctionExpression with arity 0 for each bound var
            if(data.isEmpty()){
                for(Variable var : bound){
                    String name = var.getName();
                    String nameConstant = "SK.constant." + id + "." + name;
                    while(NormalizationUtil.nameClashWithFunctions(nameConstant, functionNames)){
                        id++;
                        nameConstant = "SK.constant." + id + "." + name;
                    }
                    Type type = var.getType();
                    Function f = Function.create(nameConstant, type);
                    Variable v[] = new Variable[f.getArity()];
                    FunctionExpression expr = FunctionExpression.create(f, v);

                    toSkolemize.put(name, expr);
                    functionNames.add(nameConstant);
                }

            } else {
                //case: EXISTS in scope of a FORALL -> new FunctionExpression for each bound var
                for(Variable var : bound){
                    String name = var.getName();
                    String nameFunction = "SK.function." + id + "." + name;
                    while(NormalizationUtil.nameClashWithFunctions(nameFunction, functionNames)){
                        id++;
                        nameFunction = "SK.function." + id + "." + name;
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

            //free Variables are implicitly existentially quantified
            //could possibly be replaced by ExistentionalClosure
            if(!freeVars.isEmpty()){
                for(Variable var : freeVars){
                    String name = var.getName();
                    String nameConstant = "SK.f.constant." + id + "." + name;
                    while(NormalizationUtil.nameClashWithFunctions(nameConstant, functionNames)){
                        id++;
                        nameConstant = "SK.f.constant." + id + "." + name;
                    }
                    Type type = var.getType();
                    Function f = Function.create(nameConstant, type);
                    Variable v[] = new Variable[f.getArity()];
                    FunctionExpression expr = FunctionExpression.create(f, v);

                    toSkolemize.put(name, expr);
                    functionNames.add(nameConstant);
                }
            }

        }
        return visit(body, data);
    }

    @Override
    public <E> Expression<?> visit(Variable<E> v, List<Variable<?>> data) {
        //todo: test if works with variables outside (maybe explicit closing of formula needed)
        //inQuantifierExpression stays always true after one QuantifierExpression visit;
        // that might become a problem
        if(inQuantifierExpression){
            if(toSkolemize.containsKey(v.getName())){
                return toSkolemize.get(v.getName());
            }
        }
        return super.visit(v, data);
    }

    @Override
    protected <E> Expression<?> defaultVisit(Expression<E> expression, List<Variable<?>> data) {
        if(firstVisit){
            functionNames = NormalizationUtil.collectFunctionNames(expression);
            List<Variable<?>> freeVars = new ArrayList<>();
            expression.collectFreeVariables(freeVars);
            firstVisit = false;
        }
        return super.defaultVisit(expression, data);
    }

    public <T> Expression<T> apply(Expression<T> expr, List<Variable<?>> data) {
        return visit(expr, data).requireAs(expr.getType());
    }
}