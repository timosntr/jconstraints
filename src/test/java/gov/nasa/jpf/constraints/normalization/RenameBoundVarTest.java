package gov.nasa.jpf.constraints.normalization;

import com.google.common.base.Function;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.*;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.testng.Assert.assertEquals;


public class RenameBoundVarTest {

    Variable<Integer> x = Variable.create(BuiltinTypes.SINT32, "x");
    Variable<Integer> y = Variable.create(BuiltinTypes.SINT32, "y");
    Variable<Integer> y2 = Variable.create(BuiltinTypes.SINT32, "Q.1.x");
    Variable<Integer> z = Variable.create(BuiltinTypes.SINT32, "z");
    Constant<Integer> c1 = Constant.create(BuiltinTypes.SINT32, 1);
    Constant<Integer> c2 = Constant.create(BuiltinTypes.SINT32, 2);
    Expression e1 = NumericBooleanExpression.create(x, NumericComparator.LT, c1);
    Expression e2 = NumericBooleanExpression.create(y, NumericComparator.GE, c1);
    Expression e2mod = NumericBooleanExpression.create(y2, NumericComparator.GE, c1);


    @Test(groups = {"normalization"})
    //works
    public void simpleRenamingTest() {
        Function<String, String> data = null;
        List<Variable<?>> bound = new ArrayList<>();
        bound.add(x);
        Expression q = QuantifierExpression.create(Quantifier.FORALL, bound, ExpressionUtil.and(e1,e2));
        Expression<Boolean> renamed = (Expression<Boolean>) q.accept(RenameBoundVarVisitor.getInstance(), data);
        System.out.println(renamed);
    }

    @Test(groups = {"normalization"})
    //works
    public void nestedRenamingTest1() {
        Function<String, String> data = null;
        List<Variable<?>> bound = new ArrayList<>();
        bound.add(x);
        Expression q = ExpressionUtil.or(e1,
                QuantifierExpression.create(Quantifier.FORALL, bound, e1));

        Expression<Boolean> renamed = (Expression<Boolean>) q.accept(RenameBoundVarVisitor.getInstance(), data);
        System.out.println(renamed);
    }

    @Test(groups = {"normalization"})
    //works
    public void nestedRenamingTest2() {
        Function<String, String> data = null;
        List<Variable<?>> bound = new ArrayList<>();
        bound.add(x);
        Expression q = ExpressionUtil.or(e1, ExpressionUtil.and(
                QuantifierExpression.create(Quantifier.FORALL, bound, e1)),
                QuantifierExpression.create(Quantifier.EXISTS, bound, e1));

        Expression<Boolean> renamed = (Expression<Boolean>) q.accept(RenameBoundVarVisitor.getInstance(), data);
        System.out.println(renamed);
    }

    @Test(groups = {"normalization"})
    //works
    public void freeVarsTest() {
        Function<String, String> data = null;
        List<Variable<?>> bound = new ArrayList<>();
        bound.add(x);
        Expression q = ExpressionUtil.or(e1, ExpressionUtil.and(
                QuantifierExpression.create(Quantifier.FORALL, bound, e1)),
                ExpressionUtil.or(e2mod, e1));

        Expression<Boolean> renamed = (Expression<Boolean>) q.accept(RenameBoundVarVisitor.getInstance(), data);
        System.out.println(renamed);
    }

    @Test(groups = {"normalization"})
    //works
    public void nestedQuantifierTest() {
        Function<String, String> data = null;
        List<Variable<?>> bound1 = new ArrayList<>();
        bound1.add(x);
        List<Variable<?>> bound2 = new ArrayList<>();
        bound2.add(x);
        bound2.add(y);

        Expression<Boolean> allBound = QuantifierExpression.create(Quantifier.EXISTS, bound1,
                ExpressionUtil.or(e1, QuantifierExpression.create(Quantifier.FORALL, bound2, ExpressionUtil.and(e1, e2))));
        Expression<Boolean> renamed = (Expression<Boolean>) allBound.accept(RenameBoundVarVisitor.getInstance(), data);

        System.out.println(renamed);
    }

    @Test(groups = {"normalization"})
    //todo
    public void freeVarInOtherPathTest() {
        Function<String, String> data = null;
        List<Variable<?>> bound1 = new ArrayList<>();
        bound1.add(x);
        List<Variable<?>> bound2 = new ArrayList<>();
        bound2.add(x);
        bound2.add(y);

        Expression<Boolean> allBound = ExpressionUtil.and(e2mod, QuantifierExpression.create(Quantifier.EXISTS, bound1,
                ExpressionUtil.or(e1, QuantifierExpression.create(Quantifier.FORALL, bound2, ExpressionUtil.and(e1, e2)))));
        Expression<Boolean> renamed = (Expression<Boolean>) allBound.accept(RenameBoundVarVisitor.getInstance(), data);

        System.out.println(allBound);
        System.out.println(renamed);
    }
}
