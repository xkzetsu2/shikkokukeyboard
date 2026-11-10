package juloo.keyboard2;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Contadores de desempenho e estatísticas de digitação do teclado. */
public final class ShkPerf
{
  public static volatile long draws = 0;
  public static volatile double drawAvgMs = 0;
  public static volatile double drawMaxMs = 0;
  public static volatile double latAvgMs = 0;
  public static volatile long latCount = 0;
  public static volatile long sessionKeys = 0;
  public static volatile long sessionStart = SystemClock.elapsedRealtime();

  private static final long[] _ring = new long[64];
  private static int _ringPos = 0;
  private static int _ringCount = 0;
  private static long _unflushed = 0;
  private static Context _app = null;

  public static void init(Context c)
  {
    _app = c.getApplicationContext() != null ? c.getApplicationContext() : c;
  }

  static SharedPreferences stats(Context c)
  {
    return DirectBootAwarePreferences.get_protected_prefs(c, "shk_stats");
  }

  public static void onDraw(long nanos)
  {
    double ms = nanos / 1e6;
    draws++;
    drawAvgMs += (ms - drawAvgMs) / Math.min(draws, 120);
    if (ms > drawMaxMs) drawMaxMs = ms;
  }

  public static void onLatency(long ms)
  {
    if (ms < 0 || ms > 2000) return;
    latCount++;
    latAvgMs += (ms - latAvgMs) / Math.min(latCount, 60);
  }

  public static synchronized void onKey()
  {
    sessionKeys++;
    _unflushed++;
    _ring[_ringPos] = SystemClock.elapsedRealtime();
    _ringPos = (_ringPos + 1) % _ring.length;
    if (_ringCount < _ring.length) _ringCount++;
    if (_unflushed >= 100) flush();
  }

  /** Palavras por minuto estimadas (5 teclas = 1 palavra) nos últimos 60 s. */
  public static synchronized double wpm()
  {
    long now = SystemClock.elapsedRealtime();
    int n = 0;
    long first = now;
    for (int i = 0; i < _ringCount; i++)
    {
      long t = _ring[i];
      if (now - t <= 60000)
      {
        n++;
        if (t < first) first = t;
      }
    }
    if (n < 5) return 0;
    double span = Math.max(5000, now - first) / 60000.0;
    return (n / 5.0) / span;
  }

  static String today()
  {
    return new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
  }

  public static synchronized void flush()
  {
    if (_app == null || _unflushed == 0) return;
    try
    {
      SharedPreferences s = stats(_app);
      String d = today();
      long total = s.getLong("total", 0) + _unflushed;
      long day = d.equals(s.getString("day", "")) ? s.getLong("today", 0) : 0;
      float best = s.getFloat("best_wpm", 0);
      float w = (float)wpm();
      s.edit().putLong("total", total).putString("day", d)
        .putLong("today", day + _unflushed)
        .putFloat("best_wpm", Math.max(best, w)).apply();
    }
    catch (Throwable t) {}
    _unflushed = 0;
  }

  public static long total(Context c)
  {
    return stats(c).getLong("total", 0) + _unflushed;
  }

  public static long today(Context c)
  {
    SharedPreferences s = stats(c);
    long v = today().equals(s.getString("day", "")) ? s.getLong("today", 0) : 0;
    return v + _unflushed;
  }

  public static float bestWpm(Context c)
  {
    return Math.max(stats(c).getFloat("best_wpm", 0), (float)wpm());
  }

  public static void reset(Context c)
  {
    stats(c).edit().clear().apply();
    _unflushed = 0;
    draws = 0; drawAvgMs = 0; drawMaxMs = 0; latAvgMs = 0; latCount = 0;
    sessionKeys = 0; _ringCount = 0; _ringPos = 0;
    sessionStart = SystemClock.elapsedRealtime();
  }
}
