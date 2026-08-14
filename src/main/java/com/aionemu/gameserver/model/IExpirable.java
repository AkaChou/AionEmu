package com.aionemu.gameserver.model;

import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 可过期对象接口接口。
 * I Expirable interface.
 *
 * @author Mr. Poke
 */
public interface IExpirable {

	/** 返回过期时间 / Returns the expire time */
	public int getExpireTime();

	/** 到期结束 / expire End */
	public void expireEnd(Player player);

	/** 是否立即过期 / Whether expire now */
	public boolean canExpireNow();

	/** 发送到期提示消息 / Send the expire message */
	public void expireMessage(Player player, int time);
}
