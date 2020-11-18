package gov.nasa.jpf.constraints.normalization;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.Negation;
import gov.nasa.jpf.constraints.util.DuplicatingVisitor;
import javafx.util.Pair;

import java.util.List;

public class PolarityVisitor extends DuplicatingVisitor<List<Pair<Expression<?>, Integer>>> {

    private static final gov.nasa.jpf.constraints.normalization.PolarityVisitor INSTANCE = new gov.nasa.jpf.constraints.normalization.PolarityVisitor();

    public static gov.nasa.jpf.constraints.normalization.PolarityVisitor getInstance(){
        return INSTANCE;
    }

    @Override
    protected <E> Expression<?> defaultVisit(Expression<E> expression, List<Pair<Expression<?>, Integer>> data) {
        return super.defaultVisit(expression, data);
    }

    @Override
    public Expression<?> visit(Negation n, List<Pair<Expression<?>, Integer>> data) {
        return super.visit(n, data);
    }
}