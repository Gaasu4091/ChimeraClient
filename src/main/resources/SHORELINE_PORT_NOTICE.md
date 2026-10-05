# Shoreline AutoCrystal transplantation into Chimera (1.21.11)

Source: user's attached shoreline-1.5.jar, authored by linus, Minecraft 1.21.1.
Chimera base: the companion PistonCrystal 2b Mode build, retaining the prior
HeaderColor and Font build. Existing `CrystalAura` module identity is retained;
its implementation and settings are replaced with the attached Shoreline
AutoCrystal implementation. Other modules are unchanged.

The combat calculations, damage/safety/lethal checks, extrapolation, ranges,
Instant spawn handling, Sequential modes, crystal entity-ID prediction,
swap modes, anti-weakness, inhibit, anti-stuck, rates and latency measurements
are transplanted. The calculation remains synchronous, as in the original tick
handler. All source-defined public settings are registered in Chimera's GUI;
Shoreline's hidden debug/instant-calculation options keep their original visibility.

Framework adaptations: Chimera events and Setting types, 1.21.11 packets/item tags,
armor equipment access, registry data and inherited Minecraft members, ClientWorld
spawn mixin for Instant, sequenced-packet invoker, disconnect cleanup, Chimera
friend lists, SpeedMine integration for mining state, totem and mining packet
tracking, and rendering using Chimera's existing accent color. No Shoreline loader,
account/session system, menu, or other module is included or needed.

Shoreline's priority-based rotation manager is replaced with Chimera rotations;
Chimera does not provide a priority queue. FastLatency is not present in Chimera,
so Sequential uses the normal network latency. AutoMine's mining connection
uses Chimera SpeedMine (AutoMine delegates to SpeedMine in this base).
Millisecond/nanosecond conversions in the copied CacheTimer are corrected.
No bypass or live-server performance guarantee is made.

BuildShoreline.java arguments: port working directory, PistonCrystal base JAR,
output JAR, 1.21.11 Yarn mappings.tiny, named Minecraft JAR, intermediary
Minecraft JAR, dependency classpath. Java 21+, ASM9, TinyRemapper0.14.1, Gson,
Minecraft/Fabric dependencies are required. Source is provided in the companion
archive; copied source retains attribution to Shoreline/linus. No new license
for the original Shoreline content is claimed.

Adapt.java documents the mechanical transformation from the attached JAR's named
decompilation; compilation uses the final adapted src tree directly.
