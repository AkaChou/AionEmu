package com.aionemu.gameserver.questEngine.retail;

import java.util.Map;

/**
 * 混合链段尾过场（客户端 HTML 的 {@code <CutScene id="N"/>}）登记：{@code 任务 id → 可见段下标 → movie id}。
 * <p>
 * 过场声明在**页族尾页**（例如 {@code select1_1}），属于客户端页面自身的数据；引擎不再维护逐任务页梯，
 * 因此把这批声明收拢成一张有据可查的常量账：每条登记都写明客户端页名（Dialogs 解包 HTML 的
 * {@code <CutScene>} 声明）与旧 XML 见证（若该行曾有壳 XML）。段下标 = 可见对话段序
 * （talk/collectitem 计段，hunt/enterarea/enterworld/itemplay/talkfobj 不占段），与
 * {@link RetailQuestDialogPages#stage(int, int, int)} 同序。
 * <p>
 * CutScene (client HTML {@code <CutScene id="N"/>}) registry for mixed-chain stage tails. The
 * declaration lives on the page family's tail page (e.g. {@code select1_1}) and belongs to the client
 * page data itself; the engine no longer keeps a per-quest page ladder, so the declarations are folded
 * into this audited constant ledger — every entry names its client page and, where the row once had a
 * legacy shell XML, its {@code play-movie} witness. The stage index is the visible dialog stage ordinal
 * (talk/collectitem stages only; hunt/enterarea/enterworld/itemplay ride the ladder without a stage)
 * and follows the same order as {@link RetailQuestDialogPages#stage(int, int, int)}.
 */
public final class RetailChainCutscenes {

	/**
	 * 一条段尾过场。 / One stage-tail cutscene.
	 *
	 * @param stageIndex 可见段下标（零基） / zero-based visible stage index
	 * @param movieId    过场 id / the cutscene id
	 * @param clientPage 声明该过场的客户端页名 / the client page declaring the cutscene
	 */
	private record Cutscene(int stageIndex, int movieId, String clientPage) {
	}

	/**
	 * 全量登记（客户端 HTML 声明逐条核对；无壳 XML 的 5.8 新行以 HTML 哈希见证）。
	 * The full registry (every entry checked against the client HTML; 5.8 rows without a shell XML are
	 * witnessed by the HTML itself).
	 */
	private static final Map<Integer, Cutscene> CUTSCENES = Map.ofEntries(
		// 15306/15316：末段页族 select10 的尾页 select10_1（旧壳 XML 已无此两行的 play-movie，HTML 声明为准）。
		// 15306/15316: family select10's tail page select10_1 (no shell XML play-movie survives for these
		// two rows; the HTML declaration is the witness).
		Map.entry(15306, new Cutscene(4, 994, "select10_1")),
		Map.entry(15316, new Cutscene(4, 994, "select10_1")),
		// 15604：唯一 talk 段（页族 select3）的尾页 select3_1；旧壳 XML 有 <play-movie movie-id="1000"/>。
		// 15604: the single talk stage (family select3), tail page select3_1; the shell XML carries
		// <play-movie movie-id="1000"/>.
		Map.entry(15604, new Cutscene(0, 1000, "select3_1")),
		// 15605：第 2 段（页族 select2）的尾页 select2_1；旧壳 XML <play-movie movie-id="1002"/>。
		// 15605: stage 2 (family select2), tail page select2_1; the shell XML carries movie-id 1002.
		Map.entry(15605, new Cutscene(1, 1002, "select2_1")),
		// 15613：第 1 段（页族 select2）的尾页 select2_1；旧壳 XML <play-movie movie-id="875"/>。
		// 15613: stage 1 (family select2), tail page select2_1; the shell XML carries movie-id 875.
		Map.entry(15613, new Cutscene(0, 875, "select2_1")),
		// 16942：第 1 段（页族 select1）的尾页 select1_1；旧壳 XML <play-movie movie-id="899"/>。
		// 16942: stage 1 (family select1), tail page select1_1; the shell XML carries movie-id 899.
		Map.entry(16942, new Cutscene(0, 899, "select1_1")),
		// 25306/25316：末段页族 select10 的尾页 select10_1（同 15306 的 5.8 形，HTML 声明见证）。
		// 25306/25316: family select10's tail page select10_1 (the 15306 shape; HTML witness).
		Map.entry(25306, new Cutscene(4, 866, "select10_1")),
		Map.entry(25316, new Cutscene(4, 866, "select10_1")),
		// 25602：第 2 段（页族 select3）的尾页 select3_1；旧壳 XML <play-movie movie-id="872"/>。
		// 25602: stage 2 (family select3), tail page select3_1; the shell XML carries movie-id 872.
		Map.entry(25602, new Cutscene(1, 872, "select3_1")),
		// 25604：第 1 段（页族 select1）的尾页 select1_1；旧壳 XML <play-movie movie-id="874"/>。
		// 25604: stage 1 (family select1), tail page select1_1; the shell XML carries movie-id 874.
		Map.entry(25604, new Cutscene(0, 874, "select1_1")),
		// 26942：第 1 段（页族 select1）的尾页 select1_1；旧壳 XML <play-movie movie-id="900"/>。
		// 26942: stage 1 (family select1), tail page select1_1; the shell XML carries movie-id 900.
		Map.entry(26942, new Cutscene(0, 900, "select1_1")));

	private RetailChainCutscenes() {
	}

	/**
	 * 返回某任务某可见段声明的段尾过场；未声明为 null。
	 * Returns the stage-tail cutscene declared for the quest's visible stage, or null when undeclared.
	 * @param questId 任务 ID / quest id
	 * @param stageIndex 可见段下标 / visible stage index
	 * @return 过场 id，未声明为 null / the cutscene id, null when undeclared
	 */
	public static Integer movieId(int questId, int stageIndex) {
		Cutscene cutscene = CUTSCENES.get(questId);
		return cutscene != null && cutscene.stageIndex() == stageIndex ? cutscene.movieId() : null;
	}

	/**
	 * 声明过场的客户端页名（测试与审计用，证明登记来自客户端页面而非发明）。
	 * The client page declaring the cutscene (for tests and audits, proving the registry mirrors the
	 * client page rather than inventing one).
	 * @param questId 任务 ID / quest id
	 * @return 客户端页名，未登记为 null / the client page name, null when unregistered
	 */
	static String clientPage(int questId) {
		Cutscene cutscene = CUTSCENES.get(questId);
		return cutscene == null ? null : cutscene.clientPage();
	}

	/** 登记行数（门禁用）。 / The number of registered rows (gate use). */
	static int size() {
		return CUTSCENES.size();
	}
}
