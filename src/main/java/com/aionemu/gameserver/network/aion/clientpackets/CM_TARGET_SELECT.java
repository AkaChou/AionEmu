package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Trap;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.TeamMember;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TARGET_SELECTED;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TARGET_UPDATE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.audit.AuditLogger;

/**
 * 客户端目标选择请求包（点击或 /Select；objectId 为 0 时取消选择）。
 * Client packet for target selection (click or /Select; 0 unselects).
 *
 * @author SoulKeeper, Sweetkr, KID
 */
public class CM_TARGET_SELECT extends AionClientPacket {

	/**
	 * 目标物体 ID；0 表示取消选择。
	 * Target object id that client wants to select or 0 if wants to unselect.
	 */
	private int targetObjectId;
	private int type;

	/**
	 * packet opcode
	 * @param state 连接状态 / connection state
	 * @param restStates 其余允许状态 / additional allowed states
	 */
	public CM_TARGET_SELECT(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	/**
	 * 读取包体：d - 物体 ID；c - 选择类型。
	 * Read packet: d - object id; c - selection type.
	 */
	@Override
	protected void readImpl() {
		targetObjectId = readD();
		type = readC();
	}

	/**
	 * 设置目标并广播更新；对不可见目标记录雷达作弊审计。
	 * Set target and broadcast update; audit possible radar hacks on invisible targets.
	 */
	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();

		VisibleObject obj = null;
		if (targetObjectId == player.getObjectId()) {
			obj = player;
		} else {
			obj = player.getKnownList().getObject(targetObjectId);

			if (obj == null && player.isInTeam()) {
				TeamMember<Player> member = player.getCurrentTeam().getMember(targetObjectId);
				if (member != null) {
					obj = member.getObject();
				}
			}
		}

		if (obj != null) {
			if (type == 1) {
				if (obj.getTarget() == null)
					return;
				player.setTarget(obj.getTarget());
			} else {
				player.setTarget(obj);
			}
			if (obj instanceof Player) {
				Player target = (Player) obj;
				if (player != obj && !player.canSee(target)) {
					AuditLogger.info(player, "Possible radar hacker detected, targeting on invisible Player name: "
							+ target.getName() + " objectId: " + target.getObjectId() + " by");
				}
			} else if (obj instanceof Trap) {
				Trap target = (Trap) obj;
				boolean isSameTeamTrap = false;
				if (target.getMaster() instanceof Player) {
					isSameTeamTrap = ((Player) target.getMaster()).isInSameTeam(player);
				}
				if (player != obj && !player.canSee(target) && !isSameTeamTrap) {
					AuditLogger.info(player, "Possible radar hacker detected, targeting on invisible Trap name: "
							+ target.getName() + " objectId: " + target.getObjectId() + " by");
				}
			} else if (obj instanceof Creature) {
				Creature target = (Creature) obj;
				if (player != obj && !player.canSee(target)) {
					AuditLogger.info(player, "Possible radar hacker detected, targeting on invisible Npc name: "
							+ target.getName() + " objectId: " + target.getObjectId() + " by");
				}
			}
		} else {
			player.setTarget(null);
		}
		sendPacket(new SM_TARGET_SELECTED(player));
		PacketSendUtility.broadcastPacket(player, new SM_TARGET_UPDATE(player));
	}
}
