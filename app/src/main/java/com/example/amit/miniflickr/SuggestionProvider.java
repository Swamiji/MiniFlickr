package com.example.amit.miniflickr;

import android.content.SearchRecentSuggestionsProvider;

/**
 * Created by Amit on 1/6/2018.
 */

public class SuggestionProvider extends SearchRecentSuggestionsProvider {
    // part of content uri which is defined in AndroidManifest.xml
    public static final String AUTHORITY = "com.android.amit.miniflickr" +
            ".SuggestionProvider";

    // suggestion mode which gives recent queries
    public static final int MODE = DATABASE_MODE_QUERIES;

    public SuggestionProvider() {
        setupSuggestions(AUTHORITY, MODE);
    }
}
