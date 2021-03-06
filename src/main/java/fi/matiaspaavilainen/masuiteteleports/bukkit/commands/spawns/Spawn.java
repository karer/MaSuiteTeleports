package fi.matiaspaavilainen.masuiteteleports.bukkit.commands.spawns;

import fi.matiaspaavilainen.masuitecore.core.objects.PluginChannel;
import fi.matiaspaavilainen.masuiteteleports.bukkit.MaSuiteTeleports;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Spawn implements CommandExecutor {

    private MaSuiteTeleports plugin;

    public Spawn(MaSuiteTeleports p) {
        plugin = p;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return false;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

            if (args.length != 0) {
                plugin.formator.sendMessage(sender, plugin.config.load("teleports", "syntax.yml").getString("spawn.teleport"));
                return;
            }

            if (plugin.in_command.contains(sender)) {
                plugin.formator.sendMessage(sender, plugin.config.load(null, "messages.yml").getString("on-active-command"));
                return;
            }
            plugin.in_command.add(sender);
            Player p = (Player) sender;
            new PluginChannel(plugin, p, new Object[]{"MaSuiteTeleports", "SpawnPlayer", p.getName()}).send();
            plugin.in_command.remove(sender);

        });

        return true;
    }
}
