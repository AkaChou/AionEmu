package com.aionemu.gameserver.ai.instance.tiamatStronghold;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * Tiamat Stronghold 副本 NPC AI：Kahrun（@AIName "kahrun"），继承 NpcAI2。
 * Tiamat Stronghold instance NPC AI: Kahrun (@AIName "kahrun"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("kahrun")
public class KahrunAI2 extends NpcAI2
{
    @Override
    protected void handleDialogStart(Player player) {
        PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
    }
	
	@Override
    	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		if (dialogId == 10000) {
            PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 10));
			startProtectorateEvent();
			AI2Actions.deleteOwner(this);
        }
        return true;
    }
	
	private void startProtectorateEvent() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				Npc fileLadderCGF = getPosition().getWorldMapInstance().getNpc(730612);
				Npc aionFXPostGlow = getPosition().getWorldMapInstance().getNpc(730694);
				Npc kharunReianLeader = (Npc)spawn(800335, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) 60);
			    kharunReianLeader.setTarget(aionFXPostGlow);
			    // 退后。我来处理这屏障。 / Stand back. I will take care of this barrier.
				GameFeatureServices.npcShoutsService().sendMsg(kharunReianLeader, 1500596, kharunReianLeader.getObjectId(), 0, 1000);
				GameEngineServices.skillEngine().getSkill(kharunReianLeader, 20943, 60, aionFXPostGlow).useNoAnimationSkill();
			    fileLadderCGF.getController().onDelete();
			    aionFXPostGlow.getController().onDelete();
			}
	    }, 3000);
    }
}
