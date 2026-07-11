package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.HouseObject;
import com.aionemu.gameserver.model.gameobjects.UseableItemObject;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PortalCooldownItem;
import com.aionemu.gameserver.model.items.ItemCooldown;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_COOLDOWN;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SKILL_COOLDOWN;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

/**
 * 冷却清除指令；清除目标玩家的技能/物品/房屋物品冷却，或指定副本冷却。
 * Admin command that clears skill/item/house-object cooldowns, or specific instance cooldowns, for the target player.
 *
 * @author kecimis
 */
public class RemoveCd extends AdminCommand {

	public RemoveCd() {
		super("removecd");
	}

	/**
	 * 执行该管理指令。
	 * Executes this admin command.
	 *
	 * @param admin 执行指令的管理员 / admin executing the command
	 * command arguments
	 */
	@Override
	public void execute(Player admin, String... params) {
		VisibleObject target = admin.getTarget();
		if (target == null)
			target = admin;

		if (target instanceof Player) {
			Player player = (Player) target;
			if (params.length == 0) {
				List<Integer> delayIds = new ArrayList<Integer>();
				if (player.getSkillCoolDowns() != null) {
					long currentTime = System.currentTimeMillis();
					for (Entry<Integer, Long> en : player.getSkillCoolDowns().entrySet())
						delayIds.add(en.getKey());

					for (Integer delayId : delayIds)
						player.setSkillCoolDown(delayId, currentTime);

					delayIds.clear();
					PacketSendUtility.sendPacket(player, new SM_SKILL_COOLDOWN(player.getSkillCoolDowns()));
				}

				if (player.getItemCoolDowns() != null) {
					for (Entry<Integer, ItemCooldown> en : player.getItemCoolDowns().entrySet())
						delayIds.add(en.getKey());

					for (Integer delayId : delayIds)
						player.addItemCoolDown(delayId, 0, 0);

					delayIds.clear();
					PacketSendUtility.sendPacket(player, new SM_ITEM_COOLDOWN(player.getItemCoolDowns()));
				}

				if (player.getHouseRegistry() != null && player.getHouseObjectCooldownList().getHouseObjectCooldowns().size() > 0) {
					Iterator<HouseObject<?>> iter = player.getHouseRegistry().getObjects().iterator();
					while (iter.hasNext()) {
						HouseObject<?> obj = iter.next();
						if (obj instanceof UseableItemObject) {
							if (!player.getHouseObjectCooldownList().isCanUseObject(obj.getObjectId()))
								player.getHouseObjectCooldownList().addHouseObjectCooldown(obj.getObjectId(), 0);
						}
					}
				}

				if (player.equals(admin))
					PacketSendUtility.sendMessage(admin, "Your cooldowns were removed");
				else {
					PacketSendUtility.sendMessage(admin, "You have removed cooldowns of player: " + player.getName());
					PacketSendUtility.sendMessage(player, "Your cooldowns were removed by admin");
				}
			}
			else if (params[0].contains("instance")) {
				if (player.getPortalCooldownList() == null || player.getPortalCooldownList().getPortalCoolDowns() == null)
					return;
				if (params.length >= 2) {
					if (params[1].equalsIgnoreCase("all")) {
						List<Integer> mapIds = new ArrayList<Integer>();
                        for (Entry<Integer, PortalCooldownItem> mapId : player.getPortalCooldownList().getPortalCoolDowns().entrySet()) {
                            mapIds.add(mapId.getKey());
                        }

                        for (Integer id : mapIds) {
                            player.getPortalCooldownList().addPortalCooldown(id, 0, 0);
                        }

						mapIds.clear();
						if (player.equals(admin))
							PacketSendUtility.sendMessage(admin, "Your instance cooldowns were removed");
						else {
							PacketSendUtility
								.sendMessage(admin, "You have removed instance cooldowns of player: " + player.getName());
							PacketSendUtility.sendMessage(player, "Your instance cooldowns were removed by admin");
						}
					}
					else {
						int worldId = 0;
						try {
							worldId = Integer.parseInt(params[1]);
						}
						catch (NumberFormatException e) {
							PacketSendUtility.sendMessage(admin, "WorldId has to be integer or use \"all\"");
							return;
						}

						if (player.getPortalCooldownList().isPortalUseDisabled(worldId)) {
							player.getPortalCooldownList().addPortalCooldown(worldId, 0, 0);

							if (player.equals(admin))
								PacketSendUtility.sendMessage(admin, "Your instance cooldown worldId: " + worldId + " was removed");
							else {
								PacketSendUtility.sendMessage(admin, "You have removed instance cooldown worldId: " + worldId
									+ " of player: " + player.getName());
								PacketSendUtility.sendMessage(player, "Your instance cooldown worldId: " + worldId
									+ " was removed by admin");
							}
						}
						else
							PacketSendUtility.sendMessage(admin, "You or your target can enter given instance");

					}
				}
				else
					PacketSendUtility.sendMessage(admin, "syntax: //removecd instance <all|worldId>");
			}
		}
		else
			PacketSendUtility.sendMessage(admin, "Only players are allowed as target");
	}
}