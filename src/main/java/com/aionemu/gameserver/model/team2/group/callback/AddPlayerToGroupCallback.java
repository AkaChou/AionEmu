package com.aionemu.gameserver.model.team2.group.callback;

import com.aionemu.commons.callbacks.Callback;
import com.aionemu.commons.callbacks.CallbackResult;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.group.PlayerGroup;

/**
 * Add 玩家 To 队伍 Callback，用于团队2相关逻辑。
 * Add Player To Group Callback for team 2 logic.
 *
 * @author ATracer
 */
@SuppressWarnings("rawtypes")
public abstract class AddPlayerToGroupCallback implements Callback {

	/** 调用前 / before Call. */
	@Override
	public CallbackResult beforeCall(Object obj, Object[] args) {
		onBeforePlayerAddToGroup((PlayerGroup) args[0], (Player) args[1]);
		return CallbackResult.newContinue();
	}

	/** 调用后 / after Call. */
	@Override
	public CallbackResult afterCall(Object obj, Object[] args, Object methodResult) {
		onAfterPlayerAddToGroup((PlayerGroup) args[0], (Player) args[1]);
		return CallbackResult.newContinue();
	}

	/** 获取基础职业。 / Returns the base class. */
	@Override
	public Class<? extends Callback> getBaseClass() {
		return AddPlayerToGroupCallback.class;
	}

	/** 玩家加入小队前 / On Before Player Add To Group */
	public abstract void onBeforePlayerAddToGroup(PlayerGroup group, Player player);

	/** 玩家加入小队后 / On After Player Add To Group */
	public abstract void onAfterPlayerAddToGroup(PlayerGroup group, Player player);
}
