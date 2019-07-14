/*
 * Copyright (C) 2019 >ion
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.keyguard.clocks;

import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.ParcelFileDescriptor;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v7.graphics.Palette;
import android.text.format.DateUtils;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

import com.android.systemui.R;

import java.lang.IllegalStateException;
import java.lang.NullPointerException;
import java.util.TimeZone;

public class CustomTextClock extends TextView {

    final Resources res = getResources();

    private final String[] TensString = res.getStringArray(R.array.TensString);
    private final String[] UnitsString = res.getStringArray(R.array.UnitsString);
    private final String[] TensStringH = res.getStringArray(R.array.TensStringH);
    private final String[] UnitsStringH = res.getStringArray(R.array.UnitsStringH);

    private Time mCalendar;
    private boolean mAttached;
    private int handType;
    private Context mContext;
    private boolean h24;
    private int mClockColor;

    public CustomTextClock(Context context) {
        this(context, null);
    }

    public CustomTextClock(Context context, AttributeSet attrs) {
        super(context, attrs);

        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.CustomTextClock);

        handType = a.getInteger(R.styleable.CustomTextClock_HandType, 2);

        mContext = context;
        mCalendar = new Time();

        refreshLockFont();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter();

            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);

            // OK, this is gross but needed. This class is supported by the
            // remote views machanism and as a part of that the remote views
            // can be inflated by a context for another user without the app
            // having interact users permission - just for loading resources.
            // For exmaple, when adding widgets from a user profile to the
            // home screen. Therefore, we register the receiver as the current
            // user not the one the context is for.
            getContext().registerReceiverAsUser(mIntentReceiver,
                    android.os.Process.myUserHandle(), filter, null, getHandler());
        }

        // NOTE: It's safe to do these after registering the receiver since the receiver always runs
        // in the main thread, therefore the receiver can't run before this method returns.

        // The time zone may have changed while the receiver wasn't registered, so update the Time
        mCalendar = new Time();

        // Make sure we update to the current time
        onTimeChanged();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAttached) {
            getContext().unregisterReceiver(mIntentReceiver);
            mAttached = false;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (handType == 2) {
            mClockColor = Settings.System.getIntForUser(mContext.getContentResolver(),
                    Settings.System.LOCKSCREEN_CLOCK_SELECTION, 0,
                    UserHandle.USER_CURRENT);

            if ( mClockColor != 15 ) {
                Bitmap mBitmap;
                //Get wallpaper as bitmap
                WallpaperManager manager = WallpaperManager.getInstance(mContext);
                ParcelFileDescriptor pfd = manager.getWallpaperFile(WallpaperManager.FLAG_LOCK);

                //Sometimes lock wallpaper maybe null as getWallpaperFile doesnt return builtin wallpaper
                if (pfd == null)
                    pfd = manager.getWallpaperFile(WallpaperManager.FLAG_SYSTEM);
                try {
                    if (pfd != null)
                    {
                        mBitmap = BitmapFactory.decodeFileDescriptor(pfd.getFileDescriptor());
                    } else {
                        //Incase both cases return null wallpaper, generate a yellow bitmap
                        mBitmap = drawEmpty();
                    }
                    Palette palette = Palette.generate(mBitmap);

                    //For monochrome and single color bitmaps, the value returned is 0
                    if (Color.valueOf(palette.getLightVibrantColor(0x000000)).toArgb() == 0) {
                        //So get bodycolor on dominant color instead as a hacky workaround
                        setTextColor(palette.getDominantSwatch().getBodyTextColor());
                    //On Black Wallpapers set color to White
                    } else if(String.format("#%06X", (0xFFFFFF & (palette.getLightVibrantColor(0x000000)))) == "#000000") {
                        setTextColor(Color.WHITE);
                    } else {
                        setTextColor((Color.valueOf(palette.getLightVibrantColor(0xff000000))).toArgb());
                    }

                  //Just a fallback, although I doubt this case will ever come
                } catch (NullPointerException e) {
                    setTextColor(Color.WHITE);
                }
            } else {
                setTextColor(mContext.getResources().getColor(R.color.coverart_accent));
            }
        }
        refreshLockFont();
    }

    private void onTimeChanged() {
        mCalendar.setToNow();
        h24 = DateFormat.is24HourFormat(getContext());

        int hour = mCalendar.hour;
        int minute = mCalendar.minute;

        Log.d("CustomTextClock", ""+h24);

        if (!h24) {
            if (hour > 12) {
                hour = hour - 12;
            }
        }

        switch(handType){
            case 0:
                if (hour == 12 && minute == 0) {
                setText(res.getString(R.string.text_clock_high));
                } else {
                setText(getIntStringHour(hour));
                }
                break;
            case 1:
                if (hour == 12 && minute == 0) {
                setText(res.getString(R.string.text_clock_noon));
                } else {
                setText(getIntStringMin(minute));
                }
                break;
            default:
                break;
        }

        updateContentDescription(mCalendar, getContext());
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                String tz = intent.getStringExtra("time-zone");
                mCalendar = new Time(TimeZone.getTimeZone(tz).getID());
            }
            onTimeChanged();
            invalidate();
        }
    };

    private void updateContentDescription(Time time, Context mContext) {
        final int flags = DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_24HOUR;
        String contentDescription = DateUtils.formatDateTime(mContext,
                time.toMillis(false), flags);
        setContentDescription(contentDescription);
    }

    private String getIntStringHour (int num) {
        int tens, units;
        String NumString = "";
        if(num >= 20) {
            units = num % 10 ;
            tens =  num / 10;
            if ( units == 0 ) {
                NumString = TensStringH[tens];
            } else {
                NumString = TensStringH[tens]+" "+UnitsStringH[units];
            }
        } else if (num < 20 ) {
            NumString = UnitsStringH[num];
        }

        return NumString;
    }

    private String getIntStringMin (int num) {
        int tens, units;
        String NumString = "";
        if(num >= 20) {
            units = num % 10 ;
            tens =  num / 10;
            if ( units == 0 ) {
                NumString = TensString[tens];
            } else {
                NumString = TensString[tens] + " " + UnitsString[units];
            }
        } else if (num < 10 ) {
            NumString = res.getString(R.string.text_clock_zero_h_min) +
                    " " + UnitsString[num];
        } else if (num >= 10 && num < 20) {
            NumString = UnitsString[num];
        }
        return NumString;
    }

    private int getLockClockFont() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.LOCK_CLOCK_FONTS, 0);
    }

    private void refreshLockFont() {
        final Resources res = getContext().getResources();
        boolean isPrimary = UserHandle.getCallingUserId() == UserHandle.USER_OWNER;
        int lockClockFont = isPrimary ? getLockClockFont() : 0;
        if (lockClockFont == 0) {
            setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        }
        if (lockClockFont == 1) {
            setTypeface(Typeface.create("sans-serif", Typeface.ITALIC));
        }
        if (lockClockFont == 2) {
            setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        }
        if (lockClockFont == 3) {
            setTypeface(Typeface.create("sans-serif", Typeface.BOLD_ITALIC));
        }
        if (lockClockFont == 4) {
            setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        }
        if (lockClockFont == 5) {
            setTypeface(Typeface.create("sans-serif-light", Typeface.ITALIC));
        }
        if (lockClockFont == 6) {
            setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        }
        if (lockClockFont == 7) {
            setTypeface(Typeface.create("sans-serif-thin", Typeface.ITALIC));
        }
        if (lockClockFont == 8) {
            setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
        }
        if (lockClockFont == 9) {
            setTypeface(Typeface.create("sans-serif-condensed", Typeface.ITALIC));
        }
        if (lockClockFont == 10) {
            setTypeface(Typeface.create("sans-serif-condensed-light", Typeface.NORMAL));
        }
        if (lockClockFont == 11) {
            setTypeface(Typeface.create("sans-serif-condensed-light", Typeface.ITALIC));
        }
        if (lockClockFont == 12) {
            setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        }
        if (lockClockFont == 13) {
            setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC));
        }
        if (lockClockFont == 14) {
            setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        }
        if (lockClockFont == 15) {
            setTypeface(Typeface.create("sans-serif-medium", Typeface.ITALIC));
        }
        if (lockClockFont == 16) {
            setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
        }
        if (lockClockFont == 17) {
            setTypeface(Typeface.create("sans-serif-black", Typeface.ITALIC));
        }
        if (lockClockFont == 18) {
            setTypeface(Typeface.create("abelreg", Typeface.NORMAL));
        }
        if (lockClockFont == 19) {
            setTypeface(Typeface.create("adamcg-pro", Typeface.NORMAL));
        }
        if (lockClockFont == 20) {
            setTypeface(Typeface.create("adventpro", Typeface.NORMAL));
        }
        if (lockClockFont == 21) {
            setTypeface(Typeface.create("alexana-neue", Typeface.NORMAL));
        }
        if (lockClockFont == 22) {
            setTypeface(Typeface.create("alien-league", Typeface.NORMAL));
        }
        if (lockClockFont == 23) {
            setTypeface(Typeface.create("archivonar", Typeface.NORMAL));
        }
        if (lockClockFont == 24) {
            setTypeface(Typeface.create("autourone", Typeface.NORMAL));
        }
        if (lockClockFont == 25) {
            setTypeface(Typeface.create("azedo-light", Typeface.NORMAL));
        }
        if (lockClockFont == 26) {
            setTypeface(Typeface.create("badscript", Typeface.NORMAL));
        }
        if (lockClockFont == 27) {
            setTypeface(Typeface.create("bignoodle-regular", Typeface.NORMAL));
        }
        if (lockClockFont == 28) {
            setTypeface(Typeface.create("biko", Typeface.NORMAL));
        }
        if (lockClockFont == 29) {
            setTypeface(Typeface.create("blern", Typeface.NORMAL));
        }
        if (lockClockFont == 30) {
            setTypeface(Typeface.create("cherryswash", Typeface.NORMAL));
        }
        if (lockClockFont == 31) {
            setTypeface(Typeface.create("cocon", Typeface.NORMAL));
        }
        if (lockClockFont == 32) {
            setTypeface(Typeface.create("codystar", Typeface.NORMAL));
        }
        if (lockClockFont == 33) {
            setTypeface(Typeface.create("fester", Typeface.NORMAL));
        }
        if (lockClockFont == 34) {
            setTypeface(Typeface.create("fox-and-cat", Typeface.NORMAL));
        }
        if (lockClockFont == 35) {
            setTypeface(Typeface.create("ginora-sans", Typeface.NORMAL));
        }
        if (lockClockFont == 36) {
            setTypeface(Typeface.create("gobold-sys", Typeface.NORMAL));
        }
        if (lockClockFont == 37) {
            setTypeface(Typeface.create("ibmplex-mono", Typeface.NORMAL));
        }
        if (lockClockFont == 38) {
            setTypeface(Typeface.create("inkferno", Typeface.NORMAL));
        }
        if (lockClockFont == 39) {
            setTypeface(Typeface.create("instruction", Typeface.NORMAL));
        }
        if (lockClockFont == 40) {
            setTypeface(Typeface.create("jacklane", Typeface.NORMAL));
        }
        if (lockClockFont == 41) {
            setTypeface(Typeface.create("jura-reg", Typeface.NORMAL));
        }
        if (lockClockFont == 42) {
            setTypeface(Typeface.create("kellyslab", Typeface.NORMAL));
        }
        if (lockClockFont == 43) {
            setTypeface(Typeface.create("metropolis1920", Typeface.NORMAL));
        }
        if (lockClockFont == 44) {
            setTypeface(Typeface.create("monad", Typeface.NORMAL));
        }
        if (lockClockFont == 45) {
            setTypeface(Typeface.create("neonneon", Typeface.NORMAL));
        }
        if (lockClockFont == 46) {
            setTypeface(Typeface.create("noir", Typeface.NORMAL));
        }
        if (lockClockFont == 47) {
            setTypeface(Typeface.create("northfont", Typeface.NORMAL));
        }
        if (lockClockFont == 48) {
            setTypeface(Typeface.create("outrun-future", Typeface.NORMAL));
        }
        if (lockClockFont == 49) {
            setTypeface(Typeface.create("pompiere", Typeface.NORMAL));
        }
        if (lockClockFont == 50) {
            setTypeface(Typeface.create("qontra", Typeface.NORMAL));
        }
        if (lockClockFont == 51) {
            setTypeface(Typeface.create("raleway-light", Typeface.NORMAL));
        }
        if (lockClockFont == 52) {
            setTypeface(Typeface.create("reemkufi", Typeface.NORMAL));
        }
        if (lockClockFont == 53) {
            setTypeface(Typeface.create("riviera", Typeface.NORMAL));
        }
        if (lockClockFont == 54) {
            setTypeface(Typeface.create("roadrage-sys", Typeface.NORMAL));
        }
        if (lockClockFont == 55) {
            setTypeface(Typeface.create("satisfy", Typeface.NORMAL));
        }
        if (lockClockFont == 56) {
            setTypeface(Typeface.create("seaweedsc", Typeface.NORMAL));
        }
        if (lockClockFont == 57) {
            setTypeface(Typeface.create("sedgwick-ave", Typeface.NORMAL));
        }
        if (lockClockFont == 58) {
            setTypeface(Typeface.create("snowstorm-sys", Typeface.NORMAL));
        }
        if (lockClockFont == 59) {
            setTypeface(Typeface.create("themeable-clock", Typeface.NORMAL));
        }
        if (lockClockFont == 60) {
            setTypeface(Typeface.create("the-outbox", Typeface.NORMAL));
        }
        if (lockClockFont == 61) {
            setTypeface(Typeface.create("unionfont", Typeface.NORMAL));
        }
        if (lockClockFont == 62) {
            setTypeface(Typeface.create("vibur", Typeface.NORMAL));
        }
        if (lockClockFont == 63) {
            setTypeface(Typeface.create("voltaire", Typeface.NORMAL));
        }
    }

    private Bitmap drawEmpty() {
        Bitmap convertedBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(convertedBitmap);
        Paint paint = new Paint();
        paint.setColor(Color.YELLOW);
        canvas.drawPaint(paint);
        return convertedBitmap;
    }
}
