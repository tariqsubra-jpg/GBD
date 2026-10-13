package com.global.baluchi.dictionary;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private HashMap<String, String> keywordToIdMap = new HashMap<>();
    private HashMap<String, String> idToContentMap = new HashMap<>();
    private EditText searchInput;
    private TextView displayPanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Targeted precisely to vocabularySearchInput and dictionaryContentDisplay
        searchInput = findViewById(R.id.vocabularySearchInput);
        displayPanel = findViewById(R.id.dictionaryContentDisplay);

        initializeMobileDatabase();

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                executeMobileSearch(s.toString().trim().toLowerCase());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void initializeMobileDatabase() {
        try {
            // 1. Read index file using absolute substring indexing instead of split traps
            BufferedReader indexReader = new BufferedReader(new InputStreamReader(getAssets().open("en_balochi_index.txt"), "UTF-8"));
            String line;
            while ((line = indexReader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                int firstComma = line.indexOf(',');
                int lastComma = line.lastIndexOf(',');
                if (firstComma != -1 && lastComma != -1 && firstComma != lastComma) {
                    String id = line.substring(0, firstComma).trim();
                    String word = line.substring(firstComma + 1, lastComma).trim().toLowerCase();
                    keywordToIdMap.put(word, id);
                }
            }
            indexReader.close();

            // 2. Read definition blocks safely using absolute boundary cuts
            BufferedReader dataReader = new BufferedReader(new InputStreamReader(getAssets().open("en_balochi_data.txt"), "UTF-8"));
            while ((line = dataReader.readLine()) != null) {
                int firstComma = line.indexOf(',');
                if (firstComma != -1) {
                    String id = line.substring(0, firstComma).trim();
                    String content = line.substring(firstComma + 1).trim();
                    idToContentMap.put(id, content);
                }
            }
            dataReader.close();
        } catch (Exception e) {
            displayPanel.setText("Initializing data synchronization channels...");
        }
    }

    private void executeMobileSearch(String query) {
        if (query.isEmpty()) {
            displayPanel.setText("Welcome to Global Baluchi Dictionary. Definitions will display right here.");
            return;
        }

        if (keywordToIdMap.containsKey(query)) {
            String targetId = keywordToIdMap.get(query);
            String rawContent = idToContentMap.get(targetId);
            
            if (rawContent != null) {
                // Parse the clean line layout vertically onto your mobile layout panel
                String formattedContent = rawContent.replace(" | ", "\n\n");
                displayPanel.setText(formattedContent);
            }
        } else {
            displayPanel.setText("The word '" + query + "' is not registered inside the GBD mobile records yet.");
        }
    }
}
