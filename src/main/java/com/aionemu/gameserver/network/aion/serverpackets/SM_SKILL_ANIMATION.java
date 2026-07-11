package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.skinskill.SkillSkin;
import com.aionemu.gameserver.model.skinskill.SkillSkinList;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端同步技能皮肤/动画外观。
 * Server packet synchronizing skill skin/animation appearance to the client.
 *
 * @author FrozenKiller
 */
public class SM_SKILL_ANIMATION extends AionServerPacket {
	private SkillSkinList skillSkinList;
	private int action;
	private int skillSkinId;
	private int expire;
	private int isActive;

	/**
	 * 使用给定参数构造 SM_SKILL_ANIMATION 包。
	 * Creates a SM_SKILL_ANIMATION packet with the given parameters.
	 *
	 * skill skin id
	 * expire time
	 */
	public SM_SKILL_ANIMATION(int skillSkinId, int expire) {
		action = 0;
		this.skillSkinId = skillSkinId;
		this.expire = expire;
		isActive = 1;
	}

	/**
	 * 使用给定参数构造 SM_SKILL_ANIMATION 包。
	 * Creates a SM_SKILL_ANIMATION packet with the given parameters.
	 *
	 * 玩家 / player
	 */
	public SM_SKILL_ANIMATION(Player player) {
		action = 1;
		skillSkinList = player.getSkillSkinList();
	}

	protected void writeImpl(AionConnection con) {
		writeC(action);
		switch (action) {
		case 0:
			writeH(1);
			writeH(skillSkinId);
			writeD(expire);
			writeC(isActive);
			break;
		case 1:
			if (skillSkinList != null) {
				writeH(skillSkinList.size());
				for (SkillSkin skillSkin : skillSkinList.getSkillSkins()) {
					writeH(skillSkin.getId());
					writeD(skillSkin.getExpireTime());
					writeC(skillSkin.getIsActive());
				}
			}
			break;
		default:
			break;
		}
	}
}
