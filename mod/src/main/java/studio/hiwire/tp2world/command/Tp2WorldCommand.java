package studio.hiwire.tp2world.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.RelativeDoublePosition;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import studio.hiwire.tp2world.Tp2WorldPlugin;

/**
 * Teleport to world command: /tp2world <world> [--player name] [--position x y z] [--rotation pitch yaw roll] [--bodyRotation pitch yaw roll]
 *
 * - If player is not specified, teleports the command sender
 * - If position is not specified, uses the world's spawn point
 * - If rotation is not specified, uses the spawn point's rotation (or 0 0 0 if custom position)
 * - If bodyRotation is not specified, uses (previousPitch, headYaw, previousRoll)
 */
public class Tp2WorldCommand extends CommandBase {

  private static final Message MESSAGE_PLAYER_NOT_IN_WORLD =
      Message.translation("server.commands.errors.playerNotInWorld");
  private static final Message MESSAGE_WORLD_SPAWN_NOT_SET =
      Message.translation("server.world.spawn.notSet");
  private static final Message MESSAGE_TELEPORTED_TO_WORLD =
      Message.translation("HiWire.Tp2World.ChatMessages.Command.Tp2World.TeleportedToWorld");
  private static final Message MESSAGE_TELEPORTED_PLAYER_TO_WORLD =
      Message.translation("HiWire.Tp2World.ChatMessages.Command.Tp2World.TeleportedPlayerToWorld");
  private static final Message MESSAGE_PLAYER_OR_ARG =
      Message.translation("server.commands.errors.playerOrArg");

  @Nonnull private final RequiredArg<World> worldArg;
  @Nonnull private final OptionalArg<PlayerRef> playerArg;
  @Nonnull private final OptionalArg<RelativeDoublePosition> positionArg;
  @Nonnull private final OptionalArg<Vector3f> rotationArg;
  @Nonnull private final OptionalArg<Vector3f> bodyRotationArg;

  public Tp2WorldCommand() {
    super("tp2world", "HiWire.Tp2World.Commands.Tp2World.Desc");

    this.worldArg =
        this.withRequiredArg(
            "world", "HiWire.Tp2World.Commands.Tp2World.Param.World.Desc", ArgTypes.WORLD);
    this.playerArg =
        this.withOptionalArg(
            "player", "HiWire.Tp2World.Commands.Tp2World.Param.Player.Desc", ArgTypes.PLAYER_REF);
    this.positionArg =
        this.withOptionalArg(
            "position",
            "HiWire.Tp2World.Commands.Tp2World.Param.Position.Desc",
            ArgTypes.RELATIVE_POSITION);
    this.rotationArg =
        this.withOptionalArg(
            "rotation", "HiWire.Tp2World.Commands.Tp2World.Param.Rotation.Desc", ArgTypes.ROTATION);
    this.bodyRotationArg =
        this.withOptionalArg(
            "bodyRotation",
            "HiWire.Tp2World.Commands.Tp2World.Param.BodyRotation.Desc",
            ArgTypes.ROTATION);

    this.requirePermission(HytalePermissions.fromCommand("tp2world"));
  }

  @Override
  protected void executeSync(@Nonnull CommandContext context) {
    final var targetWorld = this.worldArg.get(context);
    final var worldName = targetWorld.getName();

    // Determine target player - get ref/store first (safe on command thread)
    final boolean teleportingSelf = !this.playerArg.provided(context);

    if (teleportingSelf) {
      // Use command sender as target
      final var senderRef = context.senderAsPlayerRef();
      if (senderRef == null || !senderRef.isValid()) {
        context.sendMessage(MESSAGE_PLAYER_OR_ARG.param("option", "player"));
        return;
      }
      final var senderStore = senderRef.getStore();
      final var currentWorld = senderStore.getExternalData().getWorld();

      // Execute on world thread - getComponent must be called from world thread
      currentWorld.execute(
          () -> {
            final var targetPlayerRef =
                senderStore.getComponent(senderRef, PlayerRef.getComponentType());
            if (targetPlayerRef == null) {
              context.sendMessage(MESSAGE_PLAYER_NOT_IN_WORLD);
              return;
            }
            executeOnWorldThread(
                context, senderStore, senderRef, targetWorld, worldName, true, targetPlayerRef);
          });
    } else {
      // Teleport another player
      final var targetPlayerRef = this.playerArg.get(context);
      if (targetPlayerRef == null) {
        context.sendMessage(MESSAGE_PLAYER_NOT_IN_WORLD);
        return;
      }

      final var ref = targetPlayerRef.getReference();
      if (ref == null || !ref.isValid()) {
        context.sendMessage(MESSAGE_PLAYER_NOT_IN_WORLD);
        return;
      }

      final var store = ref.getStore();
      final var currentWorld = store.getExternalData().getWorld();

      // Execute teleport on the target player's world thread
      currentWorld.execute(
          () ->
              executeOnWorldThread(
                  context, store, ref, targetWorld, worldName, false, targetPlayerRef));
    }
  }

  private void executeOnWorldThread(
      @Nonnull CommandContext context,
      @Nonnull Store<EntityStore> store,
      @Nonnull Ref<EntityStore> ref,
      @Nonnull World targetWorld,
      @Nonnull String worldName,
      boolean teleportingSelf,
      @Nonnull PlayerRef targetPlayerRef) {

    // Get current position/rotation for relative calculations
    final var transformComponent = store.getComponent(ref, TransformComponent.getComponentType());
    final var headRotationComponent = store.getComponent(ref, HeadRotation.getComponentType());

    if (transformComponent == null || headRotationComponent == null) {
      context.sendMessage(MESSAGE_PLAYER_NOT_IN_WORLD);
      return;
    }

    final var previousPos = transformComponent.getPosition();
    final var previousBodyRotation = transformComponent.getRotation();

    // Check if we can use the simple spawn point teleport (no overrides)
    final boolean useSpawnPoint =
        !this.positionArg.provided(context)
            && !this.rotationArg.provided(context)
            && !this.bodyRotationArg.provided(context);

    Vector3d targetPosition;
    Vector3f targetHeadRotation;
    Vector3f targetBodyRotation;
    Teleport teleport;

    if (useSpawnPoint) {
      // Use world spawn point directly with createForPlayer
      final var spawnPoint =
          targetWorld.getWorldConfig().getSpawnProvider().getSpawnPoint(ref, store);

      if (spawnPoint == null) {
        context.sendMessage(MESSAGE_WORLD_SPAWN_NOT_SET.param("worldName", worldName));
        return;
      }

      targetPosition = spawnPoint.getPosition();
      targetHeadRotation = spawnPoint.getRotation();
      // Body rotation from spawn: pitch=0, yaw from spawn, roll=0
      targetBodyRotation = new Vector3f(0, targetHeadRotation.getYaw(), 0);
      teleport = Teleport.createForPlayer(targetWorld, spawnPoint);
    } else {
      // Custom position or rotation - need to build teleport manually
      if (this.positionArg.provided(context)) {
        // Use provided position (supports relative coordinates like ~ ~10 ~)
        final var relPos = this.positionArg.get(context);
        targetPosition = relPos.getRelativePosition(previousPos, targetWorld);

        // Default head rotation to 0 0 0 when custom position provided
        targetHeadRotation = new Vector3f(0, 0, 0);
      } else {
        // Use world spawn point position and rotation
        final var spawnPoint =
            targetWorld.getWorldConfig().getSpawnProvider().getSpawnPoint(ref, store);

        if (spawnPoint == null) {
          context.sendMessage(MESSAGE_WORLD_SPAWN_NOT_SET.param("worldName", worldName));
          return;
        }

        targetPosition = spawnPoint.getPosition();
        targetHeadRotation = spawnPoint.getRotation();
      }

      // Apply head rotation override if provided
      if (this.rotationArg.provided(context)) {
        targetHeadRotation = this.rotationArg.get(context);
      }

      // Determine body rotation
      if (this.bodyRotationArg.provided(context)) {
        targetBodyRotation = this.bodyRotationArg.get(context);
      } else {
        // Default body rotation: preserve previous pitch/roll, use head yaw
        targetBodyRotation =
            new Vector3f(
                previousBodyRotation.getPitch(),
                targetHeadRotation.getYaw(),
                previousBodyRotation.getRoll());
      }

      // Create teleport with target world (constructor needed for cross-world teleport)
      teleport = new Teleport(targetWorld, targetPosition, targetBodyRotation);
      teleport.setHeadRotation(targetHeadRotation);
    }

    store.addComponent(ref, Teleport.getComponentType(), teleport);

    // Convert rotations from radians to degrees for display (default to 0 if NaN)
    final float radToDeg = 57.295776f;
    final var headRotDeg =
        new Vector3f(
            Float.isNaN(targetHeadRotation.getPitch())
                ? 0
                : targetHeadRotation.getPitch() * radToDeg,
            Float.isNaN(targetHeadRotation.getYaw()) ? 0 : targetHeadRotation.getYaw() * radToDeg,
            Float.isNaN(targetHeadRotation.getRoll())
                ? 0
                : targetHeadRotation.getRoll() * radToDeg);
    final var bodyRotDeg =
        new Vector3f(
            Float.isNaN(targetBodyRotation.getPitch())
                ? 0
                : targetBodyRotation.getPitch() * radToDeg,
            Float.isNaN(targetBodyRotation.getYaw()) ? 0 : targetBodyRotation.getYaw() * radToDeg,
            Float.isNaN(targetBodyRotation.getRoll())
                ? 0
                : targetBodyRotation.getRoll() * radToDeg);

    // Send messages
    if (teleportingSelf) {
      // Teleporting yourself - config controls if you see a message
      if (Tp2WorldPlugin.get().getConfig().isNotifyTeleportedPlayer()) {
        context.sendMessage(
            MESSAGE_TELEPORTED_TO_WORLD
                .param("ModPrefix", Tp2WorldPlugin.PREFIX)
                .param("WorldName", worldName)
                .param("X", String.format("%.2f", targetPosition.getX()))
                .param("Y", String.format("%.2f", targetPosition.getY()))
                .param("Z", String.format("%.2f", targetPosition.getZ()))
                .param("HeadPitch", String.format("%.2f", headRotDeg.getPitch()))
                .param("HeadYaw", String.format("%.2f", headRotDeg.getYaw()))
                .param("HeadRoll", String.format("%.2f", headRotDeg.getRoll()))
                .param("BodyPitch", String.format("%.2f", bodyRotDeg.getPitch()))
                .param("BodyYaw", String.format("%.2f", bodyRotDeg.getYaw()))
                .param("BodyRoll", String.format("%.2f", bodyRotDeg.getRoll())));
      }
    } else {
      // Teleporting another player - command sender always gets confirmation
      context.sendMessage(
          MESSAGE_TELEPORTED_PLAYER_TO_WORLD
              .param("ModPrefix", Tp2WorldPlugin.PREFIX)
              .param("PlayerName", targetPlayerRef.getUsername())
              .param("WorldName", worldName)
              .param("X", String.format("%.2f", targetPosition.getX()))
              .param("Y", String.format("%.2f", targetPosition.getY()))
              .param("Z", String.format("%.2f", targetPosition.getZ()))
              .param("HeadPitch", String.format("%.2f", headRotDeg.getPitch()))
              .param("HeadYaw", String.format("%.2f", headRotDeg.getYaw()))
              .param("HeadRoll", String.format("%.2f", headRotDeg.getRoll()))
              .param("BodyPitch", String.format("%.2f", bodyRotDeg.getPitch()))
              .param("BodyYaw", String.format("%.2f", bodyRotDeg.getYaw()))
              .param("BodyRoll", String.format("%.2f", bodyRotDeg.getRoll())));

      // Config controls if the teleported player sees a message
      if (Tp2WorldPlugin.get().getConfig().isNotifyTeleportedPlayer()) {
        targetPlayerRef.sendMessage(
            MESSAGE_TELEPORTED_TO_WORLD
                .param("ModPrefix", Tp2WorldPlugin.PREFIX)
                .param("WorldName", worldName)
                .param("X", String.format("%.2f", targetPosition.getX()))
                .param("Y", String.format("%.2f", targetPosition.getY()))
                .param("Z", String.format("%.2f", targetPosition.getZ()))
                .param("HeadPitch", String.format("%.2f", headRotDeg.getPitch()))
                .param("HeadYaw", String.format("%.2f", headRotDeg.getYaw()))
                .param("HeadRoll", String.format("%.2f", headRotDeg.getRoll()))
                .param("BodyPitch", String.format("%.2f", bodyRotDeg.getPitch()))
                .param("BodyYaw", String.format("%.2f", bodyRotDeg.getYaw()))
                .param("BodyRoll", String.format("%.2f", bodyRotDeg.getRoll())));
      }
    }
  }
}
