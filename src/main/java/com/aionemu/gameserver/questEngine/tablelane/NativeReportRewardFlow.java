package com.aionemu.gameserver.questEngine.tablelane;

import java.util.List;
import java.util.Optional;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.QuestTemplate;
import com.aionemu.gameserver.model.templates.quest.QuestItems;
import com.aionemu.gameserver.questEngine.definition.QuestDialogAction;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.retail.RetailQuestDriver;
import com.aionemu.gameserver.questEngine.retail.RetailQuestMetadataCompiler;
import com.aionemu.gameserver.services.QuestService;

/**
 * 原版车道的**完成/领奖口**（计划 §6.2 {@code NativeReportRewardFlow} 的完成半边）。
 * <p>
 * 已切换到 native 车道的行不再有 typed XML/IR 模板，因此不能走
 * {@code QuestService.finishQuest(env, reward)}（其首个取数面是 typed 模板，缺模板即不可用，
 * 见 QE-113）。本口用**原版 {@code quest.xml} 行**重建奖励面：
 * <ol>
 *   <li>状态门：仅 {@code REWARD} 态可结算；</li>
 *   <li>奖励面 = {@link RetailQuestDriver#retailMetadataOf(int)}（与生产目录同一条
 *       {@link RetailQuestMetadataCompiler}，逐列读 {@code reward_exp*}/{@code reward_gold*}/
 *       {@code reward_item*}/{@code selectable_reward_item*}/{@code reward_title*}/
 *       {@code *_ext}/职业奖励/随机奖励组）；</li>
 *   <li>结算体 = {@link QuestService#finishQuest(QuestEnv, int, QuestTemplate)}（同一份发放与状态写入：
 *       道具/经验/金币/称号/AP/GP/DP + {@code COMPLETE}/completeCount/reward/completeTime +
 *       客户端同步 + 重复计时）；</li>
 *   <li>fail-closed：缺行、元数据不可用、奖励符号名未解析（{@code reward:*}）一律不发放、不推进。</li>
 * </ol>
 * 奖励窗按钮语义：原版 {@code SELECTED_QUEST_REWARD1..15}(8..22) 是**奖励窗内被选中的选项**。
 * 行只声明一个奖励槽（{@code reward_*1}）时，档位固定为该槽、选项下标由对话动作 id 传递给结算段
 * （结算段按 {@code dialogId - 8} 取可选奖励）；行声明多槽时下标即档位，越界即 fail-closed。
 * 多档行的档位/窗口逐列语义仍属计划 P6 范围，不在本口发明。
 * <p>
 * Retail completion/reward port for the native lane: the reward face is rebuilt from the retail
 * {@code quest.xml} row (the same compiler that builds the production catalog), then settled by the
 * shared {@code QuestService} body. Missing rows, unavailable metadata and unresolved reward symbols
 * fail closed; the reward-window button maps to the selected option, and the tier is the row's first
 * slot whenever the row declares a single reward slot.
 */
public final class NativeReportRewardFlow {

	/** 领奖结论 + 证据码。 / Claim verdict with an evidence code. */
	public record Outcome(boolean completed, String code) {
	}

	/** 原版 {@code quest.xml} 元数据来源（缺行/不可用返回 empty）。 / Retail metadata source. */
	@FunctionalInterface
	public interface MetadataSource {
		Optional<RetailQuestMetadataCompiler.Outcome> metadata(int questId);
	}

	/** 结算执行器（生产 = {@link QuestService#finishQuest(QuestEnv, int, QuestTemplate)}）。 /
	 * Settlement sink (production: the shared {@code QuestService} body). */
	@FunctionalInterface
	public interface CompletionSink {
		boolean complete(QuestEnv env, int rewardTier, QuestTemplate template);
	}

	/** 未解析奖励符号名前缀（原版列里的名字映射不到 id）。 / Prefix of unresolved reward symbols. */
	private static final String REWARD_UNRESOLVED_PREFIX = "reward:";

	private static volatile NativeReportRewardFlow instance;

	private final MetadataSource metadata;
	private final CompletionSink sink;

	/** 生产实例：原版驱动元数据 + 共用结算体。 / Production instance: retail driver metadata + shared settlement. */
	public static NativeReportRewardFlow instance() {
		NativeReportRewardFlow local = instance;
		if (local == null) {
			synchronized (NativeReportRewardFlow.class) {
				local = instance;
				if (local == null) {
					local = new NativeReportRewardFlow(NativeReportRewardFlow::productionMetadata,
						QuestService::finishQuest);
					instance = local;
				}
			}
		}
		return local;
	}

	public NativeReportRewardFlow(MetadataSource metadata, CompletionSink sink) {
		this.metadata = metadata;
		this.sink = sink;
	}

	/**
	 * 生产元数据来源 + 指定结算体（门禁夹具的测试接缝：只替换结算体，不替换数据来源）。
	 * Production metadata source with the given settlement sink (test seam for the family gates:
	 * only the sink is replaced, never the data source).
	 */
	static NativeReportRewardFlow withSink(CompletionSink sink) {
		return new NativeReportRewardFlow(NativeReportRewardFlow::productionMetadata, sink);
	}

	/**
	 * 元数据来源与结算体都可注入（只服务包内单测：typed 目录未装载时用原版驱动元数据）。
	 * Both the metadata source and the settlement sink are injectable (package-private test seam:
	 * use the retail driver metadata when the typed catalog is not booted).
	 */
	static NativeReportRewardFlow forTest(MetadataSource metadata, CompletionSink sink) {
		return new NativeReportRewardFlow(metadata, sink);
	}

	/**
	 * 结算并完成任务行。
	 * Settles the row and completes the quest.
	 * @param env 任务环境（对话动作 id 提供奖励窗选项下标）/ quest environment
	 * @param rewardIndex 奖励窗按钮下标（8..23 段动作的 {@code dialogId - 8}）/ reward-window button index
	 * @return 领奖结论 / the claim verdict
	 */
	public Outcome claim(QuestEnv env, int rewardIndex) {
		Player player = env.getPlayer();
		int questId = env.getQuestId();
		QuestState state = player == null ? null : player.getQuestStateList().getQuestState(questId);
		if (state == null || state.getStatus() != QuestStatus.REWARD) {
			return new Outcome(false, "NATIVE_REWARD_NOT_IN_REWARD");
		}
		RetailQuestMetadataCompiler.Outcome compiled = metadata.metadata(questId).orElse(null);
		if (compiled == null) {
			return new Outcome(false, "NATIVE_REWARD_METADATA_UNAVAILABLE");
		}
		String unresolvedRewards = unresolvedRewards(compiled);
		if (!unresolvedRewards.isEmpty()) {
			// 原版奖励列里有解析不出的符号名 ⇒ 整单不放行（不发放部分奖励、不推进状态）。
			// Any unresolved retail reward symbol fails the whole claim closed.
			return new Outcome(false, "NATIVE_REWARD_UNRESOLVED:" + unresolvedRewards);
		}
		QuestTemplate template = QuestTemplate.fromMetadata(questId, compiled.metadata());
		if (template == null) {
			return new Outcome(false, "NATIVE_REWARD_METADATA_UNAVAILABLE");
		}
		int tier = rewardTier(template, rewardIndex);
		if (tier < 0) {
			return new Outcome(false, "NATIVE_REWARD_TIER_UNRESOLVED");
		}
		String buttonProblem = windowButtonProblem(template, player, rewardIndex);
		if (buttonProblem != null) {
			// 客户端按钮没有原版行声明面 ⇒ 不发奖、不完成（缺声明 fail-closed）。
			// Undeclared reward-window buttons grant nothing and never complete the row.
			return new Outcome(false, "NATIVE_REWARD_BUTTON_UNDECLARED:" + buttonProblem);
		}
		return sink.complete(claimEnv(env, player, questId, rewardIndex), tier, template)
			? new Outcome(true, "NATIVE_REWARD_COMPLETED")
			: new Outcome(false, "NATIVE_REWARD_REJECTED");
	}

	/**
	 * 领奖动作 → 结算体奖励窗语义的归一化（{@code QuestService.getRewardItems} 按 {@code dialogId} 推导
	 * 奖励选项：8..22 选项段取 {@code dialogId-8}；23 = 无选择确认走 {@code extendedRewardIndex}；
	 * 108/110..124 = 自动确认通道）。
	 * <p>
	 * 2026-10-07 实机 13830（任务窗「实时奖励」= 110）：原样传 110 ⇒ {@code selRewIndex = 110-8 = 102}
	 * 越界 ⇒ 任务完成但职业奖励物品<strong>静默未发</strong>。归一化后：自动确认通道映射为
	 * {@code 8 + rewardIndex}（等价「选中第 index 项」），23 保留动作 id 并补齐
	 * {@code extendedRewardIndex = 8 + rewardIndex}；8..22 原样透传。
	 * <p>
	 * Normalises the claim action into the reward-window vocabulary the settlement body understands:
	 * the auto-confirm channel (108/110..124) becomes {@code 8 + rewardIndex}, the no-selection confirm
	 * (23) keeps its id with {@code extendedRewardIndex} filled in, and 8..22 pass through unchanged.
	 */
	static QuestEnv claimEnv(QuestEnv env, Player player, int questId, int rewardIndex) {
		int rewardWindowDialog = env.getDialogId();
		boolean autoConfirm = env.getDialogId() == QuestDialogAction.SELECTED_QUEST_AUTO_REWARD.id()
			|| (env.getDialogId() >= QuestDialogAction.SELECTED_QUEST_AUTO_REWARD1.id()
				&& env.getDialogId() < QuestDialogAction.SELECTED_QUEST_AUTO_REWARD1.id()
					+ QuestDialogAction.AUTO_REWARD_SLOT_COUNT);
		if (autoConfirm) {
			rewardWindowDialog = QuestDialogAction.SELECTED_QUEST_REWARD1.id() + rewardIndex;
		}
		QuestEnv claimEnv = new QuestEnv(env.getVisibleObject(), player, questId, rewardWindowDialog);
		if (env.getDialogId() == QuestDialogAction.SELECTED_QUEST_NOREWARD.id()) {
			claimEnv.setExtendedRewardIndex(QuestDialogAction.SELECTED_QUEST_REWARD1.id() + rewardIndex);
		}
		return claimEnv;
	}

	/**
	 * 奖励档位：单槽行固定首档（按钮下标是档内选项，由结算段按对话 id 取）；多槽行的档位选择
	 * 语义尚未坐实（计划 P6「奖励档位/窗口」）⇒ 返回 -1 fail-closed，不用索引估算档位。
	 * Reward tier: a single-slot row always settles its first slot (the button indexes the option
	 * inside that slot, consumed by the settlement through the dialog id). Multi-slot tier selection
	 * is not yet reversed (plan P6), so it fails closed instead of guessing a tier from the index.
	 */
	static int rewardTier(QuestTemplate template, int rewardIndex) {
		int slots = template.getRewards().size();
		if (slots <= 1) {
			// 无奖励列的行仍按原版完成门收尾（奖励面为空）。 / Rows without reward columns still complete.
			return 0;
		}
		return -1;
	}

	/**
	 * 奖励窗按钮校验：原版行声明的选项面就是客户端按钮的全部合法下标
	 * （可选奖励表长，或职业奖励表长——职业奖励按玩家职业取表，与结算段同源）。
	 * Reward-window button check: the option face declared by the retail row is the complete set of
	 * legal buttons (the selectable list, or the player-class list when the row uses class rewards).
	 */
	static String windowButtonProblem(QuestTemplate template, Player player, int rewardIndex) {
		if (rewardIndex < 0) {
			return "negative index " + rewardIndex;
		}
		if (template.getRewards().isEmpty()) {
			return rewardIndex == 0 ? null : "index " + rewardIndex + " without declared options";
		}
		if (template.isUseSingleClassReward() || template.isUseRepeatedClassReward()) {
			List<QuestItems> classReward = classReward(template, player);
			if (classReward == null || classReward.isEmpty()) {
				// 职业奖励行必须能定位到本职业的原版奖励列，否则不发放也不算完成。
				// A class-reward row must resolve the player's retail class column, else it fails closed.
				return "no class reward list for " + playerClass(player);
			}
			return rewardIndex < classReward.size()
				? null : "index " + rewardIndex + " beyond " + classReward.size() + " class options";
		}
		int options = template.getRewards().get(0).getSelectableRewardItem().size();
		if (options == 0) {
			// 无声明可选奖励 ⇒ 奖励窗只有领取按钮（下标 0）。 / No options: only the claim button.
			return rewardIndex == 0 ? null : "index " + rewardIndex + " without declared options";
		}
		return rewardIndex < options ? null : "index " + rewardIndex + " beyond " + options + " options";
	}

	private static String playerClass(Player player) {
		return player == null || player.getCommonData() == null || player.getCommonData().getPlayerClass() == null
			? "(unknown)" : player.getCommonData().getPlayerClass().name();
	}

	/**
	 * 职业奖励表（原版 {@code {class}_selectable_reward} 列）。与结算段
	 * {@code QuestService} 的职业分支**同一映射**（只认转职后的职业），只用于按钮下标校验。
	 * The class-reward list (retail {@code {class}_selectable_reward} column), the exact mapping the
	 * settlement uses (advanced classes only), consulted here to validate the reward-window button.
	 */
	private static List<QuestItems> classReward(QuestTemplate template, Player player) {
		if (player == null || player.getCommonData() == null) {
			return null;
		}
		PlayerClass playerClass = player.getCommonData().getPlayerClass();
		if (playerClass == null) {
			return null;
		}
		return switch (playerClass) {
			case GLADIATOR -> template.getFighterSelectableReward();
			case TEMPLAR -> template.getKnightSelectableReward();
			case RANGER -> template.getRangerSelectableReward();
			case ASSASSIN -> template.getAssassinSelectableReward();
			case SORCERER -> template.getWizardSelectableReward();
			case SPIRIT_MASTER -> template.getElementalistSelectableReward();
			case CLERIC -> template.getPriestSelectableReward();
			case CHANTER -> template.getChanterSelectableReward();
			case GUNSLINGER -> template.getGunslingerSelectableReward();
			case SONGWEAVER -> template.getSongweaverSelectableReward();
			case AETHERTECH -> template.getAethertechSelectableReward();
			default -> null;
		};
	}

	private static String unresolvedRewards(RetailQuestMetadataCompiler.Outcome compiled) {
		return compiled.unresolved().stream()
			.filter(name -> name.startsWith(REWARD_UNRESOLVED_PREFIX))
			.map(name -> name.substring(REWARD_UNRESOLVED_PREFIX.length()))
			.reduce((left, right) -> left + "," + right)
			.orElse("");
	}

	private static Optional<RetailQuestMetadataCompiler.Outcome> productionMetadata(int questId) {
		return RetailQuestDriver.current().flatMap(driver -> driver.retailMetadataOf(questId));
	}
}
