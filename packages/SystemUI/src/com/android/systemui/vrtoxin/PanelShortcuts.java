/*
 * Copyright (C) 2014 Slimroms
 *               2015 CyanideL (Brett Rogers)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.systemui.vrtoxin;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.PorterDuff.Mode;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.Display;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.android.internal.util.vrtoxin.ImageHelper;
import com.android.internal.util.vrtoxin.ExpansionViewColorHelper;
import com.android.internal.util.vrtoxin.AppHelper;
import com.android.internal.util.vrtoxin.ActionHelper;
import com.android.internal.util.vrtoxin.ActionConfig;
import com.android.internal.util.vrtoxin.Action;

import com.android.systemui.R;

import java.util.ArrayList;

public class PanelShortcuts extends LinearLayout {

    private Handler mHandler = new Handler();
    private SettingsObserver mSettingsObserver;
    private PackageManager mPackageManager;
    private Context mContext;

    public PanelShortcuts(Context context) {
        this(context, null);
    }

    public PanelShortcuts(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mPackageManager = mContext.getPackageManager();
        mSettingsObserver = new SettingsObserver(mHandler);
        createShortcuts();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    @Override
    public void onAttachedToWindow() {
        mSettingsObserver.observe();

    }

    @Override
    public void onDetachedFromWindow() {
        mSettingsObserver.unobserve();
    }

    private void createShortcuts() {
        ArrayList<ActionConfig> actionConfigs = ActionHelper.getPanelShortcutsConfig(mContext);
        if (actionConfigs.size() == 0) {
            setVisibility(View.GONE);
            return;
        }
        setVisibility(View.VISIBLE);

        int clickType = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.PANEL_SHORTCUTS_PRESS_TYPE, 1, UserHandle.USER_CURRENT);
        int iconSize = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.PANEL_SHORTCUTS_ICON_SIZE, 36);

        ActionConfig actionConfig;

        for (int j = 0; j < actionConfigs.size(); j++) {
            actionConfig = actionConfigs.get(j);

            final String action = actionConfig.getClickAction();
            ImageView i = new ImageView(mContext);
            int dimens = Math.round(mContext.getResources().getDimensionPixelSize(
                    com.android.internal.R.dimen.app_icon_size));
            LinearLayout.LayoutParams vp =
                    new LinearLayout.LayoutParams(dimens, dimens);
            i.setLayoutParams(vp);
            i.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

            Drawable d = ImageHelper.resize(mContext,
                    ActionHelper.getActionIconImage(
                    mContext, actionConfig.getClickAction(), actionConfig.getIcon()), iconSize);

            setIconColor(i, d);
            setRippleColor(i, d);
            i.setContentDescription(AppHelper.getFriendlyNameForUri(
                    mContext, mPackageManager, actionConfig.getClickAction()));
            i.setClickable(true);

            if (clickType == 0) {
                i.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        doHapticKeyClick(HapticFeedbackConstants.VIRTUAL_KEY);
                        Action.processAction(mContext, action, false);
                    }
                });
            } else if (clickType == 1) {
                final GestureDetector gestureDetector = new GestureDetector(mContext,
                        new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        doHapticKeyClick(HapticFeedbackConstants.VIRTUAL_KEY);
                        Action.processAction(mContext, action, false);
                        return true;
                    }
                });
                i.setOnTouchListener(new OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent event) {
                        gestureDetector.onTouchEvent(event);
                        return true;
                    }
                });
            } else {
                i.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        doHapticKeyClick(HapticFeedbackConstants.LONG_PRESS);
                        Action.processAction(mContext, action, true);
                        return true;
                    }
                });
            }

            addView(i);
            if (iconSize != 24) {
                if (j+1 < actionConfigs.size()) {
                    addSeparator(iconSize == 36);
                }
            }
        }
    }

    private void setIconColor(ImageView iv, Drawable d) {
        final int iconColor = ExpansionViewColorHelper.getIconColor(mContext, d);

        iv.setColorFilter(null);
        if (ExpansionViewColorHelper.getIconColorMode(mContext) == 2
                && !ExpansionViewColorHelper.isGrayscaleIcon(mContext, d)) {
            iv.setImageBitmap(ImageHelper.getColoredBitmap(d, iconColor));
        } else {
            iv.setImageBitmap(ImageHelper.drawableToBitmap(d));
            if (iconColor != 0) {
                iv.setColorFilter(iconColor, Mode.MULTIPLY);
            }
        }
    }

    private void setRippleColor(ImageView iv, Drawable d) {
        RippleDrawable rd = (RippleDrawable) mContext.getDrawable(R.drawable.buttons_bar_ripple_drawable);
        final int rippleColor = ExpansionViewColorHelper.getRippleColor(mContext, d);

        if (rippleColor == 0) {
            iv.setColorFilter(rippleColor, Mode.MULTIPLY);

        }

        int states[][] = new int[][] {
            new int[] {
                com.android.internal.R.attr.state_enabled
            }
        };
        int colors[] = new int[] {
            rippleColor
        };
        ColorStateList color = new ColorStateList(states, colors);

        rd.setColor(color);
        iv.setBackground(rd);
    }

    public void doHapticKeyClick(int type) {
        performHapticFeedback(type,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                | HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
    }

    private void addSeparator(boolean useHalfWidth) {
        int width = mContext.getResources().getDimensionPixelSize(
                R.dimen.shortcut_seperater_width);
        if (useHalfWidth) {
            width = width / 2;
        }
        View v = new View(mContext);
        LinearLayout.LayoutParams vp =
                new LinearLayout.LayoutParams(width, 0);
        v.setLayoutParams(vp);
        addView(v);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.PANEL_SHORTCUTS),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.PANEL_SHORTCUTS_PRESS_TYPE),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.PANEL_SHORTCUTS_ICON_COLOR),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.PANEL_SHORTCUTS_ICON_COLOR_MODE),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.PANEL_SHORTCUTS_RIPPLE_COLOR_MODE),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.PANEL_SHORTCUTS_RIPPLE_COLOR),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.PANEL_SHORTCUTS_ICON_SIZE),
                    false, this, UserHandle.USER_ALL);
            update();
        }

        void unobserve() {
            mContext.getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
            update();
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            update();
        }

        public void update() {
            removeAllViews();
            createShortcuts();
        }
    }
}
