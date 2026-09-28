package dev.villagernews.runtime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
class MolangTest {
 @Test void mathAndControlFlow(){var c=new Molang.Context();assertEquals(1d,Molang.eval("math.sin(90)",c));assertEquals(3d,Molang.eval("v.a=0;loop(3,{v.a=v.a+1;});return v.a;",c));assertEquals(7d,Molang.eval("v.a==3?{return 7;}: {return 0;};",c));assertEquals(8d,Molang.eval("v.missing??8",c));assertEquals(1d,Molang.eval("'head'=='head'",c));assertEquals(0d,Molang.eval("0&&(v.a=9)",c));assertEquals(3d,c.number("v.a"));}
 @Test void testEntityTextureEvaluation() {
  var c = new Molang.Context();
  c.set("texture.xjxkmv", "textures/oreville/vn/dja");
  c.set("texture.twuqoi", "textures/oreville/vn/djc");
  String expr = "v.wycgfr?{return v.ncfcpo?Texture.trptiw:Texture.mkphiu;};v.mtbwfe?{return v.ncfcpo?Texture.afvigz:Texture.kpsivs;};return v.ncfcpo?Texture.twuqoi:Texture.xjxkmv;";
  Object res = Molang.eval(expr, c);
  assertEquals("textures/oreville/vn/dja", res);

  var c2 = new Molang.Context();
  c2.set("array.akvyuz", List.of("textures/oreville/vn/dil", "textures/oreville/vn/dim"));
  c2.queries = (q, a) -> 0d; // skin_id = 0
  String exprVillager = "v.wycgfr?{return v.ncfcpo?Array.tlpvid[v.szshwk]:Array.ssbolp[v.szshwk];};v.mtbwfe?{return v.ncfcpo?Array.prwiov[v.szshwk]:Array.qeqoqs[v.szshwk];};return v.ncfcpo?Array.iwnjgl[q.skin_id]:Array.akvyuz[q.skin_id];";
  Object resVillager = Molang.eval(exprVillager, c2);
  assertEquals("textures/oreville/vn/dil", resVillager);
 }
 @Test void everyOriginalClientExpressionParses() throws Exception {
  try(var in=getClass().getResourceAsStream("/assets/oreville_vn/port/bedrock.json")){assertNotNull(in);var root=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();List<String> failed=new ArrayList<>();Set<String> expressions=new HashSet<>();for(var e:root.entrySet())if(!e.getKey().equals("behaviors"))collect(e.getValue(),expressions);for(String s:expressions)try{Molang.compile(s);}catch(Exception ex){failed.add(ex.toString());}assertTrue(failed.isEmpty(),String.join("\n",failed));assertTrue(expressions.size()>1000);}
 }
 void collect(JsonElement e,Set<String> out){if(e.isJsonArray())e.getAsJsonArray().forEach(x->collect(x,out));else if(e.isJsonObject())e.getAsJsonObject().entrySet().forEach(x->collect(x.getValue(),out));else if(e.isJsonPrimitive()&&e.getAsJsonPrimitive().isString()){String s=e.getAsString();if(s.toLowerCase(Locale.ROOT).matches("(?s).*\\b(v|q|t|math|variable|query|temp|c)\\..*"))out.add(s);}}
}
