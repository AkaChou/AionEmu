package com.aionemu.gameserver.network.aion.clientpackets;

import lombok.extern.slf4j.Slf4j;

import com.aionemu.gameserver.lifecycle.GameEventBootstrapServices;

import java.util.ArrayList;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.services.NameRestrictionService;
import com.aionemu.gameserver.services.toypet.MinionService;
import com.aionemu.gameserver.services.toypet.PetSpawnService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 随从（Minion）养成与操作的客户端包。
 * Client packet for minion management and actions.
 *
 * @author Falke_34, FrozenKiller Reworked by G-Robson26
 */
@Slf4j
public class CM_MINIONS extends AionClientPacket {

	private int actionId;
	private String minionName;
	private int objectId;
	private int itemObjectId;
	private int charge;
	private int autoCharge;
	private int functId;
	private int subSwitch;
	private int minionObjectId;
	private int functionParam1;
	private int functionParam2;
	private ArrayList<Integer> MaterialObjIds = new ArrayList<Integer>();
	private int lock = 0;
	/**
	 * 构造该客户端包。
	 * Constructs this client packet.
	 *
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余合法状态 / additional valid states
	 */
	public CM_MINIONS(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}
	/**
	 * 按动作 ID 读取随从操作参数。
	 * Reads minion action parameters by action id.
	 */
	@Override
	protected void readImpl() {
		actionId = readH();
		switch (actionId) {
		case 0: // 添加 / add
			itemObjectId = readD(); // 物品唯一 ID（随从契约）/ Item UniqueId (Minion Contract)
			break;
		case 1: // 删除 / delete
			objectId = readD();
			break;
		case 2: // 重命名 / rename
			objectId = readD(); // 随从唯一 ID / Minion Unique ID
			minionName = readS(); // 名称 / Name
			break;
		case 3: // 锁定 / locked
			objectId = readD(); // 随从唯一 ID / Minion Unique ID
			lock = readC(); // 锁定/解锁 / lock/unlock
			break;
		case 4: // 召唤 / summon
		case 5: // 收回 / unsummon
			minionObjectId = readD(); // 随从唯一 ID / Minion Unique ID
			break;
		case 6: // 进阶 / ascension
			MaterialObjIds.clear();
			objectId = readD(); // 随从唯一 ID / Minion Unique ID
			for (int i = 0; i < 10; i++) {
				MaterialObjIds.add(readD());
			}
			break;
		case 7: // 进化 / evolution
			objectId = readD();
			break;
		case 8:// 合成 / combination
			MaterialObjIds.clear();
			for (int i = 0; i < 4; i++) {
				MaterialObjIds.add(readD());
			}
			break;
		case 9: // 随从功能：子开关后跟四个定长参数 / Minion function: sub-switch followed by four fixed-width parameters
			subSwitch = readD();
			functId = readD();
			minionObjectId = readD();
			functionParam1 = readD();
			functionParam2 = readD();
			break;
		case 10: // 无可读内容 / Nothing to read (Falke Log 5.6_Minion_Function)
			break;
		case 11: // 充能 / charge
			charge = readC(); // 充能：1 = true / 0 = false？/ Charge 1 = true / 0 = false ?
			autoCharge = readC(); // 自动充能开关 / Auto recharge on/off
			break;
		case 12:
			readC(); // 自动功能开关 / Auto Function on/off
			break;
		case 13: // 停止使用守护灵功能，无附加参数 / Stop minion functions, no payload
			break;
		case 14: // 增益开启 / BUFF ON
			readC(); // 20?
			readC();
			readC();
			break;
		default:
			break;
		}
	}
	/**
	 * 执行随从添加、删除、召唤、充能等动作。
	 * Executes minion add, delete, spawn, charge, and related actions.
	 */
	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();
		if (player == null) {
			return;
		}
		switch (actionId) {
		case 0:
			GameEventBootstrapServices.minionService().addMinion(player, itemObjectId);
			break;
		case 1:
			GameEventBootstrapServices.minionService().deleteMinion(player, objectId, false);
			break;
		case 2:
			if (NameRestrictionService.isForbiddenWord(minionName)) {
				PacketSendUtility.sendMessage(player, "You are trying to use a forbidden name. Choose another one!");
			} else {
				GameEventBootstrapServices.minionService().renameMinion(player, objectId, minionName);
			}
			break;
		case 3:
			GameEventBootstrapServices.minionService().lockMinion(player, objectId, lock);
			break;
		case 4:
			if (player.getPet() != null) {
				PetSpawnService.dismissPet(player, true);
			}
			GameEventBootstrapServices.minionService().spawnMinion(player, minionObjectId);
			break;
		case 5:
			GameEventBootstrapServices.minionService().despawnMinion(player, minionObjectId);
			break;
		case 6:
			GameEventBootstrapServices.minionService().growthUpMinion(player, objectId, MaterialObjIds);
			break;
		case 7:
			GameEventBootstrapServices.minionService().evolutionUpMinion(player, objectId);
			break;
		case 8:
			GameEventBootstrapServices.minionService().CombinationMinion(player, MaterialObjIds);
			break;
		case 9:
			switch (subSwitch) {
			case 0: {
				switch (functId) {
				case 0: { // 添加物品 / Add Item
					log.debug("CM_MINIONS handle add item. playerId={} minionObjectId={} itemId={} targetSlot={}",
							player.getObjectId(), minionObjectId, functionParam1, functionParam2);
					GameEventBootstrapServices.minionService().addMinionFunctionItem(player, minionObjectId, functionParam1,
							functionParam2);
					break;
				}
				case 1: {
					GameEventBootstrapServices.minionService().removeMinionFunctionItem(player, minionObjectId, functionParam1);
					break;
				}
				case 2: {
					log.debug("CM_MINIONS relocate doping. playerId={} minionObjectId={} targetSlot={} destinationSlot={}",
							player.getObjectId(), minionObjectId, functionParam1, functionParam2);
					GameEventBootstrapServices.minionService().relocateDoping(player, minionObjectId, functionParam1, functionParam2);
					break;
				}
				case 3: {
					log.debug("CM_MINIONS buff on. playerId={} minionObjectId={} itemId={} targetSlot={}",
							player.getObjectId(), minionObjectId, functionParam1, functionParam2);
					GameEventBootstrapServices.minionService().buffPlayer(player, minionObjectId, functionParam1, functionParam2);
					break;
				}
				}
				break;
			}
			case 1: {
				log.debug("CM_MINIONS autoloot. playerId={} minionObjectId={} activate={}", player.getObjectId(), functId,
						minionObjectId != 0);
				GameEventBootstrapServices.minionService().activateLoot(player, functId, minionObjectId != 0);
				break;
			}
			}
			break;
		case 10: // 随从功能（激活）/ MinionFunction (Activate)
			GameEventBootstrapServices.minionService().activateMinionFunction(player);
			break;
		case 11:
			GameEventBootstrapServices.minionService().addMinionSkillPoints(player, charge == 1, autoCharge == 1);
			break;
		case 12:
			break;
		case 13:
			GameEventBootstrapServices.minionService().deactivateMinionFunction(player);
			break;
		case 14:
			break;
		default:
			break;
		}
	}
}
