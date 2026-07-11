package com.aionemu.gameserver.commands.admin;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.calc.StatOwner;
import com.aionemu.gameserver.model.stats.calc.functions.IStatFunction;
import com.aionemu.gameserver.model.stats.calc.functions.StatFunctionProxy;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import java.util.TreeSet;

/**
 * 查看目标生物指定属性修饰列表的管理员命令。
 * Admin command to inspect a target creature's modifiers for a given stat.
 *
 * @author MrPoke
 */
@Slf4j
public class Stat extends AdminCommand {

	/**
	 * 构造 stat 命令。
	 * Creates the stat admin command.
	 */
	public Stat() {
		super("stat");
	}

	/**
	 * 列出目标生物某属性的修饰函数，可选 details 输出技能来源。
	 * Lists modifiers for a stat on the target creature; optional details dump skill source.
	 *
	 * 执行 GM / Admin player
	 * StatEnum name, optional details
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params.length >= 1) {
			VisibleObject target = admin.getTarget();
			if (target == null) {
				PacketSendUtility.sendMessage(admin, "No target selected");
				return;
			}
			if (target instanceof Creature) {
				Creature creature = (Creature) target;

				TreeSet<IStatFunction> stats = creature.getGameStats().getStatsByStatEnum(StatEnum.valueOf(params[0]));

				if (params.length == 1) {
					for (IStatFunction stat : stats) {
						PacketSendUtility.sendMessage(admin, stat.toString());
					}
				}
				else if ("details".equals(params[1])) {
					for (IStatFunction stat : stats) {
						String details = collectDetails(stat);
						PacketSendUtility.sendMessage(admin, details);
						log.info(details);
					}
				}
			}
		}
	}

	/**
	 * 收集属性修饰的详细信息（代理函数、技能来源等）。
	 * Collects detail text for a stat function (proxy, skill owner, etc.).
	 *
	 * @param stat 属性修饰函数 / Stat function
	 * Detail text
	 */
	private String collectDetails(IStatFunction stat) {
		StringBuffer sb = new StringBuffer();
		sb.append(stat.toString() + "\n");
		if(stat instanceof StatFunctionProxy){
			StatFunctionProxy proxy = (StatFunctionProxy) stat;
			sb.append(" -- " + proxy.getProxiedFunction().toString());
		}
		StatOwner owner = stat.getOwner();
		if(owner instanceof Effect){
			Effect effect = (Effect) owner;
			sb.append("\n -- skillId: " + effect.getSkillId());
			sb.append("\n -- skillName: " + effect.getSkillName());
		}
		return sb.toString();
	}

}
