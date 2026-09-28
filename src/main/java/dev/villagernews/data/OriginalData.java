package dev.villagernews.data;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class OriginalData {
 public static final JsonObject BEDROCK=load("port/bedrock.json");
 public static final JsonObject DIALOGUES=load("port/dialogues.json");
 public static final JsonObject GUIDE=load("port/guide.json");
 public static final JsonObject GUIDE_PT=loadOptional("port/guide_pt_br.json");
 public static final Map<String,JsonObject> GROUPS=new LinkedHashMap<>();
 public static final Map<String,JsonObject> LINES=new LinkedHashMap<>();
 public static final JsonObject SOUNDS=load("sounds.json");
 static {for(var entry:DIALOGUES.entrySet()){var g=entry.getValue().getAsJsonObject();GROUPS.put(entry.getKey(),g);String id=string(g,"id","");if(!id.isEmpty())GROUPS.put(id,g);for(var e:g.getAsJsonArray("slhkqn")){var line=e.getAsJsonObject();LINES.putIfAbsent(string(line,"animationName",""),line);}}}
 public static JsonObject load(String path){try(var in=OriginalData.class.getResourceAsStream("/assets/oreville_vn/"+path)){if(in==null)throw new IllegalStateException("Missing "+path);return JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();}catch(IOException e){throw new UncheckedIOException(e);}}
 public static JsonObject loadOptional(String path){try(var in=OriginalData.class.getResourceAsStream("/assets/oreville_vn/"+path)){if(in==null)return null;return JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();}catch(Exception e){return null;}}
 public static JsonObject section(String name){return BEDROCK.getAsJsonObject(name);}
 public static JsonObject object(JsonObject o,String name){return o!=null&&o.has(name)&&o.get(name).isJsonObject()?o.getAsJsonObject(name):new JsonObject();}
 public static JsonArray array(JsonObject o,String name){return o!=null&&o.has(name)&&o.get(name).isJsonArray()?o.getAsJsonArray(name):new JsonArray();}
 public static String string(JsonObject o,String name,String fallback){return o!=null&&o.has(name)&&o.get(name).isJsonPrimitive()?o.get(name).getAsString():fallback;}
 public static double number(JsonObject o,String name,double fallback){return o!=null&&o.has(name)&&o.get(name).isJsonPrimitive()&&o.getAsJsonPrimitive(name).isNumber()?o.get(name).getAsDouble():fallback;}
 public static boolean bool(JsonObject o,String name,boolean fallback){return o!=null&&o.has(name)&&o.get(name).isJsonPrimitive()&&o.getAsJsonPrimitive(name).isBoolean()?o.get(name).getAsBoolean():fallback;}
 private OriginalData(){}
}
