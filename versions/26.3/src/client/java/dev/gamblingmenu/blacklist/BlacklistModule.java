package dev.gamblingmenu.blacklist;

import com.google.gson.Gson;
import dev.gamblingmenu.payment.PaymentModule;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.loader.api.FabricLoader;

public final class BlacklistModule {
  public static final BlacklistModule INSTANCE = new BlacklistModule();
  private final UsernameBlacklist users = new UsernameBlacklist();
  private final Path path =
      FabricLoader.getInstance().getConfigDir().resolve("gamblingmenu-blacklist.json");
  private boolean enabled;
  private String error = "";
  private CompletableFuture<Void> writes = CompletableFuture.completedFuture(null);
  public boolean enabled() {
    return enabled;
  }
  public boolean contains(String name) {
    return users.contains(name);
  }
  public List<String> names() {
    return users.names();
  }
  public String error() {
    return error;
  }
  public void toggle() {
    enabled = !enabled;
    if (!enabled)
      PaymentModule.INSTANCE.cancelBlacklistRefunds();
  }
  public void disable() {
    enabled = false;
    PaymentModule.INSTANCE.cancelBlacklistRefunds();
  }
  public void initialize() {
    try {
      if (Files.isRegularFile(path) && Files.size(path) < 16384) {
        String[] entries = new Gson().fromJson(Files.readString(path), String[].class);
        if (entries != null)
          for (String entry : entries)
            if (entry != null)
              users.add(entry);
      }
    } catch (Exception e) {
      error = "Could not load the saved blacklist.";
    }
  }
  public String add(String name) {
    String result = users.add(name);
    if (result.startsWith("Added "))
      save();
    return result;
  }
  public void remove(String name) {
    if (users.remove(name))
      save();
  }
  private void save() {
    var snapshot = users.names();
    error = "";
    writes = writes.handle((v, e) -> null).thenRunAsync(() -> {
      try {
        Files.createDirectories(path.getParent());
        Path temporary = Files.createTempFile(path.getParent(), "blacklist-", ".tmp");
        try {
          Files.writeString(temporary, new Gson().toJson(snapshot));
          try {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
          } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
          }
        } finally {
          Files.deleteIfExists(temporary);
        }
      } catch (Exception e) {
        net.minecraft.client.Minecraft.getInstance().execute(
            () -> error = "Could not save the blacklist.");
      }
    });
  }
}
