

package com.sickboots.sickvideos;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubeStandalonePlayer;

import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshAttacher;

public class DrawerActivity extends Activity implements Util.PullToRefreshListener {
  private DrawerLayout mDrawerLayout;
  private ListView mDrawerList;
  private ActionBarDrawerToggle mDrawerToggle;

  private PullToRefreshAttacher mPullToRefreshAttacher;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_drawer);

    String[] names = new String[]{"Favorites", "Likes", "History", "Uploads", "Watch Later"};
    mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
    mDrawerList = (ListView) findViewById(R.id.left_drawer);

    // set a custom shadow that overlays the main content when the drawer opens
    mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

    // set up the drawer's list view with items and click listener
    mDrawerList.setAdapter(new ArrayAdapter<String>(this,
        R.layout.drawer_list_item, names));
    mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

    // enable ActionBar app icon to behave as action to toggle nav drawer
    getActionBar().setDisplayHomeAsUpEnabled(true);
    getActionBar().setHomeButtonEnabled(true);

    // set custom color
//    String customColor = ApplicationHub.instance().getPref(ApplicationHub.ACTION_BAR_COLOR, null);
//    if (customColor != null) {
//      int color = Integer.parseInt(customColor);
//      getActionBar().setBackgroundDrawable(new ColorDrawable(color));
//    }

    // ActionBarDrawerToggle ties together the the proper interactions
    // between the sliding drawer and the action bar app icon
    mDrawerToggle = new ActionBarDrawerToggle(
        this,                  /* host Activity */
        mDrawerLayout,         /* DrawerLayout object */
        R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
        R.string.drawer_open,  /* "open drawer" description for accessibility */
        R.string.drawer_close  /* "close drawer" description for accessibility */
    ) {
      public void onDrawerClosed(View view) {
        invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
      }

      public void onDrawerOpened(View drawerView) {
        invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
      }
    };
    mDrawerLayout.setDrawerListener(mDrawerToggle);

    if (savedInstanceState == null) {
      selectItem(0, false);
    }

    // general app tweaks
//  Util.activateStrictMode(this);
    Util.ignoreObsoleteCapacitiveMenuButton(this);

    // This shit is buggy, must be created in onCreate of the activity, can't be created in the fragment.
    mPullToRefreshAttacher = PullToRefreshAttacher.get(this);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public void onBackPressed() {
    if (getFragmentManager().getBackStackEntryCount() == 0) {

      // hides the video player if visible
      ApplicationHub.instance().sendNotification(ApplicationHub.BACK_BUTTON_NOTIFICATION);

      // do nothing, we don't want the app to disappear
      return;
    }

    super.onBackPressed();
  }

  // Add the Refreshable View and provide the refresh listener;
  @Override
  public void addRefreshableView(View theView, PullToRefreshAttacher.OnRefreshListener listener) {
    mPullToRefreshAttacher.addRefreshableView(theView, listener);
  }

  @Override
  public void setRefreshComplete() {
    mPullToRefreshAttacher.setRefreshComplete();
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      // called when playing a movie, could fail and this dialog shows the user how to fix it
      case YouTubeAPI.REQ_PLAYER_CODE:
        if (resultCode != RESULT_OK) {
          YouTubeInitializationResult errorReason = YouTubeStandalonePlayer.getReturnedInitializationResult(data);
          if (errorReason.isUserRecoverableError()) {
            errorReason.getErrorDialog(this, 0).show();
          } else {
            String errorMessage = String.format("PLAYER ERROR!! - %s", errorReason.toString());
            Util.toast(this, errorMessage);
          }
        }

        break;
      default:
        super.onActivityResult(requestCode, resultCode, data);
    }
  }

  /* Called whenever we call invalidateOptionsMenu() */
  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    // If the nav drawer is open, hide action items related to the content view
    boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
    menu.findItem(R.id.action_websearch).setVisible(!drawerOpen);
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    Intent intent;

    // The action bar home/up action should open or close the drawer.
    // ActionBarDrawerToggle will take care of this.
    if (mDrawerToggle.onOptionsItemSelected(item)) {
      return true;
    }
    // Handle action buttons
    switch (item.getItemId()) {
      case R.id.action_settings:
        intent = new Intent();
        intent.setClass(DrawerActivity.this, SettingsActivity.class);
        startActivity(intent);

        return true;
      case R.id.action_tabs:
        intent = new Intent();
        intent.setClass(DrawerActivity.this, TabActivity.class);
        startActivity(intent);

        return true;
      case R.id.action_websearch:
        intent = new Intent();
        intent.setClass(DrawerActivity.this, PlaylistChooserActivity.class);
        startActivity(intent);

        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  /* The click listner for ListView in the navigation drawer */
  private class DrawerItemClickListener implements ListView.OnItemClickListener {
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
      selectItem(position, true);
    }
  }

  private void selectItem(int position, boolean animate) {
    Fragment fragment = null;

    switch (position) {
      case 0:
        fragment = YouTubeFragment.relatedFragment(YouTubeAPI.RelatedPlaylistType.FAVORITES);
        break;
      case 1:
        fragment = YouTubeFragment.relatedFragment(YouTubeAPI.RelatedPlaylistType.LIKES);
        break;
      case 2:
        fragment = YouTubeFragment.relatedFragment(YouTubeAPI.RelatedPlaylistType.WATCHED);
        break;
      case 3:
        fragment = YouTubeFragment.relatedFragment(YouTubeAPI.RelatedPlaylistType.UPLOADS);
        break;
      case 4:
        fragment = new ColorPickerFragment();
//        fragment = YouTubeFragment.relatedFragment(YouTubeAPI.RelatedPlaylistType.WATCHLATER);
        break;
    }

    Util.showFragment(this, fragment, R.id.content_frame, animate ? 1 : 0, false);

    // update selected item and title, then close the drawer
    mDrawerList.setItemChecked(position, true);
    mDrawerLayout.closeDrawer(mDrawerList);
  }

  /**
   * When using the ActionBarDrawerToggle, you must call it during
   * onPostCreate() and onConfigurationChanged()...
   */

  @Override
  protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    // Sync the toggle state after onRestoreInstanceState has occurred.
    mDrawerToggle.syncState();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    // Pass any configuration change to the drawer toggls
    mDrawerToggle.onConfigurationChanged(newConfig);
  }

}
