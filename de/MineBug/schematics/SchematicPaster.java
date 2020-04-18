package de.MineBug.schematics;

import java.io.File;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

//@author Created by TheHolyException at 22.03.2019 - 20:22:18

public class SchematicPaster extends JavaPlugin implements CommandExecutor {
	
	private static SchematicPaster instance;
	
	public void onEnable() {
		SchematicPaster.instance = this;
		getCommand("schempaste").setExecutor(this);
	}
	
	
	//@return the instance
	public static SchematicPaster getInstance() {
		return instance;
	}
	
	
	public boolean onCommand(CommandSender sender, Command cmd, String lable, String[] args) {
		
		Player player = (Player) sender;
		if (cmd.getName().equalsIgnoreCase("schempaste")) {
			if (args.length == 1) {
				
				File file = new File("plugins//SchematicPaster//"+args[0]+".schematic.gz");
				if (file.exists()) {
					SchematicManager.pasteSchematic(player.getLocation(), SchematicManager.loadSchematic(file));
					player.sendMessage("Pasting " + file.getName());
				} else {
					sender.sendMessage("Invalid Path");
				}
			}
		}
		
		return true;
	}
	

}
