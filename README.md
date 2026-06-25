A mod for modpack creators. Allows commands for viewing & exporting loot chest contents. Very useful if you want to modify any loot tables from chests.

Commands:

/analyzeloot run <loot_table> \<iterations>

-Runs a loot simulation for a specific structure's chests and displays drop chances in chat

/analyzeloot run_all \<iterations>

-Runs loot simulations for all loot chests. After running this command, use /analyzeloot export <file_name> to view these results

/analyzeloot export <file_name>

-Exports the information from the most recently run command to a .json file

/analyzeloot structures

-Generates a list of all potential structures with chests that have loot tables and exports it to a .json file
