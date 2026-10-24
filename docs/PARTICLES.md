# Ritual effects

All times below assume the normal 20 game ticks per second.

| Trigger | Effect | Repeated triggers |
| --- | --- | --- |
| Empty bucket disappears in the void | Five white horizontal rings emitted 0.4 seconds apart; each expands to twenty-block diameter over three seconds, with 0.45-block-wide soft crests and gradual fading. Whole effect lasts 4.6 seconds | Independent five-ring effect per transformed dropped-item entity |
| Wither becomes special at its birth explosion | Flash and two-second purple rising spiral | Independent pulse for each special Wither |
| Base Matter consumed by dragon ritual | Purple #5f377f sparks spawn 0.3-2 blocks around the center and move inward | New ritual replaces the previous pending effect in that dimension |
| Exit portal becomes available | Center moves linearly from consumed Base Matter to Creative Matter spawn over four seconds; item appears at arrival | Pending center/trajectory saved in world data |
| Valid shulker enters End exit portal | Connected portal blocks emit entity_effect particles in the four requested color slots | Same portal's five-second emission duration is refreshed |
| Creative Core remains in entity form | Two light orbiting purple particle strands | Each entity owns its ambient effect |

The Wither's original texture and model are retained. Purple vertex tint is applied only to the synchronized special state; normal Withers retain vanilla appearance. No custom facial features or generated UV atlas are used.

To review effects without immediately collecting the Creative Core, observe from outside its five-block conversion radius. Portal particles stop being emitted at the timer's end, then existing particles fade naturally. Particle display settings, camera distance and terrain occlusion affect visibility.

The new `tools/RitualStateCheck.java` can be compiled against the project's main compile classpath and output. It checks the production saved-state and particle stream codecs, without starting a Minecraft world.

## buildfix10 performance

Gathering is now driven by one five-tick client-emitter update per nearby player instead of twenty separate spark updates. The client emits four sparks per tick in the same 0.3–2 block shell and advances the center using the transmitted short linear trajectory. A replacement emitter invalidates its predecessor; generation changes also remove old sparks. There is no client-world reference retained in the weak-map values. Existing instant_effect sprites and colors are preserved.

Ripple unit-circle coordinates are precomputed, using 64 segments per ring. Five simultaneously visible rings submit at most 5,120 vertices per frame, half the previous geometry. Ring count, width, radius, emission intervals, fade and two-sided visibility remain unchanged. Each bucket transformation still owns an independent ripple.
