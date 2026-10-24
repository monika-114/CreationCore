package dev.creationcore.registry;

import java.util.*;
import java.util.function.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class Registrations<T> {
  private final Registry<T> registry;
  private final List<Entry<? extends T>> entries = new ArrayList<>();

  public Registrations(Registry<T> registry) {
    this.registry = registry;
  }

  public <V extends T> Entry<V> register(String name, Supplier<V> factory) {
    Entry<V> e = new Entry<>(ResourceLocation.fromNamespaceAndPath("creationcore", name), factory);
    entries.add(e);
    return e;
  }

  public void register() {
    for (var e : entries) e.bind(registry);
  }

  public static final class Entry<V> implements Supplier<V> {
    private final ResourceLocation id;
    private final Supplier<V> factory;
    private V value;

    Entry(ResourceLocation id, Supplier<V> factory) {
      this.id = id;
      this.factory = factory;
    }

    @SuppressWarnings("unchecked")
    void bind(Registry<?> registry) {
      value = Registry.register((Registry<V>) registry, id, factory.get());
    }

    public V get() {
      if (value == null) throw new IllegalStateException("Unregistered " + id);
      return value;
    }

    public boolean isBound() {
      return value != null;
    }

    public ResourceLocation getId() {
      return id;
    }
  }

  public static final class Items extends Registrations<Item> {
    public Items() {
      super(BuiltInRegistries.ITEM);
    }

    public Entry<Item> registerSimpleItem(String name, Item.Properties props) {
      return register(name, () -> new Item(props));
    }
  }

  public static final class Blocks extends Registrations<Block> {
    public Blocks() {
      super(BuiltInRegistries.BLOCK);
    }

    public <V extends Block> Entry<V> registerBlock(
        String name, Function<BlockBehaviour.Properties, V> f, BlockBehaviour.Properties p) {
      return register(name, () -> f.apply(p));
    }
  }
}
