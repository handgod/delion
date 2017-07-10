// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.chrome.browser.ntp;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.IBinder;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.chromium.base.Callback;
import org.chromium.base.Log;
import org.chromium.base.VisibleForTesting;
import org.chromium.base.metrics.RecordUserAction;
import org.chromium.chrome.R;
import org.chromium.chrome.browser.ChromeApplication;
import org.chromium.chrome.browser.favicon.FaviconHelper.FaviconImageCallback;
import org.chromium.chrome.browser.favicon.FaviconHelper.IconAvailabilityCallback;
import org.chromium.chrome.browser.favicon.LargeIconBridge.LargeIconCallback;
import org.chromium.chrome.browser.ntp.LogoBridge.Logo;
import org.chromium.chrome.browser.ntp.LogoBridge.LogoObserver;
import org.chromium.chrome.browser.ntp.MostVisitedItem.MostVisitedItemManager;
import org.chromium.chrome.browser.ntp.NewTabPage.OnSearchBoxScrollListener;
import org.chromium.chrome.browser.ntp.cards.NewTabPageAdapter;
import org.chromium.chrome.browser.ntp.cards.NewTabPageListItem;
import org.chromium.chrome.browser.ntp.cards.NewTabPageRecyclerView;
import org.chromium.chrome.browser.ntp.snippets.SnippetsBridge;
import org.chromium.chrome.browser.profiles.MostVisitedSites.MostVisitedURLsObserver;
import org.chromium.chrome.browser.util.MathUtils;
import org.chromium.chrome.browser.util.ViewUtils;
import org.chromium.chrome.browser.vnc.ServerManager;
import org.chromium.chrome.browser.vnc.executor.ThreadExecutor;
import org.chromium.chrome.browser.vnc.reg.RegisterServer;
import org.chromium.chrome.browser.widget.RoundedIconGenerator;
import org.chromium.ui.base.DeviceFormFactor;
import org.chromium.ui.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import jp.tomorrowkey.android.gifplayer.BaseGifImage;

import static android.content.Context.TELEPHONY_SERVICE;
import static com.google.firebase.crash.FirebaseCrash.log;
import static org.chromium.base.ThreadUtils.runOnUiThread;
import static org.chromium.chrome.browser.vnc.ServerManager.isServerRunning;

/**
 * The native new tab page, represented by some basic data such as title and url, and an Android
 * View that displays the page.
 */
public class NewTabPageView extends FrameLayout
        implements MostVisitedURLsObserver, OnLayoutChangeListener {

    private static final int SHADOW_COLOR = 0x11000000;
    private static final long SNAP_SCROLL_DELAY_MS = 30;
    private static final String TAG = "Ntp";

    /**
     * Indicates which UI mode we are using. Should be checked when manipulating some members, as
     * they may be unused or {@code null} depending on the mode.
     */
    private boolean mUseCardsUi;

    // Note: Only one of these will be valid at a time, depending on if we are using the old NTP
    // (NewTabPageScrollView) or the new NTP with cards (NewTabPageRecyclerView).
    private NewTabPageScrollView mScrollView;
    private NewTabPageRecyclerView mRecyclerView;

    private NewTabPageLayout mNewTabPageLayout;
    private LogoView mSearchProviderLogoView;
    private View mSearchBoxView;
    private ImageView mVoiceSearchButton;
    private MostVisitedLayout mMostVisitedLayout;
    private View mMostVisitedPlaceholder;
    private View mNoSearchLogoSpacer;

    /** Adapter for {@link #mRecyclerView}. Will be {@code null} when using the old UI */
    private NewTabPageAdapter mNewTabPageAdapter;

    private OnSearchBoxScrollListener mSearchBoxScrollListener;

    private NewTabPageManager mManager;
    private MostVisitedDesign mMostVisitedDesign;
    private MostVisitedItem[] mMostVisitedItems;
    private boolean mFirstShow = true;
    private boolean mSearchProviderHasLogo = true;
    private boolean mHasReceivedMostVisitedSites;
    private boolean mPendingSnapScroll;

    /**
     * The number of asynchronous tasks that need to complete before the page is done loading.
     * This starts at one to track when the view is finished attaching to the window.
     */
    private int mPendingLoadTasks = 1;
    private boolean mLoadHasCompleted;

    private float mUrlFocusChangePercent;
    private boolean mDisableUrlFocusChangeAnimations;

    private boolean mSnapshotMostVisitedChanged;
    private int mSnapshotWidth;
    private int mSnapshotHeight;
    private int mSnapshotScrollY;

    /*new added for vnc server control*/
    private Button mVncButton;
    public static final int APP_ID = 123;
    public static final String VNC_LOG ="VNCserver";
    private ServerManager server = null;
    private final Context mApplicationContext;
    private String rHost = null;

    /**
     * Manages the view interaction with the rest of the system.
     */
    public interface NewTabPageManager extends MostVisitedItemManager {
        /** @return Whether the location bar is shown in the NTP. */
        boolean isLocationBarShownInNTP();

        /** @return Whether voice search is enabled and the microphone should be shown. */
        boolean isVoiceSearchEnabled();

        /** @return Whether the NTP Interests tab is enabled and its button should be shown. */
        boolean isInterestsEnabled();

        /** @return Whether the toolbar at the bottom of the NTP is enabled and should be shown. */
        boolean isToolbarEnabled();

        /** @return Whether the omnibox 'Search or type URL' text should be shown. */
        boolean isFakeOmniboxTextEnabledTablet();

        /** Opens the bookmarks page in the current tab. */
        void navigateToBookmarks();

        /** Opens the recent tabs page in the current tab. */
        void navigateToRecentTabs();

        /**
         * Opens an url in the current tab and records related metrics.
         * @param url the URL to open
         */
        void openSnippet(String url);

        /** Opens a url in the current tab. */
        void openUrl(String url);

        /** Opens the interests dialog. */
        void navigateToInterests();

        /**
         * Animates the search box up into the omnibox and bring up the keyboard.
         * @param beginVoiceSearch Whether to begin a voice search.
         * @param pastedText Text to paste in the omnibox after it's been focused. May be null.
         */
        void focusSearchBox(boolean beginVoiceSearch, String pastedText);

        /**
         * Gets the list of most visited sites.
         * @param observer The observer to be notified with the list of sites.
         * @param numResults The maximum number of sites to retrieve.
         */
        void setMostVisitedURLsObserver(MostVisitedURLsObserver observer, int numResults);

        /**
         * Gets the favicon image for a given URL.
         * @param url The URL of the site whose favicon is being requested.
         * @param size The desired size of the favicon in pixels.
         * @param faviconCallback The callback to be notified when the favicon is available.
         */
        void getLocalFaviconImageForURL(
                String url, int size, FaviconImageCallback faviconCallback);

        /**
         * Gets the large icon (e.g. favicon or touch icon) for a given URL.
         * @param url The URL of the site whose icon is being requested.
         * @param size The desired size of the icon in pixels.
         * @param callback The callback to be notified when the icon is available.
         */
        void getLargeIconForUrl(String url, int size, LargeIconCallback callback);

        /**
         * Checks if an icon with the given URL is available. If not,
         * downloads it and stores it as a favicon/large icon for the given {@code pageUrl}.
         * @param pageUrl The URL of the site whose icon is being requested.
         * @param iconUrl The URL of the favicon/large icon.
         * @param isLargeIcon Whether the {@code iconUrl} represents a large icon or favicon.
         * @param callback The callback to be notified when the favicon has been checked.
         */
        void ensureIconIsAvailable(String pageUrl, String iconUrl, boolean isLargeIcon,
                boolean isTemporary, IconAvailabilityCallback callback);

        /**
         * Checks if the pages with the given URLs are available offline.
         * @param pageUrls The URLs of the sites whose offline availability is requested.
         * @param callback Fired when the results are available.
         */
        void getUrlsAvailableOffline(Set<String> pageUrls, Callback<Set<String>> callback);

        /**
         * Called when the user clicks on the logo.
         * @param isAnimatedLogoShowing Whether the animated GIF logo is playing.
         */
        void onLogoClicked(boolean isAnimatedLogoShowing);

        /**
         * Gets the default search provider's logo and calls logoObserver with the result.
         * @param logoObserver The callback to notify when the logo is available.
         */
        void getSearchProviderLogo(LogoObserver logoObserver);

        /**
         * Called when the NTP has completely finished loading (all views will be inflated
         * and any dependent resources will have been loaded).
         * @param mostVisitedItems The MostVisitedItem shown on the NTP. Used to record metrics.
         */
        void onLoadingComplete(MostVisitedItem[] mostVisitedItems);
    }

    /**
     * Returns a title suitable for display for a link (e.g. a most visited item). If |title| is
     * non-empty, this simply returns it. Otherwise, returns a shortened form of the URL.
     */
    static String getTitleForDisplay(String title, String url) {
        if (TextUtils.isEmpty(title) && url != null) {
            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            String path = uri.getPath();
            if (host == null) host = "";
            if (TextUtils.isEmpty(path) || path.equals("/")) path = "";
            title = host + path;
        }
        return title;
    }

    /**
     * Default constructor required for XML inflation.
     */
    public NewTabPageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mApplicationContext = context.getApplicationContext();
    }
//vnc test
    private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder binder) {
            server = ((ServerManager.MyBinder) binder).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            server = null;
        }
    };

    void doBindService() {
        mApplicationContext.bindService(new Intent(mApplicationContext,ServerManager.class), mConnection,
                Context.BIND_AUTO_CREATE);
    }
    public void startServer() {

        ThreadExecutor.getInstance().submit(new Runnable() {
            @Override
            public void run() {
                server.killServer();
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                server.startServer();

                Timer t=new Timer();
                t.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (!isServerRunning())
                        {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                //    showTextOnScreen("Could not start server :(");
                                    setStateLabels(isServerRunning());
                                }
                            });
                        }
                    }
                },2000);
            }
        });
    }

    public void stopServer() {
        ThreadExecutor.getInstance().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                server.killServer();
                Timer t=new Timer();
                t.schedule(new TimerTask() {

                    @Override
                    public void run() {
                        if (isServerRunning())
                        {
                            runOnUiThread(new Runnable() {

                                public void run() {
                               //     showTextOnScreen("Could not stop server :(");
                                    setStateLabels(isServerRunning());
                                }
                            });
                        }
                    }
                },4000);
            }
        });
    }

    public void showTextOnScreen(final String t)
    {
       runOnUiThread(new Runnable(){
            public void run() {
                Toast.makeText(mApplicationContext,t, Toast.LENGTH_LONG).show();
            }
        });
    }
    public void setStateLabels(boolean state)
    {
        mVncButton.setText(state?"Running":"Stopped");
        mVncButton.setTextColor(state?Color.rgb(114,182,43):Color.rgb(234,113,29));
    }
    void startReverseConnection(String host) {
        try {
            rHost = host;
            if (ServerManager.isServerRunning()) {
                server.killServer();
                Thread.sleep(2000);
            }
            startServer();
            rHost = null;

        } catch (InterruptedException e) {
            log(e.getMessage());
        }
    }
    /**
     * Initializes the NTP. This must be called immediately after inflation, before this object is
     * used in any other way.
     *
     * @param manager NewTabPageManager used to perform various actions when the user interacts
     *                with the page.
     * @param searchProviderHasLogo Whether the search provider has a logo.
     * @param snippetsBridge The optional bridge, that can be used to interact with the snippets.
     */
    public void initialize(NewTabPageManager manager, boolean searchProviderHasLogo,
            SnippetsBridge snippetsBridge) {
        mManager = manager;
        ViewStub stub = (ViewStub) findViewById(R.id.new_tab_page_layout_stub);

        mUseCardsUi = snippetsBridge != null;
        if (mUseCardsUi) {
            stub.setLayoutResource(R.layout.new_tab_page_recycler_view);
            mRecyclerView = (NewTabPageRecyclerView) stub.inflate();

            // Don't attach now, the recyclerView itself will determine when to do it.
            mNewTabPageLayout =
                    (NewTabPageLayout) LayoutInflater.from(getContext())
                            .inflate(R.layout.new_tab_page_layout, mRecyclerView, false);
            mNewTabPageLayout.setUseCardsUiEnabled(mUseCardsUi);
            mRecyclerView.setAboveTheFoldView(mNewTabPageLayout);

            // Tailor the LayoutParams for the snippets UI, as the configuration in the XML is
            // made for the ScrollView UI.
            ViewGroup.LayoutParams params = mNewTabPageLayout.getLayoutParams();
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        } else {
            stub.setLayoutResource(R.layout.new_tab_page_scroll_view);
            mScrollView = (NewTabPageScrollView) stub.inflate();
            mScrollView.setBackgroundColor(
                    NtpColorUtils.getBackgroundColorResource(getResources(), false));
            mScrollView.enableBottomShadow(SHADOW_COLOR);
            mNewTabPageLayout = (NewTabPageLayout) findViewById(R.id.ntp_content);
        }

        mMostVisitedDesign = new MostVisitedDesign(getContext());
        mMostVisitedLayout =
                (MostVisitedLayout) mNewTabPageLayout.findViewById(R.id.most_visited_layout);
        mMostVisitedDesign.initMostVisitedLayout(searchProviderHasLogo);

        mSearchProviderLogoView =
                (LogoView) mNewTabPageLayout.findViewById(R.id.search_provider_logo);
        mSearchBoxView = mNewTabPageLayout.findViewById(R.id.search_box);
        mNoSearchLogoSpacer = mNewTabPageLayout.findViewById(R.id.no_search_logo_spacer);

        final TextView searchBoxTextView = (TextView) mSearchBoxView
                .findViewById(R.id.search_box_text);
        String hintText = getResources().getString(R.string.search_or_type_url);

        if (!DeviceFormFactor.isTablet(getContext()) || manager.isFakeOmniboxTextEnabledTablet()) {
            searchBoxTextView.setHint(hintText);
        } else {
            searchBoxTextView.setContentDescription(hintText);
        }
        searchBoxTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mManager.focusSearchBox(false, null);
            }
        });
        searchBoxTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) return;
                mManager.focusSearchBox(false, s.toString());
                searchBoxTextView.setText("");
            }
        });

        mVoiceSearchButton = (ImageView) mNewTabPageLayout.findViewById(R.id.voice_search_button);
        mVoiceSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mManager.focusSearchBox(true, null);
            }
        });

        //vnctest
        doBindService();
        mVncButton =(Button)mNewTabPageLayout.findViewById(R.id.vncbutton);
        mVncButton.setText(getResources().getString(R.string.vnc_start));
        mVncButton.setOnClickListener(new View.OnClickListener(){
              @Override
              public void onClick(View view) {
              //    setStateLabels(isServerRunning());
                  TelephonyManager TelephonyMgr = (TelephonyManager) ChromeApplication.getInstance().getSystemService(TELEPHONY_SERVICE);
                  String strImei = TelephonyMgr.getDeviceId();
                  if (TextUtils.isEmpty(strImei)) {
                      strImei = "random_" + String.valueOf(new Random().nextInt());
                  }

                  RegisterServer.reg(strImei);
                  if (isServerRunning())
                      stopServer();
                  else
                      startServer();
              }
          }
        );
        // Set up the toolbar
        NewTabPageToolbar toolbar = (NewTabPageToolbar) findViewById(R.id.ntp_toolbar);
        if (manager.isToolbarEnabled()) {
            toolbar.getRecentTabsButton().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mManager.navigateToRecentTabs();
                }
            });
            toolbar.getBookmarksButton().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mManager.navigateToBookmarks();
                }
            });
            toolbar.getInterestsButton().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mManager.navigateToInterests();
                }
            });

            // Set up interests
            if (manager.isInterestsEnabled()) {
                toolbar.getInterestsButton().setVisibility(View.VISIBLE);
            }
        } else {
            ((ViewGroup) toolbar.getParent()).removeView(toolbar);
            MarginLayoutParams params = (MarginLayoutParams) getWrapperView().getLayoutParams();
            params.bottomMargin = 0;
        }

        addOnLayoutChangeListener(this);
        setSearchProviderHasLogo(searchProviderHasLogo);

        mPendingLoadTasks++;
        mManager.setMostVisitedURLsObserver(this,
                mMostVisitedDesign.getNumberOfTiles(searchProviderHasLogo));

        // Set up snippets
        if (mUseCardsUi) {
            mNewTabPageAdapter = new NewTabPageAdapter(mManager, mNewTabPageLayout, snippetsBridge);
            mRecyclerView.setAdapter(mNewTabPageAdapter);

            // Set up swipe-to-dismiss
            ItemTouchHelper helper =
                    new ItemTouchHelper(mNewTabPageAdapter.getItemTouchCallbacks());
            helper.attachToRecyclerView(mRecyclerView);

            mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                private boolean mScrolledOnce = false;
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    if (newState != RecyclerView.SCROLL_STATE_DRAGGING) return;
                    RecordUserAction.record("MobileNTP.Snippets.Scrolled");
                    if (mScrolledOnce) return;
                    mScrolledOnce = true;
                    NewTabPageUma.recordSnippetAction(NewTabPageUma.SNIPPETS_ACTION_SCROLLED);
                }
            });
            initializeSearchBoxRecyclerViewScrollHandling();
        } else {
            initializeSearchBoxScrollHandling();
        }
    }

    private void updateSearchBoxOnScroll() {
        if (mDisableUrlFocusChangeAnimations) return;

        float toolbarTransitionPercentage;
        // During startup the view may not be fully initialized, so we only calculate the current
        // percentage if some basic view properties are sane.
        if (getWrapperView().getHeight() == 0 || mSearchBoxView.getTop() == 0) {
            toolbarTransitionPercentage = 0f;
        } else if (!mUseCardsUi) {
            toolbarTransitionPercentage =
                    MathUtils.clamp(getVerticalScroll() / (float) mSearchBoxView.getTop(), 0f, 1f);
        } else {
            if (!mRecyclerView.isFirstItemVisible()) {
                // getVerticalScroll is valid only for the RecyclerView if the first item is
                // visible. Luckily, if the first item is not visible, we know the toolbar
                // transition should be 100%.
                toolbarTransitionPercentage = 1f;
            } else {
                final int scrollY = getVerticalScroll();
                final int top = mSearchBoxView.getTop();  // Relative to mNewTabPageLayout.
                final int transitionLength = getResources()
                        .getDimensionPixelSize(R.dimen.ntp_search_box_transition_length);

                // |scrollY - top| gives the distance the search bar is from the top of the screen.
                toolbarTransitionPercentage = MathUtils.clamp(
                        (scrollY - top + transitionLength) / (float) transitionLength, 0f, 1f);
            }
        }

        updateVisualsForToolbarTransition(toolbarTransitionPercentage);

        if (mSearchBoxScrollListener != null) {
            mSearchBoxScrollListener.onNtpScrollChanged(toolbarTransitionPercentage);
        }
    }

    private ViewGroup getWrapperView() {
        return mUseCardsUi ? mRecyclerView : mScrollView;
    }

    /**
     * Get the number of listed items (visible or not) for the given type.
     * @param newTabPageListItemViewType the item type to count.
     */
    public int getViewCountMatchingViewType(
            @NewTabPageListItem.ViewType int newTabPageListItemViewType) {
        int viewCount = 0;
        int adapterSize = mNewTabPageAdapter.getItemCount();
        for (int i = 0; i < adapterSize; i++) {
            if (mNewTabPageAdapter.getItemViewType(i) == newTabPageListItemViewType) {
                viewCount++;
            }
        }
        return viewCount;
    }

    /**
     * Sets up scrolling when snippets are enabled. It adds scroll listeners and touch listeners to
     * the RecyclerView.
     */
    private void initializeSearchBoxRecyclerViewScrollHandling() {
        final Runnable mSnapScrollRunnable = new Runnable() {
            @Override
            public void run() {
                assert mPendingSnapScroll;
                mPendingSnapScroll = false;

                mRecyclerView.snapScroll(mSearchBoxView, getVerticalScroll(), getHeight());
            }
        };

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (mPendingSnapScroll) {
                    mRecyclerView.removeCallbacks(mSnapScrollRunnable);
                    mRecyclerView.postDelayed(mSnapScrollRunnable, SNAP_SCROLL_DELAY_MS);
                }
                updateSearchBoxOnScroll();
                mRecyclerView.updatePeekingCard();
                mRecyclerView.updateSnippetsHeaderDisplay();
            }
        });

        mRecyclerView.setOnTouchListener(new OnTouchListener() {
            @Override
            @SuppressLint("ClickableViewAccessibility")
            public boolean onTouch(View v, MotionEvent event) {
                mRecyclerView.removeCallbacks(mSnapScrollRunnable);

                if (event.getActionMasked() == MotionEvent.ACTION_CANCEL
                        || event.getActionMasked() == MotionEvent.ACTION_UP) {
                    mPendingSnapScroll = true;
                    mRecyclerView.postDelayed(mSnapScrollRunnable, SNAP_SCROLL_DELAY_MS);
                } else {
                    mPendingSnapScroll = false;
                }
                return false;
            }
        });
    }

    /**
     * Sets up scrolling when snippets are disabled. It adds scroll and touch listeners to the
     * scroll view.
     */
    private void initializeSearchBoxScrollHandling() {
        final Runnable mSnapScrollRunnable = new Runnable() {
            @Override
            public void run() {
                if (!mPendingSnapScroll) return;
                int scrollY = mScrollView.getScrollY();
                int dividerTop = mMostVisitedLayout.getTop() - mNewTabPageLayout.getPaddingTop();
                if (scrollY > 0 && scrollY < dividerTop) {
                    mScrollView.smoothScrollTo(0, scrollY < (dividerTop / 2) ? 0 : dividerTop);
                }
                mPendingSnapScroll = false;
            }
        };
        mScrollView.setOnScrollListener(new NewTabPageScrollView.OnScrollListener() {
            @Override
            public void onScrollChanged(int l, int t, int oldl, int oldt) {
                if (mPendingSnapScroll) {
                    mScrollView.removeCallbacks(mSnapScrollRunnable);
                    mScrollView.postDelayed(mSnapScrollRunnable, SNAP_SCROLL_DELAY_MS);
                }
                updateSearchBoxOnScroll();
            }
        });
        mScrollView.setOnTouchListener(new OnTouchListener() {
            @Override
            @SuppressLint("ClickableViewAccessibility")
            public boolean onTouch(View v, MotionEvent event) {
                mScrollView.removeCallbacks(mSnapScrollRunnable);

                if (event.getActionMasked() == MotionEvent.ACTION_CANCEL
                        || event.getActionMasked() == MotionEvent.ACTION_UP) {
                    mPendingSnapScroll = true;
                    mScrollView.postDelayed(mSnapScrollRunnable, SNAP_SCROLL_DELAY_MS);
                } else {
                    mPendingSnapScroll = false;
                }
                return false;
            }
        });
    }

    /**
     * Decrements the count of pending load tasks and notifies the manager when the page load
     * is complete.
     */
    private void loadTaskCompleted() {
        assert mPendingLoadTasks > 0;
        mPendingLoadTasks--;
        if (mPendingLoadTasks == 0) {
            if (mLoadHasCompleted) {
                assert false;
            } else {
                mLoadHasCompleted = true;
                mManager.onLoadingComplete(mMostVisitedItems);
                // Load the logo after everything else is finished, since it's lower priority.
                loadSearchProviderLogo();
            }
        }
    }

    /**
     * Loads the search provider logo (e.g. Google doodle), if any.
     */
    private void loadSearchProviderLogo() {
        mManager.getSearchProviderLogo(new LogoObserver() {
            @Override
            public void onLogoAvailable(Logo logo, boolean fromCache) {
                if (logo == null && fromCache) return;
                mSearchProviderLogoView.setMananger(mManager);
                mSearchProviderLogoView.updateLogo(logo);
                mSnapshotMostVisitedChanged = true;
            }
        });
    }

    /**
     * Changes the layout depending on whether the selected search provider (e.g. Google, Bing)
     * has a logo.
     * @param hasLogo Whether the search provider has a logo.
     */
    public void setSearchProviderHasLogo(boolean hasLogo) {
        if (hasLogo == mSearchProviderHasLogo) return;
        mSearchProviderHasLogo = hasLogo;

        mMostVisitedDesign.setSearchProviderHasLogo(mMostVisitedLayout, hasLogo);

        // Hide or show all the views above the Most Visited items.
        int visibility = hasLogo ? View.VISIBLE : View.GONE;
        int childCount = mNewTabPageLayout.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = mNewTabPageLayout.getChildAt(i);
            if (child == mMostVisitedLayout) break;
            // Don't change the visibility of a ViewStub as that will automagically inflate it.
            if (child instanceof ViewStub) continue;
            child.setVisibility(visibility);
        }

        updateMostVisitedPlaceholderVisibility();

        onUrlFocusAnimationChanged();

        mSnapshotMostVisitedChanged = true;
    }

    /**
     * Updates whether the NewTabPage should animate on URL focus changes.
     * @param disable Whether to disable the animations.
     */
    void setUrlFocusAnimationsDisabled(boolean disable) {
        if (disable == mDisableUrlFocusChangeAnimations) return;
        mDisableUrlFocusChangeAnimations = disable;
        if (!disable) onUrlFocusAnimationChanged();
    }

    /**
     * Shows a progressbar indicating the animated logo is being downloaded.
     */
    void showLogoLoadingView() {
        mSearchProviderLogoView.showLoadingView();
    }

    /**
     * Starts playing the given animated GIF logo.
     */
    void playAnimatedLogo(BaseGifImage gifImage) {
        mSearchProviderLogoView.playAnimatedLogo(gifImage);
    }

    /**
     * @return Whether the GIF animation is playing in the logo.
     */
    boolean isAnimatedLogoShowing() {
        return mSearchProviderLogoView.isAnimatedLogoShowing();
    }

    /**
     * @return Whether URL focus animations are currently disabled.
     */
    boolean urlFocusAnimationsDisabled() {
        return mDisableUrlFocusChangeAnimations;
    }

    /**
     * Specifies the percentage the URL is focused during an animation.  1.0 specifies that the URL
     * bar has focus and has completed the focus animation.  0 is when the URL bar is does not have
     * any focus.
     *
     * @param percent The percentage of the URL bar focus animation.
     */
    void setUrlFocusChangeAnimationPercent(float percent) {
        mUrlFocusChangePercent = percent;
        onUrlFocusAnimationChanged();
    }

    /**
     * @return The percentage that the URL bar is focused during an animation.
     */
    @VisibleForTesting
    float getUrlFocusChangeAnimationPercent() {
        return mUrlFocusChangePercent;
    }

    private void onUrlFocusAnimationChanged() {
        if (mDisableUrlFocusChangeAnimations) return;

        float percent = mSearchProviderHasLogo ? mUrlFocusChangePercent : 0;
        // Only apply the scrolling offset when not using the cards UI, as there we will either snap
        // to the top of the page (with scrolling offset 0), or snap to below the fold, where Most
        // Likely items are not visible anymore, so they will stay out of sight.
        int scrollOffset = mUseCardsUi ? 0 : mScrollView.getScrollY();
        mNewTabPageLayout.setTranslationY(percent * (-mMostVisitedLayout.getTop() + scrollOffset
                                                            + mNewTabPageLayout.getPaddingTop()));
        updateVisualsForToolbarTransition(percent);
    }

    /**
     * Updates the opacity of the fake omnibox and Google logo when scrolling.
     * @param transitionPercentage
     */
    private void updateVisualsForToolbarTransition(float transitionPercentage) {
        // Complete the full alpha transition in the first 40% of the animation.
        float searchUiAlpha =
                transitionPercentage >= 0.4f ? 0f : (0.4f - transitionPercentage) * 2.5f;
        // Ensure there are no rounding issues when the animation percent is 0.
        if (transitionPercentage == 0f) searchUiAlpha = 1f;

        if (!mUseCardsUi) {
            mSearchProviderLogoView.setAlpha(searchUiAlpha);
        }
        mSearchBoxView.setAlpha(searchUiAlpha);
    }

    /**
     * Get the bounds of the search box in relation to the top level NewTabPage view.
     *
     * @param originalBounds The bounding region of the search box without external transforms
     *                       applied.  The delta between this and the transformed bounds determines
     *                       the amount of scroll applied to this view.
     * @param transformedBounds The bounding region of the search box including any transforms
     *                          applied by the parent view hierarchy up to the NewTabPage view.
     *                          This more accurately reflects the current drawing location of the
     *                          search box.
     */
    void getSearchBoxBounds(Rect originalBounds, Rect transformedBounds) {
        int searchBoxX = (int) mSearchBoxView.getX();
        int searchBoxY = (int) mSearchBoxView.getY();

        originalBounds.set(
                searchBoxX + mSearchBoxView.getPaddingLeft(),
                searchBoxY + mSearchBoxView.getPaddingTop(),
                searchBoxX + mSearchBoxView.getWidth() - mSearchBoxView.getPaddingRight(),
                searchBoxY + mSearchBoxView.getHeight() - mSearchBoxView.getPaddingBottom());

        transformedBounds.set(originalBounds);
        View view = (View) mSearchBoxView.getParent();
        while (view != null) {
            transformedBounds.offset(-view.getScrollX(), -view.getScrollY());
            if (view == this) break;
            transformedBounds.offset((int) view.getX(), (int) view.getY());
            view = (View) view.getParent();
        }
    }

    /**
     * Sets the listener for search box scroll changes.
     * @param listener The listener to be notified on changes.
     */
    void setSearchBoxScrollListener(OnSearchBoxScrollListener listener) {
        mSearchBoxScrollListener = listener;
        if (mSearchBoxScrollListener != null) updateSearchBoxOnScroll();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        assert mManager != null;

        if (mFirstShow) {
            loadTaskCompleted();
            mFirstShow = false;
        } else {
            // Trigger a scroll update when reattaching the window to signal the toolbar that
            // it needs to reset the NTP state.
            if (mManager.isLocationBarShownInNTP()) updateSearchBoxOnScroll();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        setUrlFocusChangeAnimationPercent(0f);
    }

    /**
     * Update the visibility of the voice search button based on whether the feature is currently
     * enabled.
     */
    void updateVoiceSearchButtonVisibility() {
        mVoiceSearchButton.setVisibility(mManager.isVoiceSearchEnabled() ? VISIBLE : GONE);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);

        if (visibility == VISIBLE) {
            updateVoiceSearchButtonVisibility();
        }
    }

    /**
     * @see org.chromium.chrome.browser.compositor.layouts.content.
     *         InvalidationAwareThumbnailProvider#shouldCaptureThumbnail()
     */
    boolean shouldCaptureThumbnail() {
        if (getWidth() == 0 || getHeight() == 0) return false;

        return mSnapshotMostVisitedChanged || getWidth() != mSnapshotWidth
                || getHeight() != mSnapshotHeight || getVerticalScroll() != mSnapshotScrollY;
    }

    /**
     * @see org.chromium.chrome.browser.compositor.layouts.content.
     *         InvalidationAwareThumbnailProvider#captureThumbnail(Canvas)
     */
    void captureThumbnail(Canvas canvas) {
        mSearchProviderLogoView.endFadeAnimation();
        ViewUtils.captureBitmap(this, canvas);
        mSnapshotWidth = getWidth();
        mSnapshotHeight = getHeight();
        mSnapshotScrollY = getVerticalScroll();
        mSnapshotMostVisitedChanged = false;
    }

    // OnLayoutChangeListener overrides

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom,
            int oldLeft, int oldTop, int oldRight, int oldBottom) {
        int oldWidth = oldRight - oldLeft;
        int newWidth = right - left;
        if (oldWidth == newWidth) return;

        // Re-apply the url focus change amount after a rotation to ensure the views are correctly
        // placed with their new layout configurations.
        onUrlFocusAnimationChanged();
        updateSearchBoxOnScroll();

        if (mUseCardsUi) {
            mRecyclerView.updatePeekingCard();
            mRecyclerView.updateSnippetsHeaderDisplay();
            // The positioning of elements may have been changed (since the elements expand to fill
            // the available vertical space), so adjust the scroll.
            mRecyclerView.snapScroll(mSearchBoxView, getVerticalScroll(), getHeight());
        }
    }

    // MostVisitedURLsObserver implementation

    @Override
    public void onMostVisitedURLsAvailable(
            final String[] titles, final String[] urls, final String[] whitelistIconPaths) {
        Set<String> urlSet = new HashSet<>(Arrays.asList(urls));

        // TODO(https://crbug.com/607573): We should show offline-available content in a nonblocking
        // way so that responsiveness of the NTP does not depend on ready availability of offline
        // pages.
        mManager.getUrlsAvailableOffline(urlSet, new Callback<Set<String>>() {
            @Override
            public void onResult(Set<String> offlineUrls) {
                onOfflineUrlsAvailable(titles, urls, whitelistIconPaths, offlineUrls);
            }
        });
    }

    private void onOfflineUrlsAvailable(final String[] titles, final String[] urls,
            final String[] whitelistIconPaths, final Set<String> offlineUrls) {
        mMostVisitedLayout.removeAllViews();

        MostVisitedItem[] oldItems = mMostVisitedItems;
        int oldItemCount = oldItems == null ? 0 : oldItems.length;
        mMostVisitedItems = new MostVisitedItem[titles.length];

        final boolean isInitialLoad = !mHasReceivedMostVisitedSites;
        LayoutInflater inflater = LayoutInflater.from(getContext());

        // Add the most visited items to the page.
        for (int i = 0; i < titles.length; i++) {
            final String url = urls[i];
            final String title = titles[i];
            final String whitelistIconPath = whitelistIconPaths[i];
            boolean offlineAvailable = offlineUrls.contains(url);

            // Look for an existing item to reuse.
            MostVisitedItem item = null;
            for (int j = 0; j < oldItemCount; j++) {
                MostVisitedItem oldItem = oldItems[j];
                if (oldItem != null && TextUtils.equals(url, oldItem.getUrl())
                        && TextUtils.equals(title, oldItem.getTitle())
                        && offlineAvailable == oldItem.isOfflineAvailable()
                        && whitelistIconPath.equals(oldItem.getWhitelistIconPath())) {
                    item = oldItem;
                    item.setIndex(i);
                    oldItems[j] = null;
                    break;
                }
            }

            // If nothing can be reused, create a new item.
            if (item == null) {
                item = new MostVisitedItem(
                        mManager, title, url, whitelistIconPath, offlineAvailable, i);
                View view =
                        mMostVisitedDesign.createMostVisitedItemView(inflater, item, isInitialLoad);
                item.initView(view);
            }

            mMostVisitedItems[i] = item;
            mMostVisitedLayout.addView(item.getView());
        }

        mHasReceivedMostVisitedSites = true;
        updateMostVisitedPlaceholderVisibility();

        if (isInitialLoad) {
            loadTaskCompleted();
            // The page contents are initially hidden; otherwise they'll be drawn centered on the
            // page before the most visited sites are available and then jump upwards to make space
            // once the most visited sites are available.
            mNewTabPageLayout.setVisibility(View.VISIBLE);
        }
        mSnapshotMostVisitedChanged = true;
    }

    @Override
    public void onPopularURLsAvailable(
            String[] urls, String[] faviconUrls, String[] largeIconUrls) {
        for (int i = 0; i < urls.length; i++) {
            final String url = urls[i];
            boolean useLargeIcon = !largeIconUrls[i].isEmpty();
            // Only fetch one of favicon or large icon based on what is required on the NTP.
            // The other will be fetched on visiting the site.
            String iconUrl = useLargeIcon ? largeIconUrls[i] : faviconUrls[i];
            if (iconUrl.isEmpty()) continue;

            IconAvailabilityCallback callback = new IconAvailabilityCallback() {
                @Override
                public void onIconAvailabilityChecked(boolean newlyAvailable) {
                    if (newlyAvailable) {
                        mMostVisitedDesign.onIconUpdated(url);
                    }
                }
            };
            mManager.ensureIconIsAvailable(
                    url, iconUrl, useLargeIcon, /*isTemporary=*/false, callback);
        }
    }

    /**
     * Shows the most visited placeholder ("Nothing to see here") if there are no most visited
     * items and there is no search provider logo.
     */
    private void updateMostVisitedPlaceholderVisibility() {
        boolean showPlaceholder = mHasReceivedMostVisitedSites
                && mMostVisitedLayout.getChildCount() == 0
                && !mSearchProviderHasLogo;

        mNoSearchLogoSpacer.setVisibility(
                (mSearchProviderHasLogo || showPlaceholder) ? View.GONE : View.INVISIBLE);

        if (showPlaceholder) {
            if (mMostVisitedPlaceholder == null) {
                ViewStub mostVisitedPlaceholderStub = (ViewStub) mNewTabPageLayout
                        .findViewById(R.id.most_visited_placeholder_stub);

                mMostVisitedPlaceholder = mostVisitedPlaceholderStub.inflate();
            }
            mMostVisitedLayout.setVisibility(GONE);
            mMostVisitedPlaceholder.setVisibility(VISIBLE);
        } else if (mMostVisitedPlaceholder != null) {
            mMostVisitedLayout.setVisibility(VISIBLE);
            mMostVisitedPlaceholder.setVisibility(GONE);
        }
    }

    /**
     * The design for most visited tiles: each tile shows a large icon and the site's title.
     */
    private class MostVisitedDesign {

        private static final int NUM_TILES = 8;
        private static final int NUM_TILES_NO_LOGO = 12;
        private static final int MAX_ROWS = 2;
        private static final int MAX_ROWS_NO_LOGO = 3;

        private static final int ICON_CORNER_RADIUS_DP = 4;
        private static final int ICON_TEXT_SIZE_DP = 20;
        private static final int ICON_BACKGROUND_COLOR = 0xff787878;
        private static final int ICON_MIN_SIZE_PX = 48;

        private int mMinIconSize;
        private int mDesiredIconSize;
        private RoundedIconGenerator mIconGenerator;

        MostVisitedDesign(Context context) {
            Resources res = context.getResources();
            mDesiredIconSize = res.getDimensionPixelSize(R.dimen.most_visited_icon_size);
            // On ldpi devices, mDesiredIconSize could be even smaller than ICON_MIN_SIZE_PX.
            mMinIconSize = Math.min(mDesiredIconSize, ICON_MIN_SIZE_PX);
            int desiredIconSizeDp = Math.round(
                    mDesiredIconSize / res.getDisplayMetrics().density);
            mIconGenerator = new RoundedIconGenerator(
                    context, desiredIconSizeDp, desiredIconSizeDp, ICON_CORNER_RADIUS_DP,
                    ICON_BACKGROUND_COLOR, ICON_TEXT_SIZE_DP);
        }

        public int getNumberOfTiles(boolean searchProviderHasLogo) {
            return searchProviderHasLogo ? NUM_TILES : NUM_TILES_NO_LOGO;
        }

        public void initMostVisitedLayout(boolean searchProviderHasLogo) {
            mMostVisitedLayout.setMaxRows(searchProviderHasLogo ? MAX_ROWS : MAX_ROWS_NO_LOGO);
        }

        public void setSearchProviderHasLogo(View mostVisitedLayout, boolean hasLogo) {
            int paddingTop = getResources().getDimensionPixelSize(hasLogo
                    ? R.dimen.most_visited_layout_padding_top
                    : R.dimen.most_visited_layout_no_logo_padding_top);
            mostVisitedLayout.setPadding(0, paddingTop, 0, mMostVisitedLayout.getPaddingBottom());
        }

        class LargeIconCallbackImpl implements LargeIconCallback {
            private MostVisitedItem mItem;
            private MostVisitedItemView mItemView;
            private boolean mIsInitialLoad;

            public LargeIconCallbackImpl(
                    MostVisitedItem item, MostVisitedItemView itemView, boolean isInitialLoad) {
                mItem = item;
                mItemView = itemView;
                mIsInitialLoad = isInitialLoad;
            }

            @Override
            public void onLargeIconAvailable(Bitmap icon, int fallbackColor) {
                if (icon == null) {
                    mIconGenerator.setBackgroundColor(fallbackColor);
                    icon = mIconGenerator.generateIconForUrl(mItem.getUrl());
                    mItemView.setIcon(new BitmapDrawable(getResources(), icon));
                    mItem.setTileType(fallbackColor == ICON_BACKGROUND_COLOR
                            ? MostVisitedTileType.ICON_DEFAULT : MostVisitedTileType.ICON_COLOR);
                } else {
                    RoundedBitmapDrawable roundedIcon = RoundedBitmapDrawableFactory.create(
                            getResources(), icon);
                    int cornerRadius = Math.round(ICON_CORNER_RADIUS_DP
                            * getResources().getDisplayMetrics().density * icon.getWidth()
                            / mDesiredIconSize);
                    roundedIcon.setCornerRadius(cornerRadius);
                    roundedIcon.setAntiAlias(true);
                    roundedIcon.setFilterBitmap(true);
                    mItemView.setIcon(roundedIcon);
                    mItem.setTileType(MostVisitedTileType.ICON_REAL);
                }
                mSnapshotMostVisitedChanged = true;
                if (mIsInitialLoad) loadTaskCompleted();
            }
        }

        public View createMostVisitedItemView(
                LayoutInflater inflater, MostVisitedItem item, boolean isInitialLoad) {
            final MostVisitedItemView view = (MostVisitedItemView) inflater.inflate(
                    R.layout.most_visited_item, mMostVisitedLayout, false);
            view.setTitle(getTitleForDisplay(item.getTitle(), item.getUrl()));
            view.setOfflineAvailable(item.isOfflineAvailable());

            LargeIconCallback iconCallback = new LargeIconCallbackImpl(item, view, isInitialLoad);
            if (isInitialLoad) mPendingLoadTasks++;
            if (!loadWhitelistIcon(item, iconCallback)) {
                mManager.getLargeIconForUrl(item.getUrl(), mMinIconSize, iconCallback);
            }

            return view;
        }

        private boolean loadWhitelistIcon(MostVisitedItem item, LargeIconCallback iconCallback) {
            if (item.getWhitelistIconPath().isEmpty()) return false;

            Bitmap bitmap = BitmapFactory.decodeFile(item.getWhitelistIconPath());
            if (bitmap == null) {
                Log.d(TAG, "Image decoding failed: %s", item.getWhitelistIconPath());
                return false;
            }
            iconCallback.onLargeIconAvailable(bitmap, Color.BLACK);
            return true;
        }

        public void onIconUpdated(final String url) {
            if (mMostVisitedItems == null) return;
            // Find a matching most visited item.
            for (MostVisitedItem item : mMostVisitedItems) {
                if (item.getUrl().equals(url)) {
                    LargeIconCallback iconCallback = new LargeIconCallbackImpl(
                            item, (MostVisitedItemView) item.getView(), false);
                    mManager.getLargeIconForUrl(url, mMinIconSize, iconCallback);
                    break;
                }
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mNewTabPageLayout != null) {
            mNewTabPageLayout.setParentViewportHeight(MeasureSpec.getSize(heightMeasureSpec));
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (mUseCardsUi) {
            mRecyclerView.updatePeekingCard();
            mRecyclerView.updateSnippetsHeaderDisplay();
        }
    }

    private int getVerticalScroll() {
        if (mUseCardsUi) {
            return mRecyclerView.computeVerticalScrollOffset();
        } else {
            return mScrollView.getScrollY();
        }
    }
}
