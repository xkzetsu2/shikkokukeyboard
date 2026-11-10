package juloo.keyboard2;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/** Personalização SHIKKOKU: cores, presets, fundo e overrides por tecla. */
public final class ShkStyle
{
  public static final String[] SLOTS = {
    "keyboard", "key", "action", "space", "pressed",
    "label", "sub", "lbl_act", "lbl_lock", "border" };
  public static final String[] SLOT_NAMES = {
    "Fundo do teclado", "Teclas", "Teclas de ação", "Barra de espaço",
    "Tecla pressionada", "Texto das teclas", "Texto dos cantos",
    "Modificador ativo", "Modificador travado", "Bordas" };
  public static final String[] SLOT_KANJI = {
    "背", "鍵", "動", "空", "押", "字", "副", "活", "錠", "縁" };

  public static final class Preset
  {
    public final String id, name, art, kanji, mode;
    public final int[] pal; // kb, key, action, space, label, sub, lock, border
    public final int accent, radius, border;
    public final boolean topline;
    Preset(String id, String name, int accent, int[] pal, String art, String kanji,
        String mode, int radius, int border, boolean topline)
    {
      this.id = id; this.name = name; this.accent = accent; this.pal = pal;
      this.art = art; this.kanji = kanji; this.mode = mode;
      this.radius = radius; this.border = border; this.topline = topline;
    }
  }

  public static final Preset[] LIST = {
    new Preset("shikkoku", "漆黒 Shikkoku", 0xFFE5173F,
      new int[] { 0xFF0A0A0C, 0xFF141418, 0xFF1C1C22, 0xFF181820, 0xFFECE9E4, 0xFF8E8E99, 0xFFFFB020, 0xFF2A2A32 },
      "enso", "漆黒", "center", 4, 10, false),
    new Preset("washi", "和紙 Washi", 0xFFD64545,
      new int[] { 0xFFEFEAE0, 0xFFF8F5EE, 0xFFE3DDD0, 0xFFEAE4D8, 0xFF1A1A1C, 0xFF77726A, 0xFF2E7D5B, 0xFFD2CBBB },
      "mount", "和", "vertical", 6, 8, false),
    new Preset("sumi", "墨 Sumi", 0xFF6EA8FF,
      new int[] { 0xFF16181D, 0xFF21242B, 0xFF2A2E36, 0xFF262A32, 0xFFE6E8EC, 0xFF8A90A0, 0xFF5BD6A5, 0xFF353A45 },
      "bamboo", "墨", "center", 4, 10, false),
    new Preset("cyber", "電脳 Cyberpunk", 0xFFFF2BD6,
      new int[] { 0xFF0B0221, 0xFF150836, 0xFF1F0B4A, 0xFF190A40, 0xFF00F0FF, 0xFFFF2BD6, 0xFFFFE600, 0xFF3A1F8F },
      "circuit", "電脳", "center", 2, 10, true),
    new Preset("synth", "夕陽 Synthwave", 0xFFFF5EA8,
      new int[] { 0xFF1A0B2E, 0xFF2A1248, 0xFF3A1A62, 0xFF301552, 0xFFFFE9F7, 0xFFC084FC, 0xFFFFD166, 0xFF5B2A8C },
      "moon", "夕陽", "center", 6, 8, true),
    new Preset("matrix", "零壱 Matrix", 0xFF3DFF7A,
      new int[] { 0xFF000A03, 0xFF04170A, 0xFF072410, 0xFF051C0C, 0xFF3DFF7A, 0xFF1E9E4A, 0xFFE5FF3D, 0xFF0F4A22 },
      "rain", "零壱", "tile", 2, 10, false),
    new Preset("sakura", "桜 Sakura", 0xFFE8567D,
      new int[] { 0xFFFFF1F4, 0xFFFFFFFF, 0xFFFBD5DE, 0xFFFDE3EA, 0xFF3A2430, 0xFF9B6B7A, 0xFF2E9E6F, 0xFFF3C3CF },
      "sakura", "桜", "vertical", 10, 8, false),
    new Preset("ocean", "海 Oceano", 0xFF38BDF8,
      new int[] { 0xFF061A2B, 0xFF0C2A44, 0xFF103759, 0xFF0E3150, 0xFFE6F4FF, 0xFF7FB2D9, 0xFFFFD27A, 0xFF1D4A73 },
      "wave", "海", "center", 8, 8, false),
    new Preset("ember", "炎 Brasa", 0xFFFF6A00,
      new int[] { 0xFF140805, 0xFF241008, 0xFF33170B, 0xFF2B1309, 0xFFFFE8D6, 0xFFC98A6A, 0xFFFFD166, 0xFF4A2512 },
      "dots", "炎", "center", 4, 10, false) };

  public static Preset find(String id)
  {
    for (Preset q : LIST) if (q.id.equals(id)) return q;
    return null;
  }

  public static final String[] ART_IDS = { "none", "enso", "moon", "bamboo", "mount", "wave", "circuit", "dots", "rain", "sakura" };
  public static final String[] ART_NAMES = { "Nenhuma", "円 Ensō", "月 Lua", "竹 Bambu", "山 Montanhas", "波 Ondas", "回 Circuito", "点 Pontos", "雨 Chuva", "桜 Sakura" };

  public static final String[] KANJI_PRESETS = {
    "漆黒", "影", "夜", "闇", "月", "桜", "龍", "鬼", "無", "刃", "静", "禅" };

  public static final int DEFAULT_ACCENT = 0xFFE5173F;

  public static final class KeyOv { public Integer bg; public Integer fg; public Integer bc; public Integer bw; /* bw: largura x10 dp; 0 = sem borda; null = padrão */ }

  private final SharedPreferences p;
  private final Context ctx;
  private final boolean forced;
  public final String preset;
  public final Preset pr;
  public final int accent;
  private final int[] palette;

  public static ShkStyle of(Context c)
  {
    return new ShkStyle(c, DirectBootAwarePreferences.get_shared_preferences(c), null);
  }

  /** Usado pela tela do app: lê as prefs padrão (fonte da verdade). */
  public static ShkStyle ofDefault(Context c)
  {
    return new ShkStyle(c, PreferenceManager.getDefaultSharedPreferences(c), null);
  }

  /** Estilo de um preset específico, ignorando as prefs (miniaturas). */
  public static ShkStyle forPreset(Context c, String id)
  {
    return new ShkStyle(c, PreferenceManager.getDefaultSharedPreferences(c), id);
  }

  public ShkStyle(Context c, SharedPreferences prefs, String force)
  {
    ctx = c;
    p = prefs;
    forced = force != null;
    String pn = forced ? force : p.getString("shk_preset", "off");
    preset = pn == null ? "off" : pn;
    pr = find(preset);
    if (!forced && p.contains("shk_accent"))
      accent = p.getInt("shk_accent", DEFAULT_ACCENT);
    else
      accent = pr != null ? pr.accent : DEFAULT_ACCENT;
    palette = buildPalette();
  }

  public SharedPreferences prefs() { return p; }
  public boolean isForced() { return forced; }

  static int idx(String slot)
  {
    for (int i = 0; i < SLOTS.length; i++)
      if (SLOTS[i].equals(slot)) return i;
    return -1;
  }

  static int mix(int a, int b, float t)
  {
    return Color.argb(255,
        (int)(Color.red(a) * (1 - t) + Color.red(b) * t),
        (int)(Color.green(a) * (1 - t) + Color.green(b) * t),
        (int)(Color.blue(a) * (1 - t) + Color.blue(b) * t));
  }

  public static boolean isLight(int color)
  {
    double l = (0.299 * Color.red(color) + 0.587 * Color.green(color)
        + 0.114 * Color.blue(color)) / 255.0;
    return l > 0.6;
  }

  private int[] buildPalette()
  {
    if (preset.equals("system"))
      return systemPalette();
    if (pr == null)
      return null;
    int[] q = pr.pal;
    int pressed = mix(q[1], accent, isLight(q[0]) ? 0.25f : 0.35f);
    return new int[] { q[0], q[1], q[2], q[3], pressed, q[4], q[5], accent, q[6], q[7] };
  }

  private int[] systemPalette()
  {
    if (Build.VERSION.SDK_INT < 31)
      return null;
    boolean dark = (ctx.getResources().getConfiguration().uiMode
        & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    if (dark)
      return new int[] {
        g(android.R.color.system_neutral1_900), g(android.R.color.system_neutral1_800),
        g(android.R.color.system_neutral1_700), g(android.R.color.system_neutral1_700),
        g(android.R.color.system_accent1_700), g(android.R.color.system_neutral1_0),
        g(android.R.color.system_neutral1_400), g(android.R.color.system_accent1_300),
        g(android.R.color.system_accent1_200), g(android.R.color.system_neutral1_700) };
    return new int[] {
      g(android.R.color.system_neutral1_100), g(android.R.color.system_neutral1_50),
      g(android.R.color.system_neutral1_200), g(android.R.color.system_neutral1_200),
      g(android.R.color.system_accent1_200), g(android.R.color.system_neutral1_900),
      g(android.R.color.system_neutral1_500), g(android.R.color.system_accent1_600),
      g(android.R.color.system_accent1_700), g(android.R.color.system_neutral1_200) };
  }

  private int g(int res) { return ctx.getColor(res); }

  private int[] _base;
  private float _themeRadius = 5, _themeBorder = 0;

  /** Cores do tema escolhido nas configurações clássicas (usadas na prévia). */
  public int[] baseDefaults()
  {
    if (_base == null) _base = loadBase();
    return _base;
  }

  public float themeRadiusDp() { baseDefaults(); return _themeRadius; }
  public float themeBorderDp() { baseDefaults(); return _themeBorder; }

  private int[] loadBase()
  {
    try
    {
      int id = Config.getThemeId(ctx.getResources(), p.getString("theme", ""));
      android.view.ContextThemeWrapper w = new android.view.ContextThemeWrapper(ctx, id);
      android.content.res.TypedArray s = w.getTheme().obtainStyledAttributes(R.styleable.keyboard);
      int key = s.getColor(R.styleable.keyboard_colorKey, 0xFF333333);
      android.util.TypedValue tv = new android.util.TypedValue();
      int kb = w.getTheme().resolveAttribute(R.attr.colorKeyboard, tv, true) ? tv.data : 0xFF1B1B1B;
      int label = s.getColor(R.styleable.keyboard_colorLabel, 0xFFFFFFFF);
      int[] r = new int[] { kb, key,
        s.getColor(R.styleable.keyboard_colorKeyAction, key),
        s.getColor(R.styleable.keyboard_colorKeySpaceBar, key),
        s.getColor(R.styleable.keyboard_colorKeyActivated, key),
        label,
        s.getColor(R.styleable.keyboard_colorSubLabel, label),
        s.getColor(R.styleable.keyboard_colorLabelActivated, 0xFF3399FF),
        s.getColor(R.styleable.keyboard_colorLabelLocked, 0xFF33CC33),
        s.getColor(R.styleable.keyboard_keyBorderColorLeft, key) };
      float d = ctx.getResources().getDisplayMetrics().density;
      _themeRadius = s.getDimension(R.styleable.keyboard_keyBorderRadius, 0) / d;
      _themeBorder = s.getDimension(R.styleable.keyboard_keyBorderWidth, 0) / d;
      s.recycle();
      return r;
    }
    catch (Throwable t)
    {
      return new int[] { 0xFF1B1B1B, 0xFF333333, 0xFF262626, 0xFF2B2B2B, 0xFF1B1B1B,
        0xFFFFFFFF, 0xFFCCCCCC, 0xFF3399FF, 0xFF33CC33, 0xFF404040 };
    }
  }

  public int c(String slot, int def)
  {
    String k = "shk_c_" + slot;
    if (!forced && p.contains(k))
      return p.getInt(k, def);
    int i = idx(slot);
    if (palette != null && i >= 0)
      return palette[i];
    return def;
  }

  /** Cor efetiva para a pré-visualização. */
  public int eff(String slot)
  {
    return c(slot, baseDefaults()[idx(slot)]);
  }

  public boolean has(String slot)
  {
    return (!forced && p.contains("shk_c_" + slot)) || (palette != null && idx(slot) >= 0);
  }

  public boolean hasOverride(String slot) { return !forced && p.contains("shk_c_" + slot); }

  private float dp(float v)
  {
    return v * ctx.getResources().getDisplayMetrics().density;
  }

  private int presetRadius()
  {
    if (pr != null) return pr.radius;
    return preset.equals("system") ? 8 : -1;
  }

  private int presetBorder()
  {
    if (pr != null) return pr.border;
    return preset.equals("system") ? 0 : -1;
  }

  private String presetShapeId()
  {
    switch (preset)
    {
      case "cyber": return "notch";
      case "matrix": case "ember": return "chamfer";
      case "synth": return "slant";
      case "ocean": return "pill";
      default: return "round";
    }
  }

  /** Bordas por tipo de tecla: key, action, space, pressed. */
  public boolean bordersOn(String role) { return forced || p.getBoolean("shk_bord_" + role, true); }
  public boolean borderSet() { return borderX10() >= 0; }
  public boolean radiusSet() { return radiusDp() >= 0; }

  public String shape() { return forced ? presetShapeId() : p.getString("shk_shape", presetShapeId()); }
  public int shapeSizePct() { return forced ? 25 : p.getInt("shk_shape_size", 25); }
  public String customPoly() { return p.getString("shk_shape_poly", "50,0 100,50 50,100 0,50"); }

  public int radiusDp() { return forced ? presetRadius() : p.getInt("shk_radius", presetRadius()); }
  public int borderX10() { return forced ? presetBorder() : p.getInt("shk_border", presetBorder()); }

  public float radiusPx(float def)
  {
    int v = radiusDp();
    return v < 0 ? def : dp(v);
  }

  public float borderPx(float def)
  {
    int v = borderX10();
    return v < 0 ? def : dp(v / 10f);
  }

  /* ---- overrides por tecla ---- */

  public Map<String, KeyOv> keyOverrides()
  {
    return forced ? new LinkedHashMap<String, KeyOv>() : parseKeys(p.getString("shk_keys", ""));
  }

  public static Map<String, KeyOv> parseKeys(String s)
  {
    Map<String, KeyOv> m = new LinkedHashMap<String, KeyOv>();
    if (s == null || s.isEmpty())
      return m;
    for (String line : s.split("\n"))
    {
      String[] f = line.split("\t", -1);
      if (f.length < 3 || f[0].isEmpty())
        continue;
      KeyOv o = new KeyOv();
      try
      {
        if (!f[1].equals("-")) o.bg = Integer.valueOf(Integer.parseInt(f[1]));
        if (!f[2].equals("-")) o.fg = Integer.valueOf(Integer.parseInt(f[2]));
        if (f.length > 3 && !f[3].equals("-")) o.bc = Integer.valueOf(Integer.parseInt(f[3]));
        if (f.length > 4 && !f[4].equals("-")) o.bw = Integer.valueOf(Integer.parseInt(f[4]));
      }
      catch (NumberFormatException e) { continue; }
      m.put(f[0], o);
    }
    return m;
  }

  public static String serializeKeys(Map<String, KeyOv> m)
  {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, KeyOv> e : m.entrySet())
    {
      KeyOv o = e.getValue();
      if (o.bg == null && o.fg == null && o.bc == null && o.bw == null)
        continue;
      if (sb.length() > 0) sb.append('\n');
      sb.append(e.getKey()).append('\t')
        .append(o.bg == null ? "-" : String.valueOf(o.bg.intValue())).append('\t')
        .append(o.fg == null ? "-" : String.valueOf(o.fg.intValue())).append('\t')
        .append(o.bc == null ? "-" : String.valueOf(o.bc.intValue())).append('\t')
        .append(o.bw == null ? "-" : String.valueOf(o.bw.intValue()));
    }
    return sb.toString();
  }

  /* ---- imagem de fundo ---- */

  public static File bgFile(Context c)
  {
    Context d = Build.VERSION.SDK_INT >= 24 ? c.createDeviceProtectedStorageContext() : c;
    return new File(d.getFilesDir(), "shk_bg.jpg");
  }

  private static Bitmap _bgCache = null;
  private static long _bgStamp = -1;

  public static synchronized Bitmap loadBackground(Context c)
  {
    File f = bgFile(c);
    if (!f.exists())
    {
      _bgCache = null;
      _bgStamp = -1;
      return null;
    }
    long st = f.lastModified() + f.length();
    if (_bgCache != null && st == _bgStamp)
      return _bgCache;
    try
    {
      _bgCache = BitmapFactory.decodeFile(f.getAbsolutePath());
      _bgStamp = st;
    }
    catch (Throwable t) { _bgCache = null; }
    return _bgCache;
  }

  /** Copia e reduz a imagem escolhida para o armazenamento do app. */
  public static boolean saveBackground(Context c, Uri uri)
  {
    try
    {
      BitmapFactory.Options o = new BitmapFactory.Options();
      o.inJustDecodeBounds = true;
      InputStream in = c.getContentResolver().openInputStream(uri);
      BitmapFactory.decodeStream(in, null, o);
      in.close();
      int sample = 1;
      while (o.outWidth / sample > 1600 || o.outHeight / sample > 1600)
        sample *= 2;
      BitmapFactory.Options o2 = new BitmapFactory.Options();
      o2.inSampleSize = sample;
      in = c.getContentResolver().openInputStream(uri);
      Bitmap bmp = BitmapFactory.decodeStream(in, null, o2);
      in.close();
      if (bmp == null)
        return false;
      FileOutputStream out = new FileOutputStream(bgFile(c));
      bmp.compress(Bitmap.CompressFormat.JPEG, 90, out);
      out.close();
      synchronized (ShkStyle.class) { _bgCache = null; _bgStamp = -1; }
      return true;
    }
    catch (Throwable t) { return false; }
  }

  public static void deleteBackground(Context c)
  {
    bgFile(c).delete();
    synchronized (ShkStyle.class) { _bgCache = null; _bgStamp = -1; }
  }

  /** Marca as prefs como alteradas e sincroniza com o armazenamento protegido. */
  public static void commit(Context c, SharedPreferences.Editor e, SharedPreferences prefs)
  {
    e.putInt("shk_rev", prefs.getInt("shk_rev", 0) + 1);
    e.commit();
    DirectBootAwarePreferences.copy_preferences_to_protected_storage(c, prefs);
  }

  /** Aplica cores de texto/fundo às views dos painéis (emoji, clipboard, sugestões). */
  public static void skin(Context ctx, android.view.View root, boolean pane)
  {
    ShkStyle st = of(ctx);
    if (!st.has("label") && !st.has("key") && !st.has("keyboard"))
      return;
    walk(st, root, pane);
  }

  private static void walk(ShkStyle st, android.view.View v, boolean pane)
  {
    if (v instanceof android.widget.TextView)
    {
      android.widget.TextView t = (android.widget.TextView)v;
      if (st.has("label"))
        t.setTextColor(st.c("label", 0xFFFFFFFF));
      if (st.has("key") && t.getBackground() instanceof android.graphics.drawable.ColorDrawable)
        t.setBackgroundColor(st.c("key", 0xFF333333));
    }
    else if (pane && v instanceof android.widget.AbsListView)
      v.setBackgroundColor(0);
    else if (pane && v instanceof Keyboard2View)
      v.setBackgroundColor(0);
    if (v instanceof android.view.ViewGroup)
    {
      android.view.ViewGroup g = (android.view.ViewGroup)v;
      for (int i = 0; i < g.getChildCount(); i++)
        walk(st, g.getChildAt(i), pane);
    }
  }
}
