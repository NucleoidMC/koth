package io.github.restioson.koth.game;

import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.common.widget.SidebarWidget;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;

public class KothScoreboard {
    private final SidebarWidget sidebar;
    private final boolean winnerTakesAll;
    private final boolean deathMatch;
    private final boolean knockoff;

    public KothScoreboard(GlobalWidgets widgets, String name, boolean wta, boolean dm, boolean ko) {
        this.sidebar = widgets.addSidebar(
                Component.literal(name).withStyle(ChatFormatting.BLUE, ChatFormatting.BOLD)
        );
        this.winnerTakesAll = wta;
        this.deathMatch = dm;
        this.knockoff = ko;
    }

    public void render(List<KothPlayer> leaderboard, AABB throne) {
        this.sidebar.set(content -> {
            for (KothPlayer entry : leaderboard) {
                String line;

                if (this.winnerTakesAll) {
                    line = String.format("Ruler: %s%s%s", ChatFormatting.AQUA, entry.playerName(), ChatFormatting.RESET);
                } else if (this.deathMatch) {
                    line = String.format(
                            "%s%s%s: %d rounds",
                            ChatFormatting.AQUA,
                            entry.playerName(),
                            ChatFormatting.RESET,
                            entry.wins
                    );
                } else if (this.knockoff) {
                    line = String.format(
                            "%s%s%s: %d points",
                            ChatFormatting.AQUA,
                            entry.playerName(),
                            ChatFormatting.RESET,
                            entry.score
                    );
                } else if (entry.hasPlayer() && throne.intersects(entry.player().getBoundingBox())) {
                    ChatFormatting indicatorColor = entry.player().level().getGameTime() % 20 == 0 ? ChatFormatting.GOLD : ChatFormatting.YELLOW;

                    line = String.format(
                            "%s♦ %s%s%s: %ds",
                            indicatorColor,
                            ChatFormatting.AQUA,
                            entry.playerName(),
                            ChatFormatting.RESET,
                            entry.score
                    );
                } else {
                    line = String.format(
                            "%s%s%s: %ds",
                            ChatFormatting.AQUA,
                            entry.playerName(),
                            ChatFormatting.RESET,
                            entry.score
                    );
                }

                content.add(Component.literal(line));
            }
        });
    }
}
