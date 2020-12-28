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

public class ConjunctionCreatorTest {

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
    Expression dis1 = PropositionalCompound.create(e3, LogicalOperator.OR, e4);
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
    //case: (A AND B)
    public void basicTest1(){
        Expression simpleConjunction = PropositionalCompound.create(e3, LogicalOperator.AND, e4);

        Expression<Boolean> cnf = (Expression<Boolean>) simpleConjunction.accept(ConjunctionCreatorVisitor.getInstance(), null);

        assertEquals(cnf, simpleConjunction);
    }

    @Test(groups = {"normalization"})
    //case: (A OR B)
    public void basicTest2(){
        Expression simpleDisjunction = PropositionalCompound.create(e3, LogicalOperator.OR, e4);

        Expression<Boolean> cnf = (Expression<Boolean>) simpleDisjunction.accept(ConjunctionCreatorVisitor.getInstance(), null);

        assertEquals(cnf, simpleDisjunction);
    }

    @Test(groups = {"normalization"})
    //case: (A AND B) OR C
    public void disjunctionTest(){
        Expression<Boolean> cnf = (Expression<Boolean>) disjunction.accept(ConjunctionCreatorVisitor.getInstance(), null);

        assertEquals(cnf, expected);
    }

    @Test(groups = {"normalization"})
    //case: (A OR B) OR C
    public void disjunctionTest2(){
        Expression disjunction2 = PropositionalCompound.create(
                PropositionalCompound.create(e3, LogicalOperator.OR, e4),
                LogicalOperator.OR,
                b1);

        Expression<Boolean> cnf = (Expression<Boolean>) disjunction2.accept(ConjunctionCreatorVisitor.getInstance(), null);

        assertEquals(cnf, disjunction2);
    }

    @Test(groups = {"normalization"})
    //case: A AND (A OR B)
    public void conjunctionTest1(){
        Expression conjunction1 = PropositionalCompound.create(
                b1,
                LogicalOperator.AND,
                PropositionalCompound.create(b1, LogicalOperator.OR, b2));

        Expression<Boolean> cnf = (Expression<Boolean>) conjunction1.accept(ConjunctionCreatorVisitor.getInstance(), null);

        assertEquals(cnf, conjunction1);
    }

    @Test(groups = {"normalization"})
    //case: (A) OR (C AND D)
    public void disjunctionTest3(){
        Expression disjunction3 = PropositionalCompound.create(
                b1,
                LogicalOperator.OR,
                PropositionalCompound.create(e3, LogicalOperator.AND, e4));
        Expression expected3 = PropositionalCompound.create(
                PropositionalCompound.create(b1, LogicalOperator.OR, e3),
                LogicalOperator.AND,
                PropositionalCompound.create(b1, LogicalOperator.OR, e4));

        Expression<Boolean> cnf = (Expression<Boolean>) disjunction3.accept(ConjunctionCreatorVisitor.getInstance(), null);

        assertEquals(cnf, expected3);
    }

    @Test(groups = {"normalization"})
    //case: (A) OR (C OR D)
    public void disjunctionTest4(){
        Expression disjunction4 = PropositionalCompound.create(
                b1,
                LogicalOperator.OR,
                PropositionalCompound.create(e3, LogicalOperator.OR, e4));

        Expression<Boolean> cnf = (Expression<Boolean>) disjunction4.accept(ConjunctionCreatorVisitor.getInstance(), null);

        assertEquals(cnf, disjunction4);
    }

    @Test(groups = {"normalization"})
    //case: (A AND B) OR (C AND D)
    public void disjunctionTest5(){
        Expression disjunction5 = PropositionalCompound.create(con1, LogicalOperator.OR, con2);
        Expression expected = PropositionalCompound.create(
                PropositionalCompound.create(
                        PropositionalCompound.create(e3, LogicalOperator.OR, e1),
                        LogicalOperator.AND,
                        PropositionalCompound.create(e3, LogicalOperator.OR, e2)),
                LogicalOperator.AND,
                PropositionalCompound.create(
                        PropositionalCompound.create(e4, LogicalOperator.OR, e1),
                        LogicalOperator.AND,
                        PropositionalCompound.create(e4, LogicalOperator.OR, e2))
                );

        Expression<Boolean> cnf = (Expression<Boolean>) disjunction5.accept(ConjunctionCreatorVisitor.getInstance(), null);

        assertEquals(cnf, expected);
    }

    @Test(groups = {"normalization"})
    //case: (A OR B) OR (C OR D)
    public void disjunctionTest6(){
        Expression disjunction6 = PropositionalCompound.create(dis1, LogicalOperator.OR, dis2);

        Expression<Boolean> cnf = (Expression<Boolean>) disjunction6.accept(ConjunctionCreatorVisitor.getInstance(), null);

        assertEquals(cnf, disjunction6);
    }

    @Test(groups = {"normalization"})
    //case: (A OR B) OR (C OR D)
    public void disjunctionNestedTest(){
        Expression disjunction6 = PropositionalCompound.create(dis1, LogicalOperator.OR, dis3);

        Expression<Boolean> cnf = (Expression<Boolean>) disjunction6.accept(ConjunctionCreatorVisitor.getInstance(), null);
        System.out.print(cnf);
    }

    @Test(groups = {"normalization"})
    //case: (A AND B) OR (C OR D)
    public void disjunctionTest7(){
          Expression disjunction7 = PropositionalCompound.create(con1, LogicalOperator.OR, dis1);
        Expression expected = PropositionalCompound.create(
                PropositionalCompound.create(e3, LogicalOperator.OR, dis1),
                LogicalOperator.AND,
                PropositionalCompound.create(e4, LogicalOperator.OR, dis1));

        Expression<Boolean> cnf = (Expression<Boolean>) disjunction7.accept(ConjunctionCreatorVisitor.getInstance(), null);

        assertEquals(cnf, expected);
    }

    @Test(groups = {"normalization"})
    //case: (A OR B) OR (C AND D)
    public void disjunctionTest8(){
        Expression disjunction8 = PropositionalCompound.create(dis1, LogicalOperator.OR, con1);
        Expression expected = PropositionalCompound.create(
                PropositionalCompound.create(dis1, LogicalOperator.OR, e3),
                LogicalOperator.AND,
                PropositionalCompound.create(dis1, LogicalOperator.OR, e4));

        Expression<Boolean> cnf = (Expression<Boolean>) disjunction8.accept(ConjunctionCreatorVisitor.getInstance(), null);

        assertEquals(cnf, expected);
    }

    @Test(groups = {"normalization"})
    //A OR ((A AND B) OR C)
    public void nestedDisjunctionTest(){
        Expression<Boolean> cnf = (Expression<Boolean>) nestedDisjunction.accept(ConjunctionCreatorVisitor.getInstance(), null);
        Expression nestedExpected = PropositionalCompound.create(
                PropositionalCompound.create(
                        e3,
                        LogicalOperator.OR,
                        PropositionalCompound.create(e3, LogicalOperator.OR, b1)),
                LogicalOperator.AND,
                PropositionalCompound.create(
                        e3,
                        LogicalOperator.OR,
                        PropositionalCompound.create(e4, LogicalOperator.OR, b1)));

        assertEquals(cnf, nestedExpected);
    }

    //not needed as quantifiers have to be removed before transformation
    /*@Test(groups = {"normalization"})
    public void quantifierTest(){
        List<Variable<?>> bound = new ArrayList<>();
        bound.add(x);
        bound.add(y);
        Expression quantifiedDisjunction = QuantifierExpression.create(Quantifier.FORALL, bound, disjunction);
        Expression<Boolean> cnf = (Expression<Boolean>) quantifiedDisjunction.accept(ConjunctionCreatorVisitor.getInstance(), null);

        assertEquals(cnf, quantifiedDisjunction);
    }*/

    @Test(groups = {"normalization"})
    public void andTest(){
        Expression and = PropositionalCompound.create(
                        PropositionalCompound.create(b1, LogicalOperator.OR, b2),
                        LogicalOperator.AND,
                        PropositionalCompound.create(
                                PropositionalCompound.create(b1, LogicalOperator.OR, b2),
                                LogicalOperator.AND,
                                PropositionalCompound.create(b1, LogicalOperator.AND, b2)));

        Expression<Boolean> cnf = (Expression<Boolean>) and.accept(ConjunctionCreatorVisitor.getInstance(), null);

        assertEquals(cnf, and);
    }

    @Test(groups = {"normalization"})
    public void nestedTest1(){
        Expression nested = PropositionalCompound.create(
                PropositionalCompound.create(e3, LogicalOperator.AND, e4),
                LogicalOperator.OR,
                PropositionalCompound.create(
                        e3,
                        LogicalOperator.OR,
                        PropositionalCompound.create(e3, LogicalOperator.AND, e4)));

        Expression<Boolean> cnf = (Expression<Boolean>) nested.accept(ConjunctionCreatorVisitor.getInstance(), null);

        Expression expected =
                PropositionalCompound.create(
                        PropositionalCompound.create(
                                PropositionalCompound.create(
                                        e3,
                                        LogicalOperator.OR,
                                        PropositionalCompound.create(e3, LogicalOperator.OR, e3)),
                        LogicalOperator.AND,
                        PropositionalCompound.create(
                                e3,
                                LogicalOperator.OR,
                                PropositionalCompound.create(e3, LogicalOperator.OR, e4))),
                LogicalOperator.AND,
                PropositionalCompound.create(
                        PropositionalCompound.create(
                                e4,
                                LogicalOperator.OR,
                                PropositionalCompound.create(e3, LogicalOperator.OR, e3)),
                        LogicalOperator.AND,
                        PropositionalCompound.create(
                                e4,
                                LogicalOperator.OR,
                                PropositionalCompound.create(e3, LogicalOperator.OR, e4))));

        assertEquals(cnf, expected);
    }

    @Test(groups = {"normalization"})
    public void nestedTest2(){
        Expression nested2 = PropositionalCompound.create(
                PropositionalCompound.create(b1, LogicalOperator.AND, b2),
                LogicalOperator.OR,
                PropositionalCompound.create(
                        b1,
                        LogicalOperator.AND,
                        PropositionalCompound.create(b1, LogicalOperator.OR, b2)));

        Expression expected = ExpressionUtil.and(ExpressionUtil.and(ExpressionUtil.or(b1, b1), ExpressionUtil.or(b1, ExpressionUtil.or(b1, b2))),
                ExpressionUtil.and(ExpressionUtil.or(b2, b1), ExpressionUtil.or(b2, ExpressionUtil.or(b1, b2))));

        Expression<Boolean> cnf = (Expression<Boolean>) nested2.accept(ConjunctionCreatorVisitor.getInstance(), null);

        assertEquals(cnf, expected);
    }

    @Test(groups = {"normalization"})
    public void nestedTest3(){
        Expression nested3 = ExpressionUtil.and(b1, ExpressionUtil.and(ExpressionUtil.or(ExpressionUtil.and(e3,e3)),e3),
                ExpressionUtil.or(e4, ExpressionUtil.and(e4, e4)));

        Expression<Boolean> cnf = (Expression<Boolean>) nested3.accept(ConjunctionCreatorVisitor.getInstance(), null);

        assertNotEquals(cnf, nested3);
    }

    @Test(groups = {"normalization"})
    public void nestedTest4(){
        Expression nested4 = ExpressionUtil.or(ExpressionUtil.or(b1, b2), ExpressionUtil.or(ExpressionUtil.and(e3, e4)), ExpressionUtil.or(e3, e4));

        Expression<Boolean> cnf = (Expression<Boolean>) nested4.accept(ConjunctionCreatorVisitor.getInstance(), null);

        assertNotEquals(cnf, nested4);
    }

    @Test(groups = {"normalization"})
    public void nestedTest(){
        Expression nested = PropositionalCompound.create(
                PropositionalCompound.create(b1, LogicalOperator.AND, b2),
                LogicalOperator.OR,
                PropositionalCompound.create(
                        PropositionalCompound.create(b1, LogicalOperator.AND, b2),
                        LogicalOperator.OR,
                        PropositionalCompound.create(b1, LogicalOperator.AND, b2)));

        Expression<Boolean> cnf = (Expression<Boolean>) nested.accept(ConjunctionCreatorVisitor.getInstance(), null);

        System.out.println(cnf);
    }
}


