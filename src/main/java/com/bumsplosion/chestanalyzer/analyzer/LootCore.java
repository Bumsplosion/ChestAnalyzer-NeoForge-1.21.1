package com.bumsplosion.chestanalyzer.analyzer;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import java.util.*;

public class LootCore {

    // =========================
    // DATA MODELS
    // =========================
    public record ItemStats(
            double chance,
            double avgCount,
            double confidenceLow,
            double confidenceHigh
    ) {}

    public record TableExport(
            Map<String, ItemStats> items,
            int iterations
    ) {}

    public record Result(
            Map<String, Integer> appearance,
            Map<String, Integer> total,
            int iterations
    ) {}

    // =========================
    // SIMULATION CORE
    // =========================
    public static Result simulate(ServerLevel level, ResourceLocation tableId, int iterations) {

        LootTable table = LootRegistry.getLootTable(level, tableId);

        Map<String, Integer> appearance = new HashMap<>();
        Map<String, Integer> total = new HashMap<>();

        LootParams params = new LootParams.Builder(level)
                .withParameter(
                        net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,
                        level.getSharedSpawnPos().getCenter()
                )
                .create(LootContextParamSets.CHEST);

        for (int i = 0; i < iterations; i++) {

            List<ItemStack> items = table.getRandomItems(params);
            Set<String> seen = new HashSet<>();

            for (ItemStack stack : items) {
                String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                total.merge(id, stack.getCount(), Integer::sum);
                seen.add(id);
            }

            for (String id : seen) {
                appearance.merge(id, 1, Integer::sum);
            }
        }

        return new Result(appearance, total, iterations);
    }

    // =========================
    // EXPORT TABLE
    // =========================
    public static TableExport buildTable(Result r) {

        Map<String, ItemStats> items = new LinkedHashMap<>();

        for (String item : r.appearance.keySet()) {

            int a = r.appearance.getOrDefault(item, 0);
            int t = r.total.getOrDefault(item, 0);
            int n = r.iterations;

            double chance = (a * 100.0) / n;
            double avg = t / (double) n;

            double p = a / (double) n;
            double err = Math.sqrt((p * (1 - p)) / n);
            double margin = 1.96 * err * 100.0;

            items.put(item, new ItemStats(
                    chance,
                    avg,
                    Math.max(0, chance - margin),
                    Math.min(100, chance + margin)
            ));
        }

        return new TableExport(items, r.iterations);
    }
}