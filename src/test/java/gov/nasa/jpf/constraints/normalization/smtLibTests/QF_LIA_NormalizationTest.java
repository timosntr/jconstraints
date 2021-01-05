package gov.nasa.jpf.constraints.normalization.smtLibTests;

import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.smtlibUtility.SMTProblem;
import gov.nasa.jpf.constraints.smtlibUtility.parser.SMTLIBParser;
import gov.nasa.jpf.constraints.smtlibUtility.parser.SMTLIBParserException;
import gov.nasa.jpf.constraints.solvers.nativez3.NativeZ3Solver;
import org.smtlib.IParser;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.testng.Assert.assertEquals;

public class QF_LIA_NormalizationTest {

    @Test(groups = {"normalization"})
    //Exception with wrong type
    public void nnfTest1() throws IllegalArgumentException, SMTLIBParserException, IParser.ParserException, IOException, URISyntaxException {
        URL smtFile = QF_LIA_NormalizationTest.class.getClassLoader().getResource("QF_LIA/prp-5-21.smt2");
        SMTProblem problem = SMTLIBParser.parseSMTProgram(Files.readAllLines(Paths.get(smtFile.toURI()))
                .stream()
                .reduce("", (a, b) -> {
                    return b.startsWith(";") ? a : a + b;
                }));

        NativeZ3Solver z3 = new NativeZ3Solver();
        Valuation model = new Valuation();
        ConstraintSolver.Result jRes = z3.solve(problem.getAllAssertionsAsConjunction(), model);
        assertEquals(jRes, ConstraintSolver.Result.SAT);
    }

    //ToDo: Exception with BooleanExpression!

    @Test(groups = {"normalization"})
    public void equivalenceTest1(){

    }
}
