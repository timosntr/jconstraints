package gov.nasa.jpf.constraints.normalization;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.Negation;
import gov.nasa.jpf.constraints.expressions.functions.Function;
import gov.nasa.jpf.constraints.expressions.functions.FunctionExpression;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.DuplicatingVisitor;

import java.util.List;

//ToDo: evtl entfernen
//ToDo: Boolean as flag for renaming?
public class RelabelingVisitor extends DuplicatingVisitor<Void> {

    private static final RelabelingVisitor INSTANCE = new RelabelingVisitor();

    public static RelabelingVisitor getInstance(){
        return INSTANCE;
    }

    @Override
    protected <E> Expression<?> defaultVisit(Expression<E> expression, Void data) {
        return super.defaultVisit(expression, data);
    }

    @Override
    public Expression<?> visit(Negation n, Void data) {
        return super.visit(n, data);
    }

    public Expression<?> createLabel(Expression expression){
        Function f = Function.create("f", BuiltinTypes.SINT32, BuiltinTypes.SINT32);
        //Expression predicate = FunctionExpression.create();
        return expression;
    }
}
