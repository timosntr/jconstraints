package gov.nasa.jpf.constraints.normalization;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.*;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import org.testng.annotations.Test;

public class NormalizationUtilTest {

    Variable<Integer> x = Variable.create(BuiltinTypes.SINT32, "x");
    Variable<Integer> y = Variable.create(BuiltinTypes.SINT32, "y");
    Variable<Boolean> b1 = Variable.create(BuiltinTypes.BOOL, "b1");
    Variable<Boolean> b2 = Variable.create(BuiltinTypes.BOOL, "b2");

    Constant<Integer> c1 = Constant.create(BuiltinTypes.SINT32, 1);
    Constant<Integer> c2 = Constant.create(BuiltinTypes.SINT32, 2);

    Expression e1 = NumericBooleanExpression.create(x, NumericComparator.LT, c1);
    Expression e2 = NumericBooleanExpression.create(y, NumericComparator.LE, c2);

    Expression con1 = PropositionalCompound.create(e1, LogicalOperator.AND, e2);
    Expression dis1 = PropositionalCompound.create(e1, LogicalOperator.OR, e2);
    Expression xor = PropositionalCompound.create(con1, LogicalOperator.XOR, e2);
    Expression imply = PropositionalCompound.create(con1, LogicalOperator.IMPLY, e2);
    Expression negation1 = Negation.create(xor);
    Expression negation2 = Negation.create(imply);

    @Test(groups = {"normalization"})
    public void basicTest1(){
        Expression result = NormalizationUtil.pushNegation(negation1);
        System.out.println(negation1);
        System.out.println(result);
    }

    @Test(groups = {"normalization"})
    public void basicTest2(){
        Expression result = NormalizationUtil.pushNegation(negation2);
        System.out.println(negation2);
        System.out.println(result);
    }
}
