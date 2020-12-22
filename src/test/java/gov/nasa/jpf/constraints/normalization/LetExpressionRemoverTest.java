package gov.nasa.jpf.constraints.normalization;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.*;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class LetExpressionRemoverTest {

    Variable<Boolean> b = Variable.create(BuiltinTypes.BOOL, "b");
    Variable<Integer> x = Variable.create(BuiltinTypes.SINT32, "x");
    Constant<Integer> c1 = Constant.create(BuiltinTypes.SINT32, 1);
    Constant<Integer> c2 = Constant.create(BuiltinTypes.SINT32, 2);
    Expression e1 = NumericBooleanExpression.create(x, NumericComparator.EQ, c1);
    Expression e2 = NumericBooleanExpression.create(x, NumericComparator.EQ, c2);

    Expression iteExpression = IfThenElse.create(b, e1, e2);
    Expression first = PropositionalCompound.create(Negation.create(b), LogicalOperator.OR, e1);
    Expression second = PropositionalCompound.create(b, LogicalOperator.OR, e2);
    Expression iteFree = PropositionalCompound.create(first, LogicalOperator.AND, second);

    Expression nestedIte = PropositionalCompound.create(e1, LogicalOperator.IMPLY, iteExpression);
    Expression iteFree2 = PropositionalCompound.create(e1, LogicalOperator.IMPLY, iteFree);

    @Test(groups = {"normalization"})
    public void ifThenElseTest() {
        Expression<Boolean> result = (Expression<Boolean>) iteExpression.accept(IfThenElseRemoverVisitor.getInstance(), null);

        assertEquals(result, iteFree);
    }

    @Test(groups = {"normalization"})
    public void nestedIfThenElseTest() {
        Expression<Boolean> result = (Expression<Boolean>) nestedIte.accept(IfThenElseRemoverVisitor.getInstance(), null);

        assertEquals(result, iteFree2);
    }

}
