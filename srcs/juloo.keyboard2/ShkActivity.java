package juloo.keyboard2;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import juloo.keyboard2.dict.DictionariesActivity;

/** Estúdio SHIKKOKU: cores, fundo kanji, desempenho e backup do teclado. */
public class ShkActivity extends Activity
{
  static final int REQ_EXPORT = 1, REQ_IMPORT = 2, REQ_IMAGE = 3;
  static final int BG = 0xFF0A0A0C, CARD = 0xFF141418, LINE = 0xFF2A2A32,
      TX = 0xFFECE9E4, SUB = 0xFF8E8E99;
  static final String[] TAB_K = { "家", "色", "鍵", "背", "性", "保", "設", "我" };
  static final String[] TAB_N = { "Início", "Cores", "Teclas", "Fundo", "Perf.", "Backup", "Ajustes", "Sobre" };

  SharedPreferences prefs;
  final Handler syncHandler = new Handler();
  final Runnable syncRun = new Runnable() {
    public void run()
    {
      DirectBootAwarePreferences.copy_preferences_to_protected_storage(ShkActivity.this, prefs);
    }
  };
  /** Configurações clássicas gravam nas prefs padrão: espelha no armazenamento do teclado. */
  final SharedPreferences.OnSharedPreferenceChangeListener syncL =
    new SharedPreferences.OnSharedPreferenceChangeListener() {
      public void onSharedPreferenceChanged(SharedPreferences sp, String key)
      {
        if (key != null && key.startsWith("shk_")) return;
        syncHandler.removeCallbacks(syncRun);
        syncHandler.postDelayed(syncRun, 400);
      }
    };
  int ac = ShkStyle.DEFAULT_ACCENT;
  int tab = 0;
  LinearLayout page, tabBar;
  ScrollView scroll;
  ShkPreview preview;
  boolean includeImage = true;
  final Handler handler = new Handler();
  Runnable ticker;
  TextView stLatency, stDraw, stMem, stKeys, stWpm;
  View barDraw, barMem;

  /* ================= util UI ================= */

  int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

  GradientDrawable box(int fill, int radiusDp, int stroke)
  {
    GradientDrawable g = new GradientDrawable();
    g.setColor(fill);
    g.setCornerRadius(dp(radiusDp));
    if (stroke != 0) g.setStroke(dp(1), stroke);
    return g;
  }

  TextView tv(String s, float sp, int color, boolean bold)
  {
    TextView t = new TextView(this);
    t.setText(s);
    t.setTextSize(sp);
    t.setTextColor(color);
    if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
    return t;
  }

  LinearLayout.LayoutParams lp(int w, int h, int top)
  {
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
    p.topMargin = dp(top);
    return p;
  }

  LinearLayout newCard()
  {
    LinearLayout l = new LinearLayout(this);
    l.setOrientation(LinearLayout.VERTICAL);
    l.setPadding(dp(14), dp(12), dp(14), dp(12));
    l.setBackground(box(CARD, 10, LINE));
    page.addView(l, lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 10));
    return l;
  }

  TextView badge(String k, int color)
  {
    TextView t = tv(k, 15, color, true);
    t.setGravity(Gravity.CENTER);
    t.setBackground(box((color & 0x00FFFFFF) | 0x22000000, 6, color));
    t.setLayoutParams(new LinearLayout.LayoutParams(dp(30), dp(30)));
    return t;
  }

  void section(String kanji, String title)
  {
    LinearLayout r = new LinearLayout(this);
    r.setGravity(Gravity.CENTER_VERTICAL);
    r.addView(badge(kanji, ac));
    TextView t = tv(title.toUpperCase(Locale.ROOT), 12, SUB, true);
    t.setPadding(dp(10), 0, 0, 0);
    t.setLetterSpacing(0.12f);
    r.addView(t);
    page.addView(r, lp(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 22));
  }

  TextView chip(String text, boolean sel, View.OnClickListener l)
  {
    TextView t = tv(text, 13, sel ? 0xFFFFFFFF : TX, sel);
    t.setPadding(dp(14), dp(8), dp(14), dp(8));
    t.setBackground(box(sel ? ac : 0xFF1C1C22, 16, sel ? ac : LINE));
    t.setOnClickListener(l);
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    p.rightMargin = dp(8);
    t.setLayoutParams(p);
    return t;
  }

  LinearLayout chipRow(LinearLayout parent)
  {
    HorizontalScrollView hs = new HorizontalScrollView(this);
    hs.setHorizontalScrollBarEnabled(false);
    LinearLayout l = new LinearLayout(this);
    l.setPadding(0, dp(8), 0, dp(4));
    hs.addView(l);
    parent.addView(hs);
    return l;
  }

  Button button(String text, View.OnClickListener l)
  {
    Button b = new Button(this);
    b.setText(text);
    b.setAllCaps(false);
    b.setTextColor(0xFFFFFFFF);
    b.setBackground(box(ac, 10, 0));
    b.setOnClickListener(l);
    return b;
  }

  Button ghost(String text, View.OnClickListener l)
  {
    Button b = new Button(this);
    b.setText(text);
    b.setAllCaps(false);
    b.setTextColor(TX);
    b.setBackground(box(0xFF1C1C22, 10, LINE));
    b.setOnClickListener(l);
    return b;
  }

  void commit(SharedPreferences.Editor e)
  {
    ShkStyle.commit(this, e, prefs);
    if (preview != null) preview.refresh();
  }

  void refreshKeep()
  {
    final int y = scroll.getScrollY();
    show(tab);
    scroll.post(new Runnable() { public void run() { scroll.scrollTo(0, y); } });
  }

  View swatch(int color, View.OnClickListener l)
  {
    View v = new View(this);
    v.setBackground(box(color, 8, 0xFF4A4A55));
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(44), dp(30));
    p.leftMargin = dp(8);
    v.setLayoutParams(p);
    v.setOnClickListener(l);
    return v;
  }

  void colorRow(LinearLayout parent, String kanji, String name, int color,
      boolean overridden, final ShkColorPicker.Callback pick, final Runnable reset)
  {
    LinearLayout r = new LinearLayout(this);
    r.setGravity(Gravity.CENTER_VERTICAL);
    r.setPadding(0, dp(6), 0, dp(6));
    r.addView(badge(kanji, TX));
    TextView n = tv(name, 14, TX, false);
    n.setPadding(dp(10), 0, 0, 0);
    r.addView(n, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
    if (overridden && reset != null)
    {
      TextView x = tv("↺", 18, SUB, false);
      x.setPadding(dp(8), 0, dp(4), 0);
      x.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) { reset.run(); }
      });
      r.addView(x);
    }
    final String title = name;
    final int cur = color;
    r.addView(swatch(color, new View.OnClickListener() {
      public void onClick(View v) { ShkColorPicker.show(ShkActivity.this, title, cur, pick, null, null); }
    }));
    parent.addView(r);
  }

  void toggle(LinearLayout parent, String name, final String key, boolean def)
  {
    Switch s = new Switch(this);
    s.setText(name);
    s.setTextColor(TX);
    s.setChecked(prefs.getBoolean(key, def));
    s.setPadding(0, dp(8), 0, dp(8));
    s.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
      public void onCheckedChanged(android.widget.CompoundButton b, boolean on)
      {
        commit(prefs.edit().putBoolean(key, on));
      }
    });
    parent.addView(s);
  }

  void slider(LinearLayout parent, final String name, final String key, final int min,
      int max, int value, final String suffix, final float div)
  {
    final TextView label = tv(name + ": " + fmt(value / div) + suffix, 13, SUB, false);
    label.setPadding(0, dp(8), 0, 0);
    parent.addView(label);
    SeekBar s = new SeekBar(this);
    s.setMax(max - min);
    s.setProgress(Math.max(0, Math.min(max - min, value - min)));
    s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      public void onProgressChanged(SeekBar sb, int p, boolean user)
      {
        int v = p + min;
        label.setText(name + ": " + fmt(v / div) + suffix);
        if (!user) return;
        prefs.edit().putInt(key, v).apply();
        if (preview != null) preview.refresh();
      }
      public void onStartTrackingTouch(SeekBar sb) {}
      public void onStopTrackingTouch(SeekBar sb) { commit(prefs.edit()); }
    });
    parent.addView(s);
  }

  static String fmt(float f)
  {
    return f == (int)f ? String.valueOf((int)f) : String.format(Locale.US, "%.1f", f);
  }

  /* ================= ciclo de vida ================= */

  @Override
  protected void onCreate(Bundle b)
  {
    super.onCreate(b);
    prefs = PreferenceManager.getDefaultSharedPreferences(this);
    if (!prefs.contains("shk_preset"))
    {
      SharedPreferences.Editor fe = prefs.edit();
      putPreset(fe, "shikkoku");
      ShkStyle.commit(this, fe, prefs);
    }
    try { Config.migrate(prefs); } catch (Exception e) {}
    getWindow().setStatusBarColor(BG);
    getWindow().setNavigationBarColor(BG);
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(BG);
    root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
      public WindowInsets onApplyWindowInsets(View v, WindowInsets in)
      {
        v.setPadding(in.getSystemWindowInsetLeft(), in.getSystemWindowInsetTop(),
            in.getSystemWindowInsetRight(), in.getSystemWindowInsetBottom());
        return in;
      }
    });
    LinearLayout head = new LinearLayout(this);
    head.setGravity(Gravity.CENTER_VERTICAL);
    head.setPadding(dp(16), dp(10), dp(16), dp(10));
    TextView logo = tv("漆黒", 20, 0xFFFFFFFF, true);
    head.addView(logo);
    TextView sub = tv("  SHIKKOKU KEYBOARD", 12, SUB, true);
    sub.setLetterSpacing(0.15f);
    head.addView(sub);
    root.addView(head);
    View line = new View(this);
    line.setBackgroundColor(LINE);
    root.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
    scroll = new ScrollView(this);
    page = new LinearLayout(this);
    page.setOrientation(LinearLayout.VERTICAL);
    page.setPadding(dp(14), dp(4), dp(14), dp(24));
    scroll.addView(page);
    root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
    tabBar = new LinearLayout(this);
    tabBar.setBackgroundColor(0xFF0F0F12);
    root.addView(tabBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)));
    setContentView(root);
    show(Math.max(0, Math.min(TAB_K.length - 1, getIntent().getIntExtra("tab", 0))));
  }

  @Override
  protected void onNewIntent(Intent in)
  {
    super.onNewIntent(in);
    setIntent(in);
    show(Math.max(0, Math.min(TAB_K.length - 1, in.getIntExtra("tab", 0))));
  }

  @Override
  protected void onResume()
  {
    super.onResume();
    if (tab == 0 || tab == 4) show(tab);
    prefs.registerOnSharedPreferenceChangeListener(syncL);
  }

  @Override
  protected void onPause()
  {
    prefs.unregisterOnSharedPreferenceChangeListener(syncL);
    syncHandler.removeCallbacks(syncRun);
    syncRun.run();
    handler.removeCallbacksAndMessages(null);
    ShkPerf.flush();
    super.onPause();
  }

  void buildTabBar()
  {
    tabBar.removeAllViews();
    for (int i = 0; i < TAB_K.length; i++)
    {
      final int idx = i;
      LinearLayout t = new LinearLayout(this);
      t.setOrientation(LinearLayout.VERTICAL);
      t.setGravity(Gravity.CENTER);
      boolean sel = i == tab;
      int col = sel ? ac : SUB;
      TextView k = tv(TAB_K[i], 18, col, true);
      k.setGravity(Gravity.CENTER);
      TextView n = tv(TAB_N[i], 10, col, false);
      n.setGravity(Gravity.CENTER);
      t.addView(k);
      t.addView(n);
      if (sel) t.setBackground(box((ac & 0x00FFFFFF) | 0x18000000, 0, 0));
      t.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) { show(idx); }
      });
      tabBar.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
    }
  }

  void show(int t)
  {
    tab = t;
    handler.removeCallbacksAndMessages(null);
    ac = ShkStyle.ofDefault(this).accent;
    page.removeAllViews();
    preview = null;
    scroll.setVisibility(View.VISIBLE);
    switch (t)
    {
      case 0: buildHome(); break;
      case 1: buildColors(); break;
      case 2: buildKeys(); break;
      case 3: buildBackground(); break;
      case 4: buildPerf(); break;
      case 5: buildBackup(); break;
      case 6: buildSettings(); break;
      case 7: buildAbout(); break;
      default: break;
    }
    buildTabBar();
    if (t == 0 || t == 4) scroll.scrollTo(0, 0);
  }

  void addPreview()
  {
    preview = new ShkPreview(this);
    LinearLayout.LayoutParams p = lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 12);
    preview.setBackground(box(0, 10, LINE));
    page.addView(preview, p);
  }

  /* ================= 我 Sobre ================= */

  static final String ABOUT_NAME = "João";
  static final String ABOUT_HANDLE = "xkzetsu2";
  static final String ABOUT_ROLE = "Desenvolvedor Android · personalização, ferramentas e IA";
  static final String ABOUT_BIO = "Crio apps Android com foco em personalização e produtividade: teclado, hub de IA e utilitários para aparelhos. O Shikkoku Keyboard nasceu da vontade de ter um teclado bonito, completo e totalmente ajustável.";
  static final String REPO_URL = "https://github.com/xkzetsu2/shikkokukeyboard";
  static final String PROFILE_URL = "https://github.com/xkzetsu2";

  void openUrl(String url)
  {
    try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
    catch (Exception ex) { Toast.makeText(this, "Nenhum navegador encontrado.", Toast.LENGTH_SHORT).show(); }
  }

  void buildAbout()
  {
    section("我", "Sobre");
    LinearLayout c = newCard();
    c.setGravity(Gravity.CENTER_HORIZONTAL);
    FrameLayout ring = new FrameLayout(this);
    GradientDrawable rg = new GradientDrawable();
    rg.setShape(GradientDrawable.OVAL);
    rg.setColor(0xFF000000);
    rg.setStroke(dp(3), ac);
    ring.setBackground(rg);
    ring.setPadding(dp(4), dp(4), dp(4), dp(4));
    ImageView iv = new ImageView(this);
    iv.setImageResource(R.drawable.shk_profile);
    iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
    iv.setOutlineProvider(new ViewOutlineProvider() {
      public void getOutline(View v, Outline o) { o.setOval(0, 0, v.getWidth(), v.getHeight()); }
    });
    iv.setClipToOutline(true);
    ring.addView(iv, new FrameLayout.LayoutParams(dp(112), dp(112)));
    c.addView(ring, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    TextView nm = tv(ABOUT_NAME, 20, TX, true);
    nm.setGravity(Gravity.CENTER);
    c.addView(nm, lp(-1, -2, 12));
    TextView hd = tv("@" + ABOUT_HANDLE, 13, ac, true);
    hd.setGravity(Gravity.CENTER);
    c.addView(hd, lp(-1, -2, 2));
    TextView ro = tv(ABOUT_ROLE, 12, SUB, false);
    ro.setGravity(Gravity.CENTER);
    c.addView(ro, lp(-1, -2, 6));
    TextView bio = tv(ABOUT_BIO, 13, TX, false);
    bio.setGravity(Gravity.CENTER);
    c.addView(bio, lp(-1, -2, 12));

    section("碼", "Repositório");
    LinearLayout r = newCard();
    r.addView(tv("Shikkoku Keyboard", 15, TX, true));
    r.addView(tv(REPO_URL.replace("https://", ""), 12, SUB, false), lp(-1, -2, 2));
    r.addView(button("Abrir repositório no GitHub", new View.OnClickListener() {
      public void onClick(View v) { openUrl(REPO_URL); }
    }), lp(-1, -2, 10));
    r.addView(ghost("Ver perfil no GitHub", new View.OnClickListener() {
      public void onClick(View v) { openUrl(PROFILE_URL); }
    }), lp(-1, -2, 8));
    r.addView(ghost("Copiar link do repositório", new View.OnClickListener() {
      public void onClick(View v)
      {
        android.content.ClipboardManager cm = (android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        cm.setPrimaryClip(android.content.ClipData.newPlainText("repo", REPO_URL));
        Toast.makeText(ShkActivity.this, "Link copiado.", Toast.LENGTH_SHORT).show();
      }
    }), lp(-1, -2, 8));

    section("版", "Versão e licença");
    LinearLayout v = newCard();
    String ver = "?";
    try { ver = getPackageManager().getPackageInfo(getPackageName(), 0).versionName; }
    catch (Exception ex) { /* mantém ? */ }
    v.addView(tv("Versão " + ver, 14, TX, true));
    v.addView(tv("Baseado no Unexpected Keyboard (juloo), software livre sob a licença GPL-3.0. Esta versão traz o estúdio visual SHIKKOKU: temas, cores, bordas por tecla, fundo kanji, backup e desempenho.", 12, SUB, false), lp(-1, -2, 6));
  }

  /* ================= 家 Início ================= */

  boolean imeEnabled()
  {
    InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
    List<InputMethodInfo> l = imm.getEnabledInputMethodList();
    for (InputMethodInfo i : l)
      if (i.getPackageName().equals(getPackageName())) return true;
    return false;
  }

  boolean imeSelected()
  {
    String cur = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
    return cur != null && cur.startsWith(getPackageName() + "/");
  }

  void buildHome()
  {
    addPreview();
    section("始", "Começar");
    LinearLayout c = newCard();
    boolean en = imeEnabled(), sel = imeSelected();
    c.addView(tv((en ? "✓ " : "○ ") + "1. Ativar o teclado", 15, en ? 0xFF2ECC71 : TX, true));
    Button b1 = ghost("Abrir configurações de teclados", new View.OnClickListener() {
      public void onClick(View v) { startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)); }
    });
    c.addView(b1, lp(-1, -2, 6));
    TextView t2 = tv((sel ? "✓ " : "○ ") + "2. Usar como teclado atual", 15, sel ? 0xFF2ECC71 : TX, true);
    c.addView(t2, lp(-2, -2, 14));
    Button b2 = button("Escolher teclado", new View.OnClickListener() {
      public void onClick(View v)
      {
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker();
      }
    });
    c.addView(b2, lp(-1, -2, 6));
    section("試", "Testar");
    LinearLayout t = newCard();
    EditText et = new EditText(this);
    et.setHint("Digite aqui para testar o teclado…");
    et.setHintTextColor(SUB);
    et.setTextColor(TX);
    t.addView(et);
    section("設", "Mais");
    LinearLayout m = newCard();
    m.addView(ghost("設定  Ajustes do teclado (layout, gestos, tamanho)", new View.OnClickListener() {
      public void onClick(View v) { show(6); }
    }), lp(-1, -2, 0));
    m.addView(ghost("辞  Dicionários", new View.OnClickListener() {
      public void onClick(View v) { startActivity(new Intent(ShkActivity.this, DictionariesActivity.class)); }
    }), lp(-1, -2, 8));
    m.addView(ghost("導  Guia de gestos", new View.OnClickListener() {
      public void onClick(View v) { startActivity(new Intent(ShkActivity.this, LauncherActivity.class)); }
    }), lp(-1, -2, 8));
    section("打", "Hoje");
    LinearLayout s = newCard();
    s.addView(tv(ShkPerf.today(this) + " teclas hoje  ·  " + ShkPerf.total(this) + " no total", 15, TX, false));
  }

  /* ================= 色 Cores ================= */

  void clearColorOverrides(SharedPreferences.Editor e)
  {
    for (String s : ShkStyle.SLOTS) e.remove("shk_c_" + s);
    e.remove("shk_radius");
    e.remove("shk_border");
  }

  /** Grava no editor tudo que um tema define (cores, arte de fundo, kanji). */
  void putPreset(SharedPreferences.Editor e, String id)
  {
    e.putString("shk_preset", id);
    clearColorOverrides(e);
    e.remove("shk_accent").remove("shk_k_color").remove("shk_shape");
    ShkStyle.Preset q = ShkStyle.find(id);
    if (q != null)
    {
      e.putString("shk_bg_art", q.art).putBoolean("shk_k_on", true)
        .putString("shk_k_text", q.kanji).putString("shk_k_mode", q.mode)
        .putBoolean("shk_topline", q.topline);
    }
    else
    {
      e.putString("shk_bg_art", "none").putBoolean("shk_k_on", false)
        .putBoolean("shk_topline", false);
    }
  }

  View presetCard(final String id, String name, boolean sel)
  {
    LinearLayout card = new LinearLayout(this);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setPadding(dp(6), dp(6), dp(6), dp(6));
    card.setBackground(box(sel ? ((ac & 0x00FFFFFF) | 0x28000000) : 0xFF1C1C22, 12, sel ? ac : LINE));
    card.addView(new ShkPreview(this, id, 140, 80), new LinearLayout.LayoutParams(dp(140), dp(80)));
    TextView n = tv(name, 12, sel ? 0xFFFFFFFF : TX, sel);
    n.setGravity(Gravity.CENTER);
    card.addView(n, lp(-1, -2, 6));
    card.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v)
      {
        if (prefs.getBoolean("shk_mine", true))
        {
          try { ShkBackup.saveMine(ShkActivity.this); } catch (Exception ex) { /* segue sem salvar */ }
        }
        SharedPreferences.Editor e = prefs.edit();
        putPreset(e, id);
        e.putBoolean("shk_mine", false);
        commit(e);
        refreshKeep();
      }
    });
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    p.rightMargin = dp(10);
    card.setLayoutParams(p);
    return card;
  }

  View mineCard(boolean active)
  {
    final boolean has = ShkBackup.hasMine(this);
    LinearLayout card = new LinearLayout(this);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setPadding(dp(6), dp(6), dp(6), dp(6));
    card.setBackground(box(active ? ((ac & 0x00FFFFFF) | 0x28000000) : 0xFF1C1C22, 12, active ? ac : LINE));
    TextView k = tv("我", 32, active ? ac : TX, true);
    k.setGravity(Gravity.CENTER);
    k.setBackground(box(0xFF0F0F12, 8, LINE));
    card.addView(k, new LinearLayout.LayoutParams(dp(140), dp(80)));
    TextView n = tv("★ Meu teclado", 12, active ? 0xFFFFFFFF : TX, active);
    n.setGravity(Gravity.CENTER);
    card.addView(n, lp(-1, -2, 6));
    TextView sub = tv(has ? "sua configuração salva" : "ainda vazio", 10, SUB, false);
    sub.setGravity(Gravity.CENTER);
    card.addView(sub);
    final boolean act = active;
    card.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v)
      {
        if (act) return;
        if (!has)
        {
          Toast.makeText(ShkActivity.this, "Personalize e toque em “Salvar como Meu teclado”.", Toast.LENGTH_LONG).show();
          return;
        }
        try
        {
          SharedPreferences.Editor e = prefs.edit();
          ShkBackup.applyMine(ShkActivity.this, prefs, e);
          commit(e);
          ShkStyle.loadBackground(ShkActivity.this);
          refreshKeep();
        }
        catch (Exception ex)
        {
          Toast.makeText(ShkActivity.this, "Não consegui carregar: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
      }
    });
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    p.rightMargin = dp(10);
    card.setLayoutParams(p);
    return card;
  }

  void buildColors()
  {
    addPreview();
    final ShkStyle st = ShkStyle.ofDefault(this);
    section("型", "Estilo");
    LinearLayout c = newCard();
    LinearLayout cr = chipRow(c);
    boolean mineOn = prefs.getBoolean("shk_mine", false);
    cr.addView(mineCard(mineOn));
    cr.addView(presetCard("off", "Original", !mineOn && st.preset.equals("off")));
    for (ShkStyle.Preset q : ShkStyle.LIST)
      cr.addView(presetCard(q.id, q.name, !mineOn && st.preset.equals(q.id)));
    cr.addView(presetCard("system", "系統 Sistema", !mineOn && st.preset.equals("system")));
    String note = "Cada tema aplica paleta, arte de fundo e kanji. Depois você ainda pode ajustar tudo na aba Fundo e nas cores abaixo.";
    if (android.os.Build.VERSION.SDK_INT < 31)
      note += "\n“Sistema” usa as cores dinâmicas do Material You (Android 12+). Neste aparelho ele não tem efeito.";
    else
      note += "\n“Sistema” segue o papel de parede e o modo claro/escuro automaticamente.";
    note += "\n“Meu teclado” guarda o seu visual (cores, bordas, teclas, fundo). Ao trocar de tema ele é salvo sozinho e você volta a ele quando quiser.";
    c.addView(tv(note, 12, SUB, false), lp(-1, -2, 6));
    c.addView(ghost("★ Salvar como Meu teclado", new View.OnClickListener() {
      public void onClick(View v)
      {
        try
        {
          ShkBackup.saveMine(ShkActivity.this);
          commit(prefs.edit().putBoolean("shk_mine", true));
          Toast.makeText(ShkActivity.this, "Salvo em Meu teclado.", Toast.LENGTH_SHORT).show();
          refreshKeep();
        }
        catch (Exception ex)
        {
          Toast.makeText(ShkActivity.this, "Erro ao salvar: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
      }
    }), lp(-1, -2, 8));

    section("彩", "Cor de destaque");
    LinearLayout a = newCard();
    colorRow(a, "朱", "Destaque (modificadores, kanji, linha)", st.accent, prefs.contains("shk_accent"),
        new ShkColorPicker.Callback() {
          public void onPick(int col) { commit(prefs.edit().putInt("shk_accent", col)); refreshKeep(); }
        }, new Runnable() {
          public void run() { commit(prefs.edit().remove("shk_accent")); refreshKeep(); }
        });

    section("色", "Cores por elemento");
    LinearLayout e = newCard();
    for (int i = 0; i < ShkStyle.SLOTS.length; i++)
    {
      final String slot = ShkStyle.SLOTS[i];
      colorRow(e, ShkStyle.SLOT_KANJI[i], ShkStyle.SLOT_NAMES[i], st.eff(slot), st.hasOverride(slot),
          new ShkColorPicker.Callback() {
            public void onPick(int col) { commit(prefs.edit().putInt("shk_c_" + slot, col)); refreshKeep(); }
          }, new Runnable() {
            public void run() { commit(prefs.edit().remove("shk_c_" + slot)); refreshKeep(); }
          });
    }

    section("基", "Tema base");
    LinearLayout tb = newCard();
    listRow(tb, "Tema do teclado clássico", "theme", "system", R.array.pref_theme_entries, R.array.pref_theme_values);
    tb.addView(tv("Vale quando o estilo acima é “Original”. Cantos, bordas e formato das teclas ficam na aba Teclas.", 12, SUB, false), lp(-1, -2, 4));

    section("鍵", "Teclas individuais");
    LinearLayout k = newCard();
    k.addView(tv("Toque numa tecla para editar fundo, texto, cor e espessura da borda, ou ligar/desligar a borda só dela.", 12, SUB, false));
    final Map<String, ShkStyle.KeyOv> ov = st.keyOverrides();
    for (final Map.Entry<String, ShkStyle.KeyOv> en : ov.entrySet())
    {
      final ShkStyle.KeyOv o = en.getValue();
      LinearLayout r = new LinearLayout(this);
      r.setGravity(Gravity.CENTER_VERTICAL);
      r.setPadding(0, dp(6), 0, dp(6));
      TextView kl = tv(en.getKey(), 16, TX, true);
      kl.setGravity(Gravity.CENTER);
      kl.setBackground(box(0xFF1C1C22, 6, LINE));
      kl.setTypeface(Theme.getKeyFont(this));
      r.addView(kl, new LinearLayout.LayoutParams(dp(40), dp(34)));
      String bt = o.bw == null ? "borda padrão" : o.bw.intValue() == 0 ? "sem borda"
          : "borda " + fmt(o.bw.intValue() / 10f) + " dp";
      TextView spacer = tv(" " + bt, 12, SUB, false);
      spacer.setPadding(dp(10), 0, 0, 0);
      r.addView(spacer, new LinearLayout.LayoutParams(0, -2, 1f));
      r.addView(swatch(o.bg != null ? o.bg.intValue() : st.eff("key"), null));
      r.addView(swatch(o.fg != null ? o.fg.intValue() : st.eff("label"), null));
      r.addView(swatch(o.bc != null ? o.bc.intValue() : st.eff("border"), null));
      TextView x = tv("✕", 16, SUB, false);
      x.setPadding(dp(14), 0, dp(4), 0);
      x.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) { ov.remove(en.getKey()); saveKeys(ov); }
      });
      r.addView(x);
      r.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) { editKey(en.getKey(), ov, st); }
      });
      k.addView(r);
    }
    k.addView(button("+ Adicionar tecla", new View.OnClickListener() {
      public void onClick(View v) { pickKey(ov, st); }
    }), lp(-1, -2, 8));

    section("戻", "Redefinir");
    LinearLayout z = newCard();
    z.addView(ghost("Restaurar cores originais", new View.OnClickListener() {
      public void onClick(View v)
      {
        SharedPreferences.Editor ed = prefs.edit().putString("shk_preset", "off");
        clearColorOverrides(ed);
        ed.remove("shk_keys").remove("shk_accent").remove("shk_topline").remove("shk_bg_art").remove("shk_shape").remove("shk_bord_key").remove("shk_bord_action").remove("shk_bord_space").remove("shk_bord_pressed");
        commit(ed);
        refreshKeep();
      }
    }));
  }

  void saveKeys(Map<String, ShkStyle.KeyOv> ov)
  {
    commit(prefs.edit().putString("shk_keys", ShkStyle.serializeKeys(ov)));
    refreshKeep();
  }

  void persistKeys(Map<String, ShkStyle.KeyOv> ov)
  {
    commit(prefs.edit().putString("shk_keys", ShkStyle.serializeKeys(ov)));
  }

  void pickKey(final Map<String, ShkStyle.KeyOv> ov, final ShkStyle st)
  {
    final android.widget.GridLayout g = new android.widget.GridLayout(this);
    g.setColumnCount(6);
    g.setPadding(dp(10), dp(10), dp(10), dp(10));
    final AlertDialog[] dlg = new AlertDialog[1];
    java.util.List<String> labels = new java.util.ArrayList<String>();
    String letters = "abcdefghijklmnopqrstuvwxyz0123456789.,-/@";
    for (int i = 0; i < letters.length(); i++) labels.add(letters.substring(i, i + 1));
    String[] names = { "shift", "ctrl", "alt", "fn", "meta", "capslock", "config", "switch_numeric",
        "switch_emoji", "switch_clipboard", "change_method", "action", "backspace", "delete",
        "enter", "tab", "esc", "space" };
    for (String n : names)
    {
      try
      {
        KeyValue kv = KeyValue.getKeyByName(n);
        if (kv == null || kv.getKind() == KeyValue.Kind.String) continue;
        String l = kv.getString();
        if (l != null && !l.isEmpty() && !labels.contains(l)) labels.add(l);
      }
      catch (Throwable t) {}
    }
    for (final String l : labels)
    {
      Button b = ghost(l, new View.OnClickListener() {
        public void onClick(View v)
        {
          if (dlg[0] != null) dlg[0].dismiss();
          editKey(l, ov, st);
        }
      });
      b.setTypeface(Theme.getKeyFont(this));
      b.setMinWidth(0);
      b.setMinimumWidth(0);
      android.widget.GridLayout.LayoutParams p = new android.widget.GridLayout.LayoutParams();
      p.width = dp(48);
      p.height = dp(44);
      p.setMargins(dp(3), dp(3), dp(3), dp(3));
      g.addView(b, p);
    }
    ScrollView sv = new ScrollView(this);
    sv.addView(g);
    dlg[0] = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
        .setTitle("Qual tecla?").setView(sv).setNegativeButton("Cancelar", null).show();
  }

  boolean reopening = false;

  /** Editor completo de uma tecla: fundo, texto, cor/espessura/ligar da borda. */
  void editKey(final String label, final Map<String, ShkStyle.KeyOv> ov, final ShkStyle st)
  {
    final ShkStyle.KeyOv o = ov.containsKey(label) ? ov.get(label) : new ShkStyle.KeyOv();
    final AlertDialog[] dlg = new AlertDialog[1];
    final boolean[] removed = new boolean[1];
    final Runnable reopen = new Runnable() {
      public void run()
      {
        ov.put(label, o);
        persistKeys(ov);
        reopening = true;
        if (dlg[0] != null) dlg[0].dismiss();
        reopening = false;
        editKey(label, ov, st);
      }
    };
    LinearLayout b = new LinearLayout(this);
    b.setOrientation(LinearLayout.VERTICAL);
    b.setPadding(dp(18), dp(8), dp(18), dp(8));
    colorRow(b, "背", "Fundo da tecla", o.bg != null ? o.bg.intValue() : st.eff("key"), o.bg != null,
        new ShkColorPicker.Callback() { public void onPick(int col) { o.bg = Integer.valueOf(col); reopen.run(); } },
        new Runnable() { public void run() { o.bg = null; reopen.run(); } });
    colorRow(b, "字", "Texto da tecla", o.fg != null ? o.fg.intValue() : st.eff("label"), o.fg != null,
        new ShkColorPicker.Callback() { public void onPick(int col) { o.fg = Integer.valueOf(col); reopen.run(); } },
        new Runnable() { public void run() { o.fg = null; reopen.run(); } });
    colorRow(b, "縁", "Cor da borda", o.bc != null ? o.bc.intValue() : st.eff("border"), o.bc != null,
        new ShkColorPicker.Callback() { public void onPick(int col) { o.bc = Integer.valueOf(col); reopen.run(); } },
        new Runnable() { public void run() { o.bc = null; reopen.run(); } });
    b.addView(tv("Borda desta tecla", 12, SUB, false), lp(-2, -2, 10));
    LinearLayout mr = chipRow(b);
    int mode = o.bw == null ? 0 : o.bw.intValue() == 0 ? 1 : 2;
    mr.addView(chip("Padrão", mode == 0, new View.OnClickListener() {
      public void onClick(View v) { o.bw = null; reopen.run(); }
    }));
    mr.addView(chip("Sem borda", mode == 1, new View.OnClickListener() {
      public void onClick(View v) { o.bw = Integer.valueOf(0); reopen.run(); }
    }));
    mr.addView(chip("Com borda", mode == 2, new View.OnClickListener() {
      public void onClick(View v)
      {
        o.bw = Integer.valueOf(st.borderX10() > 0 ? st.borderX10() : 10);
        reopen.run();
      }
    }));
    if (mode == 2)
    {
      final TextView lab = tv("Espessura: " + fmt(o.bw.intValue() / 10f) + " dp", 13, SUB, false);
      lab.setPadding(0, dp(8), 0, 0);
      b.addView(lab);
      SeekBar sb = new SeekBar(this);
      sb.setMax(49);
      sb.setProgress(Math.max(0, Math.min(49, o.bw.intValue() - 1)));
      sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
        public void onProgressChanged(SeekBar x, int pr, boolean user)
        {
          o.bw = Integer.valueOf(pr + 1);
          lab.setText("Espessura: " + fmt((pr + 1) / 10f) + " dp");
        }
        public void onStartTrackingTouch(SeekBar x) {}
        public void onStopTrackingTouch(SeekBar x) { ov.put(label, o); persistKeys(ov); }
      });
      b.addView(sb);
    }
    ScrollView sv = new ScrollView(this);
    sv.addView(b);
    dlg[0] = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
      .setTitle("Tecla “" + label + "”").setView(sv)
      .setPositiveButton("OK", null)
      .setNeutralButton("Remover", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface d, int w) { removed[0] = true; ov.remove(label); persistKeys(ov); }
      })
      .setOnDismissListener(new DialogInterface.OnDismissListener() {
        public void onDismiss(DialogInterface d)
        {
          if (reopening) return;
          if (!removed[0]) { ov.put(label, o); persistKeys(ov); }
          refreshKeep();
        }
      }).show();
  }

  /* ---- helpers de ajustes ---- */

  float num(String key, float def)
  {
    Object o = prefs.getAll().get(key);
    return o instanceof Number ? ((Number)o).floatValue() : def;
  }

  /** Slider que grava float (prefs do tipo SlideBarPreference). */
  void fslider(LinearLayout parent, final String name, final String key, final int min,
      int max, float value, final String suffix, final float div)
  {
    int iv = Math.round(value * div);
    final TextView label = tv(name + ": " + fmt(iv / div) + suffix, 13, SUB, false);
    label.setPadding(0, dp(8), 0, 0);
    parent.addView(label);
    SeekBar sb = new SeekBar(this);
    sb.setMax(max - min);
    sb.setProgress(Math.max(0, Math.min(max - min, iv - min)));
    sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      public void onProgressChanged(SeekBar x, int pr, boolean user)
      {
        int v = pr + min;
        label.setText(name + ": " + fmt(v / div) + suffix);
        if (!user) return;
        prefs.edit().putFloat(key, v / div).apply();
        if (preview != null) preview.refresh();
      }
      public void onStartTrackingTouch(SeekBar x) {}
      public void onStopTrackingTouch(SeekBar x) { commit(prefs.edit()); }
    });
    parent.addView(sb);
  }

  /** Linha de escolha (lista) que grava a string escolhida. */
  void listRow(LinearLayout parent, final String title, final String key, String def,
      int entriesRes, int valuesRes)
  {
    final String[] ent = getResources().getStringArray(entriesRes);
    final String[] val = getResources().getStringArray(valuesRes);
    String cur = prefs.getString(key, def);
    int ci = 0;
    for (int i = 0; i < val.length; i++) if (val[i].equals(cur)) ci = i;
    final int sel = ci;
    LinearLayout r = new LinearLayout(this);
    r.setOrientation(LinearLayout.VERTICAL);
    r.setPadding(0, dp(10), 0, dp(10));
    r.addView(tv(title, 14, TX, false));
    r.addView(tv(ent.length > ci ? ent[ci] : "", 12, ac, false));
    r.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v)
      {
        new AlertDialog.Builder(ShkActivity.this, android.R.style.Theme_Material_Dialog_Alert)
          .setTitle(title)
          .setSingleChoiceItems(ent, sel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int w)
            {
              commit(prefs.edit().putString(key, val[w]));
              d.dismiss();
              refreshKeep();
            }
          }).setNegativeButton("Cancelar", null).show();
      }
    });
    parent.addView(r);
  }

  /* ================= 鍵 Teclas ================= */

  static final String[] SHAPE_IDS = { "round", "square", "pill", "chamfer", "notch", "slant", "hex", "leaf", "custom" };
  static final String[] SHAPE_NAMES = { "丸 Arredondada", "角 Quadrada", "楕 Cápsula", "切 Chanfrada",
    "欠 Entalhe", "斜 Inclinada", "六 Hexágono", "葉 Folha", "自 Própria" };

  /** Uma tecla desenhada com o formato dado e as cores atuais. */
  final class ShapeView extends View
  {
    final String id;
    final ShkStyle st;
    final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    final RectF r = new RectF();
    final Path path = new Path();

    ShapeView(String id)
    {
      super(ShkActivity.this);
      this.id = id;
      st = ShkStyle.ofDefault(ShkActivity.this);
    }

    @Override
    protected void onDraw(Canvas c)
    {
      float d = getResources().getDisplayMetrics().density;
      r.set(dp(10), dp(8), getWidth() - dp(10), getHeight() - dp(8));
      Path sp = ShkShape.build(path, r, id, st.shapeSizePct() / 100f, ShkShape.parse(st.customPoly()));
      float rad = (st.radiusDp() >= 0 ? st.radiusDp() : st.themeRadiusDp()) * d;
      float bw = (st.borderX10() >= 0 ? st.borderX10() / 10f : st.themeBorderDp()) * d;
      p.setStyle(Paint.Style.FILL);
      p.setColor(st.eff("key"));
      if (sp != null) c.drawPath(sp, p); else c.drawRoundRect(r, rad, rad, p);
      if (bw > 0)
      {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(bw);
        p.setColor(st.eff("border"));
        if (sp != null) c.drawPath(sp, p); else c.drawRoundRect(r, rad, rad, p);
      }
      p.setStyle(Paint.Style.FILL);
      p.setColor(st.eff("label"));
      p.setTextAlign(Paint.Align.CENTER);
      p.setTextSize(r.height() * 0.42f);
      c.drawText("a", r.centerX(), r.centerY() - (p.ascent() + p.descent()) / 2f, p);
    }
  }

  View shapeCard(final String id, String name, boolean sel)
  {
    LinearLayout card = new LinearLayout(this);
    card.setOrientation(LinearLayout.VERTICAL);
    card.setPadding(dp(6), dp(6), dp(6), dp(6));
    card.setBackground(box(sel ? ((ac & 0x00FFFFFF) | 0x28000000) : 0xFF1C1C22, 12, sel ? ac : LINE));
    ShapeView sv = new ShapeView(id);
    sv.setBackgroundColor(ShkStyle.ofDefault(this).eff("keyboard"));
    card.addView(sv, new LinearLayout.LayoutParams(dp(92), dp(62)));
    TextView n = tv(name, 12, sel ? 0xFFFFFFFF : TX, sel);
    n.setGravity(Gravity.CENTER);
    card.addView(n, lp(-1, -2, 6));
    card.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v)
      {
        commit(prefs.edit().putString("shk_shape", id));
        refreshKeep();
      }
    });
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    p.rightMargin = dp(10);
    card.setLayoutParams(p);
    return card;
  }

  void buildKeys()
  {
    addPreview();
    final ShkStyle st = ShkStyle.ofDefault(this);
    section("形", "Formato das teclas");
    LinearLayout c = newCard();
    LinearLayout row = chipRow(c);
    String cur = st.shape();
    for (int i = 0; i < SHAPE_IDS.length; i++)
      row.addView(shapeCard(SHAPE_IDS[i], SHAPE_NAMES[i], SHAPE_IDS[i].equals(cur)));
    slider(c, "Tamanho do corte / curva", "shk_shape_size", 0, 50, st.shapeSizePct(), "%", 1f);
    c.addView(tv("Controla o chanfro, a inclinação e as curvas de cada formato. Em “Arredondada” valem os cantos da aba Cores.", 12, SUB, false), lp(-1, -2, 4));

    section("自", "Formato próprio");
    LinearLayout k = newCard();
    k.addView(tv("Desenhe com pontos x,y de 0 a 100 (x vai para a direita, y para baixo), no mínimo 3, separados por espaço.", 12, SUB, false));
    final EditText et = new EditText(this);
    et.setText(st.customPoly());
    et.setTextColor(TX);
    et.setHintTextColor(SUB);
    et.setHint("50,0 100,50 50,100 0,50");
    k.addView(et);
    LinearLayout ex = chipRow(k);
    final String[] exN = { "Losango", "Seta", "Octógono", "Canto cortado" };
    final String[] exP = { "50,0 100,50 50,100 0,50", "0,0 80,0 100,50 80,100 0,100",
      "30,0 70,0 100,30 100,70 70,100 30,100 0,70 0,30", "0,0 100,0 100,70 75,100 0,100" };
    for (int i = 0; i < exN.length; i++)
    {
      final String pts = exP[i];
      ex.addView(chip(exN[i], false, new View.OnClickListener() {
        public void onClick(View v) { et.setText(pts); }
      }));
    }
    k.addView(button("Usar formato próprio", new View.OnClickListener() {
      public void onClick(View v)
      {
        String t = et.getText().toString().trim();
        if (ShkShape.parse(t) == null)
        {
          toast("Pontos inválidos. Use pelo menos 3 pares x,y entre 0 e 100.");
          return;
        }
        commit(prefs.edit().putString("shk_shape_poly", t).putString("shk_shape", "custom"));
        refreshKeep();
      }
    }), lp(-1, -2, 8));

    section("縁", "Bordas");
    LinearLayout bd = newCard();
    int rad = st.radiusDp() >= 0 ? st.radiusDp() : Math.round(st.themeRadiusDp());
    int bor = st.borderX10() >= 0 ? st.borderX10() : Math.round(st.themeBorderDp() * 10);
    slider(bd, "Espessura da borda", "shk_border", 0, 50, bor, " dp", 10f);
    slider(bd, "Cantos arredondados", "shk_radius", 0, 24, rad, " dp", 1f);
    bd.addView(tv("Ativar bordas em", 12, SUB, false), lp(-2, -2, 10));
    toggle(bd, "Teclas normais (letras e números)", "shk_bord_key", true);
    toggle(bd, "Teclas de ação (shift, apagar, enter)", "shk_bord_action", true);
    toggle(bd, "Barra de espaço", "shk_bord_space", true);
    toggle(bd, "Tecla pressionada", "shk_bord_pressed", true);
    colorRow(bd, "縁", "Cor da borda", st.eff("border"), st.hasOverride("border"),
        new ShkColorPicker.Callback() {
          public void onPick(int col) { commit(prefs.edit().putInt("shk_c_border", col)); refreshKeep(); }
        }, new Runnable() {
          public void run() { commit(prefs.edit().remove("shk_c_border")); refreshKeep(); }
        });
    toggle(bd, "Linha de destaque no topo do teclado", "shk_topline", false);
    bd.addView(tv("A espessura vale para todos os tipos de tecla. Para uma tecla específica, use Cores → Teclas individuais.", 12, SUB, false), lp(-1, -2, 4));

    section("寸", "Tamanho e espaçamento");
    LinearLayout sz = newCard();
    slider(sz, "Altura do teclado (retrato)", "keyboard_height", 10, 100, (int)num("keyboard_height", 35), "%", 1f);
    slider(sz, "Altura do teclado (paisagem)", "keyboard_height_landscape", 20, 65, (int)num("keyboard_height_landscape", 50), "%", 1f);
    slider(sz, "Margem inferior (retrato)", "margin_bottom_portrait", 0, 250, (int)num("margin_bottom_portrait", 7), " dp", 1f);
    slider(sz, "Margem inferior (paisagem)", "margin_bottom_landscape", 0, 120, (int)num("margin_bottom_landscape", 3), " dp", 1f);
    slider(sz, "Margem lateral (retrato)", "horizontal_margin_portrait", 0, 30, (int)num("horizontal_margin_portrait", 3), " dp", 1f);
    slider(sz, "Margem lateral (paisagem)", "horizontal_margin_landscape", 0, 200, (int)num("horizontal_margin_landscape", 28), " dp", 1f);
    if (FoldStateTracker.isFoldableDevice(this))
    {
      slider(sz, "Altura (dobrável aberto, retrato)", "keyboard_height_unfolded", 10, 50, (int)num("keyboard_height_unfolded", 35), "%", 1f);
      slider(sz, "Altura (dobrável aberto, paisagem)", "keyboard_height_landscape_unfolded", 20, 65, (int)num("keyboard_height_landscape_unfolded", 50), "%", 1f);
      slider(sz, "Margem inferior (dobrável aberto, retrato)", "margin_bottom_portrait_unfolded", 0, 250, (int)num("margin_bottom_portrait_unfolded", 7), " dp", 1f);
      slider(sz, "Margem inferior (dobrável aberto, paisagem)", "margin_bottom_landscape_unfolded", 0, 250, (int)num("margin_bottom_landscape_unfolded", 3), " dp", 1f);
      slider(sz, "Margem lateral (dobrável aberto, retrato)", "horizontal_margin_portrait_unfolded", 0, 30, (int)num("horizontal_margin_portrait_unfolded", 3), " dp", 1f);
      slider(sz, "Margem lateral (dobrável aberto, paisagem)", "horizontal_margin_landscape_unfolded", 0, 200, (int)num("horizontal_margin_landscape_unfolded", 28), " dp", 1f);
    }
    fslider(sz, "Espaço vertical entre teclas", "key_vertical_margin", 0, 50, num("key_vertical_margin", 1.5f), "%", 10f);
    fslider(sz, "Espaço horizontal entre teclas", "key_horizontal_margin", 0, 50, num("key_horizontal_margin", 2f), "%", 10f);
    fslider(sz, "Tamanho dos indicadores", "character_size", 75, 150, num("character_size", 1.15f), "x", 100f);

    section("透", "Opacidade e brilho");
    LinearLayout op = newCard();
    slider(op, "Brilho dos rótulos", "label_brightness", 50, 100, (int)num("label_brightness", 100), "%", 1f);
    slider(op, "Opacidade do fundo do teclado", "keyboard_opacity", 0, 100, (int)num("keyboard_opacity", 100), "%", 1f);
    slider(op, "Opacidade das teclas", "key_opacity", 0, 100, (int)num("key_opacity", 100), "%", 1f);
    slider(op, "Opacidade das teclas pressionadas", "key_activated_opacity", 0, 100, (int)num("key_activated_opacity", 100), "%", 1f);
    op.addView(tv("O fundo com arte, imagem ou kanji só aparece com a opacidade do fundo em 100%.", 12, SUB, false), lp(-1, -2, 4));

  }

  /* ================= 背 Fundo ================= */

  void buildBackground()
  {
    addPreview();
    final ShkStyle st = ShkStyle.ofDefault(this);
    LinearLayout mc = newCard();
    toggle(mc, "Fundo do teclado ligado (arte, imagem e kanji)", "shk_bg_master", true);
    mc.addView(tv("Desligue para um teclado de cor lisa. Suas escolhas abaixo ficam guardadas.", 12, SUB, false), lp(-1, -2, 2));
    section("絵", "Arte minimalista");
    LinearLayout art = newCard();
    LinearLayout ar = chipRow(art);
    String curArt = prefs.getString("shk_bg_art", "none");
    for (int i = 0; i < ShkStyle.ART_IDS.length; i++)
    {
      final String aid = ShkStyle.ART_IDS[i];
      ar.addView(chip(ShkStyle.ART_NAMES[i], aid.equals(curArt), new View.OnClickListener() {
        public void onClick(View v) { commit(prefs.edit().putString("shk_bg_art", aid)); refreshKeep(); }
      }));
    }
    art.addView(tv("Desenhada pelo app com a cor de destaque, atrás das teclas. Combina com o kanji abaixo.", 12, SUB, false), lp(-1, -2, 4));
    section("画", "Imagem de fundo");
    LinearLayout c = newCard();
    toggle(c, "Usar imagem de fundo", "shk_bg_on", false);
    LinearLayout br = new LinearLayout(this);
    Button pick = button("Escolher imagem", new View.OnClickListener() {
      public void onClick(View v)
      {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("image/*");
        startActivityForResult(i, REQ_IMAGE);
      }
    });
    br.addView(pick, new LinearLayout.LayoutParams(0, -2, 1f));
    Button rm = ghost("Remover", new View.OnClickListener() {
      public void onClick(View v)
      {
        ShkStyle.deleteBackground(ShkActivity.this);
        commit(prefs.edit().putBoolean("shk_bg_on", false));
        refreshKeep();
      }
    });
    LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(0, -2, 1f);
    rp.leftMargin = dp(8);
    br.addView(rm, rp);
    c.addView(br, lp(-1, -2, 6));
    c.addView(tv(ShkStyle.bgFile(this).exists() ? "Imagem salva ✓" : "Nenhuma imagem escolhida ainda.", 12, SUB, false), lp(-2, -2, 6));
    slider(c, "Opacidade da imagem", "shk_bg_alpha", 5, 100, prefs.getInt("shk_bg_alpha", 60), "%", 1f);
    slider(c, "Escurecer imagem", "shk_bg_dim", 0, 80, prefs.getInt("shk_bg_dim", 25), "%", 1f);

    section("漢", "Kanji decorativo");
    LinearLayout k = newCard();
    toggle(k, "Mostrar kanji no fundo", "shk_k_on", false);
    k.addView(tv("Sugestões", 12, SUB, false), lp(-2, -2, 4));
    LinearLayout cr = chipRow(k);
    String curText = prefs.getString("shk_k_text", "漆黒");
    for (final String t : ShkStyle.KANJI_PRESETS)
    {
      cr.addView(chip(t, t.equals(curText), new View.OnClickListener() {
        public void onClick(View v)
        {
          commit(prefs.edit().putString("shk_k_text", t).putBoolean("shk_k_on", true));
          refreshKeep();
        }
      }));
    }
    final EditText et = new EditText(this);
    et.setText(curText);
    et.setTextColor(TX);
    et.setHint("Seu texto (kanji, letras…)");
    et.setHintTextColor(SUB);
    et.setSingleLine(true);
    k.addView(et);
    k.addView(ghost("Aplicar texto", new View.OnClickListener() {
      public void onClick(View v)
      {
        String s = et.getText().toString().trim();
        if (s.isEmpty()) s = "漆黒";
        commit(prefs.edit().putString("shk_k_text", s).putBoolean("shk_k_on", true));
        refreshKeep();
      }
    }), lp(-1, -2, 4));
    k.addView(tv("Disposição", 12, SUB, false), lp(-2, -2, 10));
    LinearLayout mr = chipRow(k);
    String curMode = prefs.getString("shk_k_mode", "center");
    final String[] mid = { "center", "tile", "vertical" };
    String[] mn = { "Centro", "Mosaico", "Vertical" };
    for (int i = 0; i < mid.length; i++)
    {
      final String id = mid[i];
      mr.addView(chip(mn[i], id.equals(curMode), new View.OnClickListener() {
        public void onClick(View v) { commit(prefs.edit().putString("shk_k_mode", id)); refreshKeep(); }
      }));
    }
    k.addView(tv("Fonte", 12, SUB, false), lp(-2, -2, 6));
    LinearLayout fr = chipRow(k);
    String[] fn = { "Negrito", "Serifa", "Normal" };
    int curStyle = prefs.getInt("shk_k_style", 0);
    for (int i = 0; i < fn.length; i++)
    {
      final int id = i;
      fr.addView(chip(fn[i], id == curStyle, new View.OnClickListener() {
        public void onClick(View v) { commit(prefs.edit().putInt("shk_k_style", id)); refreshKeep(); }
      }));
    }
    slider(k, "Tamanho", "shk_k_size", 30, 160, prefs.getInt("shk_k_size", 80), "%", 1f);
    slider(k, "Opacidade", "shk_k_alpha", 2, 60, prefs.getInt("shk_k_alpha", 12), "%", 1f);
    int kc = prefs.contains("shk_k_color") ? prefs.getInt("shk_k_color", st.accent) : st.accent;
    colorRow(k, "彩", "Cor do kanji", kc, prefs.contains("shk_k_color"),
        new ShkColorPicker.Callback() {
          public void onPick(int col) { commit(prefs.edit().putInt("shk_k_color", col)); refreshKeep(); }
        }, new Runnable() {
          public void run() { commit(prefs.edit().remove("shk_k_color")); refreshKeep(); }
        });
    k.addView(tv("Dica: o fundo só aparece no teclado se a “Opacidade do teclado” nas configurações clássicas estiver em 100%.", 11, SUB, false), lp(-1, -2, 8));
  }

  /* ================= 設 Ajustes ================= */

  boolean extraOpen = false;

  void buildSettings()
  {
    addPreview();
    section("配", "Layouts");
    LinearLayout ly = newCard();
    buildLayoutsCard(ly);

    section("追", "Teclas extras");
    LinearLayout ek = newCard();
    buildExtraKeysCard(ek);

    section("入", "Layout e teclas");
    LinearLayout a = newCard();
    listRow(a, "Fileira de números", "number_row", "no_number_row", R.array.pref_show_number_row_entries, R.array.pref_show_number_row_values);
    listRow(a, "Teclado numérico", "show_numpad", "never", R.array.pref_show_numpad_entries, R.array.pref_show_numpad_values);
    listRow(a, "Ordem dos dígitos no teclado numérico", "numpad_layout", "high_first", R.array.pref_numpad_layout_entries, R.array.pref_numpad_layout_values);
    listRow(a, "Layout dividido", "split_layout", "wide", R.array.pref_split_layout_entries, R.array.pref_split_layout_values);
    listRow(a, "Layout ao digitar números e telefones", "number_entry_layout", "pin", R.array.pref_number_entry_entries, R.array.pref_number_entry_values);

    section("提", "Sugestões");
    LinearLayout g = newCard();
    toggle(g, "Sugestões de palavras e ortografia", "suggestions", true);
    toggle(g, "Completar com a barra de espaço", "space_bar_auto_complete", false);
    toggle(g, "Adicionar espaço após a palavra escolhida", "suggestions_add_space", true);
    g.addView(ghost("辞  Dicionários", new View.OnClickListener() {
      public void onClick(View v) { startActivity(new Intent(ShkActivity.this, DictionariesActivity.class)); }
    }), lp(-1, -2, 8));

    section("打", "Digitação e gestos");
    LinearLayout t = newCard();
    listRow(t, "Distância para deslizar", "swipe_dist", "15", R.array.pref_swipe_dist_entries, R.array.pref_swipe_dist_values);
    listRow(t, "Sensibilidade do gesto circular", "circle_sensitivity", "2", R.array.pref_circle_sensitivity_entries, R.array.pref_circle_sensitivity_values);
    listRow(t, "Sensibilidade do controle deslizante da barra de espaço", "slider_sensitivity", "30", R.array.pref_slider_sensitivity_entries, R.array.pref_slider_sensitivity_values);
    slider(t, "Tempo de pressionamento", "longpress_timeout", 50, 2000, (int)num("longpress_timeout", 600), " ms", 1f);
    toggle(t, "Repetir tecla ao segurar", "keyrepeat_enabled", true);
    slider(t, "Intervalo de repetição", "longpress_interval", 5, 100, (int)num("longpress_interval", 25), " ms", 1f);
    toggle(t, "Toque duplo no shift para travar", "lock_double_tap", false);
    listRow(t, "Quando há teclado físico conectado", "physical_keyboard_behavior", "hide", R.array.pref_physical_keyboard_behavior_entries, R.array.pref_physical_keyboard_behavior_values);

    section("動", "Comportamento");
    LinearLayout b = newCard();
    toggle(b, "Capitalização automática", "autocapitalisation", true);
    listRow(b, "Tecla de trocar teclado", "change_method_key_replacement", "prev", R.array.pref_change_method_key_replacement_entries, R.array.pref_change_method_key_replacement_values);
    toggle(b, "Vibração personalizada", "vibrate_custom", false);
    slider(b, "Intensidade da vibração", "vibrate_duration", 0, 100, (int)num("vibrate_duration", 20), " ms", 1f);

    section("貼", "Área de transferência");
    LinearLayout c = newCard();
    listRow(c, "Duração do histórico", "clipboard_history_duration", "5", R.array.pref_clipboard_duration_entries, R.array.pref_clipboard_duration_values);

    section("設", "Avançado");
    LinearLayout z = newCard();
    z.addView(ghost("導  Guia de gestos", new View.OnClickListener() {
      public void onClick(View v) { startActivity(new Intent(ShkActivity.this, LauncherActivity.class)); }
    }), lp(-1, -2, 8));
    z.addView(tv("Tamanho, margens, bordas e opacidade ficam na aba Teclas.", 12, SUB, false), lp(-1, -2, 6));
    z.addView(tv("Versão: Shikkoku Keyboard (base Unexpected Keyboard 2.2.0).", 11, SUB, false), lp(-1, -2, 6));
  }

  /* ---- layouts nativos ---- */

  void saveLayouts(java.util.List<juloo.keyboard2.prefs.LayoutsPreference.Layout> l)
  {
    SharedPreferences.Editor e = prefs.edit();
    juloo.keyboard2.prefs.ShkPrefsApi.saveLayouts(e, l);
    commit(e);
    refreshKeep();
  }

  void buildLayoutsCard(LinearLayout c)
  {
    final java.util.List<juloo.keyboard2.prefs.LayoutsPreference.Layout> ls =
        juloo.keyboard2.prefs.ShkPrefsApi.loadLayouts(prefs);
    c.addView(tv("O primeiro layout é o inicial; os outros se alternam pela tecla de idioma.", 12, SUB, false));
    for (int i = 0; i < ls.size(); i++)
    {
      final int idx = i;
      final juloo.keyboard2.prefs.LayoutsPreference.Layout l = ls.get(i);
      LinearLayout r = new LinearLayout(this);
      r.setGravity(Gravity.CENTER_VERTICAL);
      r.setPadding(0, dp(8), 0, dp(8));
      TextView n = tv((i + 1) + ".", 14, ac, true);
      r.addView(n, new LinearLayout.LayoutParams(dp(26), -2));
      TextView t = tv(juloo.keyboard2.prefs.ShkPrefsApi.layoutLabel(getResources(), l), 14, TX, false);
      r.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
      if (i > 0)
      {
        TextView up = tv("↑", 18, SUB, false);
        up.setPadding(dp(10), 0, dp(10), 0);
        up.setOnClickListener(new View.OnClickListener() {
          public void onClick(View v) { java.util.Collections.swap(ls, idx, idx - 1); saveLayouts(ls); }
        });
        r.addView(up);
      }
      if (i < ls.size() - 1)
      {
        TextView dn = tv("↓", 18, SUB, false);
        dn.setPadding(dp(10), 0, dp(10), 0);
        dn.setOnClickListener(new View.OnClickListener() {
          public void onClick(View v) { java.util.Collections.swap(ls, idx, idx + 1); saveLayouts(ls); }
        });
        r.addView(dn);
      }
      if (ls.size() > 1)
      {
        TextView x = tv("✕", 16, SUB, false);
        x.setPadding(dp(10), 0, dp(4), 0);
        x.setOnClickListener(new View.OnClickListener() {
          public void onClick(View v) { ls.remove(idx); saveLayouts(ls); }
        });
        r.addView(x);
      }
      r.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) { pickLayout(ls, idx); }
      });
      c.addView(r);
    }
    c.addView(button("+ Adicionar layout", new View.OnClickListener() {
      public void onClick(View v) { pickLayout(ls, -1); }
    }), lp(-1, -2, 8));
  }

  /** idx < 0 adiciona um layout novo; senão troca/edita o layout idx. */
  void pickLayout(final java.util.List<juloo.keyboard2.prefs.LayoutsPreference.Layout> ls, final int idx)
  {
    if (idx >= 0 && juloo.keyboard2.prefs.ShkPrefsApi.isCustom(ls.get(idx)))
    {
      editCustomLayout(ls, idx, juloo.keyboard2.prefs.ShkPrefsApi.customXml(ls.get(idx)));
      return;
    }
    final String[] names = juloo.keyboard2.prefs.ShkPrefsApi.layoutNames(getResources());
    String[] labels = juloo.keyboard2.prefs.ShkPrefsApi.layoutLabels(getResources());
    new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
      .setTitle(idx < 0 ? "Adicionar layout" : "Trocar layout")
      .setItems(labels, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface d, int which)
        {
          String n = names[which];
          if (n.equals("custom"))
          {
            editCustomLayout(ls, idx, juloo.keyboard2.prefs.ShkPrefsApi.initialCustomLayout(getResources()));
            return;
          }
          juloo.keyboard2.prefs.LayoutsPreference.Layout nl = juloo.keyboard2.prefs.ShkPrefsApi.namedLayout(n);
          if (idx < 0) ls.add(nl); else ls.set(idx, nl);
          saveLayouts(ls);
        }
      }).setNegativeButton("Cancelar", null).show();
  }

  void editCustomLayout(final java.util.List<juloo.keyboard2.prefs.LayoutsPreference.Layout> ls,
      final int idx, String initial)
  {
    CustomLayoutEditDialog.show(this, initial, idx >= 0 && ls.size() > 1,
      new CustomLayoutEditDialog.Callback() {
        public void select(String text)
        {
          if (text == null) { ls.remove(idx); saveLayouts(ls); return; }
          juloo.keyboard2.prefs.LayoutsPreference.Layout nl = juloo.keyboard2.prefs.ShkPrefsApi.customLayout(text);
          if (idx < 0) ls.add(nl); else ls.set(idx, nl);
          saveLayouts(ls);
        }
        public String validate(String text) { return juloo.keyboard2.prefs.ShkPrefsApi.validateLayout(text); }
      });
  }

  /* ---- teclas extras nativas ---- */

  void buildExtraKeysCard(LinearLayout c)
  {
    c.addView(tv("Teclas personalizadas: texto ou nome de tecla que entra no teclado.", 12, SUB, false));
    final java.util.List<String> cks = juloo.keyboard2.prefs.ShkPrefsApi.loadCustomKeys(prefs);
    for (int i = 0; i < cks.size(); i++)
    {
      final int idx = i;
      LinearLayout r = new LinearLayout(this);
      r.setGravity(Gravity.CENTER_VERTICAL);
      r.setPadding(0, dp(8), 0, dp(8));
      r.addView(tv(cks.get(i), 15, TX, false), new LinearLayout.LayoutParams(0, -2, 1f));
      TextView x = tv("✕", 16, SUB, false);
      x.setPadding(dp(12), 0, dp(4), 0);
      x.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v)
        {
          cks.remove(idx);
          SharedPreferences.Editor e = prefs.edit();
          juloo.keyboard2.prefs.ShkPrefsApi.saveCustomKeys(e, cks);
          commit(e);
          refreshKeep();
        }
      });
      r.addView(x);
      c.addView(r);
    }
    c.addView(ghost("+ Tecla personalizada", new View.OnClickListener() {
      public void onClick(View v)
      {
        final EditText in = new EditText(ShkActivity.this);
        in.setTextColor(TX);
        new AlertDialog.Builder(ShkActivity.this, android.R.style.Theme_Material_Dialog_Alert)
          .setTitle("Nova tecla").setView(in)
          .setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int w)
            {
              String k = in.getText().toString();
              if (k.length() == 0) return;
              cks.add(k);
              SharedPreferences.Editor e = prefs.edit();
              juloo.keyboard2.prefs.ShkPrefsApi.saveCustomKeys(e, cks);
              commit(e);
              refreshKeep();
            }
          }).setNegativeButton("Cancelar", null).show();
      }
    }), lp(-1, -2, 6));
    c.addView(ghost(extraOpen ? "Ocultar lista de teclas internas" : "Teclas internas (" + juloo.keyboard2.prefs.ShkPrefsApi.extraKeyNames().length + ")…",
      new View.OnClickListener() {
        public void onClick(View v) { extraOpen = !extraOpen; refreshKeep(); }
      }), lp(-1, -2, 6));
    if (extraOpen)
    {
      for (String n : juloo.keyboard2.prefs.ShkPrefsApi.extraKeyNames())
      {
        final String key = juloo.keyboard2.prefs.ShkPrefsApi.extraPrefKey(n);
        Switch sw = new Switch(this);
        sw.setText(juloo.keyboard2.prefs.ShkPrefsApi.extraTitle(getResources(), n));
        sw.setTextColor(TX);
        sw.setTypeface(Theme.getKeyFont(this));
        sw.setChecked(prefs.getBoolean(key, juloo.keyboard2.prefs.ShkPrefsApi.extraDefault(n)));
        sw.setPadding(0, dp(8), 0, dp(8));
        sw.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
          public void onCheckedChanged(android.widget.CompoundButton b, boolean on)
          {
            commit(prefs.edit().putBoolean(key, on));
          }
        });
        c.addView(sw);
      }
    }
  }

  /* ================= 性 Desempenho ================= */

  ProgressBarHolder pbDraw, pbMem;

  static final class ProgressBarHolder
  {
    final android.widget.ProgressBar bar;
    ProgressBarHolder(android.widget.ProgressBar b) { bar = b; }
    void set(int pct, int color)
    {
      bar.setProgress(Math.max(0, Math.min(100, pct)));
      bar.setProgressTintList(android.content.res.ColorStateList.valueOf(color));
    }
  }

  ProgressBarHolder newBar(LinearLayout parent)
  {
    android.widget.ProgressBar b = new android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
    b.setMax(100);
    b.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF26262E));
    parent.addView(b, lp(-1, dp(8), 6));
    return new ProgressBarHolder(b);
  }

  LinearLayout statCard(String kanji, String title)
  {
    LinearLayout c = newCard();
    LinearLayout h = new LinearLayout(this);
    h.setGravity(Gravity.CENTER_VERTICAL);
    h.addView(badge(kanji, ac));
    TextView t = tv(title, 14, TX, true);
    t.setPadding(dp(10), 0, 0, 0);
    h.addView(t);
    c.addView(h);
    return c;
  }

  void buildPerf()
  {
    section("性", "Desempenho");
    LinearLayout d = statCard("描", "Desenho do teclado");
    stDraw = tv("—", 15, TX, false);
    d.addView(stDraw, lp(-2, -2, 8));
    pbDraw = newBar(d);
    d.addView(tv("Meta: abaixo de 16 ms por quadro (60 fps).", 11, SUB, false), lp(-2, -2, 4));
    LinearLayout l = statCard("速", "Latência do toque");
    stLatency = tv("—", 15, TX, false);
    l.addView(stLatency, lp(-2, -2, 8));
    l.addView(tv("Tempo entre o toque e o teclado redesenhar.", 11, SUB, false), lp(-2, -2, 4));
    LinearLayout m = statCard("憶", "Memória do teclado");
    stMem = tv("—", 15, TX, false);
    m.addView(stMem, lp(-2, -2, 8));
    pbMem = newBar(m);
    LinearLayout k = statCard("打", "Digitação");
    stKeys = tv("—", 15, TX, false);
    k.addView(stKeys, lp(-2, -2, 8));
    stWpm = tv("—", 15, TX, false);
    k.addView(stWpm, lp(-2, -2, 4));

    section("調", "Ajustes");
    LinearLayout o = newCard();
    toggle(o, "Modo leve (sem imagem nem kanji no fundo)", "shk_perf_light", false);
    o.addView(tv("Se notar atraso em aparelhos fracos, ligue o modo leve. Ele desliga o fundo decorativo sem apagar suas escolhas.", 12, SUB, false), lp(-1, -2, 2));
    o.addView(ghost("Zerar estatísticas", new View.OnClickListener() {
      public void onClick(View v) { ShkPerf.reset(ShkActivity.this); updatePerf(); }
    }), lp(-1, -2, 10));
    o.addView(tv("Os quadros e a latência só contam enquanto o teclado está sendo usado.", 11, SUB, false), lp(-1, -2, 8));
    updatePerf();
    ticker = new Runnable() {
      public void run()
      {
        if (tab != 3) return;
        updatePerf();
        handler.postDelayed(this, 1000);
      }
    };
    handler.postDelayed(ticker, 1000);
  }

  void updatePerf()
  {
    if (stDraw == null) return;
    double dm = ShkPerf.drawAvgMs;
    stDraw.setText(String.format(Locale.US, "%.2f ms médio  ·  %.1f ms pico  ·  %d quadros", dm, ShkPerf.drawMaxMs, ShkPerf.draws));
    int pct = (int)(dm / 16.6 * 100);
    pbDraw.set(pct, pct < 50 ? 0xFF2ECC71 : pct < 90 ? 0xFFFFB020 : 0xFFE5173F);
    stLatency.setText(ShkPerf.latCount == 0 ? "Sem dados ainda — digite algo." : String.format(Locale.US, "%.0f ms médio", ShkPerf.latAvgMs));
    try
    {
      ActivityManager am = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
      Debug.MemoryInfo[] mi = am.getProcessMemoryInfo(new int[] { android.os.Process.myPid() });
      int pss = mi[0].getTotalPss() / 1024;
      Runtime rt = Runtime.getRuntime();
      long used = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
      long max = rt.maxMemory() / (1024 * 1024);
      stMem.setText(pss + " MB no total  ·  heap " + used + " / " + max + " MB");
      int mp = (int)(used * 100 / Math.max(1, max));
      pbMem.set(mp, mp < 50 ? 0xFF2ECC71 : mp < 80 ? 0xFFFFB020 : 0xFFE5173F);
    }
    catch (Throwable t) { stMem.setText("indisponível"); }
    stKeys.setText("Hoje: " + ShkPerf.today(this) + "  ·  Total: " + ShkPerf.total(this) + "  ·  Sessão: " + ShkPerf.sessionKeys);
    stWpm.setText(String.format(Locale.US, "Agora: %.0f ppm  ·  Melhor: %.0f ppm", ShkPerf.wpm(), ShkPerf.bestWpm(this)));
  }

  /* ================= 保 Backup ================= */

  void buildBackup()
  {
    section("保", "Backup das configurações");
    LinearLayout c = newCard();
    c.addView(tv("Guarda tudo: layouts, teclas extras, cores, fundo, kanji e as configurações clássicas. Dicionários e histórico da área de transferência não entram.", 12, SUB, false));
    Switch sw = new Switch(this);
    sw.setText("Incluir imagem de fundo");
    sw.setTextColor(TX);
    sw.setChecked(includeImage);
    sw.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
      public void onCheckedChanged(android.widget.CompoundButton b, boolean on) { includeImage = on; }
    });
    c.addView(sw, lp(-1, -2, 8));
    c.addView(button("Exportar para arquivo", new View.OnClickListener() {
      public void onClick(View v)
      {
        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/json");
        i.putExtra(Intent.EXTRA_TITLE, ShkBackup.fileName());
        startActivityForResult(i, REQ_EXPORT);
      }
    }), lp(-1, -2, 8));
    c.addView(ghost("Importar de arquivo", new View.OnClickListener() {
      public void onClick(View v)
      {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        startActivityForResult(i, REQ_IMPORT);
      }
    }), lp(-1, -2, 8));

    section("点", "Pontos de restauração");
    LinearLayout s = newCard();
    s.addView(button("Criar ponto agora", new View.OnClickListener() {
      public void onClick(View v)
      {
        try { ShkBackup.snapshot(ShkActivity.this, "manual"); refreshKeep(); }
        catch (Exception e) { toast("Erro: " + e.getMessage()); }
      }
    }));
    List<File> snaps = ShkBackup.snapshots(this);
    if (snaps.isEmpty())
      s.addView(tv("Nenhum ponto salvo ainda. Um ponto é criado automaticamente antes de cada importação.", 12, SUB, false), lp(-1, -2, 8));
    SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    for (final File f : snaps)
    {
      LinearLayout r = new LinearLayout(this);
      r.setGravity(Gravity.CENTER_VERTICAL);
      r.setPadding(0, dp(8), 0, 0);
      String nm = f.getName().startsWith("antes") ? "Antes de restaurar" : "Manual";
      TextView t = tv(nm + "\n" + df.format(new Date(f.lastModified())) + " · " + (f.length() / 1024) + " KB", 13, TX, false);
      r.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
      TextView rs = tv("Restaurar", 13, ac, true);
      rs.setPadding(dp(10), dp(8), dp(10), dp(8));
      rs.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v)
        {
          new AlertDialog.Builder(ShkActivity.this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("Restaurar este ponto?")
            .setMessage("As configurações atuais serão substituídas.")
            .setPositiveButton("Restaurar", new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface d, int w)
              {
                try
                {
                  String json = ShkBackup.readFile(f);
                  ShkBackup.snapshot(ShkActivity.this, "antes-restaurar");
                  ShkBackup.restore(ShkActivity.this, json);
                  toast("Restaurado");
                  show(tab);
                }
                catch (Exception e) { toast("Erro: " + e.getMessage()); }
              }
            }).setNegativeButton("Cancelar", null).show();
        }
      });
      r.addView(rs);
      TextView del = tv("✕", 16, SUB, false);
      del.setPadding(dp(10), dp(8), dp(4), dp(8));
      del.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) { f.delete(); refreshKeep(); }
      });
      r.addView(del);
      s.addView(r);
    }
  }

  void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }

  @Override
  protected void onActivityResult(int req, int res, Intent data)
  {
    super.onActivityResult(req, res, data);
    if (res != RESULT_OK || data == null || data.getData() == null) return;
    Uri u = data.getData();
    try
    {
      if (req == REQ_EXPORT)
      {
        java.io.OutputStream o = getContentResolver().openOutputStream(u);
        o.write(ShkBackup.export(this, includeImage).getBytes("UTF-8"));
        o.close();
        toast("Backup salvo");
      }
      else if (req == REQ_IMPORT)
      {
        String json = ShkBackup.readStream(getContentResolver().openInputStream(u));
        ShkBackup.snapshot(this, "antes-restaurar");
        int n = ShkBackup.restore(this, json);
        toast("Backup restaurado (" + n + " configurações)");
        show(tab);
      }
      else if (req == REQ_IMAGE)
      {
        if (ShkStyle.saveBackground(this, u))
        {
          commit(prefs.edit().putBoolean("shk_bg_on", true));
          refreshKeep();
        }
        else toast("Não foi possível ler essa imagem");
      }
    }
    catch (Exception e) { toast("Erro: " + e.getMessage()); }
  }
}
