package gov.nasa.jpf.constraints.normalization;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.*;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class LetExpressionRemoverTest {

    Variable x1 = Variable.create(BuiltinTypes.SINT32, "x1");
    Variable x2 = Variable.create(BuiltinTypes.SINT32, "x2");
    Constant c2 = Constant.create(BuiltinTypes.SINT32, 2);
    Constant c4 = Constant.create(BuiltinTypes.SINT32, 4);
    NumericBooleanExpression partA = NumericBooleanExpression.create(x1, NumericComparator.LE, c4);
    NumericBooleanExpression partB = NumericBooleanExpression.create(x2, NumericComparator.GE, c2);
    NumericCompound replacementA = NumericCompound.create(x2, NumericOperator.PLUS, c2);
    NumericCompound replacementB = NumericCompound.create(x2, NumericOperator.MINUS, c2);

    Expression<Boolean> letExpression = LetExpression.create(x1, replacementA, partA);
    Expression letFree = NumericBooleanExpression.create(replacementA, NumericComparator.LE, c4);
    Expression<Boolean> nestedLet = PropositionalCompound.create(
            LetExpression.create(x2, replacementB, partB), LogicalOperator.AND, letExpression);
    Expression letFree2 = PropositionalCompound.create(
            NumericBooleanExpression.create(replacementB, NumericComparator.GE, c2), LogicalOperator.AND, letFree);



    @Test(groups = {"normalization"})
    public void letTest() {
        Expression<Boolean> result = (Expression<Boolean>) letExpression.accept(LetExpressionRemoverVisitor.getInstance(), null);

        assertEquals(result, letFree);
    }

    @Test(groups = {"normalization"})
    public void nestedLetTest() {
        Expression<Boolean> result = (Expression<Boolean>) nestedLet.accept(LetExpressionRemoverVisitor.getInstance(), null);

        assertEquals(result, letFree2);
    }

}
