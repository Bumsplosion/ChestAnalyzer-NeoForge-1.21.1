package com.bumsplosion.chestanalyzer.analyzer;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.core.registries.Registries;

public class LootRegistry {

    public static LootTable getLootTable(ServerLevel level, ResourceLocation id) {

        ResourceKey<LootTable> key =
                ResourceKey.create(Registries.LOOT_TABLE, id);

        return level.getServer()
                .reloadableRegistries()
                .getLootTable(key);
    }
}