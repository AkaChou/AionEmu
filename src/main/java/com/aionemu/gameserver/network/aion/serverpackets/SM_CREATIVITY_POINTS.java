package com.aionemu.gameserver.network.aion.serverpackets;


import lombok.extern.slf4j.Slf4j;

import com.aionemu.gameserver.model.cp.PlayerCPEntry;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.RequiredArgsConstructor;
import lombok.AllArgsConstructor;
import com.aionemu.gameserver.services.player.CreativityPanel.CreativitySkillService;
import com.aionemu.gameserver.services.player.CreativityPanel.CreativityTransfoService;
import com.aionemu.gameserver.services.player.CreativityPanel.stats.Agility;
import com.aionemu.gameserver.services.player.CreativityPanel.stats.Health;
import com.aionemu.gameserver.services.player.CreativityPanel.stats.Knowledge;
import com.aionemu.gameserver.services.player.CreativityPanel.stats.Power;
import com.aionemu.gameserver.services.player.CreativityPanel.stats.Precision;
import com.aionemu.gameserver.services.player.CreativityPanel.stats.Will;

/**
 * 创造点数（CP）同步包：下发总点数、等级上限与各槽位分配，登录时并应用属性变更。
 * Creativity Points sync packet: total points, level cap and slot allocations; applies stat changes on login.
 * @author Falke_34, FrozenKiller
 */
@Slf4j
@RequiredArgsConstructor
@AllArgsConstructor
public class SM_CREATIVITY_POINTS extends AionServerPacket {

	private int totalPoint;
	@SuppressWarnings("unused")
	private final int dotStep;
	private int size;
	private boolean onLogin;

	/**
	 * 构造 CP 同步包（总点数与技能点数档位）。
	 * Creates a CP sync packet (total points and dot step).
	 * @param totalPoint 已分配创造点数总数 / total allocated creativity points
	 * @param dotStep 技能点数档位 / skill dot step
	 */
	public SM_CREATIVITY_POINTS(int totalPoint, int dotStep) {
		this.totalPoint = totalPoint;
		this.dotStep = dotStep;
	}

	private int CpByExp(int level) {
        return switch (level) {
            case 66 -> 1 + 1;
            case 67 -> 10 + 10;
            case 68 -> 19 + 15;
            case 69 -> 30 + 20;
            case 70 -> 42 + 25;
            case 71 -> 55 + 30;
            case 72 -> 70 + 35;
            case 73 -> 87 + 40;
            case 74 -> 107 + 45;
            case 75 -> 130 + 155;
            default -> 285;
        };
    }

	@Override
	protected void writeImpl(AionConnection con) {
		Player activePlayer = con.getActivePlayer();

		writeD(totalPoint); // Creativity Points Total
		writeD(CpByExp(activePlayer.getLevel()));
		writeH(size);
		if (onLogin) {
			for (PlayerCPEntry ce : activePlayer.getCP().getAllCP()) {
				writeD(ce.getSlot());
				writeH(ce.getPoint());
				if (ce.getSlot() == 1) {
					activePlayer.setCPSlot1(ce.getPoint());
					Power.getInstance().onChange(activePlayer, ce.getPoint());
				}
				if (ce.getSlot() == 2) {
					activePlayer.setCPSlot2(ce.getPoint());
					Health.getInstance().onChange(activePlayer, ce.getPoint());
				}
				if (ce.getSlot() == 3) {
					activePlayer.setCPSlot3(ce.getPoint());
					Agility.getInstance().onChange(activePlayer, ce.getPoint());
				}
				if (ce.getSlot() == 4) {
					activePlayer.setCPSlot4(ce.getPoint());
					Precision.getInstance().onChange(activePlayer, ce.getPoint());
				}
				if (ce.getSlot() == 5) {
					activePlayer.setCPSlot5(ce.getPoint());
					Knowledge.getInstance().onChange(activePlayer, ce.getPoint());
				}
				if (ce.getSlot() == 6) {
					activePlayer.setCPSlot6(ce.getPoint());
					Will.getInstance().onChange(activePlayer, ce.getPoint());
				}
				/*
				 * if (ce.getSlot() >= 15 && ce.getSlot() <= 372 && ce.getPoint() != 0) { //
				 * Reduce Server Send Packet. its handled on login
				 * CreativitySkillService.getInstance().enchantSkill(activePlayer, ce.getSlot(),
				 * ce.getPoint()); }
				 * CreativityTransfoService.getInstance().enchantTransfo(activePlayer,
				 * ce.getSlot(), ce.getPoint()); if (ce.getSlot() >= 373 && ce.getSlot() <= 400)
				 * { CreativitySkillService.getInstance().loginDaevaSkill(activePlayer,
				 * ce.getSlot(), ce.getPoint()); }
				 */
			}
		} else {
			for (PlayerCPEntry ce : activePlayer.getCP().getAllCP()) {
				if (ce.getSlot() == 1) {
					activePlayer.setCPSlot1(ce.getPoint());
				}
				if (ce.getSlot() == 2) {
					activePlayer.setCPSlot2(ce.getPoint());
				}
				if (ce.getSlot() == 3) {
					activePlayer.setCPSlot3(ce.getPoint());
				}
				if (ce.getSlot() == 4) {
					activePlayer.setCPSlot4(ce.getPoint());
				}
				if (ce.getSlot() == 5) {
					activePlayer.setCPSlot5(ce.getPoint());
				}
				if (ce.getSlot() == 6) {
					activePlayer.setCPSlot6(ce.getPoint());
				}
			}
		}
	}
}
