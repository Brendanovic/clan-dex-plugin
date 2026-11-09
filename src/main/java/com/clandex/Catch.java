package com.clandex;

import java.time.Instant;

/**
 * One caught Creature, as the clan's bot records it. Every field except the entry and shiny flag can
 * be missing (older bot versions recorded less), so the UI shows only what is there. Fields the bot
 * adds later are simply not read here.
 */
public final class Catch
{
	private final String id;
	private final Integer number;       // the player's number for it within its species (1 = their first); null if unknown
	private final int index;            // entry index, 0 = #001
	private final boolean shiny;
	private final String gender;        // "male", "female", or null (genderless or unknown)
	private final String nature;        // e.g. "Adamant"; null if unknown
	private final String ability;       // e.g. "Static"; null if unknown
	private final boolean hiddenAbility; // true when the ability is the species' rare hidden one
	private final Instant caughtAt;     // null if unknown
	private final String caughtWith;    // item id of the ball, or null
	private final Integer throwCount;   // balls thrown, misses included; null if unknown
	private final Integer level;        // null if unknown
	private final int[] ivs;            // attack, defense, speed, special (0-15); null if unknown

	Catch(String id, Integer number, int index, boolean shiny, String gender, String nature, String ability, boolean hiddenAbility,
		Instant caughtAt, String caughtWith, Integer throwCount, Integer level, int[] ivs)
	{
		this.id = id;
		this.number = number;
		this.index = index;
		this.shiny = shiny;
		this.gender = gender;
		this.nature = nature;
		this.ability = ability;
		this.hiddenAbility = hiddenAbility;
		this.caughtAt = caughtAt;
		this.caughtWith = caughtWith;
		this.throwCount = throwCount;
		this.level = level;
		this.ivs = ivs == null ? null : ivs.clone();
	}

	public String getId()
	{
		return id;
	}

	/** The player's number for this catch within its species, e.g. 2 for their second Moroka; null if unknown. */
	public Integer getNumber()
	{
		return number;
	}

	/** "male", "female", or null for genderless (or unknown). */
	public String getGender()
	{
		return gender;
	}

	public String getNature()
	{
		return nature;
	}

	public String getAbility()
	{
		return ability;
	}

	public boolean isHiddenAbility()
	{
		return hiddenAbility;
	}

	public int getIndex()
	{
		return index;
	}

	public boolean isShiny()
	{
		return shiny;
	}

	public Instant getCaughtAt()
	{
		return caughtAt;
	}

	public String getCaughtWith()
	{
		return caughtWith;
	}

	public Integer getThrowCount()
	{
		return throwCount;
	}

	public Integer getLevel()
	{
		return level;
	}

	/** Attack, defense, speed and special, 0-15 each; null if unknown. */
	public int[] getIvs()
	{
		return ivs == null ? null : ivs.clone();
	}
}
