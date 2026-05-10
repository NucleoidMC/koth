package io.github.restioson.koth.game;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

public class KothPlayer {
    private final GameSpace gameSpace;
    public long deadTime = -1;
    public int score = 0;
    public int wins = 0;
    private final PlayerRef player;
    @Nullable
    public AttackRecord lastTimeWasAttacked;
    private final String playerName;

    public KothPlayer(ServerPlayer player, GameSpace space) {
        this.player = PlayerRef.of(player);
        this.gameSpace = space;
        this.playerName = player.getScoreboardName();
    }

    public ServerPlayer player() {
        return this.player.getEntity(gameSpace);
    }

    public ServerPlayer attacker(long time, ServerLevel world) {
        if (this.lastTimeWasAttacked != null) {
            return this.lastTimeWasAttacked.isValid(time) ? this.lastTimeWasAttacked.player.getEntity(world) : null;
        } else {
            return null;
        }
    }

    public boolean hasPlayer() {
        return this.player.isOnline(gameSpace);
    }

    public String playerName() {
        return this.playerName;
    }
}
