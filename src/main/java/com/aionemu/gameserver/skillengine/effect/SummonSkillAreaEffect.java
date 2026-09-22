package com.aionemu.gameserver.skillengine.effect;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.concurrent.Future;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.NpcObjectType;
import com.aionemu.gameserver.model.gameobjects.Servant;
import com.aionemu.gameserver.skillengine.model.Effect;

/**
 * 召唤技能区域效果：在落点生成区域侍从，周期性释放技能。
 * Summon skill-area effect: spawns an area servant that periodically uses a skill.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SummonSkillAreaEffect")
public class SummonSkillAreaEffect extends SummonServantEffect {
	/**
	 * 在目标/落点生成 SKILLAREA 侍从，并按技能 ID 覆盖存活时间后周期施法。
	 * Spawns a SKILLAREA servant at the landing point, overrides duration for known skills, and ticks skill use.
	 */
	@Override
	public void applyEffect(Effect effect) {
		if (effect.getEffector().getTarget() == null) {
			effect.getEffector().setTarget(effect.getEffector());
		}
		float x = effect.getX();
		float y = effect.getY();
		float z = effect.getZ();
		if (x == 0 && y == 0) {
			Creature effected = getPositionReference(effect);
			x = effected.getX();
			y = effected.getY();
			z = effected.getZ();
		}
		int useTime = time;
        useTime = switch (effect.getSkillId()) {
            // 冰面 4.8 / Ice Sheet 4.8
            case 1308, 1309, 1310, 1311, 1312, 1313, 1314, 1315, 1316, 1317, 1318, 1319, 1320, 1321, 1322, 1323 -> 15;
            // 骑乘爆炸 4.8 / Mounting Explosion 4.8
            case 1431, 1432 -> 30;
            // 显现龙卷 4.8 / Manifest Tornado 4.8
            case 1460, 1461, 1462, 1463, 1464, 1465, 1466, 1467, 1468, 1469, 1470, 1471, 1472, 1473, 1474, 1475 -> 3;
            // 战斗召唤 4.8 / Battle Call 4.8
            case 3036, 3037 -> 11;
            // 闪电原野 5.1 / Field Of Lightning 5.1
            case 4770, 4771, 4826 -> 9;
            default -> useTime;
        };
		final Servant servant = spawnServant(effect, useTime, NpcObjectType.SKILLAREA, x, y, z);
		final int finalSkillId = servant.getSkillList() != null ? servant.getSkillList().getRandomSkill().getSkillId()
				: 0;
		Future<?> task = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(() -> servant.getController().useSkill(finalSkillId), 0, 3000);
		servant.getController().addTask(TaskId.SKILL_USE, task);
	}
}
