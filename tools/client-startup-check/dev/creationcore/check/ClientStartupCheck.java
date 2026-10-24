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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opens only the copied developer world and checks real JEI frame bounds at scales 8 and 4. */
@EventBusSubscriber(modid="creationcore",value=Dist.CLIENT)
public final class ClientStartupCheck {
    private static int phase,ticks,index,wait;
    private static boolean done;
    private static IJeiRuntime runtime;
    private static CoreInfoJeiCategory category;
    private static final List<String> result=new ArrayList<>();
    private static void check(boolean ok,String label) { if(!ok)throw new AssertionError(label);result.add("PASS: "+label); }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(done)return;
        var client=Minecraft.getInstance();
        if(++ticks>3600) { finish(client,new AssertionError("timed out at phase "+phase+" screen "+client.screen));return; }
        try {
            if(phase==0) {
                if(!(client.screen instanceof TitleScreen)||client.getOverlay()!=null)return;
                client.getWindow().setWindowed(3840,2058);
                client.options.guiScale().set(8);client.resizeDisplay();phase=1;
                client.createWorldOpenFlows().openWorld("jei-layout-test",()->finish(client,new AssertionError("world load cancelled")));
                return;
            }
            if(phase==1) {
                if(client.level==null||client.player==null||client.getOverlay()!=null||client.screen!=null)return;
                if(++wait<20)return;
                var optional=(Optional<?>)Class.forName("mezz.jei.common.Internal").getMethod("getOptionalJeiRuntime").invoke(null);
                if(optional.isEmpty())return;
                runtime=(IJeiRuntime)optional.get();
                category=(CoreInfoJeiCategory)runtime.getRecipeManager().getRecipeCategory(CreationCoreJeiPlugin.INFO);
                check(category!=null,"real JEI registered introduction category in loaded developer world");
                check(client.getWindow().getGuiScale()==8,"physical 3840x2058 client uses actual GUI scale 8");
                check(Arrays.stream(CoreInfoJeiCategory.class.getDeclaredMethods()).noneMatch(m->m.getName().equals("createRecipeExtras")),
                        "introduction has reverted to plain text without scroll widgets");
                phase=2;wait=0;return;
            }
            if(phase==2) {
                if(index>=36) { finish(client,null);return; }
                int scale=index<18?8:4;
                if(index==18) { client.options.guiScale().set(scale);client.resizeDisplay(); }
                var page=CoreInfoPage.pages().get(index%9);
                boolean english=(index/9)%2==1;
                if(english) {
                    try(var reader=client.getResourceManager().getResource(ResourceLocation.parse("creationcore:lang/en_us.json")).orElseThrow().openAsReader()) {
                        String text=JsonParser.parseReader(reader).getAsJsonObject().get(page.translationKey()).getAsString();
                        check(!text.contains("\n")&&!text.contains("\\n"),"English introduction "+page.index()+" has no explicit newline");
                        page=new CoreInfoPage(page.index(),text,page.subject(),page.previous());
                    }
                } else {
                    String text=net.minecraft.network.chat.Component.translatable(page.translationKey()).getString();
                    check(!text.contains("\n")&&!text.contains("\\n"),"Chinese introduction "+page.index()+" has no explicit newline");
                    if(page.index()==6||page.index()==7)check(!text.contains("AE2")&&!text.contains("汤姆"),"workstation introduction "+page.index()+" omits compatibility text");
                }
                var method=CoreInfoJeiCategory.class.getDeclaredMethod("description",CoreInfoPage.class);method.setAccessible(true);
                var layout=method.invoke(category,page);
                var scaleMethod=layout.getClass().getDeclaredMethod("scale");scaleMethod.setAccessible(true);
                var linesMethod=layout.getClass().getDeclaredMethod("lines");linesMethod.setAccessible(true);
                float textScale=(Float)scaleMethod.invoke(layout);
                int lines=((List<?>)linesMethod.invoke(layout)).size();
                check(44+lines*(client.font.lineHeight+2)*textScale<=category.getHeight()-4,
                        "complete "+(english?"English":"Chinese")+" page "+page.index()+" text fits at GUI scale "+scale);
                runtime.getRecipesGui().showRecipes(category,List.of(page),List.of());phase=3;wait=0;return;
            }
            if(++wait<8)return;
            var gui=runtime.getRecipesGui();
            Object area=gui.getClass().getMethod("getArea").invoke(gui);
            int y=(Integer)area.getClass().getMethod("getY").invoke(area);
            int height=(Integer)area.getClass().getMethod("getHeight").invoke(area);
            check(client.screen==gui&&y>=0&&y+height<=client.getWindow().getGuiScaledHeight(),
                    "actual JEI frame for case "+index+" remains within screen bottom");
            if(index==0||index==5||index==14||index==23) {
                try(var shot=Screenshot.takeScreenshot(client.getMainRenderTarget())) {
                    shot.writeToFile(Path.of("../build/jei-layout-case-"+index+".png"));
                }
            }
            index++;phase=2;
        } catch(Throwable failure) { finish(client,failure); }
    }
    private static void finish(Minecraft client,Throwable failure) {
        done=true;
        if(failure!=null) { failure.printStackTrace();result.add("FAIL: "+failure); }
        else result.add("ALL JEI LAYOUT CHECKS PASSED");
        try { Files.write(Path.of("../build/jei-layout-checks.txt"),result); }
        catch(Exception problem) { throw new RuntimeException(problem); }
        System.out.println(result.getLast());client.stop();
    }
}
