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
		switch (effect.getSkillId()) {
		// 冰面 4.8 / Ice Sheet 4.8
		case 1308:
		case 1309:
		case 1310:
		case 1311:
		case 1312:
		case 1313:
		case 1314:
		case 1315:
		case 1316:
		case 1317:
		case 1318:
		case 1319:
		case 1320:
		case 1321:
		case 1322:
		case 1323:
			useTime = 15;
			break;
		// 骑乘爆炸 4.8 / Mounting Explosion 4.8
		case 1431:
		case 1432:
			useTime = 30;
			break;
		// 显现龙卷 4.8 / Manifest Tornado 4.8
		case 1460:
		case 1461:
		case 1462:
		case 1463:
		case 1464:
		case 1465:
		case 1466:
		case 1467:
		case 1468:
		case 1469:
		case 1470:
		case 1471:
		case 1472:
		case 1473:
		case 1474:
		case 1475:
			useTime = 3;
			break;
		// 战斗召唤 4.8 / Battle Call 4.8
		case 3036:
		case 3037:
			useTime = 11;
			break;
		// 闪电原野 5.1 / Field Of Lightning 5.1
		case 4770:
		case 4771:
		case 4826:
			useTime = 9;
			break;
		}
		final Servant servant = spawnServant(effect, useTime, NpcObjectType.SKILLAREA, x, y, z);
		final int finalSkillId = servant.getSkillList() != null ? servant.getSkillList().getRandomSkill().getSkillId()
				: 0;
		Future<?> task = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				servant.getController().useSkill(finalSkillId);
			}
		}, 0, 3000);
		servant.getController().addTask(TaskId.SKILL_USE, task);
	}
}
