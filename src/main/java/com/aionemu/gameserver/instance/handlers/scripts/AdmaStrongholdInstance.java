package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.world.WorldMapInstance;
import java.util.Set;

/**
 * 阿德玛要塞副本事件处理器。
 * Instance event handler for Adma Stronghold.
 *
 * @author Encom
 */

@InstanceID(320130000)
public class AdmaStrongholdInstance extends GeneralInstanceHandler
{
	/**
	 * NPC 掉落表注册时处理。
	 * Handle NPC drop-table registration.
	 *
	 * npc
	 */
	
	public void onDropRegistered(Npc npc) {
		Set<DropItem> dropItems = GameWorldServices.dropRegistrationService().getCurrentDropMap().get(npc.getObjectId());
		int npcId = npc.getNpcId();
		int index = dropItems.size() + 1;
		switch (npcId) {
			case 237148: //Captain Mundirve.
			case 237149: //Butler Luitart.
			case 237150: //Chief Maid Miladi.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000032, 1)); //Observation Post Passage Key.
		    break;
			case 237155: //Bard Guionbark.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000027, 1)); //Library Key.
		    break;
			case 237240: //Enthralled Gutorum.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000026, 1)); //Inner Chamber Key.
		    break;
			case 702658: //修道院箱子。 / Abbey Box.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053579, 1)); //[活动] 修道院礼包。 / [Event] Abbey Bundle.
		    break;
			case 702659: //高级修道院箱子。 / Noble Abbey Box.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053580, 1)); //[活动] 高级修道院礼包。 / [Event] Noble Abbey Bundle.
		    break;
			case 237241: //Enthralled Karemiwen.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 170175031, 1)); //[Souvenir] Karemiwen's Teddy Bear.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000028, 1)); //Main Hall Key.
				for (Player player: instance.getPlayersInside()) {
				    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053083, 1)); //淬炼溶液箱。 / Tempering Solution Chest.
					} switch (Rnd.get(1, 2)) {
				        case 1:
						    dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 123000927, 1)); //Karemiwen's Band.
						break;
						case 2:
						    dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 123000928, 1)); //Karemiwen's Leather Belt.
						break;
					}
				}
			break;
			case 237242: //Enthralled Taliesin.
			    switch (Rnd.get(1, 3)) {
				    case 1:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000029, 1)); //Great Dining Hall Key.
				    break;
				    case 2:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000030, 1)); //Lannok Treasury Key.
				    break;
				    case 3:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000031, 1)); //Servants Quarters Key.
				    break;
			    }
		    break;
			case 237239: //Death Reaper.
			    for (Player player: instance.getPlayersInside()) {
				    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053788, 1)); //Greater Stigma Support Bundle.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053083, 1)); //淬炼溶液箱。 / Tempering Solution Chest.
					} switch (Rnd.get(1, 2)) {
				        case 1:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188054548, 1)); //Master Lanmark's Weapon Box.
					    break;
					    case 2:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053273, 1)); //Master Accessory Treasure Box.
					    break;
					}
				}
			break;
		}
	}
	
	/**
	 * 玩家对 NPC 使用物品完成时处理。
	 * Handle item-use finish on an NPC.
	 *
	 * 玩家 / player
	 * npc
	 */
	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
		switch (npc.getNpcId()) {
			case 700396: //Ntuamu's Teddy Bear.
				player.getEffectController().removeEffect(18462); //Deep Wound.
			break;
			case 700397: //Tarnished Incense Burner.
				player.getEffectController().removeEffect(18463); //Mental Tremor.
			break;
		}
	}
	
	/**
	 * 副本创建时初始化逻辑。
	 * Initialize logic when the instance is created.
	 *
	 * @param instance 世界地图实例 / world-map instance
	 */
	@Override
    public void onInstanceCreate(WorldMapInstance instance) {
        super.onInstanceCreate(instance);
		if (!runtimeState().getBoolean("adma.lannok_dead", false)) {
			spawn(237244, 606.483f, 745.0968f, 197.72092f, (byte) 61); //Enthralled Lannok.
		}
		int gutorumPosition = runtimeState().getInt("adma.gutorum_position", 0);
		if (gutorumPosition == 0) {
			gutorumPosition = Rnd.get(1, 2);
			runtimeState().put("adma.gutorum_position", gutorumPosition);
		}
		switch (gutorumPosition) {
		    case 1:
				spawn(237240, 378.346f, 222.61f, 164.007f, (byte) 65); //Enthralled Gutorum.
			break;
			case 2:
				spawn(237240, 525.4f, 222.724f, 164.007f, (byte) 88); //Enthralled Gutorum.
			break;
		}
		if (runtimeState().getBoolean("adma.complete", false)) {
			spawnExit();
		}
		long potDeadline = runtimeState().getLong("adma.pot_deadline", 0);
		if (potDeadline > 0 && !runtimeState().getBoolean("adma.pot_removed", false)) {
			spawn(237245, 451.54147f, 276.3691f, 170.08488f, (byte) 90);
			scheduleDeadline("pot", potDeadline, this::expirePot);
		}
		long reaperDeadline = runtimeState().getLong("adma.reaper_deadline", 0);
		if (reaperDeadline > 0 && !runtimeState().getBoolean("adma.reaper_spawned", false)) {
			scheduleDeadline("reaper", reaperDeadline, this::spawnReaper);
		}
    }
	
	/**
	 * 玩家进入副本时处理。
	 * Handle a player entering the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onEnterInstance(final Player player) {
		// 可疑罐子将在 3 分钟后消失。 / The Suspicious Pot will disappear in 3 minutes.
		if (runtimeState().getLong("adma.pot_deadline", 0) == 0
				&& !runtimeState().getBoolean("adma.pot_removed", false)) {
			sendMsg(1403059, 0, false, 25, 2000);
			spawn(237245, 451.54147f, 276.3691f, 170.08488f, (byte) 90); //Suspicious Pot.
			long deadline = System.currentTimeMillis() + 180_000;
			runtimeState().put("adma.pot_deadline", deadline);
			scheduleDeadline("pot", deadline, this::expirePot);
		}
	}

	private void expirePot() {
		runtimeState().put("adma.pot_removed", true);
		sendMsg(1403060);
		deleteNpc(237245);
	}
	
    /**
     * 处理死亡事件。
     * Handle a death event.
     *
     * npc
     */
    @Override
    public void onDie(Npc npc) {
        Player player = npc.getAggroList().getMostPlayerDamage();
		switch (npc.getObjectTemplate().getTemplateId()) {
			case 237245: //Suspicious Pot.
				runtimeState().put("adma.pot_removed", true);
				cancelDeadline("pot");
				// 须按正确顺序摧毁暗影幽魂，否则它们会保留力量。 / Destroy the Shadow Specters in the proper order or they will retain their power.
				sendMsg(1403038, 0, false, 25, 4000);
				// 暗影幽魂正前往仓库，寻找仓库管理员古托伦。 / The Shadow Specter is moving towards the storehouse, to look for Warehouse Manager Gutorum.
				sendMsg(1403014, 0, false, 25, 6000);
			break;
			case 237240: //Enthralled Gutorum.
				// 暗影幽魂正前往卡雷米温卧室，寻找卡雷米温公主。 / The Shadow Specter is moving towards Karemiwen's Bedroom, to look for Princess Karemiwen.
				sendMsg(1403015, 0, false, 25, 2000);
			break;
			case 237241: //Enthralled Karemiwen.
				// 一道暗影正滑向二楼主厅。 / A dark shadow is slithering towards the 2nd floor Main Hall.
				sendMsg(1403016, 0, false, 25, 2000);
			break;
			case 237242: //Enthralled Taliesin.
				// 暗影幽魂正前往地下马厩，寻找马厩管理员泽图伦。 / The Shadow Specter is moving towards the Underground Stable, to look for Stable Keeper Zeeturun.
				sendMsg(1403017, 0, false, 25, 2000);
			break;
			case 237243: //Enthralled Zeeturun.
				// 暗影幽魂正前往崩塌观察哨，寻找兰马克领主。 / The Shadow Specter is moving towards the Collapsed Observation Post, to look for Lord Lanmark.
				sendMsg(1403018, 0, false, 25, 2000);
			break;
			case 237148: //Captain Mundirve.
				spawn(237159, 346.57733f, 534.69476f, 181.204f, (byte) 40);
                spawn(237159, 345.45975f, 544.2697f, 182.18115f, (byte) 71);
                spawn(237160, 359.008f, 557.2267f, 181.3445f, (byte) 79);
                spawn(237160, 349.17578f, 519.81287f, 181.27892f, (byte) 15);
                spawn(237161, 344.26273f, 525.73566f, 180.68095f, (byte) 69);
                spawn(237161, 356.2736f, 556.973f, 180.74712f, (byte) 12);
            break;
			case 237244: //Enthralled Lannok.
				despawnNpc(npc);
				runtimeState().put("adma.lannok_dead", true);
				long deadline = System.currentTimeMillis() + 3_000;
				runtimeState().put("adma.reaper_deadline", deadline);
				scheduleDeadline("reaper", deadline, this::spawnReaper);
            break;
			case 237239: //Death Reaper.
				// 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You have finished <Adma Stronghold>");
/* 				switch (Rnd.get(1, 2)) {
		            case 1:
				        spawn(702658, 614.9905f, 745.60156f, 198.75998f, (byte) 60); //修道院箱子。 / Abbey Box.
					break;
					case 2:
					    spawn(702659, 614.9905f, 745.60156f, 198.75998f, (byte) 60); //高级修道院箱子。 / Noble Abbey Box.
					break;
				} */
				runtimeState().put("adma.complete", true);
				spawnExit();
            break;
		}
    }

	private void spawnReaper() {
		if (runtimeState().getBoolean("adma.reaper_spawned", false)) {
			return;
		}
		runtimeState().put("adma.reaper_spawned", true);
		sendMsg(1403019, 0, false, 25, 0);
		spawn(237239, 606.483f, 745.0968f, 197.72092f, (byte) 61);
	}

	private void spawnExit() {
		SpawnTemplate exit = SpawnEngine.addNewSingleTimeSpawn(320130000, 730176, 627.72888f, 745.44885f, 199.8019f, (byte) 0);
		exit.setEntityId(66);
		SpawnEngine.spawnObject(exit, instanceId);
	}

	private void deleteNpc(int npcId) {
		Npc npc = getNpc(npcId);
		if (npc != null) {
			npc.getController().onDelete();
		}
	}
	/**
	 * 移除相关物品。
	 * Remove related items.
	 *
	 * @param player 玩家 / player
	 */
	
	public void removeItems(Player player) {
        Storage storage = player.getInventory();
        storage.decreaseByItemId(185000026, storage.getItemCountByItemId(185000026)); //Inner Chamber Key.
		storage.decreaseByItemId(185000027, storage.getItemCountByItemId(185000027)); //Library Key.
        storage.decreaseByItemId(185000028, storage.getItemCountByItemId(185000028)); //Main Hall Key.
		storage.decreaseByItemId(185000029, storage.getItemCountByItemId(185000029)); //Great Dining Hall Key.
		storage.decreaseByItemId(185000030, storage.getItemCountByItemId(185000030)); //Lannok Treasury Key.
		storage.decreaseByItemId(185000031, storage.getItemCountByItemId(185000031)); //Servants Quarters Key.
		storage.decreaseByItemId(185000032, storage.getItemCountByItemId(185000032)); //Observation Post Passage Key.
    }
	
	private void despawnNpc(Npc npc) {
		if (npc != null) {
			npc.getController().onDelete();
		}
	}
	
	/**
	 * 玩家从该副本登出时处理。
	 * Handle a player logging out from this instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onPlayerLogOut(Player player) {
		removeItems(player);
	}
	
	/**
	 * 玩家离开副本时处理。
	 * Handle a player leaving the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onLeaveInstance(Player player) {
		removeItems(player);
	}
}
