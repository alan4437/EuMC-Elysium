package dev.gamblingmenu.blacklist;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/** Case-insensitive Java usernames, preserving original spelling and insertion order. */
public final class UsernameBlacklist {
  private final LinkedHashMap<String, String> names = new LinkedHashMap<>();
  private static String key(String name) {
    return name.strip().toLowerCase(Locale.ROOT);
  }
  public String add(String name) {
    name = name.strip();
    if (!name.matches("[A-Za-z0-9_]{3,16}"))
      return "Use a Minecraft username (3–16 letters, digits or _).";
    if (names.containsKey(key(name)))
      return "That username is already listed.";
    if (names.size() >= 200)
      return "The list is full (200 usernames).";
    names.put(key(name), name);
    return "Added " + name;
  }
  public boolean remove(String name) {
    return names.remove(key(name)) != null;
  }
  public boolean contains(String name) {
    return names.containsKey(key(name));
  }
  public List<String> names() {
    return List.copyOf(names.values());
  }
}
