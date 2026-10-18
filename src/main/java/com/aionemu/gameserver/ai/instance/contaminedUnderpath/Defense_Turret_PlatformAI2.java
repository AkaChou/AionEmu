package com.aionemu.gameserver.ai.instance.contaminedUnderpath;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIALOG_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 污染暗道副本 NPC AI：防卫炮塔基座（@AIName "defense_turret_platform"），继承 NpcAI2。
 * Contamined Underpath instance NPC AI: Defense Turret Platform (@AIName "defense_turret_platform"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("defense_turret_platform")
public class Defense_Turret_PlatformAI2 extends NpcAI2 {

	@Override
	protected void handleDialogStart(Player player) {
		// 对齐客户端 Dialogs/event_branch/idluna_swprototype_01a.html 的 select1 页面（dialogId=1011）
		// Align with client's select1 page (dialogId=1011) in idluna_swprototype_01a.html
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 1011));
	}

	@Override
	public boolean onDialogSelect(final Player player, int dialogId, int questId, int extendedRewardIndex) {
		int turretNpcId = 0;
		int cost = 0;
		if (dialogId == 10000) { // HACTION_SETPRO1: 设置速射型连射炮(1个玲珑的奥德) / Rapid Fire Cannon (1 Aether)
			turretNpcId = 833808;
			cost = 1;
		} else if (dialogId == 10001) { // HACTION_SETPRO2: 设置范围型对空炮(2个玲珑的奥德) / AA Gun (2 Aether)
			turretNpcId = 833809;
			cost = 2;
		} else if (dialogId == 10002) { // HACTION_SETPRO3: 设置强力魔力炮(2个玲珑的奥德) / Magic Cannon (2 Aether)
			turretNpcId = 833810;
			cost = 2;
		}

		if (turretNpcId != 0) {
			if (player.getInventory().decreaseByItemId(182007405, cost)) { // 明亮的奥德 / Bright Aether
				spawn(turretNpcId, getOwner().getX(), getOwner().getY(), getOwner().getZ(), getOwner().getHeading());
				PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
				AI2Actions.deleteOwner(this);
			} else {
				// 奥德不足：提示物品数量不足并关闭对话，绝不删除基座 / Insufficient aether: notify and keep platform
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1401544)); // 物品数量不足。 / Not enough items.
				PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
			}
			return true;
		}

		// 结束对话或取消，仅关闭对话，绝不删除基座 / Cancel or finish: close dialog, keep platform
		PacketSendUtility.sendPacket(player, new SM_DIALOG_WINDOW(getObjectId(), 0));
		return true;
	}
}
