package com.aionemu.gameserver.model.team2.group.callback;

import com.aionemu.commons.callbacks.Callback;
import com.aionemu.commons.callbacks.CallbackResult;
import com.aionemu.gameserver.model.team2.group.PlayerGroup;

/**
 * 玩家队伍 DisbandCallback，用于团队2相关逻辑。
 * Player Group Disband Callback for team 2 logic.
 *
 * @author ATracer
 */
@SuppressWarnings("rawtypes")
public abstract class PlayerGroupDisbandCallback implements Callback {

	/** 调用前 / before Call. */
	@Override
	public CallbackResult beforeCall(Object obj, Object[] args) {
		onBeforeGroupDisband((PlayerGroup) args[0]);
		return CallbackResult.newContinue();
	}

	/** 调用后 / after Call. */
	@Override
	public CallbackResult afterCall(Object obj, Object[] args, Object methodResult) {
		onAfterGroupDisband((PlayerGroup) args[0]);
		return CallbackResult.newContinue();
	}

	/** 获取基础职业。 / Returns the base class. */
	@Override
	public Class<? extends Callback> getBaseClass() {
		return PlayerGroupDisbandCallback.class;
	}

	/** 在 group disband 前 / On Before Group Disband */
	public abstract void onBeforeGroupDisband(PlayerGroup group);

	/** 在 group disband 后 / On After Group Disband */
	public abstract void onAfterGroupDisband(PlayerGroup group);
}
