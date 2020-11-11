package gov.nasa.jpf.constraints.smtlibUtility.export;

import gov.nasa.jpf.constraints.api.SolverContext;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.IfThenElse;
import gov.nasa.jpf.constraints.expressions.Negation;
import gov.nasa.jpf.constraints.expressions.PropositionalCompound;
import gov.nasa.jpf.constraints.smtlibUtility.solver.SMTLibExportWrapper;
import gov.nasa.jpf.constraints.solvers.dontknow.DontKnowSolver;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static gov.nasa.jpf.constraints.expressions.LogicalOperator.AND;
import static gov.nasa.jpf.constraints.expressions.LogicalOperator.EQUIV;
import static gov.nasa.jpf.constraints.expressions.LogicalOperator.IMPLY;
import static gov.nasa.jpf.constraints.expressions.LogicalOperator.OR;
import static gov.nasa.jpf.constraints.expressions.LogicalOperator.XOR;
import static org.testng.Assert.assertEquals;

public class LogicalExpressionTest {
	Variable var1, var2;

	SolverContext se;
	ByteArrayOutputStream baos;
	PrintStream ps;

	@BeforeMethod
	public void initialize() {
		var1 = Variable.create(BuiltinTypes.BOOL, "x");
		var2 = Variable.create(BuiltinTypes.BOOL, "y");
		baos = new ByteArrayOutputStream();
		ps = new PrintStream(baos);
		se = (new SMTLibExportWrapper(new DontKnowSolver(), ps)).createContext();
	}

	@Test
	public void PropositionalCompoundAndTest() {
		String expected = "(declare-const x Bool)\n" + "(declare-const y Bool)\n" + "(assert (and x y))\n";
		PropositionalCompound expr = PropositionalCompound.create(var1, AND, var2);
		se.add(expr);
		assertEquals(baos.toString(), expected);
	}

	@Test
	public void PropositionalCompoundOrTest() {
		String expected = "(declare-const x Bool)\n" + "(declare-const y Bool)\n" + "(assert (or x y))\n";
		PropositionalCompound expr = PropositionalCompound.create(var1, OR, var2);
		se.add(expr);
		assertEquals(baos.toString(), expected);
	}

	@Test
	public void PropositionalCompoundImplyTest() {
		String expected = "(declare-const x Bool)\n" + "(declare-const y Bool)\n" + "(assert (=> x y))\n";
		PropositionalCompound expr = PropositionalCompound.create(var1, IMPLY, var2);
		se.add(expr);
		assertEquals(baos.toString(), expected);
	}

	@Test
	public void PropositionalCompoundEquivalentTest() {
		String expected = "(declare-const x Bool)\n" + "(declare-const y Bool)\n" + "(assert (= x y))\n";
		PropositionalCompound expr = PropositionalCompound.create(var1, EQUIV, var2);
		se.add(expr);
		assertEquals(baos.toString(), expected);
	}

	@Test
	public void PropositionalXORAndTest() {
		String expected = "(declare-const x Bool)\n" + "(declare-const y Bool)\n" + "(assert (xor x y))\n";
		PropositionalCompound expr = PropositionalCompound.create(var1, XOR, var2);
		se.add(expr);
		assertEquals(baos.toString(), expected);
	}

	@Test
	public void NegationTest() {
		String expected = "(declare-const x Bool)\n" + "(assert (not x))\n";
		Negation expr = Negation.create(var1);
		se.add(expr);
		assertEquals(baos.toString(), expected);
	}

	@Test
	public void ifThenElseTest() {
		String expected =
				"(declare-const x Bool)\n" + "(declare-const y Bool)\n" + "(declare-const z Bool)\n" + "(assert (ite x y z))" +
				"\n";

		Variable var1 = Variable.create(BuiltinTypes.BOOL, "x");
		Variable var2 = Variable.create(BuiltinTypes.BOOL, "y");
		Variable var3 = Variable.create(BuiltinTypes.BOOL, "z");
		IfThenElse expr = IfThenElse.create(var1, var2, var3);
		se.add(expr);
		assertEquals(baos.toString(), expected);

	}
}