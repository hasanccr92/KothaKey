package com.kothakey.ime;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.kothakey.R;

public class SetupActivity extends Activity {

  private static final int GREEN = 0xFF1A6B4A;
  private static final int RED = 0xFFC23B2E;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_setup);

    TextView titleEn = findViewById(R.id.title_en);
    TextView titleBn = findViewById(R.id.title_bn);
    titleEn.setText(coloredBrand("Kotha", "Key"));
    titleBn.setText(coloredBrand("কথা", "কি"));

    Button enable = findViewById(R.id.enable);
    Button choose = findViewById(R.id.choose);
    Button settings = findViewById(R.id.settings);
    Button telegram = findViewById(R.id.telegram);
    EditText tryIt = findViewById(R.id.try_it);

    enable.setOnClickListener(
        v -> startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));
    choose.setOnClickListener(
        v -> {
          InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
          if (imm != null) {
            imm.showInputMethodPicker();
          }
        });
    settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
    findViewById(R.id.bengali_guide)
        .setOnClickListener(v -> startActivity(new Intent(this, BengaliGuideActivity.class)));
    findViewById(R.id.linkedin).setOnClickListener(v -> open(R.string.about_linkedin_url));
    findViewById(R.id.github).setOnClickListener(v -> open(R.string.about_github_url));
    findViewById(R.id.thanks_orbit).setOnClickListener(v -> open(R.string.about_thanks_orbit_url));
    findViewById(R.id.thanks_ask).setOnClickListener(v -> open(R.string.about_thanks_ask_url));
    findViewById(R.id.thanks_avro).setOnClickListener(v -> open(R.string.about_thanks_avro_url));
    findViewById(R.id.thanks_doto).setOnClickListener(v -> open(R.string.about_thanks_doto_url));
    String versionName = "1.0.1";
    try {
      versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
    } catch (Exception ignored) {
    }
    ((TextView) findViewById(R.id.app_version)).setText("v" + versionName);
    telegram.setOnClickListener(
        v -> {
          Intent intent =
              new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.setup_telegram_url)));
          startActivity(intent);
        });
    // Leave the try field unfocused so the brand header stays visible on open.
    tryIt.clearFocus();
  }

  private void open(int urlId) {
    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(urlId))));
  }

  private static CharSequence coloredBrand(String greenPart, String redPart) {
    SpannableString span = new SpannableString(greenPart + redPart);
    span.setSpan(
        new ForegroundColorSpan(GREEN), 0, greenPart.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    span.setSpan(
        new StyleSpan(Typeface.BOLD), 0, greenPart.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    span.setSpan(
        new ForegroundColorSpan(RED),
        greenPart.length(),
        span.length(),
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    span.setSpan(
        new StyleSpan(Typeface.BOLD),
        greenPart.length(),
        span.length(),
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    return span;
  }
}
