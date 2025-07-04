package com.example.iptvplayer;

import android.content.Context;
import xyz.doikki.videoplayer.controller.GestureVideoController;

public class StandardVideoController extends GestureVideoController {

    public StandardVideoController(Context context) {
        super(context);
    }

    @Override
    protected int getLayoutId() {
        return 0; // Ou o ID do layout correto, se houver um layout específico para este controlador
    }
}


