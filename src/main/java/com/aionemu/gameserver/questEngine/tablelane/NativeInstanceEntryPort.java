package com.aionemu.gameserver.questEngine.tablelane;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

import org.w3c.dom.Element;

import com.aionemu.gameserver.questEngine.retail.RetailLedgerXml;

/**
 * 原版副本入口端口：DD 附加动作 case 9（Enter Instance）的落点解析面（偏差修复第九批落面，
 * 2026-10-03）。数据 = 原版 {@code Map/XML/instance_creation.xml}（creationId → worldId +
 * {@code start_point_alias_01/02}）× 原版 {@code Map/Worlds/<world>/world.xml} 的
 * {@code location_alias_list}（别名 → x/y/z/dir），两表均在原版服务器数据内（本批复核推翻
 * 「别名→坐标四路不可达」旧登记）。原版解析器 = {@code WorldDb::LoadInstanceCreation} →
 * {@code WorldBase::GetLocationAliasPoint}：别名在 world.xml 缺失时原版自身只记错误日志、
 * 落点空置（creation 2 = IDElim 的 {@code IDElim_Entrance_alias} 即此内在缺失）⇒
 * {@code resolved=false} 行在 DD 车道维持 fail-closed 冻结。
 * <p>
 * The retail instance-entry port: the landing-position face of the DD Enter Instance extra
 * action (deviation-fix batch 9). Data = the retail instance_creation.xml × the per-world
 * world.xml location_alias_list (both inside the retail server data). The retail resolver
 * (WorldDb::LoadInstanceCreation → WorldBase::GetLocationAliasPoint) logs an error and leaves
 * the start point empty when the alias is missing (creation 2's alias is intrinsically absent
 * in the retail idelim world file) ⇒ resolved=false rows stay fail-closed frozen.
 */
public final class NativeInstanceEntryPort {

	/** 原版副本入口表资源路径。 / The retail instance-entry table resource. */
	public static final String TABLE_RESOURCE =
		"/aion/data/static_data/quest/retail/retail-instance-entry-points.xml";

	/**
	 * 一个副本入口落点。{@code resolved=false} = 原版世界文件本就无此别名（内在缺失，禁用）。
	 * One entry point; {@code resolved=false} = the alias is intrinsically absent in the retail
	 * world file (unusable).
	 */
	public record EntryPoint(int creationId, int worldId, String alias, float x, float y, float z, int heading,
			boolean resolved) {
	}

	private static volatile NativeInstanceEntryPort instance;

	private final Map<Integer, EntryPoint> byCreationId;

	private NativeInstanceEntryPort(Map<Integer, EntryPoint> byCreationId) {
		this.byCreationId = byCreationId;
	}

	/** 生产单例（类路径表；缺表 = 装载失败）。 / The production singleton (missing table = load failure). */
	public static NativeInstanceEntryPort instance() {
		NativeInstanceEntryPort local = instance;
		if (local == null) {
			synchronized (NativeInstanceEntryPort.class) {
				local = instance;
				if (local == null) {
					local = load();
					instance = local;
				}
			}
		}
		return local;
	}

	static NativeInstanceEntryPort load() {
		Map<Integer, EntryPoint> byCreationId = new java.util.LinkedHashMap<>();
		try (InputStream input = NativeInstanceEntryPort.class.getResourceAsStream(TABLE_RESOURCE)) {
			if (input == null) {
				throw new IllegalStateException("INSTANCE_ENTRY_TABLE_MISSING: " + TABLE_RESOURCE);
			}
			// 旧 TSV「少于 9 列即 malformed」语义保留：九个列元素任一缺席即 MALFORMED
			// （source 列装载器不读，但其存在性属旧列数检查的一部分）。
			// The old "fewer than nine columns is malformed" rule is preserved: any of the nine
			// column elements missing is MALFORMED (the source column is unread by the loader, but
			// its presence was part of the old column-count check).
			for (Element row : RetailLedgerXml.rows(RetailLedgerXml.parse(input), "entry_point")) {
				String creationIdText = RetailLedgerXml.text(row, "creation_id");
				String worldId = RetailLedgerXml.text(row, "world_id");
				String alias = RetailLedgerXml.text(row, "alias");
				String x = RetailLedgerXml.text(row, "x");
				String y = RetailLedgerXml.text(row, "y");
				String z = RetailLedgerXml.text(row, "z");
				String heading = RetailLedgerXml.text(row, "heading");
				String resolved = RetailLedgerXml.text(row, "resolved");
				String source = RetailLedgerXml.text(row, "source");
				if (creationIdText == null || worldId == null || alias == null || x == null || y == null
						|| z == null || heading == null || resolved == null || source == null) {
					throw new IllegalStateException("INSTANCE_ENTRY_TABLE_MALFORMED: creation_id=" + creationIdText);
				}
				int creationId = Integer.parseInt(creationIdText);
				EntryPoint entry = new EntryPoint(creationId, Integer.parseInt(worldId), alias,
					Float.parseFloat(x), Float.parseFloat(y), Float.parseFloat(z), Integer.parseInt(heading),
					Boolean.parseBoolean(resolved));
				byCreationId.put(creationId, entry);
			}
		} catch (IOException e) {
			throw new IllegalStateException("INSTANCE_ENTRY_TABLE_UNREADABLE: " + TABLE_RESOURCE, e);
		}
		return new NativeInstanceEntryPort(Map.copyOf(byCreationId));
	}

	/**
	 * 按 creationId 查入口（无行 = Optional.empty）。
	 * The entry by creationId (absent row = Optional.empty).
	 */
	public Optional<EntryPoint> entry(int creationId) {
		return Optional.ofNullable(byCreationId.get(creationId));
	}

	/** 全表（门禁断言面）。 / All rows (the gate-assertion face). */
	public Map<Integer, EntryPoint> all() {
		return byCreationId;
	}
}
