# Async Screenshot

![MC](https://img.shields.io/badge/MC-1.18--26.2-blue)
![Fabric](https://img.shields.io/badge/Fabric-✓-green)
![Forge](https://img.shields.io/badge/Forge-✓-green)
![NeoForge](https://img.shields.io/badge/NeoForge-✓-green)
![License](https://img.shields.io/badge/License-GPL--3.0-blue)

> 让 F2 截图不再卡顿（PNG 编码 / 磁盘写入移出渲染线程）。

---

## ✨ 特性（Features）

- ✅ 截图保存异步化，不再阻塞渲染线程
- ✅ PNG 编码 / 磁盘写入在后台线程完成
- ✅ 完成时回到主线程显示系统消息（`Screenshot saved to ...`）
- ✅ 连续截图串行化写盘，无文件冲突
- ✅ 无配置即可用；Fabric / Forge / NeoForge，MC 1.18 – 26.2

## 🎯 解决什么问题（The Problem）

按 F2 截图时，PNG 编码与磁盘写入在渲染线程同步执行，画面会卡顿数十毫秒；
连续截图（视频录制 / 拍屏）时严重掉帧。高分辨率 / 大纹理包下问题更明显。

## ⚙️ 工作原理（How It Works）

- Hook 点：`ScreenshotHelper.save`（Mojang 名，经 mappings.dev 验证）
- 机制：Mixin 拦截 → 同步段只做「抓取像素数据快照」（内存拷贝，快）→
  后台线程执行「PNG 编码 + 文件写入 + 文件名唯一化」→
  `Minecraft.getInstance().execute(...)` 回主线程显示系统消息。
- 多版本：`VersionProbe` 反射能力探测 + `docs/mappings.md` 对照表，
  1.18 – 26.2 全覆盖；某版本 Hook 探测失败则静默禁用本功能。

## 📦 安装（Installation）

1. 安装对应加载器（Fabric / Forge / NeoForge）与合适版本。
2. 将 `AsyncScreenshot-<version>+<loader>.jar` 放入 `.minecraft/mods`。
3. 启动游戏即生效（客户端 mod，无需服务端）。

## 🕹️ 使用（Usage）

无需配置，装好即用：按 F2（或截图键）截图即可。若某极端版本 Hook 探测失败，
本模组自动退化为原版截图行为（不做任何事）。

## 🔨 从源码构建（Build from Source）

```bash
./gradlew shadowJar   # 产出 build/libs/AsyncScreenshot-<version>+<loader>.jar
```

## 📄 兼容性（Compatibility）

| MC | Fabric | Forge | NeoForge |
|---|---|---|---|
| 1.18 – 26.2 | ✅ | ✅ | ✅ |

> 说明：若某版本某 Hook 探测失败将**静默禁用**该功能（不崩溃）。

## 📜 开源许可（License）

[GPL-3.0](LICENSE) — 架构参考自 [MinerTrack](https://github.com/At87668/MinerTrack)（FastReflection）。

## 💬 已知限制（Known Limitations）

- 同步段仍需一次像素抓取（内存拷贝），极端大分辨率下存在极短占用。
- 截图文件名提示在极端大分辨率下可能延迟显示（后台编码完成后才回抛消息）。
