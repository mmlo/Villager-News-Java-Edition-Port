package dev.villagernews.runtime;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/** A per-entity Molang interpreter. No JavaScript or host code is executed. */
public final class Molang {
    public interface Expr { Object eval(Context c); }
    private static final List<Object> NO_ARGS = Collections.emptyList();
    public static final class Context {
        public final Map<String,Object> vars = new HashMap<>();
        public BiFunction<String,List<Object>,Object> queries = (s,a)->0d;
        public final Random random = new Random();
        public Object get(String key) {
            key=canonical(key);
            Object v = vars.get(key);
            if (v != null) return v;
            int dot=key.lastIndexOf('.');
            if(dot>0) {
                Object parent=vars.get(key.substring(0,dot));
                if(parent instanceof Map<?,?> m) return m.get(key.substring(dot+1));
            }
            if(key.startsWith("q."))return queries.apply(key.substring(2),NO_ARGS);
            if(key.equals("true"))return 1d;
            if(key.equals("false"))return 0d;
            return null;
        }
        public void set(String key,Object v) {vars.put(canonical(key),v);}
        public double number(String key) {return num(get(key));}
        public Object call(String key,List<Object> args) {
            key=canonical(key);
            if(key.startsWith("q."))return queries.apply(key.substring(2),args);
            if(key.startsWith("math."))return math(key.substring(5),args,random);
            throw new IllegalArgumentException("Unsupported Molang function: "+key);
        }
    }
    public static double num(Object o) {if(o instanceof Number n)return n.doubleValue();if(o instanceof Boolean b)return b?1:0;return 0;}
    public static boolean truth(Object o){return o instanceof String s?!s.isEmpty():num(o)!=0;}
    public static String str(Object o){return o==null?"":o instanceof Number n&&n.doubleValue()==n.longValue()?Long.toString(n.longValue()):o.toString();}
    static String canonical(String s){
        if(s==null||s.isEmpty())return "";
        char c=s.charAt(0);
        if(s.length()>=2&&s.charAt(1)=='.'&&(c=='v'||c=='q'||c=='t'||c=='c'))return s;
        if(s.regionMatches(true,0,"variable.",0,9))return "v."+s.substring(9).toLowerCase(Locale.ROOT);
        if(s.regionMatches(true,0,"query.",0,6))return "q."+s.substring(6).toLowerCase(Locale.ROOT);
        if(s.regionMatches(true,0,"temp.",0,5))return "t."+s.substring(5).toLowerCase(Locale.ROOT);
        if(s.regionMatches(true,0,"context.",0,8))return "c."+s.substring(8).toLowerCase(Locale.ROOT);
        return s.toLowerCase(Locale.ROOT);
    }
    private static final Map<String,Expr> CACHE=new ConcurrentHashMap<>();
    public static Expr compile(String s){return CACHE.computeIfAbsent(s,k->new Parser(k).program());}
    public static Object eval(String s,Context c){try{return compile(s).eval(c);}catch(Return r){return r.value;}}
    private static final class Return extends RuntimeException {final Object value;Return(Object v){super(null,null,false,false);value=v;}}
    private record Var(String name) implements Expr {public Object eval(Context c){return c.get(name);}}
    private record Const(Object value) implements Expr {public Object eval(Context c){return value;}}
    private record Token(String text,boolean string){}
    private static final class Parser {
        final List<Token> tokens=new ArrayList<>();int at;final String source;
        Parser(String s){source=s;for(int i=0;i<s.length();) {
            char ch=s.charAt(i);if(Character.isWhitespace(ch)){i++;continue;}
            if(ch=='\''||ch=='"'){char quote=ch;StringBuilder b=new StringBuilder();i++;while(i<s.length()&&s.charAt(i)!=quote){if(s.charAt(i)=='\\'&&i+1<s.length())i++;b.append(s.charAt(i++));}if(i>=s.length())throw error("unclosed string");i++;tokens.add(new Token(b.toString(),true));continue;}
            int start=i;
            if(Character.isDigit(ch)||(ch=='.'&&i+1<s.length()&&Character.isDigit(s.charAt(i+1)))){i++;while(i<s.length()&&(Character.isDigit(s.charAt(i))||s.charAt(i)=='.'))i++;if(i<s.length()&&(s.charAt(i)=='e'||s.charAt(i)=='E')){i++;if(i<s.length()&&(s.charAt(i)=='-'||s.charAt(i)=='+'))i++;while(i<s.length()&&Character.isDigit(s.charAt(i)))i++;}tokens.add(new Token(s.substring(start,i),false));continue;}
            if(Character.isLetter(ch)||ch=='_'){i++;while(i<s.length()&&(Character.isLetterOrDigit(s.charAt(i))||s.charAt(i)=='_'||s.charAt(i)=='.'))i++;tokens.add(new Token(s.substring(start,i).toLowerCase(Locale.ROOT),false));continue;}
            String op=i+1<s.length()?s.substring(i,i+2):"";if(Set.of("==","!=","<=",">=","&&","||","??","+=","-=","*=","/=","->").contains(op)){tokens.add(new Token(op,false));i+=2;}else{tokens.add(new Token(String.valueOf(ch),false));i++;}
        }tokens.add(new Token("<end>",false));}
        IllegalArgumentException error(String e){return new IllegalArgumentException(e+" at "+at+" in "+source);}
        String peek(){return tokens.get(at).text;}
        boolean take(String s){if(peek().equals(s)){at++;return true;}return false;}
        void require(String s){if(!take(s))throw error("Expected "+s+", got "+peek());}
        Expr program(){Expr e=statements("<end>");require("<end>");return e;}
        Expr statements(String end){List<Expr> list=new ArrayList<>();while(!peek().equals(end)&&!peek().equals("<end>")){if(take(";"))continue;if(take("return")){Expr v=expr(0);list.add(c->{throw new Return(v.eval(c));});}else list.add(expr(0));if(!take(";")&&!peek().equals(end))throw error("Expected semicolon");}return c->{Object r=0d;for(Expr e:list)r=e.eval(c);return r;};}
        Expr expr(int min){Expr left=prefix();while(true){String op=peek();int prec=precedence(op);if(prec<min)break;at++;Expr a=left;
            if(op.equals("?")){Expr yes=expr(0);Expr no=take(":")?expr(prec):new Const(0d);left=c->truth(a.eval(c))?yes.eval(c):no.eval(c);continue;}
            boolean assign=op.equals("=")||op.endsWith("=")&&Set.of("+=","-=","*=","/=","->").contains(op);
            Expr b=expr(prec+(assign?0:1));
            if(assign){if(!(a instanceof Var v))throw error("Invalid assignment");left=c->{Object r=b.eval(c);if(!op.equals("="))r=binary(op.substring(0,1),a.eval(c),r);c.set(v.name,r);return r;};}
            else left=c->{Object av=a.eval(c);return switch(op){case "&&"->truth(av)?(truth(b.eval(c))?1d:0d):0d;case "||"->truth(av)?1d:(truth(b.eval(c))?1d:0d);case "??"->av==null?b.eval(c):av;default->binary(op,av,b.eval(c));};};
        }return left;}
        Expr prefix(){if(take("!")){Expr x=expr(12);return c->truth(x.eval(c))?0d:1d;}if(take("-")){Expr x=expr(12);return c->-num(x.eval(c));}if(take("+"))return expr(12);
            Expr x;if(take("(")){x=expr(0);require(")");}else if(take("{")){x=statements("}");require("}");}else{Token t=tokens.get(at++);if(t.string)x=new Const(t.text);else if(Character.isDigit(t.text.charAt(0))||t.text.startsWith("."))x=new Const(Double.parseDouble(t.text));else if(Character.isLetter(t.text.charAt(0))||t.text.charAt(0)=='_')x=new Var(t.text);else throw error("Unexpected "+t.text);}
            while(true){Expr parent=x;if(take("(")){if(!(x instanceof Var v))throw error("Function expected");List<Expr> args=new ArrayList<>();if(!take(")")){do{args.add(expr(0));}while(take(","));require(")");}if(v.name.equals("loop")){if(args.size()!=2)throw error("loop arity");x=c->{int count=Math.min(1024,Math.max(0,(int)num(args.get(0).eval(c))));for(int i=0;i<count;i++)args.get(1).eval(c);return 0d;};continue;}x=c->{List<Object> vals=new ArrayList<>();for(Expr a:args)vals.add(a.eval(c));return c.call(v.name,vals);};}
                else if(take("->")){Expr child=prefix();x=c->{Object owner=parent.eval(c);return child.eval(owner instanceof Context own?own:c);};}
                else if(take("[")){Expr index=expr(0);require("]");x=c->{Object a=parent.eval(c);int n=(int)num(index.eval(c));if(a instanceof List<?> l&&!l.isEmpty())return l.get(Math.floorMod(n,l.size()));return 0d;};}
                else if(take(".")){String field=tokens.get(at++).text;x=c->{Object a=parent.eval(c);for(String k:field.split("\\."))a=a instanceof Map<?,?> m?m.get(k):null;return a;};}
                else break;
            }return x;
        }
        int precedence(String s){return switch(s){case "=","+=","-=","*=","/="->0;case "?"->1;case "??"->2;case "||"->3;case "&&"->4;case "==","!="->5;case "<",">","<=",">="->6;case "+","-"->7;case "*","/","%"->8;default->-1;};}
    }
    private static Object binary(String op,Object a,Object b){double x=num(a),y=num(b);return switch(op){case "+"->x+y;case "-"->x-y;case "*"->x*y;case "/"->y==0?0d:x/y;case "%"->y==0?0d:x%y;case "=="->(a instanceof String||b instanceof String?Objects.equals(a,b):x==y)?1d:0d;case "!="->(a instanceof String||b instanceof String?!Objects.equals(a,b):x!=y)?1d:0d;case "<"->x<y?1d:0d;case ">"->x>y?1d:0d;case "<="->x<=y?1d:0d;case ">="->x>=y?1d:0d;default->throw new IllegalArgumentException(op);};}
    private static double arg(List<Object> a,int i){return i<a.size()?num(a.get(i)):0;}
    private static double math(String name,List<Object> args,Random random){double a=arg(args,0),b=arg(args,1),c=arg(args,2);return switch(name){
        case "sin"->Math.sin(Math.toRadians(a));case "cos"->Math.cos(Math.toRadians(a));case "tan"->Math.tan(Math.toRadians(a));case "asin"->Math.toDegrees(Math.asin(Math.max(-1,Math.min(1,a))));case "acos"->Math.toDegrees(Math.acos(Math.max(-1,Math.min(1,a))));case "atan"->Math.toDegrees(Math.atan(a));case "atan2"->Math.toDegrees(Math.atan2(a,b));
        case "abs"->Math.abs(a);case "sqrt"->Math.sqrt(Math.max(0,a));case "pow"->Math.pow(a,b);case "exp"->Math.exp(a);case "ln"->a>0?Math.log(a):0;case "floor"->Math.floor(a);case "ceil"->Math.ceil(a);case "round"->Math.floor(a+.5);case "trunc"->(long)a;
        case "min"->Math.min(a,b);case "max"->Math.max(a,b);case "clamp"->Math.max(b,Math.min(c,a));case "lerp"->a+(b-a)*c;case "lerprotate"->a+(((b-a)%360+540)%360-180)*c;case "mod"->b==0?0:a%b;case "copy_sign"->Math.copySign(a,b);case "hermite_blend"->a*a*(3-2*a);case "pi"->Math.PI;
        case "random"->a+random.nextDouble()*(b-a);case "random_integer"->Math.floor(a+random.nextDouble()*(b-a+1));
        case "ease_in_expo"->a+(b-a)*(c==0?0:Math.pow(2,10*c-10));case "ease_out_expo"->a+(b-a)*(c==1?1:1-Math.pow(2,-10*c));
        default->throw new IllegalArgumentException("Unsupported math."+name);
    };}
}
