package com.aionemu.gameserver.questEngine.retail;

import java.io.InputStream;
import java.util.List;

/**
 * 版本化 NPC id 别名表入口，供真端目标名到生产 npc_id 的直连映射使用。
 * <p>
 * Versioned direct npc-id aliases for retail target names that cannot be derived from the template index.
 */
public final class RetailNpcNameAliases {

	private RetailNpcNameAliases() {
	}

	private static InputStream openStream() {
		InputStream input = RetailNpcNameAliases.class.getResourceAsStream(
			"/aion/data/static_data/quest/retail/retail-npc-name-aliases.tsv");
		if (input == null) {
			throw new IllegalStateException("missing versioned retail npc name alias table");
		}
		return input;
	}

	/** 获取版本化别名表流。 / Returns the versioned alias table stream. */
	public static List<InputStream> streams() {
		return List.of(openStream());
	}
}
