package com.example.amit.miniflickr;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.SearchRecentSuggestions;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.GridView;
import android.widget.SearchView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.reginald.swiperefresh.CustomSwipeRefreshLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GalleryActivity extends AppCompatActivity {
    //private static final String TAG = GalleryActivity.class.getSimpleName();
    Toolbar toolbar;
    //SearchView mSearchView;
    private static final String TAG = "GalleryFragment";

    private static final int COLUMN_NUM = 3;
    private static final int ITEM_PER_PAGE = 100;

    private RequestQueue mRq;
    private RecyclerView mRecyclerView;

    private GridLayoutManager mLayoutManager;

    private CustomSwipeRefreshLayout mCustomSwipeRefreshLayout;

    private AdapterGallery mAdapter;
    private ArrayList<GalleryItem> mItems;


    private boolean mLoading = false;
    private boolean mHasMore = true;

    private SearchView mSearchView;
    //26.1
    private GridView mGridView;

    //27.4
//    private ThumbnailDownloader<ImageView> mThumbnailThread;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "----------onCreate----------");
        super.onCreate(savedInstanceState);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);

        //this.getSupportActionBar().hide();
        this.getSupportActionBar().show();
        setContentView(R.layout.activity_gallery);
        // test to fix searchview
        //handleIntent(getIntent());
        /*toolbar=(Toolbar)findViewById(R.id.toolbar);
        toolbar.setTitle("Mini Flicker");
        setSupportActionBar(toolbar);*/
        mRq = Volley.newRequestQueue(getApplicationContext());
//        RequestQueue queue = Volley.newRequestQueue(getActivity());


        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                int totalItem = mLayoutManager.getItemCount();
                Log.d(TAG, "--------onScrolled-----totalItem is " + totalItem);

                int lastItemPos = mLayoutManager.findLastVisibleItemPosition();
                Log.d(TAG, "--------onScrolled-----lastItemPos is " + lastItemPos);

                if (mHasMore && !mLoading && totalItem - 1 != lastItemPos) {
                    startLoading();
                }
            }
        });


        mLayoutManager = new GridLayoutManager(getApplicationContext(), COLUMN_NUM);

        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new AdapterGallery(getApplicationContext(), new ArrayList<GalleryItem>());
//        Log.d("Contact", "--------onCreateView----- generate 100 items for Adapter");
//
//        mAdapter = new GalleryAdapter(getActivity(), Contact.generateSampleList(100));

        mRecyclerView.setAdapter(mAdapter);


        mCustomSwipeRefreshLayout = (CustomSwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        mCustomSwipeRefreshLayout.setOnRefreshListener(
                new CustomSwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        refresh();
                    }
                }
        );


        startLoading();

    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d(TAG, "----------onNewIntent----------");
        setIntent(intent);
        handleIntent(intent);
    }

    @SuppressLint("ApplySharedPref")
    private void handleIntent(Intent intent) {
        Log.d(TAG, "----------handleIntent----------");


        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Log.d(TAG, "---handleIntent----testsearchview---Received a new search query: " + query);

//            PreferenceManager.getDefaultSharedPreferences(this)
//                    .edit()
//                    .putString(FlickrFetchr.PREF_SEARCH_QUERY, query)
//                    .commit();

            SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
                    SuggestionProvider.AUTHORITY, SuggestionProvider.MODE);
            suggestions.saveRecentQuery(query, null);

            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putString(UrlManager.PREF_SEARCH_QUERY, query)
                    .commit();

            /*FragmentManager fm = getSupportFragmentManager();
            Fragment fragment = fm.findFragmentById(R.id.gallery_fragment);

            if (fragment != null) {
                ((GalleryFragment) fragment).refresh();
            }*/
        }
    }

    public void refresh() {
        mAdapter.clear();
        startLoading();
    }

    private void startLoading() {
        Log.d(TAG, "startLoading");
        mLoading = true;

        int totalItem = mLayoutManager.getItemCount();
        final int page = totalItem / ITEM_PER_PAGE + 1;

//        String query = PreferenceManager.
//                getDefaultSharedPreferences(getActivity()).
//                getString(FlickrFetchr.PREF_SEARCH_QUERY, null);

        String query = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext())
                .getString(UrlManager.PREF_SEARCH_QUERY, null);

        Log.d(TAG, "startLoading--testsearchview-----query is----" + query);

        String url = UrlManager.getInstance().getItemUrl(query, page);
//        String url = FlickrFetchr.getInstance().getItemUrl(query , page);

        JsonObjectRequest request = new JsonObjectRequest(url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d(TAG, "onResponse " + response);

                List<GalleryItem> result = new ArrayList<>();

                try {
                    JSONObject photos = response.getJSONObject("photos");

                    if (photos.getInt("pages") == page) {
                        mHasMore = false;
                    }

                    JSONArray photoArr = photos.getJSONArray("photo");
                    for (int i = 0; i < photoArr.length(); i++) {
                        JSONObject itemObj = photoArr.getJSONObject(i);
                        GalleryItem item = new GalleryItem(
                                itemObj.getString("id"),
                                itemObj.getString("secret"),
                                itemObj.getString("server"),
                                itemObj.getString("farm")
                        );

                        result.add(item);
                    }
                } catch (JSONException e) {

                }
                mAdapter.addAll(result);
                mAdapter.notifyDataSetChanged();
                mLoading = false;
                mCustomSwipeRefreshLayout.refreshComplete();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }
        );
        request.setTag(TAG);
        mRq.add(request);


    }

    private void stopLoading() {
        if (mRq != null) {
            mRq.cancelAll(TAG);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        stopLoading();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        mSearchView = (SearchView) searchItem.getActionView();

/**********************/

//        mSearchView.setIconifiedByDefault(true);
//        mSearchView.onActionViewExpanded();// 写上此句后searchView初始是可以点击输入的状态，如果不写，那么就需要点击下放大镜，才能出现输入框
//        mSearchView.setFocusable(false);// 是否获取焦点
//        mSearchView.clearFocus();
//
//        mSearchView.setOnQueryTextListener(new OnQueryTextListener() {
//
//            private String TAG = getClass().getSimpleName();
//
//            /*
//             * 在输入时触发的方法，当字符真正显示到searchView中才触发，像是拼音，在舒服法组词的时候不会触发
//             *
//             * @param queryText
//             *
//             * @return false if the SearchView should perform the default action
//             * of showing any suggestions if available, true if the action was
//             * handled by the listener.
//             */
//            @Override
//            public boolean onQueryTextChange(String queryText) {
//                Log.d(TAG, "onQueryTextChange = " + queryText);
//
//
//
//                // TODO:当searchview中文字改变时进行的操作
//                return true;
//            }
//
//            /*
//             * 输入完成后，提交时触发的方法，一般情况是点击输入法中的搜索按钮才会触发。表示现在正式提交了
//             *
//             * @param queryText
//             *
//             * @return true to indicate that it has handled the submit request.
//             * Otherwise return false to let the SearchView handle the
//             * submission by launching any associated intent.
//             */
//            @Override
//            public boolean onQueryTextSubmit(String queryText) {
//                Log.d(TAG, "onQueryTextSubmit = " + queryText);
//
//                // TODO：当用户提交搜索结果时，需要进行的操作return true;
//                return true;
//            }
//        });
/**********************/

        if (mSearchView != null) {
            Log.d("onCreateOptionsMenu", "---testsearchview---- mSearchView is not null  -----------");

            // search suggestion
            mSearchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
                @Override
                public boolean onSuggestionSelect(int position) {
                    String suggestion = getSuggestion(position);

                    if (mSearchView != null && suggestion != null) {
                        mSearchView.setQuery(suggestion, true);
                    }
                    return true;
                }

                @Override
                public boolean onSuggestionClick(int position) {
                    String suggestion = getSuggestion(position);

                    if (mSearchView != null && suggestion != null) {
                        mSearchView.setQuery(suggestion, true);
                    }

                    return true;
                }

                private String getSuggestion(int position) {
                    String suggest = null;

                    if (mSearchView != null) {
                        Cursor cursor = (Cursor) mSearchView.getSuggestionsAdapter().getItem(position);
                        suggest = cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1));
                    }
                    return suggest;
                }
            });
        }

        SearchManager searchManager = (SearchManager) getApplicationContext().getSystemService(Context.SEARCH_SERVICE);
        ComponentName name=getComponentName();
        SearchableInfo searchInfo = searchManager.getSearchableInfo(name);
        mSearchView.setSearchableInfo(searchInfo);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean selectionHandled = false;

        switch (item.getItemId()) {
            case R.id.menu_item_search:
                Log.d("onOptionsItemSelected", "---testsearchview---- menu search request -----------");

                onSearchRequested();
                selectionHandled = true;
                break;
            /*
            case R.id.menu_item_move:
                if (mRecyclerView != null) {
                    mRecyclerView.smoothScrollToPosition(0);
                }
                selectionHandled = true;
                break;
            case R.id.menu_item_clear:
                Log.d("onOptionsItemSelected", "---testsearchview---- menu clear request -----------");
                SearchRecentSuggestions suggestions = new SearchRecentSuggestions(getActivity(),
                        SuggestionProvider.AUTHORITY, SuggestionProvider.MODE);
                suggestions.clearHistory();
                if (mSearchView != null) {
                    mSearchView.setQuery("", false);
                    mSearchView.setIconified(false);
                }
//                PreferenceManager.getDefaultSharedPreferences(getActivity())
//                        .edit()
//                        .putString(FlickrFetchr.PREF_SEARCH_QUERY, null)
//                        .commit();
//
                PreferenceManager.getDefaultSharedPreferences(getActivity())
                        .edit()
                        .putString(UrlManager.PREF_SEARCH_QUERY, null)
                        .commit();
                refresh();
                selectionHandled = true;
                break;
            */
            /*case R.id.menu_item_delete:
                Log.d("onOptionsItemSelected", "---testsearchview---- menu clear request -----------");

                SearchRecentSuggestions suggestions = new SearchRecentSuggestions(getApplicationContext(),
                        SuggestionProvider.AUTHORITY, SuggestionProvider.MODE);
                suggestions.clearHistory();

                if (mSearchView != null) {
                    mSearchView.setQuery("", false);
                    mSearchView.setIconified(false);
                }

//                PreferenceManager.getDefaultSharedPreferences(getActivity())
//                        .edit()
//                        .putString(FlickrFetchr.PREF_SEARCH_QUERY, null)
//                        .commit();
//
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                        .edit()
                        .putString(UrlManager.PREF_SEARCH_QUERY, null)
                        .commit();

                refresh();
                selectionHandled = true;
                break;
            default:
                selectionHandled = super.onOptionsItemSelected(item);
                break;*/
        }
        return selectionHandled;
    }
}
