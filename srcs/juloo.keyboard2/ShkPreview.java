package juloo.keyboard2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.preference.PreferenceManager;
import android.view.View;
import juloo.keyboard2.prefs.LayoutsPreference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.util.DisplayMetrics;

/** Pré-visualização do teclado: usa o tema real, a opacidade e o fundo atuais. */
public final class ShkPreview extends View
{
  static final String[][] ROWS = {
    { "q", "w", "e", "r", "t", "y", "u", "i", "o", "p" },
    { "a", "s", "d", "f", "g", "h", "j", "k", "l" },
    { "⇧", "z", "x", "c", "v", "b", "n", "m", "⌫" },
    { "?123", ",", "␣", ".", "⏎" } };

  private ShkBackground _bg;
  private ShkStyle _st;
  private final String _preset; // null = estilo atual do usuário
  private final int _wDp, _hDp;
  private int _kbAlpha = 255, _keyAlpha = 255;
  private final Paint _p = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF _r = new RectF();
  private final android.graphics.Path _path = new android.graphics.Path();
  private String _shape = "round";
  private float _shapeSize = 0.25f;
  private float[] _poly;
  private final float _d;
  private List<KeyboardData.Row> _rows;
  private float _kw, _sumUnits;
  private SharedPreferences _sp;

  public ShkPreview(Context c)
  {
    this(c, null, 0, 190);
  }

  /** Miniatura de um preset (id "off"/"system" também valem). */
  public ShkPreview(Context c, String presetId, int widthDp, int heightDp)
  {
    super(c);
    _preset = presetId;
    _wDp = widthDp;
    _hDp = heightDp;
    _d = c.getResources().getDisplayMetrics().density;
    refresh();
  }

  public void refresh()
  {
    Context c = getContext();
    _st = _preset == null ? ShkStyle.ofDefault(c) : ShkStyle.forPreset(c, _preset);
    _bg = new ShkBackground(c, _st, _st.eff("keyboard"));
    _shape = _st.shape();
    _shapeSize = _st.shapeSizePct() / 100f;
    _poly = ShkShape.parse(_st.customPoly());
    _rows = null;
    requestLayout();
    if (_preset == null)
    {
      android.content.SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(c);
      _sp = p;
      loadRows(c, p);
      _kbAlpha = p.getInt("keyboard_opacity", 100) * 255 / 100;
      _keyAlpha = p.getInt("key_opacity", 100) * 255 / 100;
    }
    invalidate();
  }

  float num(String k, float d)
  {
    Object o = _sp == null ? null : _sp.getAll().get(k);
    return o instanceof Number ? ((Number)o).floatValue() : d;
  }

  private void loadRows(Context c, SharedPreferences sp)
  {
    try
    {
      Resources res = c.getResources();
      List<KeyboardData> ls = LayoutsPreference.load_from_preferences(res, sp);
      KeyboardData kd = ls.isEmpty() ? null : ls.get(0);
      if (kd == null) kd = LayoutsPreference.layout_of_string(res, "latn_qwerty_us");
      if (kd == null) return;
      List<KeyboardData.Row> rs = new ArrayList<KeyboardData.Row>(kd.rows);
      String nr = sp.getString("number_row", "no_number_row");
      if (!"no_number_row".equals(nr) && !kd.embedded_number_row)
        rs.add(0, KeyboardData.load_row(res, "symbols".equals(nr) ? R.xml.number_row : R.xml.number_row_no_symbols));
      if (kd.bottom_row)
        rs.add(KeyboardData.load_row(res, R.xml.bottom_row));
      float kw = 0, su = 0;
      for (KeyboardData.Row r : rs)
      {
        kw = Math.max(kw, r.keysWidth);
        su += r.shift + r.height;
      }
      if (kw <= 0 || su <= 0) return;
      _kw = kw;
      _sumUnits = su;
      _rows = rs;
    }
    catch (Throwable t) { _rows = null; }
  }

  private boolean landscape()
  {
    return getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
  }

  private float unitPx()
  {
    DisplayMetrics dm = getResources().getDisplayMetrics();
    float base = Math.min(dm.heightPixels, dm.widthPixels * 16f / 9f);
    float pct = landscape() ? num("keyboard_height_landscape", 50) : num("keyboard_height", 35);
    return base * pct / 395f;
  }

  @Override
  protected void onMeasure(int w, int h)
  {
    int width = _wDp > 0 ? (int)(_wDp * _d) : MeasureSpec.getSize(w);
    if (_preset == null && _rows != null)
    {
      DisplayMetrics dm = getResources().getDisplayMetrics();
      float scale = width / (float)dm.widthPixels;
      float mb = num(landscape() ? "margin_bottom_landscape" : "margin_bottom_portrait", landscape() ? 3 : 7) * dm.density;
      int hh = (int)((_sumUnits * unitPx() + mb) * scale);
      setMeasuredDimension(width, Math.max(hh, (int)(70 * _d)));
      return;
    }
    setMeasuredDimension(width, (int)(_hDp * _d));
  }

  @Override
  protected void onDraw(Canvas canvas)
  {
    if (_preset == null && _rows != null)
    {
      try { drawFull(canvas); return; }
      catch (Throwable t) { /* cai para a versão simples */ }
    }
    drawThumb(canvas);
  }

  /** Teclado completo: layout real, margens, altura, formatos, bordas e rótulos. */
  void drawFull(Canvas canvas)
  {
    DisplayMetrics dm = getResources().getDisplayMetrics();
    int W = getWidth(), H = getHeight();
    _bg.setAlpha(_kbAlpha);
    _bg.setBounds(0, 0, W, H);
    _bg.draw(canvas);
    float scale = W / (float)dm.widthPixels;
    boolean land = landscape();
    float hm = num(land ? "horizontal_margin_landscape" : "horizontal_margin_portrait", land ? 28 : 3) * dm.density * scale;
    float unit = unitPx() * scale;
    float keyUnit = (W - 2 * hm) / _kw;
    float gapV = num("key_vertical_margin", 1.5f) / 100f * unit;
    float gapH = num("key_horizontal_margin", 2f) / 100f * keyUnit;
    float charSize = num("character_size", 1.15f) * (land ? 1.25f : 1f);
    float base = Math.min(unit - gapV, (W / 10f - gapH) * 1.5f) * charSize;
    float mainSize = base * 0.33f, subSize = base * 0.22f;
    int lblA = (int)num("label_brightness", 100) * 255 / 100;
    boolean borderCfg = _sp.getBoolean("border_config", false);
    Map<String, ShkStyle.KeyOv> ov = _st.keyOverrides();
    String shape = _st.shape();
    float shapeSize = _st.shapeSizePct() / 100f;
    float[] poly = ShkShape.parse(_st.customPoly());
    android.graphics.Typeface kf = Theme.getKeyFont(getContext());
    float y = 0;
    for (KeyboardData.Row row : _rows)
    {
      y += row.shift * unit;
      float rowH = row.height * unit;
      float x = hm;
      for (KeyboardData.Key k : row.keys)
      {
        x += k.shift * keyUnit;
        float kw = k.width * keyUnit;
        float x0 = x;
        x += kw;
        if (k.keys[0] == null && k.keys[1] == null && k.keys[2] == null && k.keys[3] == null
            && k.keys[4] == null && k.keys[5] == null && k.keys[6] == null
            && k.keys[7] == null && k.keys[8] == null)
          continue;
        String role = k.role == KeyboardData.Key.Role.Action ? "action"
            : k.role == KeyboardData.Key.Role.Space_bar ? "space" : "key";
        if (k.role == KeyboardData.Key.Role.Suggestion) continue;
        _r.set(x0 + gapH / 2, y + gapV / 2, x0 + kw - gapH / 2, y + rowH - gapV / 2);
        int bg = _st.eff(role);
        int fg = _st.eff("label");
        int sub = _st.eff("sub");
        int kbc = _st.eff("border");
        String name = k.keys[0] != null ? k.keys[0].getString() : "";
        ShkStyle.KeyOv o = ov.get(name);
        if (o != null && o.bg != null) bg = o.bg.intValue();
        if (o != null && o.fg != null) fg = o.fg.intValue();
        if (o != null && o.bc != null) kbc = o.bc.intValue();
        float radius;
        if (_st.radiusSet()) radius = _st.radiusDp() * dm.density * scale;
        else if (borderCfg) radius = num("custom_border_radius", 0) / 100f * (x0 + kw - x0);
        else radius = _st.themeRadiusDp() * dm.density * scale;
        float bwDp;
        if (!_st.bordersOn(role)) bwDp = 0;
        else if (_st.borderSet()) bwDp = _st.borderX10() / 10f;
        else if (borderCfg && role.equals("key")) bwDp = num("custom_border_line_width", 0);
        else bwDp = _st.themeBorderDp();
        float bw = bwDp * dm.density * scale;
        if (bwDp > 0 && bw < 1f) bw = 1f;
        if (o != null && o.bw != null) bw = o.bw.intValue() == 0 ? 0 : Math.max(1f, o.bw.intValue() / 10f * dm.density * scale);
        if (bw > 0) _r.inset(bw / 2f, bw / 2f);
        _p.setStyle(Paint.Style.FILL);
        _p.setColor(bg);
        _p.setAlpha(android.graphics.Color.alpha(bg) * _keyAlpha / 255);
        android.graphics.Path sp = ShkShape.build(_path, _r, shape, shapeSize, poly);
        if (sp != null) canvas.drawPath(sp, _p); else canvas.drawRoundRect(_r, radius, radius, _p);
        if (bw > 0)
        {
          _p.setStyle(Paint.Style.STROKE);
          _p.setStrokeWidth(bw);
          _p.setColor(kbc);
          if (sp != null) canvas.drawPath(sp, _p); else canvas.drawRoundRect(_r, radius, radius, _p);
        }
        _p.setStyle(Paint.Style.FILL);
        // rótulos
        float pad = Math.max(1.5f, unit * 0.08f);
        for (int i = 0; i < 9; i++)
        {
          KeyValue kv = k.keys[i];
          if (kv == null) continue;
          String t = kv.getString();
          if (t == null || t.length() == 0) continue;
          boolean main = i == 0;
          float ts = (main ? mainSize : subSize) * (kv.hasFlagsAny(KeyValue.FLAG_SMALLER_FONT) ? 0.75f : 1f);
          _p.setTypeface(kv.hasFlagsAny(KeyValue.FLAG_KEY_FONT) ? kf : android.graphics.Typeface.DEFAULT);
          _p.setTextSize(ts);
          _p.setColor(main ? fg : sub);
          _p.setAlpha(android.graphics.Color.alpha(main ? fg : sub) * lblA / 255);
          int len = t.length();
          if (len > 3 && kv.getKind() == KeyValue.Kind.String) len = 3;
          float cx = _r.centerX(), ty;
          Paint.Align al;
          switch (i)
          {
            case 1: case 3: case 5: al = Paint.Align.LEFT; cx = _r.left + pad; break;
            case 2: case 4: case 6: al = Paint.Align.RIGHT; cx = _r.right - pad; break;
            default: al = Paint.Align.CENTER; break;
          }
          switch (i)
          {
            case 1: case 2: case 7: ty = _r.top + pad - _p.ascent(); break;
            case 3: case 4: case 8: ty = _r.bottom - pad - _p.descent(); break;
            default: ty = _r.centerY() - (_p.ascent() + _p.descent()) / 2f; break;
          }
          _p.setTextAlign(al);
          canvas.drawText(t, 0, len, cx, ty, _p);
        }
        _p.setTypeface(android.graphics.Typeface.DEFAULT);
      }
      y += rowH;
    }
  }

  void drawThumb(Canvas canvas)
  {
    int W = getWidth(), H = getHeight();
    _bg.setAlpha(_kbAlpha);
    _bg.setBounds(0, 0, W, H);
    _bg.draw(canvas);
    boolean thumb = _preset != null;
    Map<String, ShkStyle.KeyOv> ov = _st.keyOverrides();
    float pad = (thumb ? 4 : 6) * _d, gap = (thumb ? 3 : 4) * _d;
    float rowH = (H - pad * 2) / ROWS.length;
    float radius = (_st.radiusDp() >= 0 ? _st.radiusDp() : _st.themeRadiusDp()) * _d;
    float bw = (_st.borderX10() >= 0 ? _st.borderX10() / 10f : _st.themeBorderDp()) * _d;
    for (int r = 0; r < ROWS.length; r++)
    {
      String[] row = ROWS[r];
      float total = 0;
      for (String k : row) total += weight(k);
      float unit = (W - pad * 2) / total;
      float x = pad;
      for (String k : row)
      {
        float w = unit * weight(k);
        int slot = (k.equals("⏎") || k.equals("⇧") || k.equals("⌫") || k.equals("?123")) ? 2
            : k.equals("␣") ? 3 : 1;
        int bg = _st.eff(slot == 2 ? "action" : slot == 3 ? "space" : "key");
        int fg = _st.eff("label");
        ShkStyle.KeyOv o = ov.get(k);
        if (o != null && o.bg != null) bg = o.bg.intValue();
        if (o != null && o.fg != null) fg = o.fg.intValue();
        float kbw = _st.bordersOn(slot == 2 ? "action" : slot == 3 ? "space" : "key") ? bw : 0;
        int kbc = _st.eff("border");
        if (o != null && o.bc != null) kbc = o.bc.intValue();
        if (o != null && o.bw != null) kbw = o.bw.intValue() / 10f * _d;
        _r.set(x + gap / 2, pad + r * rowH + gap / 2, x + w - gap / 2, pad + (r + 1) * rowH - gap / 2);
        _p.setStyle(Paint.Style.FILL);
        _p.setColor(bg);
        _p.setAlpha(android.graphics.Color.alpha(bg) * _keyAlpha / 255);
        android.graphics.Path sp = ShkShape.build(_path, _r, _shape, _shapeSize, _poly);
        if (sp != null) canvas.drawPath(sp, _p); else canvas.drawRoundRect(_r, radius, radius, _p);
        if (kbw > 0)
        {
          _p.setStyle(Paint.Style.STROKE);
          _p.setStrokeWidth(kbw);
          _p.setColor(kbc);
          if (sp != null) canvas.drawPath(sp, _p); else canvas.drawRoundRect(_r, radius, radius, _p);
        }
        _p.setStyle(Paint.Style.FILL);
        _p.setColor(fg);
        _p.setTextAlign(Paint.Align.CENTER);
        _p.setTextSize(rowH * 0.36f);
        canvas.drawText(k, _r.centerX(), _r.centerY() - (_p.ascent() + _p.descent()) / 2f, _p);
        x += w;
      }
    }
  }

  static float weight(String k)
  {
    if (k.equals("␣")) return 4f;
    if (k.equals("?123") || k.equals("⏎") || k.equals("⇧") || k.equals("⌫")) return 1.5f;
    return 1f;
  }
}
