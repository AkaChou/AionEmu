package com.aionemu.gameserver.ai.instance.bastionOfSouls;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.GeneralNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.AbstractAI;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.NpcType;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;

/**
 * Bastion Of Souls 副本 NPC AI：Detachment Captain Bastikan（@AIName "Vatiskhan_Terrarium"），继承 GeneralNpcAI2。
 * Bastion Of Souls instance NPC AI: Detachment Captain Bastikan (@AIName "Vatiskhan_Terrarium"), extends GeneralNpcAI2.
 *
 * @author Encom
 */
@AIName("Vatiskhan_Terrarium")
public class Detachment_Captain_BastikanAI2 extends GeneralNpcAI2
{
	private boolean isInstanceDestroyed;
	
	@Override
	protected void handleDialogStart(Player player) {
		if (player.getLevel() >= 66) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
		}
	}
	
	@Override
    public boolean onDialogSelect(final Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 10000) {
			startInvulnerable();
			startBastionOfSoulsWave();
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
		return true;
	}
	
	private void startInvulnerable() {
		final Npc detachmentCaptainBastikan = getPosition().getWorldMapInstance().getNpc(806703);
		detachmentCaptainBastikan.setTarget(getOwner());
		detachmentCaptainBastikan.setNpcType(NpcType.INVULNERABLE);
		WorldMapInstance instance = getPosition().getWorldMapInstance();
		for (Player player: instance.getPlayersInside()) {
			if (MathUtil.isIn3dRange(player, detachmentCaptainBastikan, 50)) {
				player.getEffectController().updatePlayerEffectIcons();
				player.clearKnownlist();
				player.updateKnownlist();
			}
		}
	}
	
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		Npc owner = getOwner();
		owner.getLifeStats().setCurrentHpPercent(5);
	}
	
	private void startBastionOfSoulsWave() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				attackBastionOfSouls((Npc)spawn(246520, 260.62317f, 753.64874f, 421.74033f, (byte) 67), 238.8924f, 748.75555f, 421.254f, false);
				attackBastionOfSouls((Npc)spawn(246521, 229.63022f, 773.9174f, 421.83142f, (byte) 94), 238.8924f, 748.75555f, 421.254f, false);
				attackBastionOfSouls((Npc)spawn(246522, 220.49199f, 743.5425f, 421.625f, (byte) 14), 238.8924f, 748.75555f, 421.254f, false);
				attackBastionOfSouls((Npc)spawn(246523, 244.47363f, 733.99384f, 421.38748f, (byte) 33), 238.8924f, 748.75555f, 421.254f, false);
			}
		}, 1000);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				attackBastionOfSouls((Npc)spawn(246520, 260.62317f, 753.64874f, 421.74033f, (byte) 67), 238.8924f, 748.75555f, 421.254f, false);
				attackBastionOfSouls((Npc)spawn(246521, 229.63022f, 773.9174f, 421.83142f, (byte) 94), 238.8924f, 748.75555f, 421.254f, false);
				attackBastionOfSouls((Npc)spawn(246522, 220.49199f, 743.5425f, 421.625f, (byte) 14), 238.8924f, 748.75555f, 421.254f, false);
				attackBastionOfSouls((Npc)spawn(246523, 244.47363f, 733.99384f, 421.38748f, (byte) 33), 238.8924f, 748.75555f, 421.254f, false);
			}
		}, 30000);
	}
	
	private void attackBastionOfSouls(final Npc npc, float x, float y, float z, boolean despawn) {
		((AbstractAI) npc.getAi2()).setStateIfNot(AIState.WALKING);
		npc.setState(1);
		npc.getMoveController().moveToPoint(x, y, z);
		PacketSendUtility.broadcastPacket(npc, new SM_EMOTION(npc, EmotionType.START_EMOTE2, 0, npc.getObjectId()));
	}
	
	public void onInstanceDestroy() {
		isInstanceDestroyed = true;
	}
}
