package com.aionemu.gameserver.commands.admin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.revive_start_points.WorldReviveStartPoints;
import com.aionemu.gameserver.model.templates.teleport.TelelocationTemplate;
import com.aionemu.gameserver.model.templates.world.WorldMapTemplate;
import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.WorldMapType;

/**
 * 中文传送命令：按国服中文地图/区域名、地图 ID 或 NPC 传送管理员。
 * Chinese move command: teleports the admin by CN-client map/area name, world id or NPC.
 * <p>
 * 中文名来自国服客户端字符串表（client_strings_level.xml 等），与 data/static_data/teleport_location.xml
 * 的 loc_id 对齐后落在 config/administration/teleport_names_zh.txt；服务端只查表与传送，坐标仍以
 * teleport_location.xml 为准，避免两份坐标漂移。
 * Chinese names come from the CN client string tables and are mapped onto teleport_location.xml loc ids in
 * config/administration/teleport_names_zh.txt; the server only looks names up and teleports, so coordinates
 * stay owned by teleport_location.xml.
 */
public class MoveToZh extends AdminCommand {

	/**
	 * 中文地点别名表（相对运行配置目录）；{@code ChineseTeleportNamesTest} 依赖该路径。
	 * Chinese location alias table (relative to the runtime config dir); relied upon by
	 * {@code ChineseTeleportNamesTest}.
	 */
	static final String NAME_TABLE_PATH = "administration/teleport_names_zh.txt";

	/** {@code list} 子命令每条消息显示的名称数量。 / Names per message for the {@code list} subcommand. */
	private static final int LIST_PAGE_SIZE = 8;

	/** {@code list} 子命令最多显示的名称总数。 / Maximum number of names the {@code list} subcommand shows. */
	private static final int LIST_MAX_NAMES = 24;

	/** 纯数字参数（地图 ID / NPC ID）。 / Pure numeric argument (world id / npc id). */
	private static final Pattern NUMERIC = Pattern.compile("\\d+");

	/**
	 * 以中文别名 {@code 移动} 构造命令。
	 * Constructs the command with the Chinese alias {@code 移动}.
	 */
	public MoveToZh() {
		super("移动");
	}

	/**
	 * 分发 npc / list / 地图 ID / 中文地点名四种用法。
	 * Dispatches the npc, list, world id and Chinese location name usages.
	 * @param player 执行 GM / Admin executing the command
	 * @param params 子命令与参数 / Subcommand and arguments
	 */
	@Override
	public void execute(Player player, String... params) {
		if (params == null || params.length == 0) {
			showUsage(player);
			return;
		}

		if (params[0].equalsIgnoreCase("npc")) {
			moveToNpc(player, Arrays.copyOfRange(params, 1, params.length));
			return;
		}
		if (params[0].equalsIgnoreCase("list") || params[0].equals("列表")) {
			listNames(player, String.join("", Arrays.copyOfRange(params, 1, params.length)));
			return;
		}

		// 中文地名没有空格，直接拼接更稳妥；数字参数按地图 ID 处理。
		// CN location names contain no spaces, so join directly; numeric input means a world id.
		String location = String.join("", params);
		if (NUMERIC.matcher(location).matches()) {
			moveToWorld(player, Integer.parseInt(location));
			return;
		}
		moveToName(player, location);
	}

	/**
	 * 传送到当前地图内指定 NPC 的刷点（与 {@code //movetonpc} 等价）。
	 * Teleports to the spawn of the given NPC in the current world (same as {@code //movetonpc}).
	 * @param player 执行 GM / Admin executing the command
	 * @param params NPC ID 或名称 / NPC id or name
	 */
	private void moveToNpc(Player player, String... params) {
		if (params.length == 0) {
			PacketSendUtility.sendMessage(player, "语法：//移动 npc <NPC ID|NPC 名称>");
			return;
		}

		int npcId = 0;
		String npcName = String.join(" ", params);
		if (NUMERIC.matcher(params[0]).matches()) {
			npcId = Integer.parseInt(params[0]);
		}
		else {
			for (NpcTemplate template : DataManager.NPC_DATA.getNpcData().values()) {
				if (template.getName().equalsIgnoreCase(npcName)) {
					npcId = template.getTemplateId();
					break;
				}
			}
			if (npcId == 0) {
				PacketSendUtility.sendMessage(player, "没有找到 NPC：" + npcName);
				return;
			}
		}

		// teleportToNpc 只在当前地图查找刷点，这里先自查一次以便给出明确回显。
		// teleportToNpc only searches the current world, so probe the spawn first for clear feedback.
		if (DataManager.SPAWNS_DATA2.getFirstSpawnByNpcId(player.getWorldId(), npcId) == null) {
			PacketSendUtility.sendMessage(player,
					"当前地图没有 NPC " + npcId + " 的刷点（//移动 npc 与 //movetonpc 一样只搜索当前地图）。");
			return;
		}
		TeleportService2.teleportToNpc(player, npcId);
		PacketSendUtility.sendMessage(player, "正在传送到 NPC " + npcId + "。");
	}

	/**
	 * 按地图 ID 传送：优先使用该地图在中文表里的入口坐标，其次使用世界复活起点。
	 * Teleports by world id: prefers the world's entry coordinates from the CN table, then its revive start point.
	 * @param player 执行 GM / Admin executing the command
	 * @param worldId 世界 ID / World id
	 */
	private void moveToWorld(Player player, int worldId) {
		if (WorldMapType.getWorld(worldId) == null) {
			PacketSendUtility.sendMessage(player,
					"未知地图 ID：" + worldId + "（//移动 list <关键字> 可查询已收录的中文地点名）");
			return;
		}

		Map<String, List<Integer>> table = loadNameTable();
		TelelocationTemplate location = table == null ? null : findWorldEntry(table, worldId);
		if (location != null) {
			teleport(player, location, String.valueOf(worldId));
			return;
		}

		WorldReviveStartPoints startPoint = TeleportService2.getReviveWorldStartPoints(worldId, player.getRace(),
				player.getLevel());
		if (startPoint != null) {
			boolean moved = TeleportService2.teleportTo(player, worldId, startPoint.getX(), startPoint.getY(),
					startPoint.getZ(), startPoint.getH());
			PacketSendUtility.sendMessage(player, moved
					? "已传送到地图 " + worldId + "（" + startPoint.getName() + "）"
					: "传送地图 " + worldId + " 失败（角色可能处于死亡状态）。");
			return;
		}

		PacketSendUtility.sendMessage(player,
				"地图 " + worldId + " 没有可用坐标，请改用 //移动 <中文地点名> 或其它传送命令。");
	}

	/**
	 * 按中文地名传送；没有精确名称或名称跨多个地图时给出候选。
	 * Teleports by Chinese location name, listing candidates when the name is unknown or spans several worlds.
	 * @param player 执行 GM / Admin executing the command
	 * @param name 中文地点名 / Chinese location name
	 */
	private void moveToName(Player player, String name) {
		Map<String, List<Integer>> table = loadNameTable();
		if (table == null) {
			reportMissingTable(player);
			return;
		}

		List<Integer> locIds = table.get(name);
		if (locIds == null) {
			List<String> candidates = new ArrayList<>();
			for (String known : table.keySet()) {
				if (known.contains(name)) {
					candidates.add(known);
				}
			}
			PacketSendUtility.sendMessage(player, candidates.isEmpty()
					? "没有找到地点：" + name + "（//移动 list <关键字> 可查询已收录的中文地点名）"
					: "没有精确匹配 " + name + "，已收录的相近地点：" + String.join(" / ", candidates));
			return;
		}

		List<TelelocationTemplate> templates = resolveTemplates(locIds);
		if (templates.isEmpty()) {
			PacketSendUtility.sendMessage(player, "地点 " + name + " 的传送模板已不存在，请重新生成中文地点表。");
			return;
		}

		Set<Integer> worlds = new LinkedHashSet<>();
		for (TelelocationTemplate template : templates) {
			worlds.add(template.getMapId());
		}
		if (worlds.size() > 1) {
			StringBuilder candidates = new StringBuilder();
			for (TelelocationTemplate template : templates) {
				candidates.append(name).append('@').append(template.getMapId()).append("(loc_id=")
						.append(template.getLocId()).append(") ");
			}
			PacketSendUtility.sendMessage(player,
					name + " 对应多个地图，请改用 //移动 <地图 ID> 指定：" + candidates.toString().trim());
			return;
		}

		TelelocationTemplate location = pickByRace(player, templates);
		if (!hasCoordinates(location)) {
			PacketSendUtility.sendMessage(player, "地点 " + name + " 在服务端没有坐标（客户端飞行传送点，loc_id="
					+ location.getLocId() + "），暂不支持直接传送。");
			return;
		}
		teleport(player, location, name);
	}

	/**
	 * 列出中文地点名（可按关键字过滤），没有服务端坐标的条目会标注。
	 * Lists Chinese location names, optionally filtered, marking entries without server coordinates.
	 * @param player 执行 GM / Admin executing the command
	 * @param keyword 关键字，可为空 / Keyword, may be empty
	 */
	private void listNames(Player player, String keyword) {
		Map<String, List<Integer>> table = loadNameTable();
		if (table == null) {
			reportMissingTable(player);
			return;
		}

		List<String> matched = new ArrayList<>();
		for (Map.Entry<String, List<Integer>> entry : table.entrySet()) {
			if (keyword.isEmpty() || entry.getKey().contains(keyword)) {
				List<TelelocationTemplate> templates = resolveTemplates(entry.getValue());
				boolean movable = false;
				for (TelelocationTemplate template : templates) {
					movable |= hasCoordinates(template);
				}
				matched.add(movable ? entry.getKey() : entry.getKey() + "（无坐标）");
			}
		}
		if (matched.isEmpty()) {
			PacketSendUtility.sendMessage(player, "没有匹配 " + keyword + " 的中文地点名。");
			return;
		}

		int shown = Math.min(matched.size(), LIST_MAX_NAMES);
		PacketSendUtility.sendMessage(player, "匹配 " + (keyword.isEmpty() ? "全部" : keyword) + " 的中文地点名共 "
				+ matched.size() + " 个" + (matched.size() > shown ? "（只显示前 " + shown + " 个，可加关键字缩小范围）" : "")
				+ "：");

		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < shown; i++) {
			buffer.append(matched.get(i)).append(" / ");
			if ((i + 1) % LIST_PAGE_SIZE == 0) {
				PacketSendUtility.sendMessage(player, buffer.toString());
				buffer.setLength(0);
			}
		}
		if (buffer.length() > 0) {
			PacketSendUtility.sendMessage(player, buffer.toString());
		}
	}

	/**
	 * 读取中文地点表（UTF-8，逐行 {@code loc_id<TAB>中文名<TAB>英文名}）。
	 * Loads the Chinese location table (UTF-8, one {@code loc_id<TAB>zh name<TAB>en name} per line).
	 * @return 名称到 loc_id 列表的映射；文件缺失或不可读时返回 null / Name-to-loc-ids map, or null when unreadable
	 */
	private Map<String, List<Integer>> loadNameTable() {
		File file = Config.configFile(NAME_TABLE_PATH);
		List<String> lines;
		try {
			lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
		}
		catch (IOException e) {
			return null;
		}

		Map<String, List<Integer>> table = new LinkedHashMap<>();
		for (String rawLine : lines) {
			String line = rawLine.trim();
			if (line.isEmpty() || line.startsWith("#")) {
				continue;
			}
			String[] columns = line.split("\\t+");
			if (columns.length < 2) {
				continue;
			}
			try {
				int locId = Integer.parseInt(columns[0].trim());
				String name = columns[1].trim();
				if (!name.isEmpty()) {
					table.computeIfAbsent(name, key -> new ArrayList<>()).add(locId);
				}
			}
			catch (NumberFormatException ignored) {
				// 缺列或坏行直接跳过，避免一条脏数据让整张表不可用。
				// Skip malformed rows so a single bad line cannot disable the whole table.
			}
		}
		return table;
	}

	/**
	 * 取地图在中文表里最靠前且带坐标的传送点（loc_id 最小者即地图级入口）。
	 * Returns the world's lowest-loc-id entry that carries coordinates (the map-level entry).
	 * @param table 中文地点表 / Chinese location table
	 * @param worldId 世界 ID / World id
	 * @return 传送点模板或 null / Telelocation template or null
	 */
	private TelelocationTemplate findWorldEntry(Map<String, List<Integer>> table, int worldId) {
		TelelocationTemplate found = null;
		int smallestLocId = Integer.MAX_VALUE;
		for (List<Integer> locIds : table.values()) {
			for (int locId : locIds) {
				if (locId >= smallestLocId) {
					continue;
				}
				TelelocationTemplate template = DataManager.TELELOCATION_DATA.getTelelocationTemplate(locId);
				if (template != null && template.getMapId() == worldId && hasCoordinates(template)) {
					smallestLocId = locId;
					found = template;
				}
			}
		}
		return found;
	}

	/**
	 * 把 loc_id 列表解析为仍然存在的传送模板。
	 * Resolves loc ids into telelocation templates that still exist.
	 * @param locIds 候选 loc_id / Candidate loc ids
	 * @return 传送模板列表 / Template list
	 */
	private List<TelelocationTemplate> resolveTemplates(List<Integer> locIds) {
		List<TelelocationTemplate> templates = new ArrayList<>(locIds.size());
		for (int locId : locIds) {
			TelelocationTemplate template = DataManager.TELELOCATION_DATA.getTelelocationTemplate(locId);
			if (template != null) {
				templates.add(template);
			}
		}
		return templates;
	}

	/**
	 * 同一个中文名可能对应天/魔两条 loc_id，优先取当前阵营的模板。
	 * A Chinese name may map to per-race loc ids; prefer the template of the player's race.
	 * @param player 执行 GM / Admin executing the command
	 * @param templates 同一名称的模板 / Templates sharing the name
	 * @return 选中的模板 / Chosen template
	 */
	private TelelocationTemplate pickByRace(Player player, List<TelelocationTemplate> templates) {
		for (TelelocationTemplate template : templates) {
			String englishName = template.getName() == null ? "" : template.getName();
			boolean elyosOnly = englishName.contains("(Elyos)") && !englishName.contains("(Asmodians)");
			boolean asmodianOnly = englishName.contains("(Asmodians)") && !englishName.contains("(Elyos)");
			if (!elyosOnly && !asmodianOnly) {
				return template;
			}
			if (elyosOnly && player.getRace() == Race.ELYOS) {
				return template;
			}
			if (asmodianOnly && player.getRace() == Race.ASMODIANS) {
				return template;
			}
		}
		return templates.get(0);
	}

	/**
	 * 判断传送模板是否带服务端坐标。
	 * Whether the telelocation template carries server-side coordinates.
	 * @param template 传送点模板 / Telelocation template
	 * @return 任一座标非 0 时为 true / True when any coordinate is non-zero
	 */
	private boolean hasCoordinates(TelelocationTemplate template) {
		return template.getX() != 0 || template.getY() != 0 || template.getZ() != 0;
	}

	/**
	 * 执行传送并回显结果。
	 * Teleports and reports the outcome.
	 * @param player 执行 GM / Admin executing the command
	 * @param location 传送点模板 / Telelocation template
	 * @param label 回显用的地点名 / Label used in the feedback message
	 */
	private void teleport(Player player, TelelocationTemplate location, String label) {
		boolean moved = teleportTo(player, location);
		PacketSendUtility.sendMessage(player, moved
				? "已传送到 " + label + "（地图 " + location.getMapId() + "，loc_id=" + location.getLocId() + "）"
				: "传送 " + label + " 失败（角色可能处于死亡状态）。");
	}

	/**
	 * 传送到传送点坐标；副本地图先取实例并登记玩家。
	 * Teleports to the telelocation coordinates; instance worlds first resolve an instance and register the player.
	 * <p>
	 * 副本地图若只按 {@code instanceId=1} 传送，{@code World.setPosition} 找不到实例会直接返回，
	 * 玩家会被留在没有有效坐标的状态；这里与 GM 面板 {@code CmdTeleportTo} 的处理保持一致。
	 * Teleporting an instance world with {@code instanceId=1} makes {@code World.setPosition} bail out silently and
	 * leaves the player without a valid position, so this mirrors the GM-panel handler {@code CmdTeleportTo}.
	 * @param player 执行 GM / Admin executing the command
	 * @param location 传送点模板 / Telelocation template
	 * @return 是否发起传送 / Whether the teleport was initiated
	 */
	private boolean teleportTo(Player player, TelelocationTemplate location) {
		WorldMapTemplate worldTemplate = DataManager.WORLD_MAPS_DATA.getTemplate(location.getMapId());
		if (worldTemplate != null && worldTemplate.isInstance() && player.getWorldId() != location.getMapId()) {
			WorldMapInstance instance = InstanceService.getNextAvailableInstance(location.getMapId());
			InstanceService.registerPlayerWithInstance(instance, player);
			return TeleportService2.teleportTo(player, location.getMapId(), instance.getInstanceId(), location.getX(),
					location.getY(), location.getZ(), (byte) location.getHeading());
		}
		return TeleportService2.teleportTo(player, location.getMapId(), location.getX(), location.getY(),
				location.getZ(), (byte) location.getHeading());
	}

	/**
	 * 中文地点表缺失时的提示。
	 * Hint shown when the Chinese location table is missing.
	 * @param player 执行 GM / Admin executing the command
	 */
	private void reportMissingTable(Player player) {
		PacketSendUtility.sendMessage(player,
				"没有找到中文地点表 " + NAME_TABLE_PATH + "，请确认运行配置目录已同步该文件（随 config 一起部署）。");
	}

	/**
	 * 打印用法。
	 * Prints the usage.
	 * @param player 执行 GM / Admin executing the command
	 */
	private void showUsage(Player player) {
		PacketSendUtility.sendMessage(player, "语法：//移动 npc <NPC ID|名称>");
		PacketSendUtility.sendMessage(player, "　　　//移动 <地图名|区域名>，例如 //移动 泰奥勃莫斯");
		PacketSendUtility.sendMessage(player, "　　　//移动 <地图 ID>（优先中文表入口坐标，其次世界复活起点）");
		PacketSendUtility.sendMessage(player, "　　　//移动 list [关键字] 查询已收录的中文地点名");
	}

	/**
	 * 参数错误时显示语法。
	 * Shows the usage when parameters are invalid.
	 */
	@Override
	public void onFail(Player player, String message) {
		showUsage(player);
	}
}
