package studio.hiwire.tp2world.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import lombok.Getter;

/**
 * Configuration for the Tp2World plugin.
 */
@Getter
public final class Tp2WorldConfig {
  public static final BuilderCodec<Tp2WorldConfig> CODEC =
      BuilderCodec.builder(Tp2WorldConfig.class, Tp2WorldConfig::new)
          .append(
              new KeyedCodec<>("NotifyTeleportedPlayer", Codec.BOOLEAN),
              (config, value) -> config.notifyTeleportedPlayer = value,
              config -> config.notifyTeleportedPlayer)
          .add()
          .build();

  /** Whether to send a notification message to the teleported player. Default is true. */
  private boolean notifyTeleportedPlayer = true;
}
