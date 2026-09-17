package com.aionemu.gameserver.model.gameobjects.player.title;

import com.aionemu.gameserver.model.IExpirable;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.TitleTemplate;
import lombok.Getter;
import lombok.AllArgsConstructor;

/**
 * 称号游戏对象。
 * Title game object.
 *
 * @author Mr. Poke
 */
@Getter
@AllArgsConstructor
public class Title implements IExpirable {

	/**
	 * @return 称号模板 / the template
	 */
	private final TitleTemplate template;
	/**
	 * @return 称号 ID / the id
	 */
	private final int id;
	private final int dispearTime;

	/**
	 * @return 剩余时间 / remaining time
	 */
	public int getRemainingTime() {
		if (dispearTime == 0) {
			return 0;
		}
		return dispearTime - (int) (System.currentTimeMillis() / 1000);
	}

	/** 获取过期时间。 / Returns the expire time. */
	@Override
	public int getExpireTime() {
		return dispearTime;
	}

	/** 到期结束 / Expire End */
	@Override
	public void expireEnd(Player player) {
		player.getTitleList().removeTitle(id);
	}

	/** 过期消息。 / Expire Message. */
	@Override
	public void expireMessage(Player player, int time) {
	}

	/** 是否立即过期 / Whether expire now */
	@Override
	public boolean canExpireNow() {
		return true;
	}
}
