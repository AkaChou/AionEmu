# `bm_restrict_category` 语义坐实（写出侧 / 消费侧 / 位名表 / 数据分布）

> 主题：解决计划 §10.3-#12（QE-114）——`quest.xml` 的 `bm_restrict_category` 到底是「128 位地图位集」
> 还是别的轴；双向对拍后给出本服可落地的判定口径。
> 日期：2026-10-01。分支：`quest`。
> 证据来源：`<真端根>/server58-source`（反编译源码）+ `<真端根>/MainServer/Server64.exe`（位名表实际数据）。
> 口径：只采信真端服务端源码与二进制数据；本服实现按 §5 落地。

---

## 1. 结论（一句话）

`bm_restrict_category` **不是**地图位集，而是**账号限制类别下标**（服务端存 1 字节，`<0 → 0`、`>8 → 8`）。
真端判定为「**玩家限制位图的第 `类别 + 19` 位为 1 ⇒ 拒绝接取**」；位 `20..23` 的名字是
`quest_acquire1..4`。本服无计费/账号类型来源 ⇒ 玩家位集为空（真端全订阅账号同形）⇒ 类别 1 的行
**按真端可接取**，不再 fail-closed。

---

## 2. 写出侧：服务端只存 1 字节类别下标

| 进程 | 位置 | 行为 |
|---|---|---|
| NPCServer | `NPCServer_NPCSvr64/fun/fun_052.cpp:3723-3739`（`Quest::Set`，属性 index `0x128`） | 解析整数 → 负值写 0；`> 8` 截到 8；写入 `param_1 + 0x7c8c`（**1 字节**） |
| MainServer | `MainServer_Server64/fun/fun_249.cpp:4117-4135`（同名属性） | 同上语义，写入 `param_1 + 0x1f23`（**1 字节**） |

两处都是「解析 → clamp 到 `[0, 8]` → 存字节」，**没有任何 128 位展开**。

## 3. 消费侧：位下标 = 类别 + 19

`NPCServer_NPCSvr64/classes/Quest/Quest.cpp:142-217`（`Quest::CanAcquireQuest`）：

```c
cVar2 = *(char *)(puVar16 + 0x1f23);        // 行声明的类别（+0x7c8c 同一字段）
if (cVar2 != '\0') {
    ... 取玩家对象 128 位位图（param_3 + 0x1067 起，两段 OR 合并到 local_e8[0..1]）...
    iVar4 = FUN_1402d10a0(local_e8, cVar2);
    if (iVar4 == 0) { ... "Quest::CanAcquireQuest, Q(%d) user(%d) bmRestricted 0x%I64x out of %d" ... 拒绝 }
}
```

`NPCServer_NPCSvr64/fun/fun_055.cpp:3247`：

```c
uint64_t FUN_1402d10a0(int64_t bits, unsigned char category) {
  if (category != 0 && (uVar1 = category + 0x13 /* +19 */, uVar1 < 0x44 /* 68 */)
      && ((bits[uVar1 >> 6] >> (uVar1 & 0x3f)) & 1) != 0) return 0;   // 位置位 ⇒ 拒绝
  return 1;                                                          // 否则可接取
}
```

即：**位 `类别 + 19` 置位 ⇒ 拒绝**，位置为空 ⇒ 放行；类别 0 完全不判。类别由写出侧 clamp 到 `0..8`。

## 4. 位名表：`类别 + 19` 落在 `quest_acquire1..4`

位名表是 MainServer 里的 68 项全局数组（`PTR_u_chat_141122090`，`FUN_140db5c30` 用它把
`bm_restrict.xml` 的 `restrictN` 名单转成位集）。从 `<真端根>/MainServer/Server64.exe` 的 `.rdata`
（VA `0x141122090`）实读：

| 下标 | 名字 | | 下标 | 名字 |
|---|---|---|---|---|
| 0–19 | `chat`, `shout`, …, `warehouse_use` | | 26–31 | `instance_cooltime1..6` |
| **20** | **`quest_acquire1`** | | 32–37 | `item_equip1..6` |
| **21** | **`quest_acquire2`** | | 38–46 | `level_up`, `combine_skillpoint`, … |
| **22** | **`quest_acquire3`** | | 47–57 | `char_slot_1..11` |
| **23** | **`quest_acquire4`** | | 58–67 | `party_match_use`, …, `guild_warehouse_deposit` |
| 24–25 | `channel_chat_write1/2` | | | |

⇒ **类别 1..4 才是「任务接取限制」**（`quest_acquire1..4`）；5..8 落到频道/副本冷却等非任务位名。

复现（PE 解析 + 读数组）：

```bash
python3 - <<'PY'
import struct, pathlib
data = pathlib.Path('<真端根>/MainServer/Server64.exe').read_bytes()
e=struct.unpack_from('<I',data,0x3c)[0]; coff=e+4
nsec=struct.unpack_from('<H',data,coff+2)[0]; opt_size=struct.unpack_from('<H',data,coff+16)[0]
opt=coff+20; base=struct.unpack_from('<Q',data,opt+24)[0]; sec=opt+opt_size; sections=[]
for i in range(nsec):
    o=sec+i*40; name=data[o:o+8].rstrip(b'\0').decode()
    vs,va,rs,ra=struct.unpack_from('<IIII',data,o+8); sections.append((name,va,vs,ra,rs))
def off(va):
    for _,vaddr,vsize,raddr,rsize in sections:
        if vaddr<=va<vaddr+max(vsize,rsize): return raddr+(va-vaddr)
def deref(ptr):
    o=off(ptr-base); raw=data[o:o+300]; end=raw.find(b'\0\0')
    return raw[:end+1].decode('utf-16-le','replace')
o=off(0x141122090-base)
for i,ptr in enumerate(struct.unpack_from('<68Q',data,o)): print(i, deref(ptr))
PY
```

## 5. 位集来源与数据分布（决定本服口径）

- 位集来源：账号类型/计费侧。`MainServer_Server64/classes/Misc/BillInfoMgr.cpp:110-215` 加载
  `bm_restrict.xml`（`id` → `account_type` + `restrict1..N` 名单），名单经 §4 的位名表转位。
- 各区域 `bm_restrict.xml`（`<真端根>/Map/XML{,/USA,/Europe,/Japan,/Taiwan,/China}/bm_restrict.xml`）
  **没有任何一条 restrict 名单包含 `quest_acquire*`** ⇒ 现网账号类型永远不会点亮这些位。
- `quest.xml` 数据分布：全表 **3477/10035** 行声明该列，**取值只有 `1`**（无其它类别）；
  各族已切换行的声明数：SimpleTalk 982、SimpleHunt 507、SimpleSerialHunt 2。
- 本服没有计费/账号类型子系统 ⇒ 玩家限制位集 = **空位集**（真端全订阅账号的 restrict 名单同样为空）
  ⇒ `quest_acquire1` 位为 0 ⇒ 类别 1 的行**可接取**。

## 6. 本服落地（同批实现）

- `NativeQuestStartPort.restrictCategory(row)`：与真端同形解析（缺列/负值 → 0、`>8` → 8）。
- `NativeQuestStartPort.RestrictionBitmap`：玩家限制位图端口，生产实现 `EMPTY`（无计费来源），
  判定 `bit(类别 + 19)` 置位 ⇒ `Outcome.BM_RESTRICT_BLOCKED`（拒绝建档）。
- 位下标常量 `QUEST_ACQUIRE_FIRST_BIT = 20`；测试注入置位位图验证拒绝面与位下标
  （`NativeQuestStartPortTest`），`EarlyElyosQuestRegressionTest` 的 1414/1691 改为
  「位集为空 ⇒ 真端可接取 + 前置轴拒接」的门态。
- 边界：若将来引入计费/账号类型，只需替换 `RestrictionBitmap` 的生产实现，判定公式不变。
