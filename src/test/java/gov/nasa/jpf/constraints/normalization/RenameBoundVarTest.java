package gov.nasa.jpf.constraints.normalization;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.*;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;


public class RenameBoundVarTest {

    Variable<Integer> x = Variable.create(BuiltinTypes.SINT32, "x");
    Constant<Integer> c1 = Constant.create(BuiltinTypes.SINT32, 1);
    Expression e3 = NumericBooleanExpression.create(x, NumericComparator.GE, c1);
    Expression negE3 = NumericBooleanExpression.create(x, NumericComparator.LT, c1);


    @Test(groups = {"normalization"})
    public void quantifierCheckTest() {
        List<Variable<?>> bound = new ArrayList<Variable<?>>();
        bound.add(x);
        Expression<Boolean> quantified = Negation.create(QuantifierExpression.create(Quantifier.EXISTS, bound, e3));
        boolean checkExpression = NormalizationUtil.quantifierCheck(quantified);

        assertEquals(checkExpression, true);
    }

    @Test(groups = {"normalization"})
    public void quantifierCheckVisitorTest() {
        List<Variable<?>> bound = new ArrayList<Variable<?>>();
        bound.add(x);
        Expression<Boolean> quantified = Negation.create(PropositionalCompound.create(QuantifierExpression.create(Quantifier.EXISTS, bound, e3), LogicalOperator.OR, negE3));
        boolean checkExpression = (boolean) quantified.accept(QuantifierCheckVisitor.getInstance(), null);

        assertEquals(checkExpression, true);
    }
}
