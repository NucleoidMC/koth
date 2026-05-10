package io.github.restioson.koth.game;

import io.github.restioson.koth.game.map.KothMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

public class KothSpawnLogic {
    private final ServerLevel world;
    private final KothMap map;
    private final Map<BlockBounds, LongList> spawnPositionsMap;

    public KothSpawnLogic(ServerLevel world, KothMap map) {
        this.world = world;
        this.map = map;
        this.spawnPositionsMap = collectSpawnPositions(world, map);
    }

    public JoinAcceptorResult.Teleport acceptPlayer(JoinAcceptor offer, GameType gameMode, @Nullable KothStageManager stageManager) {
        return offer.teleport(this.world, this.findSpawnFor(this.map.getSpawn(world.getRandom()))).thenRunForEach(player -> {
                    player.setYRot(this.map.spawnAngle);
                    this.resetPlayer(player, gameMode, stageManager);
                });
    }

    public void resetAndRespawn(ServerPlayer player, GameType gameMode, @Nullable KothStageManager stageManager, int index) {
        this.resetAndRespawn(player, gameMode, stageManager, this.map.getSpawn(index));
    }

    public void resetAndRespawnRandomly(ServerPlayer player, GameType gameMode, @Nullable KothStageManager stageManager) {
        this.resetAndRespawn(player, gameMode, stageManager, this.map.getSpawn(player.getRandom()));
    }

    private void resetAndRespawn(ServerPlayer player, GameType gameMode, @Nullable KothStageManager stageManager, BlockBounds bounds) {
        Vec3 spawn = this.findSpawnFor(bounds);
        player.teleportTo(this.world, spawn.x, spawn.y, spawn.z, Set.of(), this.map.spawnAngle, 0.0F, false);

        this.resetPlayer(player, gameMode, stageManager);
    }

    public void resetPlayer(ServerPlayer player, GameType gameMode, @Nullable KothStageManager stageManager) {
        player.setGameMode(gameMode);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0f;
        player.setRemainingFireTicks(0);

        player.addEffect(new MobEffectInstance(
                MobEffects.NIGHT_VISION,
                -1,
                1,
                true,
                false
        ));

        player.connection.resetPosition();

        if (stageManager != null) {
            KothStageManager.FrozenPlayer state = stageManager.frozen.computeIfAbsent(player, p -> new KothStageManager.FrozenPlayer());
            state.lastPos = player.position();
        }
    }

    public Vec3 findSpawnFor(BlockBounds bounds) {
        RandomSource random = this.world.getRandom();

        LongList spawnPositions = this.spawnPositionsMap.get(bounds);
        long packedPos = spawnPositions.getLong(random.nextInt(spawnPositions.size()));
        BlockPos min = bounds.min();

        int x = BlockPos.getX(packedPos);
        int z = BlockPos.getZ(packedPos);

        return new Vec3(x + random.nextDouble(), min.getY(), z + random.nextDouble());
    }

    private static Map<BlockBounds, LongList> collectSpawnPositions(ServerLevel world, KothMap map) {
        Map<BlockBounds, LongList> spawnPositionsMap = new HashMap<>();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (BlockBounds spawn : map.spawns) {
            LongList spawnPositions = new LongArrayList(64);
            spawnPositionsMap.put(spawn, spawnPositions);

            BlockPos min = spawn.min();
            BlockPos max = spawn.max();

            for (int x = min.getX(); x < max.getX(); x++) {
                for (int z = min.getZ(); z < max.getZ(); z++) {
                    for (int y = min.getY(); y > min.getY() - 3; y--) {
                        pos.set(x, y, z);
                        if (!world.getBlockState(pos).isAir()) {
                            spawnPositions.add(pos.asLong());
                            continue;
                        }
                    }
                }
            }

            if (spawnPositions.isEmpty()) {
                BlockPos centerBottom = BlockPos.containing(spawn.centerBottom());
                spawnPositions.add(centerBottom.asLong());
            }
        }

        return spawnPositionsMap;
    }
}
