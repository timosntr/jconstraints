package gov.nasa.jpf.constraints.normalization;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.*;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

public class MiniScopingTest {

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
    Expression dis1 = PropositionalCompound.create(e2, LogicalOperator.OR, e4);
    Expression con2 = PropositionalCompound.create(e1, LogicalOperator.AND, e2);
    Expression dis2 = PropositionalCompound.create(e1, LogicalOperator.OR, e2);
    Expression dis3 = PropositionalCompound.create(e1, LogicalOperator.OR, con2);

    Expression disjunction = PropositionalCompound.create(
            PropositionalCompound.create(e3, LogicalOperator.AND, e4),
            LogicalOperator.OR,
            b1);
    Expression expected = PropositionalCompound.create(
            PropositionalCompound.create(e3, LogicalOperator.OR, b1),
            LogicalOperator.AND,
            PropositionalCompound.create(e4, LogicalOperator.OR, b1));
    Expression nestedDisjunction = PropositionalCompound.create(e3, LogicalOperator.OR, disjunction);

    @Test(groups = {"normalization"})
    //free vars left
    public void leftTest(){
        List<Variable<?>> bound = new ArrayList<>();
        bound.add(x);
        Expression q = ExpressionUtil.or(e1,
                QuantifierExpression.create(Quantifier.FORALL, bound, ExpressionUtil.or(e2, e1)));

        Expression<Boolean> minimized = (Expression<Boolean>) q.accept(MiniScopingVisitor.getInstance(), null);
        System.out.println(minimized);
    }

    @Test(groups = {"normalization"})
    //free vars right
    public void rightTest(){
        List<Variable<?>> bound = new ArrayList<>();
        bound.add(x);
        Expression q = ExpressionUtil.or(QuantifierExpression.create(Quantifier.FORALL, bound, ExpressionUtil.and(e1, e2)), dis1);

        Expression<Boolean> minimized = (Expression<Boolean>) q.accept(MiniScopingVisitor.getInstance(), null);
        System.out.println(minimized);
    }

    @Test(groups = {"normalization"})
    //free vars in both
    public void existsTest(){
        List<Variable<?>> bound = new ArrayList<>();
        bound.add(x);
        Expression q = QuantifierExpression.create(Quantifier.EXISTS, bound, ExpressionUtil.and(ExpressionUtil.or(e1, e2), con1));

        Expression<Boolean> minimized = (Expression<Boolean>) q.accept(MiniScopingVisitor.getInstance(), null);
        System.out.println(q);
        System.out.println(minimized);
    }

    @Test(groups = {"normalization"})
    //free vars in both
    public void forallTest(){
        List<Variable<?>> bound = new ArrayList<>();
        bound.add(x);
        Expression q = QuantifierExpression.create(Quantifier.FORALL, bound, ExpressionUtil.or(ExpressionUtil.and(e1, e2), con1));

        Expression<Boolean> minimized = (Expression<Boolean>) q.accept(MiniScopingVisitor.getInstance(), null);
        System.out.println(q);
        System.out.println(minimized);
    }

    @Test(groups = {"normalization"})
    //free vars in both
    public void multipleQuantifierTest(){
        List<Variable<?>> bound1 = new ArrayList<>();
        bound1.add(x);
        List<Variable<?>> bound2 = new ArrayList<>();
        bound2.add(y);
        Expression q = QuantifierExpression.create(Quantifier.EXISTS, bound1,
                QuantifierExpression.create(Quantifier.FORALL, bound2, ExpressionUtil.and(ExpressionUtil.or(e1, e2), con1)));

        Expression<Boolean> minimized = (Expression<Boolean>) q.accept(MiniScopingVisitor.getInstance(), null);
        System.out.println(q);
        System.out.println(minimized);
    }

}


