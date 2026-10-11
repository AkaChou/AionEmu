package com.aionemu.gameserver.ai.rvr;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Npc;

import java.util.List;

/**
 * RvR 相关 NPC AI：Dynamic Norsvold Monster（@AIName "dynamic_norsvold_monster"），继承 AggressiveNpcAI2。
 * RvR-related NPC AI: Dynamic Norsvold Monster (@AIName "dynamic_norsvold_monster"), extends AggressiveNpcAI2.
 * @author Encom
 */
@AIName("dynamic_norsvold_monster")
public class Dynamic_Norsvold_MonsterAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		switch (Rnd.get(1, 24)) {
			case 1:
				spawnDF6EventDoor();
			break;
			case 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24:
			break;
		}
		super.handleDied();
	}

	private void spawnDF6EventDoor() {
		switch (Rnd.get(1, 4)) {
			case 1:
			    spawn(241054, 1026.9843f, 1356.3972f, 284.44946f, (byte) 0); //传送门。 / Portal.
				spawn(240971, 1026.9843f, 1356.3972f, 284.44946f, (byte) 0); //Guardian Warrior.
				spawn(241054, 1477.9161f, 1006.49695f, 231.65082f, (byte) 0); //传送门。 / Portal.
				spawn(240972, 1477.9161f, 1006.49695f, 231.65082f, (byte) 0); //Guardian Mage.
				spawn(241054, 2227.855f, 1868.7146f, 235.28452f, (byte) 0); //传送门。 / Portal.
				spawn(240973, 2227.855f, 1868.7146f, 235.28452f, (byte) 0); //Guardian Scout.
				spawn(241054, 1133.7673f, 2570.3909f, 235.72313f, (byte) 0); //传送门。 / Portal.
				spawn(240974, 1133.7673f, 2570.3909f, 235.72313f, (byte) 0); //Guardian Marksman.
			break;
			case 2:
				spawn(241054, 1026.9843f, 1356.3972f, 284.44946f, (byte) 0); //传送门。 / Portal.
				spawn(240972, 1026.9843f, 1356.3972f, 284.44946f, (byte) 0); //Guardian Mage.
				spawn(241054, 1477.9161f, 1006.49695f, 231.65082f, (byte) 0); //传送门。 / Portal.
				spawn(240973, 1477.9161f, 1006.49695f, 231.65082f, (byte) 0); //Guardian Scout.
				spawn(241054, 2227.855f, 1868.7146f, 235.28452f, (byte) 0); //传送门。 / Portal.
				spawn(240974, 2227.855f, 1868.7146f, 235.28452f, (byte) 0); //Guardian Marksman.
				spawn(241054, 1133.7673f, 2570.3909f, 235.72313f, (byte) 0); //传送门。 / Portal.
				spawn(240971, 1133.7673f, 2570.3909f, 235.72313f, (byte) 0); //Guardian Warrior.
			break;
			case 3:
				spawn(241054, 1026.9843f, 1356.3972f, 284.44946f, (byte) 0); //传送门。 / Portal.
				spawn(240973, 1026.9843f, 1356.3972f, 284.44946f, (byte) 0); //Guardian Scout.
				spawn(241054, 1477.9161f, 1006.49695f, 231.65082f, (byte) 0); //传送门。 / Portal.
				spawn(240974, 1477.9161f, 1006.49695f, 231.65082f, (byte) 0); //Guardian Marksman.
				spawn(241054, 2227.855f, 1868.7146f, 235.28452f, (byte) 0); //传送门。 / Portal.
				spawn(240971, 2227.855f, 1868.7146f, 235.28452f, (byte) 0); //Guardian Warrior.
				spawn(241054, 1133.7673f, 2570.3909f, 235.72313f, (byte) 0); //传送门。 / Portal.
				spawn(240972, 1133.7673f, 2570.3909f, 235.72313f, (byte) 0); //Guardian Mage.
			break;
			case 4:
				spawn(241054, 1026.9843f, 1356.3972f, 284.44946f, (byte) 0); //传送门。 / Portal.
				spawn(240974, 1026.9843f, 1356.3972f, 284.44946f, (byte) 0); //Guardian Marksman.
				spawn(241054, 1477.9161f, 1006.49695f, 231.65082f, (byte) 0); //传送门。 / Portal.
				spawn(240971, 1477.9161f, 1006.49695f, 231.65082f, (byte) 0); //Guardian Warrior.
				spawn(241054, 2227.855f, 1868.7146f, 235.28452f, (byte) 0); //传送门。 / Portal.
				spawn(240972, 2227.855f, 1868.7146f, 235.28452f, (byte) 0); //Guardian Mage.
				spawn(241054, 1133.7673f, 2570.3909f, 235.72313f, (byte) 0); //传送门。 / Portal.
				spawn(240973, 1133.7673f, 2570.3909f, 235.72313f, (byte) 0); //Guardian Scout.
			break;
		}
		//传送门。 / Portal.
		GameThreadPoolServices.threadPoolManager().schedule(this::despawnNpc, 60000);
    }

	private void despawnNpc() {
		if (getPosition().getWorldMapInstance().getNpcs(241054) != null) {
			List<Npc> npcs = getPosition().getWorldMapInstance().getNpcs(241054);
			for (Npc npc: npcs) {
				npc.getController().onDelete();
			}
		}
	}
}
