package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAY_MOVIE;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import java.util.Set;

@InstanceID(301140000)
public class SeizedDanuarSanctuaryInstance extends GeneralInstanceHandler
{
	private Race spawnRace;
	private boolean raceActorsSpawned;
	
	/**
	 * 副本创建时初始化逻辑。
	 * Initialize logic when the instance is created.
	 *
	 * @param instance 世界地图实例 / world-map instance
	 */
	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		String race = runtimeState().get("seized.race");
		if (race != null) {
			spawnRace = Race.valueOf(race);
			spawnRaceActors();
		}
		restoreWarnings();
		spawnBoss();
	}
	
	/**
	 * 玩家进入副本时处理。
	 * Handle a player entering the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onEnterInstance(Player player) {
		if (runtimeState().getLong("seized.warning_start", 0) == 0) {
			runtimeState().put("seized.warning_start", System.currentTimeMillis());
			restoreWarnings();
		}
		PacketSendUtility.sendPacket(player, new SM_PLAY_MOVIE(0, player.getRace() == Race.ELYOS ? 910 : 911));
		if (spawnRace == null) {
			spawnRace = player.getRace();
			runtimeState().put("seized.race", spawnRace.name());
			spawnRaceActors();
		}
	}

	private void spawnRaceActors() {
		if (raceActorsSpawned) {
			return;
		}
		raceActorsSpawned = true;
		final int seizedGuard1 = spawnRace == Race.ASMODIANS ? 233126 : 233129;
		final int seizedGuard2 = spawnRace == Race.ASMODIANS ? 233127 : 233130;
		final int seizedGuard3 = spawnRace == Race.ASMODIANS ? 233128 : 233131;
		spawn(seizedGuard1, 911.333f, 904.6127f, 284.5891f, (byte) 110);
		spawn(seizedGuard1, 917.35785f, 901.0081f, 284.5891f, (byte) 50);
		spawn(seizedGuard1, 1025.9675f, 474.7492f, 290.26837f, (byte) 0);
		spawn(seizedGuard1, 1033.9897f, 474.7517f, 290.26837f, (byte) 61);
		spawn(seizedGuard2, 1029.233f, 484.0199f, 290.52118f, (byte) 31);
		spawn(seizedGuard2, 978.1413f, 1337.8359f, 335.875f, (byte) 34);
		spawn(seizedGuard2, 1019.45715f, 1367.1343f, 337.25f, (byte) 52);
		spawn(seizedGuard2, 881.45166f, 892.719f, 284.55508f, (byte) 109);
		spawn(seizedGuard2, 885.13104f, 898.88446f, 284.50986f, (byte) 109);
		spawn(seizedGuard3, 1103.6545f, 439.36285f, 284.61642f, (byte) 66);
		spawn(seizedGuard3, 833.283f, 961.50146f, 304.86777f, (byte) 79);
		spawn(seizedGuard3, 824.21826f, 967.07446f, 304.86777f, (byte) 79);
		spawn(seizedGuard3, 932.1827f, 876.7008f, 305.45746f, (byte) 92);
		spawn(seizedGuard3, 949.92975f, 903.508f, 299.75253f, (byte) 93);
	}

	private void restoreWarnings() {
		long start = runtimeState().getLong("seized.warning_start", 0);
		if (start == 0) {
			return;
		}
		int[] messages = { 1401855, 1401856, 1401857, 1401858, 1401859, 1401860, 1401861 };
		long now = System.currentTimeMillis();
		for (int i = 0; i < messages.length; i++) {
			long deadline = start + (i + 1L) * 300_000;
			if (deadline > now) {
				int message = messages[i];
				scheduleDeadline("warning." + i, deadline, () -> sendMsg(message));
			}
		}
	}

	private void spawnBoss() {
		if (runtimeState().getBoolean("seized.boss_dead", false)) {
			return;
		}
		int bossId = runtimeState().getInt("seized.boss_id", 0);
		if (bossId == 0) {
			bossId = switch (Rnd.get(1, 3)) {
				case 1 -> 235619;
				case 2 -> 235620;
				default -> 235621;
			};
			runtimeState().put("seized.boss_id", bossId);
		}
		spawn(bossId, 1056.5953f, 693.457f, 287.9919f, (byte) 0);
	}

	@Override
	public void onDropRegistered(Npc npc) {
		int npcId = npc.getNpcId();
		if (npcId == 233391) {
			sendMsg(1401946, 0, false, 25, 0);
			return;
		}
		if (npcId != 235574) {
			return;
		}
		Set<DropItem> dropItems = GameWorldServices.dropRegistrationService().getCurrentDropMap().get(npc.getObjectId());
		dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 186000254, 1)); //Seal Breaking Magic Cannonball.
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
			case 701859: //Metallic Mystic KeyStone.
			case 701860: //Golden Mystic KeyStone.
				if (player.getInventory().isFull()) {
					sendMsg(1390149, 0, false, 25, 0);
					return;
				}
				despawnNpc(npc);
				ItemService.addItem(player, 188052613, 1); //Sanctuary Treasure Crate.
				break;
			case 701863: //Spherical Mystic KeyStone.
				// 某处有一扇门已打开。 / A door has opened somewhere.
				sendMsg(1401838, 0, false, 25, 0);
			break;
			case 701864: //Pyramidal Mystic KeyStone.
				//某处沉重的门已打开。 / A heavy door has opened somewhere.
				sendMsg(1401839, 0, false, 25, 0);
			break;
		}
	}

	/**
	 * 处理死亡事件。
	 * Handle a death event.
	 *
	 * npc
	 */
	@Override
	public void onDie(Npc npc) {
		int npcId = npc.getNpcId();
		if (npcId == 235619 || npcId == 235620 || npcId == 235621) {
			runtimeState().put("seized.boss_dead", true);
		}
	}

	private void despawnNpc(Npc npc) {
		if (npc != null) {
			npc.getController().onDelete();
		}
	}
}
