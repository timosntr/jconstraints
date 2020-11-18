package gov.nasa.jpf.constraints.normalization;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.*;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import javafx.util.Pair;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class PolarityTest {

    Variable<Integer> x = Variable.create(BuiltinTypes.SINT32, "x");
    Variable<Integer> y = Variable.create(BuiltinTypes.SINT32, "y");
    Variable<Boolean> b1 = Variable.create(BuiltinTypes.BOOL, "b1");

    Constant<Integer> c1 = Constant.create(BuiltinTypes.SINT32, 1);
    Constant<Integer> c2 = Constant.create(BuiltinTypes.SINT32, 2);

    Expression e3 = NumericBooleanExpression.create(x, NumericComparator.GE, c1);
    Expression e4 = NumericBooleanExpression.create(y, NumericComparator.EQ, c2);
    Expression negE3 = NumericBooleanExpression.create(x, NumericComparator.LT, c1);
    Expression negE4 = NumericBooleanExpression.create(y, NumericComparator.NE, c2);

    @Test(groups = {"normalization"})
    public void positivePolarity1(){

    }

    public void positivePolarity2(){

    }

    @Test(groups = {"normalization"})
    public void negativePolarity1(){
        Expression<Boolean> disjunction = ExpressionUtil.or(negE3, negE4);
        List<Pair> list = new ArrayList<Pair>();
        list = (List<Pair>) disjunction.accept(PolarityVisitor.getInstance(), null);

    }

    public void negativePolarity2(){

    }

    @Test(groups = {"normalization"})
    public void neutralPolarity(){

    }

}
