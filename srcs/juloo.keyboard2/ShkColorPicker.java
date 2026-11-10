package juloo.keyboard2;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/** Seletor de cor (HSV + alfa + hex + paleta SHIKKOKU). */
public final class ShkColorPicker
{
  public interface Callback { void onPick(int color); }

  static final int[] SWATCHES = {
    0xFFE5173F, 0xFFFF6A00, 0xFFFFB020, 0xFF2ECC71, 0xFF22D3EE, 0xFF3B82F6,
    0xFF8B5CF6, 0xFFEC4899, 0xFFFFFFFF, 0xFFECE9E4, 0xFF8E8E99, 0xFF141418,
    0xFF0A0A0C, 0xFF000000 };

  private final Context c;
  private final float[] hsv = new float[3];
  private int alpha = 255;
  private boolean busy = false;
  private View preview;
  private EditText hex;
  private SeekBar sH, sS, sV, sA;

  private int dp(int v) { return (int)(v * c.getResources().getDisplayMetrics().density); }

  public static void show(Context c, String title, int initial, Callback cb, String neutralLabel, Runnable onNeutral)
  {
    new ShkColorPicker(c).open(title, initial, cb, neutralLabel, onNeutral);
  }

  private ShkColorPicker(Context c) { this.c = c; }

  private int current()
  {
    return Color.HSVToColor(alpha, hsv);
  }

  private SeekBar bar(LinearLayout root, String name, int max, int val)
  {
    TextView t = new TextView(c);
    t.setText(name);
    t.setTextColor(0xFFBBBBC4);
    t.setTextSize(12);
    root.addView(t);
    SeekBar s = new SeekBar(c);
    s.setMax(max);
    s.setProgress(val);
    root.addView(s, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    return s;
  }

  private void sync(boolean fromHex)
  {
    busy = true;
    preview.setBackground(round(current(), 10, 0xFF3A3A44));
    if (!fromHex)
      hex.setText(String.format("#%08X", current()));
    sH.setProgress((int)hsv[0]);
    sS.setProgress((int)(hsv[1] * 100));
    sV.setProgress((int)(hsv[2] * 100));
    sA.setProgress(alpha);
    busy = false;
  }

  /** Paleta SHIKKOKU + cores do sistema (Material You, Android 12+). */
  static int[] allSwatches(Context c)
  {
    if (android.os.Build.VERSION.SDK_INT < 31)
      return SWATCHES;
    int[] sys = {
      android.R.color.system_accent1_200, android.R.color.system_accent1_400,
      android.R.color.system_accent1_600, android.R.color.system_accent2_300,
      android.R.color.system_accent3_300, android.R.color.system_neutral1_100,
      android.R.color.system_neutral1_700, android.R.color.system_neutral1_900 };
    int[] r = new int[SWATCHES.length + sys.length];
    for (int i = 0; i < sys.length; i++) r[i] = c.getColor(sys[i]);
    System.arraycopy(SWATCHES, 0, r, sys.length, SWATCHES.length);
    return r;
  }

  static GradientDrawable round(int color, int dpRadius, int stroke)
  {
    GradientDrawable g = new GradientDrawable();
    g.setColor(color);
    g.setCornerRadius(dpRadius * 3f);
    g.setStroke(2, stroke);
    return g;
  }

  private void open(String title, int initial, final Callback cb, String neutralLabel, final Runnable onNeutral)
  {
    Color.colorToHSV(initial, hsv);
    alpha = Color.alpha(initial);
    LinearLayout root = new LinearLayout(c);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(20), dp(12), dp(20), 0);
    preview = new View(c);
    root.addView(preview, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));
    hex = new EditText(c);
    hex.setTextColor(0xFFFFFFFF);
    hex.setSingleLine(true);
    hex.setTextSize(15);
    root.addView(hex);
    sH = bar(root, "色相 Matiz", 360, 0);
    sS = bar(root, "彩度 Saturação", 100, 0);
    sV = bar(root, "明度 Brilho", 100, 0);
    sA = bar(root, "透明 Opacidade", 255, 255);
    SeekBar.OnSeekBarChangeListener l = new SeekBar.OnSeekBarChangeListener() {
      public void onProgressChanged(SeekBar s, int p, boolean user)
      {
        if (!user || busy) return;
        hsv[0] = sH.getProgress();
        hsv[1] = sS.getProgress() / 100f;
        hsv[2] = sV.getProgress() / 100f;
        alpha = sA.getProgress();
        sync(false);
      }
      public void onStartTrackingTouch(SeekBar s) {}
      public void onStopTrackingTouch(SeekBar s) {}
    };
    sH.setOnSeekBarChangeListener(l);
    sS.setOnSeekBarChangeListener(l);
    sV.setOnSeekBarChangeListener(l);
    sA.setOnSeekBarChangeListener(l);
    hex.addTextChangedListener(new TextWatcher() {
      public void beforeTextChanged(CharSequence s, int a, int b, int d) {}
      public void onTextChanged(CharSequence s, int a, int b, int d) {}
      public void afterTextChanged(Editable e)
      {
        if (busy) return;
        try
        {
          String t = e.toString().trim();
          if (t.length() < 7) return;
          int col = Color.parseColor(t);
          Color.colorToHSV(col, hsv);
          alpha = Color.alpha(col);
          sync(true);
        }
        catch (Exception ex) {}
      }
    });
    LinearLayout sw = new LinearLayout(c);
    sw.setOrientation(LinearLayout.HORIZONTAL);
    sw.setPadding(0, dp(10), 0, 0);
    android.widget.HorizontalScrollView hs = new android.widget.HorizontalScrollView(c);
    for (final int col : allSwatches(c))
    {
      View v = new View(c);
      v.setBackground(round(col, 8, 0xFF3A3A44));
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(34), dp(34));
      lp.rightMargin = dp(8);
      v.setOnClickListener(new View.OnClickListener() {
        public void onClick(View x)
        {
          Color.colorToHSV(col, hsv);
          alpha = Color.alpha(col);
          sync(false);
        }
      });
      sw.addView(v, lp);
    }
    hs.addView(sw);
    root.addView(hs);
    sync(false);
    AlertDialog.Builder b = new AlertDialog.Builder(c, android.R.style.Theme_Material_Dialog_Alert);
    b.setTitle(title).setView(root);
    b.setPositiveButton("OK", new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface d, int w) { cb.onPick(current()); }
    });
    b.setNegativeButton("Cancelar", null);
    if (neutralLabel != null && onNeutral != null)
      b.setNeutralButton(neutralLabel, new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface d, int w) { onNeutral.run(); }
      });
    b.show();
  }
}
