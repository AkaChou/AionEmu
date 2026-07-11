package com.aionemu.gameserver.ai.instance.anguishedDragonLordRefuge;

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

/**
 * Anguished Dragon Lord Refuge 副本 NPC AI：ID Tiamat2 Hard Kahrun（@AIName "kahrun3"），继承 NpcAI2。
 * Anguished Dragon Lord Refuge instance NPC AI: ID Tiamat2 Hard Kahrun (@AIName "kahrun3"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("kahrun3")
public class IDTiamat2HardKahrunAI2 extends NpcAI2
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
			switch (getNpcId()) {
			    case 833483: //Kahrun (Reian Leader)
				    SpawnTemplate template = SpawnEngine.addNewSingleTimeSpawn(300630000, 730625, 503.219757f, 516.651733f, 242.604065f, (byte) 0);
				    template.setEntityId(4);
				    portal.put(730625, SpawnEngine.spawnObject(template, instanceId));
				    AI2Actions.deleteOwner(IDTiamat2HardKahrunAI2.this);
				break;
			}
		}
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 10));
		return true;
	}
}
