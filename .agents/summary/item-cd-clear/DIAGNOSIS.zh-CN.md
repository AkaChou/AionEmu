# 物品 CD「清不掉」定位与修复（164000139 类任务道具）

## 触发与症状

- 实机：使用 `164000139`（Neith's Sleepstone，`<actions><skilluse skillid="9834"/>`，
  `<uselimits usearea="IDElim_ItemUse" usedelayid="62" usedelay="60000"/>`）后跑 `//cooldown`，
  客户端图标上的冷却扫描仍在转，且道具置灰不能使用 —— 拦住用的是**客户端**，不是服务端。
- 用户口径：「任务道具有时不能清 cd」。「有时」= 服务端 `item_cooldowns` 表里当时有没有该 delayId 的记录。
- 客户端侧数据与仓库 XML 一致：`<客户端解包根>/Items1_unpacked/client_items_misc.xml` 条目 164000139
  声明 `use_delay_type_id=62`、`use_delay=60000`、`activation_skill=item_deva_sleep_idelim`、`area_to_use=IDElim_ItemUse`。

## 权威证据链（真端：`<真端根>/server58-source`）

1. 真端物品 CD 的计时器由**客户端**持有：
   - 服务端唯一的下发点是 `User_Start`（`classes/Account/User.cpp:41269` 内的 `FUN_140531ca0` 调用），
     即 `S_LOAD_ITEM_COOLTIME`（`TRUE_SERVER_PROTOCOLS.md` slot 103）。
   - 下发器 `fun/fun_052.cpp:2134`（日志名 `SendItemUseDelay`）**只发还有剩余秒数的条目**
     （`now < deadline && (deadline-now)/1000 != 0`），按 typeId 1..0xbf 扫描；条目 10 字节
     `typeId(H) + leftSec(D) + oriSec(D)`，与本仓库 `SM_ITEM_COOLDOWN.writeImpl` 结构一致。
   - 客户端会把自身冷却表回报给服务端：`classes/Net/Socket.cpp:5961` → `User_LoadItemCoolTimePacket`
     （`classes/Account/User.cpp:74965`），带 `[CRITICAL] LoadItemCoolTimePacket, Invalid Data, ... Reset To Zero~` 校验。
2. 真端**没有任何服务端「清物品 CD」函数**：用户侧物品 CD 表（用户对象偏移 `0x3790`）的全部触点只有
   检查 `User_CheckItemUseDelay`（`User.cpp:60578`）、写入 `User_SetNextItemUseDelay`（`User.cpp:60652`，
   使用道具时 `now + useDelay`）、保存 `User_SaveItemCoolTime`（`User.cpp:75066`）、载入（客户端回报）与进世界下发。
   ⇒ 服务端要改变客户端图标上的扫描，**只能**靠整表「载入」包。
3. 交叉验证：技能侧另有专门的「重置」语义（真端 slot 73 `S_RESET_SKILL_COOLING_TIME`，本仓库对应
   `SM_SKILL_COOLDOWN(..., isSkillRemove=true)`）；物品侧没有这样的包 —— 物品侧只有载入包一条路。

## 我们侧的缺陷

`Cooldown.java`、`RemoveCd.java`、`CmdItemCoolTime.java` 都把物品冷却下发写在 `if (表非空)` 里：
当服务端表为空时（使用被服务端拒绝，如区域 `IDElim_ItemUse` / `RestrictionsManager` 拦截；
或 `coolDownZero` 生效期间根本没记录冷却）**一个包都不发**，客户端上一轮本地预测的 60 秒扫描
没有任何事由被清掉，道具持续置灰 ⇒「有时不能清 cd」。
`PlayerEnterWorldService:334` 同样只在表非空时才下发，同一个客户端实例重登时残留扫描也不会被覆盖。

## 修改（Java，5 个文件）

- `network/aion/serverpackets/SM_ITEM_COOLDOWN.java`：新增 `load(Map)` 工厂，`null` 表按空表处理，
  作为「整表载入」语义的唯一出口。
- `commands/admin/Cooldown.java`、`commands/admin/RemoveCd.java`、`network/aion/gmhandler/CmdItemCoolTime.java`：
  物品冷却清空后**无条件整表下发**（保留原「把条目置 0」的写法，兼容客户端按条目解析的情形）。
- `services/player/PlayerEnterWorldService.java`：进世界时无条件整表下发（空表 = 没有任何物品冷却）。

## 验证边界

- 已完成：`git diff --check`；IDE 对改动文件检查无 error（仅存量 warning）。
- 未执行（按仓库规则需用户授权）：Maven 构建/测试、服务端重启、实机复测。
- 实机验收要点：
  1. 让 164000139 起 60 秒扫描（需要「服务端表为空」场景时，用被拒使用制造：不在 `IDElim_ItemUse`
     区域内点用道具，客户端已起扫描而服务端不记录）；
  2. 跑 `//cooldown` → 图标扫描应立刻消失且道具可点；
  3. 同一个客户端实例重登 → 上一轮残留扫描也应消失。
- 待实机确认后可沉淀为 Pattern：客户端持有型状态的下行同步必须支持「空态覆盖」。
