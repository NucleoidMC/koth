package io.github.restioson.koth;

import io.github.restioson.koth.game.KothConfig;
import io.github.restioson.koth.game.KothWaiting;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.nucleoid.plasmid.api.game.GameType;
import xyz.nucleoid.plasmid.api.game.GameTypes;

public class Koth implements ModInitializer {
    public static final String ID = "koth";
    public static final Logger LOGGER = LogManager.getLogger(ID);

    public static final GameType<KothConfig> TYPE = GameTypes.register(
            Identifier.fromNamespaceAndPath(ID, "koth"),
            KothConfig.CODEC,
            KothWaiting::open
    );

    @Override
    public void onInitialize() {}
}
