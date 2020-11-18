package gov.nasa.jpf.constraints.expressions;

public enum StringBooleanOperator implements ExpressionOperator {
	CONTAINS("str.contains"),
	EQUALS("="),
	NOTEQUALS("!="),
	PREFIXOF("str.prefixof"),
	SUFFIXOF("str.suffixof");
	
	private final String str;
	
	StringBooleanOperator(String str) {
		this.str=str;
	}
	
	@Override
	  public String toString() {
	    return str;
	  }
	  
	  public static StringBooleanOperator fromString(String str){
	    switch(str){
	      case "str.contains": return CONTAINS; 
	      case "=": return EQUALS;
	      case "!=": return NOTEQUALS;
	      case "str.prefixof": return PREFIXOF;
	      case "str.suffixof": return SUFFIXOF;
	      default: return null;
	    }
	  }
}
