package io.github.restioson.koth.game;

import io.github.restioson.koth.game.map.KothMap;
import io.github.restioson.koth.game.map.KothMapBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.GameType;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;
import xyz.nucleoid.plasmid.api.game.GameOpenContext;
import xyz.nucleoid.plasmid.api.game.GameOpenException;
import xyz.nucleoid.plasmid.api.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.api.game.GameResult;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptor;
import xyz.nucleoid.plasmid.api.game.player.JoinAcceptorResult;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class KothWaiting {
    private final ServerLevel world;
    private final GameSpace gameSpace;
    private final KothMap map;
    private final KothConfig config;
    private final KothSpawnLogic spawnLogic;

    private KothWaiting(ServerLevel world, GameSpace gameSpace, KothMap map, KothConfig config) {
        this.world = world;
        this.gameSpace = gameSpace;
        this.map = map;
        this.config = config;
        this.spawnLogic = new KothSpawnLogic(world, map);
    }

    public static GameOpenProcedure open(GameOpenContext<KothConfig> context) {
        KothConfig config = context.config();
        KothMapBuilder generator = new KothMapBuilder(context.config().map());
        KothMap map = generator.create(context.server());

        if (!config.winnerTakesAll() && map.throne == null) {
            throw new GameOpenException(Component.literal("throne must exist if winner doesn't take all"));
        }

        RuntimeLevelConfig worldConfig = new RuntimeLevelConfig()
                .setDimensionType(ResourceKey.create(Registries.DIMENSION_TYPE, config.dimension()))
                .setGenerator(map.asGenerator(context.server()));

        return context.openWithLevel(worldConfig, (activity, world) -> {
            KothWaiting waiting = new KothWaiting(world, activity.getGameSpace(), map, context.config());

            GameWaitingLobby.addTo(activity, config.players());

            activity.listen(GameActivityEvents.REQUEST_START, waiting::requestStart);
            activity.listen(GameActivityEvents.TICK, waiting::tick);
            activity.listen(GamePlayerEvents.ACCEPT, waiting::acceptPlayer);
            activity.listen(PlayerDamageEvent.EVENT, waiting::onPlayerDamage);
            activity.listen(PlayerDeathEvent.EVENT, waiting::onPlayerDeath);
            // Todo
            //worldConfig.setTimeOfDay(config.map().time());
        });
    }

    private void tick() {
        for (ServerPlayer player : this.gameSpace.getPlayers()) {
            if (!this.map.bounds.contains(player.blockPosition())) {
                this.spawnPlayer(player);
            }
        }
    }

    private GameResult requestStart() {
        KothActive.open(this.world, this.gameSpace, this.map, this.config);
        return GameResult.ok();
    }

    private JoinAcceptorResult acceptPlayer(JoinAcceptor offer) {
        return this.spawnLogic.acceptPlayer(offer, GameType.ADVENTURE, null);
    }

    private EventResult onPlayerDamage(ServerPlayer player, DamageSource source, float value) {
        if (source.is(DamageTypeTags.IS_FIRE)) {
            this.spawnPlayer(player);
        }

        return EventResult.DENY;
    }

    private EventResult onPlayerDeath(ServerPlayer player, DamageSource source) {
        this.spawnPlayer(player);
        return EventResult.DENY;
    }

    private void spawnPlayer(ServerPlayer player) {
        this.spawnLogic.resetAndRespawnRandomly(player, GameType.ADVENTURE, null);
    }
}
