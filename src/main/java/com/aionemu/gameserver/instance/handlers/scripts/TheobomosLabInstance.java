package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.world.WorldMapInstance;
import java.util.Set;

/**
 * 西奥博莫斯实验室副本事件处理器。
 * Instance event handler for Theobomos Lab.
 *
 * @author Encom
 */

@InstanceID(310110000)
public class TheobomosLabInstance extends GeneralInstanceHandler
{
	/** silikor guard / silikor guard */
		private int silikorGuard;
	
	/**
	 * NPC 掉落表注册时处理。
	 * Handle NPC drop-table registration.
	 *
	 * npc
	 */
	@Override
    public void onDropRegistered(Npc npc) {
        Set<DropItem> dropItems = GameWorldServices.dropRegistrationService().getCurrentDropMap().get(npc.getObjectId());
		int npcId = npc.getNpcId();
		int index = dropItems.size() + 1;
        switch (npcId) {
			case 237108: //Frozen Harint.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000016, 1)); //Fire Key.
		    break;
			case 237110: //Naughty Pocaching.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000025, 1)); //Laboratory Key.
		    break;
			case 237112: //Wistful Syripne.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000023, 1)); //Syripne's Key.
		    break;
			case 237113: //Soul Spirit Nomura.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000022, 1)); //Nomura's Key.
		    break;
			case 237114: //Water Spirit Undine.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000021, 1)); //Undine's Key.
		    break;
			case 700422: //Faded Book.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 182208053, 1)); //Research Center Document.
		    break;
			case 702658: //修道院箱子。 / Abbey Box.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053579, 1)); //[活动] 修道院礼包。 / [Event] Abbey Bundle.
		    break;
			case 702659: //高级修道院箱子。 / Noble Abbey Box.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 188053580, 1)); //[活动] 高级修道院礼包。 / [Event] Noble Abbey Bundle.
		    break;
			case 237247: //Watcher Cracked Nuhas.
				switch (Rnd.get(1, 3)) {
				    case 1:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000018, 1)); //Water Key.
					break;
					case 2:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000019, 1)); //Earth Key.
					break;
					case 3:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000020, 1)); //Wind Key.
					break;
				}
		    break;
			case 237251: //Corrupted Ifrit.
			    for (Player player: instance.getPlayersInside()) {
				    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053788, 1)); //Greater Stigma Support Bundle.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053083, 1)); //淬炼溶液箱。 / Tempering Solution Chest.
					} switch (Rnd.get(1, 2)) {
				        case 1:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188054176, 1)); //Master Triroan's Weapon Box.
					    break;
					    case 2:
				            dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188054180, 1)); //Master Accessory Relic Boxx.
					    break;
					}
				}
			break;
			case 237118: //Titan Protector.
				dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 185000015, 1)); //Laboratory Chest Key.
		    break;
			case 237119: //Antique Treasure Chest.
				switch (Rnd.get(1, 8)) {
					case 1:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 166050023, 2)); //Noble Blue Idian: Physical Attack.
					break;
					case 2:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 166050024, 2)); //Noble Blue Idian: Magical Attack.
					break;
					case 3:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 166050025, 2)); //Noble Blue Idian: Physical Defense.
					break;
					case 4:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 166050026, 2)); //Noble Blue Idian: Magical Defense.
					break;
					case 5:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 166050027, 2)); //Noble Blue Idian: Assistance.
					break;
					case 6:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 166050028, 2)); //Noble Blue Idian: Resistance.
					break;
					case 7:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 166050029, 2)); //Noble Blue Idian: Physical Magical Attack.
					break;
					case 8:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 166050030, 2)); //Noble Blue Idian: Physical Magical Defense.
					break;
				}
			break;
			case 237120: //Antique Treasure Chest.
			case 237121: //Antique Treasure Chest.
				switch (Rnd.get(1, 8)) {
					case 1:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 166050031, 2)); //Esoteric Idian: Physical Attack.
					break;
					case 2:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 166050032, 2)); //Esoteric Idian: Magical Attack.
					break;
					case 3:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 166050033, 2)); //Esoteric Idian: Physical Defense.
					break;
					case 4:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 166050034, 2)); //Esoteric Idian: Magical Defense.
					break;
					case 5:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 166050035, 2)); //Esoteric Idian: Assistance.
					break;
					case 6:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 166050036, 2)); //Esoteric Idian: Resistance.
					break;
					case 7:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 166050037, 2)); //Esoteric Idian: Physical Magical Attack.
					break;
					case 8:
				        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 166050038, 2)); //Esoteric Idian: Physical Magical Defense.
					break;
				}
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
		silikorGuard = runtimeState().getInt("theobomos.silikor_guard", 0);
		if (!runtimeState().getBoolean("theobomos.triroan_dead", false)
				&& !runtimeState().getBoolean("theobomos.complete", false)) {
			spawn(237250, 616.169f, 488.758f, 196.015f, (byte) 62);
		}
		int chest = runtimeState().getInt("theobomos.chest", 0);
		if (chest == 0) {
			chest = Rnd.get(1, 3);
			runtimeState().put("theobomos.chest", chest);
		}
		switch (chest) {
			case 1:
				spawn(237119, 455.78845f, 774.0474f, 157.89963f, (byte) 0); //Antique Treasure Chest.
			break;
			case 2:
				spawn(237120, 455.78845f, 774.0474f, 157.89963f, (byte) 0); //Antique Treasure Chest.
			break;
			case 3:
				spawn(237121, 455.78845f, 774.0474f, 157.89963f, (byte) 0); //Antique Treasure Chest.
			break;
		}
		long stoneDeadline = runtimeState().getLong("theobomos.stone_deadline", 0);
		if (stoneDeadline > 0 && !runtimeState().getBoolean("theobomos.stone_removed", false)) {
			spawn(237253, 477.88632f, 230.60364f, 173.06987f, (byte) 90);
			scheduleDeadline("stone", stoneDeadline, this::expireStone);
		}
		long ifritDeadline = runtimeState().getLong("theobomos.ifrit_deadline", 0);
		if (ifritDeadline > 0 && !runtimeState().getBoolean("theobomos.ifrit_spawned", false)) {
			scheduleDeadline("ifrit", ifritDeadline, this::spawnIfrit);
		}
		if (runtimeState().getBoolean("theobomos.complete", false)) {
			spawnExit();
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
		if (runtimeState().getLong("theobomos.stone_deadline", 0) == 0
				&& !runtimeState().getBoolean("theobomos.stone_removed", false)) {
			sendMsg(1403061, 0, false, 25, 2000);
			spawn(237253, 477.88632f, 230.60364f, 173.06987f, (byte) 90);
			long deadline = System.currentTimeMillis() + 180_000;
			runtimeState().put("theobomos.stone_deadline", deadline);
			scheduleDeadline("stone", deadline, this::expireStone);
		}
	}

	private void expireStone() {
		runtimeState().put("theobomos.stone_removed", true);
		sendMsg(1403062);
		deleteNpc(237253);
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
			case 237253: //Fiery Sealing Stone.
				runtimeState().put("theobomos.stone_removed", true);
				cancelDeadline("stone");
				// 若未按正确顺序举行仪式，辉煌元素将失去力量。 / If you do not perform the proper order of the ritual, the Brilliant Elemental will lose its power.
				sendMsg(1403039, 0, false, 25, 4000);
				// 辉煌元素正朝记忆硅石所在的元素核心生成室发出光束。 / The Brilliant Elemental is beaming towards the researcher's lounge where Queen Arachne is located.
				sendMsg(1403021, 0, false, 25, 6000);
				spawn(237258, 477.88632f, 230.60364f, 173.06987f, (byte) 90); //Demon Lord Mulion.
			break;
			case 237246: //Watcher Queen Arachne.
				// 刺眼光束正射向中央控制室。 / The blinding light is beaming towards the Central Control Room.
				sendMsg(1403022, 0, false, 25, 2000);
            break;
			case 237247: //Watcher Cracked Nuhas.
				// 辉煌元素正朝记忆硅石所在的元素核心生成室发出光束。 / The Brilliant Elemental is beaming towards the Elemental Core Generation Room where the Silicanimum of Memory is located.
				sendMsg(1403023, 0, false, 25, 2000);
            break;
			case 237248: //Watcher Silikor Of Memory.
				// 辉煌元素正朝记忆硅石所在的元素核心生成室发出光束。 / The Brilliant Elemental is beaming towards the Library of Theobomos where Jilitia of Innocence is located.
				sendMsg(1403024, 0, false, 25, 2000);
            break;
			case 237249: //Watcher Jilitia.
				// 辉煌元素正朝记忆硅石所在的元素核心生成室发出光束。 / The Brilliant Elemental is beaming towards the Elemental Core Testing Room where Unstable Triroan is located.
				sendMsg(1403025, 0, false, 25, 2000);
            break;
			case 280971: //First Silikor Guard.
			case 280972: //Second Silikor Guard.
				silikorGuard ++;
				runtimeState().put("theobomos.silikor_guard", silikorGuard);
				if (silikorGuard == 1) {
				} else if (silikorGuard == 2) {
					spawn(237248, 392.5771f, 744.2743f, 189.38637f, (byte) 41); //Watcher Silikor Of Memory.
				}
            break;
			case 237250: //Sealed Unstable Triroan.
				despawnNpc(npc);
				runtimeState().put("theobomos.triroan_dead", true);
				long deadline = System.currentTimeMillis() + 3_000;
				runtimeState().put("theobomos.ifrit_deadline", deadline);
				scheduleDeadline("ifrit", deadline, this::spawnIfrit);
			break;
			case 237251: //Corrupted Ifrit.
			    //sendMsg("Congratulation]: you finish <Theobomos Lab>");
/* 				switch (Rnd.get(1, 2)) {
		            case 1:
				        spawn(702658, 602.04486f, 488.82837f, 196.01512f, (byte) 60); //修道院箱子。 / Abbey Box.
					break;
					case 2:
					    spawn(702659, 602.04486f, 488.82837f, 196.01512f, (byte) 60); //高级修道院箱子。 / Noble Abbey Box.
					break;
				} */
				runtimeState().put("theobomos.complete", true);
				spawnExit();
			break;
		}
	}

	private void spawnIfrit() {
		if (runtimeState().getBoolean("theobomos.ifrit_spawned", false)) {
			return;
		}
		runtimeState().put("theobomos.ifrit_spawned", true);
		sendMsg(1403026, 0, false, 25, 0);
		spawn(237251, 616.169f, 488.758f, 196.015f, (byte) 62);
	}

	private void spawnExit() {
		spawn(730178, 637.3241f, 475.9548f, 195.96295f, (byte) 0, 244);
	}

	private void deleteNpc(int npcId) {
		Npc npc = getNpc(npcId);
		if (npc != null) {
			npc.getController().onDelete();
		}
	}
	
	private void despawnNpc(Npc npc) {
		if (npc != null) {
			npc.getController().onDelete();
		}
	}
	
}
