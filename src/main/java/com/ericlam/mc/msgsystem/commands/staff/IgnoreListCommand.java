package com.ericlam.mc.msgsystem.commands.staff;

import com.ericlam.mc.bungee.dnmc.builders.MessageBuilder;
import com.ericlam.mc.bungee.dnmc.commands.caxerx.CommandNode;
import com.ericlam.mc.bungee.dnmc.container.OfflinePlayer;
import com.ericlam.mc.bungee.dnmc.main.DragoniteMC;
import com.ericlam.mc.bungee.dnmc.permission.Perm;
import com.ericlam.mc.msgsystem.commands.MSGSystemCommandNode;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.List;
import java.util.stream.Collectors;

public class IgnoreListCommand extends MSGSystemCommandNode {

    public IgnoreListCommand(CommandNode parent) {
        super(parent, "ignorelist", Perm.HELPER, "查看玩家的忽略列表", "<player>", "ignores");
    }

    @Override
    public void executionPlayer(ProxiedPlayer player, List<String> list) {
        String name = list.get(0);
        DragoniteMC.getAPI().getPlayerManager().getOfflinePlayer(name).whenComplete((off, ex) -> {
            if (off.isEmpty()) {
                MessageBuilder.sendMessage(player, DragoniteMC.getAPI().getMainConfig().getNoThisPlayer());
                return;
            }

            OfflinePlayer target = off.get();

            List<String> ignoredPlayers = playerIgnoreManager.getIgnoredPlayers(target.getUniqueId()).stream().map(OfflinePlayer::getName).collect(Collectors.toList());

            MessageBuilder.sendMessage(player, msg.get(list.size() > 0 ? "msg.ignore.list" : "msg.ignore.none").replace("<player>", target.getName()).replace("<list>", ignoredPlayers.toString()));

        });
    }
}
