# npc_type × 真端 unattackable 全库审计

## 结论摘要

1. **`npc_template_*.xml` 不是真端迁移产物**：它是 Encom（AionEmu 血统）静态数据包，仓库第 2 个提交 `911440146`（2026-06-23）即已入库；`a52b525f4` 只是把它拆成 9 个分片。真端迁移的是刷怪位置 / 条件 / AI pattern（`spawns/Instances/*.xml` 头部有「数据来源：真端 …world.xml」注释），不是 npc 模板本身。
2. **本地 `npc_type` 与真端 `unattackable` 是两列独立数据，不构成映射**：真端 `unattackable=1` 的 17,518 条里只有 1,840 条是本地 `INVULNERABLE`，其余散在 `NON_ATTACKABLE` / `PEACE` / `ATTACKABLE`；`unattackable=0` 的 29,975 条里 16,974 条是 `NON_ATTACKABLE`。因此「真端 `unattackable=0` → 本地 `ATTACKABLE`」**不是**可用判据（会误伤友军与触发器）。
3. **可用判据是游戏行为合同**：本地 `npc_type` 非 `ATTACKABLE`（玩家打不了）+ 该 NPC 的 AI pattern 会主动攻击 + 真端 `unattackable=0`，全库共 **9278** 条；其中在本地刷怪数据里**真实刷出**的 **1329** 条，是本审计的可操作清单（见同目录 TSV）。

## 数据来源与判据

| 轴 | 来源 |
|---|---|
| 本地 npc_type / name_desc / tribe | `src/main/resources/aion/data/static_data/npcs/npc_template_*.xml`（8 个分片，87,982 条） |
| 本地 npc → pattern | `src/main/resources/aion/definitions/compact/ai/npc-ai-parts/*.xml`（87,721 条） |
| pattern 是否攻击 | `definitions/compact/ai/npcaipatterns*.xml`（12,798 个 pattern，4,201 个含攻击动作） |
| 真端 unattackable / tribe / race_type / ai_name | 真端 `58Server/Map/XML/npcs.xml`（UTF-16，87,734 条） |
| 刷出来源 | `static_data/spawns/**/*.xml`（static / instance）+ `definitions/compact/ai/condition-spawns.xml` |

「pattern 会攻击」的判据：pattern 体内出现 `<attack_most_hating>`，或 `<use_skill>` 的目标为 `OBJI_CUR_TARGET` / `OBJI_MOST_HATING` / `OBJI_ATTACKER` / `OBJI_TARGET`。

## 全量交叉表（本地 npc_type × 真端 unattackable）

| 本地 npc_type | unattackable=0 | =1 | 无该字段 | 不在真端表 | 合计 |
|---|---|---|---|---|---|
| ATTACKABLE | 12,877 | 1,154 | 27,685 | 112 | 41,828 |
| NON_ATTACKABLE | 16,974 | 13,037 | 10,817 | 176 | 41,004 |
| PEACE | 636 | 1,487 | 1,051 | 1 | 3,175 |
| INVULNERABLE | 27 | 1,840 | 81 | 0 | 1,948 |
| AGGRESSIVE | 0 | 0 | 26 | 0 | 26 |

## 高信号清单（本地不可攻击 + pattern 会攻击 + 真端 unattackable=0）

- 全库 **9278** 条；其中本地刷怪数据真实刷出 **1329** 条。
- 已刷出子集按 pattern 家族聚类（前 25）：

| pattern | 条数 |
|---|---|
| `LGuard_FeA` | 105 |
| `AB1_LDGuard_Artifact` | 90 |
| `DGuard_FeA` | 78 |
| `F5_PvPLight_DGuard_Kn_An` | 63 |
| `DGuard_FeB` | 41 |
| `LGuard_FeB` | 39 |
| `LGuard_PeB` | 30 |
| `DGuard_PeB` | 26 |
| `DGuard_WeA` | 25 |
| `BGuard_ChiefC` | 24 |
| `LGuard_AeA` | 24 |
| `DGuard_FhA` | 23 |
| `LGuard_WeA` | 21 |
| `DGuard_PeA` | 20 |
| `LGuard_FhA` | 19 |
| `LGuard_PeA` | 19 |
| `DGuard_ReB` | 17 |
| `F5_PvPLight_LGuard_Kn_An` | 16 |
| `DGuard_AeA` | 15 |
| `LGuard_WeB` | 15 |
| `DGuard_AeB` | 14 |
| `DGuard_WeB` | 14 |
| `LGuard_ReA_L50` | 14 |
| `LGuard_ReB` | 14 |
| `F5_PvPLight_DGuard_Fi_An` | 12 |

## 高价值子集：非守卫 / 非深渊家族（102 条，多为疑似真缺陷，逐条列出）

这些不是「PvP 守卫按阵营相对可攻击」的老问题，而是**实例/世界里的具名 Boss、士兵、炮台**等在真端可击杀、本地却标了不可攻击：

| npc_id | name_desc | local_npc_type | local_tribe | retail_tribe | local_pattern | spawn |
|---|---|---|---|---|---|---|
| 277225 | GAb1_Sub_Rune_Howitzer_Dest_69_65_Ah | NON_ATTACKABLE | GAB1_SUB_DEST_69_AGGRESSIVE | GAb1_Sub_Dest_69_Aggressive | BLDF5_SWHowitzer_Gu_65_Ae | condition,static |
| 277226 | GAb1_Sub_Rune_Howitzer_Dest_70_65_Ah | NON_ATTACKABLE | GAB1_SUB_DEST_70_AGGRESSIVE | GAb1_Sub_Dest_70_Aggressive | BLDF5_SWHowitzer_Gu_65_Ae | condition,static |
| 277227 | GAb1_Sub_Rune_Howitzer_Dest_71_65_Ah | NON_ATTACKABLE | GAB1_SUB_DEST_71_AGGRESSIVE | GAb1_Sub_Dest_71_Aggressive | BLDF5_SWHowitzer_Gu_65_Ae | condition,static |
| 277228 | GAb1_Sub_Rune_Howitzer_Dest_72_65_Ah | NON_ATTACKABLE | GAB1_SUB_DEST_72_AGGRESSIVE | GAb1_Sub_Dest_72_Aggressive | BLDF5_SWHowitzer_Gu_65_Ae | condition,static |
| 258220 | DF4_3111_Boss_Da_1 | NON_ATTACKABLE | GUARD_DARK | Guard_Dark | DF4_GH_KJS | static |
| 258221 | DF4_3111_Boss_Da_2 | NON_ATTACKABLE | GUARD_DARK | Guard_Dark | DF4_GH_KJS | static |
| 258222 | DF4_3111_Boss_Da_3 | NON_ATTACKABLE | GUARD_DARK | Guard_Dark | DF4_GH_KJS | static |
| 258223 | DF4_3111_Boss_Da_4 | NON_ATTACKABLE | GUARD_DARK | Guard_Dark | DF4_GH_KJS | static |
| 258224 | DF4_3111_Boss_Da_5 | NON_ATTACKABLE | GUARD_DARK | Guard_Dark | DF4_GH_KJS | static |
| 884355 | DF4_M_9011_Boss_Li_1 | NON_ATTACKABLE | GUARD | Guard | DF4_SP_V01_Boss | static |
| 884360 | DF4_M_9011_Boss_Da_1 | NON_ATTACKABLE | GUARD_DARK | Guard_Dark | DF4_SP_V01_Boss | static |
| 884370 | DF4_M_9012_Boss_Li_1 | NON_ATTACKABLE | GUARD | Guard | DF4_SP_V02_Boss | static |
| 884375 | DF4_M_9012_Boss_Da_1 | NON_ATTACKABLE | GUARD_DARK | Guard_Dark | DF4_SP_V02_Boss | static |
| 884385 | DF4_M_9013_Boss_Li_1 | NON_ATTACKABLE | GUARD | Guard | DF4_SP_V03_Boss | static |
| 884390 | DF4_M_9013_Boss_Da_1 | NON_ATTACKABLE | GUARD_DARK | Guard_Dark | DF4_SP_V03_Boss | static |
| 884400 | DF4_M_9014_Boss_Li_1 | NON_ATTACKABLE | GUARD | Guard | DF4_SP_V04_Boss | static |
| 884401 | DF4_M_9014_Boss_Li_2 | NON_ATTACKABLE | GUARD | Guard | DF4_SP_V04_Boss | static |
| 884402 | DF4_M_9014_Boss_Li_3 | NON_ATTACKABLE | GUARD | Guard | DF4_SP_V04_Boss | static |
| 884403 | DF4_M_9014_Boss_Li_4 | NON_ATTACKABLE | GUARD | Guard | DF4_SP_V04_Boss | static |
| 884404 | DF4_M_9014_Boss_Li_5 | NON_ATTACKABLE | GUARD | Guard | DF4_SP_V04_Boss | static |
| 884405 | DF4_M_9014_Boss_Da_1 | NON_ATTACKABLE | GUARD_DARK | Guard_Dark | DF4_SP_V04_Boss | static |
| 884406 | DF4_M_9014_Boss_Da_2 | NON_ATTACKABLE | GUARD_DARK | Guard_Dark | DF4_SP_V04_Boss | static |
| 884407 | DF4_M_9014_Boss_Da_3 | NON_ATTACKABLE | GUARD_DARK | Guard_Dark | DF4_SP_V04_Boss | static |
| 884408 | DF4_M_9014_Boss_Da_4 | NON_ATTACKABLE | GUARD_DARK | Guard_Dark | DF4_SP_V04_Boss | static |
| 884409 | DF4_M_9014_Boss_Da_5 | NON_ATTACKABLE | GUARD_DARK | Guard_Dark | DF4_SP_V04_Boss | static |
| 884415 | DF4_M_9015_Boss_Li_1 | NON_ATTACKABLE | GUARD | Guard | DF4_SP_V05_Boss | static |
| 884420 | DF4_M_9015_Boss_Da_1 | NON_ATTACKABLE | GUARD_DARK | Guard_Dark | DF4_SP_V05_Boss | static |
| 884430 | DF4_M_9016_Boss_Li_1 | NON_ATTACKABLE | GUARD | Guard | DF4_SP_V06_Boss | static |
| 884435 | DF4_M_9016_Boss_Da_1 | NON_ATTACKABLE | GUARD_DARK | Guard_Dark | DF4_SP_V06_Boss | static |
| 884445 | DF4_M_9017_Boss_Li_1 | NON_ATTACKABLE | GUARD | Guard | DF4_SP_V07_Boss | static |
| 884450 | DF4_M_9017_Boss_Da_1 | NON_ATTACKABLE | GUARD_DARK | Guard_Dark | DF4_SP_V07_Boss | static |
| 834155 | IDF6_LF1_NPC_Ra_L_03 | NON_ATTACKABLE | IDF6_LF1_PROTECT | IDF6_LF1_Protect | DF6_B2_Din_02_Enter_Attack_75 | condition |
| 834156 | IDF6_LF1_NPC_Ra_D_03 | NON_ATTACKABLE | IDF6_LF1_PROTECT_D | IDF6_LF1_Protect_d | DF6_B2_Din_02_Enter_Attack_75 | condition |
| 834046 | IDF6_LF1_NPC_Ra_L_02 | NON_ATTACKABLE | IDF6_LF1_PROTECT | IDF6_LF1_Protect | DF6_I_Din_01_Enter_Attack_74 | condition |
| 834047 | IDF6_LF1_NPC_Ra_D_02 | NON_ATTACKABLE | IDF6_LF1_PROTECT_D | IDF6_LF1_Protect_d | DF6_I_Din_01_Enter_Attack_74 | condition |
| 834037 | IDF6_LF1_NPC_Ra_L_01 | NON_ATTACKABLE | IDF6_LF1_PROTECT | IDF6_LF1_Protect | DF6_I_Din_02_Enter_Attack_74 | condition |
| 834038 | IDF6_LF1_NPC_Ra_D_01 | NON_ATTACKABLE | IDF6_LF1_PROTECT_D | IDF6_LF1_Protect_d | DF6_I_Din_02_Enter_Attack_74 | condition |
| 220588 | IDEternity_02_Event_Nepilim_03 | NON_ATTACKABLE | NEPILIM_EARTH | Nepilim_Earth | IDEternity_02_Event_Nepilim_03 | instance |
| 833897 | IDLUNA_SIEGE_Soldier_As_An_02 | NON_ATTACKABLE | IDLUNA_SIEGE_NPC | IDLUNA_SIEGE_NPC | IDLUNA_SIEGE_Soldier_As_An_02_AI | condition |
| 833826 | IDLUNA_SIEGE_Soldier_Fi_An_01 | NON_ATTACKABLE | IDLUNA_SIEGE_NPC | IDLUNA_SIEGE_NPC | IDLUNA_SIEGE_Soldier_Fi_An_01_AI | condition |
| 833829 | IDLUNA_SIEGE_Soldier_Kn_N_An_01 | NON_ATTACKABLE | IDLUNA_SIEGE_NPC | IDLUNA_SIEGE_NPC | IDLUNA_SIEGE_Soldier_Kn_N_An_01_AI | condition |
| 833828 | IDLUNA_SIEGE_Soldier_Pr_An_01 | NON_ATTACKABLE | IDLUNA_SIEGE_NPC | IDLUNA_SIEGE_NPC | IDLUNA_SIEGE_Soldier_Pr_An_01_AI | condition |
| 833827 | IDLUNA_SIEGE_Soldier_Ra_An_01 | NON_ATTACKABLE | IDLUNA_SIEGE_NPC | IDLUNA_SIEGE_NPC | IDLUNA_SIEGE_Soldier_Ra_An_01_AI | condition |
| 835812 | IDRUN_Spider_01 | NON_ATTACKABLE | AGGRESSIVESINGLEMONSTER | AggressiveSingleMonster | IDRUN_Spider_01_AI | instance |
| 799648 | IDYun_Soldier_A | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND1 | instance |
| 799654 | IDYun_Soldier_D | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND1 | instance |
| 800285 | IDYun_Soldier_D_1 | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND1 | instance |
| 800288 | IDYun_Soldier_D_2 | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND1 | instance |
| 800291 | IDYun_Soldier_D_3 | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND1 | instance |
| 833466 | IDYUN_Hard_Soldier_D | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND1 | instance |
| 833467 | IDYUN_Hard_Soldier_D_1 | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND1 | instance |
| 833468 | IDYUN_Hard_Soldier_D_2 | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND1 | instance |
| 833469 | IDYUN_Hard_Soldier_D_3 | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND1 | instance |
| 833473 | IDYUN_Hard_Soldier_A | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND1 | instance |
| 799652 | IDYun_Soldier_B | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND2 | instance |
| 799655 | IDYun_Soldier_E | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND2 | instance |
| 800286 | IDYun_Soldier_E_1 | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND2 | instance |
| 800289 | IDYun_Soldier_E_2 | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND2 | instance |
| 800292 | IDYun_Soldier_E_3 | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND2 | instance |
| 833470 | IDYUN_Hard_Soldier_E_1 | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND2 | instance |
| 833471 | IDYUN_Hard_Soldier_E_2 | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND2 | instance |
| 833472 | IDYUN_Hard_Soldier_E_3 | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND2 | instance |
| 799653 | IDYun_Soldier_C | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND3 | instance |
| 799656 | IDYun_Soldier_F | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND3 | instance |
| 833474 | IDYUN_Hard_Soldier_C | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND3 | instance |
| 833475 | IDYun_Hard_Soldier_F | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND3 | instance |
| 204021 | NPC_Kaidan_Warrior | NON_ATTACKABLE | GUARD | guard | Krall_FnB | static |
| 204020 | NPC_Mabangta | NON_ATTACKABLE | GUARD | Guard | Krall_WeB | static |
| 235065 | LDF4_Advance_GodEliteDM | NON_ATTACKABLE | LDF4_ADVANCE_GODELITEDM | LDF4_Advance_GodEliteDM | LDF4_Advance_GodEliteDM | static |
| 235064 | LDF4_Advance_GodEliteLF | NON_ATTACKABLE | LDF4_ADVANCE_GODELITELF | LDF4_Advance_GodEliteLF | LDF4_Advance_GodEliteLF | static |
| 258215 | DF4_3111_Boss_Li_1 | NON_ATTACKABLE | GUARD | Guard | LF4_GH_KJS | static |
| 258216 | DF4_3111_Boss_Li_2 | NON_ATTACKABLE | GUARD | Guard | LF4_GH_KJS | static |
| 258217 | DF4_3111_Boss_Li_3 | NON_ATTACKABLE | GUARD | Guard | LF4_GH_KJS | static |
| 258218 | DF4_3111_Boss_Li_4 | NON_ATTACKABLE | GUARD | Guard | LF4_GH_KJS | static |
| 258219 | DF4_3111_Boss_Li_5 | NON_ATTACKABLE | GUARD | Guard | LF4_GH_KJS | static |
| 884250 | LF4_M_8011_Boss_Li_1 | NON_ATTACKABLE | GUARD | Guard | LF4_SP_V01_Boss | static |
| 884255 | LF4_M_8011_Boss_Da_1 | NON_ATTACKABLE | GUARD_DARK | Guard_Dark | LF4_SP_V01_Boss | static |
| 884265 | LF4_M_8012_Boss_Li_1 | NON_ATTACKABLE | GUARD | Guard | LF4_SP_V02_Boss | static |
| 884270 | LF4_M_8012_Boss_Da_1 | NON_ATTACKABLE | GUARD_DARK | Guard_Dark | LF4_SP_V02_Boss | static |
| 884280 | LF4_M_8013_Boss_Li_1 | NON_ATTACKABLE | GUARD | Guard | LF4_SP_V03_Boss | static |
| 884285 | LF4_M_8013_Boss_Da_1 | NON_ATTACKABLE | GUARD_DARK | Guard_Dark | LF4_SP_V03_Boss | static |
| 884295 | LF4_M_8014_Boss_Li_1 | NON_ATTACKABLE | GUARD | Guard | LF4_SP_V04_Boss | static |
| 884300 | LF4_M_8014_Boss_Da_1 | NON_ATTACKABLE | GUARD_DARK | Guard_Dark | LF4_SP_V04_Boss | static |
| 884310 | LF4_M_8015_Boss_Li_1 | NON_ATTACKABLE | GUARD | Guard | LF4_SP_V05_Boss | static |
| 884315 | LF4_M_8015_Boss_Da_1 | NON_ATTACKABLE | GUARD_DARK | Guard_Dark | LF4_SP_V05_Boss | static |
| 884325 | LF4_M_8016_Boss_Li_1 | NON_ATTACKABLE | GUARD | Guard | LF4_SP_V06_Boss | static |
| 884330 | LF4_M_8016_Boss_Da_1 | NON_ATTACKABLE | GUARD_DARK | Guard_Dark | LF4_SP_V06_Boss | static |
| 884340 | LF4_M_8017_Boss_Li_1 | NON_ATTACKABLE | GUARD | Guard | LF4_SP_V07_Boss | static |
| 884345 | LF4_M_8017_Boss_Da_1 | NON_ATTACKABLE | GUARD_DARK | Guard_Dark | LF4_SP_V07_Boss | static |
| 204387 | DF2_NPC_Ananta | NON_ATTACKABLE | GENERAL_DARK | general_dark | Lycan_WeB | static |
| 204811 | NPC_Gark | NON_ATTACKABLE | GENERAL_DARK | general_dark | ND2_AnJ_BR | static |
| 236985 | IDLF1_F_MBrownieFWaCra_50_Ae_SP | NON_ATTACKABLE | BROWNIE | Brownie | ND2_FeL1 | instance |
| 236984 | IDLF1_F_DBrownieMScCra_50_Ae_SP | NON_ATTACKABLE | BROWNIE | Brownie | ND2_FeL2 | instance |
| 204388 | DF2_NPC_Chaikata | NON_ATTACKABLE | GENERAL_DARK | general_dark | ND2_FhJ | static |
| 204805 | NPC_Vanargand | NON_ATTACKABLE | GENERAL_DARK | general_dark | ND2_KnC | static |
| 204572 | NPC_Karanus | NON_ATTACKABLE | GUARD | Guard | ND2_PnA | static |
| 204605 | NPC_EarthElemental_6 | NON_ATTACKABLE | GUARD | Guard | ND2_PnA | static |
| 204570 | NPC_Ageios | NON_ATTACKABLE | GUARD | Guard | ND2_PnE | static |
| 204819 | DF3_NPC_FrostEyebrows_warrior | NON_ATTACKABLE | GUARD_DARK | Guard_dark | NLycan_FeA | static |
| 207838 | DGuard_LycanFighter_45_Ae2 | NON_ATTACKABLE | GUARD_DARK | Guard_dark | NLycan_FeA | static |
| 207840 | DGuard_LycanFighter_50_Ae2 | NON_ATTACKABLE | GUARD_DARK | Guard_dark | NLycan_FeA | static |
| 204400 | DF2_NPC_Kuriuta | NON_ATTACKABLE | GENERAL_DARK | general_dark | NLycan_KeA | static |

## 实例 / 条件刷出的命中条目（spawn_source 含 instance 或 condition，45 条）

| npc_id | name_desc | local_npc_type | local_tribe | retail_tribe | local_pattern | spawn |
|---|---|---|---|---|---|---|
| 277225 | GAb1_Sub_Rune_Howitzer_Dest_69_65_Ah | NON_ATTACKABLE | GAB1_SUB_DEST_69_AGGRESSIVE | GAb1_Sub_Dest_69_Aggressive | BLDF5_SWHowitzer_Gu_65_Ae | condition,static |
| 277226 | GAb1_Sub_Rune_Howitzer_Dest_70_65_Ah | NON_ATTACKABLE | GAB1_SUB_DEST_70_AGGRESSIVE | GAb1_Sub_Dest_70_Aggressive | BLDF5_SWHowitzer_Gu_65_Ae | condition,static |
| 277227 | GAb1_Sub_Rune_Howitzer_Dest_71_65_Ah | NON_ATTACKABLE | GAB1_SUB_DEST_71_AGGRESSIVE | GAb1_Sub_Dest_71_Aggressive | BLDF5_SWHowitzer_Gu_65_Ae | condition,static |
| 277228 | GAb1_Sub_Rune_Howitzer_Dest_72_65_Ah | NON_ATTACKABLE | GAB1_SUB_DEST_72_AGGRESSIVE | GAb1_Sub_Dest_72_Aggressive | BLDF5_SWHowitzer_Gu_65_Ae | condition,static |
| 834155 | IDF6_LF1_NPC_Ra_L_03 | NON_ATTACKABLE | IDF6_LF1_PROTECT | IDF6_LF1_Protect | DF6_B2_Din_02_Enter_Attack_75 | condition |
| 834156 | IDF6_LF1_NPC_Ra_D_03 | NON_ATTACKABLE | IDF6_LF1_PROTECT_D | IDF6_LF1_Protect_d | DF6_B2_Din_02_Enter_Attack_75 | condition |
| 834046 | IDF6_LF1_NPC_Ra_L_02 | NON_ATTACKABLE | IDF6_LF1_PROTECT | IDF6_LF1_Protect | DF6_I_Din_01_Enter_Attack_74 | condition |
| 834047 | IDF6_LF1_NPC_Ra_D_02 | NON_ATTACKABLE | IDF6_LF1_PROTECT_D | IDF6_LF1_Protect_d | DF6_I_Din_01_Enter_Attack_74 | condition |
| 834037 | IDF6_LF1_NPC_Ra_L_01 | NON_ATTACKABLE | IDF6_LF1_PROTECT | IDF6_LF1_Protect | DF6_I_Din_02_Enter_Attack_74 | condition |
| 834038 | IDF6_LF1_NPC_Ra_D_01 | NON_ATTACKABLE | IDF6_LF1_PROTECT_D | IDF6_LF1_Protect_d | DF6_I_Din_02_Enter_Attack_74 | condition |
| 802054 | IDF5_TD_War_Latkel_E | PEACE | GENERAL_DARK | general_dark | F5_PvPLight_DGuard_Kn_An | instance |
| 802384 | IDLDF5_Fortress_Re_Seide_E | PEACE | GENERAL_DARK | general_dark | F5_PvPLight_DGuard_Kn_An | instance |
| 802055 | IDF5_TD_War_Demades_E | PEACE | GENERAL | general | F5_PvPLight_LGuard_Kn_An | instance |
| 802383 | IDLDF5_Fortress_Re_Thales_E | PEACE | GENERAL | General | F5_PvPLight_LGuard_Kn_An | instance |
| 220588 | IDEternity_02_Event_Nepilim_03 | NON_ATTACKABLE | NEPILIM_EARTH | Nepilim_Earth | IDEternity_02_Event_Nepilim_03 | instance |
| 833897 | IDLUNA_SIEGE_Soldier_As_An_02 | NON_ATTACKABLE | IDLUNA_SIEGE_NPC | IDLUNA_SIEGE_NPC | IDLUNA_SIEGE_Soldier_As_An_02_AI | condition |
| 833826 | IDLUNA_SIEGE_Soldier_Fi_An_01 | NON_ATTACKABLE | IDLUNA_SIEGE_NPC | IDLUNA_SIEGE_NPC | IDLUNA_SIEGE_Soldier_Fi_An_01_AI | condition |
| 833829 | IDLUNA_SIEGE_Soldier_Kn_N_An_01 | NON_ATTACKABLE | IDLUNA_SIEGE_NPC | IDLUNA_SIEGE_NPC | IDLUNA_SIEGE_Soldier_Kn_N_An_01_AI | condition |
| 833828 | IDLUNA_SIEGE_Soldier_Pr_An_01 | NON_ATTACKABLE | IDLUNA_SIEGE_NPC | IDLUNA_SIEGE_NPC | IDLUNA_SIEGE_Soldier_Pr_An_01_AI | condition |
| 833827 | IDLUNA_SIEGE_Soldier_Ra_An_01 | NON_ATTACKABLE | IDLUNA_SIEGE_NPC | IDLUNA_SIEGE_NPC | IDLUNA_SIEGE_Soldier_Ra_An_01_AI | condition |
| 835812 | IDRUN_Spider_01 | NON_ATTACKABLE | AGGRESSIVESINGLEMONSTER | AggressiveSingleMonster | IDRUN_Spider_01_AI | instance |
| 799648 | IDYun_Soldier_A | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND1 | instance |
| 799654 | IDYun_Soldier_D | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND1 | instance |
| 800285 | IDYun_Soldier_D_1 | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND1 | instance |
| 800288 | IDYun_Soldier_D_2 | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND1 | instance |
| 800291 | IDYun_Soldier_D_3 | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND1 | instance |
| 833466 | IDYUN_Hard_Soldier_D | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND1 | instance |
| 833467 | IDYUN_Hard_Soldier_D_1 | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND1 | instance |
| 833468 | IDYUN_Hard_Soldier_D_2 | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND1 | instance |
| 833469 | IDYUN_Hard_Soldier_D_3 | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND1 | instance |
| 833473 | IDYUN_Hard_Soldier_A | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND1 | instance |
| 799652 | IDYun_Soldier_B | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND2 | instance |
| 799655 | IDYun_Soldier_E | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND2 | instance |
| 800286 | IDYun_Soldier_E_1 | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND2 | instance |
| 800289 | IDYun_Soldier_E_2 | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND2 | instance |
| 800292 | IDYun_Soldier_E_3 | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND2 | instance |
| 833470 | IDYUN_Hard_Soldier_E_1 | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND2 | instance |
| 833471 | IDYUN_Hard_Soldier_E_2 | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND2 | instance |
| 833472 | IDYUN_Hard_Soldier_E_3 | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND2 | instance |
| 799653 | IDYun_Soldier_C | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND3 | instance |
| 799656 | IDYun_Soldier_F | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND3 | instance |
| 833474 | IDYUN_Hard_Soldier_C | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND3 | instance |
| 833475 | IDYun_Hard_Soldier_F | NON_ATTACKABLE | IDYUN_ANCIENT | IDYun_Ancient | IDYun_Soldier_ND3 | instance |
| 236985 | IDLF1_F_MBrownieFWaCra_50_Ae_SP | NON_ATTACKABLE | BROWNIE | Brownie | ND2_FeL1 | instance |
| 236984 | IDLF1_F_DBrownieMScCra_50_Ae_SP | NON_ATTACKABLE | BROWNIE | Brownie | ND2_FeL2 | instance |

## 已知误报与边界

- **友军小队**：`IDLUNA_SIEGE_Soldier_*`（833826/833827/833828/833829/833897，秘密军工厂联盟小队）真端 `unattackable=0`，但它们是**与玩家并肩作战的友军**，本地 `NON_ATTACKABLE` 是正确行为，**不得**改为 `ATTACKABLE`。
- **导演 / 触发器对象**：`IDLuna_def_direct_*a`、`IDLuna_Boss_Sound01` 等 `sensory_range=0` 的脚本对象同理。
- **PvP 守卫**：`Ab1_*` / `LGuard_*` / `DGuard_*` / `BGuard_*` 家族（占已刷出命中的绝大多数）在真端按**阵营相对**可攻击；本地 `npc_type` 是绝对量，改为 `ATTACKABLE` 属设计决策，须按 SDJ-007 的边界逐域裁定，勿一次性泛化。
- 本审计**只读**，未修改任何生产数据。

## 复现方法

```bash
# 真端 npcs.xml（UTF-16）转 UTF-8
iconv -f UTF-16 -t UTF-8 58Server/Map/XML/npcs.xml > /tmp/npcs_utf8.xml
# 再按「本地 npc_type != ATTACKABLE 且 真端 unattackable=0 且 pattern 含攻击动作」取交集
```

完整清单：`docs/static-data/npc-type-retail-unattackable-audit.tsv`（列：npc_id / name_desc / local_npc_type / local_tribe / retail_unattackable / retail_tribe / retail_ai_name / local_pattern / spawn_source）。
