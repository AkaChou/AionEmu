package com.aionemu.chatserver.model.channel;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import com.aionemu.chatserver.configs.Config;
import com.aionemu.chatserver.model.Gender;
import com.aionemu.chatserver.model.PlayerClass;
import com.aionemu.chatserver.model.Race;
import com.aionemu.chatserver.service.GameServerService;

/**
 * 聊天频道注册表：预置 LFG/交易/地区/职业等频道，并按标识解析频道。
 * Chat channel registry that seeds LFG/trade/region/job channels and resolves them by identifier.
 * <p>频道名单是数据：世界表与各语言表按行声明，装配逻辑只有一份。</p>
 * <p>Channel lists are data: the world table and the per-language tables are declared row-wise,
 * and the assembly logic exists only once.</p>
 * @author ATracer
 */
@Slf4j
public class ChatChannels {

    /**
     * 已注册频道列表。
     * Registered channel list.
     */
    private static final List<Channel> channels = new ArrayList<>();

    /**
     * 世界频道表：每行「地图 ID 频道短名」，短名取自客户端 WorldId.xml。
     * 每行同时注册公共频道与交易频道。
     * World channel table, "map id short name" per row; short names come from the client
     * WorldId.xml, and each row registers both the public and the trade channel.
     */
    private static final String WORLD_CHANNELS = """
        110010000 LC1
        110020000 LC2
        110070000 Arena_L_Lobby
        120010000 DC1
        120020000 DC2
        120080000 Arena_D_Lobby
        130090000 Arena_L_CLobby
        140010000 Arena_D_CLobby
        210010000 LF1
        210020000 LF2
        210030000 LF1A
        210040000 LF3
        210050000 LF4
        210050000 LF4_M1
        210060000 LF2A
        210070000 LF5
        210080000 LF5_Ship
        210090000 LDF5_Under_L
        210100000 LF6
        210110000 LF_Tower
        210120000 Arena_event_L
        220010000 DF1
        220020000 DF2
        220030000 DF1A
        220040000 DF3
        220050000 DF2A
        220070000 DF4
        220080000 DF5
        220090000 DF5_Ship
        220100000 LDF5_Under_D
        220110000 DF6
        220120000 DF_Tower
        220130000 Arena_event_D
        220140000 DF4_M
        300010000 IDAbPro
        300020000 IDTest_Dungeon
        300030000 IDAb1_MiniCastle
        300040000 IDLF1
        300050000 IDAbRe_Up_Asteria
        300060000 IDAbRe_Low_Divine
        300070000 IDAbRe_Up_Rhoo
        300080000 IDAbRe_Low_Wciel
        300090000 IDAbRe_Low_Eciel
        300100000 IDshulackShip
        300110000 IDAb1_Dreadgion
        300120000 IDAbRe_Up3_Dkisas
        300130000 IDAbRe_Up3_Lamiren
        300140000 IDAbRe_Up3_Crotan
        300150000 IDTemple_Up
        300160000 IDTemple_Low
        300170000 IDCatacombs
        300190000 IDElim
        300200000 IDNovice
        300210000 IDDreadgion_02
        300220000 IDAbRe_Core
        300230000 IDCromede
        300240000 IDStation
        300241000 IDStation_event
        300250000 IDF4Re_Drana
        300260000 IDElemental_1
        300270000 IDElemental_2
        300280000 IDYun
        300290000 Test_MRT_IDZone
        300300000 IDArena
        300310000 IDRaksha
        300320000 IDArena_Solo
        300330000 IDLDF4A
        300350000 IDArena_pvp01
        300360000 IDArena_pvp02
        300380000 IDLDF4A_Raid
        300390000 IDLDF4A_Lehpar
        300400000 IDLDF4b_Tiamat
        300410000 IDLDF4a_Intro
        300420000 IDArena_pvp01_T
        300430000 IDArena_pvp02_T
        300440000 IDDreadgion_03
        300450000 IDArena_Team01
        300460000 IDshulackShip_Solo
        300470000 IDTiamat_Reward
        300480000 IDLDF5RE_Solo
        300490000 IDTiamat_Solo
        300500000 IDTiamat_Israphel
        300510000 IDTiamat_1
        300520000 IDTiamat_2
        300530000 IDLDF5Re_02
        300540000 IDLDF5b_TD
        300550000 IDArena_Glory
        300560000 IDDF2Flying_Event01
        300570000 IDArena_Team01_T
        300580000 IDLDF5Re_03
        300590000 IDLDF5_Under_01
        300600000 IDAbRe_Core_02
        300610000 IDRaksha_solo
        300620000 IDYun_Hard
        300630000 IDTiamat_2_Hard
        300700000 IDUnderpassRe
        300800000 IDRuneweapon
        300900000 IDRuneweapon_Q
        301000000 IDLDF5RE_solo_Q
        301010000 IDShulack_Rose_Solo_01
        301020000 IDShulack_Rose_Solo_02
        301030000 IDShulack_Rose_01
        301040000 IDShulack_Rose_02
        301050000 IDShulack_Rose_03
        301100000 IDArena_Team01_T2
        301110000 IDLDF5_Under_Rune
        301120000 IDKamar
        301130000 IDVritra_Base
        301140000 IDLDF5_Under_02
        301150000 IDAsteria_IU_Solo
        301160000 IDAsteria_IU_Party
        301170000 IDLDF5Re_02_L
        301180000 IDLDF5Re_03_L
        301190000 IDLDF5RE_Solo_L
        301200000 IDAsteria_IU_World
        301210000 IDLDF5_Under_01_War
        301220000 IDF5_TD_War
        301230000 IDLDF5_Under_03
        301240000 IDAbRe_Up3_Dkisas_02
        301250000 IDAbRe_Up3_Lamiren_02
        301260000 IDAbRe_Up3_Crotan_02
        301270000 IDLDF4Re_01
        301280000 IDAbRe_Up3_Dkisas_02_N
        301290000 IDAbRe_Up3_Lamiren_02_N
        301300000 IDAbRe_Up3_Crotan_02_N
        301310000 IDLDF5_Fortress_Re
        301320000 IDLDF5_Under_01_PC
        301330000 IDLDF5_Under_Rune_PC
        301340000 IDLDF4Re_01_Q
        301360000 IDLDF5_Under_Rune_H
        301370000 IDLDF5_Under_03_H
        301380000 IDLDF5_Under_02_E
        301390000 IDSeal
        301400000 IDSweep
        301500000 IDLegion
        301510000 IDElemental_2_E
        301520000 IDSeal_Q
        301540000 IDEternity_01
        301550000 IDEternity_02
        301560000 IDEternity_03
        301570000 IDEternity_Q
        301580000 IDF6_OP
        301590000 IDSweep_02
        301600000 IDF6_Adma
        301610000 IDF6_Lap
        301620000 IDF6_Dragon
        301630000 IDLuna_def
        301631000 IDEvent_def
        301640000 IDLuna_Siege
        301650000 IDDreadgion_04
        301660000 IDF6_LF1
        301670000 IDLDF5_Under_02_War
        301680000 IDLDF5_Fortress_War
        301690000 IDF6_Q
        301700000 IDRun
        301720000 IDAbre_core_03
        302000000 IDDF2_Dflame_Event
        302100000 IDTransform
        302110000 IDTransform_event
        302200000 IDLC1_dreadgion
        302300000 IDDC1_dreadgion
        302310000 IDTM_Arena_01
        302320000 IDTM_Lobby_01
        302330000 IDEvent_Solo
        302340000 IDAb1_Ere
        302350000 IDEternity_War
        302360000 IDTM_ArenaE_01
        302370000 IDTM_LobbyE_01
        302380000 IDTM_ArenaP_01
        302390000 IDTM_LobbyP_01
        302400000 IDInfinity
        302410000 IDTM_ArenaP_02
        302420000 IDTM_LobbyP_02
        310010000 IDAbProL1
        310020000 IDAbProL2
        310030000 IDAbGateL1
        310040000 IDAbGateL2
        310050000 IDLF3Lp
        310060000 IDLF1B
        310070000 IDLF1B_Stigma
        310080000 IDLC1_arena
        310090000 IDLF3_Castle_indratoo
        310100000 IDLF3_Castle_Lehpar
        310110000 IDLF2a_Lab
        310120000 IDAbProL3
        310160000 IDAb1_Heroes_L
        320010000 IDAbProD1
        320020000 IDAbProD2
        320030000 IDAbGateD1
        320040000 IDAbGateD2
        320050000 IDDF2Flying
        320060000 IDDF1B
        320070000 IDSpace
        320080000 IDDF3_Dragon
        320090000 IDDC1_arena
        320100000 IDDF2_Dflame
        320110000 IDDF3_LP
        320120000 IDDC1_Arena_3F
        320130000 IDDf2a_Adma
        320140000 IDAbProD3
        320150000 IDDramata_01
        320160000 IDAb1_Heroes_D
        400010000 Ab1
        400020000 Gab1_01
        400030000 GAb1_Sub
        400040000 Gab1_02
        400050000 Gab1_03
        400060000 Gab1_04
        510010000 LF_Prison
        520010000 DF_Prison
        600010000 Underpass
        600020000 LDF4a
        600030000 LDF4b
        600040000 Tiamat_Down
        600041000 Tiamat_Down_M
        600050000 LDF5a
        600060000 LDF5b
        600070000 LDF5_Under
        600080000 IDIU
        600090000 LDF5_Fortress
        600100000 LDF4_Advance
        600110000 Underpass_M
        600200000 LDF7
        700010000 Housing_LF_personal
        700020000 Housing_LC_legion
        710010000 Housing_DF_personal
        710020000 Housing_DC_legion
        720010000 housing_idlf_personal
        730010000 housing_iddf_personal
        900020000 Test_Basic
        900030000 Test_Server
        900100000 Test_GiantMonster
        900110000 Housing_barrack
        900120000 Test_IDArena
        900130000 IDLDF5RE_test
        900140000 Test_Kgw
        900150000 Test_Basic_Mj
        900170000 test_intro
        900180000 Test_server_art
        900190000 Test_TagMatch
        900200000 test_timeattack
        900210000 IDTournament_Stage_Test
        900220000 System_Basic
        900230000 IDTournament_Lobby_Test
        """;

    /**
     * 语言与职业频道表，按 LANG_CHAT 选择。
     * 单列行注册语言频道；三列行「职业枚举 男性名 女性名」注册两个性别的职业频道，
     * 女性名为 - 时与男性同名，否则女性频道名带 [f:"..."] 后缀。
     * Localization table selected by LANG_CHAT. A single-token row registers a language channel;
     * a three-token "class enum male name female name" row registers both genders of a job channel,
     * where a - female name shares the male name and otherwise carries the [f:"..."] suffix.
     */
    private static final String LOCALIZATION_EN = """
            User_English
            User_French
            User_German
            User_Italian
            User_Spanish
            User_Danish
            User_Swedish
            User_Finnish
            User_Norwegian
            User_Greek
            GLADIATOR Gladiator -
            TEMPLAR Templar -
            SORCERER Sorcerer -
            SPIRIT_MASTER Spiritmaster -
            CHANTER Chanter -
            RANGER Ranger -
            ASSASSIN Assassin -
            CLERIC Cleric -
            ENGINEER engineer -
            ARTIST artist -
            GUNNER gunner -
            BARD bard -
            RIDER rider -
        """;

    private static final String LOCALIZATION_FR = """
            User_Anglais
            User_Francais
            User_Allemand
            User_Italien
            User_Espagnol
            User_Danois
            User_Suedois
            User_Finnois
            User_Norvegien
            User_Grec
            GLADIATOR Gladiateur Gladiateur
            TEMPLAR Templier Templier
            SORCERER Sorcier Sorcier
            SPIRIT_MASTER Spiritualiste Spiritualiste
            CHANTER Chanteur Chanteur
            RANGER Ranger Ranger
            ASSASSIN Assassin Assassine
            CLERIC Clerc Clerc
            ENGINEER engineer Ingenieur
            ARTIST artist Artiste
            GUNNER gunner Garde
            BARD bard Barde
            RIDER rider Mecha
        """;

    private static final String LOCALIZATION_DE = """
            User_Englisch
            User_Französisch
            User_Deutsch
            User_Italienisch
            User_Spanisch
            User_Dänisch
            User_Schwedisch
            User_Finnisch
            User_Norwegisch
            User_Griechisch
            WARRIOR Krieger Kriegerin
            GLADIATOR Gladiator Gladiatorin
            TEMPLAR Templer Templerin
            MAGE Magier Magierin
            SORCERER Zauberer Zauberin
            SPIRIT_MASTER Beschwoerer Beschwoererin
            PRIEST Priester Priesterin
            CHANTER Kantor Kantorin
            CLERIC Kleriker Klerikerin
            SCOUT Spaeher Spaeherin
            RANGER Jaeger Jaegerin
            ASSASSIN Assassine Assassine
            ENGINEER Ingenieur Ingenieur
            GUNNER Schuetze Schuetze
            RIDER Aethertech Aethertech
            ARTIST Kuenstler Kuenstler
            BARD Barde Barde
        """;

    static {
        // 寻找小队频道 / LFG channels
        addGroupChannel("partyFind_PF");
        addGroupChannel("gotcha_PF");
        addChineseJobChannels();
        // 世界频道 / World channels
        WORLD_CHANNELS.lines()
            .map(String::strip)
            .filter(row -> !row.isEmpty())
            .forEach(row -> {
                int split = row.indexOf(' ');
                String shortName = row.substring(split + 1);
                addRegionChannel(Integer.parseInt(row.substring(0, split)), "public_" + shortName);
                addTradeChannel("trade_" + shortName);
            });
        // 镜像服英吉斯温 210130000 只保留交易频道 / Master-server Inggison keeps a trade channel only
        addTradeChannel("trade_LF4_M");
        // 语言与职业 / LANG & JOB
        localization().lines()
            .map(String::strip)
            .filter(row -> !row.isEmpty())
            .forEach(ChatChannels::addLocalizationRow);
    }

    private static void addChannel(Channel Channel) {
        channels.add(Channel);
    }

    /**
     * 按频道 ID 获取频道，未找到时抛异常。
     * Returns the channel by id, throws if none registered.
     * @param channelId 频道 ID / channel id
     * @return 匹配的频道 / matching channel
     * @throws IllegalArgumentException 未注册该 ID 的频道 / no channel registered for the id
     */
    public static Channel getChannelById(int channelId) {
        for (Channel channel : channels) {
            if (channel.getChannelId() == channelId) {
                return channel;
            }
        }
        if (Config.LOG_CHANNEL_INVALID) {
            log.warn(I18n.get("log.1700dbac8256", channelId));
        }
        throw new IllegalArgumentException("No channel provided for id " + channelId);
    }

    /**
     * 按标识符获取频道，未找到时返回 {@code null}。
     * Returns the channel by identifier, {@code null} if none registered.
     * @param identifier 标识字节 / identifier bytes
     * @return 匹配的频道，未找到为 null / matching channel, null if not found
     */
    public static Channel getChannelByIdentifier(byte[] identifier) {
        for (Channel channel : channels) {
            if (Arrays.equals(channel.getIdentifierBytes(), identifier)) {
                return channel;
            }
        }
        if (Config.LOG_CHANNEL_INVALID) {
            log.warn(I18n.get("log.b99c072ad830", new String(identifier, StandardCharsets.UTF_16LE)));
        }
        // 频道名支持 i18n 之前不能抛运行时异常 / no runtime exception before i18n channel names
        return null;
    }

    /**
     * 返回当前配置对应的语言与职业频道表。
     * Returns the localization table for the configured language.
     * @return 语言与职业频道表 / localization table
     */
    private static String localization() {
        return switch (Config.LANG_CHAT) {
            case 1 -> LOCALIZATION_EN;
            case 2 -> LOCALIZATION_FR;
            case 3 -> LOCALIZATION_DE;
            default -> "";
        };
    }

    /**
     * 解析并注册一行语言或职业频道。
     * Parses and registers one language or job channel row.
     * @param row 语言名，或「职业枚举 男性名 女性名」 / language name, or "class male female"
     */
    private static void addLocalizationRow(String row) {
        String[] parts = row.split(" ");
        if (parts.length == 1) {
            addLangChannel(parts[0]);
            return;
        }
        PlayerClass playerClass = PlayerClass.valueOf(parts[0]);
        addJobChannel(Gender.MALE, playerClass, "job_" + parts[1]);
        addJobChannel(Gender.FEMALE, playerClass,
            "-".equals(parts[2]) ? "job_" + parts[1] : femaleJob(parts[1], parts[2]));
    }

    /**
     * 按种族注册一个频道族。
     * Registers one channel family for every race.
     * @param channelName 频道名 / channel name
     * @param factory 以种族与标识符构造频道 / builds a channel from race and identifier
     */
    private static void addRaceChannels(String channelName, BiFunction<Race, String, Channel> factory) {
        for (Race race : Race.values()) {
            addChannel(factory.apply(race, identifier(channelName, race)));
        }
    }

    /**
     * 拼接频道标识符。
     * Builds a channel identifier.
     * @param channelName 频道名 / channel name
     * @param race 阵营 / race
     * @return 频道标识符 / channel identifier
     */
    private static String identifier(String channelName, Race race) {
        return "@\u0001" + channelName + "\u0001" + GameServerService.GAMESERVER_ID + "."
                + race.getRaceId() + ".AION.KOR";
    }

    /**
     * 添加 LFG 组队频道。
     * Adds an LFG party-find channel.
     * @param channelName 频道名 / channel name
     */
    private static void addGroupChannel(String channelName) {
        addRaceChannels(channelName, LfgChannel::new);
    }

    /**
     * 添加交易频道。
     * Adds a trade channel.
     * @param channelName 频道名 / channel name
     */
    private static void addTradeChannel(String channelName) {
        addRaceChannels(channelName, TradeChannel::new);
    }

    /**
     * 添加地区频道。
     * Adds a region channel.
     * @param mapId 地图 ID / map id
     * @param channelName 频道名 / channel name
     */
    private static void addRegionChannel(int mapId, String channelName) {
        addRaceChannels(channelName, (race, id) -> new RegionChannel(mapId, race, id));
    }

    /**
     * 添加语言频道。
     * Adds a language channel.
     * @param channelName 频道名 / channel name
     */
    private static void addLangChannel(String channelName) {
        addRaceChannels(channelName, LangChannel::new);
    }

    /**
     * 添加指定性别的职业频道。
     * Adds a job channel for the given gender.
     * @param gender 性别 / gender
     * @param playerClass 职业 / player class
     * @param channelName 频道名 / channel name
     */
    private static void addJobChannel(Gender gender, PlayerClass playerClass, String channelName) {
        addRaceChannels(channelName, (race, id) -> new JobChannel(gender, playerClass, race, id));
    }

    /**
     * 为两种性别注册同名职业频道。
     * Registers the same job channel name for both genders.
     * @param playerClass 职业 / player class
     * @param channelName 频道名 / channel name
     */
    private static void addJobChannels(PlayerClass playerClass, String channelName) {
        for (Gender gender : Gender.values()) {
            addJobChannel(gender, playerClass, channelName);
        }
    }

    /**
     * 拼接女性职业频道名。
     * Builds a female job channel name.
     * @param maleName 男性职业名 / male job name
     * @param femaleName 女性职业名 / female job name
     * @return 频道名 / channel name
     */
    private static String femaleJob(String maleName, String femaleName) {
        return "job_" + maleName + "[f:\"" + femaleName + "\"]";
    }

    /**
     * 中文职业频道（各语言分支共用）。
     * Chinese job channels (shared by every language branch).
     */
    private static void addChineseJobChannels() {
        addChineseJobChannel(PlayerClass.WARRIOR, "战士");
        addChineseJobChannel(PlayerClass.GLADIATOR, "剑星");
        addChineseJobChannel(PlayerClass.TEMPLAR, "守护星");
        addChineseJobChannel(PlayerClass.SCOUT, "斥候");
        addChineseJobChannel(PlayerClass.ASSASSIN, "杀星");
        addChineseJobChannel(PlayerClass.RANGER, "弓星");
        addChineseJobChannel(PlayerClass.MAGE, "法师");
        addChineseJobChannel(PlayerClass.SORCERER, "魔道星");
        addChineseJobChannel(PlayerClass.SPIRIT_MASTER, "精灵星");
        addChineseJobChannel(PlayerClass.PRIEST, "祭司");
        addChineseJobChannel(PlayerClass.CLERIC, "治愈星");
        addChineseJobChannel(PlayerClass.CHANTER, "护法星");
        addChineseJobChannel(PlayerClass.ENGINEER, "工程师");
        addChineseJobChannel(PlayerClass.RIDER, "机甲星");
        addChineseJobChannel(PlayerClass.GUNNER, "枪炮星");
        addChineseJobChannel(PlayerClass.ARTIST, "艺术家");
        addChineseJobChannel(PlayerClass.BARD, "吟游星");
    }

    private static void addChineseJobChannel(PlayerClass playerClass, String name) {
        addJobChannels(playerClass, "job_" + name);
    }
}
