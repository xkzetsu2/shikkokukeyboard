package juloo.keyboard2;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/** Backup e restauração de todas as configurações do teclado (JSON). */
public final class ShkBackup
{
  static final String MAGIC = "shikkoku-keyboard-backup";

  public static String fileName()
  {
    return "shikkoku-keyboard-" + new SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(new Date()) + ".json";
  }

  public static String export(Context c, boolean withImage) throws Exception
  {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
    JSONObject root = new JSONObject();
    root.put("magic", MAGIC);
    root.put("format", 1);
    root.put("created", System.currentTimeMillis());
    JSONObject jp = new JSONObject();
    for (Map.Entry<String, ?> e : prefs.getAll().entrySet())
    {
      String k = e.getKey();
      if (k.equals("need_migration")) continue;
      Object v = e.getValue();
      JSONObject o = new JSONObject();
      if (v instanceof Boolean) { o.put("t", "b"); o.put("v", ((Boolean)v).booleanValue()); }
      else if (v instanceof Integer) { o.put("t", "i"); o.put("v", ((Integer)v).intValue()); }
      else if (v instanceof Long) { o.put("t", "l"); o.put("v", ((Long)v).longValue()); }
      else if (v instanceof Float) { o.put("t", "f"); o.put("v", (double)((Float)v).floatValue()); }
      else if (v instanceof String) { o.put("t", "s"); o.put("v", (String)v); }
      else if (v instanceof Set)
      {
        o.put("t", "set");
        JSONArray a = new JSONArray();
        for (Object x : (Set<?>)v) a.put(String.valueOf(x));
        o.put("v", a);
      }
      else continue;
      jp.put(k, o);
    }
    root.put("prefs", jp);
    File img = ShkStyle.bgFile(c);
    if (withImage && img.exists() && img.length() < 3 * 1024 * 1024)
      root.put("bg_image", Base64.encodeToString(readAll(img), Base64.NO_WRAP));
    return root.toString(2);
  }

  /** Restaura. Retorna o número de chaves aplicadas. */
  public static int restore(Context c, String json) throws Exception
  {
    JSONObject root = new JSONObject(json);
    if (!MAGIC.equals(root.optString("magic")))
      throw new IllegalArgumentException("Arquivo de backup inválido");
    JSONObject jp = root.getJSONObject("prefs");
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
    SharedPreferences.Editor e = prefs.edit();
    e.clear();
    int n = 0;
    Iterator<String> it = jp.keys();
    while (it.hasNext())
    {
      String k = it.next();
      JSONObject o = jp.getJSONObject(k);
      String t = o.getString("t");
      if (t.equals("b")) e.putBoolean(k, o.getBoolean("v"));
      else if (t.equals("i")) e.putInt(k, o.getInt("v"));
      else if (t.equals("l")) e.putLong(k, o.getLong("v"));
      else if (t.equals("f")) e.putFloat(k, (float)o.getDouble("v"));
      else if (t.equals("s")) e.putString(k, o.getString("v"));
      else if (t.equals("set"))
      {
        JSONArray a = o.getJSONArray("v");
        Set<String> s = new HashSet<String>();
        for (int i = 0; i < a.length(); i++) s.add(a.getString(i));
        e.putStringSet(k, s);
      }
      else continue;
      n++;
    }
    e.putInt("shk_rev", prefs.getInt("shk_rev", 0) + 1);
    e.commit();
    if (root.has("bg_image"))
    {
      FileOutputStream out = new FileOutputStream(ShkStyle.bgFile(c));
      out.write(Base64.decode(root.getString("bg_image"), Base64.DEFAULT));
      out.close();
      ShkStyle.loadBackground(c);
    }
    DirectBootAwarePreferences.copy_preferences_to_protected_storage(c, prefs);
    return n;
  }


  /* ---- "Meu teclado": snapshot só do visual (estilo, cores, bordas, fundo) ---- */

  static boolean isMineKey(String k)
  {
    if (k.equals("theme")) return true;
    return k.startsWith("shk_") && !k.equals("shk_rev") && !k.equals("shk_mine");
  }

  static File mineFile(Context c) { return new File(c.getFilesDir(), "meu_teclado.json"); }

  public static boolean hasMine(Context c) { return mineFile(c).exists(); }

  /** Guarda o visual atual como "Meu teclado". */
  public static void saveMine(Context c) throws Exception
  {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
    JSONObject root = new JSONObject();
    root.put("magic", "shikkoku-keyboard-mine");
    root.put("created", System.currentTimeMillis());
    JSONObject jp = new JSONObject();
    for (Map.Entry<String, ?> e : prefs.getAll().entrySet())
    {
      String k = e.getKey();
      if (!isMineKey(k)) continue;
      Object v = e.getValue();
      JSONObject o = new JSONObject();
      if (v instanceof Boolean) { o.put("t", "b"); o.put("v", ((Boolean)v).booleanValue()); }
      else if (v instanceof Integer) { o.put("t", "i"); o.put("v", ((Integer)v).intValue()); }
      else if (v instanceof Long) { o.put("t", "l"); o.put("v", ((Long)v).longValue()); }
      else if (v instanceof Float) { o.put("t", "f"); o.put("v", (double)((Float)v).floatValue()); }
      else if (v instanceof String) { o.put("t", "s"); o.put("v", (String)v); }
      else if (v instanceof Set)
      {
        o.put("t", "set");
        JSONArray a = new JSONArray();
        for (Object x : (Set<?>)v) a.put(String.valueOf(x));
        o.put("v", a);
      }
      else continue;
      jp.put(k, o);
    }
    root.put("prefs", jp);
    File img = ShkStyle.bgFile(c);
    if (img.exists() && img.length() < 3 * 1024 * 1024)
      root.put("bg_image", Base64.encodeToString(readAll(img), Base64.NO_WRAP));
    FileOutputStream out = new FileOutputStream(mineFile(c));
    out.write(root.toString().getBytes("UTF-8"));
    out.close();
  }

  /** Coloca no editor tudo que "Meu teclado" define. Quem chama faz o commit. */
  public static void applyMine(Context c, SharedPreferences prefs, SharedPreferences.Editor e) throws Exception
  {
    JSONObject root = new JSONObject(new String(readAll(mineFile(c)), "UTF-8"));
    JSONObject jp = root.getJSONObject("prefs");
    for (String k : new ArrayList<String>(prefs.getAll().keySet()))
      if (isMineKey(k)) e.remove(k);
    Iterator<String> it = jp.keys();
    while (it.hasNext())
    {
      String k = it.next();
      JSONObject o = jp.getJSONObject(k);
      String t = o.getString("t");
      if (t.equals("b")) e.putBoolean(k, o.getBoolean("v"));
      else if (t.equals("i")) e.putInt(k, o.getInt("v"));
      else if (t.equals("l")) e.putLong(k, o.getLong("v"));
      else if (t.equals("f")) e.putFloat(k, (float)o.getDouble("v"));
      else if (t.equals("s")) e.putString(k, o.getString("v"));
      else if (t.equals("set"))
      {
        JSONArray a = o.getJSONArray("v");
        Set<String> st = new HashSet<String>();
        for (int i = 0; i < a.length(); i++) st.add(a.getString(i));
        e.putStringSet(k, st);
      }
    }
    e.putBoolean("shk_mine", true);
    if (root.has("bg_image"))
    {
      FileOutputStream out = new FileOutputStream(ShkStyle.bgFile(c));
      out.write(Base64.decode(root.getString("bg_image"), Base64.DEFAULT));
      out.close();
    }
  }

  /* ---- snapshots internos ---- */

  static File dir(Context c)
  {
    File d = new File(c.getFilesDir(), "backups");
    if (!d.exists()) d.mkdirs();
    return d;
  }

  public static File snapshot(Context c, String tag) throws Exception
  {
    File f = new File(dir(c), tag + "-" + fileName());
    FileOutputStream out = new FileOutputStream(f);
    out.write(export(c, true).getBytes("UTF-8"));
    out.close();
    // mantém só os 10 mais recentes
    List<File> l = snapshots(c);
    for (int i = 10; i < l.size(); i++) l.get(i).delete();
    return f;
  }

  public static List<File> snapshots(Context c)
  {
    File[] fs = dir(c).listFiles();
    List<File> l = new ArrayList<File>();
    if (fs != null) l.addAll(Arrays.asList(fs));
    java.util.Collections.sort(l, new Comparator<File>() {
      public int compare(File a, File b) { return Long.compare(b.lastModified(), a.lastModified()); }
    });
    return l;
  }

  public static String readFile(File f) throws Exception
  {
    return new String(readAll(f), "UTF-8");
  }

  public static String readStream(InputStream in) throws Exception
  {
    ByteArrayOutputStream bo = new ByteArrayOutputStream();
    byte[] buf = new byte[8192];
    int r;
    while ((r = in.read(buf)) > 0) bo.write(buf, 0, r);
    in.close();
    return bo.toString("UTF-8");
  }

  static byte[] readAll(File f) throws Exception
  {
    FileInputStream in = new FileInputStream(f);
    ByteArrayOutputStream bo = new ByteArrayOutputStream();
    byte[] buf = new byte[8192];
    int r;
    while ((r = in.read(buf)) > 0) bo.write(buf, 0, r);
    in.close();
    return bo.toByteArray();
  }
}
