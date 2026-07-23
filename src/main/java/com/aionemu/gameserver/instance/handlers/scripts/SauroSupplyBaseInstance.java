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

@InstanceID(301130000)
public class SauroSupplyBaseInstance extends GeneralInstanceHandler
{
	@Override
	public void onDropRegistered(Npc npc) {
		int npcId = npc.getNpcId();
		if (npcId == 230847) {
			sendMsg(1401946, 0, false, 25, 0);
			return;
		}
		if (npcId != 802181) {
			return;
		}
		Set<DropItem> dropItems = GameWorldServices.dropRegistrationService().getCurrentDropMap().get(npc.getObjectId());
		dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 186000051, 30)); //Major Ancient Crown.
		dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 186000052, 30)); //Greater Ancient Crown.
		dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 186000236, 50)); //Blood Mark.
		dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, 186000237, 50)); //Ancient Coin.
	}
	
	/**
	 * 奖励怪：“索罗基地盗墓者”，可出现在 5 个房间，掉落古代硬币、古代魔石、外观。 / Bonus Monster: "Sauro Base Grave Robber" They can appear in "5 different rooms" and give: Ancient Coins. Ancient Manastones. Skins
	 */
	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		scheduleWarnings();
		spawnThief();
		int completionBoss = runtimeState().getInt("sauro.completion_boss", 0);
		if (completionBoss != 0) {
			spawnCompletionObjects(completionBoss);
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
		switch (npc.getObjectTemplate().getTemplateId()) {
			case 230846:
				runtimeState().put("sauro.thief_killed", true);
				break;
			case 230849: //Guard Captain Rohuka.
				setDoorState(383, true);
				break;
			case 230851: //Chief Gunner Kurmata.
				setDoorState(59, true);
				// 通往达努阿尔冥想花园的门已打开。 / The door to the Danuar Meditation Garden has opened.
				sendMsg(1401915, 0, false, 25, 0);
				int side = runtimeState().getInt("sauro.kurmata_side", 0);
				if (side == 0) {
					side = Rnd.get(1, 2);
					runtimeState().put("sauro.kurmata_side", side);
				}
				switch (side) {
					case 1:
						setDoorState(382, true);
						// 守门人倒下，左侧门已打开！ / With the gatekeeper down, the door on the left is open!
						sendMsg(1401229, 0, false, 25, 5000);
						break;
					default:
						setDoorState(387, true);
						// 守门人倒下，右侧门已打开！ / With the gatekeeper down, the door on the right is open!
						sendMsg(1401230, 0, false, 25, 5000);
						break;
				}
				break;
			case 230818: //Sheban Legion Elite Gunner.
				setDoorState(372, true);
				break;
			case 230850: //Research Teselik.
				setDoorState(375, true);
				break;
			case 233255: //Gatekeeper Stranir.
				setDoorState(378, true);
				break;
			case 230852: //Commander Ranodim.
				setDoorState(388, true);
				break;
			case 230791: //Sheban Legion Elite Assaulter.
				setDoorState(376, true);
				break;
			case 230853: //Chief Of Staff Moriata.
				// 通往达努阿尔万神殿的通道将开放 5 分钟。 / The passage to the Danuar Omphanium will be open for five minutes.
				sendMsg(1401922, 0, false, 25, 5000);
				break;
			case 230857: //Guard Captain Ahuradim.
			case 230858: //Brigade General Sheba.
				if (runtimeState().getInt("sauro.completion_boss", 0) == 0) {
					runtimeState().put("sauro.completion_boss", npc.getNpcId());
					spawnCompletionObjects(npc.getNpcId());
				}
				break;
		}
	}

	private void scheduleWarnings() {
		long start = runtimeState().getLong("sauro.warning_start", 0);
		if (start == 0) {
			start = System.currentTimeMillis();
			runtimeState().put("sauro.warning_start", start);
		}
		scheduleWarning("warning_1", start + 10_000, 1401810);
		scheduleWarning("warning_2", start + 30_000, 1401811);
		scheduleWarning("warning_3", start + 50_000, 1401812);
		scheduleWarning("warning_4", start + 70_000, 1401813);
		scheduleWarning("warning_5", start + 130_000, 1401814);
	}

	private void scheduleWarning(String key, long deadline, int messageId) {
		if (deadline > System.currentTimeMillis()) {
			scheduleDeadline(key, deadline, () -> sendMsg(messageId));
		}
	}

	private void spawnThief() {
		if (runtimeState().getBoolean("sauro.thief_killed", false)) {
			return;
		}
		int selection = runtimeState().getInt("sauro.thief_spawn", 0);
		if (selection == 0) {
			selection = Rnd.get(1, 5);
			runtimeState().put("sauro.thief_spawn", selection);
		}
		switch (selection) {
			case 1 -> spawn(230846, 464.07788f, 401.3575f, 182.15321f, (byte) 10);
			case 2 -> spawn(230846, 496.30792f, 412.814f, 182.13792f, (byte) 73);
			case 3 -> spawn(230846, 497.15717f, 392.34656f, 182.14955f, (byte) 75);
			case 4 -> spawn(230846, 496.2902f, 358.0765f, 182.14955f, (byte) 48);
			default -> spawn(230846, 464.15985f, 389.7157f, 182.15321f, (byte) 109);
		}
	}

	private void spawnCompletionObjects(int bossId) {
		if (bossId == 230857) {
			spawn(801967, 708.9197f, 884.59625f, 411.57986f, (byte) 45);
			spawn(802181, 710.25726f, 889.6806f, 411.59103f, (byte) 0);
		} else {
			spawn(801967, 905.3781f, 895.2461f, 411.57785f, (byte) 75);
			spawn(802181, 906.9721f, 889.6604f, 411.59854f, (byte) 0);
		}
	}
	}
