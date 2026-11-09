package com.clandex.stats;

/**
 * Turns a caught Creature's numbers into its stats. One implementation per rule set; the clan's bot
 * names the one to use in its file ("statFormula"), and {@link StatFormulas} looks it up.
 *
 * <p>To add a formula: write a class implementing this, give it a name, and register it in
 * {@link StatFormulas}. Then the bot can switch to it by publishing that name.
 */
public interface StatFormula
{
	/** Order of every stats array: HP, attack, defense, speed, special. */
	String[] STAT_NAMES = {"HP", "Atk", "Def", "Spe", "Spc"};

	/** The name the bot uses for this formula, e.g. "gen1". */
	String name();

	/**
	 * @param base  species base stats {hp, atk, def, spe, spc}
	 * @param ivs   the catch's IVs {atk, def, spe, spc}
	 * @param level its level
	 * @return {hp, atk, def, spe, spc}
	 */
	int[] compute(int[] base, int[] ivs, int level);
}
