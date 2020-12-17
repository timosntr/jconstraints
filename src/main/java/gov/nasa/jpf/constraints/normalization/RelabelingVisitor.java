package gov.nasa.jpf.constraints.normalization;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.Negation;
import gov.nasa.jpf.constraints.util.DuplicatingVisitor;

import java.util.List;

public class RelabelingVisitor extends DuplicatingVisitor<List<Pair>> {

    private static final RelabelingVisitor INSTANCE = new RelabelingVisitor();

    public static RelabelingVisitor getInstance(){
        return INSTANCE;
    }

    @Override
    protected <E> Expression<?> defaultVisit(Expression<E> expression, List<Pair> data) {
        return super.defaultVisit(expression, data);
    }

    @Override
    public Expression<?> visit(Negation n, List<Pair> data) {
        return super.visit(n, data);
    }
}
