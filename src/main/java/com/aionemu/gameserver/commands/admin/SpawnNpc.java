package com.aionemu.gameserver.commands.admin;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

import java.io.IOException;

/**
 * NPC 生成指令；在管理员位置按模板 ID 生成单位，支持 {@code <npcid>*<num>} 与 {@code *<num>} 批量写法，并可持久化到刷怪数据。
 * Admin command that spawns a template at the admin position, supports the {@code <npcid>*<num>} and
 * {@code *<num>} batch forms, and optionally persists the spawn.
 * @author Luno
 */
@Slf4j
public class SpawnNpc extends AdminCommand {

	/**
	 * 单次批量生成的数量上限，避免误输入导致服务端卡顿。
	 * Maximum number of NPCs spawned in one batch, so a mistyped count cannot stall the server.
	 */
	private static final int MAX_BATCH_COUNT = 100;

	/**
	 * 非批量刷怪的默认重生时间（秒）。
	 * Default respawn time, in seconds, for a non-batch spawn.
	 */
	private static final int DEFAULT_RESPAWN_TIME = 295;

	public SpawnNpc() {
		super("spawn");
	}

	/**
	 * 解析后的刷怪目标。
	 * Parsed spawn target.
	 * @param templateId NPC 模板 ID；{@code null} 表示取管理员当前选中的目标
	 *                   / npc template id; {@code null} means the admin's currently selected target
	 * @param count 生成数量，至少为 1 / spawn count, at least 1
	 * @param batch 是否使用了 {@code *} 批量写法 / whether the {@code *} batch form was used
	 */
	record SpawnTarget(Integer templateId, int count, boolean batch) {
	}

	/**
	 * 解析刷怪目标参数，支持 {@code <npcid>}、{@code <npcid>*<num>} 与 {@code *<num>} 三种写法。
	 * Parses the spawn target argument, accepting the {@code <npcid>}, {@code <npcid>*<num>} and
	 * {@code *<num>} forms.
	 * <p>
	 * 各段均容错首尾空白；任何一段不是整数，或数量超出 [1, {@value #MAX_BATCH_COUNT}]，一律判为非法。
	 * {@code *<num>} 写法本身不携带模板 ID，该值由调用方从管理员选中目标解析。
	 * Every part tolerates surrounding whitespace; a non-integer part, or a count outside
	 * [1, {@value #MAX_BATCH_COUNT}], is rejected as malformed. The {@code *<num>} form carries no template
	 * id of its own; the caller resolves it from the admin's selected target.
	 * @param raw 原始参数 / raw argument
	 * @return 解析结果；格式非法时返回 {@code null} / parse result, or {@code null} when malformed
	 */
	static SpawnTarget parseTarget(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}

		int starIndex = raw.indexOf('*');
		try {
			if (starIndex < 0) {
				return new SpawnTarget(Integer.parseInt(raw.trim()), 1, false);
			}

			int count = Integer.parseInt(raw.substring(starIndex + 1).trim());
			if (count < 1 || count > MAX_BATCH_COUNT) {
				return null;
			}

			String idPart = raw.substring(0, starIndex).trim();
			return idPart.isEmpty()
					? new SpawnTarget(null, count, true)
					: new SpawnTarget(Integer.parseInt(idPart), count, true);
		}
		catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * 执行该管理指令。
	 * Executes this admin command.
	 * <p>
	 * {@code //spawn <npcid> [respawn_time]} 生成单个单位；{@code //spawn <npcid>*<num> [respawn_time]}
	 * 与 {@code //spawn *<num> [respawn_time]}（模板取自当前选中目标）在管理员位置一次生成 {@code num}
	 * 个完全重叠的单位，未显式给出重生时间时按临时刷怪处理（不写盘）。
	 * {@code //spawn <npcid> [respawn_time]} spawns a single unit; {@code //spawn <npcid>*<num> [respawn_time]}
	 * and {@code //spawn *<num> [respawn_time]} (template taken from the currently selected target) spawn
	 * {@code num} fully overlapping units at the admin position and treat them as temporary (not persisted)
	 * unless a respawn time is given explicitly.
	 * @param admin 执行指令的管理员 / admin executing the command
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params.length < 1) {
			onFail(admin, null);
			return;
		}

		SpawnTarget target = parseTarget(params[0]);
		if (target == null) {
			onFail(admin, null);
			return;
		}

		Integer templateId = target.templateId();

		if (templateId == null) {
			VisibleObject selected = admin.getTarget();
			if (!(selected instanceof Npc npc)) {
				PacketSendUtility.sendMessage(admin, "Select an NPC target first, or use //spawn <id>*<num>.");
				return;
			}
			templateId = npc.getNpcId();
		}

		int respawnTime = target.batch() ? 0 : DEFAULT_RESPAWN_TIME;

		if (params.length >= 2) {
			try {
				respawnTime = Integer.parseInt(params[1].trim());
			}
			catch (NumberFormatException e) {
				onFail(admin, null);
				return;
			}
		}

		float x = admin.getX();
		float y = admin.getY();
		float z = admin.getZ();
		byte heading = admin.getHeading();
		int worldId = admin.getWorldId();

		VisibleObject firstSpawned = null;
		int spawned = 0;

		for (int i = 0; i < target.count(); i++) {
			SpawnTemplate spawn = SpawnEngine.addNewSpawn(worldId, templateId, x, y, z, heading, respawnTime);

			if (spawn == null) {
				break;
			}

			VisibleObject visibleObject = SpawnEngine.spawnObject(spawn, admin.getInstanceId());

			if (visibleObject == null) {
				break;
			}

			firstSpawned = visibleObject;
			spawned++;

			if (respawnTime > 0) {
				try {
					DataManager.SPAWNS_DATA2.saveSpawn(admin, visibleObject, false);
				}
				catch (IOException e) {
					log.error(I18n.get("log.b9f71a8a3b96", visibleObject.getObjectId()), e);
					PacketSendUtility.sendMessage(admin, "Could not save spawn");
					break;
				}
			}
		}

		if (firstSpawned == null) {
			PacketSendUtility.sendMessage(admin, "There is no template with id " + templateId);
			return;
		}

		String objectName = firstSpawned.getObjectTemplate().getName();
		PacketSendUtility.sendMessage(admin,
				target.batch() ? objectName + " spawned x" + spawned : objectName + " spawned");
	}

	/**
	 * 参数错误时输出用法。
	 * Prints usage when arguments are invalid.
	 * @param player 接收提示的玩家 / player receiving the message
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "syntax //spawn <template_id> [respawn_time] | "
				+ "//spawn <template_id>*<num> [respawn_time] (0 for temp)");
		PacketSendUtility.sendMessage(player, "       //spawn *<num> [respawn_time]: "
				+ "spawns copies of the selected target");
	}
}
