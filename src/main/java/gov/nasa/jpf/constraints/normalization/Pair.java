package gov.nasa.jpf.constraints.normalization;

import gov.nasa.jpf.constraints.api.Expression;

//shall be used for tracking of polarity of an expression
class Pair {
    private Expression expression;
    private Object property;

    Pair(Expression expression, Object property) {
        this.expression = expression;
        this.property = property;
    }

    public Expression getExpression() {
        return expression;
    }

    public Object getProperty() {
        return property;
    }

}
