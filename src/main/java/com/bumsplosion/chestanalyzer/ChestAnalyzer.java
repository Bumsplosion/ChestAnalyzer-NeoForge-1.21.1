package com.bumsplosion.chestanalyzer;

import net.neoforged.neoforge.event.RegisterCommandsEvent;
import com.bumsplosion.chestanalyzer.commands.LootCommand;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(ChestAnalyzer.MODID)
public class ChestAnalyzer {
    public static final String MODID = "chestanalyzer";

    public ChestAnalyzer() {
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void registerCommands(RegisterCommandsEvent event) {
        LootCommand.register(event.getDispatcher());
    }
}