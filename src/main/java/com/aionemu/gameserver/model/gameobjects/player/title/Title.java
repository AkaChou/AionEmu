package com.aionemu.gameserver.model.gameobjects.player.title;

import com.aionemu.gameserver.model.IExpirable;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.TitleTemplate;

/**
 * 称号游戏对象。
 * Title game object.
 *
 * @author Mr. Poke
 */
public class Title implements IExpirable {

	private TitleTemplate template;
	private int id;
	private int dispearTime;

	/**
	 * @param template
	 * @param id
	 * @param dispearTime
	 */
	public Title(TitleTemplate template, int id, int dispearTime) {
		this.template = template;
		this.id = id;
		this.dispearTime = dispearTime;
	}

	/**
	 * @return Returns the template.
	 */
	public TitleTemplate getTemplate() {
		return template;
	}

	/**
	 * @return Returns the id.
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return Returns the dispearTime.
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
