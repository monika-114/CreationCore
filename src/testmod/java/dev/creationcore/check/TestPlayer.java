package dev.creationcore.check;

public class TestPlayer extends net.minecraft.server.level.ServerPlayer {
  public TestPlayer(
      net.minecraft.server.level.ServerLevel level, com.mojang.authlib.GameProfile profile) {
    super(
        level.getServer(),
        level,
        profile,
        net.minecraft.server.level.ClientInformation.createDefault());
    connection =
        new net.minecraft.server.network.ServerGamePacketListenerImpl(
            level.getServer(),
            new net.minecraft.network.Connection(
                net.minecraft.network.protocol.PacketFlow.SERVERBOUND),
            this,
            net.minecraft.server.network.CommonListenerCookie.createInitial(profile, false)) {
          @Override
          public void send(net.minecraft.network.protocol.Packet<?> packet) {}
        };
  }
}
