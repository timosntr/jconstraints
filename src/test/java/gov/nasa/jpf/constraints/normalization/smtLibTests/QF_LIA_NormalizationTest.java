/**
 * Copyright 2020, TU Dortmund, Malte Mues (@mmuesly)
 *
 * <p>This is a derived version of JConstraints original located at:
 * https://github.com/psycopaths/jconstraints
 *
 * <p>Until commit: https://github.com/tudo-aqua/jconstraints/commit/876e377 the original license
 * is: Copyright (C) 2015, United States Government, as represented by the Administrator of the
 * National Aeronautics and Space Administration. All rights reserved.
 *
 * <p>The PSYCO: A Predicate-based Symbolic Compositional Reasoning environment platform is licensed
 * under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * <p>Modifications and new contributions are Copyright by TU Dortmund 2020, Malte Mues under Apache
 * 2.0 in alignment with the original repository license.
 */
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
