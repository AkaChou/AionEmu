# P0a 重冻（QE-112 落地后）：owner-identity + raw vars 两探针重跑

> 计划 §10.3-#2 · 2026-10-01 · 触发 = QE-112 落地批 `aa4c990f9`
> 只读审计；**未启停服务器**（raw vars 走本地开发库的一次只读 SELECT）。

## 1. 为什么要重跑

P0a 开工时 QE-112（DD 行阶梯 + 4 行翻 owner + 指纹重冻）仍在飞，`qe112-inflight-status.md` 明确记录：
「owner-identity.tsv 基于 QE-112 未提交版本；**正式冻结须等 QE-112 落地或用户裁决后重跑**」。
本批即执行该纪律：QE-112 已落地 ⇒ 以 HEAD 的台账/目录重出 owner 矩阵，并复跑存档分布审计。

## 2. 工具面（本批顺带修根解析）

两个 P0a 工具原先硬编码本机绝对路径，改为 ENVIRONMENT.md 的**同宿主目录约定**
（由 `__file__` 向上找 `pom.xml` 推 `<仓库根>`，再取 `../58Server`、`../PycharmProjects/unpak/Quest_unpacked`；
支持 `<workspace>` 与 `HOME` 两种布局，失败即 fail-closed 报错）：

- `p0a/tools/owner_identity.py`
- `p0a/tools/raw_vars_probe.py`

## 3. 结果

| 探针 | 输出 | 相对上一快照 |
|---|---|---|
| `owner_identity.py` | `owner-identity.tsv`（9233 行） | **仅 2 行差**：18213/28213 `XML_RETENTION → RETAIL_TABLE`、`xml_in_repo True → False`（P5D 步 3 激活批，QE-112 的 4 行翻转已包含在上一快照里——当时台账已是未提交版本） |
| `raw_vars_probe.py` | `raw-vars-probe-output.txt` | **逐字节一致**：retail-owned distinct (quest,vars) = 617、rows = 956、`canonical 916 / packed_unexpected 40` |

结论：QE-112 落地未改变存档分布口径（存档审计本就是读 DB 现网行，与编译形无关），owner 矩阵中 QE-112 的
4 行翻转此前已在快照内、本次补齐的是 P5D 步 3 的两行 —— 即 **owner 快照现已与 HEAD 台账一致**。

## 4. 边界与后续

- raw vars 分布来自**本地开发库**的一次只读 SELECT（`player_quests`）；不代表生产在线数据，也未做写回。
- `packed_unexpected 40` 是既有存档形状债（旧 XML 形写入的 var），其逐条归因不属本批；QE-112 的 3123=[1] 等
  样本属该类（旧形存档），随各族切换后的旧存档自愈面处置。
- 后续任何 owner 面变更（P7 DD 切换、P8 typed 车道删除）必须再重跑本批两步，本文件即为「重冻纪律」的落点。
