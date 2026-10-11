package com.aionemu.gameserver.ai.rvr;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Npc;

import java.util.List;

/**
 * RvR 相关 NPC AI：Dynamic Iluma Monster（@AIName "dynamic_iluma_monster"），继承 AggressiveNpcAI2。
 * RvR-related NPC AI: Dynamic Iluma Monster (@AIName "dynamic_iluma_monster"), extends AggressiveNpcAI2.
 * @author Encom
 */
@AIName("dynamic_iluma_monster")
public class Dynamic_Iluma_MonsterAI2 extends AggressiveNpcAI2 {


	@Override
	protected void handleDied() {
		switch (Rnd.get(1, 24)) {
			case 1:
				spawnLF6EventDoor();
			break;
			case 2, 3, 4, 5, 6, 7, 24, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23:
			break;
		}
		super.handleDied();
	}

	private void spawnLF6EventDoor() {
		switch (Rnd.get(1, 4)) {
			case 1:
				spawn(241053, 804.516f, 1842.256f, 305.86325f, (byte) 0); //传送门。 / Portal.
			    spawn(240887, 804.516f, 1842.256f, 305.86325f, (byte) 0); //Archon Warrior.
				spawn(241053, 1865.9077f, 2071.3066f, 344.01154f, (byte) 0); //传送门。 / Portal.
				spawn(240888, 1865.9077f, 2071.3066f, 344.01154f, (byte) 0); //Archon Mage.
				spawn(241053, 2436.7002f, 1326.0476f, 224.875f, (byte) 0); //传送门。 / Portal.
				spawn(240889, 2436.7002f, 1326.0476f, 224.875f, (byte) 0); //Archon Scout.
				spawn(241053, 1358.5513f, 335.7875f, 348.35382f, (byte) 0); //传送门。 / Portal.
				spawn(240890, 1358.5513f, 335.7875f, 348.35382f, (byte) 0); //Archon Marksman.
			break;
			case 2:
				spawn(241053, 804.516f, 1842.256f, 305.86325f, (byte) 0); //传送门。 / Portal.
			    spawn(240888, 804.516f, 1842.256f, 305.86325f, (byte) 0); //Archon Mage.
				spawn(241053, 1865.9077f, 2071.3066f, 344.01154f, (byte) 0); //传送门。 / Portal.
				spawn(240889, 1865.9077f, 2071.3066f, 344.01154f, (byte) 0); //Archon Scout.
				spawn(241053, 2436.7002f, 1326.0476f, 224.875f, (byte) 0); //传送门。 / Portal.
				spawn(240890, 2436.7002f, 1326.0476f, 224.875f, (byte) 0); //Archon Marksman.
				spawn(241053, 1358.5513f, 335.7875f, 348.35382f, (byte) 0); //传送门。 / Portal.
				spawn(240887, 1358.5513f, 335.7875f, 348.35382f, (byte) 0); //Archon Warrior.
			break;
			case 3:
				spawn(241053, 804.516f, 1842.256f, 305.86325f, (byte) 0); //传送门。 / Portal.
			    spawn(240889, 804.516f, 1842.256f, 305.86325f, (byte) 0); //Archon Scout.
				spawn(241053, 1865.9077f, 2071.3066f, 344.01154f, (byte) 0); //传送门。 / Portal.
				spawn(240890, 1865.9077f, 2071.3066f, 344.01154f, (byte) 0); //Archon Marksman.
				spawn(241053, 2436.7002f, 1326.0476f, 224.875f, (byte) 0); //传送门。 / Portal.
				spawn(240887, 2436.7002f, 1326.0476f, 224.875f, (byte) 0); //Archon Warrior.
				spawn(241053, 1358.5513f, 335.7875f, 348.35382f, (byte) 0); //传送门。 / Portal.
				spawn(240888, 1358.5513f, 335.7875f, 348.35382f, (byte) 0); //Archon Mage.
			break;
			case 4:
				spawn(241053, 804.516f, 1842.256f, 305.86325f, (byte) 0); //传送门。 / Portal.
			    spawn(240890, 804.516f, 1842.256f, 305.86325f, (byte) 0); //Archon Marksman.
				spawn(241053, 1865.9077f, 2071.3066f, 344.01154f, (byte) 0); //传送门。 / Portal.
				spawn(240887, 1865.9077f, 2071.3066f, 344.01154f, (byte) 0); //Archon Warrior.
				spawn(241053, 2436.7002f, 1326.0476f, 224.875f, (byte) 0); //传送门。 / Portal.
				spawn(240888, 2436.7002f, 1326.0476f, 224.875f, (byte) 0); //Archon Mage.
				spawn(241053, 1358.5513f, 335.7875f, 348.35382f, (byte) 0); //传送门。 / Portal.
				spawn(240889, 1358.5513f, 335.7875f, 348.35382f, (byte) 0); //Archon Scout.
			break;
		}
		//传送门。 / Portal.
		GameThreadPoolServices.threadPoolManager().schedule(this::despawnNpc, 60000);
	}

	private void despawnNpc() {
		if (getPosition().getWorldMapInstance().getNpcs(241053) != null) {
			List<Npc> npcs = getPosition().getWorldMapInstance().getNpcs(241053);
			for (Npc npc: npcs) {
				npc.getController().onDelete();
			}
		}
	}
}
