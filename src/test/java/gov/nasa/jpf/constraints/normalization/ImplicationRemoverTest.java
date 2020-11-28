package gov.nasa.jpf.constraints.normalization;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.*;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class ImplicationRemoverTest {

        Variable<Integer> x = Variable.create(BuiltinTypes.SINT32, "x");
        Constant<Integer> c1 = Constant.create(BuiltinTypes.SINT32, 1);
        Constant<Integer> c2 = Constant.create(BuiltinTypes.SINT32, 2);
        Expression e1 = NumericBooleanExpression.create(x, NumericComparator.LT, c1);
        Expression negE1 = Negation.create(e1);
        Expression e2 = NumericBooleanExpression.create(x, NumericComparator.LE, c2);

        Expression p1 = PropositionalCompound.create(e1, LogicalOperator.AND, e2);
        Expression p2 = NumericBooleanExpression.create(x, NumericComparator.EQ, c2);
        Expression p3 = PropositionalCompound.create(e1, LogicalOperator.IMPLY, e2);
        Expression negP1 = Negation.create(p1);

        Expression<Boolean> containsImply = PropositionalCompound.create(p1, LogicalOperator.IMPLY, p2);
        Expression<Boolean> containsImply2 = PropositionalCompound.create(p1, LogicalOperator.AND, p3);

        Expression<Boolean> implyFree = PropositionalCompound.create(negP1, LogicalOperator.OR, p2);

        Expression<Boolean> implyFree2 = PropositionalCompound.create(
                p1,
                LogicalOperator.AND,
                PropositionalCompound.create(negE1, LogicalOperator.OR, e2));


    @Test(groups = {"normalization"})
    public void implicationRemoverTest() {
        Expression<Boolean> result = (Expression<Boolean>) containsImply.accept(ImplicationRemoverVisitor.getInstance(), null);

        assertEquals(result, implyFree);
    }

    @Test(groups = {"normalization"})
    public void nestedImplicationRemoverTest() {
        Expression<Boolean> result = (Expression<Boolean>) containsImply2.accept(ImplicationRemoverVisitor.getInstance(), null);

        assertEquals(result, implyFree2);
    }

}
