# ZenoSkills - Zeno SMP custom skills plugin

Paper **1.21.11** - Java **21**

Every new player gets a cinematic **WELCOME -> TO ZENO SMP -> CHOOSE YOUR SKILL** intro,
then picks **one** skill (permanent) from two categories. A blast of particles, sounds and a
title plays when the skill awakens.

## Build
```
mvn clean package
```
The jar is created at `target/ZenoSkills.jar` - drop it into your server's `plugins/` folder and restart.

## Using it
- `/skill` - open the menu (or show your skill if you already have one)
- `/skill info` - details about your skill
- **Shift + F** (or `/skill use`) - cast your active ability
- Admin (`zenoskills.admin`, default op): `/skill reset <player>`, `/skill set <player> <skill_id>`, `/skill reload`

## The 10 skills
| Category | Skill | Passive | Active |
|---|---|---|---|
| Life | Lifebloom | Regeneration I | Bloom: heal 8 hearts + Regen II |
| Life | Titan Heart | +5 hearts | Titan's Resolve: 8 absorption hearts + Resistance |
| Life | Phoenix Soul | Fire Resistance, auto-revive on fatal damage (10 min cd) | Ascension: fire launch + slow fall |
| Life | Gaia's Blessing | Jump Boost I, 50% slower hunger | Nature's Embrace: heals everyone within 8 blocks |
| Life | Vampiric Heart | 12% lifesteal | Blood Frenzy: 50% lifesteal + Strength + Speed |
| Destruction | Inferno | Fire Resistance, hits ignite | Inferno Burst: fire ring, 8 dmg + knockback |
| Destruction | Tempest | Speed I, no fall damage | Cyclone: hurl foes skyward |
| Destruction | Thunder God | Haste I, 12% smite on hit | Thunder Judgment: lightning on up to 5 foes |
| Destruction | Frost Wrath | Freeze immunity, hits slow | Absolute Zero: freeze + Slowness IV |
| Destruction | Void Walker | Night Vision, 15% Wither on hit | Void Rift: teleport 10 blocks + damage |

Skill ids for `/skill set`: lifebloom, titan_heart, phoenix_soul, gaia_blessing, vampiric_heart,
inferno, tempest, thunder_god, frost_wrath, void_walker.

## Notes
- The chosen skill is stored in the player's data (PersistentDataContainer), so it survives restarts.
- Cooldowns are kept in memory (they reset on server restart).
- Ability damage goes through normal damage events, so WorldGuard PvP/mob flags still apply.
- No blocks are ever destroyed or set on fire by any skill.
- Tweak `config.yml` for cooldown multiplier, force-choice, intro on/off, etc.
- To change damage numbers / durations, edit `Abilities.java`; to rename skills or text, edit `Skill.java`.


## Expanded skills and Admin Skill

The skill picker now contains 20 standard skills: 10 Life and 10 Destruction.
The five additional Life skills are Celestial Guard, Ocean Soul, Shadow Mender,
Wind Blessing and Lucky Star. The five additional Destruction skills are
Earthshaker, Storm Caller, Solar Flare, Gravity Bender and Soul Reaper.

### Admin Skill

Operators (or users with `zenoskills.admin`) can grant the special Admin Skill:

`/zenoskills give adminskill <player>`

The grant is stored on the player and persists through relogs. Granting it
immediately triggers the global ascension cinematic. Every online player sees
the particle/sound sequence and receives 3 seconds of Levitation; only the
recipient receives the ongoing admin buffs and water-walking. The recipient
can replay the global cinematic with Shift + F or `/skill adminuse`.

The Admin Skill applies Resistance 200 and a collection of beneficial effects.
Use `/skill reset <player>` to reset a normal skill; the Admin Skill is separate.
