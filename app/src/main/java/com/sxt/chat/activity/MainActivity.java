package com.sxt.chat.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.StringSignature;
import com.sxt.chat.App;
import com.sxt.chat.R;
import com.sxt.chat.ar.HelloArActivity;
import com.sxt.chat.base.BaseFragment;
import com.sxt.chat.base.TabActivity;
import com.sxt.chat.db.User;
import com.sxt.chat.fragment.ChartFragment;
import com.sxt.chat.fragment.GallaryFragment;
import com.sxt.chat.fragment.HomePageFragment;
import com.sxt.chat.fragment.NewsFragment;
import com.sxt.chat.json.ResponseInfo;
import com.sxt.chat.task.MainService;
import com.sxt.chat.utils.AnimationUtil;
import com.sxt.chat.utils.ArithTool;
import com.sxt.chat.utils.Constants;
import com.sxt.chat.utils.Prefs;
import com.sxt.chat.utils.ScreenCaptureUtil;
import com.sxt.chat.utils.glide.GlideCircleTransformer;
import com.sxt.chat.view.searchview.MaterialSearchView;
import com.sxt.chat.ws.BmobRequest;

import java.util.HashMap;
import java.util.Map;

import cn.bmob.v3.BmobUser;

public class MainActivity extends TabActivity implements View.OnClickListener {

    private boolean isFirst = true;
    private ImageView userIcon;
    private TextView userInfo, userName;
    private LinearLayout tabGroup;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private final long millis = 5 * 60 * 1000L;
    public static String KEY_IS_AUTO_LOGIN = "KEY_IS_AUTO_LOGIN";
    public static final String KEY_IS_WILL_GO_LOGIN_ACTIVITY = "KEY_IS_WILL_GO_LOGIN_ACTIVITY";
    public final String CMD_UPDATE_USER_INFO = this.getClass().getName() + "CMD_UPDATE_USER_INFO";
    private MaterialSearchView searchView;
    private Menu menu;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);

        if (BmobUser.getCurrentUser() == null) {
            startActivity(new Intent(App.getCtx(), LoginActivity.class));
            finish();
        } else {
            setContentView(R.layout.activity_main);
            drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
            tabGroup = (LinearLayout) findViewById(R.id.radio_group);
            findViewById(R.id.basic_info).setOnClickListener(this);
            findViewById(R.id.normal_settings).setOnClickListener(this);
            findViewById(R.id.ocr_scan_id_card).setOnClickListener(this);
            findViewById(R.id.ocr_scan).setOnClickListener(this);
            findViewById(R.id.exoplayer).setOnClickListener(this);
            findViewById(R.id.wifi).setOnClickListener(this);
            findViewById(R.id.notifycation).setOnClickListener(this);
            findViewById(R.id.ar).setOnClickListener(this);
            findViewById(R.id.vr).setOnClickListener(this);
            findViewById(R.id.map).setOnClickListener(this);
            findViewById(R.id.change_login).setOnClickListener(this);
            userIcon = (ImageView) findViewById(R.id.user_icon);
            userInfo = (TextView) findViewById(R.id.user_info);
            userName = (TextView) findViewById(R.id.user_name);

            initDrawer();
            initFragment();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(new Intent(App.getCtx(), MainService.class));
            } else {
                startService(new Intent(App.getCtx(), MainService.class));
            }
        }
    }

    private void initFragment() {
        Map<Integer, BaseFragment> fragmentMap = new HashMap<>();
        fragmentMap.put(0, new HomePageFragment());
        fragmentMap.put(1, new GallaryFragment());
        fragmentMap.put(2, new ChartFragment());
        fragmentMap.put(3, new NewsFragment());

        Map<Integer, RadioButton> tabMap = new HashMap<>();
        for (int i = 0; i < tabGroup.getChildCount(); i++) {
            tabMap.put(i, (RadioButton) tabGroup.getChildAt(i));
        }
        String[] titles = new String[]{getString(R.string.string_tab_home), getString(R.string.string_tab_gallary), getString(R.string.string_tab_chart), getString(R.string.string_tab_news)};
        initFragment(fragmentMap, tabMap, titles, R.id.container, 0);
    }

    private void initDrawer() {
        View searchContainer = getLayoutInflater().inflate(R.layout.toolbar_search, null);
        Toolbar toolbar = searchContainer.findViewById(R.id.toolbar);
        searchView = searchContainer.findViewById(R.id.search_view);
        replaceToolbarView(searchContainer, toolbar);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close) {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
//                View content = drawerLayout.getChildAt(0); // 得到contentView
//                int offset = (int) (drawerView.getWidth() * slideOffset);
//                content.setTranslationX(offset);
            }
        };
        drawerToggle.syncState();//实现箭头和三条杠图案切换和侧滑菜单开关的同步
        drawerLayout.addDrawerListener(drawerToggle);

        searchView.setVoiceSearch(false);
        searchView.setEllipsize(false);
        searchView.setBackground(ContextCompat.getDrawable(this, R.drawable.white_solid_round_4));
        searchView.setSuggestions(getResources().getStringArray(R.array.query_suggestions));
        searchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                //Do some magic
                Snackbar.make(findViewById(R.id.container), "Query: " + query, Snackbar.LENGTH_LONG).show();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                //Do some magic
                return false;
            }
        });

        searchView.setOnSearchViewListener(new MaterialSearchView.SearchViewListener() {
            @Override
            public void onSearchViewShown() {
                //Do some magic
            }

            @Override
            public void onSearchViewClosed() {
                //Do some magic
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkUser();
    }

    private void checkUser() {
        BmobRequest.getInstance(this).updateUserInfo(CMD_UPDATE_USER_INFO);
    }

    @Override
    public void onMessage(ResponseInfo resp) {
        if (ResponseInfo.OK == resp.getCode()) {
            if (CMD_UPDATE_USER_INFO.equals(resp.getCmd())) {
                updateUserInfo(BmobUser.getCurrentUser(User.class));
                loadAD();
            }
        } else {
            if (CMD_UPDATE_USER_INFO.equals(resp.getCmd())) {
                startActivity(new Intent(App.getCtx(), LoginActivity.class));
            } else {
                Toast(resp.getError());
            }
        }
    }

    private void loadAD() {
        if (isFirst) {
            isFirst = false;
        } else {
            long lastMillis = Prefs.getInstance(this).getLong(Prefs.KEY_LAST_RESUME_MILLIS, 0);
            if (System.currentTimeMillis() - lastMillis > millis) {
                Prefs.getInstance(this).putLong(Prefs.KEY_LAST_RESUME_MILLIS, System.currentTimeMillis());
                //如果上次可视的时间距离现在短于2分钟,就去赚取广告费 嘎嘎嘎嘎
                Intent intent = new Intent(App.getCtx(), SplashActivity.class);
                intent.putExtra(MainActivity.KEY_IS_WILL_GO_LOGIN_ACTIVITY, false);
                startActivity(intent);
            }
        }
    }

    private void updateUserInfo(User user) {
        if (user != null) {
            userName.setText(user.getName());
            String info = "";
            if (0 != (user.getAge() == null ? 0 : user.getAge())) {
                if ("F".equals(user.getGender())) {
                    info = user.getAge() + "岁" + "  女  ";
                } else {
                    info = user.getAge() + "岁" + "  男  ";
                }
            } else {
                if ("F".equals(user.getGender())) {
                    info = "女  ";
                } else {
                    info = "男  ";
                }
            }
            double pow = Math.pow((user.getHeight() == null ? 0 : user.getHeight()), 2);
            if (pow > 0) {
                info = info + " BMI:" + (ArithTool.div((user.getWeight() == null ? 0 : user.getWeight()) * 10000, pow, 1));
            }
            userInfo.setText(info);
            loadHeader(user.getImgUri(), user.getGender());
            Prefs.getInstance(this).putString(Prefs.KEY_USER_GENDER, user.getGender());
        }
    }

    private void loadHeader(String url, String gender) {
        Glide.with(this).load(url)
                .error("M".equals(gender) ? R.mipmap.men : R.mipmap.female)
                .bitmapTransform(new GlideCircleTransformer(this))
                .signature(new StringSignature(Prefs.getInstance(App.getCtx()).getString(Prefs.KEY_USER_HEADER_IMAGE_FLAG, "")))
                .into(userIcon);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.item_search_menu, menu);
        MenuItem item = menu.findItem(R.id.action_search);
        if (searchView != null) {
            searchView.setMenuItem(item);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share:
                share();
                break;
            case R.id.action_more:
                screenCapture();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void screenCapture() {
        if (menu == null) {
            return;
        }
        menu.findItem(R.id.action_more).setEnabled(false);
        ScreenCaptureUtil.getInstance(this)
                .capture(this.getWindow().getDecorView())
                .setOnScreenCaptureListener(new ScreenCaptureUtil.OnScreenCaptureListener() {
                    @Override
                    public void onCaptureSuccessed(final String path) {
                        final FrameLayout decorView = (FrameLayout) MainActivity.this.getWindow().getDecorView();
                        int[] decorViewLocation = new int[2];
                        decorView.getLocationOnScreen(decorViewLocation);
                        final ImageView imageView = new ImageView(MainActivity.this);
                        Bitmap bitmap = ScreenCaptureUtil.getInstance(MainActivity.this).getBitmapFromMemory(path);
                        if (bitmap != null) {
                            imageView.setImageBitmap(bitmap);
                            imageView.setX(decorViewLocation[0]);
                            imageView.setY(decorViewLocation[1]);
                            decorView.addView(imageView);
                            AnimationUtil.fadeInScaleView(MainActivity.this, imageView, 400, new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    super.onAnimationEnd(animation);
                                }
                            });
                            imageView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        imageView.setTransitionName("shareView");
                                    }
                                    Intent intent = new Intent(MainActivity.this, ShareCaptureActivity.class);
                                    intent.putExtra(Prefs.KEY_BITMAP, path);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        startActivity(intent,
                                                ActivityOptions.makeSceneTransitionAnimation
                                                        (MainActivity.this, imageView, "shareView").toBundle());
                                    } else {
                                        startActivity(intent);
                                    }
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            decorView.removeView(imageView);
                                            menu.findItem(R.id.action_more).setEnabled(true);
                                        }
                                    }, 500);
                                }
                            });
                            Log.e("capture.mp3", String.format("截屏成功 path : %s", path));
                        }
                    }

                    @Override
                    public void onCaptureFailed() {
                        Log.e("capture.mp3", "截屏失败");
                    }
                });
    }

    private void share() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, Constants.SHARE_CONTENT);
        intent.setType("text/plain");
        startActivity(Intent.createChooser(intent, getString(R.string.share_with)));
    }

    @Override
    protected void onTabCheckedChange(String[] titles, int checkedId) {
        super.onTabCheckedChange(titles, checkedId);
        if (titles != null && titles.length > checkedId) {
            setToolbarTitle(titles[checkedId]);
            if (menu != null) {
                if (checkedId == 0) {
                    menu.findItem(R.id.action_search).setVisible(true);
                } else {
                    menu.findItem(R.id.action_search).setVisible(false);
                }
            }
        }
    }

    @Override
    public void onGoBack(View view) {
        moveTaskToBack(true);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (searchView != null && searchView.isSearchOpen()) {
                searchView.closeSearch();
                return false;
            }
            if (drawerLayout != null && drawerLayout.isDrawerOpen(Gravity.START)) {
                drawerLayout.closeDrawers();
                return false;
            }
            long preMillis = Prefs.getInstance(App.getCtx()).getLong(Prefs.KEY_EXIT_ACTIVITY, 0);
            if (System.currentTimeMillis() - preMillis > 2000) {
                Prefs.getInstance(App.getCtx()).putLong(Prefs.KEY_EXIT_ACTIVITY, System.currentTimeMillis());
                Toast(getString(R.string.exit_press_again));
            } else {
                Prefs.getInstance(App.getCtx()).putLong(Prefs.KEY_EXIT_ACTIVITY, 0);
                onBackPressed();
            }
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        onGoBack(null);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
//        super.onSaveInstanceState(outState);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.basic_info:
                startActivity(new Intent(this, BasicInfoActivity.class));
                break;
            case R.id.normal_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.change_login:
                startActivity(new Intent(this, ChangeLoginActivity.class));
                break;
            case R.id.ocr_scan:
                startActivity(new Intent(this, YouTuActivity.class));
                break;
            case R.id.ocr_scan_id_card:
                startActivity(new Intent(this, YouTuIdCardActivity.class));
                break;
            case R.id.wifi:
                startActivity(new Intent(this, WiFiSettingsActivity.class));
                break;
            case R.id.exoplayer:
                startActivity(new Intent(this, VideoExoPlayerActivity.class));
                break;
            case R.id.notifycation:
                startActivity(new Intent(this, NotifycationActivity.class));
                break;
            case R.id.ar:
                startActivity(new Intent(this, HelloArActivity.class));
                break;
            case R.id.vr:
                startActivity(new Intent(this, VR360Activity.class));
                break;
            case R.id.map:
                startActivity(new Intent(this, MapActivity.class));
                break;
        }
    }

}
