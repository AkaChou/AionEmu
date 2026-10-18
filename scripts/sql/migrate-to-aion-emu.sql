-- AionEmu 存量数据迁移脚本（只搬数据：不建库、不建表）
-- AionEmu legacy data-migration script (data only: no CREATE DATABASE, no CREATE TABLE).
--
-- 作用 / Purpose:
--   把旧库 al_server_gs + al_server_ls 的数据整表搬进 `aion_emu`（目标库里 ls_ 开头的表取自 al_server_ls，其余取自 al_server_gs）。
--   本文件不含任何建库/建表语句：目标库与 89 张表必须已经存在（见下方「前提」）。
--   逐表先清空再整表搬运（重跑 = 用旧库快照覆盖，不会累积重复行），末尾附行数校验。
--   Moves every table's rows from the two legacy databases into `aion_emu` (ls_-prefixed targets from
--   al_server_ls, the rest from al_server_gs). This file creates nothing: the target database and all 89 tables
--   must already exist. Each table is emptied, then copied whole (a re-run overwrites with the
--   legacy snapshot and never accumulates duplicates); a row-count verification query is appended.
--
-- 用法 / Usage（只需在客户端里填好连接信息 / only connection info is needed）:
--   1. 目标库还没有表时，先启动一次服务端让 Flyway 建好 89 张表（也可在客户端里对空库执行
--      src/main/resources/db/migration/V1__init.sql），然后停服。
--      If `aion_emu` has no tables yet, start the server once so Flyway builds them (or run
--      src/main/resources/db/migration/V1__init.sql against the empty database), then stop the server.
--   2. 停服状态下执行本文件 / run this file with the server stopped.
--   3. 末尾结果集为空 = 89 张表行数全部一致；有行 = 列出的表没搬齐。
--      Empty trailing result set = all 89 row counts match; any row names a table that failed.
--
-- 前提 / Prerequisites:
--   * 迁移期间服务端必须停着，否则内存态会回写覆盖刚搬好的数据。
--     The server must be stopped while this runs, or in-memory state can overwrite the migrated rows.
--   * 两个旧库与目标库必须位于同一个 MySQL 实例（脚本用跨库 INSERT ... SELECT 搬运）。
--     Both legacy databases and the target must live on one MySQL instance (cross-database INSERT ... SELECT).
--   * 库名与默认值不同时，全文替换 al_server_gs / al_server_ls / aion_emu 即可（只有这三个名字）。
--     Replace al_server_gs / al_server_ls / aion_emu globally if your database names differ (only these three).
--   * 每张目标表都会被清空：V1__init.sql 给 10 张表灌了种子数据，不清空会与旧库数据重复；
--     重跑也需要先清空，否则会累积重复行。旧库本身不会被本脚本修改。
--     Every target table is emptied first: V1__init.sql seeds 10 tables, and a re-run would
--     otherwise accumulate duplicate rows. The legacy databases themselves are never modified.
--   * 只搬运 V1__init.sql 声明过的列；旧库若缺少其中某列会直接报 Unknown column 中断（不自动降级，
--     也不跳过任何表）。某个表在旧库不存在时报 Unknown table —— 删掉该表那一行 INSERT ... SELECT 即可，
--     该表保留目标库现有数据，重跑也安全。
--     Only columns declared in V1__init.sql are copied; a legacy database lacking one fails with
--     Unknown column (no silent downgrade, no table is skipped). A missing legacy table reports
--     Unknown table - delete that table's INSERT ... SELECT line; re-running stays safe.
--   * 迁移前先备份旧库 / back up the legacy databases before running.
--
-- shortcut: 各表的列清单由 V1__init.sql 一次性生成（89 张表）；V1 的表或列有变动后需重新生成本文件。
--   The per-table column lists are generated once from V1__init.sql; regenerate this file whenever
--   V1's tables or columns change.

SET NAMES utf8mb4;

-- 前置提示：目标库不存在或没有任何表时返回一行（本脚本不建表）。
-- Preflight: returns a row when `aion_emu` is missing or has no tables (this script creates neither).
SELECT 'WARNING: `aion_emu` has no tables - start the server once (Flyway builds the schema) or run
  src/main/resources/db/migration/V1__init.sql first, and run this file with the server stopped.' AS hint
  FROM information_schema.tables WHERE table_schema = 'aion_emu' HAVING COUNT(*) = 0;

SET FOREIGN_KEY_CHECKS = 0;

-- ==== abyss_landing  <-  al_server_gs.abyss_landing ====
DELETE FROM `aion_emu`.`abyss_landing`;
INSERT INTO `aion_emu`.`abyss_landing` (`id`,`level`,`points`,`siege`,`commander`,`artefact`,`base`,`monuments`,`quest`,`facility`,`race`,`level_up_date`) SELECT `id`,`level`,`points`,`siege`,`commander`,`artefact`,`base`,`monuments`,`quest`,`facility`,`race`,`level_up_date` FROM `al_server_gs`.`abyss_landing`;

-- ==== abyss_rank  <-  al_server_gs.abyss_rank ====
DELETE FROM `aion_emu`.`abyss_rank`;
INSERT INTO `aion_emu`.`abyss_rank` (`player_id`,`daily_ap`,`daily_gp`,`weekly_ap`,`weekly_gp`,`ap`,`gp`,`rank`,`top_ranking`,`daily_kill`,`weekly_kill`,`all_kill`,`max_rank`,`last_kill`,`last_ap`,`last_gp`,`last_update`,`rank_pos`,`old_rank_pos`,`rank_ap`) SELECT `player_id`,`daily_ap`,`daily_gp`,`weekly_ap`,`weekly_gp`,`ap`,`gp`,`rank`,`top_ranking`,`daily_kill`,`weekly_kill`,`all_kill`,`max_rank`,`last_kill`,`last_ap`,`last_gp`,`last_update`,`rank_pos`,`old_rank_pos`,`rank_ap` FROM `al_server_gs`.`abyss_rank`;

-- ==== announcements  <-  al_server_gs.announcements ====
DELETE FROM `aion_emu`.`announcements`;
INSERT INTO `aion_emu`.`announcements` (`id`,`announce`,`faction`,`type`,`delay`) SELECT `id`,`announce`,`faction`,`type`,`delay` FROM `al_server_gs`.`announcements`;

-- ==== base_location  <-  al_server_gs.base_location ====
DELETE FROM `aion_emu`.`base_location`;
INSERT INTO `aion_emu`.`base_location` (`id`,`race`) SELECT `id`,`race` FROM `al_server_gs`.`base_location`;

-- ==== blocks  <-  al_server_gs.blocks ====
DELETE FROM `aion_emu`.`blocks`;
INSERT INTO `aion_emu`.`blocks` (`player`,`blocked_player`,`reason`) SELECT `player`,`blocked_player`,`reason` FROM `al_server_gs`.`blocks`;

-- ==== bookmark  <-  al_server_gs.bookmark ====
DELETE FROM `aion_emu`.`bookmark`;
INSERT INTO `aion_emu`.`bookmark` (`id`,`name`,`char_id`,`x`,`y`,`z`,`world_id`) SELECT `id`,`name`,`char_id`,`x`,`y`,`z`,`world_id` FROM `al_server_gs`.`bookmark`;

-- ==== broker  <-  al_server_gs.broker ====
DELETE FROM `aion_emu`.`broker`;
INSERT INTO `aion_emu`.`broker` (`id`,`item_pointer`,`item_id`,`item_count`,`item_creator`,`seller`,`price`,`broker_race`,`expire_time`,`settle_time`,`seller_id`,`is_sold`,`is_settled`,`is_splitsell`) SELECT `id`,`item_pointer`,`item_id`,`item_count`,`item_creator`,`seller`,`price`,`broker_race`,`expire_time`,`settle_time`,`seller_id`,`is_sold`,`is_settled`,`is_splitsell` FROM `al_server_gs`.`broker`;

-- ==== challenge_tasks  <-  al_server_gs.challenge_tasks ====
DELETE FROM `aion_emu`.`challenge_tasks`;
INSERT INTO `aion_emu`.`challenge_tasks` (`task_id`,`quest_id`,`owner_id`,`owner_type`,`complete_count`,`complete_time`) SELECT `task_id`,`quest_id`,`owner_id`,`owner_type`,`complete_count`,`complete_time` FROM `al_server_gs`.`challenge_tasks`;

-- ==== competition_ranking  <-  al_server_gs.competition_ranking ====
DELETE FROM `aion_emu`.`competition_ranking`;
INSERT INTO `aion_emu`.`competition_ranking` (`player_id`,`table_id`,`rank`,`last_rank`,`points`,`last_points`,`high_points`,`low_points`,`position_match`) SELECT `player_id`,`table_id`,`rank`,`last_rank`,`points`,`last_points`,`high_points`,`low_points`,`position_match` FROM `al_server_gs`.`competition_ranking`;

-- ==== craft_cooldowns  <-  al_server_gs.craft_cooldowns ====
DELETE FROM `aion_emu`.`craft_cooldowns`;
INSERT INTO `aion_emu`.`craft_cooldowns` (`player_id`,`delay_id`,`reuse_time`) SELECT `player_id`,`delay_id`,`reuse_time` FROM `al_server_gs`.`craft_cooldowns`;

-- ==== event_items  <-  al_server_gs.event_items ====
DELETE FROM `aion_emu`.`event_items`;
INSERT INTO `aion_emu`.`event_items` (`player_id`,`item_id`,`counts`) SELECT `player_id`,`item_id`,`counts` FROM `al_server_gs`.`event_items`;

-- ==== f2paccount  <-  al_server_gs.f2paccount ====
DELETE FROM `aion_emu`.`f2paccount`;
INSERT INTO `aion_emu`.`f2paccount` (`player_id`,`time`) SELECT `player_id`,`time` FROM `al_server_gs`.`f2paccount`;

-- ==== friends  <-  al_server_gs.friends ====
DELETE FROM `aion_emu`.`friends`;
INSERT INTO `aion_emu`.`friends` (`player`,`friend`) SELECT `player`,`friend` FROM `al_server_gs`.`friends`;

-- ==== guides  <-  al_server_gs.guides ====
DELETE FROM `aion_emu`.`guides`;
INSERT INTO `aion_emu`.`guides` (`guide_id`,`player_id`,`title`) SELECT `guide_id`,`player_id`,`title` FROM `al_server_gs`.`guides`;

-- ==== house_bids  <-  al_server_gs.house_bids ====
DELETE FROM `aion_emu`.`house_bids`;
INSERT INTO `aion_emu`.`house_bids` (`player_id`,`house_id`,`bid`,`bid_time`) SELECT `player_id`,`house_id`,`bid`,`bid_time` FROM `al_server_gs`.`house_bids`;

-- ==== house_object_cooldowns  <-  al_server_gs.house_object_cooldowns ====
DELETE FROM `aion_emu`.`house_object_cooldowns`;
INSERT INTO `aion_emu`.`house_object_cooldowns` (`player_id`,`object_id`,`reuse_time`) SELECT `player_id`,`object_id`,`reuse_time` FROM `al_server_gs`.`house_object_cooldowns`;

-- ==== house_scripts  <-  al_server_gs.house_scripts ====
DELETE FROM `aion_emu`.`house_scripts`;
INSERT INTO `aion_emu`.`house_scripts` (`house_id`,`index`,`script`) SELECT `house_id`,`index`,`script` FROM `al_server_gs`.`house_scripts`;

-- ==== houses  <-  al_server_gs.houses ====
DELETE FROM `aion_emu`.`houses`;
INSERT INTO `aion_emu`.`houses` (`id`,`player_id`,`building_id`,`address`,`acquire_time`,`settings`,`status`,`fee_paid`,`next_pay`,`sell_started`,`sign_notice`) SELECT `id`,`player_id`,`building_id`,`address`,`acquire_time`,`settings`,`status`,`fee_paid`,`next_pay`,`sell_started`,`sign_notice` FROM `al_server_gs`.`houses`;

-- ==== ingameshop  <-  al_server_gs.ingameshop ====
DELETE FROM `aion_emu`.`ingameshop`;
INSERT INTO `aion_emu`.`ingameshop` (`object_id`,`item_id`,`item_count`,`item_price`,`category`,`sub_category`,`list`,`sales_ranking`,`item_type`,`gift`,`title_description`,`description`) SELECT `object_id`,`item_id`,`item_count`,`item_price`,`category`,`sub_category`,`list`,`sales_ranking`,`item_type`,`gift`,`title_description`,`description` FROM `al_server_gs`.`ingameshop`;

-- ==== inventory  <-  al_server_gs.inventory ====
DELETE FROM `aion_emu`.`inventory`;
INSERT INTO `aion_emu`.`inventory` (`item_unique_id`,`item_id`,`item_count`,`item_color`,`color_expires`,`item_owner`,`item_creator`,`itemCreationTime`,`expire_time`,`is_equiped`,`is_soul_bound`,`slot`,`item_location`,`enchant`,`enchant_bonus`,`item_skin`,`fusioned_item`,`optional_socket`,`optional_fusion_socket`,`activation_count`,`charge`,`rnd_bonus`,`rnd_count`,`wrappable_count`,`is_packed`,`tempering_level`,`is_topped`,`strengthen_skill`,`skin_skill`,`luna_reskin`,`reduction_level`,`is_seal`,`isEnhance`,`enhanceSkillId`,`enhanceSkillEnchant`) SELECT `item_unique_id`,`item_id`,`item_count`,`item_color`,`color_expires`,`item_owner`,`item_creator`,`itemCreationTime`,`expire_time`,`is_equiped`,`is_soul_bound`,`slot`,`item_location`,`enchant`,`enchant_bonus`,`item_skin`,`fusioned_item`,`optional_socket`,`optional_fusion_socket`,`activation_count`,`charge`,`rnd_bonus`,`rnd_count`,`wrappable_count`,`is_packed`,`tempering_level`,`is_topped`,`strengthen_skill`,`skin_skill`,`luna_reskin`,`reduction_level`,`is_seal`,`isEnhance`,`enhanceSkillId`,`enhanceSkillEnchant` FROM `al_server_gs`.`inventory`;

-- ==== item_cooldowns  <-  al_server_gs.item_cooldowns ====
DELETE FROM `aion_emu`.`item_cooldowns`;
INSERT INTO `aion_emu`.`item_cooldowns` (`player_id`,`delay_id`,`use_delay`,`reuse_time`) SELECT `player_id`,`delay_id`,`use_delay`,`reuse_time` FROM `al_server_gs`.`item_cooldowns`;

-- ==== item_drop  <-  al_server_gs.item_drop ====
DELETE FROM `aion_emu`.`item_drop`;
INSERT INTO `aion_emu`.`item_drop` (`npc_id`,`item_id`,`drop_group`,`race`,`min_amount`,`max_amount`,`chance`,`no_reduce`,`eachmember`) SELECT `npc_id`,`item_id`,`drop_group`,`race`,`min_amount`,`max_amount`,`chance`,`no_reduce`,`eachmember` FROM `al_server_gs`.`item_drop`;

-- ==== item_stones  <-  al_server_gs.item_stones ====
DELETE FROM `aion_emu`.`item_stones`;
INSERT INTO `aion_emu`.`item_stones` (`item_unique_id`,`item_id`,`slot`,`category`,`polishNumber`,`polishCharge`,`proc_count`) SELECT `item_unique_id`,`item_id`,`slot`,`category`,`polishNumber`,`polishCharge`,`proc_count` FROM `al_server_gs`.`item_stones`;

-- ==== ladder_player  <-  al_server_gs.ladder_player ====
DELETE FROM `aion_emu`.`ladder_player`;
INSERT INTO `aion_emu`.`ladder_player` (`player_id`,`rating`,`wins`,`losses`,`leaves`,`rank`,`last_rank`,`last_update`) SELECT `player_id`,`rating`,`wins`,`losses`,`leaves`,`rank`,`last_rank`,`last_update` FROM `al_server_gs`.`ladder_player`;

-- ==== legion_announcement_list  <-  al_server_gs.legion_announcement_list ====
DELETE FROM `aion_emu`.`legion_announcement_list`;
INSERT INTO `aion_emu`.`legion_announcement_list` (`legion_id`,`announcement`,`date`) SELECT `legion_id`,`announcement`,`date` FROM `al_server_gs`.`legion_announcement_list`;

-- ==== legion_emblems  <-  al_server_gs.legion_emblems ====
DELETE FROM `aion_emu`.`legion_emblems`;
INSERT INTO `aion_emu`.`legion_emblems` (`legion_id`,`emblem_id`,`color_r`,`color_g`,`color_b`,`emblem_type`,`emblem_data`) SELECT `legion_id`,`emblem_id`,`color_r`,`color_g`,`color_b`,`emblem_type`,`emblem_data` FROM `al_server_gs`.`legion_emblems`;

-- ==== legion_history  <-  al_server_gs.legion_history ====
DELETE FROM `aion_emu`.`legion_history`;
INSERT INTO `aion_emu`.`legion_history` (`id`,`legion_id`,`date`,`history_type`,`name`,`tab_id`,`description`) SELECT `id`,`legion_id`,`date`,`history_type`,`name`,`tab_id`,`description` FROM `al_server_gs`.`legion_history`;

-- ==== legion_join_requests  <-  al_server_gs.legion_join_requests ====
DELETE FROM `aion_emu`.`legion_join_requests`;
INSERT INTO `aion_emu`.`legion_join_requests` (`legionId`,`playerId`,`playerName`,`playerClassId`,`playerRaceId`,`playerLevel`,`playerGenderId`,`joinRequestMsg`,`date`) SELECT `legionId`,`playerId`,`playerName`,`playerClassId`,`playerRaceId`,`playerLevel`,`playerGenderId`,`joinRequestMsg`,`date` FROM `al_server_gs`.`legion_join_requests`;

-- ==== legion_members  <-  al_server_gs.legion_members ====
DELETE FROM `aion_emu`.`legion_members`;
INSERT INTO `aion_emu`.`legion_members` (`legion_id`,`player_id`,`nickname`,`rank`,`selfintro`,`challenge_score`) SELECT `legion_id`,`player_id`,`nickname`,`rank`,`selfintro`,`challenge_score` FROM `al_server_gs`.`legion_members`;

-- ==== legions  <-  al_server_gs.legions ====
DELETE FROM `aion_emu`.`legions`;
INSERT INTO `aion_emu`.`legions` (`id`,`name`,`level`,`contribution_points`,`deputy_permission`,`centurion_permission`,`legionary_permission`,`volunteer_permission`,`disband_time`,`rank_cp`,`rank_pos`,`old_rank_pos`,`description`,`joinType`,`minJoinLevel`,`territory`) SELECT `id`,`name`,`level`,`contribution_points`,`deputy_permission`,`centurion_permission`,`legionary_permission`,`volunteer_permission`,`disband_time`,`rank_cp`,`rank_pos`,`old_rank_pos`,`description`,`joinType`,`minJoinLevel`,`territory` FROM `al_server_gs`.`legions`;

-- ==== limited_quest_counters  <-  al_server_gs.limited_quest_counters ====
DELETE FROM `aion_emu`.`limited_quest_counters`;
INSERT INTO `aion_emu`.`limited_quest_counters` (`quest_id`,`remaining`) SELECT `quest_id`,`remaining` FROM `al_server_gs`.`limited_quest_counters`;

-- ==== mail  <-  al_server_gs.mail ====
DELETE FROM `aion_emu`.`mail`;
INSERT INTO `aion_emu`.`mail` (`mail_unique_id`,`mail_recipient_id`,`sender_name`,`mail_title`,`mail_message`,`unread`,`attached_item_id`,`attached_kinah_count`,`attached_ap_count`,`express`,`recieved_time`) SELECT `mail_unique_id`,`mail_recipient_id`,`sender_name`,`mail_title`,`mail_message`,`unread`,`attached_item_id`,`attached_kinah_count`,`attached_ap_count`,`express`,`recieved_time` FROM `al_server_gs`.`mail`;

-- ==== old_names  <-  al_server_gs.old_names ====
DELETE FROM `aion_emu`.`old_names`;
INSERT INTO `aion_emu`.`old_names` (`id`,`player_id`,`old_name`,`new_name`) SELECT `id`,`player_id`,`old_name`,`new_name` FROM `al_server_gs`.`old_names`;

-- ==== outpost_location  <-  al_server_gs.outpost_location ====
DELETE FROM `aion_emu`.`outpost_location`;
INSERT INTO `aion_emu`.`outpost_location` (`id`,`race`) SELECT `id`,`race` FROM `al_server_gs`.`outpost_location`;

-- ==== petitions  <-  al_server_gs.petitions ====
DELETE FROM `aion_emu`.`petitions`;
INSERT INTO `aion_emu`.`petitions` (`id`,`player_id`,`type`,`title`,`message`,`add_data`,`time`,`status`) SELECT `id`,`player_id`,`type`,`title`,`message`,`add_data`,`time`,`status` FROM `al_server_gs`.`petitions`;

-- ==== player_appearance  <-  al_server_gs.player_appearance ====
DELETE FROM `aion_emu`.`player_appearance`;
INSERT INTO `aion_emu`.`player_appearance` (`player_id`,`voice`,`skin_rgb`,`hair_rgb`,`eye_rgb`,`lip_rgb`,`face`,`hair`,`deco`,`tattoo`,`face_contour`,`expression`,`pupil_shape`,`remove_mane`,`right_eye_rgb`,`eye_lash_shape`,`jaw_line`,`forehead`,`eye_height`,`eye_space`,`eye_width`,`eye_size`,`eye_shape`,`eye_angle`,`brow_height`,`brow_angle`,`brow_shape`,`nose`,`nose_bridge`,`nose_width`,`nose_tip`,`cheek`,`lip_height`,`mouth_size`,`lip_size`,`smile`,`lip_shape`,`jaw_height`,`chin_jut`,`ear_shape`,`head_size`,`neck`,`neck_length`,`shoulder_size`,`torso`,`chest`,`waist`,`hips`,`arm_thickness`,`hand_size`,`leg_thickness`,`facial_rate`,`foot_size`,`arm_length`,`leg_length`,`shoulders`,`face_shape`,`pupil_size`,`upper_torso`,`fore_arm_thickness`,`hand_span`,`calf_thickness`,`height`) SELECT `player_id`,`voice`,`skin_rgb`,`hair_rgb`,`eye_rgb`,`lip_rgb`,`face`,`hair`,`deco`,`tattoo`,`face_contour`,`expression`,`pupil_shape`,`remove_mane`,`right_eye_rgb`,`eye_lash_shape`,`jaw_line`,`forehead`,`eye_height`,`eye_space`,`eye_width`,`eye_size`,`eye_shape`,`eye_angle`,`brow_height`,`brow_angle`,`brow_shape`,`nose`,`nose_bridge`,`nose_width`,`nose_tip`,`cheek`,`lip_height`,`mouth_size`,`lip_size`,`smile`,`lip_shape`,`jaw_height`,`chin_jut`,`ear_shape`,`head_size`,`neck`,`neck_length`,`shoulder_size`,`torso`,`chest`,`waist`,`hips`,`arm_thickness`,`hand_size`,`leg_thickness`,`facial_rate`,`foot_size`,`arm_length`,`leg_length`,`shoulders`,`face_shape`,`pupil_size`,`upper_torso`,`fore_arm_thickness`,`hand_span`,`calf_thickness`,`height` FROM `al_server_gs`.`player_appearance`;

-- ==== player_atreian_bestiary  <-  al_server_gs.player_atreian_bestiary ====
DELETE FROM `aion_emu`.`player_atreian_bestiary`;
INSERT INTO `aion_emu`.`player_atreian_bestiary` (`player_id`,`id`,`kill_count`,`level`,`claim_reward`) SELECT `player_id`,`id`,`kill_count`,`level`,`claim_reward` FROM `al_server_gs`.`player_atreian_bestiary`;

-- ==== player_bind_point  <-  al_server_gs.player_bind_point ====
DELETE FROM `aion_emu`.`player_bind_point`;
INSERT INTO `aion_emu`.`player_bind_point` (`player_id`,`map_id`,`x`,`y`,`z`,`heading`) SELECT `player_id`,`map_id`,`x`,`y`,`z`,`heading` FROM `al_server_gs`.`player_bind_point`;

-- ==== player_cooldowns  <-  al_server_gs.player_cooldowns ====
DELETE FROM `aion_emu`.`player_cooldowns`;
INSERT INTO `aion_emu`.`player_cooldowns` (`player_id`,`cooldown_id`,`reuse_delay`) SELECT `player_id`,`cooldown_id`,`reuse_delay` FROM `al_server_gs`.`player_cooldowns`;

-- ==== player_cp  <-  al_server_gs.player_cp ====
DELETE FROM `aion_emu`.`player_cp`;
INSERT INTO `aion_emu`.`player_cp` (`player_id`,`slot`,`point`) SELECT `player_id`,`slot`,`point` FROM `al_server_gs`.`player_cp`;

-- ==== player_effects  <-  al_server_gs.player_effects ====
DELETE FROM `aion_emu`.`player_effects`;
INSERT INTO `aion_emu`.`player_effects` (`player_id`,`skill_id`,`skill_lvl`,`current_time`,`end_time`) SELECT `player_id`,`skill_id`,`skill_lvl`,`current_time`,`end_time` FROM `al_server_gs`.`player_effects`;

-- ==== player_emotions  <-  al_server_gs.player_emotions ====
DELETE FROM `aion_emu`.`player_emotions`;
INSERT INTO `aion_emu`.`player_emotions` (`player_id`,`emotion`,`remaining`) SELECT `player_id`,`emotion`,`remaining` FROM `al_server_gs`.`player_emotions`;

-- ==== player_equipment_setting  <-  al_server_gs.player_equipment_setting ====
DELETE FROM `aion_emu`.`player_equipment_setting`;
INSERT INTO `aion_emu`.`player_equipment_setting` (`player_id`,`slot`,`name`,`display`,`m_hand`,`s_hand`,`helmet`,`torso`,`glove`,`boots`,`earrings_left`,`earrings_right`,`ring_left`,`ring_right`,`necklace`,`shoulder`,`pants`,`powershard_left`,`powershard_right`,`wings`,`waist`,`m_off_hand`,`s_off_hand`,`plume`,`bracelet`) SELECT `player_id`,`slot`,`name`,`display`,`m_hand`,`s_hand`,`helmet`,`torso`,`glove`,`boots`,`earrings_left`,`earrings_right`,`ring_left`,`ring_right`,`necklace`,`shoulder`,`pants`,`powershard_left`,`powershard_right`,`wings`,`waist`,`m_off_hand`,`s_off_hand`,`plume`,`bracelet` FROM `al_server_gs`.`player_equipment_setting`;

-- ==== player_life_stats  <-  al_server_gs.player_life_stats ====
DELETE FROM `aion_emu`.`player_life_stats`;
INSERT INTO `aion_emu`.`player_life_stats` (`player_id`,`hp`,`mp`,`fp`) SELECT `player_id`,`hp`,`mp`,`fp` FROM `al_server_gs`.`player_life_stats`;

-- ==== player_luna_shop  <-  al_server_gs.player_luna_shop ====
DELETE FROM `aion_emu`.`player_luna_shop`;
INSERT INTO `aion_emu`.`player_luna_shop` (`player_id`,`free_under`,`free_munition`,`free_chest`) SELECT `player_id`,`free_under`,`free_munition`,`free_chest` FROM `al_server_gs`.`player_luna_shop`;

-- ==== player_macrosses  <-  al_server_gs.player_macrosses ====
DELETE FROM `aion_emu`.`player_macrosses`;
INSERT INTO `aion_emu`.`player_macrosses` (`player_id`,`order`,`macro`) SELECT `player_id`,`order`,`macro` FROM `al_server_gs`.`player_macrosses`;

-- ==== player_minions  <-  al_server_gs.player_minions ====
DELETE FROM `aion_emu`.`player_minions`;
INSERT INTO `aion_emu`.`player_minions` (`player_id`,`object_id`,`minion_id`,`name`,`grade`,`level`,`birthday`,`growthpoints`,`is_locked`,`buff_bag`) SELECT `player_id`,`object_id`,`minion_id`,`name`,`grade`,`level`,`birthday`,`growthpoints`,`is_locked`,`buff_bag` FROM `al_server_gs`.`player_minions`;

-- ==== player_motions  <-  al_server_gs.player_motions ====
DELETE FROM `aion_emu`.`player_motions`;
INSERT INTO `aion_emu`.`player_motions` (`player_id`,`motion_id`,`time`,`active`) SELECT `player_id`,`motion_id`,`time`,`active` FROM `al_server_gs`.`player_motions`;

-- ==== player_npc_factions  <-  al_server_gs.player_npc_factions ====
DELETE FROM `aion_emu`.`player_npc_factions`;
INSERT INTO `aion_emu`.`player_npc_factions` (`player_id`,`faction_id`,`active`,`time`,`state`,`quest_id`) SELECT `player_id`,`faction_id`,`active`,`time`,`state`,`quest_id` FROM `al_server_gs`.`player_npc_factions`;

-- ==== player_passkey  <-  al_server_gs.player_passkey ====
DELETE FROM `aion_emu`.`player_passkey`;
INSERT INTO `aion_emu`.`player_passkey` (`account_id`,`passkey`) SELECT `account_id`,`passkey` FROM `al_server_gs`.`player_passkey`;

-- ==== player_passports  <-  al_server_gs.player_passports ====
DELETE FROM `aion_emu`.`player_passports`;
INSERT INTO `aion_emu`.`player_passports` (`account_id`,`passport_id`,`stamps`,`last_stamp`,`rewarded`) SELECT `account_id`,`passport_id`,`stamps`,`last_stamp`,`rewarded` FROM `al_server_gs`.`player_passports`;

-- ==== player_pets  <-  al_server_gs.player_pets ====
DELETE FROM `aion_emu`.`player_pets`;
INSERT INTO `aion_emu`.`player_pets` (`player_id`,`pet_id`,`decoration`,`name`,`hungry_level`,`feed_progress`,`reuse_time`,`birthday`,`mood_started`,`counter`,`mood_cd_started`,`gift_cd_started`,`dopings`,`despawn_time`,`expire_time`) SELECT `player_id`,`pet_id`,`decoration`,`name`,`hungry_level`,`feed_progress`,`reuse_time`,`birthday`,`mood_started`,`counter`,`mood_cd_started`,`gift_cd_started`,`dopings`,`despawn_time`,`expire_time` FROM `al_server_gs`.`player_pets`;

-- ==== player_punishments  <-  al_server_gs.player_punishments ====
DELETE FROM `aion_emu`.`player_punishments`;
INSERT INTO `aion_emu`.`player_punishments` (`player_id`,`punishment_type`,`start_time`,`duration`,`reason`) SELECT `player_id`,`punishment_type`,`start_time`,`duration`,`reason` FROM `al_server_gs`.`player_punishments`;

-- ==== player_quests  <-  al_server_gs.player_quests ====
DELETE FROM `aion_emu`.`player_quests`;
INSERT INTO `aion_emu`.`player_quests` (`player_id`,`quest_id`,`status`,`quest_vars`,`complete_count`,`complete_time`,`next_repeat_time`,`reward`) SELECT `player_id`,`quest_id`,`status`,`quest_vars`,`complete_count`,`complete_time`,`next_repeat_time`,`reward` FROM `al_server_gs`.`player_quests`;

-- ==== player_recipes  <-  al_server_gs.player_recipes ====
DELETE FROM `aion_emu`.`player_recipes`;
INSERT INTO `aion_emu`.`player_recipes` (`player_id`,`recipe_id`) SELECT `player_id`,`recipe_id` FROM `al_server_gs`.`player_recipes`;

-- ==== player_registered_items  <-  al_server_gs.player_registered_items ====
DELETE FROM `aion_emu`.`player_registered_items`;
INSERT INTO `aion_emu`.`player_registered_items` (`player_id`,`item_unique_id`,`item_id`,`expire_time`,`color`,`color_expires`,`owner_use_count`,`visitor_use_count`,`x`,`y`,`z`,`h`,`area`,`floor`) SELECT `player_id`,`item_unique_id`,`item_id`,`expire_time`,`color`,`color_expires`,`owner_use_count`,`visitor_use_count`,`x`,`y`,`z`,`h`,`area`,`floor` FROM `al_server_gs`.`player_registered_items`;

-- ==== player_settings  <-  al_server_gs.player_settings ====
DELETE FROM `aion_emu`.`player_settings`;
INSERT INTO `aion_emu`.`player_settings` (`player_id`,`settings_type`,`settings`) SELECT `player_id`,`settings_type`,`settings` FROM `al_server_gs`.`player_settings`;

-- ==== player_shugo_sweep  <-  al_server_gs.player_shugo_sweep ====
DELETE FROM `aion_emu`.`player_shugo_sweep`;
INSERT INTO `aion_emu`.`player_shugo_sweep` (`player_id`,`free_dice`,`sweep_step`,`board_id`) SELECT `player_id`,`free_dice`,`sweep_step`,`board_id` FROM `al_server_gs`.`player_shugo_sweep`;

-- ==== player_skill_skins  <-  al_server_gs.player_skill_skins ====
DELETE FROM `aion_emu`.`player_skill_skins`;
INSERT INTO `aion_emu`.`player_skill_skins` (`player_id`,`skin_id`,`remaining`,`active`) SELECT `player_id`,`skin_id`,`remaining`,`active` FROM `al_server_gs`.`player_skill_skins`;

-- ==== player_skills  <-  al_server_gs.player_skills ====
DELETE FROM `aion_emu`.`player_skills`;
INSERT INTO `aion_emu`.`player_skills` (`player_id`,`skill_id`,`skill_level`,`skin_id`,`skin_active_date`,`skin_expire_time`,`skin_activated`) SELECT `player_id`,`skill_id`,`skill_level`,`skin_id`,`skin_active_date`,`skin_expire_time`,`skin_activated` FROM `al_server_gs`.`player_skills`;

-- ==== player_stigmas_equipped  <-  al_server_gs.player_stigmas_equipped ====
DELETE FROM `aion_emu`.`player_stigmas_equipped`;
INSERT INTO `aion_emu`.`player_stigmas_equipped` (`player_id`,`item_id`,`item_name`) SELECT `player_id`,`item_id`,`item_name` FROM `al_server_gs`.`player_stigmas_equipped`;

-- ==== player_titles  <-  al_server_gs.player_titles ====
DELETE FROM `aion_emu`.`player_titles`;
INSERT INTO `aion_emu`.`player_titles` (`player_id`,`title_id`,`remaining`) SELECT `player_id`,`title_id`,`remaining` FROM `al_server_gs`.`player_titles`;

-- ==== player_transform  <-  al_server_gs.player_transform ====
DELETE FROM `aion_emu`.`player_transform`;
INSERT INTO `aion_emu`.`player_transform` (`player_id`,`panel_id`,`item_id`) SELECT `player_id`,`panel_id`,`item_id` FROM `al_server_gs`.`player_transform`;

-- ==== player_vars  <-  al_server_gs.player_vars ====
DELETE FROM `aion_emu`.`player_vars`;
INSERT INTO `aion_emu`.`player_vars` (`player_id`,`param`,`value`,`time`) SELECT `player_id`,`param`,`value`,`time` FROM `al_server_gs`.`player_vars`;

-- ==== player_wardrobe  <-  al_server_gs.player_wardrobe ====
DELETE FROM `aion_emu`.`player_wardrobe`;
INSERT INTO `aion_emu`.`player_wardrobe` (`player_id`,`item_id`,`slot`,`reskin_count`) SELECT `player_id`,`item_id`,`slot`,`reskin_count` FROM `al_server_gs`.`player_wardrobe`;

-- ==== players  <-  al_server_gs.players ====
DELETE FROM `aion_emu`.`players`;
INSERT INTO `aion_emu`.`players` (`id`,`name`,`account_id`,`account_name`,`exp`,`recoverexp`,`x`,`y`,`z`,`heading`,`world_id`,`gender`,`race`,`player_class`,`creation_date`,`deletion_date`,`last_online`,`quest_expands`,`advenced_stigma_slot_size`,`warehouse_size`,`mailbox_letters`,`bind_point`,`title_id`,`bonus_title_id`,`online`,`note`,`npc_expands`,`world_owner`,`dp`,`soul_sickness`,`reposte_energy`,`mentor_flag_time`,`last_transfer_time`,`stamps`,`last_stamp`,`rewarded_pass`,`passport_time`,`is_archdaeva`,`creativity_point`,`aura_of_growth`,`join_legion_id`,`join_state`,`berdin_star`,`luna_consume`,`muni_keys`,`luna_consume_count`,`wardrobe_slot`,`abyss_favor`,`luna_points`,`frenzy_points`,`frenzy_count`,`toc_floor`,`stone_cp`,`golden_dice`,`sweep_reset`,`minion_skill_points`,`minion_function_time`) SELECT `id`,`name`,`account_id`,`account_name`,`exp`,`recoverexp`,`x`,`y`,`z`,`heading`,`world_id`,`gender`,`race`,`player_class`,`creation_date`,`deletion_date`,`last_online`,`quest_expands`,`advenced_stigma_slot_size`,`warehouse_size`,`mailbox_letters`,`bind_point`,`title_id`,`bonus_title_id`,`online`,`note`,`npc_expands`,`world_owner`,`dp`,`soul_sickness`,`reposte_energy`,`mentor_flag_time`,`last_transfer_time`,`stamps`,`last_stamp`,`rewarded_pass`,`passport_time`,`is_archdaeva`,`creativity_point`,`aura_of_growth`,`join_legion_id`,`join_state`,`berdin_star`,`luna_consume`,`muni_keys`,`luna_consume_count`,`wardrobe_slot`,`abyss_favor`,`luna_points`,`frenzy_points`,`frenzy_count`,`toc_floor`,`stone_cp`,`golden_dice`,`sweep_reset`,`minion_skill_points`,`minion_function_time` FROM `al_server_gs`.`players`;

-- ==== portal_cooldowns  <-  al_server_gs.portal_cooldowns ====
DELETE FROM `aion_emu`.`portal_cooldowns`;
INSERT INTO `aion_emu`.`portal_cooldowns` (`player_id`,`world_id`,`reuse_time`,`entry_count`) SELECT `player_id`,`world_id`,`reuse_time`,`entry_count` FROM `al_server_gs`.`portal_cooldowns`;

-- ==== server_variables  <-  al_server_gs.server_variables ====
DELETE FROM `aion_emu`.`server_variables`;
INSERT INTO `aion_emu`.`server_variables` (`key`,`value`) SELECT `key`,`value` FROM `al_server_gs`.`server_variables`;

-- ==== siege_locations  <-  al_server_gs.siege_locations ====
DELETE FROM `aion_emu`.`siege_locations`;
INSERT INTO `aion_emu`.`siege_locations` (`id`,`race`,`legion_id`,`occupy_count`) SELECT `id`,`race`,`legion_id`,`occupy_count` FROM `al_server_gs`.`siege_locations`;

-- ==== skill_motions  <-  al_server_gs.skill_motions ====
DELETE FROM `aion_emu`.`skill_motions`;
INSERT INTO `aion_emu`.`skill_motions` (`motion_name`,`skill_id`,`attack_speed`,`weapon_type`,`off_weapon_type`,`race`,`gender`,`time`) SELECT `motion_name`,`skill_id`,`attack_speed`,`weapon_type`,`off_weapon_type`,`race`,`gender`,`time` FROM `al_server_gs`.`skill_motions`;

-- ==== special_landing  <-  al_server_gs.special_landing ====
DELETE FROM `aion_emu`.`special_landing`;
INSERT INTO `aion_emu`.`special_landing` (`id`,`type`) SELECT `id`,`type` FROM `al_server_gs`.`special_landing`;

-- ==== surveys  <-  al_server_gs.surveys ====
DELETE FROM `aion_emu`.`surveys`;
INSERT INTO `aion_emu`.`surveys` (`unique_id`,`owner_id`,`item_id`,`item_count`,`html_text`,`html_radio`,`used`,`used_time`) SELECT `unique_id`,`owner_id`,`item_id`,`item_count`,`html_text`,`html_radio`,`used`,`used_time` FROM `al_server_gs`.`surveys`;

-- ==== tasks  <-  al_server_gs.tasks ====
DELETE FROM `aion_emu`.`tasks`;
INSERT INTO `aion_emu`.`tasks` (`id`,`task_type`,`trigger_type`,`trigger_param`,`exec_param`) SELECT `id`,`task_type`,`trigger_type`,`trigger_param`,`exec_param` FROM `al_server_gs`.`tasks`;

-- ==== towns  <-  al_server_gs.towns ====
DELETE FROM `aion_emu`.`towns`;
INSERT INTO `aion_emu`.`towns` (`id`,`level`,`points`,`race`,`level_up_date`) SELECT `id`,`level`,`points`,`race`,`level_up_date` FROM `al_server_gs`.`towns`;

-- ==== users  <-  al_server_gs.users ====
DELETE FROM `aion_emu`.`users`;
INSERT INTO `aion_emu`.`users` (`id`,`account`,`ip`,`date`) SELECT `id`,`account`,`ip`,`date` FROM `al_server_gs`.`users`;

-- ==== veteran_rewards  <-  al_server_gs.veteran_rewards ====
DELETE FROM `aion_emu`.`veteran_rewards`;
INSERT INTO `aion_emu`.`veteran_rewards` (`id`,`player`,`type`,`item`,`count`,`kinah`,`sender`,`title`,`message`) SELECT `id`,`player`,`type`,`item`,`count`,`kinah`,`sender`,`title`,`message` FROM `al_server_gs`.`veteran_rewards`;

-- ==== web_reward  <-  al_server_gs.web_reward ====
DELETE FROM `aion_emu`.`web_reward`;
INSERT INTO `aion_emu`.`web_reward` (`unique`,`item_owner`,`item_id`,`item_count`,`added`,`received`,`rewarded`) SELECT `unique`,`item_owner`,`item_id`,`item_count`,`added`,`received`,`rewarded` FROM `al_server_gs`.`web_reward`;

-- ==== ls_account_data  <-  al_server_ls.account_data ====
DELETE FROM `aion_emu`.`ls_account_data`;
INSERT INTO `aion_emu`.`ls_account_data` (`id`,`name`,`password`,`activated`,`access_level`,`membership`,`old_membership`,`last_server`,`last_ip`,`last_mac`,`ip_force`,`expire`,`toll`,`email`,`question`,`answer`,`balance`,`luna`,`return_account`,`return_end`) SELECT `id`,`name`,`password`,`activated`,`access_level`,`membership`,`old_membership`,`last_server`,`last_ip`,`last_mac`,`ip_force`,`expire`,`toll`,`email`,`question`,`answer`,`balance`,`luna`,`return_account`,`return_end` FROM `al_server_ls`.`account_data`;

-- ==== ls_account_playtime  <-  al_server_ls.account_playtime ====
DELETE FROM `aion_emu`.`ls_account_playtime`;
INSERT INTO `aion_emu`.`ls_account_playtime` (`account_id`,`accumulated_online`,`lastupdate`) SELECT `account_id`,`accumulated_online`,`lastupdate` FROM `al_server_ls`.`account_playtime`;

-- ==== ls_account_rewards  <-  al_server_ls.account_rewards ====
DELETE FROM `aion_emu`.`ls_account_rewards`;
INSERT INTO `aion_emu`.`ls_account_rewards` (`uniqId`,`accountId`,`added`,`points`,`received`,`rewarded`,`luna`) SELECT `uniqId`,`accountId`,`added`,`points`,`received`,`rewarded`,`luna` FROM `al_server_ls`.`account_rewards`;

-- ==== ls_account_time  <-  al_server_ls.account_time ====
DELETE FROM `aion_emu`.`ls_account_time`;
INSERT INTO `aion_emu`.`ls_account_time` (`account_id`,`last_active`,`expiration_time`,`session_duration`,`accumulated_online`,`accumulated_rest`,`penalty_end`) SELECT `account_id`,`last_active`,`expiration_time`,`session_duration`,`accumulated_online`,`accumulated_rest`,`penalty_end` FROM `al_server_ls`.`account_time`;

-- ==== ls_account_vip  <-  al_server_ls.account_vip ====
DELETE FROM `aion_emu`.`ls_account_vip`;
INSERT INTO `aion_emu`.`ls_account_vip` (`account_id`,`vip_level`,`vip_exp`,`expire_time`) SELECT `account_id`,`vip_level`,`vip_exp`,`expire_time` FROM `al_server_ls`.`account_vip`;

-- ==== ls_banned_ip  <-  al_server_ls.banned_ip ====
DELETE FROM `aion_emu`.`ls_banned_ip`;
INSERT INTO `aion_emu`.`ls_banned_ip` (`id`,`mask`,`time_end`) SELECT `id`,`mask`,`time_end` FROM `al_server_ls`.`banned_ip`;

-- ==== ls_banned_mac  <-  al_server_ls.banned_mac ====
DELETE FROM `aion_emu`.`ls_banned_mac`;
INSERT INTO `aion_emu`.`ls_banned_mac` (`uniId`,`address`,`time`,`details`) SELECT `uniId`,`address`,`time`,`details` FROM `al_server_ls`.`banned_mac`;

-- ==== ls_gameservers  <-  al_server_ls.gameservers ====
DELETE FROM `aion_emu`.`ls_gameservers`;
INSERT INTO `aion_emu`.`ls_gameservers` (`id`,`mask`,`password`) SELECT `id`,`mask`,`password` FROM `al_server_ls`.`gameservers`;

-- ==== ls_player_transfers  <-  al_server_ls.player_transfers ====
DELETE FROM `aion_emu`.`ls_player_transfers`;
INSERT INTO `aion_emu`.`ls_player_transfers` (`id`,`source_server`,`target_server`,`source_account_id`,`target_account_id`,`player_id`,`status`,`time_added`,`time_performed`,`time_done`,`comment`) SELECT `id`,`source_server`,`target_server`,`source_account_id`,`target_account_id`,`player_id`,`status`,`time_added`,`time_performed`,`time_done`,`comment` FROM `al_server_ls`.`player_transfers`;

-- ==== ls_svstats  <-  al_server_ls.svstats ====
DELETE FROM `aion_emu`.`ls_svstats`;
INSERT INTO `aion_emu`.`ls_svstats` (`id`,`server`,`status`,`current`,`max`,`last_update`) SELECT `id`,`server`,`status`,`current`,`max`,`last_update` FROM `al_server_ls`.`svstats`;

-- ==== ls_tasks  <-  al_server_ls.tasks ====
DELETE FROM `aion_emu`.`ls_tasks`;
INSERT INTO `aion_emu`.`ls_tasks` (`id`,`task_type`,`trigger_type`,`exec_param`,`trigger_param`) SELECT `id`,`task_type`,`trigger_type`,`exec_param`,`trigger_param` FROM `al_server_ls`.`tasks`;

SET FOREIGN_KEY_CHECKS = 1;

-- 刷新统计信息：先清空再批量写入后，InnoDB 的行数统计仍停留在旧值（显示 0），
-- 客户端（Navicat 等）的表格列表会因此看起来「没有迁移」；这里刷新为真实行数。
-- Refresh InnoDB row-count statistics: after empty-then-bulk-load they stay stale (0), so client
-- table lists look like "nothing was migrated"; this restores real counts.
ANALYZE TABLE
  `aion_emu`.`abyss_landing`, `aion_emu`.`abyss_rank`, `aion_emu`.`announcements`, `aion_emu`.`base_location`,
  `aion_emu`.`blocks`, `aion_emu`.`bookmark`, `aion_emu`.`broker`, `aion_emu`.`challenge_tasks`,
  `aion_emu`.`competition_ranking`, `aion_emu`.`craft_cooldowns`, `aion_emu`.`event_items`, `aion_emu`.`f2paccount`,
  `aion_emu`.`friends`, `aion_emu`.`guides`, `aion_emu`.`house_bids`, `aion_emu`.`house_object_cooldowns`,
  `aion_emu`.`house_scripts`, `aion_emu`.`houses`, `aion_emu`.`ingameshop`, `aion_emu`.`inventory`,
  `aion_emu`.`item_cooldowns`, `aion_emu`.`item_drop`, `aion_emu`.`item_stones`, `aion_emu`.`ladder_player`,
  `aion_emu`.`legion_announcement_list`, `aion_emu`.`legion_emblems`, `aion_emu`.`legion_history`,
  `aion_emu`.`legion_join_requests`, `aion_emu`.`legion_members`, `aion_emu`.`legions`,
  `aion_emu`.`limited_quest_counters`, `aion_emu`.`mail`, `aion_emu`.`old_names`, `aion_emu`.`outpost_location`,
  `aion_emu`.`petitions`, `aion_emu`.`player_appearance`, `aion_emu`.`player_atreian_bestiary`,
  `aion_emu`.`player_bind_point`, `aion_emu`.`player_cooldowns`, `aion_emu`.`player_cp`, `aion_emu`.`player_effects`,
  `aion_emu`.`player_emotions`, `aion_emu`.`player_equipment_setting`,
  `aion_emu`.`player_life_stats`, `aion_emu`.`player_luna_shop`, `aion_emu`.`player_macrosses`,
  `aion_emu`.`player_minions`, `aion_emu`.`player_motions`, `aion_emu`.`player_npc_factions`,
  `aion_emu`.`player_passkey`, `aion_emu`.`player_passports`, `aion_emu`.`player_pets`,
  `aion_emu`.`player_punishments`, `aion_emu`.`player_quests`, `aion_emu`.`player_recipes`,
  `aion_emu`.`player_registered_items`, `aion_emu`.`player_settings`, `aion_emu`.`player_shugo_sweep`,
  `aion_emu`.`player_skill_skins`, `aion_emu`.`player_skills`, `aion_emu`.`player_stigmas_equipped`,
  `aion_emu`.`player_titles`, `aion_emu`.`player_transform`, `aion_emu`.`player_vars`, `aion_emu`.`player_wardrobe`,
  `aion_emu`.`players`, `aion_emu`.`portal_cooldowns`, `aion_emu`.`server_variables`, `aion_emu`.`siege_locations`,
  `aion_emu`.`skill_motions`, `aion_emu`.`special_landing`, `aion_emu`.`surveys`, `aion_emu`.`tasks`,
  `aion_emu`.`towns`, `aion_emu`.`users`, `aion_emu`.`veteran_rewards`, `aion_emu`.`web_reward`,
  `aion_emu`.`ls_account_data`, `aion_emu`.`ls_account_playtime`, `aion_emu`.`ls_account_rewards`,
  `aion_emu`.`ls_account_time`, `aion_emu`.`ls_account_vip`, `aion_emu`.`ls_banned_ip`, `aion_emu`.`ls_banned_mac`,
  `aion_emu`.`ls_gameservers`, `aion_emu`.`ls_player_transfers`, `aion_emu`.`ls_svstats`, `aion_emu`.`ls_tasks`;

-- 行数校验 / Row-count verification：结果集为空即通过。/ an empty result set means every table matched.
SELECT * FROM (
  SELECT 'abyss_landing' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`abyss_landing`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`abyss_landing`) AS dst_rows
  UNION ALL
  SELECT 'abyss_rank' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`abyss_rank`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`abyss_rank`) AS dst_rows
  UNION ALL
  SELECT 'announcements' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`announcements`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`announcements`) AS dst_rows
  UNION ALL
  SELECT 'base_location' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`base_location`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`base_location`) AS dst_rows
  UNION ALL
  SELECT 'blocks' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`blocks`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`blocks`) AS dst_rows
  UNION ALL
  SELECT 'bookmark' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`bookmark`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`bookmark`) AS dst_rows
  UNION ALL
  SELECT 'broker' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`broker`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`broker`) AS dst_rows
  UNION ALL
  SELECT 'challenge_tasks' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`challenge_tasks`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`challenge_tasks`) AS dst_rows
  UNION ALL
  SELECT 'competition_ranking' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`competition_ranking`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`competition_ranking`) AS dst_rows
  UNION ALL
  SELECT 'craft_cooldowns' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`craft_cooldowns`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`craft_cooldowns`) AS dst_rows
  UNION ALL
  SELECT 'event_items' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`event_items`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`event_items`) AS dst_rows
  UNION ALL
  SELECT 'f2paccount' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`f2paccount`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`f2paccount`) AS dst_rows
  UNION ALL
  SELECT 'friends' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`friends`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`friends`) AS dst_rows
  UNION ALL
  SELECT 'guides' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`guides`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`guides`) AS dst_rows
  UNION ALL
  SELECT 'house_bids' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`house_bids`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`house_bids`) AS dst_rows
  UNION ALL
  SELECT 'house_object_cooldowns' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`house_object_cooldowns`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`house_object_cooldowns`) AS dst_rows
  UNION ALL
  SELECT 'house_scripts' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`house_scripts`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`house_scripts`) AS dst_rows
  UNION ALL
  SELECT 'houses' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`houses`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`houses`) AS dst_rows
  UNION ALL
  SELECT 'ingameshop' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`ingameshop`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`ingameshop`) AS dst_rows
  UNION ALL
  SELECT 'inventory' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`inventory`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`inventory`) AS dst_rows
  UNION ALL
  SELECT 'item_cooldowns' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`item_cooldowns`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`item_cooldowns`) AS dst_rows
  UNION ALL
  SELECT 'item_drop' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`item_drop`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`item_drop`) AS dst_rows
  UNION ALL
  SELECT 'item_stones' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`item_stones`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`item_stones`) AS dst_rows
  UNION ALL
  SELECT 'ladder_player' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`ladder_player`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`ladder_player`) AS dst_rows
  UNION ALL
  SELECT 'legion_announcement_list' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`legion_announcement_list`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`legion_announcement_list`) AS dst_rows
  UNION ALL
  SELECT 'legion_emblems' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`legion_emblems`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`legion_emblems`) AS dst_rows
  UNION ALL
  SELECT 'legion_history' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`legion_history`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`legion_history`) AS dst_rows
  UNION ALL
  SELECT 'legion_join_requests' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`legion_join_requests`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`legion_join_requests`) AS dst_rows
  UNION ALL
  SELECT 'legion_members' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`legion_members`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`legion_members`) AS dst_rows
  UNION ALL
  SELECT 'legions' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`legions`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`legions`) AS dst_rows
  UNION ALL
  SELECT 'limited_quest_counters' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`limited_quest_counters`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`limited_quest_counters`) AS dst_rows
  UNION ALL
  SELECT 'mail' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`mail`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`mail`) AS dst_rows
  UNION ALL
  SELECT 'old_names' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`old_names`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`old_names`) AS dst_rows
  UNION ALL
  SELECT 'outpost_location' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`outpost_location`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`outpost_location`) AS dst_rows
  UNION ALL
  SELECT 'petitions' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`petitions`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`petitions`) AS dst_rows
  UNION ALL
  SELECT 'player_appearance' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`player_appearance`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`player_appearance`) AS dst_rows
  UNION ALL
  SELECT 'player_atreian_bestiary' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`player_atreian_bestiary`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`player_atreian_bestiary`) AS dst_rows
  UNION ALL
  SELECT 'player_bind_point' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`player_bind_point`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`player_bind_point`) AS dst_rows
  UNION ALL
  SELECT 'player_cooldowns' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`player_cooldowns`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`player_cooldowns`) AS dst_rows
  UNION ALL
  SELECT 'player_cp' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`player_cp`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`player_cp`) AS dst_rows
  UNION ALL
  SELECT 'player_effects' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`player_effects`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`player_effects`) AS dst_rows
  UNION ALL
  SELECT 'player_emotions' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`player_emotions`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`player_emotions`) AS dst_rows
  UNION ALL
  SELECT 'player_equipment_setting' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`player_equipment_setting`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`player_equipment_setting`) AS dst_rows
  UNION ALL
  SELECT 'player_life_stats' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`player_life_stats`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`player_life_stats`) AS dst_rows
  UNION ALL
  SELECT 'player_luna_shop' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`player_luna_shop`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`player_luna_shop`) AS dst_rows
  UNION ALL
  SELECT 'player_macrosses' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`player_macrosses`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`player_macrosses`) AS dst_rows
  UNION ALL
  SELECT 'player_minions' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`player_minions`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`player_minions`) AS dst_rows
  UNION ALL
  SELECT 'player_motions' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`player_motions`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`player_motions`) AS dst_rows
  UNION ALL
  SELECT 'player_npc_factions' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`player_npc_factions`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`player_npc_factions`) AS dst_rows
  UNION ALL
  SELECT 'player_passkey' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`player_passkey`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`player_passkey`) AS dst_rows
  UNION ALL
  SELECT 'player_passports' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`player_passports`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`player_passports`) AS dst_rows
  UNION ALL
  SELECT 'player_pets' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`player_pets`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`player_pets`) AS dst_rows
  UNION ALL
  SELECT 'player_punishments' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`player_punishments`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`player_punishments`) AS dst_rows
  UNION ALL
  SELECT 'player_quests' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`player_quests`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`player_quests`) AS dst_rows
  UNION ALL
  SELECT 'player_recipes' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`player_recipes`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`player_recipes`) AS dst_rows
  UNION ALL
  SELECT 'player_registered_items' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`player_registered_items`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`player_registered_items`) AS dst_rows
  UNION ALL
  SELECT 'player_settings' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`player_settings`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`player_settings`) AS dst_rows
  UNION ALL
  SELECT 'player_shugo_sweep' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`player_shugo_sweep`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`player_shugo_sweep`) AS dst_rows
  UNION ALL
  SELECT 'player_skill_skins' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`player_skill_skins`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`player_skill_skins`) AS dst_rows
  UNION ALL
  SELECT 'player_skills' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`player_skills`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`player_skills`) AS dst_rows
  UNION ALL
  SELECT 'player_stigmas_equipped' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`player_stigmas_equipped`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`player_stigmas_equipped`) AS dst_rows
  UNION ALL
  SELECT 'player_titles' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`player_titles`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`player_titles`) AS dst_rows
  UNION ALL
  SELECT 'player_transform' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`player_transform`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`player_transform`) AS dst_rows
  UNION ALL
  SELECT 'player_vars' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`player_vars`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`player_vars`) AS dst_rows
  UNION ALL
  SELECT 'player_wardrobe' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`player_wardrobe`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`player_wardrobe`) AS dst_rows
  UNION ALL
  SELECT 'players' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`players`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`players`) AS dst_rows
  UNION ALL
  SELECT 'portal_cooldowns' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`portal_cooldowns`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`portal_cooldowns`) AS dst_rows
  UNION ALL
  SELECT 'server_variables' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`server_variables`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`server_variables`) AS dst_rows
  UNION ALL
  SELECT 'siege_locations' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`siege_locations`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`siege_locations`) AS dst_rows
  UNION ALL
  SELECT 'skill_motions' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`skill_motions`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`skill_motions`) AS dst_rows
  UNION ALL
  SELECT 'special_landing' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`special_landing`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`special_landing`) AS dst_rows
  UNION ALL
  SELECT 'surveys' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`surveys`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`surveys`) AS dst_rows
  UNION ALL
  SELECT 'tasks' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`tasks`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`tasks`) AS dst_rows
  UNION ALL
  SELECT 'towns' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`towns`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`towns`) AS dst_rows
  UNION ALL
  SELECT 'users' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`users`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`users`) AS dst_rows
  UNION ALL
  SELECT 'veteran_rewards' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`veteran_rewards`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`veteran_rewards`) AS dst_rows
  UNION ALL
  SELECT 'web_reward' AS tbl, (SELECT COUNT(*) FROM `al_server_gs`.`web_reward`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`web_reward`) AS dst_rows
  UNION ALL
  SELECT 'ls_account_data' AS tbl, (SELECT COUNT(*) FROM `al_server_ls`.`account_data`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`ls_account_data`) AS dst_rows
  UNION ALL
  SELECT 'ls_account_playtime' AS tbl, (SELECT COUNT(*) FROM `al_server_ls`.`account_playtime`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`ls_account_playtime`) AS dst_rows
  UNION ALL
  SELECT 'ls_account_rewards' AS tbl, (SELECT COUNT(*) FROM `al_server_ls`.`account_rewards`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`ls_account_rewards`) AS dst_rows
  UNION ALL
  SELECT 'ls_account_time' AS tbl, (SELECT COUNT(*) FROM `al_server_ls`.`account_time`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`ls_account_time`) AS dst_rows
  UNION ALL
  SELECT 'ls_account_vip' AS tbl, (SELECT COUNT(*) FROM `al_server_ls`.`account_vip`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`ls_account_vip`) AS dst_rows
  UNION ALL
  SELECT 'ls_banned_ip' AS tbl, (SELECT COUNT(*) FROM `al_server_ls`.`banned_ip`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`ls_banned_ip`) AS dst_rows
  UNION ALL
  SELECT 'ls_banned_mac' AS tbl, (SELECT COUNT(*) FROM `al_server_ls`.`banned_mac`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`ls_banned_mac`) AS dst_rows
  UNION ALL
  SELECT 'ls_gameservers' AS tbl, (SELECT COUNT(*) FROM `al_server_ls`.`gameservers`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`ls_gameservers`) AS dst_rows
  UNION ALL
  SELECT 'ls_player_transfers' AS tbl, (SELECT COUNT(*) FROM `al_server_ls`.`player_transfers`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`ls_player_transfers`) AS dst_rows
  UNION ALL
  SELECT 'ls_svstats' AS tbl, (SELECT COUNT(*) FROM `al_server_ls`.`svstats`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`ls_svstats`) AS dst_rows
  UNION ALL
  SELECT 'ls_tasks' AS tbl, (SELECT COUNT(*) FROM `al_server_ls`.`tasks`) AS src_rows, (SELECT COUNT(*) FROM `aion_emu`.`ls_tasks`) AS dst_rows
) AS v WHERE v.src_rows <> v.dst_rows;
