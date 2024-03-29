package com.ericlam.mc.msgsystem.listener;

import com.ericlam.mc.bungee.dnmc.builders.MessageBuilder;
import com.ericlam.mc.bungee.dnmc.config.YamlManager;
import com.ericlam.mc.msgsystem.api.IllegalChatManager;
import com.ericlam.mc.msgsystem.config.ChatConfig;
import com.ericlam.mc.msgsystem.config.MSGConfig;
import com.ericlam.mc.msgsystem.main.MSGSystem;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MSGChatListener implements Listener, IllegalChatManager {

    private final Pattern IP_PATTERN = Pattern.compile("(?:[0-9]{1,3}( ?\\. ?|\\(?dot\\)?)){3}[0-9]{1,3}");
    private final Pattern DOMAIN_PATTERN = Pattern.compile("(http(s)?:\\/\\/.)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}( ?\\. ?| ?\\(?dot\\)? ?)[a-z]{2,6}\\b([-a-zA-Z0-9@:%_+.~#?&/=]*)");
    private Map<UUID, LocalDateTime> antiSpam = new ConcurrentHashMap<>();
    private Map<UUID, Integer> spamDuplicateMap = new ConcurrentHashMap<>();
    private Map<UUID, Duplication> duplicationMap = new ConcurrentHashMap<>();
    private ChatConfig chatConfig;
    private MSGConfig msg;

    public MSGChatListener() {
        YamlManager configManager = MSGSystem.getApi().getConfigManager();
        this.chatConfig = configManager.getConfigAs(ChatConfig.class);
        this.msg = configManager.getConfigAs(MSGConfig.class);
    }

    @EventHandler
    public void onPlayerChat(final ChatEvent e) {
        if (e.isCommand()) return;
        if (!(e.getSender() instanceof ProxiedPlayer)) return;
        if (e.getMessage().startsWith("#")) return; //for channel
        ProxiedPlayer player = (ProxiedPlayer) e.getSender();
        if (this.antiSpam(player, e)) return;
        if (this.antiDuplicate(player, e)) return;
        if (this.antiCharDuplicate(player, e)) return;
        this.antiAdvertise(player, e);
    }

    @Override
    public boolean antiSpam(ProxiedPlayer player, final ChatEvent e) {
        long chatCooldown = chatConfig.cooldownChat;
        if (!antiSpam.containsKey(player.getUniqueId())) {
            antiSpam.put(player.getUniqueId(), LocalDateTime.now());
            return false;
        }
        LocalDateTime previousTalkTime = antiSpam.get(player.getUniqueId());
        Duration duration = Duration.between(previousTalkTime, LocalDateTime.now());
        final long dupMax = chatConfig.duplicateMax;
        if (duration.toMillis() >= chatCooldown) {
            antiSpam.put(player.getUniqueId(), LocalDateTime.now());
            if (spamDuplicateMap.getOrDefault(player.getUniqueId(), 0) > dupMax)
                spamDuplicateMap.remove(player.getUniqueId());
            return false;
        }
        e.setCancelled(true);
        double sec = BigDecimal.valueOf((double) (chatCooldown - duration.toMillis()) / 1000).setScale(1, RoundingMode.HALF_EVEN).doubleValue();
        MessageBuilder.sendMessage(player, msg.get("msg.chat.too-fast").replace("<sec>", sec + ""));
        this.spamDuplicateMap.putIfAbsent(player.getUniqueId(), 0);
        this.spamDuplicateMap.computeIfPresent(player.getUniqueId(), (d, i) -> ++i);
        if (this.spamDuplicateMap.get(player.getUniqueId()) >= chatConfig.antiSpamDuplicate) {
            antiSpam.put(player.getUniqueId(), LocalDateTime.now());
        }
        return true;
    }

    @Override
    public boolean antiAdvertise(ProxiedPlayer player, final ChatEvent e) {
        List<String> ipWhitelist = chatConfig.whitelist.get("ips");
        List<String> domainWhitelist = chatConfig.whitelist.get("domains");
        boolean cancel;
        final String line = e.getMessage();
        Matcher ipMatcher = IP_PATTERN.matcher(line);
        Matcher domainMatcher = DOMAIN_PATTERN.matcher(line);
        cancel = checkMatch(ipWhitelist, ipMatcher) || checkMatch(domainWhitelist, domainMatcher);
        if (cancel) {
            e.setCancelled(true);
            MessageBuilder.sendMessage(player, msg.get("msg.chat.advertise"));
        }
        return cancel;
    }

    @Override
    public boolean antiCharDuplicate(ProxiedPlayer player, final ChatEvent e) {
        int spamChatMax = chatConfig.spamCharMax;
        final String message = e.getMessage();
        int i = 0;
        char previousChar = '\u0000';
        boolean cancel = false;
        for (char ch : message.toCharArray()) {

            if (previousChar == '\u0000') {
                previousChar = ch;
                continue;
            }

            if (previousChar == ch) {
                i++;
            } else {
                previousChar = ch;
                i = 0;
            }

            if (i >= spamChatMax) {
                cancel = true;
                break;
            }
        }
        if (cancel) {
            e.setCancelled(true);
            MessageBuilder.sendMessage(player, msg.get("msg.chat.no-spam"));
            return true;
        }
        return false;
    }

    @Override
    public boolean antiDuplicate(ProxiedPlayer player, final ChatEvent e) {
        Duplication duplication = this.duplicationMap.getOrDefault(player.getUniqueId(), new Duplication(1, e.getMessage()));
        boolean cancel = false;
        if (this.duplicationMap.containsKey(player.getUniqueId())) {
            final String msg = e.getMessage();
            if (msg.equalsIgnoreCase(duplication.message)) {
                duplication.duplicated += 1;
            } else {
                duplication = new Duplication(1, e.getMessage());
            }
            int duplicate = chatConfig.duplicateMax;
            cancel = duplication.duplicated >= duplicate;
            if (cancel) {
                e.setCancelled(true);
                MessageBuilder.sendMessage(player, this.msg.get("msg.chat.no-spam"));
            }
        }
        this.duplicationMap.put(player.getUniqueId(), duplication);
        return cancel;
    }

    private boolean checkMatch(List<String> whitelist, Matcher matcher) {
        boolean cancel = false;
        if (matcher.find()) {
            String find = matcher.group().replace(".", "").replace(" ", "").replace("(dot)", "").replace("dot", "");
            cancel = true;
            for (String list : whitelist) {
                if (find.contains(list.replace(".", ""))) {
                    cancel = false;
                    break;
                }
            }
        }
        return cancel;
    }

    private static class Duplication {

        private int duplicated;
        private String message;

        private Duplication(int duplicated, String message) {
            this.duplicated = duplicated;
            this.message = message;
        }
    }

}
