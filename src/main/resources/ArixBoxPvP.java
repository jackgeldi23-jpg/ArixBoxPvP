package com.arix.boxpvp;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.server.TabCompleteEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ArixBoxPvP extends JavaPlugin implements Listener, CommandExecutor {

    private File dataFile;
    private FileConfiguration dataConfig;

    private final Map<UUID, Long> chatCooldowns = new HashMap<>();
    private final Set<UUID> cratePreviewers = new HashSet<>();
    private boolean maintenanceMode = false;

    private final Map<String, String> swearList = new LinkedHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadDataFile();
        setupSwearList();

        getServer().getPluginManager().registerEvents(this, this);

        String[] cmds = {"kasaayarla", "kasasil", "anahtarver", "duyuru", "bakimaal", "ip", "yardim", "medya", "kurallar", "cekilis", "pv", "pv1", "pv2", "pv3", "pv4", "pv5", "pvsil", "pvbak"};
        for (String cmd : cmds) {
            if (getCommand(cmd) != null) {
                getCommand(cmd).setExecutor(this);
            }
        }

        getLogger().info("ArixBoxPvP Java 22 / Purpur 1.20.1 üzerinde sorunsuz başlatıldı!");
    }

    @Override
    public void onDisable() {
        saveDataFile();
    }

    private void setupSwearList() {
        swearList.put("sik", "2h");
        swearList.put("orosbu", "4h");
        swearList.put("meme", "3h");
        swearList.put("amk", "4h");
        swearList.put("yarrak", "3h");
        swearList.put("pic", "1h");
        swearList.put("oocc", "2h");
        swearList.put("yrm", "3h");
        swearList.put("yrk", "3h");
        swearList.put("yrrk", "3h");
        swearList.put("sg", "2h");
        swearList.put("oe", "3h");
        swearList.put("anneni", "6h");
        swearList.put("orospu", "4h");
        swearList.put("orspu", "4h");
        swearList.put("babani", "6h");
        swearList.put("atani", "1d");
    }

    private String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    private String locToString(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private boolean isCrate(Location loc) {
        return dataConfig.contains("kasa." + locToString(loc));
    }

    private String getCrateType(Location loc) {
        return dataConfig.getString("kasa." + locToString(loc));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCrateBreak(BlockBreakEvent event) {
        if (isCrate(event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(color("&c&lARİX KORUMA &8» &7Kasalar kırılamaz!"));
        }
    }

    @EventHandler
    public void onCrateInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) return;

        if (block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST && !block.getType().name().contains("SHULKER_BOX")) {
            return;
        }

        Location loc = block.getLocation();
        if (!isCrate(loc)) return;

        Player p = event.getPlayer();
        String crateType = getCrateType(loc);

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (!p.isOp()) {
                event.setCancelled(true);
                Container container = (Container) block.getState();
                p.openInventory(container.getInventory());
                cratePreviewers.add(p.getUniqueId());
                p.sendMessage(color("&eKasa önizleme modu! Sadece ödüllere bakabilirsin."));
            }
        } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            ItemStack hand = p.getInventory().getItemInMainHand();

            if (hand == null || hand.getType() == Material.AIR) {
                p.sendMessage(color("&cKanka kasayı açmak için elinde anahtar olmalı! SAĞ TIKLA içine bakabilirsin."));
                return;
            }

            ItemMeta meta = hand.getItemMeta();
            String itemName = (meta != null && meta.hasDisplayName()) ? ChatColor.stripColor(meta.getDisplayName()) : "";

            boolean approve = false;
            if (crateType.equalsIgnoreCase("kit") && itemName.contains("Kit Kasası")) approve = true;
            else if (crateType.equalsIgnoreCase("para") && itemName.contains("Para Kasası")) approve = true;
            else if (crateType.equalsIgnoreCase("alet") && itemName.contains("Alet Kasası")) approve = true;
            else if (crateType.equalsIgnoreCase("event") && itemName.contains("Event Kasası")) approve = true;
            else if (crateType.equalsIgnoreCase("charm") && itemName.contains("Charm Kasası")) approve = true;

            if (approve) {
                Container container = (Container) block.getState();
                List<ItemStack> rewards = new ArrayList<>();
                for (ItemStack item : container.getInventory().getContents()) {
                    if (item != null && item.getType() != Material.AIR) {
                        rewards.add(item.clone());
                    }
                }

                if (rewards.isEmpty()) {
                    p.sendMessage(color("&cKanka bu kasa boş! OP hesabınla SAĞ TIKLA ve içine ödül koy."));
                    return;
                }

                hand.setAmount(hand.getAmount() - 1);
                ItemStack prize = rewards.get(new Random().nextInt(rewards.size()));
                p.getInventory().addItem(prize);

                p.playSound(p.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
                p.sendMessage(color("&aBaşarıyla kasayı SOL tıkla açtın ve ödülünü aldın!"));
            } else {
                p.sendMessage(color("&cKanka yanlış anahtar! Bu kasaya uymuyor. SAĞ TIKLA içine bakabilirsin."));
            }
        }
    }

    @EventHandler
    public void onCratePreviewClick(InventoryClickEvent event) {
        Player p = (Player) event.getWhoClicked();
        if (cratePreviewers.contains(p.getUniqueId()) && !p.isOp()) {
            event.setCancelled(true);
        }

        String title = event.getView().getTitle();
        if (title.equals(color("&9&lMEDYA ŞARTLARI")) || title.equals(color("&c&lSUNUCU KURALLARI")) || title.equals(color("&8&lSanal Depoların"))) {
            event.setCancelled(true);

            if (title.equals(color("&8&lSanal Depoların")) && event.getCurrentItem() != null) {
                int slot = event.getRawSlot();
                if (slot >= 2 && slot <= 6) {
                    p.performCommand("pv" + (slot - 1));
                }
            }
        }
    }

    @EventHandler
    public void onCratePreviewClose(InventoryCloseEvent event) {
        Player p = (Player) event.getPlayer();
        cratePreviewers.remove(p.getUniqueId());

        String title = event.getView().getTitle();
        if (title.contains(color("&c&lİnceleme:"))) return;

        for (int i = 1; i <= 5; i++) {
            if (title.equals(color("&8&lSanal Depo - " + i))) {
                savePV(p.getUniqueId(), i, event.getInventory());
                break;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player p = event.getPlayer();
        String msg = event.getMessage();

        if (!p.isOp()) {
            long last = chatCooldowns.getOrDefault(p.getUniqueId(), 0L);
            if (System.currentTimeMillis() - last < 2000) {
                event.setCancelled(true);
                p.sendMessage(color("&c&lARİX KORUMA &8» &7Tekrar mesaj yazabilmek için &e2 saniye &7beklemelisiniz!"));
                return;
            }
            chatCooldowns.put(p.getUniqueId(), System.currentTimeMillis());
        }

        if (msg.equalsIgnoreCase("!ip")) {
            event.setCancelled(true);
            p.sendMessage(color("&5&l» &9&lARIXMCTR &8» &7Sunucu IP Adresi: &barixmctr.aternos.me"));
            return;
        }

        if (!p.isOp()) {
            String lowerMsg = msg.toLowerCase(Locale.ROOT);
            for (Map.Entry<String, String> entry : swearList.entrySet()) {
                if (lowerMsg.contains(entry.getKey())) {
                    event.setCancelled(true);
                    String duration = entry.getValue();
                    Bukkit.getScheduler().runTask(this, () -> {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mute " + p.getName() + " " + duration + " Otomatik Küfür Filtresi");
                    });
                    p.sendMessage(color("&5&l» &9&lARIXMCTR &8» &cArgo/Küfür kullanımı yasaktır! &e" + duration + " &cmutelendiniz."));
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player p = event.getPlayer();
        if (p.isOp()) return;

        String cmd = event.getMessage().toLowerCase(Locale.ROOT);
        String blockedMsg = color("&c&lARİX KORUMA &8» &7Bu komut sunucu güvenliği nedeniyle engellenmiştir!");

        if (cmd.startsWith("/plugins") || cmd.startsWith("/pl") || cmd.startsWith("/bukkit:") ||
            cmd.startsWith("/ver") || cmd.startsWith("/version") || cmd.startsWith("/about") ||
            cmd.startsWith("/help") || cmd.contains(":")) {
            event.setCancelled(true);
            p.sendMessage(blockedMsg);
            return;
        }

        if (cmd.startsWith("/irp") || cmd.startsWith("/inventoryrollback")) {
            if (cmd.contains("restore")) {
                notifyOps(color("&c&lARİX GÜVENLİK &8» &e&l" + p.getName() + " &7adlı yetkili &c" + event.getMessage() + " &7komutunu kullandı!"));
            }
        }
    }

    @EventHandler
    public void onTabComplete(TabCompleteEvent event) {
        if (!(event.getSender() instanceof Player)) return;
        Player p = (Player) event.getSender();
        if (p.isOp()) return;

        String buffer = event.getBuffer().toLowerCase(Locale.ROOT);
        if (buffer.startsWith("/pl") || buffer.startsWith("/plugins") || buffer.startsWith("/bukkit:") ||
            buffer.startsWith("/ver") || buffer.startsWith("/about") || buffer.startsWith("/?")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (maintenanceMode && !event.getPlayer().isOp()) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, color("&c&lSUNUCU BAKIM MODUNDA!\n&7Lütfen daha sonra tekrar dene kanka."));
        }
    }

    private void notifyOps(String msg) {
        for (Player op : Bukkit.getOnlinePlayers()) {
            if (op.isOp()) {
                op.sendMessage(msg);
                op.playSound(op.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player) && !label.equalsIgnoreCase("bakimaal")) {
            sender.sendMessage("Bu komut sadece oyuncular içindir.");
            return true;
        }

        Player p = (sender instanceof Player) ? (Player) sender : null;

        switch (label.toLowerCase(Locale.ROOT)) {
            case "kasaayarla":
                if (args.length < 1) {
                    p.sendMessage(color("&cKullanım: /kasaayarla [kit/para/alet/event/charm]"));
                    return true;
                }
                Block target = p.getTargetBlockExact(5);
                if (target != null && (target.getType() == Material.CHEST || target.getType() == Material.TRAPPED_CHEST || target.getType().name().contains("SHULKER_BOX"))) {
                    dataConfig.set("kasa." + locToString(target.getLocation()), args[0].toLowerCase());
                    saveDataFile();
                    p.sendMessage(color("&aBaktığın sandık başarıyla &e&l" + args[0] + " Kasası &aolarak ayarlandı!"));
                } else {
                    p.sendMessage(color("&cKanka baktığın bloğun bir Sandık veya Shulker olması lazım!"));
                }
                break;

            case "kasasil":
                Block targetDel = p.getTargetBlockExact(5);
                if (targetDel != null && isCrate(targetDel.getLocation())) {
                    dataConfig.set("kasa." + locToString(targetDel.getLocation()), null);
                    saveDataFile();
                    p.sendMessage(color("&cKasa başarıyla silindi, artık normal sandık."));
                } else {
                    p.sendMessage(color("&cBaktığın yer zaten bir kasa değil kanka."));
                }
                break;

            case "anahtarver":
                if (args.length < 3) {
                    p.sendMessage(color("&cKullanım: /anahtarver [oyuncu/herkes] [kit/para/alet/event/charm] [miktar]"));
                    return true;
                }
                int amount = Integer.parseInt(args[2]);
                String crateType = args[1].toLowerCase();
                ItemStack key = createKeyItem(crateType, amount);

                if (args[0].equalsIgnoreCase("herkes") || args[0].equalsIgnoreCase("@a") || args[0].equalsIgnoreCase("all")) {
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        online.getInventory().addItem(key.clone());
                    }
                    Bukkit.broadcastMessage(color("&c&lARİX MCTR &8» &a&lHERKESE &f" + amount + " &aadet &e" + crateType + " kasası anahtarı &adağıtıldı!"));
                } else {
                    Player targetPlayer = Bukkit.getPlayer(args[0]);
                    if (targetPlayer != null && targetPlayer.isOnline()) {
                        targetPlayer.getInventory().addItem(key);
                        p.sendMessage(color("&e" + targetPlayer.getName() + " &aadlı oyuncuya &f" + amount + " &aadet &e" + crateType + " &aanahtarı verdin!"));
                    } else {
                        p.sendMessage(color("&cBelirttiğin oyuncu oyunda değil veya geçersiz bir isim girdin!"));
                    }
                }
                break;

            case "duyuru":
                if (args.length == 0) {
                    p.sendMessage(color("&cKullanım: /duyuru [mesaj]"));
                    return true;
                }
                String msg = String.join(" ", args);
                for (Player online : Bukkit.getOnlinePlayers()) {
                    online.sendTitle(color("&e&lDUYURU"), color("&f" + msg), 10, 100, 20);
                    online.playSound(online.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                }
                break;

            case "bakimaal":
                if (args.length < 1) {
                    sender.sendMessage(color("&cKullanım: /bakimaal <ac/kapat>"));
                    return true;
                }
                if (args[0].equalsIgnoreCase("ac")) {
                    maintenanceMode = true;
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if (!online.isOp()) {
                            online.kickPlayer(color("&c&lSUNUCU BAKIM MODUNDA!\n&7Daha sonra tekrar görüşmek üzere kanka."));
                        }
                    }
                    sender.sendMessage(color("&6&lBoxPvP &r&8» &eBakım modu açıldı, yetkisiz herkes atıldı!"));
                } else if (args[0].equalsIgnoreCase("kapat")) {
                    maintenanceMode = false;
                    sender.sendMessage(color("&6&lBoxPvP &r&8» &aBakım modu kapatıldı, sunucuya girişler serbest!"));
                }
                break;

            case "ip":
                p.sendMessage(color("&5&l» &9&lARIXMCTR &8» &7Sunucu IP Adresi: &barixmctr.aternos.me"));
                break;

            case "yardim":
                p.sendMessage("");
                p.sendMessage(color("&5&l» &9&lARIXMCTR &8» &d&lREHBER VE YARDIM &5&l«"));
                p.sendMessage(color("&7- &b/kit &7yazarak oyuncu kitinizi alın."));
                p.sendMessage(color("&7- Madenlerde kasılıp Takas bölgesinden paraya çevirin ve oynayın!"));
                p.sendMessage(color("&7- Başka sorunlar için: &b/discord &7yazabilirsiniz."));
                p.sendMessage("");
                break;

            case "medya":
                openGUI(p, "&9&lMEDYA ŞARTLARI", Material.ENDER_PEARL, "&d&lMEDYA ŞARTLARI", Arrays.asList(
                    "&cMedya Şartları:", "&7- 800+ izlenen Video", "&7- Kesintisiz 8+ izlenen yayın",
                    "&7- 200+ izlenen YouTube Shorts", "&7- 150+ Takipçi", "&7- 13+ Yaş", "",
                    "&eBunlardan En az 3'ü uygunsa", "&eEn kısa zamanda Discord'dan Ticket Açınız!!"
                ));
                break;

            case "kurallar":
                openGUI(p, "&c&lSUNUCU KURALLARI", Material.PAPER, "&c&lSUNUCU KURALLARI", Arrays.asList(
                    "&4&lGENEL KURALLAR:", "&7- Hile, makro ve 3. parti yazılım kullanımı kesinlikle yasaktır.",
                    "&7- Küfür, hakaret, argo ve kışkırtıcı söylemler yasaktır.", "&7- Reklam ve tanıtım yapmak yasaktır.",
                    "&7- Dini, milli ve siyasi değerlere hakaret süresiz ban sebebidir.", "",
                    "&c&lÖNEMLİ MADDELER:", "&c- Sunucu açığı bulan oyuncuların bildirmesi zorunludur."
                ));
                break;

            case "cekilis":
                if (Bukkit.getOnlinePlayers().size() < 2) {
                    p.sendMessage(color("&c&lARİX MCTR &8» &7Çekiliş başlatabilmek için sunucuda en az &e2 oyuncu &7olmalıdır!"));
                    return true;
                }
                Bukkit.broadcastMessage(color("&c&lARİX MCTR &8» &a&lÇEKİLİŞ BAŞLADI! &7Kazanan 6 saniye içinde belirleniyor..."));
                List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());

                new BukkitRunnable() {
                    int count = 0;
                    @Override
                    public void run() {
                        if (count >= 12) {
                            Player winner = players.get(new Random().nextInt(players.size()));
                            for (Player online : Bukkit.getOnlinePlayers()) {
                                online.sendTitle(color("&a&lKAZANAN!"), color("&e&l" + winner.getName()), 10, 100, 20);
                                online.playSound(online.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                            }
                            Bukkit.broadcastMessage(color("&8&m----------------------------------------
