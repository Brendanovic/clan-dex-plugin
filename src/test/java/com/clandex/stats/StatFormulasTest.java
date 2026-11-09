package com.clandex.stats;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertSame;
import org.junit.Test;

/** The Gen 1 formula against worked examples, and how formulas are picked by name. */
public class StatFormulasTest
{
	private static final int[] EXAMPLE_BASE_STATS = {45, 49, 49, 45, 65};

	@Test
	public void gen1ExampleAtLevel14()
	{
		// IVs atk 2, def 5, spe 5, spc 9 -> HP IV 0b0111 = 7
		// HP = 104 * 14 / 100 + 24 = 38; Atk = 102 * 14 / 100 + 5 = 19; Def = 108 * 14 / 100 + 5 = 20
		// Spe = 100 * 14 / 100 + 5 = 19; Spc = 148 * 14 / 100 + 5 = 25
		assertArrayEquals(new int[]{38, 19, 20, 19, 25}, new Gen1Formula().compute(EXAMPLE_BASE_STATS, new int[]{2, 5, 5, 9}, 14));
	}

	@Test
	public void gen1LevelOne()
	{
		// HP = 90 / 100 + 1 + 10 = 11; Atk = 98 / 100 + 5 = 5; Spc = 130 / 100 + 5 = 6
		assertArrayEquals(new int[]{11, 5, 5, 5, 6}, new Gen1Formula().compute(EXAMPLE_BASE_STATS, new int[]{0, 0, 0, 0}, 1));
	}

	@Test
	public void picksByNameAndFallsBackToDefault()
	{
		assertSame(StatFormulas.DEFAULT, StatFormulas.forName("gen1"));
		assertSame(StatFormulas.DEFAULT, StatFormulas.forName(null));
		assertSame(StatFormulas.DEFAULT, StatFormulas.forName("some-future-formula"));
	}
}
