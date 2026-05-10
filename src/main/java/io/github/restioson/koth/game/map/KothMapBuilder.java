package io.github.restioson.koth.game.map;

import io.github.restioson.koth.Koth;
import io.github.restioson.koth.game.KothConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.biome.Biomes;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.MapTemplateMetadata;
import xyz.nucleoid.map_templates.MapTemplateSerializer;
import xyz.nucleoid.map_templates.TemplateRegion;
import xyz.nucleoid.plasmid.api.game.GameOpenException;

import java.io.IOException;
import java.util.List;

public class KothMapBuilder {

    private static final int DEFAULT_PRIORITY = 1;

    private final KothConfig.MapConfig config;

    public KothMapBuilder(KothConfig.MapConfig config) {
        this.config = config;
    }

    public KothMap create(MinecraftServer server) throws GameOpenException {
        try {
            MapTemplate template = MapTemplateSerializer.loadFromResource(server, this.config.id());
            MapTemplateMetadata metadata = template.getMetadata();

            List<BlockBounds> spawns = getSpawns(metadata);

            BlockBounds throne = metadata.getFirstRegionBounds("throne");

            KothMap map = new KothMap(template, spawns, throne, this.config.spawnAngle());
            template.setBiome(Biomes.PLAINS);

            return map;
        } catch (IOException e) {
            throw new GameOpenException(Component.literal("Failed to load template"), e);
        }
    }

    private static List<BlockBounds> getSpawns(MapTemplateMetadata metadata) {
        List<BlockBounds> spawns = metadata.getRegions("spawn").sorted((a, b) -> {
            return getPriority(b) - getPriority(a);
        }).map(TemplateRegion::getBounds).toList();

        if (spawns.isEmpty()) {
            Koth.LOGGER.error("No spawn is defined on the map! The game will not work.");
            throw new GameOpenException(Component.literal("no spawn defined"));
        } else {
            return spawns;
        }
    }

    private static int getPriority(TemplateRegion region) {
        if (region == null) return DEFAULT_PRIORITY;

        CompoundTag data = region.getData();
        if (data == null) return DEFAULT_PRIORITY;

        return data.getIntOr("Priority", DEFAULT_PRIORITY);
    }
}
