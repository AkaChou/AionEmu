package com.aionemu.gameserver.ai.worlds.iluma;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.world.WorldPosition;

import java.util.List;

/**
 * Iluma 区域 NPC AI：Tunes Of Splendor Scout（@AIName "tunes_of_splendor_scout"），继承 NpcAI2。
 * Iluma zone NPC AI: Tunes Of Splendor Scout (@AIName "tunes_of_splendor_scout"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("tunes_of_splendor_scout")
public class Tunes_Of_Splendor_ScoutAI2 extends NpcAI2
{
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		startLifeTask();
		startRiftEscapeTask();
		startRiftEscapeClosed();
		// 我还活着吗？ / Am I still alive ?
		sendMsg(1501533, getObjectId(), false, 5000);
		// 我还以为一切都完了。 / And I thought all was lost.
		sendMsg(1501532, getObjectId(), false, 8000);
	}
	
	private void startRiftEscapeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				spawnRiftEscape(701132);
			}
		}, 2500);
	}
	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				AI2Actions.deleteOwner(Tunes_Of_Splendor_ScoutAI2.this);
			}
		}, 10000);
	}
	private void startRiftEscapeClosed() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				despawnNpc(701132);
			}
		}, 12500);
	}
	
	private void spawnRiftEscape(int npcId) {
		rndSpawnInRange(npcId, Rnd.get(1, 3));
	}
	
	private Npc rndSpawnInRange(int npcId, float distance) {
		float direction = Rnd.get(0, 199) / 100f;
		float x1 = (float) (Math.cos(Math.PI * direction) * distance);
		float y1 = (float) (Math.sin(Math.PI * direction) * distance);
		WorldPosition p = getPosition();
		return (Npc) spawn(npcId, p.getX() + x1, p.getY() + y1, p.getZ(), (byte) 0);
	}
	
	private void despawnNpc(int npcId) {
		if (getPosition().getWorldMapInstance().getNpcs(npcId) != null) {
			List<Npc> npcs = getPosition().getWorldMapInstance().getNpcs(npcId);
			for (Npc npc: npcs) {
				npc.getController().onDelete();
			}
		}
	}
	
	private void sendMsg(int msg, int Obj, boolean isShout, int time) {
		GameFeatureServices.npcShoutsService().sendMsg(getPosition().getWorldMapInstance(), msg, Obj, isShout, 0, time);
	}
}
