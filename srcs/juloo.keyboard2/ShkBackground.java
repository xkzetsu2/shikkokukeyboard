package juloo.keyboard2;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

/** Fundo do teclado: cor + imagem + kanji decorativo + linha de destaque. */
public final class ShkBackground extends Drawable
{
  private final Paint _p = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Rect _src = new Rect();
  private int _alpha = 255;
  private final int _baseColor;
  private final Bitmap _bmp;
  private final int _imgAlpha;
  private final int _dim;
  private final boolean _kanji;
  private final String _text;
  private final String _mode;
  private final int _kSize;
  private final int _kAlpha;
  private final int _kColor;
  private final int _kStyle;
  private final boolean _topLine;
  private final int _accent;
  private final float _density;
  private final String _art;

  /** Troca o fundo da view por um [ShkBackground] baseado no fundo atual. */
  public static void install(Context ctx, View v)
  {
    Drawable cur = v.getBackground();
    if (cur instanceof ShkBackground)
      return;
    int base = (cur instanceof ColorDrawable) ? ((ColorDrawable)cur).getColor() : 0xFF000000;
    ShkStyle st = ShkStyle.of(ctx);
    base = st.c("keyboard", base);
    v.setBackground(new ShkBackground(ctx, st, base));
  }

  public ShkBackground(Context ctx, ShkStyle st, int baseColor)
  {
    SharedPreferences p = st.prefs();
    _density = ctx.getResources().getDisplayMetrics().density;
    _baseColor = baseColor;
    _accent = st.accent;
    if (st.isForced())
    {
      // Miniatura de um preset: não lê as prefs do usuário.
      ShkStyle.Preset q = st.pr;
      _art = q != null ? q.art : "none";
      _topLine = q != null && q.topline;
      _imgAlpha = 0; _dim = 0; _bmp = null;
      _kanji = q != null;
      _text = q != null ? q.kanji : "漆黒";
      _mode = q != null ? q.mode : "center";
      _kSize = 80; _kAlpha = 12 * 255 / 100; _kColor = _accent; _kStyle = 0;
      return;
    }
    boolean light = p.getBoolean("shk_perf_light", false) || !p.getBoolean("shk_bg_master", true);
    _art = light ? "none" : p.getString("shk_bg_art", "none");
    _topLine = p.getBoolean("shk_topline", false);
    _imgAlpha = p.getInt("shk_bg_alpha", 60) * 255 / 100;
    _dim = p.getInt("shk_bg_dim", 25) * 255 / 100;
    _bmp = (!light && p.getBoolean("shk_bg_on", false)) ? ShkStyle.loadBackground(ctx) : null;
    _kanji = !light && p.getBoolean("shk_k_on", false);
    String t = p.getString("shk_k_text", "漆黒");
    _text = (t == null || t.isEmpty()) ? "漆黒" : t;
    _mode = p.getString("shk_k_mode", "center");
    _kSize = p.getInt("shk_k_size", 80);
    _kAlpha = p.getInt("shk_k_alpha", 12) * 255 / 100;
    _kColor = p.contains("shk_k_color") ? p.getInt("shk_k_color", _accent) : _accent;
    _kStyle = p.getInt("shk_k_style", 0);
  }

  /** Arte minimalista procedural: traços finos e baixa opacidade, sem preenchimentos pesados. */
  private void drawArt(Canvas cv, Rect b)
  {
    if (_art == null || _art.equals("none")) return;
    String art = _art;
    if (art.equals("sun")) art = "moon";
    else if (art.equals("grid")) art = "circuit";
    else if (art.equals("petal")) art = "sakura";
    Paint a = new Paint(Paint.ANTI_ALIAS_FLAG);
    a.setColor(_accent);
    a.setStyle(Paint.Style.STROKE);
    a.setStrokeCap(Paint.Cap.ROUND);
    a.setStrokeJoin(Paint.Join.ROUND);
    int al = _alpha;
    float W = b.width(), H = b.height(), d = _density;
    cv.save();
    cv.clipRect(b);
    if (art.equals("enso"))
    {
      float cx = b.left + W * 0.80f, cy = b.top + H * 0.50f, r = H * 0.58f;
      android.graphics.RectF o = new android.graphics.RectF(cx - r, cy - r, cx + r, cy + r);
      a.setStrokeWidth(H * 0.075f);
      a.setAlpha(44 * al / 255);
      cv.drawArc(o, 200, 290, false, a);
      android.graphics.RectF o2 = new android.graphics.RectF(o);
      o2.inset(H * 0.035f, H * 0.035f);
      a.setStrokeWidth(H * 0.03f);
      a.setAlpha(26 * al / 255);
      cv.drawArc(o2, 215, 250, false, a);
    }
    else if (art.equals("moon"))
    {
      float cx = b.left + W * 0.82f, cy = b.top + H * 0.46f, r = H * 0.42f;
      a.setStrokeWidth(1.2f * d);
      a.setAlpha(80 * al / 255);
      cv.drawCircle(cx, cy, r, a);
      a.setStyle(Paint.Style.FILL);
      a.setAlpha(30 * al / 255);
      cv.drawCircle(cx, cy, r * 0.72f, a);
      a.setStyle(Paint.Style.STROKE);
      a.setStrokeWidth(1f * d);
      a.setAlpha(26 * al / 255);
      for (int i = 0; i < 4; i++)
      {
        float y = b.top + H * (0.60f + 0.1f * i);
        cv.drawLine(b.left + W * (0.06f + 0.1f * i), y, b.left + W * (0.52f + 0.07f * i), y, a);
      }
    }
    else if (art.equals("bamboo"))
    {
      float[] xs = { 0.05f, 0.12f, 0.90f, 0.96f };
      float[] ws = { 3f, 2f, 3.5f, 2f };
      for (int i = 0; i < xs.length; i++)
      {
        float x = b.left + W * xs[i];
        a.setStrokeWidth(ws[i] * d);
        a.setAlpha(32 * al / 255);
        cv.drawLine(x, b.top - 4 * d, x, b.bottom + 4 * d, a);
        a.setStrokeWidth(1.2f * d);
        a.setAlpha(60 * al / 255);
        for (float y = b.top + H * (0.14f + 0.07f * i); y < b.bottom; y += H * 0.32f)
          cv.drawLine(x - ws[i] * d * 1.6f, y, x + ws[i] * d * 1.6f, y, a);
      }
    }
    else if (art.equals("mount"))
    {
      float[] xs = { 0f, 0.16f, 0.28f, 0.45f, 0.62f, 0.78f, 1f };
      float[][] ys = { { 0.55f, 0.28f, 0.50f, 0.20f, 0.52f, 0.40f, 0.60f },
                       { 0.78f, 0.58f, 0.74f, 0.52f, 0.76f, 0.66f, 0.82f } };
      int[] alp = { 70, 38 };
      for (int k = 0; k < 2; k++)
      {
        android.graphics.Path pth = new android.graphics.Path();
        for (int i = 0; i < xs.length; i++)
        {
          float x = b.left + W * xs[i], y = b.top + H * ys[k][i];
          if (i == 0) pth.moveTo(x, y); else pth.lineTo(x, y);
        }
        a.setStrokeWidth(1.3f * d);
        a.setAlpha(alp[k] * al / 255);
        cv.drawPath(pth, a);
      }
      a.setStrokeWidth(1.2f * d);
      a.setAlpha(60 * al / 255);
      cv.drawCircle(b.left + W * 0.70f, b.top + H * 0.20f, H * 0.11f, a);
    }
    else if (art.equals("wave"))
    {
      a.setStrokeWidth(1f * d);
      a.setAlpha(32 * al / 255);
      float r = 28 * d;
      int row = 0;
      for (float y = b.top + r; y < b.bottom + r * 2; y += r * 0.55f, row++)
        for (float x = b.left - (row % 2) * r; x < b.right + r * 2; x += r * 2)
          for (int k = 1; k <= 3; k++)
            cv.drawCircle(x, y, r * k / 3f, a);
    }
    else if (art.equals("circuit"))
    {
      a.setStrokeWidth(1.2f * d);
      for (int i = 0; i < 6; i++)
      {
        float y0 = b.top + H * (0.14f + 0.14f * i);
        float bend = H * 0.09f * (i % 2 == 0 ? 1 : -1);
        float x1 = b.left + W * (0.18f + 0.08f * i), x2 = b.left + W * (0.55f + 0.07f * i);
        android.graphics.Path pth = new android.graphics.Path();
        pth.moveTo(b.left, y0);
        pth.lineTo(x1, y0);
        pth.lineTo(x1 + Math.abs(bend), y0 + bend);
        pth.lineTo(x2, y0 + bend);
        a.setStyle(Paint.Style.STROKE);
        a.setAlpha(52 * al / 255);
        cv.drawPath(pth, a);
        a.setAlpha(80 * al / 255);
        cv.drawCircle(x2 + 4 * d, y0 + bend, 2.4f * d, a);
      }
    }
    else if (art.equals("dots"))
    {
      a.setStyle(Paint.Style.FILL);
      float step = 14 * d;
      for (float y = b.top + step / 2; y < b.bottom; y += step)
        for (float x = b.left + step / 2; x < b.right; x += step)
        {
          float f = (x - b.left) / W;
          a.setAlpha((int)((16 + 54 * f) * al / 255));
          cv.drawCircle(x, y, d * (0.6f + 0.8f * f), a);
        }
    }
    else if (art.equals("rain"))
    {
      a.setStyle(Paint.Style.FILL);
      a.setTextSize(13 * d);
      a.setTypeface(Typeface.MONOSPACE);
      String glyphs = "ｱｲｳｴｵｶｷｸｹｺ01ｻｼｽｾｿ";
      int cols = (int)(W / (15 * d));
      for (int c = 0; c < cols; c++)
      {
        int seed = c * 7919 + 13;
        int len = 4 + (seed % 7);
        float x = b.left + c * 15 * d + 3 * d;
        float y0 = b.top + ((seed * 31) % 100) / 100f * H;
        for (int i = 0; i < len; i++)
        {
          float y = y0 + i * 14 * d;
          if (y > b.bottom) y -= H + 20 * d;
          a.setAlpha((i == len - 1 ? 110 : 16 + i * 6) * al / 255);
          cv.drawText(String.valueOf(glyphs.charAt((seed + i * 3) % glyphs.length())), x, y, a);
        }
      }
    }
    else if (art.equals("sakura"))
    {
      float[][] pos = { { 0.12f, 0.30f, 10f }, { 0.86f, 0.25f, 12f }, { 0.74f, 0.72f, 8f },
                        { 0.30f, 0.78f, 9f }, { 0.50f, 0.20f, 7f } };
      for (float[] f : pos)
      {
        float cx = b.left + W * f[0], cy = b.top + H * f[1], r = f[2] * d;
        a.setStyle(Paint.Style.STROKE);
        a.setStrokeWidth(1f * d);
        a.setAlpha(58 * al / 255);
        for (int k = 0; k < 5; k++)
        {
          cv.save();
          cv.rotate(k * 72f, cx, cy);
          cv.drawOval(new android.graphics.RectF(cx - r * 0.38f, cy - r * 1.1f, cx + r * 0.38f, cy), a);
          cv.restore();
        }
        a.setStyle(Paint.Style.FILL);
        a.setAlpha(70 * al / 255);
        cv.drawCircle(cx, cy, r * 0.14f, a);
      }
    }
    cv.restore();
  }

  @Override
  public void draw(Canvas canvas)
  {
    Rect b = getBounds();
    if (b.isEmpty())
      return;
    _p.setStyle(Paint.Style.FILL);
    _p.setColor(_baseColor);
    _p.setAlpha(Color.alpha(_baseColor) * _alpha / 255);
    canvas.drawRect(b, _p);
    drawArt(canvas, b);
    if (_bmp != null)
    {
      int w = _bmp.getWidth(), h = _bmp.getHeight();
      float s = Math.max((float)b.width() / w, (float)b.height() / h);
      float sw = b.width() / s, sh = b.height() / s;
      _src.set((int)((w - sw) / 2), (int)((h - sh) / 2),
          (int)((w + sw) / 2), (int)((h + sh) / 2));
      _p.setColor(0xFFFFFFFF);
      _p.setAlpha(_imgAlpha * _alpha / 255);
      canvas.drawBitmap(_bmp, _src, b, _p);
      if (_dim > 0)
      {
        _p.setColor(0xFF000000);
        _p.setAlpha(_dim * _alpha / 255);
        canvas.drawRect(b, _p);
      }
    }
    if (_kanji)
      drawKanji(canvas, b);
    if (_topLine)
    {
      _p.setColor(_accent);
      _p.setAlpha(_alpha);
      canvas.drawRect(b.left, b.top, b.right, b.top + 2 * _density, _p);
    }
  }

  static List<String> chars(String s)
  {
    List<String> l = new ArrayList<String>();
    int i = 0;
    while (i < s.length())
    {
      int n = Character.charCount(s.codePointAt(i));
      String c = s.substring(i, i + n);
      if (!c.trim().isEmpty())
        l.add(c);
      i += n;
    }
    return l;
  }

  private void drawKanji(Canvas canvas, Rect b)
  {
    List<String> cs = chars(_text);
    if (cs.isEmpty())
      return;
    Paint k = new Paint(Paint.ANTI_ALIAS_FLAG);
    k.setColor(_kColor);
    k.setAlpha(_kAlpha * _alpha / 255);
    k.setTypeface(_kStyle == 1 ? Typeface.create(Typeface.SERIF, Typeface.BOLD)
        : _kStyle == 2 ? Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        : Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
    k.setTextAlign(Paint.Align.CENTER);
    float scale = _kSize / 80f;
    canvas.save();
    canvas.clipRect(b);
    if (_mode.equals("tile"))
    {
      float size = b.height() * 0.30f * scale;
      k.setTextSize(size);
      float step = size * 1.25f;
      int n = 0;
      int row = 0;
      for (float y = b.top + size; y < b.bottom + size; y += step, row++)
      {
        float off = (row % 2 == 0) ? 0 : step / 2f;
        for (float x = b.left + off; x < b.right + step; x += step)
        {
          canvas.drawText(cs.get(n % cs.size()), x, y, k);
          n++;
        }
      }
    }
    else if (_mode.equals("vertical"))
    {
      float size = Math.min(b.height() / (float)cs.size() * 0.85f, b.height() * 0.45f) * scale;
      k.setTextSize(size);
      float x = b.right - size * 0.9f;
      float y = b.top + size * 0.95f;
      for (String c : cs)
      {
        canvas.drawText(c, x, y, k);
        y += size * 1.05f;
      }
      k.setStrokeWidth(_density);
      canvas.drawLine(x - size * 0.7f, b.top, x - size * 0.7f, b.bottom, k);
    }
    else
    {
      float size = b.height() * 0.85f * scale;
      k.setTextSize(size);
      float total = k.measureText(_text);
      float maxW = b.width() * 0.96f;
      if (total > maxW)
      {
        size = size * maxW / total;
        k.setTextSize(size);
      }
      float y = b.centerY() - (k.ascent() + k.descent()) / 2f;
      canvas.drawText(_text, b.centerX(), y, k);
    }
    canvas.restore();
  }

  @Override public void setAlpha(int a) { _alpha = a; invalidateSelf(); }
  @Override public int getAlpha() { return _alpha; }
  @Override public void setColorFilter(ColorFilter f) {}
  @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
