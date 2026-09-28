package dev.villagernews;
import net.minecraftforge.common.ForgeConfigSpec;
public final class NewsConfig {
 public static final ForgeConfigSpec SERVER,CLIENT;
 public static final ForgeConfigSpec.EnumValue<Chattiness> CHATTINESS;
 public static final ForgeConfigSpec.EnumValue<RareLines> RARE_LINES;
 public static final ForgeConfigSpec.BooleanValue SUBTITLES;
 public static final ForgeConfigSpec.IntValue STYLE;
 public enum Chattiness {OFF,REDUCED,NORMAL,CHATTY}
 public enum RareLines {NEVER,NORMAL,FREQUENT}
 static {var b=new ForgeConfigSpec.Builder();CHATTINESS=b.defineEnum("chattiness",Chattiness.NORMAL);RARE_LINES=b.defineEnum("rareLines",RareLines.NORMAL);SERVER=b.build();b=new ForgeConfigSpec.Builder();SUBTITLES=b.define("subtitles",true);STYLE=b.comment("0: Vanilla, 1: Actions & Stuff, 2: Flat").defineInRange("style",0,0,2);CLIENT=b.build();}
}
