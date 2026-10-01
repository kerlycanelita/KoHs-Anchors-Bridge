<!-- Modrinth summary: The server side of KoHs Anchor's: your server decides which anchor options its players may use, and tells them which anchors are theirs. -->
<p align="center"> <img src="https://raw.githubusercontent.com/kerlycanelita/KoHs-Anchors-Bridge/main/icon.png" alt="KoHs Anchor's Bridge icon" width="220"> </p>
<p align="center"> <a href="https://github.com/kerlycanelita/KoHs-Anchors-Bridge"><img src="https://img.shields.io/badge/GitHub-Source-6f2cff?style=for-the-badge&logo=github" alt="GitHub"></a> <a href="https://github.com/kerlycanelita/KoHs-Anchors-Bridge/issues"><img src="https://img.shields.io/badge/Report-Issues-a855f7?style=for-the-badge&logo=githubissues" alt="Issues"></a> <a href="https://discord.gg/9t2VxEF7UU"><img src="https://img.shields.io/badge/Join-Discord-5865F2?style=for-the-badge&logo=discord&logoColor=white" alt="Discord"></a> </p>

# KoHs Anchor's Bridge

**The server side of [KoHs Anchor's](https://github.com/kerlycanelita/KoHs-Anchors): your server decides
which anchor options its players may use, and tells them which anchors are theirs.**

Players with KoHs Anchor's see the bridge in their *Anchors Server* tab, which turns green on your
server. Players without the mod see no difference.

## 🟪 What it adds

- **Better glow enemy anchors** — when a respawn anchor is placed, every player with the mod in that
  world is told whether it is theirs or someone else's (never who), so their own anchors never show
  as an enemy's.
- **Real latency** — answers the mod's ping once a second, for its connection-dependent waits.
- **The anchor chain** — off until you allow it. The no-wait chain and the instant detonation click
  change *when* clicks reach your server; with `anchor-chain.enabled: true`, players who switch them
  on may use them. With Grim installed, the bridge tells Grim through its API that the chain is
  allowed: a flag of the listed checks is let through only for those players, only within the window
  after they acted on a respawn anchor.

## ⚠️ Read this once

The bridge never changes a block, an item, an attack, a movement or a rule of the game: your server
keeps deciding everything. The mod's
[legitimacy audit](https://github.com/kerlycanelita/KoHs-Anchors/blob/main/docs/audits/legitimacy.md)
explains every feature and what it sends.

## Install

| Platform | Put the jar in | What it does |
|---|---|---|
| Paper · Purpur · Spigot · Bukkit · Folia | `plugins/` | The bridge (`plugins/KoHsAnchorsBridge/config.yml`, `/kohsbridge reload`) |
| Velocity · BungeeCord · Waterfall | `plugins/` of the proxy | The network's limits; the bridge must also be on each backend server |

One jar for Minecraft 1.21.11 to 26.3, Java 21 or newer. `/kohsbridge` shows the policy and the
players using the mod (`kohsanchors.bridge.admin`); `kohsanchors.bridge.chain` lets a player use the
anchor chain where it is allowed.

Made by **Zymekoh** (a.k.a. Kohzemyora) · MIT License · KoHs on top.
