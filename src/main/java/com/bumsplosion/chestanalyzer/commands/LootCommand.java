package com.bumsplosion.chestanalyzer.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import com.bumsplosion.chestanalyzer.analyzer.LootCore;
import com.bumsplosion.chestanalyzer.analyzer.LootExports;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeMap;

public class LootCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
                Commands.literal("analyzeloot")
                        .requires(s -> s.hasPermission(2))

                        // RUN COMMAND + AUTOSUGGEST
                        .then(Commands.literal("run")

                                .executes(ctx -> {
                                    ctx.getSource().sendFailure(
                                            Component.literal("Usage: /analyzeloot run <table> <iterations>")
                                    );
                                    return 0;
                                })

                                .then(Commands.argument("table", ResourceLocationArgument.id())
                                        .suggests((ctx, builder) -> {

                                            var server = ctx.getSource().getServer();

                                            server.reloadableRegistries()
                                                    .getKeys(Registries.LOOT_TABLE)
                                                    .forEach(id -> {
                                                        if (id.toString().startsWith("minecraft:chests/")) {
                                                            builder.suggest(id.toString());
                                                        }
                                                    });

                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("iterations", IntegerArgumentType.integer(1))
                                                .suggests((ctx, builder) -> {
                                                    builder.suggest("250");
                                                    builder.suggest("1000");
                                                    builder.suggest("2500");
                                                    builder.suggest("5000");
                                                    builder.suggest("10000");
                                                    return builder.buildFuture();
                                                })
                                                .executes(ctx -> {

                                                    var source = ctx.getSource();
                                                    var level = source.getLevel();

                                                    var tableId = ResourceLocationArgument.getId(ctx, "table");
                                                    int iterations = IntegerArgumentType.getInteger(ctx, "iterations");

                                                    var result = LootCore.simulate(level, tableId, iterations);
                                                    var table = LootCore.buildTable(result);

                                                    source.sendSuccess(() ->
                                                            Component.literal("Loot Scan Complete → " + tableId), false);

                                                    table.items().forEach((id, stats) ->
                                                            source.sendSuccess(() ->
                                                                    Component.literal(
                                                                            id + " → " + String.format("%.2f", stats.chance()) + "%"
                                                                    ), false)
                                                    );

                                                    return 1;
                                                })
                                        )
                                )
                        )
            
                        // DEEP SCAN + AUTO EXPORT
                        .then(Commands.literal("export")
                                .then(Commands.argument("table", ResourceLocationArgument.id())
                                        .suggests((ctx, builder) -> {

                                            var server = ctx.getSource().getServer();

                                            server.reloadableRegistries()
                                                    .getKeys(Registries.LOOT_TABLE)
                                                    .forEach(id -> {
                                                        if (id.toString().startsWith("minecraft:chests/")) {
                                                            builder.suggest(id.toString());
                                                        }
                                                    });

                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("maxIterations", IntegerArgumentType.integer(1))
                                                .then(Commands.argument("filename", StringArgumentType.word())
                                                        .executes(ctx -> {

                                                            var source = ctx.getSource();
                                                            var level = source.getLevel();

                                                            var tableId = ResourceLocationArgument.getId(ctx, "table");
                                                            int max = IntegerArgumentType.getInteger(ctx, "maxIterations");
                                                            String filename = StringArgumentType.getString(ctx, "filename");

                                                            var result = LootExports.deepScan(level, tableId, max);

                                                            LootExports.exportDeep(result, tableId.toString(), filename);
                                                            LootExports.LAST_RESULT = result;
                                                            LootExports.LAST_TYPE = "deep";

                                                            source.sendSuccess(() ->
                                                                    Component.literal("Deep scan exported"), false);

                                                            return 1;
                                                        })
                                                )
                                        )
                                )
                        )
            
                        // STRUCTURES EXPORT
                        .then(Commands.literal("structures")
                                .executes(ctx -> {

                                    var source = ctx.getSource();
                                    var server = source.getServer();

                                    try {
                                        TreeMap<String, String> structures = new TreeMap<>();

                                        server.reloadableRegistries()
                                                .getKeys(Registries.LOOT_TABLE)
                                                .forEach(id -> {

                                                    String full = id.toString();

                                                    if (full.contains("chests/")) {
                                                        String shortName = full
                                                                .replace("minecraft:chests/", "")
                                                                .replace("chests/", "");

                                                        structures.put(shortName, full);
                                                    }
                                                });

                                        Path folder = Path.of("loot_exports");
                                        Files.createDirectories(folder);

                                        Path out = folder.resolve("loot_structures.json");

                                        StringBuilder json = new StringBuilder("{\n");

                                        boolean first = true;
                                        for (var entry : structures.entrySet()) {
                                            if (!first) json.append(",\n");

                                            json.append("  \"")
                                                    .append(entry.getKey())
                                                    .append("\": \"")
                                                    .append(entry.getValue())
                                                    .append("\"");

                                            first = false;
                                        }

                                        json.append("\n}");

                                        Files.writeString(out, json.toString());

                                        source.sendSuccess(() ->
                                                Component.literal(
                                                        "Exported " + structures.size()
                                                                + " structures to loot_exports/loot_structures.json"
                                                ), false);

                                        return 1;

                                    } catch (Exception e) {
                                        source.sendFailure(
                                                Component.literal("Structure export failed: " + e.getMessage())
                                        );
                                        e.printStackTrace();
                                        return 0;
                                    }
                                })
                        )
        );
    }
}
