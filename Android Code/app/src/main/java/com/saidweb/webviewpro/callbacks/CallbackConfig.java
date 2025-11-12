package com.saidweb.webviewpro.callbacks;

import com.saidweb.webviewpro.models.Ads;
import com.saidweb.webviewpro.models.App;
import com.saidweb.webviewpro.models.Navigation;
import com.saidweb.webviewpro.models.Notification;

import java.util.ArrayList;
import java.util.List;

public class CallbackConfig {

    public App app = null;
    public List<Navigation> menus = new ArrayList<>();
    public Notification notification = null;
    public Ads ads = null;

}