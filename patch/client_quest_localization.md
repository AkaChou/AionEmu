# Aion 5.8 中文任务词典补丁

任务页面和任务摘要由客户端本地 `L10N/CHS/Data/data.pak` 渲染，服务端只下发任务 ID、状态和进度变量。任务 `1153` 的服务端交付 NPC 是西亚诺（`203150`），但 5.8 中文客户端的 `STR_DIC_N_Hianu` 丢失了词典显示名和分隔符，导致客户端直接显示异常词典文本。

对权威中文包中 9,116 个 active 任务页面、11,972 个词典引用进行交叉扫描后，共确认 3 个“基础语言有显示名和分隔符、中文条目丢失”的同型缺陷：

| 词典条目 | 影响任务 | 修复后的显示名 |
| --- | --- | --- |
| `STR_DIC_N_Hianu` | `1153` | 西亚诺 |
| `STR_DIC_I_quest_23020a` | `23020` | 污染的伊德凝胶箱子碎块 |
| `STR_DIC_I_noblemetal_d_q5303_65a` | `25303`、`25313` | 佩尔农露水 |

仓库内已经生成可直接覆盖的两个完整字符串表：

```text
patch/L10N/CHS/Data/Strings/client_strings_dic_people.xml
patch/L10N/CHS/Data/Strings/client_strings_dic_item.xml
```

将 `patch/L10N/CHS/Data/Strings` 中的文件复制到客户端的 `L10N/CHS/Data/Strings`。若客户端配置禁用了 loose-file 覆盖，则用这两个文件替换完整解包目录中的同名文件，再重新打包 `L10N/CHS/Data/data.pak`。

补丁必须从 SHA-256 为 `ac54a2ee01aef730500513c05755e32d8c2d4ee2eb9443ecb323a47717f99553` 的权威中文 `data.pak` 解包内容生成：

```bash
rtk python3 scripts/generate_client_quest_localization_patch.py \
  --strings-dir /path/to/unpacked/L10N/CHS/Data/Strings
```

生成器会核对原始错误文本，只修正这 3 个条目；来源内容漂移时会直接失败。
