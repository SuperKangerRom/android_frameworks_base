/*
 * Copyright (C) 2016 Brett Rogers (rogersb11)
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
 * limitations under the License
 */

package com.android.systemui.vrtoxin;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.PorterDuff.Mode;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.View;
import android.view.HapticFeedbackConstants;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.statusbar.phone.ActivityStarter;

import com.android.systemui.R;

import com.android.internal.util.vrtoxin.ExpansionViewColorHelper;

public class ExpansionView extends FrameLayout {

    private ActivityStarter mActivityStarter;

    private boolean mListening = false;

    private View mShortcutPanel;

    // Weather
    private WeatherBarContainer mWeatherPanel;

    private TextView mCustomText;
    private ImageView mShadeRomLogo;
    private ImageView mLayoutChanger;
 
    private boolean mShow = false;
    private boolean mShowChanger = false;
    private boolean mShortcutPanelVisible = false;
    private boolean mWeatherAvailable = false;

    public ExpansionView(Context context) {
        this(context, null);
    }

    public ExpansionView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ExpansionView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setUp(ActivityStarter starter) {
        mActivityStarter = starter;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
 
        mShortcutPanel = findViewById(R.id.expansion_view_shortcut_panel);
        mWeatherPanel = (WeatherBarContainer) findViewById(R.id.expansion_view_weather_container);
        mCustomText = (TextView) findViewById(R.id.custom_text);
        mShadeRomLogo = (ImageView) findViewById(R.id.expansion_view_rom_logo);
        mLayoutChanger = (ImageView) findViewById(R.id.expansion_view_layout_changer);

        mCustomText.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                doHapticKeyClick(HapticFeedbackConstants.VIRTUAL_KEY);
                handleTextLongClick();
            return true;
            }
        });

        mShadeRomLogo.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                doHapticKeyClick(HapticFeedbackConstants.VIRTUAL_KEY);
                handleLogoLongClick();
            return true;
            }
        });

        mLayoutChanger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doHapticKeyClick(HapticFeedbackConstants.VIRTUAL_KEY);
                changeView(true);
            }
        });

        changeView(false);
    }

    public void changeView(boolean animate) {
        if (!mShortcutPanelVisible) {
            mWeatherPanel.setVisibility(View.INVISIBLE);
            mShortcutPanel.setVisibility(View.VISIBLE);
            if (animate) {
                mShortcutPanel.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.push_right_in));
                mWeatherPanel.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.push_left_out));
             }
             mShortcutPanelVisible = true;
        } else {
            mShortcutPanel.setVisibility(View.INVISIBLE);
            mWeatherPanel.setVisibility(View.VISIBLE);
            if (animate) {
                mWeatherPanel.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.push_left_in));
                mShortcutPanel.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.push_right_out));
             }
             mShortcutPanelVisible = false;
         }
         mWeatherPanel.setListening(!mShortcutPanelVisible);
    }

    public void showRomLogo(boolean show) {
        mShow = show;
        if (mShow) {
            mShadeRomLogo.setVisibility(View.VISIBLE);
        } else {
            mShadeRomLogo.setVisibility(View.GONE);
        }
    }

    public void showLayoutChanger(boolean showChanger) {
        mShowChanger = showChanger;
        if (mShowChanger) {
            mLayoutChanger.setVisibility(View.VISIBLE);
        } else {
            mLayoutChanger.setVisibility(View.GONE);
        }
    }
 
    public void setCustomText(String text) {
        mCustomText.setText(text);
    }

    public void setBackgroundColor() {
        final int backgroundColor = ExpansionViewColorHelper.getBackgroundColor(mContext);
        final boolean showBackground = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.EXPANSION_VIEW_BACKGROUND, 0) == 1;
        ((Drawable) getBackground()).setTint(backgroundColor);
        ((Drawable) getBackground()).setAlpha(showBackground ? 255 : 0);
    }

    public void updateIconColor(int color) {
        if (mLayoutChanger != null) {
            mLayoutChanger.setColorFilter(color, Mode.MULTIPLY);
        }
    }

    public void setRippleColor() {
        RippleDrawable background =
                (RippleDrawable) mContext.getDrawable(R.drawable.ripple_drawable_rectangle).mutate();
        RippleDrawable ovalBackground =
                (RippleDrawable) mContext.getDrawable(R.drawable.ripple_drawable_oval).mutate();
        final int color = ExpansionViewColorHelper.getNormalRippleColor(mContext);
        background.setColor(ColorStateList.valueOf(color));
        mCustomText.setBackground(background);
    }

    public void updateTextColor(int color) {
        mCustomText.setTextColor(color);
        //((TextView) findViewById(R.id.rom_logo_text)).setTextColor(color);
    }

    public void setTextSize(int size) {
        mCustomText.setTextSize(size);
    }

    public void setTypeface(Typeface tf) {
        mCustomText.setTypeface(tf);
    }

    private void handleTextLongClick() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName("com.android.settings",
            "com.android.settings.Settings$ExpansionViewSettingsActivity");
        mActivityStarter.startActivity(intent, true /* dismissShade */);
    }

    private void handleLogoLongClick() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName("com.android.settings",
            "com.android.settings.Settings$MainSettingsActivity");
        mActivityStarter.startActivity(intent, true /* dismissShade */);
    }

    private void doHapticKeyClick(int type) {
        performHapticFeedback(type,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
                | HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_BACKGROUND),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_BACKGROUND_COLOR),
                    false, this, UserHandle.USER_ALL);
            update();
        }

        void unobserve() {
            mContext.getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_BACKGROUND))
                || uri.equals(Settings.System.getUriFor(
                    Settings.System.EXPANSION_VIEW_BACKGROUND_COLOR))) {
                setBackgroundColor();
            }

        }

       public void update() {
            setBackgroundColor();
        }
    }
}
