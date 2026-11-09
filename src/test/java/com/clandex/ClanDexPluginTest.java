package com.clandex;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

// Launches a developer RuneLite client with this plugin loaded (./gradlew run)
public class ClanDexPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ClanDexPlugin.class);
		RuneLite.main(args);
	}
}
