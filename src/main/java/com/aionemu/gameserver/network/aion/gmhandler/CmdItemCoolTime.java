package com.aionemu.gameserver.network.aion.gmhandler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.aionemu.gameserver.model.gameobjects.HouseObject;
import com.aionemu.gameserver.model.gameobjects.UseableItemObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.ItemCooldown;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_COOLDOWN;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SKILL_COOLDOWN;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * GM 指令：重置目标玩家技能/物品/房屋物件冷却。
 * GM command handler that resets skill, item and house-object cooldowns for the target.
 * @author Alcapwnd
 */
public class CmdItemCoolTime extends AbstractGMHandler {

	/**
	 * 创建处理器并立即重置冷却。
	 * Creates the handler and immediately resets cooldowns.
	 * @param admin 执行指令的管理员 / the admin executing the command
	 */
	public CmdItemCoolTime(Player admin) {
		super(admin, "");
		run();
	}

	/**
	 * 重置目标玩家的技能、物品与房屋物件冷却并同步客户端。
	 * Resets skill, item and house-object cooldowns for the target and syncs the client.
	 */
	private void run() {
		Player playerT = target != null ? target : admin;

		List<Integer> delayIds = new ArrayList<>();
		if (playerT.getSkillCoolDowns() != null) {
			long currentTime = System.currentTimeMillis();
			for (Entry<Integer, Long> en : playerT.getSkillCoolDowns().entrySet()) {
				delayIds.add(en.getKey());
			}
			for (Integer delayId : delayIds) {
				playerT.setSkillCoolDown(delayId, currentTime);
			}
			delayIds.clear();
			PacketSendUtility.sendPacket(playerT, new SM_SKILL_COOLDOWN(playerT.getSkillCoolDowns()));
		}

		if (playerT.getItemCoolDowns() != null) {
			for (Entry<Integer, ItemCooldown> en : playerT.getItemCoolDowns().entrySet()) {
				delayIds.add(en.getKey());
			}
			for (Integer delayId : delayIds) {
				playerT.addItemCoolDown(delayId, 0, 0);
			}
			delayIds.clear();
		}
		// 物品冷却的计时器由客户端持有，本包是唯一能改写它的手段：表为空时也必须整表下发，
		// 否则客户端本地预测的冷却扫描会残留，道具一直置灰并被客户端拦住无法使用。
		// The client owns the item-cooldown timers and this packet is the only way to rewrite them:
		// it must be sent even when the table is empty, otherwise the client's locally predicted sweep
		// stays and the item remains greyed out and unusable.
		PacketSendUtility.sendPacket(playerT, SM_ITEM_COOLDOWN.load(playerT.getItemCoolDowns()));

		if (playerT.getHouseRegistry() != null
				&& playerT.getHouseObjectCooldownList().getHouseObjectCooldowns().size() > 0) {
			Iterator<HouseObject<?>> iter = playerT.getHouseRegistry().getObjects().iterator();
			while (iter.hasNext()) {
				HouseObject<?> obj = iter.next();
				if (obj instanceof UseableItemObject) {
					if (!playerT.getHouseObjectCooldownList().isCanUseObject(obj.getObjectId())) {
						playerT.getHouseObjectCooldownList().addHouseObjectCooldown(obj.getObjectId(), 0);
					}
				}
			}
		}
	}
}
