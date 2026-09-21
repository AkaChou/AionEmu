package com.aionemu.gameserver.ai.event.conquestOffering;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.HTMLService;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.world.WorldPosition;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.List;

/**
 * Conquest Offering 活动 NPC AI：Conquest Inggison Boss（@AIName "conquest_inggison"），继承 AggressiveNpcAI2。
 * Conquest Offering event NPC AI: Conquest Inggison Boss (@AIName "conquest_inggison"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("conquest_inggison")
public class Conquest_Inggison_BossAI2 extends AggressiveNpcAI2
{
	@Override
    protected void handleSpawned() {
        super.handleSpawned();
		boostDefense();
    }

	@Override
	protected void handleDied() {
		final WorldPosition p = getPosition();
		if (p != null) {
			sendGuide();
		} switch (Rnd.get(1, 2)) {
			case 1:
			    spawnSecretPortal();
			break;
			case 2:
			break;
		}
		super.handleDied();
	}

    private void spawnSecretPortal() {
		GameThreadPoolServices.threadPoolManager().schedule(() -> {
			spawn(833018, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) 0); //Secret Portal.
		}, 15000);
		GameThreadPoolServices.threadPoolManager().schedule(() -> {
			despawnNpc(833018); //Secret Portal.
		}, 300000); //5 分钟。 / 5 Minutes.
    }



	private void boostDefense() {
		GameEngineServices.skillEngine().getSkill(getOwner(), 21923, 1, getOwner()).useNoAnimationSkill(); //Boost Defense.
	}

	private void sendGuide() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(player -> {
			if (MathUtil.isIn3dRange(player, getOwner(), 15)) {
				HTMLService.sendGuideHtml(player, "Conquest_Offering");
			}
		});
	}

	private void despawnNpc(int npcId) {
		if (getPosition().getWorldMapInstance().getNpcs(npcId) != null) {
			List<Npc> npcs = getPosition().getWorldMapInstance().getNpcs(npcId);
			for (Npc npc: npcs) {
				npc.getController().onDelete();
			}
		}
	}
}
