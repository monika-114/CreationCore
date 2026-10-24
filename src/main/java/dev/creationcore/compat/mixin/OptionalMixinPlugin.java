package dev.creationcore.compat.mixin;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.*;

public final class OptionalMixinPlugin implements IMixinConfigPlugin {
  @Override
  public void onLoad(String name) {}

  @Override
  public String getRefMapperConfig() {
    return null;
  }

  @Override
  public boolean shouldApplyMixin(String target, String mixin) {
    String mod = mixin.contains("Sophisticated") ? "sophisticatedcore" : "toms_storage";
    return FabricLoader.getInstance().isModLoaded(mod);
  }

  @Override
  public void acceptTargets(Set<String> mine, Set<String> others) {}

  @Override
  public List<String> getMixins() {
    return null;
  }

  @Override
  public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}

  @Override
  public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
