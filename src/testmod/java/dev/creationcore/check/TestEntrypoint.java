package dev.creationcore.check;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.loader.api.FabricLoader;

public class TestEntrypoint implements ModInitializer {
  public void onInitialize() {
    if (FabricLoader.getInstance().getEnvironmentType() != net.fabricmc.api.EnvType.SERVER) return;
    ServerLifecycleEvents.SERVER_STARTED.register(
        server -> {
          if (Boolean.getBoolean("creationcore.gameplay")) {
            GameplayChecks.started(server);
            return;
          }
          if (Boolean.getBoolean("creationcore.redstone")) {
            try {
              RedstoneChecks.start(server);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
            return;
          }
          boolean full =
              FabricLoader.getInstance().isModLoaded("toms_storage")
                  && FabricLoader.getInstance().isModLoaded("sophisticatedbackpacks")
                  && FabricLoader.getInstance().isModLoaded("sophisticatedstorage");
          if (full) CompatChecks.started(server);
          else {
            System.out.println("FABRIC_SMOKE_PASS optional-mod isolation");
            server.halt(false);
          }
        });
    ServerTickEvents.END_WORLD_TICK.register(
        world -> {
          if (!Boolean.getBoolean("creationcore.redstone")
              && !Boolean.getBoolean("creationcore.gameplay")
              && FabricLoader.getInstance().isModLoaded("sophisticatedbackpacks")
              && FabricLoader.getInstance().isModLoaded("sophisticatedstorage")
              && FabricLoader.getInstance().isModLoaded("toms_storage")) CompatChecks.tick(world);
        });
    ServerTickEvents.END_SERVER_TICK.register(
        server -> {
          if (Boolean.getBoolean("creationcore.redstone"))
            try {
              RedstoneChecks.tick(server);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
        });
  }
}
