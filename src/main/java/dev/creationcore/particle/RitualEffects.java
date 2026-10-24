package dev.creationcore.particle;

import dev.creationcore.CreativeCoreMod;
import dev.creationcore.data.DragonRitualSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import java.util.*;

@EventBusSubscriber(modid = CreativeCoreMod.MODID)
public final class RitualEffects {
    public static final int DARK = 0xFF5F377F;
    private static final int[] COLORS = {DARK,0xFF8757AB,0xFFB989DC,0xFFB989DC};
    private static final Map<ServerLevel, State> STATES = new WeakHashMap<>();
    private static final class State {
        final List<Portal> portals = new ArrayList<>();
        final List<Pulse> births = new ArrayList<>();
    }
    private static final class Portal {
        final Set<BlockPos> blocks;
        long expires;
        Portal(Set<BlockPos> blocks,long expires) { this.blocks=blocks;this.expires=expires; }
    }
    private record Pulse(Vec3 center,long started) {}
    private RitualEffects() {}

    public static void voidRipple(ServerLevel level, Vec3 pos) {
        // No coalescing: every transformed ItemEntity emits its own full-size ripple.
        for(var player:level.players()) {
            if(player.distanceToSqr(pos)<256*256)level.sendParticles(player,ModParticles.VOID_RIPPLE.get(),true,pos.x,pos.y,pos.z,1,0,0,0,0);
        }
    }

    public static void inward(ServerLevel level, DragonRitualSavedData ritual, long now) {
        Vec3 center = ritual.particleCenter(now);
        // One five-tick emitter packet replaces twenty individual spark packets.
        Vec3 velocity = ritual.particleCenter(now + 5).subtract(center).scale(0.2);
        var emitter = new InwardParticleOptions((float) velocity.x, (float) velocity.y,
                (float) velocity.z, DARK, 5, ritual.particleGroup());
        for (var player : level.players()) {
            if (player.distanceToSqr(center) < 128 * 128)
                level.sendParticles(player, emitter, true, center.x, center.y, center.z, 1, 0, 0, 0, 0);
        }
    }

    public static void witherBirth(ServerLevel level,Vec3 center) {
        STATES.computeIfAbsent(level,k->new State()).births.add(new Pulse(center,level.getGameTime()));
        level.sendParticles(ParticleTypes.FLASH,center.x,center.y,center.z,1,0,0,0,0);
    }

    public static void portal(ServerLevel level,BlockPos hit) {
        if (!level.getBlockState(hit).is(Blocks.END_PORTAL)) hit=hit.below();
        if (!level.getBlockState(hit).is(Blocks.END_PORTAL)) return;
        Set<BlockPos> connected=new HashSet<>();
        ArrayDeque<BlockPos> queue=new ArrayDeque<>();
        connected.add(hit);queue.add(hit);
        while (!queue.isEmpty()) {
            BlockPos p=queue.removeFirst();
            for(Direction direction:Direction.values()) {
                BlockPos n=p.relative(direction);
                if (!connected.contains(n) && level.hasChunkAt(n) && level.getBlockState(n).is(Blocks.END_PORTAL)) {
                    connected.add(n);queue.addLast(n);
                }
            }
        }
        State state=STATES.computeIfAbsent(level,k->new State());
        for(Portal portal:state.portals) {
            if (!Collections.disjoint(portal.blocks,connected)) {
                portal.blocks.addAll(connected);portal.expires=level.getGameTime()+100;return;
            }
        }
        state.portals.add(new Portal(connected,level.getGameTime()+100));
    }

    public static void core(ServerLevel level,Vec3 center,int tick) {
        for(int strand=0;strand<2;strand++) {
            double angle=tick*0.08+strand*Math.PI;
            double radius=0.65+0.1*Math.sin(tick*0.04);
            Vec3 p=center.add(Math.cos(angle)*radius,0.15+Math.sin(angle*2)*0.2,Math.sin(angle)*radius);
            level.sendParticles(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT,COLORS[(tick/8+strand)%4]),
                    p.x,p.y,p.z,1,0.015,0.02,0.015,0.02);
        }
    }

    @SubscribeEvent public static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        State state=STATES.get(level);
        if(state==null)return;
        long now=level.getGameTime();
        state.portals.removeIf(p->now>=p.expires);
        state.births.removeIf(p->now-p.started()>=40);
        if(now%2==0) {
            for(Portal portal:state.portals) {
                for(BlockPos p:portal.blocks) {
                    if(!level.getBlockState(p).is(Blocks.END_PORTAL))continue;
                    int color=COLORS[Math.floorMod(p.getX()+p.getZ()+(int)(now/4),4)];
                    level.sendParticles(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT,color),
                            p.getX()+0.5,p.getY()+0.85,p.getZ()+0.5,3,0.42,0.08,0.42,0.04);
                }
            }
            for(Pulse pulse:state.births) {
                double t=(now-pulse.started())/40.0;
                for(int i=0;i<10;i++) {
                    double a=i*Math.PI/5+t*Math.PI*3;
                    Vec3 p=pulse.center().add(Math.cos(a)*(0.5+2*t),t*2,Math.sin(a)*(0.5+2*t));
                    level.sendParticles(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT,COLORS[i%4]),
                            p.x,p.y,p.z,1,0.02,0.03,0.02,0.04);
                }
            }
        }
        if(state.portals.isEmpty()&&state.births.isEmpty())STATES.remove(level);
    }
}
