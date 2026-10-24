package dev.creationcore.check;

import com.google.gson.JsonParser;
import dev.creationcore.compat.jei.*;
import java.nio.file.*;
import java.util.*;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.ResourceLocation;

/** Opens only the copied developer world and checks real JEI frame bounds at scales 8 and 4. */
public final class ClientStartupCheck {
  private static int phase, ticks, index, wait;
  private static boolean done;
  private static IJeiRuntime runtime;
  private static CoreInfoJeiCategory category;
  private static final List<String> result = new ArrayList<>();

  private static void check(boolean ok, String label) {
    if (!ok) throw new AssertionError(label);
    result.add("PASS: " + label);
  }

  public static void tick(Minecraft ignored) {
    if (done) return;
    var client = Minecraft.getInstance();
    if (++ticks > 3600) {
      finish(
          client, new AssertionError("timed out at phase " + phase + " screen " + client.screen));
      return;
    }
    try {
      if (ticks % 100 == 0)
        System.out.println("CLIENT_CHECK phase=" + phase + " screen=" + client.screen);
      if (phase == 0) {
        if (client.screen != null
            && client.getOverlay() == null
            && !(client.screen instanceof TitleScreen)) client.setScreen(new TitleScreen());
        if (!(client.screen instanceof TitleScreen) || client.getOverlay() != null) return;
        client.getWindow().setWindowed(1920, 1080);
        client.options.guiScale().set(4);
        client.resizeDisplay();
        phase = 1;
        client
            .createWorldOpenFlows()
            .openWorld(
                "jei-layout-test",
                () -> finish(client, new AssertionError("world load cancelled")));
        return;
      }
      if (phase == 1) {
        if (client.level == null
            || client.player == null
            || client.getOverlay() != null
            || client.screen != null) return;
        if (++wait < 20) return;
        Optional<?> optional;
        try {
          optional =
              Optional.ofNullable(
                  Class.forName("mezz.jei.common.Internal")
                      .getMethod("getJeiRuntime")
                      .invoke(null));
        } catch (java.lang.reflect.InvocationTargetException notReady) {
          return;
        }
        if (optional.isEmpty()) return;
        runtime = (IJeiRuntime) optional.get();
        category =
            (CoreInfoJeiCategory)
                runtime.getRecipeManager().getRecipeCategory(CreationCoreJeiPlugin.INFO);
        check(
            category != null,
            "real JEI registered introduction category in loaded developer world");
        check(
            runtime.getRecipeManager().createRecipeLookup(CreationCoreJeiPlugin.INFO).get().count()
                == 9,
            "JEI has all nine introduction pages");
        check(
            runtime
                    .getRecipeManager()
                    .createRecipeLookup(CreationCoreJeiPlugin.CRAFTING)
                    .get()
                    .count()
                == client
                    .level
                    .getRecipeManager()
                    .getAllRecipesFor(
                        dev.creationcore.registry.ModRecipes.CREATIVE_CRAFTING_TYPE.get())
                    .size(),
            "JEI lists every loaded creative recipe");
        check(
            client.getWindow().getGuiScale() == 4,
            "physical 1920x1080 client uses actual GUI scale 4");
        check(
            Arrays.stream(CoreInfoJeiCategory.class.getDeclaredMethods())
                .noneMatch(m -> m.getName().equals("createRecipeExtras")),
            "introduction has reverted to plain text without scroll widgets");
        phase = 2;
        wait = 0;
        return;
      }
      if (phase == 2) {
        if (index >= 36) {
          finish(client, null);
          return;
        }
        int scale = index < 18 ? 4 : 2;
        if (index == 18) {
          client.options.guiScale().set(scale);
          client.resizeDisplay();
        }
        var page = CoreInfoPage.pages().get(index % 9);
        boolean english = (index / 9) % 2 == 1;
        try (var reader =
            client
                .getResourceManager()
                .getResource(
                    ResourceLocation.parse(
                        "creationcore:lang/" + (english ? "en_us" : "zh_cn") + ".json"))
                .orElseThrow()
                .openAsReader()) {
          String text =
              JsonParser.parseReader(reader)
                  .getAsJsonObject()
                  .get(page.translationKey())
                  .getAsString();
          check(
              !text.contains("\n") && !text.contains("\\n"),
              (english ? "English" : "Chinese")
                  + " introduction "
                  + page.index()
                  + " has no explicit newline");
          page = new CoreInfoPage(page.index(), text, page.subject(), page.previous());
        }
        var method = CoreInfoJeiCategory.class.getDeclaredMethod("description", CoreInfoPage.class);
        method.setAccessible(true);
        var layout = method.invoke(category, page);
        var scaleMethod = layout.getClass().getDeclaredMethod("scale");
        scaleMethod.setAccessible(true);
        var linesMethod = layout.getClass().getDeclaredMethod("lines");
        linesMethod.setAccessible(true);
        float textScale = (Float) scaleMethod.invoke(layout);
        int lines = ((List<?>) linesMethod.invoke(layout)).size();
        check(
            44 + lines * (client.font.lineHeight + 2) * textScale <= category.getHeight() - 4,
            "complete "
                + (english ? "English" : "Chinese")
                + " page "
                + page.index()
                + " text fits at GUI scale "
                + scale);
        runtime.getRecipesGui().showRecipes(category, List.of(page), List.of());
        phase = 3;
        wait = 0;
        return;
      }
      if (++wait < 8) return;
      var gui = runtime.getRecipesGui();
      Object area = gui.getClass().getMethod("getArea").invoke(gui);
      int y = (Integer) area.getClass().getMethod("getY").invoke(area);
      int height = (Integer) area.getClass().getMethod("getHeight").invoke(area);
      check(
          client.screen == gui && y >= 0 && y + height <= client.getWindow().getGuiScaledHeight(),
          "actual JEI frame for case " + index + " remains within screen bottom");
      if (index == 0 || index == 5 || index == 14 || index == 23) {
        try (var shot = Screenshot.takeScreenshot(client.getMainRenderTarget())) {
          shot.writeToFile(Path.of("../build/jei-layout-case-" + index + ".png"));
        }
      }
      index++;
      phase = 2;
    } catch (Throwable failure) {
      finish(client, failure);
    }
  }

  private static void finish(Minecraft client, Throwable failure) {
    done = true;
    if (failure != null) {
      failure.printStackTrace();
      result.add("FAIL: " + failure);
    } else result.add("ALL JEI LAYOUT CHECKS PASSED");
    try {
      Files.write(Path.of("../build/jei-layout-checks.txt"), result);
    } catch (Exception problem) {
      throw new RuntimeException(problem);
    }
    System.out.println(result.getLast());
    client.stop();
  }
}
