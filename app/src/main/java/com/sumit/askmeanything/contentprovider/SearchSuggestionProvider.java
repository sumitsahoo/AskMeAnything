package com.sumit.askmeanything.contentprovider;

import android.content.SearchRecentSuggestionsProvider;

/**
 * Created by sumit on 6/12/2016.
 */
public class SearchSuggestionProvider extends SearchRecentSuggestionsProvider {

    public final static String AUTHORITY = "com.sumit.askmeanything.contentprovider.SearchSuggestionProvider";
    public final static int MODE = DATABASE_MODE_QUERIES;

    public SearchSuggestionProvider() {
        setupSuggestions(AUTHORITY, MODE);
    }
}
