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
        List<? extends Variable> bound = q.getBoundVariables();
        Expression body = visit(q.getBody(), data);
        //if quantified body is not a Propositional Compound, mini scoping is done here
        //negations have to be pushed beforehand!
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

        if(!boundInFreeLeft){
            //no bound variables in left child of the Propositional Compound
            Expression newLeft = visit(leftChild, data);
            Expression newRight = visit(QuantifierExpression.create(quantifier, (List<? extends Variable<?>>) bound, rightChild), data);
            return PropositionalCompound.create(newLeft, operator, newRight);
        }

        if(!boundInFreeRight){
            //no bound variables in right child of the Propositional Compound
            Expression newLeft = visit(QuantifierExpression.create(quantifier, (List<? extends Variable<?>>) bound, leftChild), data);
            Expression newRight = visit(rightChild, data);
            return PropositionalCompound.create(newLeft, operator, newRight);
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
                Expression result = NormalizationUtil.createCNF(body);
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
                Expression result = NormalizationUtil.createDNF(body);
                if(result instanceof PropositionalCompound){
                    LogicalOperator newOperator = ((PropositionalCompound) result).getOperator();
                    if(newOperator == LogicalOperator.OR){
                        return visit(QuantifierExpression.create(quantifier, (List<? extends Variable<?>>) bound, result));
                    }
                }
            }
        }
        //no other option, I guess
        return q;
    }

    public <T> Expression<T> apply(Expression<T> expr, Void data) {
        return visit(expr, data).requireAs(expr.getType());
    }
}