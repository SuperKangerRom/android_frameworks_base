/*
* Copyright (C) 2015 DarkKat
*               2015 CyanideL (Brett Rogers)
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.android.internal.util.vrtoxin;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.provider.Settings;

import com.android.internal.util.NotificationColorUtil;

public class ExpansionViewColorHelper {

    private static int WHITE = 0xffffffff;
    private static int VRTOXIN_BLUE = 0xff1976D2;

    public static int getIconColor(Context context, Drawable d) {
        if (colorizeIcon(context, d)) {
           return getCustomIconColor(context);
        } else {
            return 0;
        }
    }

    public static int getRippleColor(Context context, Drawable d) {
        final int iconColorMode = getIconColorMode(context);
        final int rippleColorMode = getRippleColorMode(context);
        final boolean isGreyscale = isGrayscaleIcon(context, d);

        if (rippleColorMode == 0) {
            if (iconColorMode == 0) {
                return isGreyscale ? WHITE : 0; 
            } else if (iconColorMode == 1) {
                return isGreyscale ? getCustomIconColor(context) : 0;
            } else {
                return getCustomIconColor(context);
            }
        } else if (rippleColorMode == 1) {
            return getCustomRippleColor(context);
        } else {
            return WHITE;
        }
    }

    private static boolean colorizeIcon(Context context, Drawable d) {
        if (d == null) {
            return false;
        }

        final int iconColorMode = getIconColorMode(context);
        final boolean isGreyscale = isGrayscaleIcon(context, d);

        if (iconColorMode == 0) {
            return false;
        } else if (iconColorMode == 1) {
            return isGreyscale;
        } else {
            return true;
        }
    }

    public static boolean isGrayscaleIcon(Context context, Drawable d) {
        NotificationColorUtil cu = NotificationColorUtil.getInstance(context);
        return cu.isGrayscaleIcon(d);
    }

    public static int getIconColorMode(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.PANEL_SHORTCUTS_ICON_COLOR_MODE, 0);
    }

    private static int getRippleColorMode(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.PANEL_SHORTCUTS_RIPPLE_COLOR_MODE, 2);
    }

    private static int getCustomIconColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.PANEL_SHORTCUTS_ICON_COLOR, WHITE);
    }

    private static int getCustomRippleColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.PANEL_SHORTCUTS_RIPPLE_COLOR, WHITE);
    }

    public static int getExpansionViewTextColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.EXPANSION_VIEW_TEXT_COLOR,
                WHITE);
    }

    public static int getExpansionViewIconColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.EXPANSION_VIEW_ICON_COLOR,
                WHITE);
    }

    public static int getWeatherIconColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.EXPANSION_VIEW_WEATHER_ICON_COLOR,
                WHITE);
    }

    public static int getWeatherTextColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.EXPANSION_VIEW_WEATHER_TEXT_COLOR,
                WHITE);
    }

    public static int getBackgroundColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.EXPANSION_VIEW_BACKGROUND_COLOR, WHITE);
    }

    public static int getNormalRippleColor(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.EXPANSION_VIEW_RIPPLE_COLOR, WHITE);
    }
}
