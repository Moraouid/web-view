package com.saidweb.webviewpro.fragments;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.saidweb.webviewpro.R;
import com.saidweb.webviewpro.activities.MainActivity;
import com.saidweb.webviewpro.databases.prefs.SharedPref;
import com.saidweb.webviewpro.listener.DrawerStateListener;
import com.saidweb.webviewpro.listener.LoadUrlListener;
import com.saidweb.webviewpro.listener.WebViewOnKeyListener;
import com.saidweb.webviewpro.listener.WebViewOnTouchListener;
import com.saidweb.webviewpro.utils.MyFragment;
import com.saidweb.webviewpro.utils.Utils;
import com.saidweb.webviewpro.webview.AdvancedWebView;
import com.saidweb.webviewpro.webview.VideoEnabledWebChromeClient;
import com.saidweb.webviewpro.webview.VideoEnabledWebView;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.Objects;

public class FragmentWebView extends MyFragment implements AdvancedWebView.Listener {

    private MainActivity mainActivity;
    private Toolbar toolbar;
    private int mStoredActivityRequestCode;
    private int mStoredActivityResultCode;
    private Intent mStoredActivityResultIntent;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View lytNoNetwork;
    private View lytNoPage;
    View rootView, parentView;
    AdvancedWebView webView;
    String pageType;
    String pageUrl;
    String pageTitle;
    Button btnRetry;
    SharedPref sharedPref;
    StringBuilder adservers;

    private void readAdServers() {
        String line = "";
        adservers = new StringBuilder();

        InputStream is = this.getResources().openRawResource(R.raw.adblockserverlist);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        if(is != null) {
            try {
                while ((line = br.readLine()) != null) {
                    adservers.append(line);
                    adservers.append("\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public FragmentWebView() {

    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mainActivity = (MainActivity) context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        readAdServers();
        setHasOptionsMenu(true);
        setRetainInstance(true);
        if (getArguments() != null) {
            pageTitle = getArguments().getString("name");
            pageType = getArguments().getString("type");
            if (pageType.equals("assets")) {
                pageUrl = "file:///android_asset/" + getArguments().getString("url");
            } else {
                pageUrl = getArguments().getString("url");
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_webview, container, false);
        if (getActivity() != null) {
            parentView = getActivity().findViewById(R.id.main_non_video_layout);
        }
        sharedPref = new SharedPref(getActivity());
        initView();
        setupToolbar();
        return rootView;
    }

    private void initView() {
        swipeRefreshLayout = rootView.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        swipeRefreshLayout.setRefreshing(true);
        webView = rootView.findViewById(R.id.webView);
        lytNoNetwork = rootView.findViewById(R.id.lyt_no_network);
        lytNoPage = rootView.findViewById(R.id.lyt_no_page);
        btnRetry = rootView.findViewById(R.id.btn_retry);
        toolbar = rootView.findViewById(R.id.toolbar);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (sharedPref.getNavigationDrawer().equals("true")) {
            mainActivity.setupNavigationDrawer(toolbar);
        }
        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        }

        initComponents();

        if (sharedPref.getGeolocation().endsWith("true")) {
            Utils.checkPermissionAccessLocation(this);
        }

        loadData();

        swipeRefreshLayout.setOnRefreshListener(this::refreshData);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (Utils.checkPermissionReadExternalStorageAndCamera(this)) {
            webView.onActivityResult(requestCode, resultCode, intent);
        } else {
            mStoredActivityRequestCode = requestCode;
            mStoredActivityResultCode = resultCode;
            mStoredActivityResultIntent = intent;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        setUserVisibleHint(true);
        webView.saveState(outState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case Utils.REQUEST_PERMISSION_READ_EXTERNAL_STORAGE_AND_CAMERA:
            case Utils.REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE:
            case Utils.REQUEST_PERMISSION_ACCESS_LOCATION: {
                if (grantResults.length > 0) {
                    for (int grantResult : grantResults) {
                        if (grantResult == PackageManager.PERMISSION_GRANTED) {
                            if (requestCode == Utils.REQUEST_PERMISSION_READ_EXTERNAL_STORAGE_AND_CAMERA) {
                                if (mStoredActivityResultIntent != null) {
                                    webView.onActivityResult(mStoredActivityRequestCode, mStoredActivityResultCode, mStoredActivityResultIntent);
                                    mStoredActivityRequestCode = 0;
                                    mStoredActivityResultCode = 0;
                                    mStoredActivityResultIntent = null;
                                }
                            }
                        }
                    }
                }
                break;
            }
        }
    }

    @SuppressLint({"ClickableViewAccessibility", "SetJavaScriptEnabled"})
    private void initComponents() {
        webView.getSettings().setJavaScriptEnabled(true);

        //webView cache
        if (sharedPref.getCache().equals("true")) {
            webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
            webView.getSettings().setDomStorageEnabled(true);
            webView.getSettings().setDatabaseEnabled(true);
        }


        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(false);

        //advanced webView settings
        webView.setListener(getActivity(), FragmentWebView.this);

        if (sharedPref.getGeolocation().equals("true")) {
            webView.getSettings().setGeolocationEnabled(true);
            webView.setGeolocationEnabled(true);
        }

        //webView style
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);

        //webView hardware acceleration
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        //webView chrome client
        View nonVideoLayout = requireActivity().findViewById(R.id.main_non_video_layout);
        ViewGroup videoLayout = getActivity().findViewById(R.id.main_video_layout);
        @SuppressLint("InflateParams") View progressView = getActivity().getLayoutInflater().inflate(R.layout.lyt_progress, null);
        VideoEnabledWebChromeClient webChromeClient = new VideoEnabledWebChromeClient(nonVideoLayout, videoLayout, progressView, (VideoEnabledWebView) webView);
        webChromeClient.setOnToggledFullscreen(new WebViewToggledFullscreenCallback());
        webView.setWebChromeClient(webChromeClient);

        //webView client
        webView.setWebViewClient(new MyWebViewClient());

        //webView key listener
        webView.setOnKeyListener(new WebViewOnKeyListener((DrawerStateListener) getActivity()));

        //webView touch listener
        webView.requestFocus(View.FOCUS_DOWN);
        webView.setOnTouchListener(new WebViewOnTouchListener());

    }

    private void loadData() {
        if (getActivity() != null) {
            if (Utils.isOnline(getActivity())) {
                webView.loadUrl(pageUrl);
            } else {
                swipeRefreshLayout.setRefreshing(false);
                lytNoNetwork.setVisibility(View.VISIBLE);
                webView.setVisibility(View.GONE);
                retryData();
            }
        }
    }

    public void refreshData() {
        if (getActivity() != null) {
            if (Utils.isOnline(getActivity())) {
                swipeRefreshLayout.setRefreshing(true);
                lytNoNetwork.setVisibility(View.GONE);
                lytNoPage.setVisibility(View.GONE);
                String pageUrl = webView.getUrl();
                if (pageUrl == null || pageUrl.equals("")) pageUrl = this.pageUrl;
                webView.loadUrl(pageUrl);
            } else {
                swipeRefreshLayout.setRefreshing(false);
                lytNoNetwork.setVisibility(View.VISIBLE);
                webView.setVisibility(View.GONE);
                retryData();
            }
        }
    }

    private void retryData() {
        btnRetry.setOnClickListener(v -> refreshData());
    }

    private class WebViewToggledFullscreenCallback implements VideoEnabledWebChromeClient.ToggledFullscreenCallback {
        @Override
        public void toggledFullscreen(boolean full_screen) {
            WindowManager.LayoutParams attrs = requireActivity().getWindow().getAttributes();
            if (full_screen) {
                attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
                attrs.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
                getActivity().getWindow().setAttributes(attrs);
                getActivity().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
            } else {
                attrs.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
                attrs.flags &= ~WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
                getActivity().getWindow().setAttributes(attrs);
                getActivity().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            }
        }
    }

    private class MyWebViewClient extends WebViewClient {

        private boolean mSuccess = true;

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            ByteArrayInputStream EMPTY = new ByteArrayInputStream("".getBytes());
            String kk5 = String.valueOf(adservers);

            if (kk5.contains(":::::" + request.getUrl().getHost())) {
                return new WebResourceResponse("text/plain", "utf-8", EMPTY);
            }
            return super.shouldInterceptRequest(view, request);
        }

        @SuppressWarnings("deprecation")
        @Override
        public void onPageFinished(final WebView view, final String url) {
            runTaskCallback(() -> {
                if (getActivity() != null && mSuccess) {
                    swipeRefreshLayout.setRefreshing(false);
                    webView.setVisibility(View.VISIBLE);
                    CookieSyncManager.getInstance().sync();
                }
                mSuccess = true;
            });
        }

        @Override
        public void onReceivedError(final WebView view, final int errorCode, final String description, final String failingUrl) {
            runTaskCallback(() -> {
                if (getActivity() != null) {
                    mSuccess = false;
                    lytNoPage.setVisibility(View.VISIBLE);
                    webView.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                }
            });
        }

        @TargetApi(Build.VERSION_CODES.M)
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            onReceivedError(view, error.getErrorCode(), error.getDescription().toString(), request.getUrl().toString());
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (Utils.isDownloadableFile(url)) {
                if (Build.VERSION.SDK_INT > 28) {
                    ((MainActivity) requireActivity()).showSnackBar(getString(R.string.msg_download));
                    Utils.downloadFile(requireActivity(), url, Utils.getFileName(url));
                } else if (Utils.checkPermissionWriteExternalStorage(FragmentWebView.this)) {
                    ((MainActivity) requireActivity()).showSnackBar(getString(R.string.msg_download));
                    Utils.downloadFile(requireActivity(), url, Utils.getFileName(url));
                    return true;
                }
                return true;
            } else if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                ((LoadUrlListener) requireActivity()).onLoadUrl(url);
                boolean external = isLinkExternal(url);
                boolean internal = isLinkInternal(url);
                if (!external && !internal) {
                    external = sharedPref.getOpenLinkInExternalBrowser().equals("true");
                }
                if (external) {
                    Utils.startWebActivity(requireContext(), url);
                    return true;
                } else {
                    swipeRefreshLayout.setRefreshing(true);
                    return false;
                }
            } else if (url != null && url.startsWith("file://")) {
                ((LoadUrlListener) requireActivity()).onLoadUrl(url);
                return false;
            } else {
                return Utils.startIntentActivity(getContext(), url);
            }
        }
    }

    @Override
    public void onPageStarted(String url, Bitmap favicon) {
    }

    @Override
    public void onPageFinished(String url) {
    }

    @Override
    public void onPageError(int errorCode, String description, String failingUrl) {
    }

    @Override
    public void onDownloadRequested(String url, String suggestedFilename, String mimeType, long contentLength, String contentDisposition, String userAgent) {
        if (Utils.checkPermissionWriteExternalStorage(FragmentWebView.this)) {
            ((MainActivity) requireActivity()).showSnackBar(getString(R.string.msg_download));
            Utils.downloadFile(requireActivity(), url, Utils.getFileName(url));
        }
    }

    @Override
    public void onExternalPageRequest(String url) {
    }

    private boolean isLinkExternal(String url) {
        for (String rule : Utils.LINKS_OPENED_IN_EXTERNAL_BROWSER) {
            if (url.contains(rule)) return true;
        }
        return false;
    }

    private boolean isLinkInternal(String url) {
        for (String rule : Utils.LINKS_OPENED_IN_INTERNAL_WEBVIEW) {
            if (url.contains(rule)) return true;
        }
        return false;
    }

    private void setupToolbar() {
        toolbar.setTitle(pageTitle);
        mainActivity.setSupportActionBar(toolbar);
        if (sharedPref.getToolbar().equals("true")) {
            toolbar.setVisibility(View.VISIBLE);
        } else {
            toolbar.setVisibility(View.GONE);
        }
        if (sharedPref.getIsDarkTheme()) {
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorToolbarDark));
            toolbar.getContext().setTheme(R.style.ThemeOverlay_AppCompat_Dark);
        } else {
            toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            toolbar.setPopupTheme(R.style.ThemeOverlay_AppCompat_Light);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        webView.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        webView.onDestroy();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        rootView = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

}