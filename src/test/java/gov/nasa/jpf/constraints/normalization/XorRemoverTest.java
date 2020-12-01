package gov.nasa.jpf.constraints.normalization;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.*;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class XorRemoverTest {

    Variable<Integer> x = Variable.create(BuiltinTypes.SINT32, "x");
    Constant<Integer> c1 = Constant.create(BuiltinTypes.SINT32, 1);
    Constant<Integer> c2 = Constant.create(BuiltinTypes.SINT32, 2);
    Expression e1 = NumericBooleanExpression.create(x, NumericComparator.EQ, c1);
    Expression e2 = NumericBooleanExpression.create(x, NumericComparator.EQ, c2);

    Expression xorExpression = PropositionalCompound.create(e1, LogicalOperator.XOR, e2);
    Expression first = PropositionalCompound.create(e1, LogicalOperator.OR, e2);
    Expression second = PropositionalCompound.create(Negation.create(e1), LogicalOperator.OR, Negation.create(e2));
    Expression xorFree = PropositionalCompound.create(first, LogicalOperator.AND, second);

    Expression nestedXor = PropositionalCompound.create(e1, LogicalOperator.IMPLY, xorExpression);
    Expression xorFree2 = PropositionalCompound.create(e1, LogicalOperator.IMPLY, xorFree);

    @Test(groups = {"normalization"})
    public void xorTest() {
        Expression<Boolean> result = (Expression<Boolean>) xorExpression.accept(XorRemoverVisitor.getInstance(), null);

        assertEquals(result, xorFree);
    }

    @Test(groups = {"normalization"})
    public void nestedXorTest() {
        Expression<Boolean> result = (Expression<Boolean>) nestedXor.accept(XorRemoverVisitor.getInstance(), null);

        assertEquals(result, xorFree2);
    }

}
