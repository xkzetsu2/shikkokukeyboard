package juloo.keyboard2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/** As configurações clássicas foram integradas ao app: redireciona para a aba Ajustes. */
public class SettingsActivity extends Activity
{
  @Override
  public void onCreate(Bundle b)
  {
    super.onCreate(b);
    Intent i = new Intent(this, ShkActivity.class);
    i.putExtra("tab", 6);
    startActivity(i);
    finish();
  }
}
