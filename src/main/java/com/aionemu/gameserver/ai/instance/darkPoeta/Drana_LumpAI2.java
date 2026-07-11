package com.aionemu.gameserver.ai.instance.darkPoeta;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.world.WorldMapInstance;

import java.util.concurrent.Future;

/**
 * Dark Poeta 副本 NPC AI：Drana Lump（@AIName "drana_lump"），继承 AggressiveNpcAI2。
 * Dark Poeta instance NPC AI: Drana Lump (@AIName "drana_lump"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("drana_lump")
public class Drana_LumpAI2 extends AggressiveNpcAI2
{
	private Future<?> dranaBreakTask;
	
	@Override
	public void think() {
	}
	
	@Override
	protected void handleSpawned() {
		startDranaBreak();
		super.handleSpawned();
	}
	
	private void startDranaBreak() {
		final Npc spallerEchtra = getPosition().getWorldMapInstance().getNpc(214880); //Spaller Echtra.
		final Npc spallerRakanatra = getPosition().getWorldMapInstance().getNpc(215388); //Spaller Rakanatra.
		final Npc spallerDhatra = getPosition().getWorldMapInstance().getNpc(215389); //Spaller Dhatra.
		WorldMapInstance instance = getPosition().getWorldMapInstance();
		for (Player player: instance.getPlayersInside()) {
			if (MathUtil.isIn3dRange(player, spallerEchtra, 8)) {
				dranaBreak();
			} if (MathUtil.isIn3dRange(player, spallerRakanatra, 8)) {
				dranaBreak();
			} if (MathUtil.isIn3dRange(player, spallerDhatra, 8)) {
				dranaBreak();
			}
		}
	}
	
	private void dranaBreak() {
		dranaBreakTask = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				AI2Actions.targetCreature(Drana_LumpAI2.this, getPosition().getWorldMapInstance().getNpc(214880)); //Spaller Echtra.
				AI2Actions.targetCreature(Drana_LumpAI2.this, getPosition().getWorldMapInstance().getNpc(215388)); //Spaller Rakanatra.
				AI2Actions.targetCreature(Drana_LumpAI2.this, getPosition().getWorldMapInstance().getNpc(215389)); //Spaller Dhatra.
				AI2Actions.useSkill(Drana_LumpAI2.this, 18536); //Drana Break.
			}
		}, 1000, 6000);
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
