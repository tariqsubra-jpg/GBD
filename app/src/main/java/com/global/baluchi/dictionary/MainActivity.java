package com.global.baluchi.dictionary;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private EditText vocabularySearchInput;
    private TextView dictionaryContentDisplay;
    private TextView clearSearchButton;
    private TextView infoAppButton;
    
    private final Map<String, String> keywordToIdRegistry = new HashMap<>();
    private final Map<String, String> idToContentRegistry = new HashMap<>();
    private final Map<String, String> idToBalochiWord = new HashMap<>();
    private final Map<String, String> idToForeignWord = new HashMap<>();

    private String currentLanguagePrefix = "en";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vocabularySearchInput = findViewById(R.id.vocabularySearchInput);
        dictionaryContentDisplay = findViewById(R.id.dictionaryContentDisplay);
        clearSearchButton = findViewById(R.id.clearSearchButton);
        infoAppButton = findViewById(R.id.infoAppButton);

        detectKeyboardLanguageAndReloadData();

        clearSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                vocabularySearchInput.setText("");
            }
        });

        infoAppButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayInfoContentPanel();
            }
        });

        vocabularySearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                detectKeyboardLanguageAndReloadData();
                executeBiDirectionalSearch(s.toString().trim().toLowerCase());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void displayInfoContentPanel() {
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(getAssets().open("info.txt"), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line).append("\n");
            }
            r.close();
            dictionaryContentDisplay.setText(sb.toString());

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com"));
            startActivity(browserIntent);

        } catch (Exception e) {
            dictionaryContentDisplay.setText("Welcome! GBD is completely free.\nVisit: https://github.com");
        }
    }

    private void detectKeyboardLanguageAndReloadData() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                InputMethodSubtype subtype = imm.getCurrentInputMethodSubtype();
                if (subtype != null) {
                    String langTag = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        langTag = subtype.getLanguageTag();
                    }
                    
                    if (langTag == null || langTag.isEmpty()) {
                        @SuppressWarnings("deprecation")
                        String deprecatedLocale = subtype.getLocale();
                        langTag = deprecatedLocale;
                    }

                    if (langTag != null && !langTag.isEmpty()) {
                        String detectedPrefix = langTag.substring(0, 2).toLowerCase();
                        
                        if (!detectedPrefix.equals(currentLanguagePrefix)) {
                            currentLanguagePrefix = detectedPrefix;
                            keywordToIdRegistry.clear();
                            idToContentRegistry.clear();
                            idToBalochiWord.clear();
                            idToForeignWord.clear();
                        }
                    }
                }
            }
        } catch (Exception e) {}

        if (keywordToIdRegistry.isEmpty()) {
            loadRelationalDataStructures(currentLanguagePrefix);
        }
    }

    private void loadRelationalDataStructures(String langCode) {
        String indexFile = langCode + "_balochi_index.txt";
        String dataFile = langCode + "_balochi_data.txt";

        try {
            try {
                getAssets().open(indexFile).close();
            } catch (Exception e) {
                indexFile = "en_balochi_index.txt";
                dataFile = "en_balochi_data.txt";
            }

            BufferedReader r1 = new BufferedReader(new InputStreamReader(getAssets().open(indexFile), "UTF-8"));
            String line;
            while ((line = r1.readLine()) != null) {
                String[] parts = line.split(",", 3);
                if (parts.length >= 3) {
                    String id = parts[0].trim();
                    String balochiWord = parts[1].trim().toLowerCase();
                    String foreignWord = parts[2].trim().toLowerCase();
                    
                    keywordToIdRegistry.put(balochiWord, id);
                    keywordToIdRegistry.put(foreignWord, id);
                    
                    idToBalochiWord.put(id, parts[1].trim());
                    idToForeignWord.put(id, parts[2].trim());
                }
            }
            r1.close();

            BufferedReader r2 = new BufferedReader(new InputStreamReader(getAssets().open(dataFile), "UTF-8"));
            while ((line = r2.readLine()) != null) {
                String[] parts = line.split(",", 3);
                if (parts.length >= 3) {
                    String id = parts[0].trim();
                    String foreignContent = parts[1].trim();
                    String balochiContent = parts[2].trim();
                    
                    String structuredPayload = "📖 FOREIGN EXPLANATION:\n" + foreignContent + 
                                                "\n\n" + 
                                                "🌴 BALOCHI EXPLANATION:\n" + balochiContent;
                                             
                    idToContentRegistry.put(id, structuredPayload);
                }
            }
            r2.close();

        } catch (Exception e) {
            if (dictionaryContentDisplay != null) {
                dictionaryContentDisplay.setText("System Notification: Missing dataset asset records for mapping.");
            }
        }
    }

    private void executeBiDirectionalSearch(String query) {
        if (query.isEmpty()) {
            dictionaryContentDisplay.setText("Welcome to Global Baluchi Dictionary. Definitions will display right here.");
            return;
        }

        if (keywordToIdRegistry.containsKey(query)) {
            String targetPointerId = keywordToIdRegistry.get(query);
            if (idToContentRegistry.containsKey(targetPointerId)) {
                
                String bWord = idToBalochiWord.get(targetPointerId);
                String fWord = idToForeignWord.get(targetPointerId);
                String coreDefinition = idToContentRegistry.get(targetPointerId);
                
                String dynamicFormattedDisplay = "🔍 " + fWord.toUpperCase() + " / " + bWord + "\n" +
                                                 "━━━━━━━━━━━━━━━━━━━━━━\n" +
                                                 coreDefinition;
                                                 
                dictionaryContentDisplay.setText(dynamicFormattedDisplay);
            } else {
                dictionaryContentDisplay.setText("System Mapping Error: Row ID pointer references missing data sectors.");
            }
        } else {
            dictionaryContentDisplay.setText("The word '" + query + "' is not registered inside the GBD system records yet.");
        }
    }
}

