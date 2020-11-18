package gov.nasa.jpf.constraints.normalization;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.QuantifierExpression;

public class NormalizationUtil {



    //ToDo: decide whether check here or as separate visitor
    public static boolean quantifierCheck(Expression<?> expr){
        if(expr instanceof QuantifierExpression){
            return true;
        }

        Expression<?>[] exprChildren = expr.getChildren();
        for(Expression i : exprChildren){
            if(quantifierCheck(i)){
                return true;
            }
        }
        return false;
    }
}
