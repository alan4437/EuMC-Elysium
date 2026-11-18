package dev.discordlink.gui;

import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

/** Single-category, local-only menu shell. */
public final class MenuScreen extends Screen {
  private final Screen parent;
  private final long opened = System.nanoTime();
  private long closing;
  private int x, y, w, h, sidebar, cx, cw;
  private SearchInput search;
  private float contentScale = 1;
  private String query = "";
  private boolean settingsOpen, modeOpen, blacklistSettings, guiSettings, multiplierSettings,
      europePage, auctionSettings, profitSettings, resetProfitConfirm;
  private AmountInput usernameInput;
  private String usernameDraft = "", blacklistMessage = "";
  private int blacklistScroll, profitScroll;
  private long inspectorOpened, inspectorClosing;
  private final java.util.Set<net.minecraft.client.gui.components.AbstractWidget> inspectorWidgets =
      new java.util.HashSet<>();
  public void openBlacklistSettings() {
    profitSettings = false;
    auctionSettings = false;
    europePage = false;
    multiplierSettings = false;
    if (settingsEditedAt != 0)
      applyPaymentSettings();
    if (!settingsOpen || !blacklistSettings)
      inspectorOpened = System.nanoTime();
    blacklistSettings = true;
    settingsOpen = true;
    inspectorClosing = 0;
    rebuild();
  }
  public boolean blacklistSettingsOpen() {
    return settingsOpen && blacklistSettings;
  }
  private void closeInspector() {
    if (settingsEditedAt != 0)
      applyPaymentSettings();
    inspectorClosing = System.nanoTime();
    for (var widget : inspectorWidgets) widget.active = false;
  }
  private float inspectorOffset() {
    if (!GuiPreferences.animations)
      return 0;
    float progress = inspectorClosing == 0
        ? Math.min(
              1, (System.nanoTime() - inspectorOpened) / (GuiPreferences.duration * 1_000_000f))
        : Math.max(
              0, 1 - (System.nanoTime() - inspectorClosing) / (GuiPreferences.duration * 750_000f));
    return (float) Math.pow(1 - progress, 3) * 24;
  }
  private int settingsX, settingsW, contentW, mainW;
  private AmountInput minInput, maxInput;
  private String minDraft = dev.gamblingmenu.payment.PaymentModule.INSTANCE.min();
  private String maxDraft = dev.gamblingmenu.payment.PaymentModule.INSTANCE.max();
  private dev.gamblingmenu.payment.PaymentRules.Mode modeDraft =
      dev.gamblingmenu.payment.PaymentModule.INSTANCE.mode();
  private String settingsMessage = "";
  private long settingsEditedAt;
  public void openPaymentSettings() {
    profitSettings = false;
    auctionSettings = false;
    europePage = false;
    multiplierSettings = false;
    if (!settingsOpen || blacklistSettings)
      inspectorOpened = System.nanoTime();
    blacklistSettings = false;
    inspectorClosing = 0;
    settingsOpen = true;
    rebuild();
  }
  public String settingsValidationMessage() {
    return settingsMessage;
  }
  public boolean paymentSettingsOpen() {
    return settingsOpen;
  }
  public void paymentDrafts(String low, String high) {
    minInput.setValue(low);
    maxInput.setValue(high);
  }
  public String applyPaymentSettings() {
    if (minInput != null) {
      minDraft = minInput.getValue();
      maxDraft = maxInput.getValue();
    }
    String result =
        dev.gamblingmenu.payment.PaymentModule.INSTANCE.apply(minDraft, maxDraft, modeDraft);
    settingsMessage = result.equals("Settings applied") ? "" : result;
    settingsEditedAt = 0;
    return result;
  }
  private void settingsEdited() {
    settingsEditedAt = System.nanoTime();
    settingsMessage = "";
  }
  private final List<ModuleDefinition> modules = List.of(
      new ModuleDefinition("payments", "Payment Guard", "Payment limits, refunds and donations"),
      new ModuleDefinition(
          "blacklist", "Gamble Blacklist", "Refund payments from blocked usernames"),
      new ModuleDefinition("multipliers", "Payment Multipliers", "Clickable chat payout options"),
      new ModuleDefinition("auction", "Auction", "Timed auctions and top five bids"),
      new ModuleDefinition(
          "profit", "Profit Tracker", "Live session and lifetime payment statistics"));

  public MenuScreen(Screen parent) {
    super(Inter.text("Gambling", Inter.Weight.SEMIBOLD));
    this.parent = parent;
  }
  public record ModuleDefinition(String id, String name, String description) {
    public boolean matches(String query) {
      String key = query.strip().toLowerCase(Locale.ROOT);
      return (name + " " + description).toLowerCase(Locale.ROOT).contains(key);
    }
  }
  public List<ModuleDefinition> searchResults() {
    return modules.stream()
        .filter(m
            -> (europePage == (m.id().equals("auction") || m.id().equals("multipliers")))
                && m.matches(query))
        .toList();
  }
  public void setSearchQuery(String value) {
    search.setValue(value);
  }
  public String searchQuery() {
    return query;
  }
  @Override
  public void onClose() {
    if (closing == 0) {
      if (settingsEditedAt != 0)
        applyPaymentSettings();
      dev.gamblingmenu.profit.ProfitTracker.INSTANCE.release();
      closing = System.nanoTime();
      setFocused(null);
      for (var child : children())
        if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget)
          widget.active = false;
    }
  }
  @Override
  protected void init() {
    // Keep physical typography consistent across Minecraft GUI scales, including Auto.
    double guiScale = minecraft.getWindow().getGuiScale();
    float physicalScale = Math.min(Math.clamp(minecraft.getWindow().getWidth() / 1280f, .9f, 2f),
        Math.min(
            minecraft.getWindow().getWidth() / 640f, minecraft.getWindow().getHeight() / 480f));
    contentScale = (float) (physicalScale / guiScale);
    int viewportWidth = Math.round(width / contentScale);
    int viewportHeight = Math.round(height / contentScale);
    w = Math.min(1400, viewportWidth - 20);
    h = Math.min(900, viewportHeight - 16);
    x = (viewportWidth - w) / 2;
    y = (viewportHeight - h) / 2;
    sidebar = Math.min(208, Math.max(118, w / 5));
    cx = x + sidebar + 18;
    cw = w - sidebar - 36;
    rebuild();
    if (closing != 0)
      for (var child : children())
        if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget)
          widget.active = false;
  }
  private void rebuild() {
    if (minInput != null) {
      minDraft = minInput.getValue();
      maxDraft = maxInput.getValue();
    }
    if (usernameInput != null)
      usernameDraft = usernameInput.getValue();
    usernameInput = null;
    minInput = null;
    maxInput = null;
    clearWidgets();
    // The inspector is a separate full-height glass pane, matching the supplied reference.
    settingsW = Math.min(330, Math.max(230, Math.round(w * .265f)));
    settingsX = x + w - settingsW;
    mainW = settingsOpen ? w - settingsW - 16 : w;
    sidebar = Math.min(170, Math.max(128, mainW / 5));
    cx = x + sidebar + 16;
    cw = mainW - sidebar - 32;
    contentW = cw;
    addRenderableWidget(new DiscordInviteButton(x + mainW - 46, y + h - 46));
    addRenderableWidget(new CategoryEntry(x + 10, y + 82, sidebar - 20, "Gambling",
        b -> selectPage(false), !guiSettings && !europePage));
    addRenderableWidget(new CategoryEntry(
        x + 10, y + h - 44, sidebar - 20, "Settings", b -> selectPage(true), guiSettings));
    addRenderableWidget(new CategoryEntry(
        x + 10, y + 120, sidebar - 20, "EuropeMC", b -> selectEuropePage(), europePage));
    inspectorWidgets.clear();
    if (guiSettings) {
      buildGuiSettings();
      return;
    }
    search = addRenderableWidget(
        new SearchInput(Inter.editingFont(), cx + 28, y + 27, Math.max(30, cw - 42)));
    search.setValue(query);
    search.setResponder(value -> {
      query = value;
      int cols = contentW >= 520 ? 2 : 1, cardWidth = (contentW - (cols - 1) * 10) / cols, slot = 0;
      for (var child : children())
        if (child instanceof PaymentCard card) {
          card.visible = modules.get(card.kind()).matches(query);
          if (card.visible) {
            card.setX(cx + (slot % cols) * (cardWidth + 10));
            card.setY(y + 70 + (slot / cols) * 58);
            slot++;
          }
        }
    });

    int columns = contentW >= 520 ? 2 : 1;
    int cardWidth = (contentW - (columns - 1) * 10) / columns;
    int slot = 0;
    for (int index = 0; index < modules.size(); index++) {
      if (europePage != (index == 2 || index == 3))
        continue;
      boolean blacklist = index == 1;
      Runnable open = index == 4 ? this::openProfitSettings
          : index == 3           ? this::openAuctionSettings
          : index == 2           ? this::openMultiplierSettings
          : blacklist            ? this::openBlacklistSettings
                                 : this::openPaymentSettings;
      var card = addRenderableWidget(new PaymentCard(cx + (slot % columns) * (cardWidth + 10),
          y + 70 + (slot / columns) * 58, cardWidth, open, index));
      card.visible = modules.get(index).matches(query);
      if (card.visible)
        slot++;
    }
    inspectorWidgets.clear();
    var before = new java.util.HashSet<>(children());
    if (settingsOpen) {
      if (profitSettings)
        buildProfitSettings();
      else if (auctionSettings)
        buildAuctionSettings();
      else if (multiplierSettings)
        buildMultiplierSettings();
      else if (blacklistSettings)
        buildBlacklistSettings();
      else
        buildPaymentSettings();
    }
    for (var child : children())
      if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget
          && !before.contains(child))
        inspectorWidgets.add(widget);
    setFocused((net.minecraft.client.gui.components.events.GuiEventListener) null);
  }
  private void selectPage(boolean settings) {
    if (settingsEditedAt != 0)
      applyPaymentSettings();
    guiSettings = settings;
    europePage = false;
    settingsOpen = false;
    inspectorClosing = 0;
    modeOpen = false;
    rebuild();
  }
  private void preferenceButton(int column, int row, String label, Runnable change) {
    int cardWidth = (cw - 14) / 2;
    addRenderableWidget(new RoundedButton(
        cx + column * (cardWidth + 14) + 12, y + 158 + row * 32, cardWidth - 24, label, b -> {
          change.run();
          GuiPreferences.save();
          rebuild();
        }));
  }
  private void buildGuiSettings() {
    int cardWidth = (cw - 14) / 2;
    for (int i = 0; i < 2; i++)
      addRenderableWidget(
          new ColourWheel(cx + i * (cardWidth + 14) + 12, y + 105, cardWidth - 24, i == 0));
    preferenceButton(0, 1, "Animations: " + (GuiPreferences.animations ? "On" : "Off"),
        () -> GuiPreferences.animations = !GuiPreferences.animations);
    preferenceButton(0, 2, "Speed: " + GuiPreferences.duration + " ms",
        ()
            -> GuiPreferences.duration = GuiPreferences.duration == 320 ? 480
            : GuiPreferences.duration == 480                            ? 160
                                                                        : 320);
    preferenceButton(0, 3, "Glow: " + GuiPreferences.glow + "%",
        () -> GuiPreferences.glow = (GuiPreferences.glow + 25) % 175);
    preferenceButton(0, 4, "Shadows: " + (GuiPreferences.shadows ? "On" : "Off"),
        () -> GuiPreferences.shadows = !GuiPreferences.shadows);
    preferenceButton(1, 1, "Blur: " + (GuiPreferences.blur ? "On" : "Off"),
        () -> GuiPreferences.blur = !GuiPreferences.blur);
    preferenceButton(1, 2, "Blur strength: " + GuiPreferences.blurStrength,
        ()
            -> GuiPreferences.blurStrength =
                   GuiPreferences.blurStrength >= 16 ? 2 : GuiPreferences.blurStrength + 2);
    preferenceButton(1, 3, "Panel opacity: " + GuiPreferences.opacity + "%",
        ()
            -> GuiPreferences.opacity =
                   GuiPreferences.opacity >= 100 ? 25 : GuiPreferences.opacity + 25);
    preferenceButton(1, 4, "Background dim: " + GuiPreferences.dim + "%",
        ()
            -> GuiPreferences.dim =
                   GuiPreferences.dim >= 70 ? 0 : Math.min(70, GuiPreferences.dim + 14));
    preferenceButton(0, 5,
        "Corners: "
            + (GuiPreferences.corners == 100       ? "Rounded"
                    : GuiPreferences.corners == 50 ? "Soft"
                                                   : "Square"),
        ()
            -> GuiPreferences.corners = GuiPreferences.corners == 100 ? 50
            : GuiPreferences.corners == 50                            ? 0
                                                                      : 100);
    preferenceButton(1, 5, "Gloss: " + GuiPreferences.gloss + "%",
        () -> GuiPreferences.gloss = (GuiPreferences.gloss + 25) % 175);
    addRenderableWidget(
        new RoundedButton(cx, y + 374, Math.min(180, cw), "Reset GUI settings", b -> {
          GuiPreferences.reset();
          GuiPreferences.save();
          rebuild();
        }));
    addRenderableWidget(new RoundedButton(cx + 192, y + 374, Math.min(210, cw - 192),
        "Edit HUD positions", b -> minecraft.setScreen(new HudEditorScreen(this))));
  }
  private void renderGuiSettings(GuiGraphicsExtractor g) {
    Inter.draw(g, "GUI Settings", cx + 8, y + 27, 0xffeeeeef, Inter.Weight.MEDIUM);
    Glass.glow(g, cx + 6, y + 48, 90, 2, 1, 1);
    round(g, cx + 6, y + 48, 90, 2, 1, 0xff000000 | GuiPreferences.accent);
    int cardWidth = (cw - 14) / 2;
    for (int i = 0; i < 2; i++) {
      int px = cx + i * (cardWidth + 14);
      Glass.inset(g, px, y + 72, cardWidth, 286, 14);
      Inter.draw(g, i == 0 ? "Accent & animation" : "Panel & backdrop", px + 12, y + 86, 0xffeeeeef,
          Inter.Weight.SEMIBOLD);
    }
    Inter.draw(g, "Changes save automatically", cx, y + 410, 0xff989ca8, Inter.Weight.REGULAR);
  }
  public void openProfitSettings() {
    if (settingsEditedAt != 0)
      applyPaymentSettings();
    profitSettings = true;
    auctionSettings = multiplierSettings = blacklistSettings = false;
    europePage = guiSettings = false;
    settingsOpen = true;
    resetProfitConfirm = false;
    profitScroll = 0;
    inspectorOpened = System.nanoTime();
    inspectorClosing = 0;
    rebuild();
  }
  private void profitButton(int offset, String label, Runnable action) {
    addRenderableWidget(
        new RoundedButton(settingsX + 16, y + offset - profitScroll, settingsW - 32, label, b -> {
          action.run();
          dev.gamblingmenu.profit.ProfitTracker.INSTANCE.changed();
          rebuild();
        }));
  }
  private void buildProfitSettings() {
    var p = dev.gamblingmenu.profit.ProfitTracker.INSTANCE;
    int px = settingsX + 16, pw = settingsW - 32;
    profitScroll = Math.clamp(profitScroll, 0, Math.max(0, 648 - h));
    var before = new java.util.HashSet<>(children());
    addRenderableWidget(new PaymentToggle(px, y + 47, pw, 4));
    profitButton(82, "HUD: " + (p.showHud ? "Shown" : "Hidden"), () -> p.showHud = !p.showHud);
    profitButton(115, "View: " + (p.lifetimeView ? "Lifetime" : "Current session"),
        () -> p.lifetimeView = !p.lifetimeView);
    profitButton(
        148, resetProfitConfirm ? "Confirm session reset" : "Reset current session", () -> {
          if (resetProfitConfirm) {
            p.resetSession();
            resetProfitConfirm = false;
          } else
            resetProfitConfirm = true;
        });
    profitButton(181, "Colours: " + (p.customColour ? "Custom" : "GUI theme"),
        () -> p.customColour = !p.customColour);
    addRenderableWidget(new ColourWheel(px, y + 221 - profitScroll, pw, "", () -> p.primary, v -> {
      p.primary = v;
      p.customColour = true;
    }, p::changed));
    addRenderableWidget(new ColourWheel(px, y + 323 - profitScroll, pw, "", () -> p.surface, v -> {
      p.surface = v;
      p.customColour = true;
    }, p::changed));
    String[] names = {"Received", "Paid Out", "Profit", "Gambles"};
    for (int i = 0; i < 4; i++) {
      int n = i;
      addRenderableWidget(
          new RoundedButton(px + (i % 2) * (pw / 2 + 3), y + 411 + (i / 2) * 32 - profitScroll,
              pw / 2 - 3, names[i] + ": " + (p.rows[i] ? "On" : "Off"), b -> {
                p.rows[n] = !p.rows[n];
                p.changed();
                rebuild();
              }));
    }
    profitInput("HUD scale", p.scale, 484, .6f, 2f, v -> p.scale = v);
    profitInput("HUD opacity", p.opacity, 516, 20, 100, v -> p.opacity = Math.round(v));
    profitButton(553, "View detailed statistics",
        () -> minecraft.setScreen(new dev.gamblingmenu.profit.ProfitDetailsScreen(this)));
    for (var child : children())
      if (!before.contains(child)
          && child instanceof net.minecraft.client.gui.components.AbstractWidget widget
          && !(widget instanceof PaymentToggle)) {
        widget.visible = widget.getY() >= y + 80 && widget.getBottom() <= y + h - 12;
        widget.active = widget.visible;
      }
    addRenderableWidget(
        new RoundedButton(settingsX + settingsW - 42, y + 19, 26, "X", b -> closeInspector()));
  }
  private void profitInput(String label, float value, int offset, float min, float max,
      java.util.function.Consumer<Float> setter) {
    var input = addRenderableWidget(
        new AmountInput(settingsX + settingsW - 104, y + offset - profitScroll, 88, label,
            java.math.BigDecimal.valueOf(value)
                .setScale(2, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString()));
    input.setMaxLength(6);
    input.setResponder(text -> {
      try {
        float n = Float.parseFloat(text);
        if (!Float.isFinite(n) || n < min || n > max)
          throw new IllegalArgumentException();
        setter.accept(n);
        dev.gamblingmenu.profit.ProfitTracker.INSTANCE.changed();
        input.setTextColor(0xffeeeeef);
      } catch (Exception e) {
        input.setTextColor(0xffff7777);
      }
    });
  }
  private void renderProfitSettings(GuiGraphicsExtractor g) {
    int px = settingsX + 16, pw = settingsW - 32;
    Glass.panel(g, settingsX, y, settingsW, h, 26, true);
    Inter.draw(g, "Profit Tracker", px, y + 28, 0xffeeeeef, Inter.Weight.SEMIBOLD);
    g.enableScissor(px, y + 80, settingsX + settingsW - 12, y + h - 12);
    Inter.draw(g, "Text colour", px, y + 213 - profitScroll, 0xffaaaebb, Inter.Weight.REGULAR);
    Inter.draw(g, "Panel colour", px, y + 315 - profitScroll, 0xffaaaebb, Inter.Weight.REGULAR);
    String[] labels = {"Scale (0.6–2)", "Opacity (%)"};
    for (int i = 0; i < labels.length; i++) {
      Glass.inset(g, settingsX + settingsW - 110, y + 481 + i * 32 - profitScroll, 94, 27, 7);
      Inter.draw(
          g, labels[i], px, y + 491 + i * 32 - profitScroll, 0xffb8bbc7, Inter.Weight.REGULAR);
    }
    Inter.wrap(g, "Move HUDs from Settings → Edit HUD positions.", px, y + 590 - profitScroll, pw,
        0xff999daa);
    g.disableScissor();
  }
  private void selectEuropePage() {
    if (settingsEditedAt != 0)
      applyPaymentSettings();
    europePage = true;
    guiSettings = false;
    settingsOpen = false;
    modeOpen = false;
    inspectorClosing = 0;
    rebuild();
  }
  public void openAuctionSettings() {
    profitSettings = false;
    if (settingsEditedAt != 0)
      applyPaymentSettings();
    auctionSettings = true;
    multiplierSettings = false;
    blacklistSettings = false;
    settingsOpen = true;
    inspectorOpened = System.nanoTime();
    inspectorClosing = 0;
    rebuild();
  }
  private void buildAuctionSettings() {
    var auction = dev.gamblingmenu.auction.AuctionModule.INSTANCE;
    int px = settingsX + 16, pw = settingsW - 32;
    addRenderableWidget(
        new RoundedButton(settingsX + settingsW - 42, y + 19, 26, "X", b -> closeInspector()));
    addRenderableWidget(new RoundedButton(px, y + 65, pw - 94, "Choose item", b -> {
      if (!auction.enabled())
        minecraft.setScreen(new AuctionItemScreen(this));
    })).active = !auction.enabled();
    addRenderableWidget(new RoundedButton(px + pw - 86, y + 65, 86, "HUD appearance",
        b -> minecraft.setScreen(new dev.gamblingmenu.auction.AuctionHudScreen(this))));
    var min = addRenderableWidget(
        new AmountInput(px + 91, y + 147, pw - 101, "Auction minimum", auction.minimum()));
    min.setEditable(!auction.enabled());
    min.setResponder(v -> min.setTextColor(auction.minimum(v) ? 0xffeeeeef : 0xffff9999));
    var duration = addRenderableWidget(new AmountInput(px + 91, y + 185, pw - 101,
        "Auction duration seconds", Integer.toString(auction.seconds())));
    duration.setMaxLength(5);
    duration.setEditable(!auction.enabled());
    duration.setResponder(
        v -> duration.setTextColor(auction.duration(v) ? 0xffeeeeef : 0xffff9999));
    addRenderableWidget(new RoundedButton(px, y + 220, pw / 2 - 4, "Start / Stop", b -> {
      auction.toggle();
      rebuild();
    }));
    addRenderableWidget(
        new RoundedButton(px + pw / 2 + 4, y + 220, pw / 2 - 4, "Hide results", b -> {
          auction.hide();
          rebuild();
        }));
    addRenderableWidget(new ColourWheel(px, y + 272, pw, "Primary colour", auction::primary,
        c -> auction.colour(true, c), auction::save));
    addRenderableWidget(new ColourWheel(px, y + 374, pw, "Secondary colour", auction::secondary,
        c -> auction.colour(false, c), auction::save));
  }
  private void renderAuctionSettings(GuiGraphicsExtractor g) {
    var auction = dev.gamblingmenu.auction.AuctionModule.INSTANCE;
    int px = settingsX + 16, pw = settingsW - 32;
    Glass.panel(g, settingsX, y, settingsW, h, 26, true);
    Inter.draw(g, "Auction", px, y + 28, 0xffeeeeef, Inter.Weight.SEMIBOLD);
    var stack = auction.item();
    String title = stack.isEmpty() ? "No item selected"
                                   : stack.getHoverName().getString() + " ×" + stack.getCount();
    Inter.draw(g, Inter.editingFont().plainSubstrByWidth(title, pw), px, y + 105, 0xffa7abb5,
        Inter.Weight.MEDIUM);
    Inter.draw(g, "Minimum", px, y + 147, 0xffc9ccd6, Inter.Weight.MEDIUM);
    Inter.draw(g, "Seconds", px, y + 185, 0xffc9ccd6, Inter.Weight.MEDIUM);
    Glass.inset(g, px + 81, y + 137, pw - 81, 30, 8);
    Glass.inset(g, px + 81, y + 175, pw - 81, 30, 8);
    Inter.draw(g, "Primary colour", px, y + 255, 0xffeeeeef, Inter.Weight.MEDIUM);
    Inter.draw(g, "Secondary colour", px, y + 357, 0xffeeeeef, Inter.Weight.MEDIUM);
    if (h > 510)
      Inter.wrap(g, auction.status(), px, y + 470, pw, 0xffa7abb5);
  }
  public void openMultiplierSettings() {
    profitSettings = false;
    auctionSettings = false;
    europePage = true;
    if (settingsEditedAt != 0)
      applyPaymentSettings();
    multiplierSettings = true;
    blacklistSettings = false;
    settingsOpen = true;
    inspectorOpened = System.nanoTime();
    inspectorClosing = 0;
    rebuild();
  }
  private void buildMultiplierSettings() {
    int px = settingsX + 16, pw = settingsW - 32;
    addRenderableWidget(
        new RoundedButton(settingsX + settingsW - 42, y + 19, 26, "X", b -> closeInspector()));
    addRenderableWidget(new PaymentToggle(px, y + 50, pw, 2));
    for (int i = 0; i < 3; i++) {
      int slot = i;
      var input = addRenderableWidget(
          new AmountInput(px + 94, y + 188 + i * 46, pw - 104, "Multiplier slot " + (i + 1),
              dev.gamblingmenu.multiplier.MultiplierModule.INSTANCE.slot(i)));
      input.setMaxLength(8);
      input.setResponder(value
          -> input.setTextColor(
              dev.gamblingmenu.multiplier.MultiplierModule.INSTANCE.setSlot(slot, value)
                  ? 0xffeeeeef
                  : 0xffff9999));
    }
  }
  private void renderMultiplierSettings(GuiGraphicsExtractor g) {
    int px = settingsX + 16, pw = settingsW - 32;
    Glass.panel(g, settingsX, y, settingsW, h, 26, true);
    Inter.draw(g, "Payment Multipliers", px, y + 28, 0xffeeeeef, Inter.Weight.SEMIBOLD);
    g.fill(px, y + 86, px + pw, y + 87, 0x258e909b);
    Inter.wrap(g, "Add clickable payout tags to incoming payments.", px, y + 103, pw, 0xff989da8);
    Inter.draw(g, "MULTIPLIER SLOTS", px, y + 151, 0xff7e818b, Inter.Weight.MEDIUM);
    for (int i = 0; i < 3; i++) {
      Inter.draw(g, "Slot " + (i + 1), px, y + 188 + i * 46, 0xffc9ccd6, Inter.Weight.MEDIUM);
      Glass.inset(g, px + 84, y + 177 + i * 46, pw - 84, 32, 10);
    }
    Inter.wrap(g,
        "Use 2x, 4x or a decimal like 1.5x. Leave a slot blank to hide it. Changes save "
            + "automatically.",
        px, y + 325, pw, 0xff989da8);
    Inter.wrap(g,
        "Click one tag to pay the sender. One payout per receipt; tags expire after 5 minutes.", px,
        y + 395, pw, 0xff989da8);
  }
  private void buildPaymentSettings() {
    int px = settingsX + 16, pw = settingsW - 32;
    addRenderableWidget(
        new RoundedButton(settingsX + settingsW - 42, y + 19, 26, "X", b -> { closeInspector(); }));
    addRenderableWidget(new PaymentToggle(px, y + 50, pw));
    int fieldX = px + 94, fieldW = pw - 94;
    minInput = addRenderableWidget(
        new AmountInput(fieldX + 10, y + 188, fieldW - 20, "Minimum amount", minDraft));
    maxInput = addRenderableWidget(
        new AmountInput(fieldX + 10, y + 234, fieldW - 20, "Maximum amount", maxDraft));
    minInput.setResponder(value -> {
      minDraft = value;
      settingsEdited();
    });
    maxInput.setResponder(value -> {
      maxDraft = value;
      settingsEdited();
    });
    String mode =
        modeDraft == dev.gamblingmenu.payment.PaymentRules.Mode.DONATION ? "Donation" : "Pay back";
    addRenderableWidget(new RoundedButton(fieldX, y + 272, fieldW, mode + "  v", b -> {
      modeOpen = !modeOpen;
      rebuild();
    }));
    if (modeOpen) {
      addRenderableWidget(new RoundedButton(fieldX, y + 300, fieldW, "Donation", b -> {
        modeDraft = dev.gamblingmenu.payment.PaymentRules.Mode.DONATION;
        modeOpen = false;
        applyPaymentSettings();
        rebuild();
      }));
      addRenderableWidget(new RoundedButton(fieldX, y + 328, fieldW, "Pay back", b -> {
        modeDraft = dev.gamblingmenu.payment.PaymentRules.Mode.PAY_BACK;
        modeOpen = false;
        applyPaymentSettings();
        rebuild();
      }));
    }
  }
  private int blacklistRows() {
    return Math.max(1, (h - 294) / 30);
  }
  private void buildBlacklistSettings() {
    var blacklist = dev.gamblingmenu.blacklist.BlacklistModule.INSTANCE;
    int px = settingsX + 16, pw = settingsW - 32;
    addRenderableWidget(
        new RoundedButton(settingsX + settingsW - 42, y + 19, 26, "X", b -> closeInspector()));
    addRenderableWidget(new PaymentToggle(px, y + 50, pw, true));
    usernameInput =
        addRenderableWidget(new AmountInput(px + 10, y + 188, pw - 82, "Username", usernameDraft));
    usernameInput.setMaxLength(16);
    usernameInput.setResponder(value -> usernameDraft = value);
    addRenderableWidget(new RoundedButton(px + pw - 62, y + 181, 62, "Add", b -> {
      blacklistMessage = blacklist.add(usernameInput.getValue());
      if (blacklistMessage.startsWith("Added ")) {
        usernameDraft = "";
        usernameInput.setValue("");
        blacklistScroll = Math.max(0, blacklist.names().size() - blacklistRows());
      }
      rebuild();
    }));
    var names = blacklist.names();
    blacklistScroll = Math.clamp(blacklistScroll, 0, Math.max(0, names.size() - blacklistRows()));
    for (int row = 0; row < blacklistRows() && row + blacklistScroll < names.size(); row++) {
      String name = names.get(row + blacklistScroll);
      addRenderableWidget(new RoundedButton(px + pw - 33, y + 245 + row * 30, 30, "X", b -> {
        blacklist.remove(name);
        blacklistMessage = "";
        rebuild();
      }));
    }
  }
  private void renderBlacklistSettings(GuiGraphicsExtractor g) {
    int px = settingsX + 16, pw = settingsW - 32;
    var blacklist = dev.gamblingmenu.blacklist.BlacklistModule.INSTANCE;
    Glass.panel(g, settingsX, y, settingsW, h, 26, true);
    g.enableScissor(px, y + 16, settingsX + settingsW - 48, y + 46);
    g.pose().pushMatrix();
    g.pose().translate(px, y + 23);
    g.pose().scale(1.45f, 1.45f);
    Inter.draw(g, "Gamble Blacklist", 0, 0, 0xffeceef5, Inter.Weight.SEMIBOLD);
    g.pose().popMatrix();
    g.disableScissor();
    g.fill(px, y + 86, px + pw, y + 87, 0x258e909b);
    Inter.wrap(
        g, "Automatically return payments from listed players.", px, y + 103, pw, 0xff989da8);
    Inter.draw(g, "USERNAMES", px, y + 151, 0xff7e818b, Inter.Weight.MEDIUM);
    g.fill(px + 78, y + 156, px + pw, y + 157, 0x207f818a);
    Glass.inset(g, px, y + 177, pw - 72, 32, 10);
    String feedback = blacklist.error().isEmpty() ? blacklistMessage : blacklist.error();
    g.enableScissor(px, y + 213, px + pw, y + 240);
    Inter.wrap(g, feedback.isEmpty() ? "Enter a Minecraft username" : feedback, px, y + 216, pw,
        0xffa1a6b2);
    g.disableScissor();
    var names = blacklist.names();
    g.enableScissor(px, y + 242, px + pw, y + h - 48);
    if (names.isEmpty())
      Inter.draw(g, "No usernames added", px, y + 252, 0xff989da8, Inter.Weight.REGULAR);
    for (int row = 0; row < blacklistRows() && row + blacklistScroll < names.size(); row++) {
      Glass.inset(g, px, y + 243 + row * 30, pw - 40, 27, 7);
      Inter.draw(g, names.get(row + blacklistScroll), px + 10, y + 252 + row * 30, 0xffd5d7df,
          Inter.Weight.MEDIUM);
    }
    g.disableScissor();
    String footer = names.size() + " / 200 players"
        + (names.size() > blacklistRows() ? " · scroll to see more" : "");
    Inter.draw(g, footer, px, y + h - 31, 0xff80858f, Inter.Weight.REGULAR);
  }
  @Override
  public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
    double localX = mx / contentScale - inspectorOffset(),
           localY = my / contentScale - animationOffset();
    if (settingsOpen && profitSettings && localX >= settingsX && localX < settingsX + settingsW
        && localY >= y + 80 && localY < y + h) {
      profitScroll =
          Math.clamp(profitScroll - Math.round((float) vertical * 32), 0, Math.max(0, 648 - h));
      rebuild();
      return true;
    }
    if (settingsOpen && blacklistSettings && localX >= settingsX && localX < settingsX + settingsW
        && localY >= y + 242 && localY < y + h - 48) {
      blacklistScroll -= Math.round((float) vertical * 3);
      rebuild();
      return true;
    }
    return super.mouseScrolled(mx, my, horizontal, vertical);
  }
  private void renderPaymentSettings(GuiGraphicsExtractor g) {
    if (!settingsOpen)
      return;
    if (profitSettings) {
      renderProfitSettings(g);
      return;
    }
    if (auctionSettings) {
      renderAuctionSettings(g);
      return;
    }
    if (multiplierSettings) {
      renderMultiplierSettings(g);
      return;
    }
    if (blacklistSettings) {
      renderBlacklistSettings(g);
      return;
    }
    int px = settingsX + 16, pw = settingsW - 32, fieldX = px + 94, fieldW = pw - 94;
    Glass.panel(g, settingsX, y, settingsW, h, 26, true);
    g.enableScissor(px, y + 16, settingsX + settingsW - 48, y + 46);
    g.pose().pushMatrix();
    g.pose().translate(px, y + 23);
    g.pose().scale(1.6f, 1.6f);
    Inter.draw(g, "Payment Guard", 0, 0, 0xffeceef5, Inter.Weight.SEMIBOLD);
    g.pose().popMatrix();
    g.disableScissor();
    g.fill(px, y + 86, px + pw, y + 87, 0x258e909b);
    Inter.wrap(g, "Payment limits, refunds and donations", px, y + 103, pw, 0xff989da8);
    Inter.draw(g, "SETTINGS", px, y + 151, 0xff7e818b, Inter.Weight.MEDIUM);
    g.fill(px + 63, y + 156, px + pw, y + 157, 0x207f818a);
    Inter.draw(g, "Minimum", px, y + 188, 0xffc9ccd6, Inter.Weight.MEDIUM);
    Glass.inset(g, fieldX, y + 177, fieldW, 32, 10);
    Inter.draw(g, "Maximum", px, y + 234, 0xffc9ccd6, Inter.Weight.MEDIUM);
    Glass.inset(g, fieldX, y + 223, fieldW, 32, 10);
    Inter.draw(g, "Mode", px, y + 280, 0xffc9ccd6, Inter.Weight.MEDIUM);
    if (!modeOpen) {
      String help = modeDraft == dev.gamblingmenu.payment.PaymentRules.Mode.DONATION
          ? "Outside your limits: keep the payment and show a thank-you."
          : "Outside your limits: return the payment to its sender.";
      g.enableScissor(px, y + 311, px + pw, y + h - 24);
      Inter.wrap(g, help, px, y + 314, pw, 0xff989dab);
      Inter.draw(g, settingsEditedAt != 0 ? "Editing…" : "Changes save automatically", px, y + 366,
          0xff80858f, Inter.Weight.REGULAR);
      if (!settingsMessage.isEmpty())
        Inter.wrap(g, settingsMessage, px, y + 391, pw, 0xffe0b1ac);
      g.disableScissor();
    }
  }
  @Override
  public void tick() {
    if (inspectorClosing != 0
        && System.nanoTime() - inspectorClosing >= (GuiPreferences.duration * 750_000L)) {
      settingsOpen = false;
      modeOpen = false;
      inspectorClosing = 0;
      rebuild();
    }
    if (closing == 0 && settingsEditedAt != 0
        && System.nanoTime() - settingsEditedAt >= 600_000_000L)
      applyPaymentSettings();
    if (closing != 0
        && System.nanoTime() - closing
            >= (GuiPreferences.animations ? GuiPreferences.duration * 750_000L : 0))
      minecraft.setScreen(parent);
  }
  private float animationOffset() {
    if (!GuiPreferences.animations)
      return 0;
    long now = System.nanoTime();
    float progress = closing == 0
        ? Math.min(1, (now - opened) / (GuiPreferences.duration * 1_000_000f))
        : Math.max(0, 1 - (now - closing) / (GuiPreferences.duration * 750_000f));
    return (float) Math.pow(1 - progress, 3) * 14;
  }
  private net.minecraft.client.input.MouseButtonEvent localEvent(
      net.minecraft.client.input.MouseButtonEvent event) {
    double localX = event.x() / contentScale;
    if (settingsOpen && localX >= settingsX) {
      if (localX < settingsX + inspectorOffset())
        localX = -1000;
      else
        localX -= inspectorOffset();
    }
    return new net.minecraft.client.input.MouseButtonEvent(
        localX, event.y() / contentScale - animationOffset(), event.buttonInfo());
  }
  @Override
  public boolean mouseClicked(
      net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
    return closing != 0 || super.mouseClicked(localEvent(event), doubleClick);
  }
  @Override
  public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
    return closing != 0 || super.mouseReleased(localEvent(event));
  }
  @Override
  public boolean mouseDragged(
      net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
    return closing != 0 || super.mouseDragged(localEvent(event), dx, dy);
  }
  @Override
  public boolean isPauseScreen() {
    return false;
  }
  @Override
  public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float delta) {
    if (GuiPreferences.blur)
      extractBlurredBackground(g);
    g.fill(0, 0, width, height, (Math.round(GuiPreferences.dim * 2.55f) << 24) | 0x08080b);
  }
  @Override
  public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
    dev.gamblingmenu.auction.AuctionModule.INSTANCE.extractRenderState(g);
    dev.gamblingmenu.profit.ProfitTracker.INSTANCE.extractRenderState(g, false);
    dev.gamblingmenu.payment.PaymentModule.INSTANCE.renderNotification(g);
    float offset = animationOffset();
    float inspectorShift = inspectorOffset();
    g.pose().pushMatrix();
    g.pose().scale(contentScale, contentScale);
    g.pose().translate(0, offset);
    Glass.panel(g, x, y, mainW, h, 26, false);
    g.enableScissor(x + 1, y + 1, x + sidebar, y + h - 1);
    Glass.gradient(g, x + 1, y + 1, mainW - 2, h - 2, 25,
        (Math.round(24 * GuiPreferences.gloss / 100f) << 24) | 0xffffff, 0x02000000);
    g.disableScissor();
    g.fill(x + sidebar, y + 12, x + sidebar + 1, y + h - 12, 0x15d0d0d8);
    if (!guiSettings)
      Glass.inset(g, cx, y + 15, cw, 34, 11);
    g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
        net.minecraft.resources.Identifier.fromNamespaceAndPath(
            "discordlink", "textures/gui/accent-logo.png"),
        x + (sidebar - 72) / 2, y + h - 130, 0, 0, 72, 75, 1231, 1277, 1231, 1277,
        0xff000000 | GuiPreferences.accent);
    // Attribution uses the user's supplied face, bundled locally with no skin/account lookup.
    g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
        net.minecraft.resources.Identifier.fromNamespaceAndPath(
            "discordlink", "textures/gui/utud-face.png"),
        x + 15, y + 19, 0, 0, 22, 23, 26, 27, 26, 27, 0xffffffff);
    Inter.draw(g, "made by utud", x + 45, y + 26, 0xffe1e3ea, Inter.Weight.MEDIUM);
    Inter.draw(g, "FEATURES", x + 14, y + 64, 0xff868994, Inter.Weight.MEDIUM);
    if (!guiSettings) {
      icon(g, "search", cx + 9, y + 24, 14, 0xff969aa5);
      g.enableScissor(cx, y + 59, x + mainW - 12, y + h - 14);
      if (searchResults().isEmpty())
        Inter.draw(g, "No modules found", cx, y + 76, 0xff9397a4, Inter.Weight.REGULAR);
      g.disableScissor();
      g.pose().pushMatrix();
      g.pose().translate(inspectorShift, 0);
      renderPaymentSettings(g);
      g.pose().popMatrix();
    } else {
      renderGuiSettings(g);
    }
    // Widgets share the animation transform and clipping, including text cursor and selection.
    g.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);
    for (var child : children())
      if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget) {
        if (settingsOpen && profitSettings && widget.getMessage().getString().startsWith("Colours: ")) {
          String label="Colours: "+(dev.gamblingmenu.profit.ProfitTracker.INSTANCE.customColour?"Custom":"GUI theme");
          if (!widget.getMessage().getString().equals(label)) widget.setMessage(Inter.text(label,Inter.Weight.MEDIUM));
        }
        float shift = inspectorWidgets.contains(widget) ? inspectorShift : 0;
        g.pose().pushMatrix();
        g.pose().translate(shift, 0);
        widget.extractRenderState(g, Math.round(mx / contentScale - shift),
            Math.round(my / contentScale - offset), delta);
        g.pose().popMatrix();
      }
    g.disableScissor();
    g.pose().popMatrix();
  }
  public static void round(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int color) {
    if (w <= 0 || h <= 0)
      return;
    r = Math.max(0, Math.min(GuiPreferences.radius(r), Math.min(w, h) / 2));
    // A cached antialiased circle supplies four corners, without integer staircase edges.
    g.fill(x + r, y, x + w - r, y + h, color);
    g.fill(x, y + r, x + r, y + h - r, color);
    g.fill(x + w - r, y + r, x + w, y + h - r, color);
    if (r == 0)
      return;
    var texture = net.minecraft.resources.Identifier.fromNamespaceAndPath(
        "discordlink", "textures/gui/circle.png");
    for (int corner = 0; corner < 4; corner++) {
      boolean right = (corner & 1) != 0, bottom = (corner & 2) != 0;
      g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture,
          right ? x + w - r : x, bottom ? y + h - r : y, right ? 64 : 0, bottom ? 64 : 0, r, r, 64,
          64, 128, 128, color);
    }
  }
  public static void icon(GuiGraphicsExtractor g, String name, int x, int y, int size, int color) {
    g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
        net.minecraft.resources.Identifier.fromNamespaceAndPath(
            "discordlink", "textures/gui/" + name + ".png"),
        x, y, 0, 0, size, size, 128, 128, 128, 128, color);
  }
}
