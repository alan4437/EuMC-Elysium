package dev.discordlink.gui;

import java.nio.file.*;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.loader.api.FabricLoader;

/** Local menu preferences; never changes Minecraft's global options. */
public final class GuiPreferences {
  private static final Path FILE =
      FabricLoader.getInstance().getConfigDir().resolve("gamblingmenu-gui.properties");
  public static int duration = 320;
  public static boolean blur = true, animations = true, shadows = true;
  public static int corners = 100, gloss = 100;
  public static int radius(int r) {
    return r >= 8 ? Math.round(r * corners / 100f) : r;
  }
  public static int accent = 0xeeeeef, surface = 0x39393e, opacity = 75, glow = 100,
                    blurStrength = 8, dim = 28;
  public static void reset() {
    corners = 100;
    gloss = 100;
    duration = 320;
    blur = true;
    animations = true;
    shadows = true;
    accent = 0xeeeeef;
    surface = 0x39393e;
    opacity = 75;
    glow = 100;
    blurStrength = 8;
    dim = 28;
  }
  public static boolean setColour(String value, boolean isAccent) {
    if (!value.matches("#?[0-9a-fA-F]{6}"))
      return false;
    int rgb = Integer.parseInt(value.replace("#", ""), 16);
    if (isAccent)
      accent = rgb;
    else
      surface = rgb;
    save();
    return true;
  }
  public static String hex(int rgb) {
    return String.format("#%06X", rgb);
  }
  private static int number(Properties p, String key, int fallback, int min, int max) {
    try {
      return Math.clamp(Integer.parseInt(p.getProperty(key)), min, max);
    } catch (Exception e) {
      return fallback;
    }
  }
  private static CompletableFuture<Void> writes = CompletableFuture.completedFuture(null);
  static {
    if (Files.isRegularFile(FILE)) {
      try (var in = Files.newInputStream(FILE)) {
        Properties p = new Properties();
        p.load(in);
        duration = Math.clamp(Integer.parseInt(p.getProperty("duration", "320")), 160, 640);
        blur = Boolean.parseBoolean(p.getProperty("blur", "true"));
        corners = number(p, "corners", 100, 0, 100);
        gloss = number(p, "gloss", 100, 0, 150);
        animations = Boolean.parseBoolean(p.getProperty("animations", "true"));
        shadows = Boolean.parseBoolean(p.getProperty("shadows", "true"));
        accent = number(p, "accent", accent, 0, 0xffffff);
        surface = number(p, "surface", surface, 0, 0xffffff);
        opacity = number(p, "opacity", 75, 25, 100);
        glow = number(p, "glow", 100, 0, 150);
        blurStrength = number(p, "blurStrength", 8, 2, 16);
        dim = number(p, "dim", 28, 0, 70);
      } catch (Exception ignored) {
        duration = 320;
        blur = true;
      }
    }
  }
  public static void save() {
    Properties p = new Properties();
    p.setProperty("duration", Integer.toString(duration));
    p.setProperty("blur", Boolean.toString(blur));
    p.setProperty("corners", Integer.toString(corners));
    p.setProperty("gloss", Integer.toString(gloss));
    p.setProperty("animations", Boolean.toString(animations));
    p.setProperty("shadows", Boolean.toString(shadows));
    p.setProperty("accent", Integer.toString(accent));
    p.setProperty("surface", Integer.toString(surface));
    p.setProperty("opacity", Integer.toString(opacity));
    p.setProperty("glow", Integer.toString(glow));
    p.setProperty("blurStrength", Integer.toString(blurStrength));
    p.setProperty("dim", Integer.toString(dim));
    writes = writes.handle((v, e) -> null).thenRunAsync(() -> {
      try {
        Files.createDirectories(FILE.getParent());
        Path tmp = Files.createTempFile(FILE.getParent(), "gui-", ".tmp");
        try {
          try (var out = Files.newOutputStream(tmp)) {
            p.store(out, "Gambling Menu GUI");
          }
          Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
          Files.deleteIfExists(tmp);
        }
      } catch (Exception e) {
        org.slf4j.LoggerFactory.getLogger("gamblingmenu").warn("Could not save GUI preferences", e);
      }
    });
  }
  private GuiPreferences() {}
}
