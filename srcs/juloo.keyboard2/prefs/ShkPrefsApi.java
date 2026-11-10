package juloo.keyboard2.prefs;

import android.content.SharedPreferences;
import android.content.res.Resources;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import juloo.keyboard2.*;

/** Ponte pública para o app SHIKKOKU usar layouts e teclas extras sem as telas clássicas. */
public final class ShkPrefsApi
{
  public static List<LayoutsPreference.Layout> loadLayouts(SharedPreferences p)
  {
    List<LayoutsPreference.Layout> l = ListGroupPreference.load_from_preferences(
        LayoutsPreference.KEY, p, LayoutsPreference.DEFAULT, LayoutsPreference.SERIALIZER);
    if (l == null || l.isEmpty())
      return new ArrayList<LayoutsPreference.Layout>(LayoutsPreference.DEFAULT);
    return new ArrayList<LayoutsPreference.Layout>(l);
  }

  public static void saveLayouts(SharedPreferences.Editor e, List<LayoutsPreference.Layout> items)
  {
    LayoutsPreference.save_to_preferences(e, items);
  }

  public static String[] layoutNames(Resources res)
  {
    return res.getStringArray(R.array.pref_layout_values);
  }

  public static String[] layoutLabels(Resources res)
  {
    return res.getStringArray(R.array.pref_layout_entries);
  }

  public static String layoutLabel(Resources res, LayoutsPreference.Layout l)
  {
    if (l instanceof LayoutsPreference.NamedLayout)
    {
      String n = ((LayoutsPreference.NamedLayout)l).name;
      List<String> names = Arrays.asList(layoutNames(res));
      int i = names.indexOf(n);
      return i < 0 ? n : layoutLabels(res)[i];
    }
    if (l instanceof LayoutsPreference.CustomLayout)
    {
      LayoutsPreference.CustomLayout c = (LayoutsPreference.CustomLayout)l;
      if (c.parsed != null && c.parsed.name != null && !c.parsed.name.equals(""))
        return c.parsed.name;
      return res.getString(R.string.pref_layout_e_custom);
    }
    return res.getString(R.string.pref_layout_e_system);
  }

  public static String initialCustomLayout(Resources res)
  {
    try { return Utils.read_all_utf8(res.openRawResource(R.raw.latn_qwerty_us)); }
    catch (Exception e) { return ""; }
  }

  public static String validateLayout(String text)
  {
    try { KeyboardData.load_string_exn(text); return null; }
    catch (Exception e) { return e.getMessage(); }
  }

  public static LayoutsPreference.Layout customLayout(String xml)
  {
    return LayoutsPreference.CustomLayout.parse(xml);
  }

  public static boolean isCustom(LayoutsPreference.Layout l) { return l instanceof LayoutsPreference.CustomLayout; }
  public static String customXml(LayoutsPreference.Layout l) { return ((LayoutsPreference.CustomLayout)l).xml; }
  public static LayoutsPreference.Layout namedLayout(String n)
  {
    if (n.equals("system")) return new LayoutsPreference.SystemLayout();
    return new LayoutsPreference.NamedLayout(n);
  }

  /* ---- teclas extras ---- */

  public static List<String> loadCustomKeys(SharedPreferences p)
  {
    List<String> l = ListGroupPreference.load_from_preferences(
        CustomExtraKeysPreference.KEY, p, new ArrayList<String>(), CustomExtraKeysPreference.SERIALIZER);
    return l == null ? new ArrayList<String>() : new ArrayList<String>(l);
  }

  public static void saveCustomKeys(SharedPreferences.Editor e, List<String> items)
  {
    ListGroupPreference.save_to_preferences(CustomExtraKeysPreference.KEY, e, items,
        CustomExtraKeysPreference.SERIALIZER);
  }

  public static String[] extraKeyNames() { return ExtraKeysPreference.extra_keys; }
  public static boolean extraDefault(String n) { return ExtraKeysPreference.default_checked(n); }
  public static String extraPrefKey(String n) { return ExtraKeysPreference.pref_key_of_key_name(n); }

  public static String extraTitle(Resources res, String n)
  {
    KeyValue kv = KeyValue.getKeyByName(n);
    String t = ExtraKeysPreference.key_title(n, kv);
    String d = ExtraKeysPreference.key_description(res, n);
    return d != null ? t + " (" + d + ")" : t;
  }
}
