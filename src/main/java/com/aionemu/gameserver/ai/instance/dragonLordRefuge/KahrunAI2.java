package com.aionemu.gameserver.ai.instance.dragonLordRefuge;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import java.util.LinkedHashMap;
import java.util.Map;
/** Midified Ranastic (Encom)

/**
 * Dragon Lord Refuge 副本 NPC AI：Kahrun（@AIName "kahrun2"），继承 NpcAI2。
 * Dragon Lord Refuge instance NPC AI: Kahrun (@AIName "kahrun2"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("kahrun2")
public class KahrunAI2 extends NpcAI2
{
	private Map<Integer, VisibleObject> portal = new LinkedHashMap<Integer, VisibleObject>();
	
	@Override
	protected void handleDialogStart(Player player) {
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
	}
	
	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		int instanceId = getPosition().getInstanceId();
		if (dialogId == 10000) {
			PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 10));
			switch (getNpcId()) {
			    case 800429: //Kahrun (Reian Leader).
				    SpawnTemplate template = SpawnEngine.addNewSingleTimeSpawn(300520000, 730625, 503.219757f, 516.651733f, 242.604065f, (byte) 0); //Blood Red Jewel.
				    template.setEntityId(4);
				    portal.put(730625, SpawnEngine.spawnObject(template, instanceId));
				    AI2Actions.deleteOwner(KahrunAI2.this);
				break;
			}
		}
		return true;
	}
}
