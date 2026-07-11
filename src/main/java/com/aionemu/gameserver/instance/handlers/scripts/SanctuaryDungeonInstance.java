package com.aionemu.gameserver.instance.handlers.scripts;

import java.util.*;

import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 圣所地下城副本事件处理器。
 * Instance event handler for Sanctuary Dungeon.
 *
 * @author Encom
 */

@InstanceID(301580000)
public class SanctuaryDungeonInstance extends GeneralInstanceHandler
{
	/** 刷怪种族 / spawn race */
	private Race spawnRace;
	
	/**
	 * 玩家进入副本时处理。
	 * Handle a player entering the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onEnterInstance(Player player) {
		if (spawnRace == null) {
			spawnRace = player.getRace();
			spawnSanctuaryRace();
		}
	}
	
	private void spawnSanctuaryRace() {
		// NPC / Npc
		final int Feregran_Weatha = spawnRace == Race.ASMODIANS ? 806080 : 806076;
		spawn(Feregran_Weatha, 432.54724f, 479.6076f, 99.59915f, (byte) 31);
		//Tp
		final int Dungeon_Exit = spawnRace == Race.ASMODIANS ? 806190 : 806189;
		spawn(Dungeon_Exit, 432.7019f, 475.63489f, 99.471016f, (byte) 0, 20);
	}
}