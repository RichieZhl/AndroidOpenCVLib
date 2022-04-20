package com.richie.androidwechatqrdemo;

import android.app.Application;
import com.richie.opencvLib.OpenCVUtils;

/**
 * Created by lylaut on 2022/04/20
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        OpenCVUtils.init(this);
    }
}
