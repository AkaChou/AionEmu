package com.aionemu.gameserver.ai.instance.tiamatStronghold;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.GeneralNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.NpcType;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;

/**
 * Tiamat Stronghold 副本 NPC AI：Surama The Traitor（@AIName "suramathetraitor"），继承 GeneralNpcAI2。
 * Tiamat Stronghold instance NPC AI: Surama The Traitor (@AIName "suramathetraitor"), extends GeneralNpcAI2.
 *
 * @author Encom
 */
@AIName("suramathetraitor")
public class SuramaTheTraitorAI2 extends GeneralNpcAI2
{
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		moveToRaksha();
	}

	@Override
	protected void handleDied() {
		super.handleDied();
		GameFeatureServices.npcShoutsService().sendMsg(getOwner(), 390845, getOwner().getObjectId(), 0, 2000);
	}

	private void moveToRaksha() {
		setStateIfNot(AIState.WALKING);
		getOwner().setState(1);
		getMoveController().moveToPoint(651, 1319, 487);
		PacketSendUtility.broadcastPacket(getOwner(), new SM_EMOTION(getOwner(), EmotionType.START_EMOTE2, 0, getOwner().getObjectId()));
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
		    @Override
		    public void run() {
			    startDialog();
		    }
	    }, 10000);
	}

	private void startDialog() {
		final Npc laksyaka = getPosition().getWorldMapInstance().getNpc(219356); //Brigade General Laksyaka.
		GameFeatureServices.npcShoutsService().sendMsg(getOwner(), 390841, getOwner().getObjectId(), 0, 0);
		GameFeatureServices.npcShoutsService().sendMsg(getOwner(), 390842, getOwner().getObjectId(), 0, 3000);
		GameFeatureServices.npcShoutsService().sendMsg(laksyaka, 390843, laksyaka.getObjectId(), 0, 6000);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
		    @Override
		    public void run() {
			    WorldMapInstance instance = getPosition().getWorldMapInstance();
			    laksyaka.setTarget(getOwner());
			    GameEngineServices.skillEngine().getSkill(laksyaka, 20952, 60, getOwner()).useNoAnimationSkill();
			    laksyaka.setNpcType(NpcType.ATTACKABLE);
			    for (Player player: instance.getPlayersInside()) {
					if (MathUtil.isIn3dRange(player, laksyaka, 100)) {
						player.clearKnownlist();
						player.updateKnownlist();
					}
				}
		    }
	    }, 8000);
	}
}
