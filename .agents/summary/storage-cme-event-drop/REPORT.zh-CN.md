# 背包裸 LinkedHashMap 并发迭代 CME 收口（活动掉落链）

日期：2026-10-08
状态：修复完成，验证 PENDING（未编译/未跑测试/未实机复测，按 AGENTS.md 规则需用户授权）

## 现象

`2026-10-08 19:07:09`，活动掉落定时任务（`pool-5-thread-30`）抛
`java.util.ConcurrentModificationException`：

```
LinkedHashMap$LinkedHashIterator.nextNode
  ← ItemStorage.getCubeItems(ItemStorage.java:159)
  ← ItemStorage.isFull(ItemStorage.java:135)
  ← Storage.isFull(320/335)
  ← ItemService.addItem(329 → 211 → 96)
  ← ItemService.dropItemToInventory(618)
  ← EventTemplate.dropInventoryItem(200)
  ← EventTemplate.lambda$Start$1(172)
  ← PlayerContainer.doOnAllPlayers(111)   [scheduleAtFixedRate 调度线程]
```

## 根因

`ItemStorage.items` 是裸 `LinkedHashMap`，storage 包内原无任何同步设施：

- 后台事件掉落线程经 `doOnAllPlayers` 对每名在线玩家调用
  `dropItemToInventory` → `isFull` → `getCubeItems()`，该方法迭代
  `items.values()` 构建魔立方副本；
- 同一时刻玩家 IO 线程（移动/丢弃/交易等）并发 `putItem`/`removeItem`
  对同一 map 做结构性修改 → fail-fast 迭代器抛 CME。

`doOnAllPlayers` 只保护玩家集合本身的遍历，不保护访问器内部对玩家
物品存储的访问；本例中玩家 Kk 于 19:05:52 登录，19:07:09 掉落周期
触发时其背包恰被并发修改。

## 修复（单点收敛 `ItemStorage.java`）

1. `items` 改为 `Collections.synchronizedMap(new LinkedHashMap<>())`：
   单次读写（`get`/`remove`/`size`/`containsKey`）自带同一监视器互斥；
2. 类内全部 6 处裸迭代（`getItems`/`getFirstItemById`/`getItemsById`/
   `getSlotIdByItemId`/`getSpecialCubeItems`/`getCubeItems`）以
   `synchronized (items)` 包锁——与 synchronizedMap 共用同一把锁；
3. `putItem` 的 `containsKey` + `put` 复合操作同样包锁，保持原子性；
4. 类级 Javadoc 声明并发语义：禁止在锁外裸迭代 `items`。

## 影响面核对

- `Storage` 全部经 `itemStorage.xxx()` 方法委托（逐行核对，无绕过）；
- `Equipment` 用独立 `TreeMap` 且仅玩家线程操作，不在本崩溃链，未动；
- 类级 `@Getter` 对 `items` 不生成 getter（与手写 `getItems()` 同名，
  Lombok 按方法名跳过），map 不会外泄；
- `Storage` 内部快照/恢复类（:389）经 `getItems()` 副本迭代，安全；
- lint 仅报存量 WARNING（未使用方法等），无新增问题。

## 遗留观察（不在本次范围）

- `EventTemplate.isStarted` 的 check-then-set 非原子（`Start`/`Stop`
  并发双启动风险），与本链无关，未扩大修改范围。

## 验证建议（PENDING）

1. 重启实机服务端（IDEA 常驻进程，改动须重启生效）；
2. 触发含 `inventory_drop` 的活动，观察至少一个掉落周期日志无
   `ConcurrentModificationException`；
3. 如需单元级回归，可授权跑既有 quest 背包相关测试
   （`PlayerQuestInventoryPortTest`/`PlayerQuestRewardPortTest` 覆盖
   ItemStorage 路径）。

## AOCI

本次修改了受管理对象 `ItemStorage.java`；AOCI 索引处于未完成
bootstrap 状态（2026-10-08 用户已裁决暂不推进），认知条目未同步，
未调用 maintain 以避免重复触发既有裁决。
