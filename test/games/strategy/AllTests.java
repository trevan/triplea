/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package games.strategy;

import games.strategy.kingstable.KingsTableTest;
import games.strategy.triplea.TripleATest;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class AllTests extends TestCase
{
	public static Test suite()
	{
		final TestSuite suite = new TestSuite(AllTests.class.getSimpleName());
		// tests for the engine code
		suite.addTest(StrategyGameTest.suite());
		// tests for triplea code
		suite.addTest(TripleATest.suite());
		// tests for King's Table code
		suite.addTest(KingsTableTest.suite());
		return suite;
	}
}
