package com.kothakey.ime;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.kothakey.R;
import com.kothakey.ime.dict.LearnedWordsStore;
import com.kothakey.ime.keyboard.Arrangement;
import com.kothakey.ime.ui.KeyType;
import com.kothakey.ime.ui.KothaKeyTheme;

public class SettingsActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_settings);

    KothaKeyPrefs prefs = new KothaKeyPrefs(this);
    Switch numberRow = findViewById(R.id.number_row);
    Switch sound = findViewById(R.id.sound);
    Switch vibrate = findViewById(R.id.vibrate);
    Switch autoPunct = findViewById(R.id.auto_punct);
    Switch doubleSpace = findViewById(R.id.double_space);
    RadioGroup layouts = findViewById(R.id.layout_group);

    autoPunct.setChecked(prefs.autoPunctuation());
    doubleSpace.setChecked(prefs.doubleSpacePeriod());
    autoPunct.setOnCheckedChangeListener((v, on) -> prefs.setAutoPunctuation(on));
    doubleSpace.setOnCheckedChangeListener((v, on) -> prefs.setDoubleSpacePeriod(on));
    numberRow.setChecked(prefs.numberRow());
    sound.setChecked(prefs.sound());
    vibrate.setChecked(prefs.vibrate());
    layouts.check(
        prefs.arrangement() == Arrangement.QWERTY ? R.id.layout_qwerty : R.id.layout_azerty);

    bindColorway(prefs);
    bindFont(prefs);
    numberRow.setOnCheckedChangeListener((v, on) -> prefs.setNumberRow(on));
    sound.setOnCheckedChangeListener((v, on) -> prefs.setSound(on));
    vibrate.setOnCheckedChangeListener((v, on) -> prefs.setVibrate(on));
    findViewById(R.id.bengali_guide)
        .setOnClickListener(v -> startActivity(new Intent(this, BengaliGuideActivity.class)));
    findViewById(R.id.clear_learned)
        .setOnClickListener(
            v -> {
              LearnedWordsStore.delete(LearnedWordsStore.file(this));
              LearnedWordsStore.delete(LearnedWordsStore.named(this, "latin_learned.txt"));
              LearnedWordsStore.delete(LearnedWordsStore.named(this, "next_latin.txt"));
              LearnedWordsStore.delete(LearnedWordsStore.named(this, "next_bn.txt"));
              prefs.bumpLearnedEpoch();
              Toast.makeText(this, R.string.settings_learned_cleared, Toast.LENGTH_SHORT).show();
            });
    layouts.setOnCheckedChangeListener(
        (group, checkedId) ->
            prefs.setArrangement(
                checkedId == R.id.layout_qwerty ? Arrangement.QWERTY : Arrangement.AZERTY));
  }

  private void bindColorway(KothaKeyPrefs prefs) {
    KothaKeyTheme[] themes = KothaKeyTheme.all();
    String[] names = new String[themes.length];
    int selected = 0;
    for (int i = 0; i < themes.length; i++) {
      names[i] = themes[i].name;
      if (themes[i].id.equals(prefs.colorway())) {
        selected = i;
      }
    }
    Spinner spinner = findViewById(R.id.colorway);
    spinner.setAdapter(
        new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names));
    spinner.setSelection(selected);
    paintPreview(themes[selected]);
    spinner.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            prefs.setColorway(themes[position].id);
            paintPreview(themes[position]);
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {}
        });
  }

  private void paintPreview(KothaKeyTheme theme) {
    TextView preview = findViewById(R.id.color_preview);
    preview.setBackgroundColor(theme.key);
    preview.setTextColor(theme.label);
  }

  private void bindFont(KothaKeyPrefs prefs) {
    String[] ids = KeyType.ids();
    String[] names = new String[ids.length];
    int selected = 0;
    for (int i = 0; i < ids.length; i++) {
      names[i] = KeyType.name(ids[i]);
      if (ids[i].equals(prefs.keyFont())) {
        selected = i;
      }
    }
    Spinner spinner = findViewById(R.id.key_font);
    spinner.setAdapter(
        new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names));
    spinner.setSelection(selected);
    applyPreviewFont(ids[selected]);
    spinner.setOnItemSelectedListener(
        new AdapterView.OnItemSelectedListener() {
          @Override
          public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            prefs.setKeyFont(ids[position]);
            applyPreviewFont(ids[position]);
          }

          @Override
          public void onNothingSelected(AdapterView<?> parent) {}
        });
  }

  private void applyPreviewFont(String id) {
    TextView preview = findViewById(R.id.color_preview);
    Typeface face = KeyType.typeface(this, id);
    preview.setTypeface(face);
  }
}
