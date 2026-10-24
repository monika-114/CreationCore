import dev.creationcore.data.DragonRitualSavedData;
import dev.creationcore.particle.InwardParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import io.netty.buffer.Unpooled;

public final class RitualStateCheck {
    private static void check(boolean condition,String message) {
        if(!condition)throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        DragonRitualSavedData state=new DragonRitualSavedData();
        state.begin(new BlockPos(20,0,-30),70.5,123);
        check(state.isPending()&&!state.isMoving(),"Waiting for portal");
        check(state.particleCenter(500).y==70.5,"Stationary gathering before portal");
        state.startMovement(100,80.5);
        for(int t=100;t<=180;t++) {
            check(Math.abs(state.particleCenter(t).y-(70.5+(t-100)*0.125))<1e-9,"Constant linear motion");
        }
        check(!state.movementComplete(179)&&state.movementComplete(180),"Exact arrival time");
        CompoundTag saved=state.save(new CompoundTag(),null);
        var load=DragonRitualSavedData.class.getDeclaredMethod("load",CompoundTag.class,HolderLookup.Provider.class);
        load.setAccessible(true);
        DragonRitualSavedData restored=(DragonRitualSavedData)load.invoke(null,saved,null);
        check(restored.particleGroup()==123&&restored.isPending(),"Persist generation and pending state");
        check(restored.particleCenter(140).equals(state.particleCenter(140)),"Persist exact trajectory");
        restored.begin(new BlockPos(-10,0,50),65.5,456);
        check(!restored.isMoving()&&restored.particleGroup()==456,"New ritual replaces old motion/group");
        check(restored.particleCenter(180).x==-9.5,"Replace source origin");
        check(restored.shouldSearchPortal(100), "First portal search is immediate");
        for (int t=101;t<110;t++) check(!restored.shouldSearchPortal(t), "Throttle failed portal searches");
        check(restored.shouldSearchPortal(110), "Retry portal search after ten ticks");
        check(restored.shouldAttemptOutput(200), "First output attempt is immediate");
        check(!restored.shouldAttemptOutput(219)&&restored.shouldAttemptOutput(220), "Rejected output retry is throttled");
        restored.begin(new BlockPos(0,0,0),75.5,789);
        check(restored.shouldSearchPortal(100)&&restored.shouldAttemptOutput(100), "Replacement resets retry timers");
        restored.complete();check(!restored.isPending(),"Complete once");

        var data=new InwardParticleOptions(0.25F,-1.5F,0.75F,0xFF5F377F,17,456);
        var buffer=Unpooled.buffer();
        try {
            InwardParticleOptions.STREAM_CODEC.encode(buffer,data);
            check(InwardParticleOptions.STREAM_CODEC.decode(buffer).equals(data),"Network data round trip");
            check(buffer.readableBytes()==0,"No protocol residue");
        } finally { buffer.release(); }
        System.out.println("PASS: linear ritual motion, portal wait, save/load, replacement, completion, particle network codec.");
    }
}
