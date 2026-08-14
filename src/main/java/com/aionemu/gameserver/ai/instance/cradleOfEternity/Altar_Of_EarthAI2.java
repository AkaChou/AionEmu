package com.aionemu.gameserver.ai.instance.cradleOfEternity;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

import java.util.List;

/**
 * Cradle Of Eternity 副本 NPC AI：Altar Of Earth（@AIName "Altar_Of_Earth"），继承 ActionItemNpcAI2。
 * Cradle Of Eternity instance NPC AI: Altar Of Earth (@AIName "Altar_Of_Earth"), extends ActionItemNpcAI2.
 *
 * @author Encom
 */
@AIName("Altar_Of_Earth")
public class Altar_Of_EarthAI2 extends ActionItemNpcAI2
{
	@Override
	protected void handleDialogStart(Player player) {
		super.handleDialogStart(player);
	}
	
	@Override
	protected void handleUseItemFinish(Player player) {
		if (!player.getInventory().decreaseByItemId(185000266, 1)) { // 大地孔雀石 / Earthen Malachite.
			// 你没有可放在祭坛上的大地孔雀石。 / You don’t have a Malachite of Earth to place on the altar.
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1403447));
			return;
		} switch (getNpcId()) {
			case 834006: // 大地祭坛 / Altar Of Earth.
				// 大地孔雀石发出光芒并开始漂浮。 / The Malachite of Earth emits a light and starts to float.
				PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_IDEternity_02_SYSTEM_MSG_37, 5000);
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					@Override
					public void run() {
						despawnNpc(834006);
						AI2Actions.deleteOwner(Altar_Of_EarthAI2.this);
						spawn(834006, 1025.1476f, 774.97748f, 1033.6420f, (byte) 0, 291);
				    }
			    }, 5000);
			break;
			case 834019: // 大地祭坛 / Altar Of Earth.
				// 大地孔雀石发出光芒并开始漂浮。 / The Malachite of Earth emits a light and starts to float.
				PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_IDEternity_02_SYSTEM_MSG_37, 5000);
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					@Override
					public void run() {
						despawnNpc(834019);
						AI2Actions.deleteOwner(Altar_Of_EarthAI2.this);
						spawn(834019, 1027.2802f, 771.84601f, 1033.6420f, (byte) 0, 340);
				    }
			    }, 5000);
			break;
			case 834020: // 大地祭坛 / Altar Of Earth.
				// 大地孔雀石发出光芒并开始漂浮。 / The Malachite of Earth emits a light and starts to float.
				PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_IDEternity_02_SYSTEM_MSG_37, 5000);
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					@Override
					public void run() {
						despawnNpc(834020);
						AI2Actions.deleteOwner(Altar_Of_EarthAI2.this);
						spawn(834020, 1027.4769f, 777.98260f, 1033.6420f, (byte) 0, 299);
				    }
			    }, 5000);
			break;
			case 834021: // 大地祭坛 / Altar Of Earth.
				// 大地孔雀石发出光芒并开始漂浮。 / The Malachite of Earth emits a light and starts to float.
				PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_IDEternity_02_SYSTEM_MSG_37, 5000);
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					@Override
					public void run() {
						despawnNpc(834021);
						AI2Actions.deleteOwner(Altar_Of_EarthAI2.this);
						spawn(834021, 1031.0382f, 776.67932f, 1033.6420f, (byte) 0, 387);
				    }
			    }, 5000);
			break;
			case 834022: // 大地祭坛 / Altar Of Earth.
				// 大地孔雀石发出光芒并开始漂浮。 / The Malachite of Earth emits a light and starts to float.
				PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_IDEternity_02_SYSTEM_MSG_37, 5000);
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					@Override
					public void run() {
						despawnNpc(834017);
						despawnNpc(834022);
						AI2Actions.deleteOwner(Altar_Of_EarthAI2.this);
						spawn(834022, 1030.9221f, 772.90582f, 1033.6420f, (byte) 0, 395);
						spawn(834091, 974.25085f, 775.06488f, 1027.0811f, (byte) 0, 322);
				    }
			    }, 5000);
			break;
		}
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
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
