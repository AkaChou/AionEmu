package com.aionemu.gameserver.ai.instance.fallenPoeta;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.world.WorldPosition;

import java.util.List;

/**
 * Fallen Poeta 副本 NPC AI：Brigade General Tahabata（@AIName "Brigade_General_Tahabata"），继承 AggressiveNpcAI2。
 * Fallen Poeta instance NPC AI: Brigade General Tahabata (@AIName "Brigade_General_Tahabata"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("Brigade_General_Tahabata")
public class Brigade_General_TahabataAI2 extends AggressiveNpcAI2
{
	private int phase = 0;
	
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		checkPercentage(getLifeStats().getHpPercentage());
	}
	
	private void checkPercentage(int hpPercentage) {
		if (hpPercentage == 90 && phase < 1) {
			phase = 1;
			fireTornado();
			startPhase();
		} if (hpPercentage == 50 && phase < 2) {
			phase = 2;
			fireTornado();
			startPhase();
		} if (hpPercentage == 20 && phase < 3) {
			phase = 3;
			fireTornado();
			startPhase();
		}
	}
	
	private void fireTornado() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				spawn(243961, 679.88f, 1068.88f, 497.88f, (byte) 0); //IDF6_LF1_Thor_SumStatue_PhyAtk.
			}
		}, 5000);
	}
	
	private void startPhase() {
		AI2Actions.useSkill(this, 20060); //Lava Eruption.
	}
	
	@Override
	protected void handleDied() {
		final WorldPosition p = getPosition();
		if (p != null) {
			deleteNpcs(p.getWorldMapInstance().getNpcs(243961)); //IDF6_LF1_Thor_SumStatue_PhyAtk.
		}
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
	
	private void deleteNpcs(List<Npc> npcs) {
		for (Npc npc: npcs) {
			if (npc != null) {
				npc.getController().onDelete();
			}
		}
	}
}
