package juloo.keyboard2;

import android.graphics.Path;
import android.graphics.RectF;

/** Formatos geométricos das teclas. [build] devolve null para "round" (usa drawRoundRect). */
public final class ShkShape
{
  /** Lê "x,y x,y ..." (0 a 100). Devolve pares x,y ou null se inválido. */
  public static float[] parse(String s)
  {
    if (s == null) return null;
    String[] parts = s.trim().split("[\\s;]+");
    if (parts.length < 3) return null;
    float[] r = new float[parts.length * 2];
    for (int i = 0; i < parts.length; i++)
    {
      String[] xy = parts[i].split(",");
      if (xy.length != 2) return null;
      try
      {
        float x = Float.parseFloat(xy[0].trim()), y = Float.parseFloat(xy[1].trim());
        r[i * 2] = Math.max(0f, Math.min(100f, x));
        r[i * 2 + 1] = Math.max(0f, Math.min(100f, y));
      }
      catch (NumberFormatException e) { return null; }
    }
    return r;
  }

  public static Path build(Path p, RectF r, String shape, float size, float[] poly)
  {
    if (shape == null || shape.equals("round"))
      return null;
    float w = r.width(), h = r.height();
    float s = Math.max(0f, Math.min(size, 0.5f)) * Math.min(w, h);
    float l = r.left, t = r.top, rt = r.right, b = r.bottom;
    p.reset();
    switch (shape)
    {
      case "square":
        p.addRect(r, Path.Direction.CW);
        return p;
      case "pill":
        p.addRoundRect(r, h / 2f, h / 2f, Path.Direction.CW);
        return p;
      case "chamfer":
        p.moveTo(l + s, t); p.lineTo(rt - s, t); p.lineTo(rt, t + s); p.lineTo(rt, b - s);
        p.lineTo(rt - s, b); p.lineTo(l + s, b); p.lineTo(l, b - s); p.lineTo(l, t + s);
        p.close();
        return p;
      case "notch":
        p.moveTo(l + s, t); p.lineTo(rt, t); p.lineTo(rt, b - s); p.lineTo(rt - s, b);
        p.lineTo(l, b); p.lineTo(l, t + s);
        p.close();
        return p;
      case "slant":
        p.moveTo(l + s, t); p.lineTo(rt, t); p.lineTo(rt - s, b); p.lineTo(l, b);
        p.close();
        return p;
      case "hex":
      {
        float hs = Math.min(s, w / 2f), cy = (t + b) / 2f;
        p.moveTo(l + hs, t); p.lineTo(rt - hs, t); p.lineTo(rt, cy);
        p.lineTo(rt - hs, b); p.lineTo(l + hs, b); p.lineTo(l, cy);
        p.close();
        return p;
      }
      case "leaf":
      {
        float big = h / 2f, small = Math.max(h * 0.06f, s * 0.25f);
        p.addRoundRect(r, new float[] { big, big, small, small, big, big, small, small },
            Path.Direction.CW);
        return p;
      }
      case "custom":
        if (poly == null) return null;
        for (int i = 0; i < poly.length / 2; i++)
        {
          float x = l + poly[i * 2] / 100f * w, y = t + poly[i * 2 + 1] / 100f * h;
          if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
        }
        p.close();
        return p;
      default:
        return null;
    }
  }
}
