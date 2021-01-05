package gov.nasa.jpf.constraints.normalization.smtLibTests;

import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.expressions.LetExpression;
import gov.nasa.jpf.constraints.normalization.NormalizationUtil;
import gov.nasa.jpf.constraints.smtlibUtility.SMTProblem;
import gov.nasa.jpf.constraints.smtlibUtility.parser.SMTLIBParser;
import gov.nasa.jpf.constraints.smtlibUtility.parser.SMTLIBParserException;
import gov.nasa.jpf.constraints.solvers.ConstraintSolverFactory;
import org.smtlib.IParser;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static gov.nasa.jpf.constraints.smtlibUtility.parser.utility.ResourceParsingHelper.parseResourceFile;
import static org.testng.Assert.assertEquals;

public class QF_LIA_NormalizationTest {

    @Test(groups = {"normalization"})
    //UnsupportedOperationException (BooleanExpression.getChildren)
    //ToDo: how to run with jar?
    public void nnfTest1() throws IllegalArgumentException, SMTLIBParserException, IParser.ParserException, IOException, URISyntaxException {
        /*SMTProblem problem = parseResourceFile("QF_LIA/constraint-1635444.smt2");
        ConstraintSolverFactory factory = ConstraintSolverFactory.getRootFactory();
        ConstraintSolver constraintSolver = factory.createSolver("z3");
        Valuation val = new Valuation();

        List<Expression<Boolean>> childrenList = problem.assertions;
        //for(Expression c: childrenList) {
        //    Expression nnf = NormalizationUtil.pushNegation(c);
        //    int index = childrenList.indexOf(c);
        //    childrenList.remove(index);
        //    childrenList.add(index, nnf);
        //}
        ConstraintSolver.Result res = constraintSolver.solve(problem.getAllAssertionsAsConjunction(), val);
        System.out.println("RESULT: " + res);*/
    }

}
