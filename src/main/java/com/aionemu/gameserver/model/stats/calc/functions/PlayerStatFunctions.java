package com.aionemu.gameserver.model.stats.calc.functions;

import java.util.ArrayList;
import java.util.List;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.container.StatEnum;

/**
 * 玩家属性 Functions，用于属性相关逻辑。
 * Player Stat Functions for stats logic.
 */

public class PlayerStatFunctions {
	private static final List<IStatFunction> FUNCTIONS = new ArrayList<IStatFunction>();

	static {
		FUNCTIONS.add(new AttackSpeedFunction());
		FUNCTIONS.add(new PhysicalAttackFunction());
		FUNCTIONS.add(new MagicalAttackFunction());
		FUNCTIONS.add(new BoostCastingTimeFunction());
		FUNCTIONS.add(new PvPAttackRatioFunction());
		FUNCTIONS.add(new PvPDefendRatioFunction());
		FUNCTIONS.add(new PvPPhysicalAttackRatioFunction());
		FUNCTIONS.add(new PvPMagicalAttackRatioFunction());
		FUNCTIONS.add(new PvPPhysicalDefendRatioFunction());
		FUNCTIONS.add(new PvPMagicalDefendRatioFunction());
		FUNCTIONS.add(new PDefFunction());
		FUNCTIONS.add(new MaxHpFunction());
		FUNCTIONS.add(new MaxMpFunction());
		FUNCTIONS.add(new AgilityModifierFunction(StatEnum.BLOCK, 0.25f));
		FUNCTIONS.add(new AgilityModifierFunction(StatEnum.PARRY, 0.25f));
		FUNCTIONS.add(new AgilityModifierFunction(StatEnum.EVASION, 0.3f));
	}

	/** 返回 functions / Returns the functions */
	public static final List<IStatFunction> getFunctions() {
		return FUNCTIONS;
	}

	/** 添加 predefined stat functions / Adds predefined stat functions */
	public static final void addPredefinedStatFunctions(Player player) {
		player.getGameStats().addEffectOnly(null, FUNCTIONS);
	}
}
