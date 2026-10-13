# UJobs

A modern jobs plugin for Paper/Folia servers.

Unlike traditional jobs plugins, players do not need to manually join a job; they earn progress in every job simultaneously while they play.

## What this project is

`UJobs` is a skills/jobs system plugin built for Paper 1.21+ servers. It tracks player actions (like mining, farming, combat, fishing, trading, and more), awards XP and money per configured job action, and presents progression with in-game GUIs, leaderboards, placeholders, boss bars, NPC/hologram integrations, and flexible storage backends (`pdc`, `sqlite`, `mysql`, `postgresql`).

## Custom items, blocks and furniture

UJobs works with [ItemsAdder](https://www.spigotmc.org/resources/73355/), [Oraxen](https://github.com/oraxen/oraxen), [Nexo](https://nexomc.com/) and [CraftEngine](https://github.com/Xiao-MoMi/craft-engine). Use their content in `jobs.yml` exactly like vanilla values, written as `<plugin>:<id>`:

| Plugin | Format | Example |
| --- | --- | --- |
| ItemsAdder | `itemsadder:<namespace>:<id>` | `itemsadder:my_pack:ruby_ore` |
| Oraxen | `oraxen:<id>` | `oraxen:ruby_sword` |
| Nexo | `nexo:<id>` | `nexo:ruby` |
| CraftEngine | `craftengine:<namespace>:<id>` | `craftengine:default:ruby_ore` |

```yaml
jobs:
  miner:
    icon: itemsadder:my_pack:ruby_pickaxe
    actions:
      break:
        itemsadder:my_pack:ruby_ore:
          exp: 5
```

| Action | Custom content that is rewarded |
| --- | --- |
| `break`, `place` | Custom blocks and furniture |
| `craft`, `fish`, `harvest`, `trade` | Custom items |
| `kill`, `tame`, `breed` | Custom mobs (ItemsAdder only) |

`smelt` is limited to vanilla items: Bukkit does not tell which custom item came out of a furnace.

## Download

[![Available at Modrinth](https://img.shields.io/badge/Available%20at-Modrinth-1BD96A?style=for-the-badge&logo=modrinth&logoColor=white)](https://modrinth.com/plugin/ujobs)

https://modrinth.com/plugin/ujobs

## Wiki

Documentation and setup guides are available on the GitHub Wiki:

- https://github.com/UsainSrht/UJobs/wiki

## Build

### Requirements

- Java `21`
- Maven `3.9+`

### Build from source

```bash
mvn clean package
```

Output jar:

- `target/UJobs-<version>.jar`

## Contribute

Contributions are welcome.

1. Fork the repository.
2. Create a feature branch.
3. Make your changes with clear commit messages.
4. Open a Pull Request with a short description and test notes.

If you are unsure where to start, open an issue with your proposal first.
