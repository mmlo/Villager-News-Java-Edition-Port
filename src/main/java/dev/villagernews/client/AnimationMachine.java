package dev.villagernews.client;
import com.google.gson.*;
import dev.villagernews.data.OriginalData;
import dev.villagernews.runtime.Molang;
import java.util.*;
import static dev.villagernews.data.OriginalData.*;

/** Evaluates the original controller graphs and keyframes in entity-local state. */
public final class AnimationMachine {
 public static final class Transform {
  public final double[] position={0,0,0},rotation={0,0,0},scale={1,1,1};
  public boolean worldRotation;
  public Transform copy(){Transform t=new Transform();System.arraycopy(position,0,t.position,0,3);System.arraycopy(rotation,0,t.rotation,0,3);System.arraycopy(scale,0,t.scale,0,3);t.worldRotation=worldRotation;return t;}
 }
 private record TimelineEntry(double time, JsonElement action) {}
 private static final Map<JsonObject, NavigableMap<Double, JsonElement>> KEYFRAME_CACHE = Collections.synchronizedMap(new IdentityHashMap<>());
 private static final Map<JsonObject, TimelineEntry[]> TIMELINE_CACHE = Collections.synchronizedMap(new IdentityHashMap<>());
 private static final String[] CHANNELS = {"position", "rotation", "scale"};

 private static NavigableMap<Double, JsonElement> getKeyframes(JsonObject frames) {
  NavigableMap<Double, JsonElement> map = KEYFRAME_CACHE.get(frames);
  if (map != null) return map;
  synchronized (KEYFRAME_CACHE) {
   map = KEYFRAME_CACHE.get(frames);
   if (map == null) {
    TreeMap<Double, JsonElement> tree = new TreeMap<>();
    for (var en : frames.entrySet()) {
     try { tree.put(Double.parseDouble(en.getKey()), en.getValue()); } catch (NumberFormatException ignored) {}
    }
    map = tree;
    KEYFRAME_CACHE.put(frames, map);
   }
   return map;
  }
 }

 private static TimelineEntry[] getTimeline(JsonObject timeline) {
  TimelineEntry[] entries = TIMELINE_CACHE.get(timeline);
  if (entries != null) return entries;
  synchronized (TIMELINE_CACHE) {
   entries = TIMELINE_CACHE.get(timeline);
   if (entries == null) {
    List<TimelineEntry> list = new ArrayList<>();
    for (var e : timeline.entrySet()) {
     try { list.add(new TimelineEntry(Double.parseDouble(e.getKey()), e.getValue())); } catch (NumberFormatException ignored) {}
    }
    list.sort(Comparator.comparingDouble(TimelineEntry::time));
    entries = list.toArray(TimelineEntry[]::new);
    TIMELINE_CACHE.put(timeline, entries);
   }
   return entries;
  }
 }

 private static final class Track {double time,last=-1,seen;}
 private static final class Controller {String state,oldState;double start,changed,oldStart;}
 public final Molang.Context context=new Molang.Context();
 public final Map<String,Transform> pose=new HashMap<>();
 final Map<String,Track> tracks=new HashMap<>();
 final Map<String,Controller> controllers=new HashMap<>();
 final JsonObject description;
 double clock,delta,lastClock=-1;boolean initialized;
 public AnimationMachine(JsonObject description){this.description=description;}
 public Object evaluate(JsonElement v){if(v==null||v.isJsonNull())return 0d;if(v.isJsonPrimitive()){var p=v.getAsJsonPrimitive();if(p.isNumber())return p.getAsDouble();if(p.isBoolean())return p.getAsBoolean()?1d:0d;return Molang.eval(p.getAsString(),context);}return 0d;}
 public double number(JsonElement v){return Molang.num(evaluate(v));}
 public void statements(JsonArray array){for(JsonElement s:array)evaluate(s);}
 public void frame(double time,String speech,double speechTime){
  clock=time;delta=lastClock<0?1d/20:Math.min(.1,Math.max(0,time-lastClock));lastClock=time;
  context.set("q.delta_time",delta);context.vars.keySet().removeIf(k->k.startsWith("t."));pose.clear();
  if(!initialized){statements(array(object(description,"scripts"),"initialize"));initialized=true;}
  statements(array(object(description,"scripts"),"pre_animation"));
  // Lip variables must be available before the original mouth and gesture controllers run.
  if(speech!=null&&!speech.isEmpty())speech(speech,speechTime);
  else {context.set("v.invysa",0d);context.set("v.cfulfb",1d);context.set("v.ziisoq",1d);context.set("v.skwjdr",0d);}
  animateList(array(object(description,"scripts"),"animate"),1,"root",0);
  tracks.entrySet().removeIf(e->e.getValue().seen<clock-1);
 }
 private void speech(String name,double time){
  var a=object(section("animations"),name);context.set("q.anim_time",time);
  JsonObject tl=object(a,"timeline");if(tl.size()==0)return;
  for(TimelineEntry te:getTimeline(tl))if(te.time<=time){if(te.action.isJsonArray())statements(te.action.getAsJsonArray());else evaluate(te.action);}
 }
 private void animateList(JsonArray list,double weight,String path,int depth){if(depth>16)return;for(JsonElement v:list){if(v.isJsonPrimitive())animate(v.getAsString(),weight,path,depth+1);else for(var e:v.getAsJsonObject().entrySet()){double w=number(e.getValue());if(w!=0)animate(e.getKey(),weight*w,path,depth+1);}}}
 private void animate(String alias,double weight,String path,int depth){String name=string(object(description,"animations"),alias,alias);if(name.startsWith("controller."))controller(name,weight,path,depth);else animation(name,weight,path);}
 private void controller(String name,double weight,String path,int depth){JsonObject definition=object(section("controllers"),name);if(definition.size()==0)return;String key=path+"/"+name;Controller c=controllers.computeIfAbsent(key,k->{Controller x=new Controller();x.state=string(definition,"initial_state","default");x.start=clock;return x;});JsonObject state=object(object(definition,"states"),c.state);
  context.set("q.state_time",clock-c.start);context.set("q.any_animation_finished",0d);context.set("q.all_animations_finished",1d);
  if(c.oldState!=null){double blend=OriginalData.number(object(object(definition,"states"),c.oldState),"blend_transition",0);double progress=blend==0?1:Math.min(1,(clock-c.changed)/blend);if(progress<1)animateList(array(object(object(definition,"states"),c.oldState),"animations"),weight*(1-progress),key+"/"+c.oldState,depth);else c.oldState=null;animateList(array(state,"animations"),weight*progress,key+"/"+c.state,depth);}else animateList(array(state,"animations"),weight,key+"/"+c.state,depth);
  for(JsonElement t:array(state,"transitions"))for(var e:t.getAsJsonObject().entrySet())if(Molang.truth(evaluate(e.getValue()))){statements(array(state,"on_exit"));c.oldState=c.state;c.oldStart=c.start;c.changed=clock;c.start=clock;c.state=e.getKey();String prefix=key+"/"+c.state;tracks.keySet().removeIf(k->k.startsWith(prefix));statements(array(object(object(definition,"states"),c.state),"on_entry"));return;}
 }
 private void animation(String name,double weight,String path){if("animation.oreville_vn.trsqws".equals(name))return;JsonObject a=object(section("animations"),name);if(a.size()==0)return;String key=path+"/"+name;Track t=tracks.computeIfAbsent(key,k->new Track());if(t.seen<clock-.2)t.time=0;else t.time+=delta;t.seen=clock;
  context.set("q.anim_time",t.time);if(a.has("anim_time_update"))t.time=number(a.get("anim_time_update"));double length=OriginalData.number(a,"animation_length",0);boolean loop=bool(a,"loop",false);double time=length>0?(loop?t.time%length:Math.min(t.time,length)):t.time;
  boolean finished=length>0&&t.time>=length;context.set("q.any_animation_finished",Math.max(context.number("q.any_animation_finished"),finished?1:0));if(!finished)context.set("q.all_animations_finished",0d);context.set("q.anim_time",time);
  if(finished&&!loop&&!string(a,"loop","").equals("hold_on_last_frame"))return;
  JsonObject tl=object(a,"timeline");
  if(tl.size()>0){if(time<t.last)t.last=-1;for(TimelineEntry te:getTimeline(tl)){if(te.time>t.last&&te.time<=time){if(te.action.isJsonArray())statements(te.action.getAsJsonArray());else evaluate(te.action);}}t.last=time;}
  double w=weight*(a.has("blend_weight")?number(a.get("blend_weight")):1);boolean override=bool(a,"override_previous_animation",false);
  for(var e:object(a,"bones").entrySet()) {Transform p=pose.computeIfAbsent(e.getKey(),k->new Transform());JsonObject bone=e.getValue().getAsJsonObject();if(override){Arrays.fill(p.position,0);Arrays.fill(p.rotation,0);Arrays.fill(p.scale,1);}for(String channel:CHANNELS){if(!bone.has(channel))continue;double[] target=channel.equals("position")?p.position:channel.equals("rotation")?p.rotation:p.scale;double[] sample=sample(bone.get(channel),time,target);for(int i=0;i<3;i++)target[i]+=w*(sample[i]-(channel.equals("scale")?1:0));}if(object(bone,"relative_to").has("rotation"))p.worldRotation=true;}
 }
 private double[] sample(JsonElement e,double time,double[] current){if(!e.isJsonObject())return vector(e,current);JsonObject frames=e.getAsJsonObject();if(frames.has("vector"))return vector(frames.get("vector"),current);
  NavigableMap<Double,JsonElement> keys=getKeyframes(frames);
  if(keys.isEmpty())return vector(e,current);var lo=keys.floorEntry(time);var hi=keys.ceilingEntry(time);if(lo==null)return endpoint(keys.firstEntry().getValue(),"pre",current);if(hi==null)return endpoint(keys.lastEntry().getValue(),"post",current);double[] a=endpoint(lo.getValue(),"post",current);if(lo.getKey().equals(hi.getKey()))return a;double[] b=endpoint(hi.getValue(),"pre",current);double f=(time-lo.getKey())/(hi.getKey()-lo.getKey());boolean cat=hi.getValue().isJsonObject()&&string(hi.getValue().getAsJsonObject(),"lerp_mode","").equals("catmullrom");
  double[] prev=a,next=b;if(cat){var before=keys.lowerEntry(lo.getKey());var after=keys.higherEntry(hi.getKey());if(before!=null)prev=endpoint(before.getValue(),"post",current);if(after!=null)next=endpoint(after.getValue(),"pre",current);}double[] out=new double[3];for(int i=0;i<3;i++)out[i]=cat?.5*((2*a[i])+(-prev[i]+b[i])*f+(2*prev[i]-5*a[i]+4*b[i]-next[i])*f*f+(-prev[i]+3*a[i]-3*b[i]+next[i])*f*f*f):a[i]+(b[i]-a[i])*f;return out;
 }
 private double[] endpoint(JsonElement e,String side,double[] current){if(e.isJsonObject()){JsonObject o=e.getAsJsonObject();e=o.has(side)?o.get(side):o.has("vector")?o.get("vector"):o.has("post")?o.get("post"):o.get("pre");}return vector(e,current);}
 private double[] vector(JsonElement e,double[] current){double[] out=new double[3];for(int i=0;i<3;i++){context.set("this",current[i]);out[i]=number(e!=null&&e.isJsonArray()?e.getAsJsonArray().get(i):e);}return out;}
}
