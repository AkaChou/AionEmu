package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.zone.ZoneType;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.zone.ZoneInstance;
import com.aionemu.gameserver.world.zone.ZoneName;

import java.util.List;

/**
 * 查询/刷新区域信息的管理员命令。
 * Admin command to inspect or refresh zone information.
 *
 * @author ATracer
 */
public class Zone extends AdminCommand {

	/**
	 * 构造 zone 命令。
	 * Creates the zone command.
	 */
	public Zone() {
		super("zone");
	}

	/**
	 * 无参列出目标所在区域；refresh 重算；inside 判断是否在指定区域。
	 * With no args lists zones for target; refresh revalidates; inside checks a zone name.
	 *
	 * @param admin 执行 GM / Admin player
	 * @param params Optional refresh|inside &lt;name&gt;。
	 */
	@Override
	public void execute(Player admin, String... params) {
		Creature target;
		if (admin.getTarget() == null || !(admin.getTarget() instanceof Creature))
			target = admin;
		else
			target = (Creature) admin.getTarget();
		if (params.length == 0) {
			List<ZoneInstance> zones = target.getPosition().getMapRegion().getZones(target);
			if (zones.isEmpty()) {
				PacketSendUtility.sendMessage(admin, target.getName() + " are out of any zone");
			}
			else {
				PacketSendUtility.sendMessage(admin, target.getName() + " are in zone: ");
				PacketSendUtility.sendMessage(admin, "Registered zones:");
				if (admin.isInsideZoneType(ZoneType.DAMAGE))
					PacketSendUtility.sendMessage(admin, "DAMAGE");
				if (admin.isInsideZoneType(ZoneType.FLY))
					PacketSendUtility.sendMessage(admin, "FLY");
				if (admin.isInsideZoneType(ZoneType.PVP))
					PacketSendUtility.sendMessage(admin, "PVP");
				if (admin.isInsideZoneType(ZoneType.SIEGE))
					PacketSendUtility.sendMessage(admin, "CASTLE");
				if (admin.isInsideZoneType(ZoneType.WATER))
					PacketSendUtility.sendMessage(admin, "WATER");
				for (ZoneInstance zone : zones) {
					PacketSendUtility.sendMessage(admin, zone.getAreaTemplate().getZoneName().name());
					PacketSendUtility.sendMessage(admin, "Fly: " + zone.canFly() + "; Glide: " + zone.canGlide());
					PacketSendUtility.sendMessage(admin, "Ride: " + zone.canRide() + "; Fly-ride: " + zone.canFlyRide());
					PacketSendUtility.sendMessage(admin, "Kisk: " + zone.canPutKisk() + "; Racall: " + zone.canRecall());
					PacketSendUtility.sendMessage(admin, "Same race duels: " + zone.isSameRaceDuelsAllowed() + "; Other race duels: " + zone.isOtherRaceDuelsAllowed());
					PacketSendUtility.sendMessage(admin, "PvP: " + zone.isPvpAllowed());
				}
			}
		}
		else if ("?".equalsIgnoreCase(params[0])) {
			onFail(admin, null);
		}
		else if ("refresh".equalsIgnoreCase(params[0])) {
			admin.revalidateZones();
		}
		else if ("inside".equalsIgnoreCase(params[0])) {
			try {
				ZoneName name = ZoneName.get(params[1]);
				PacketSendUtility.sendMessage(admin, "isInsideZone: " + admin.isInsideZone(name));
			}
			catch (Exception e) {
				PacketSendUtility.sendMessage(admin, "Zone name missing!");
				PacketSendUtility.sendMessage(admin, "Syntax: //zone inside <zone name> ");
			}
		}
	}

	/**
	 * 参数错误时的用法提示。
	 * Usage hint on invalid parameters.
	 *
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "Syntax: //zone refresh | inside");
	}
}
