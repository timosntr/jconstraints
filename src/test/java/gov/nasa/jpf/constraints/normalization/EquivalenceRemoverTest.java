package gov.nasa.jpf.constraints.normalization;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.*;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class EquivalenceRemoverTest {

    Variable<Integer> x = Variable.create(BuiltinTypes.SINT32, "x");
    Constant<Integer> c1 = Constant.create(BuiltinTypes.SINT32, 1);
    Constant<Integer> c2 = Constant.create(BuiltinTypes.SINT32, 2);
    Expression e1 = NumericCompound.create(x, NumericOperator.PLUS, c1);
    Expression negE1 = Negation.create(e1);
    Expression e2 = NumericBooleanExpression.create(x, NumericComparator.GE, c1);
    Expression negE2 = Negation.create(e2);

    Expression p1 = PropositionalCompound.create(e1, LogicalOperator.AND, e2);
    Expression p2 = NumericBooleanExpression.create(e1, NumericComparator.EQ, c2);
    Expression p3 = PropositionalCompound.create(e1, LogicalOperator.EQUIV, e2);
    Expression negP1 = Negation.create(p1);
    Expression negP2 = Negation.create(p2);

    Expression<Boolean> containsEquiv = PropositionalCompound.create(p1, LogicalOperator.EQUIV, p2);
    Expression<Boolean> containsEquiv2 = PropositionalCompound.create(p1, LogicalOperator.AND, p3);

    Expression<Boolean> equivFree = PropositionalCompound.create(
            PropositionalCompound.create(negP1, LogicalOperator.OR, p2),
            LogicalOperator.AND,
            PropositionalCompound.create(negP2, LogicalOperator.OR, p1));

    Expression<Boolean> equivFree2 = PropositionalCompound.create(
            p1,
            LogicalOperator.AND,
            PropositionalCompound.create(
                    PropositionalCompound.create(negE1, LogicalOperator.OR, e2),
                    LogicalOperator.AND,
                    PropositionalCompound.create(negE2, LogicalOperator.OR, e1)));


    @Test(groups = {"normalization"})
    public void equivalenceRemoverTest() {
        Expression<Boolean> result = (Expression<Boolean>) containsEquiv.accept(EquivalenceRemoverVisitor.getInstance(), null);

        assertEquals(result, equivFree);
    }

    @Test(groups = {"normalization"})
    public void nestedEquivalenceRemoverTest() {
        Expression<Boolean> result = (Expression<Boolean>) containsEquiv2.accept(EquivalenceRemoverVisitor.getInstance(), null);

        assertEquals(result, equivFree2);
    }

}
