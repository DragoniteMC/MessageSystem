package com.ericlam.mc.msgsystem.commands;

import com.ericlam.mc.bungee.dnmc.builders.MessageBuilder;
import com.ericlam.mc.bungee.dnmc.permission.Perm;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.List;
import java.util.Optional;

public class ReplyCommand extends MSGSystemCommandNode {
    public ReplyCommand() {
        super(null, "reply", null, "回覆訊息", "<message>", "r");
    }

    @Override
    public void executionPlayer(ProxiedPlayer player, List<String> list) {
        final String message = player.hasPermission(Perm.DONOR) ? ChatColor.translateAlternateColorCodes('&', String.join(" ", list)) : String.join(" ", list);
        Optional<ProxiedPlayer> replier = pmManager.getLastMessager(player.getUniqueId());
        if (replier.isEmpty()) {
            MessageBuilder.sendMessage(player, msg.get("msg.no-replier"));
            return;
        }
        ProxiedPlayer target = replier.get();
        pmManager.sendPrivateMessage(player, target, message);
    }
}
