package com.aionemu.gameserver.network.aion.clientpackets;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.configs.main.SecurityConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.AionClientPacket;
import com.aionemu.gameserver.network.aion.AionConnection.State;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.TYPE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CAPTCHA;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.PunishmentService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 提交验证码（Captcha）答案的客户端包。
 * Client packet submitting a captcha answer.
 *
 * @author Cura
 */
@Slf4j
public class CM_CAPTCHA extends AionClientPacket {

	private int type;
	private int count;
	private String word;

	/**
	 * @param opcode
	 */
	public CM_CAPTCHA(int opcode, State state, State... restStates) {
		super(opcode, state, restStates);
	}

	@Override
	protected void readImpl() {
		type = readC();

		switch (type) {
		case 0x02:
			count = readC();
			word = readS();
			break;
		default:
			log.warn(I18n.get("log.aa5ba2351828", Integer.toHexString(type).toUpperCase()));
			break;
		}
	}

	@Override
	protected void runImpl() {
		Player player = getConnection().getActivePlayer();

		switch (type) {
		case 0x02:
			if (player.getCaptchaWord().equalsIgnoreCase(word)) {
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400270));
				PacketSendUtility.sendPacket(player, new SM_CAPTCHA(true, 0));

				PunishmentService.setIsNotGatherable(player, 0, false, 0);

				// 飞行时间加成（如正式服） / fp bonus (like retail)
				player.getLifeStats().increaseFp(TYPE.FP, SecurityConfig.CAPTCHA_BONUS_FP_TIME);
			} else {
				int banTime = SecurityConfig.CAPTCHA_EXTRACTION_BAN_TIME
						+ SecurityConfig.CAPTCHA_EXTRACTION_BAN_ADD_TIME * count;

				if (count < 3) {
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400271, 3 - count));
					PacketSendUtility.sendPacket(player, new SM_CAPTCHA(false, banTime));
					PunishmentService.setIsNotGatherable(player, count, true, banTime * 1000L);
				} else {
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400272));
					PunishmentService.setIsNotGatherable(player, count, true, banTime * 1000L);
				}
			}
			break;
		}
	}
}
