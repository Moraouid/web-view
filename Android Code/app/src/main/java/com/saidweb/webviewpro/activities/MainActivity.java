package com.saidweb.webviewpro.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.saidweb.webviewpro.fragments.FragmentWebView;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Launch the game HTML fragment directly
        FragmentWebView fragment = new FragmentWebView();
        Bundle data = new Bundle();
        data.putString("name", "Hello Game");
        data.putString("type", "assets");
        data.putString("url", "hello_game.html");
        fragment.setArguments(data);

        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit();
    }

    // --- The following legacy navigation/menu code is now commented out for the single-game version ---
/*
    private void loadNavigationMenu(boolean selectFirstItem) {
        // recyclerView = findViewById(R.id.recyclerView);
        // recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));
        // recyclerView.setItemAnimator(new DefaultItemAnimator());
        // AdapterNavigation adapterNavigation = new AdapterNavigation(this, new ArrayList<>());
        // adapterNavigation.setListData(items);
        // recyclerView.setAdapter(adapterNavigation);
        // if (selectFirstItem) {
        //     recyclerView.postDelayed(() -> Objects.requireNonNull(recyclerView.findViewHolderForAdapterPosition(0)).itemView.performClick(), 10);
        // }
        // adapterNavigation.setOnItemClickListener((v, obj, position) -> drawerLayout.closeDrawer(GravityCompat.START));
    }

    public void setupNavigationDrawer(Toolbar toolbar) {
        // actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, 0, 0) {
        //     @Override
        //     public void onDrawerOpened(View drawerView) { super.onDrawerOpened(drawerView); }
        //     @Override
        //     public void onDrawerClosed(View drawerView) { super.onDrawerClosed(drawerView); }
        // };
        // drawerLayout.addDrawerListener(actionBarDrawerToggle);
        // actionBarDrawerToggle.syncState();
    }

    public void notificationOpenHandler() {
        // loadWebPage("Hello Game", "assets", "hello_game.html");
    }
*/
// --- End of legacy code ---
}
