# KoHs Anchor's Bridge

[![GitHub](https://img.shields.io/badge/GitHub-Source-6f2cff?style=for-the-badge&logo=github)](https://github.com/kerlycanelita/KoHs-Anchors-Bridge)
[![Issues](https://img.shields.io/badge/Report-Issues-a855f7?style=for-the-badge&logo=githubissues)](https://github.com/kerlycanelita/KoHs-Anchors-Bridge/issues)
[![Discord](https://img.shields.io/badge/Join-Discord-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/9t2VxEF7UU)

The server side of [KoHs Anchor's](https://github.com/kerlycanelita/KoHs-Anchors). With it, the mod's
**Anchors Server** tab turns green: the server says which anchor options it allows, tells players
which anchors are theirs, and answers their pings.

One jar for every platform, Minecraft 1.21.11 to 26.3, Java 21:

| Platform | What it does |
| --- | --- |
| Paper, Purpur, Spigot, Bukkit, Folia | The bridge itself (`config.yml`) |
| Velocity, BungeeCord, Waterfall | The network's limits; the bridge runs on each backend server |

## What it adds

- **Better glow enemy anchors**: when a respawn anchor is placed, every player with the mod in that
  world is told whether it is theirs or someone else's (never who). Their own anchors never show as
  an enemy's.
- **Real latency**: answers the mod's ping once a second.
- **The anchor chain** (off until you allow it): the no-wait chain and the instant detonation click
  change *when* clicks reach your server. With `anchor-chain.enabled: true` players who switch them
  on can use them, and with Grim installed the bridge tells Grim, through its API, that the chain is
  allowed: a flag of the listed checks is let through only for those players and only within the
  window after they acted on a respawn anchor.

It never changes a block, an item, an attack or a movement. Players without the mod see no
difference. The mod's [legitimacy audit](https://github.com/kerlycanelita/KoHs-Anchors/blob/main/docs/audits/legitimacy.md)
explains every feature.

## Commands and permissions

- `/kohsbridge [status|reload]` (`kohsanchors.bridge.admin`, ops).
- `kohsanchors.bridge.chain` (everyone): may use the anchor chain where `config.yml` allows it.

## Building

```
./gradlew build
```

Made by **Zymekoh** (a.k.a. Kohzemyora) · MIT License · KoHs on top.
