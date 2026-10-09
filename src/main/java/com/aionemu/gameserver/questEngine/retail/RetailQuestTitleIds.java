package com.aionemu.gameserver.questEngine.retail;

import java.util.HashMap;
import java.util.Map;

/**
 * 原版称号符号名（reward_titleN，如 light_title04）→ 称号 id 映射。
 * <p>
 * 原版 quest.xml 只写符号名，运行时称号奖励是数字 id；本表来自
 * "reward_titleN × 生产 XML TITLE id" 全库投票（无歧义，生成与复核脚本见
 * .agents/summary/scriptdll-quest-driver/）。原版称号表本身未入仓，此为已验证快照。
 * Maps retail title symbol names to runtime title ids (verified unambiguous library-wide).
 */
public final class RetailQuestTitleIds {

	private static final Map<String, Integer> IDS;

	static {
		Map<String, Integer> ids = new HashMap<>();
		ids.put("dark_title01", 51);
		ids.put("dark_title02", 52);
		ids.put("dark_title03", 53);
		ids.put("dark_title04", 54);
		ids.put("dark_title05", 55);
		ids.put("dark_title06", 56);
		ids.put("dark_title07", 57);
		ids.put("dark_title08", 58);
		ids.put("dark_title09", 59);
		ids.put("dark_title10", 60);
		ids.put("dark_title11", 61);
		ids.put("dark_title12", 62);
		ids.put("dark_title13", 63);
		ids.put("dark_title14", 64);
		ids.put("dark_title15", 65);
		ids.put("dark_title16", 66);
		ids.put("dark_title17", 67);
		ids.put("dark_title18", 68);
		ids.put("dark_title19", 69);
		ids.put("dark_title20", 70);
		ids.put("dark_title21", 71);
		ids.put("dark_title22", 72);
		ids.put("dark_title23", 73);
		ids.put("dark_title24", 74);
		ids.put("dark_title25", 75);
		ids.put("dark_title250", 250);
		ids.put("dark_title26", 76);
		ids.put("dark_title27", 77);
		ids.put("dark_title28", 78);
		ids.put("dark_title29", 79);
		ids.put("dark_title294", 294);
		ids.put("dark_title295", 302);
		ids.put("dark_title30", 80);
		ids.put("dark_title31", 81);
		ids.put("dark_title32", 82);
		ids.put("dark_title33", 83);
		ids.put("dark_title34", 84);
		ids.put("dark_title35", 85);
		ids.put("dark_title36", 86);
		ids.put("dark_title37", 87);
		ids.put("dark_title38", 88);
		ids.put("dark_title39", 89);
		ids.put("dark_title41", 91);
		ids.put("dark_title42", 92);
		ids.put("dark_title44", 94);
		ids.put("dark_title45", 95);
		ids.put("dark_title46", 96);
		ids.put("dark_title47", 97);
		ids.put("dark_title48", 98);
		ids.put("dark_title49", 99);
		ids.put("dark_title50", 100);
		ids.put("dark_title51", 126);
		ids.put("dark_title52", 127);
		ids.put("dark_title55", 130);
		ids.put("dark_title56", 131);
		ids.put("dark_title57", 132);
		ids.put("dark_title71", 150);
		ids.put("dark_title75", 169);
		ids.put("dark_title76", 170);
		ids.put("dark_title77", 171);
		ids.put("dark_title78", 172);
		ids.put("dark_title79", 173);
		ids.put("dark_title80", 175);
		ids.put("dark_title81", 184);
		ids.put("dark_title87", 217);
		ids.put("dark_title88", 218);
		ids.put("dark_title92", 222);
		ids.put("light_title01", 1);
		ids.put("light_title02", 2);
		ids.put("light_title03", 3);
		ids.put("light_title04", 4);
		ids.put("light_title05", 5);
		ids.put("light_title06", 6);
		ids.put("light_title07", 7);
		ids.put("light_title08", 8);
		ids.put("light_title09", 9);
		ids.put("light_title10", 10);
		ids.put("light_title11", 11);
		ids.put("light_title12", 12);
		ids.put("light_title13", 13);
		ids.put("light_title14", 14);
		ids.put("light_title15", 15);
		ids.put("light_title16", 16);
		ids.put("light_title17", 17);
		ids.put("light_title18", 18);
		ids.put("light_title19", 19);
		ids.put("light_title20", 20);
		ids.put("light_title21", 21);
		ids.put("light_title22", 22);
		ids.put("light_title23", 23);
		ids.put("light_title24", 24);
		ids.put("light_title249", 249);
		ids.put("light_title25", 25);
		ids.put("light_title26", 26);
		ids.put("light_title27", 27);
		ids.put("light_title28", 28);
		ids.put("light_title29", 29);
		ids.put("light_title293", 293);
		ids.put("light_title295", 301);
		ids.put("light_title30", 30);
		ids.put("light_title31", 31);
		ids.put("light_title32", 32);
		ids.put("light_title33", 33);
		ids.put("light_title34", 34);
		ids.put("light_title35", 35);
		ids.put("light_title36", 36);
		ids.put("light_title37", 37);
		ids.put("light_title38", 38);
		ids.put("light_title39", 39);
		ids.put("light_title41", 41);
		ids.put("light_title42", 42);
		ids.put("light_title44", 44);
		ids.put("light_title45", 45);
		ids.put("light_title46", 46);
		ids.put("light_title47", 47);
		ids.put("light_title48", 48);
		ids.put("light_title49", 49);
		ids.put("light_title50", 50);
		ids.put("light_title51", 107);
		ids.put("light_title52", 108);
		ids.put("light_title55", 111);
		ids.put("light_title56", 112);
		ids.put("light_title57", 113);
		ids.put("light_title71", 149);
		ids.put("light_title75", 164);
		ids.put("light_title76", 165);
		ids.put("light_title77", 166);
		ids.put("light_title78", 167);
		ids.put("light_title79", 168);
		ids.put("light_title80", 174);
		ids.put("light_title81", 183);
		ids.put("light_title87", 206);
		ids.put("light_title88", 207);
		ids.put("light_title92", 211);
		ids.put("new_deva_title01", 306);
		ids.put("vip_dark_title01", 244);
		ids.put("vip_dark_title02", 245);
		ids.put("vip_dark_title03", 246);
		ids.put("vip_dark_title04", 247);
		ids.put("vip_dark_title05", 248);
		ids.put("vip_light_title01", 239);
		ids.put("vip_light_title02", 240);
		ids.put("vip_light_title03", 241);
		ids.put("vip_light_title04", 242);
		ids.put("vip_light_title05", 243);
		IDS = Map.copyOf(ids);
	}

	private RetailQuestTitleIds() {
	}

	/** 解析称号 id；未知符号名返回 null。 / Resolves a title id, or null for unknown names. */
	public static Integer idOf(String name) {
		return name == null ? null : IDS.get(name.trim());
	}

	public static int size() {
		return IDS.size();
	}
}
