package com.clandex.stats;

/**
 * The Gen 1 (Red/Blue) formula, without stat experience (there are no battles yet).
 *
 * <pre>
 *   HP    = floor((base + iv) * 2 * level / 100) + level + 10
 *   other = floor((base + iv) * 2 * level / 100) + 5
 * </pre>
 * As in Gen 1, the HP IV isn't stored: it is built from the lowest bit of each other IV.
 */
public final class Gen1Formula implements StatFormula
{
	@Override
	public String name()
	{
		return "gen1";
	}

	@Override
	public int[] compute(int[] base, int[] ivs, int level)
	{
		int hpIv = ((ivs[0] & 1) << 3) | ((ivs[1] & 1) << 2) | ((ivs[2] & 1) << 1) | (ivs[3] & 1);
		int[] out = new int[5];
		out[0] = (base[0] + hpIv) * 2 * level / 100 + level + 10;
		for (int i = 1; i < 5; i++)
		{
			out[i] = (base[i] + ivs[i - 1]) * 2 * level / 100 + 5;
		}
		return out;
	}
}
