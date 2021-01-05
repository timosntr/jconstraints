package gov.nasa.jpf.constraints.normalization;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.*;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class ExistentionalClosureTest {

    Variable<Integer> x = Variable.create(BuiltinTypes.SINT32, "x");
    Variable<Integer> y = Variable.create(BuiltinTypes.SINT32, "y");
    Variable<Boolean> b1 = Variable.create(BuiltinTypes.BOOL, "b1");
    Variable<Boolean> b2 = Variable.create(BuiltinTypes.BOOL, "b2");

    Constant<Integer> c1 = Constant.create(BuiltinTypes.SINT32, 1);
    Constant<Integer> c2 = Constant.create(BuiltinTypes.SINT32, 2);

    Expression e1 = NumericBooleanExpression.create(x, NumericComparator.LT, c1);
    Expression e2 = NumericBooleanExpression.create(y, NumericComparator.LE, c2);
    Expression e3 = NumericBooleanExpression.create(x, NumericComparator.GE, c1);
    Expression e4 = NumericBooleanExpression.create(y, NumericComparator.EQ, c2);

    Expression con1 = PropositionalCompound.create(e3, LogicalOperator.AND, e4);

    Expression disjunction = PropositionalCompound.create(
            PropositionalCompound.create(e3, LogicalOperator.AND, e4),
            LogicalOperator.OR,
            b1);

    @Test(groups = {"normalization"})
    //outer forall
    public void outerTest(){
        List<Variable<?>> bound = new ArrayList<Variable<?>>();
        bound.add(x);
        Expression quantified = QuantifierExpression.create(Quantifier.FORALL, bound, con1);
        Expression expected = con1;

        Expression<Boolean> noForall = (Expression<Boolean>) quantified.accept(ForallRemoverVisitor.getInstance(), null);

        assertEquals(noForall, expected);
    }

    @Test(groups = {"normalization"})
    //inner forall
    public void innerTest(){
        List<Variable<?>> bound1 = new ArrayList<Variable<?>>();
        bound1.add(x);
        List<Variable<?>> bound2 = new ArrayList<Variable<?>>();
        bound2.add(y);
        Expression quantified = QuantifierExpression.create(Quantifier.FORALL, bound1,
                PropositionalCompound.create(con1, LogicalOperator.AND,
                        QuantifierExpression.create(Quantifier.FORALL, bound2, e2)));
        Expression expected = PropositionalCompound.create(con1, LogicalOperator.AND, e2);

        Expression<Boolean> noForall = (Expression<Boolean>) quantified.accept(ForallRemoverVisitor.getInstance(), null);

        assertEquals(noForall, expected);
    }

}


