package com.aionemu.gameserver.network.loginserver.serverpackets;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.MacroList;
import com.aionemu.gameserver.model.gameobjects.player.PetCommonData;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerAppearance;
import com.aionemu.gameserver.model.gameobjects.player.PlayerSettings;
import com.aionemu.gameserver.model.gameobjects.player.QuestStateList;
import com.aionemu.gameserver.model.gameobjects.player.RecipeList;
import com.aionemu.gameserver.model.gameobjects.player.emotion.Emotion;
import com.aionemu.gameserver.model.gameobjects.player.emotion.EmotionList;
import com.aionemu.gameserver.model.gameobjects.player.motion.Motion;
import com.aionemu.gameserver.model.gameobjects.player.motion.MotionList;
import com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFaction;
import com.aionemu.gameserver.model.gameobjects.player.npcFaction.NpcFactions;
import com.aionemu.gameserver.model.gameobjects.player.title.Title;
import com.aionemu.gameserver.model.gameobjects.player.title.TitleList;
import com.aionemu.gameserver.model.items.GodStone;
import com.aionemu.gameserver.model.items.ManaStone;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.model.skill.PlayerSkillEntry;
import com.aionemu.gameserver.model.skill.PlayerSkillList;
import com.aionemu.gameserver.network.loginserver.LoginServerConnection;
import com.aionemu.gameserver.network.loginserver.LsServerPacket;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.services.transfers.TransferablePlayer;

/**
 * 游戏服向登录服发送玩家跨服/角色转移控制数据的服务端包。
 * Server packet that transfers player cross-server/character transfer control data to the login server.
 */
@Slf4j
public class SM_PTRANSFER_CONTROL extends LsServerPacket {
	public static final byte CHARACTER_INFORMATION = 1;
	public static final byte ERROR = 2;
	public static final byte OK = 3;
	public static final byte TASK_STOP = 4;
	private byte type;
	private Player player;
	private String result;
	private int taskId;

	/**
	 * 构造仅含任务 ID 的转移控制包。
	 * Constructs a transfer-control packet with task id only.
	 *
	 * @param type 控制类型 / control type
	 * @param taskId 任务 ID / task id
	 */
	public SM_PTRANSFER_CONTROL(byte type, int taskId) {
		super(14);
		this.type = type;
		this.taskId = taskId;
	}

	/**
	 * 构造携带可转移玩家数据的控制包。
	 * Constructs a transfer-control packet with transferable player data.
	 *
	 * @param type 控制类型 / control type
	 * @param tp 可转移玩家 / transferable player
	 */
	public SM_PTRANSFER_CONTROL(byte type, TransferablePlayer tp) {
		super(14);
		this.type = type;
		this.taskId = tp.taskId;
		this.player = tp.player;
	}

	/**
	 * 构造携带可转移玩家与结果文本的控制包。
	 * Constructs a transfer-control packet with transferable player and result text.
	 *
	 * @param type 控制类型 / control type
	 * @param tp 可转移玩家 / transferable player
	 * @param result 结果文本 / result text
	 */
	public SM_PTRANSFER_CONTROL(byte type, TransferablePlayer tp, String result) {
		super(14);
		this.type = type;
		this.result = result;
	}

	/**
	 * 构造携带任务 ID 与结果文本的控制包。
	 * Constructs a transfer-control packet with task id and result text.
	 *
	 * @param type 控制类型 / control type
	 * @param taskId 任务 ID / task id
	 * @param result 结果文本 / result text
	 */
	public SM_PTRANSFER_CONTROL(byte type, int taskId, String result) {
		super(14);
		this.type = type;
		this.taskId = taskId;
		this.result = result;
	}

	/**
	 * 按控制类型写入任务状态或完整角色转移数据。
	 * Writes task status or full character-transfer payload by control type.
	 */
	@Override
	protected void writeImpl(LoginServerConnection con) {
		writeC(type);
		switch (type) {
		case OK:
			writeD(taskId);
			break;
		case ERROR:
			writeD(taskId);
			writeS(result);
			break;
		case TASK_STOP:
			writeD(taskId);
			writeS(result);
			break;
		case CHARACTER_INFORMATION: {
			writeD(taskId);
			writeS(player.getName());
			writeQ(player.getCommonData().getExp());
			writeD(player.getPlayerClass().getClassId());
			writeD(player.getRace().getRaceId());
			writeD(player.getCommonData().getGender().getGenderId());
			writeD(player.getCommonData().getTitleId());
			writeD(player.getCommonData().getDp());
			writeD(player.getCommonData().getQuestExpands());
			writeD(player.getCommonData().getNpcExpands());
			writeD(player.getCommonData().getAdvancedStigmaSlotSize());
			writeD(player.getCommonData().getWarehouseSize());
			PlayerAppearance playerAppearance = player.getPlayerAppearance();
			writeD(playerAppearance.getSkinRGB());
			writeD(playerAppearance.getHairRGB());
			writeD(playerAppearance.getEyeRGB());
			writeD(playerAppearance.getLipRGB());
			writeC(playerAppearance.getFace());
			writeC(playerAppearance.getHair());
			writeC(playerAppearance.getDeco());
			writeC(playerAppearance.getTattoo());
			writeC(playerAppearance.getFaceContour());
			writeC(playerAppearance.getExpression());
			writeC(playerAppearance.getJawLine());
			writeC(playerAppearance.getForehead());
			writeC(playerAppearance.getEyeHeight());
			writeC(playerAppearance.getEyeSpace());
			writeC(playerAppearance.getEyeWidth());
			writeC(playerAppearance.getEyeSize());
			writeC(playerAppearance.getEyeShape());
			writeC(playerAppearance.getEyeAngle());
			writeC(playerAppearance.getBrowHeight());
			writeC(playerAppearance.getBrowAngle());
			writeC(playerAppearance.getBrowShape());
			writeC(playerAppearance.getNose());
			writeC(playerAppearance.getNoseBridge());
			writeC(playerAppearance.getNoseWidth());
			writeC(playerAppearance.getNoseTip());
			writeC(playerAppearance.getCheek());
			writeC(playerAppearance.getLipHeight());
			writeC(playerAppearance.getMouthSize());
			writeC(playerAppearance.getLipSize());
			writeC(playerAppearance.getSmile());
			writeC(playerAppearance.getLipShape());
			writeC(playerAppearance.getJawHeigh());
			writeC(playerAppearance.getChinJut());
			writeC(playerAppearance.getEarShape());
			writeC(playerAppearance.getHeadSize());
			writeC(playerAppearance.getNeck());
			writeC(playerAppearance.getNeckLength());
			writeC(playerAppearance.getShoulderSize());
			writeC(playerAppearance.getTorso());
			writeC(playerAppearance.getChest()); // 仅女性 / only woman
			writeC(playerAppearance.getWaist());
			writeC(playerAppearance.getHips());
			writeC(playerAppearance.getArmThickness());
			writeC(playerAppearance.getHandSize());
			writeC(playerAppearance.getLegThickness());
			writeC(playerAppearance.getFootSize());
			writeC(playerAppearance.getFacialRate());
			writeC(playerAppearance.getArmLength());
			writeC(playerAppearance.getLegLength());
			writeC(playerAppearance.getShoulders());
			writeC(playerAppearance.getFaceShape());
			writeC(playerAppearance.getVoice());
			writeF(playerAppearance.getHeight());
			writeF(player.getX());
			writeF(player.getY());
			writeF(player.getZ());
			writeC(player.getHeading());
			writeD(player.getWorldId());
			// 背包 / inventory
			List<Item> inv = DAOManager.getDAO(InventoryDAO.class).loadStorageDirect(player.getObjectId(),
					StorageType.CUBE);
			writeD(inv.size());
			ItemService.loadItemStones(inv);
			for (Item item : inv) {
				writeD(item.getObjectId());
				writeD(item.getItemId());
				writeQ(item.getItemCount());
				writeD(item.getItemColor());
				writeS(item.getItemCreator());
				writeD(item.getExpireTime());
				writeD(item.getActivationCount());
				writeD(item.isEquipped() ? 1 : 0);
				writeD(item.isSoulBound() ? 1 : 0);
				writeQ(item.getEquipmentSlot());
				writeD(item.getItemLocation());
				writeD(item.getEnchantLevel());
				writeD(item.getItemSkinTemplate().getTemplateId());
				writeD(item.getFusionedItemId());
				writeD(item.getOptionalSocket());
				writeD(item.getOptionalFusionSocket());
				writeD(item.getChargePoints());
				Set<ManaStone> itemStones = item.getItemStones();
				writeD(itemStones.size());
				for (ManaStone stone : itemStones) {
					writeD(stone.getItemId());
					writeD(stone.getSlot());
				}
				itemStones = item.getFusionStones();
				writeD(itemStones.size());
				for (ManaStone stone : itemStones) {
					writeD(stone.getItemId());
					writeD(stone.getSlot());
				}
				GodStone stone = item.getGodStone();
				writeC(stone == null ? 0 : 1);
				if (stone != null) {
					writeD(stone.getItemId());
				}
				writeD(item.getColorExpireTime());
				writeD(item.getBonusNumber());
				writeD(item.getRandomCount());
			}
			inv = DAOManager.getDAO(InventoryDAO.class).loadStorageDirect(player.getObjectId(),
					StorageType.REGULAR_WAREHOUSE);
			ItemService.loadItemStones(inv);
			writeD(inv.size());
			for (Item item : inv) {
				writeD(item.getObjectId());
				writeD(item.getItemId());
				writeQ(item.getItemCount());
				writeD(item.getItemColor());
				writeS(item.getItemCreator());
				writeD(item.getExpireTime());
				writeD(item.getActivationCount());
				writeD(item.isEquipped() ? 1 : 0);
				writeD(item.isSoulBound() ? 1 : 0);
				writeQ(item.getEquipmentSlot());
				writeD(item.getItemLocation());
				writeD(item.getEnchantLevel());
				writeD(item.getItemSkinTemplate().getTemplateId());
				writeD(item.getFusionedItemId());
				writeD(item.getOptionalSocket());
				writeD(item.getOptionalFusionSocket());
				writeD(item.getChargePoints());
				Set<ManaStone> itemStones = item.getItemStones();
				writeD(itemStones.size());
				for (ManaStone stone : itemStones) {
					writeD(stone.getItemId());
					writeD(stone.getSlot());
				}
				itemStones = item.getFusionStones();
				writeD(itemStones.size());
				for (ManaStone stone : itemStones) {
					writeD(stone.getItemId());
					writeD(stone.getSlot());
				}
				GodStone stone = item.getGodStone();
				writeC(stone == null ? 0 : 1);
				if (stone != null) {
					writeD(stone.getItemId());
				}
				writeD(item.getColorExpireTime());
				writeD(item.getBonusNumber());
				writeD(item.getRandomCount());
			}
			EmotionList emo = player.getEmotions();
			writeD(emo.getEmotions().size());
			for (Emotion e : emo.getEmotions()) {
				writeD(e.getId());
				writeD(e.getRemainingTime());
			}
			MotionList motions = player.getMotions();
			writeD(motions.getMotions().size());
			for (Motion motion : motions.getMotions().values()) {
				writeD(motion.getId());
				writeD(motion.getExpireTime());
				writeC(motion.isActive() ? 1 : 0);
			}
			MacroList macro = player.getMacroList();
			writeD(macro.getMacrosses().size());
			for (Entry<Integer, String> m : macro.getMacrosses().entrySet()) {
				writeD(m.getKey());
				writeS(m.getValue());
			}
			NpcFactions nf = player.getNpcFactions();
			writeD(nf.getNpcFactions().size());
			for (NpcFaction f : nf.getNpcFactions()) {
				writeD(f.getId());
				writeD(f.getTime());
				writeD(f.isActive() ? 1 : 0);
				writeS(f.getState().toString());
				writeD(f.getQuestId());
			}
			Collection<PetCommonData> pets = player.getPetList().getPets();
			writeD(pets.size());
			for (PetCommonData pet : pets) {
				writeD(pet.getPetId());
				writeD(pet.getDecoration());
				long birthday = pet.getBirthdayTimestamp() == null ? 0 : pet.getBirthdayTimestamp().getTime();
				writeQ(birthday);
				writeS(pet.getName());
			}
			RecipeList rec = player.getRecipeList();
			writeD(rec.getRecipeList().size());
			for (int id : rec.getRecipeList()) {
				writeD(id);
			}
			PlayerSkillList skillList = player.getSkillList();
			// 丢弃烙印之石技能 / discard stigma skills
			List<PlayerSkillEntry> skills = new ArrayList<PlayerSkillEntry>();
			for (PlayerSkillEntry sk : skillList.getAllSkills()) {
				if (!sk.isStigma()) {
					skills.add(sk);
				}
			}
			writeD(skills.size());
			for (PlayerSkillEntry sk : skills) {
				writeD(sk.getSkillId());
				writeD(sk.getSkillLevel());
			}
			TitleList titles = player.getTitleList();
			writeD(titles.getTitles().size());
			for (Title t : titles.getTitles()) {
				writeD(t.getId());
				writeD(t.getRemainingTime());
			}
			PlayerSettings ps = player.getPlayerSettings();
			writeD(ps.getUiSettings() == null ? 0 : ps.getUiSettings().length);
			writeD(ps.getShortcuts() == null ? 0 : ps.getShortcuts().length);
			if (ps.getUiSettings() != null) {
				writeB(ps.getUiSettings());
			}
			if (ps.getShortcuts() != null) {
				writeB(ps.getShortcuts());
			}
			writeD(ps.getDeny());
			writeD(ps.getDisplay());
			QuestStateList qsl = player.getQuestStateList();
			List<QuestState> quests = new ArrayList<QuestState>();
			for (QuestState qs : qsl.getQuests().values()) {
				if (qs == null) {
					log.warn(I18n.get("log.ae410cf2be90", player.getName(), taskId));
					continue;
				}
				quests.add(qs);
			}
			writeD(quests.size());
			for (QuestState qs : quests) {
				writeD(qs.getQuestId());
				writeS(qs.getStatus().toString());
				writeD(qs.getQuestVars().getQuestVars());
				writeD(qs.getCompleteCount());
				writeS(qs.getNextRepeatTime() == null ? null : qs.getNextRepeatTime().toString());
				writeD(qs.getReward());
			}
		}
			break;
		}
	}
}
