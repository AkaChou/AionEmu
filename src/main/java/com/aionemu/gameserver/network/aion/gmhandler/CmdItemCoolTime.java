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
 *
 * @author Alcapwnd
 */
public class CmdItemCoolTime extends AbstractGMHandler {

	/**
	 * 创建处理器并立即重置冷却。
	 * Creates the handler and immediately resets cooldowns.
	 *
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

		List<Integer> delayIds = new ArrayList<Integer>();
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
			PacketSendUtility.sendPacket(playerT, new SM_ITEM_COOLDOWN(playerT.getItemCoolDowns()));
		}

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
