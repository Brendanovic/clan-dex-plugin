package com.clandex.ui;

/** Lets a catch line show and change whether that catch is in the party. */
interface PartyControl
{
	boolean contains(String catchId);

	/** Adds or removes the catch. Returns false if it couldn't be added because the party is full. */
	boolean toggle(String catchId);
}
