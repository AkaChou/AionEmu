package com.aionemu.gameserver.model.skinskill;

import com.aionemu.gameserver.model.IExpirable;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.SkillSkinTemplate;
import com.aionemu.gameserver.utils.PacketSendUtility;
import lombok.Getter;

/**
 * 技能外观模型。
 * Skill Skin model.
 *
 * @author Rinzler (Encom)
 */
public class SkillSkin implements IExpirable {

	@Getter
	private SkillSkinTemplate template;
	@Getter
	private int id;
	private int dispearTime = 0;
	@Getter
	private int isActive;

	public SkillSkin(SkillSkinTemplate template, int id, int dispearTime, int isActive) {
		this.template = template;
		this.id = id;
		this.dispearTime = dispearTime;
		this.isActive = isActive;
	}

	/** 返回剩余时间 / Returns the remaining time */
	public int getRemainingTime() {
		if (dispearTime == 0) {
			return 0;
		}
		return dispearTime - (int) (System.currentTimeMillis() / 1000L);
	}

	/** 获取过期时间。 / Returns the expire time. */
	@Override
	public int getExpireTime() {
		return dispearTime;
	}

	/** 到期结束 / Expire End */
	@Override
	public void expireEnd(Player player) {
		player.getSkillSkinList().removeSkillSkin(id);
	}

	/** 过期消息。 / Expire Message. */
	@Override
	public void expireMessage(Player player, int time) {
		PacketSendUtility.sendBrightYellowMessageOnCenter(player, "Skill Animation Expired"); // For testing should be
																								// 若全部则稍后移除 / removed later if all
																								// 100% 有效 / works 100%
	}

	/** 是否立即过期 / Whether expire now */
	@Override
	public boolean canExpireNow() {
		return true;
	}
}
