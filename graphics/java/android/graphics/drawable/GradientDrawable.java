/*
 * Copyright (C) 2006 The Android Open Source Project
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

package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.Resources.Theme;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;

import com.android.internal.R;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * A Drawable with a color gradient for buttons, backgrounds, etc. 
 *
 * <p>It can be defined in an XML file with the <code>&lt;shape></code> element. For more
 * information, see the guide to <a
 * href="{@docRoot}guide/topics/resources/drawable-resource.html">Drawable Resources</a>.</p>
 *
 * @attr ref android.R.styleable#GradientDrawable_visible
 * @attr ref android.R.styleable#GradientDrawable_shape
 * @attr ref android.R.styleable#GradientDrawable_innerRadiusRatio
 * @attr ref android.R.styleable#GradientDrawable_innerRadius
 * @attr ref android.R.styleable#GradientDrawable_thicknessRatio
 * @attr ref android.R.styleable#GradientDrawable_thickness
 * @attr ref android.R.styleable#GradientDrawable_useLevel
 * @attr ref android.R.styleable#GradientDrawableSize_width
 * @attr ref android.R.styleable#GradientDrawableSize_height
 * @attr ref android.R.styleable#GradientDrawableGradient_startColor
 * @attr ref android.R.styleable#GradientDrawableGradient_centerColor
 * @attr ref android.R.styleable#GradientDrawableGradient_endColor
 * @attr ref android.R.styleable#GradientDrawableGradient_useLevel
 * @attr ref android.R.styleable#GradientDrawableGradient_angle
 * @attr ref android.R.styleable#GradientDrawableGradient_type
 * @attr ref android.R.styleable#GradientDrawableGradient_centerX
 * @attr ref android.R.styleable#GradientDrawableGradient_centerY
 * @attr ref android.R.styleable#GradientDrawableGradient_gradientRadius
 * @attr ref android.R.styleable#GradientDrawableSolid_color
 * @attr ref android.R.styleable#GradientDrawableStroke_width
 * @attr ref android.R.styleable#GradientDrawableStroke_color
 * @attr ref android.R.styleable#GradientDrawableStroke_dashWidth
 * @attr ref android.R.styleable#GradientDrawableStroke_dashGap
 * @attr ref android.R.styleable#GradientDrawablePadding_left
 * @attr ref android.R.styleable#GradientDrawablePadding_top
 * @attr ref android.R.styleable#GradientDrawablePadding_right
 * @attr ref android.R.styleable#GradientDrawablePadding_bottom
 */
public class GradientDrawable extends Drawable {
    /**
     * Shape is a rectangle, possibly with rounded corners
     */
    public static final int RECTANGLE = 0;
    
    /**
     * Shape is an ellipse
     */
    public static final int OVAL = 1; 
    
    /**
     * Shape is a line
     */
    public static final int LINE = 2;

    /**
     * Shape is a ring.
     */
    public static final int RING = 3;

    /**
     * Gradient is linear (default.)
     */
    public static final int LINEAR_GRADIENT = 0;

    /**
     * Gradient is circular.
     */
    public static final int RADIAL_GRADIENT = 1;

    /**
     * Gradient is a sweep.
     */
    public static final int SWEEP_GRADIENT  = 2;

    /** Radius is in pixels. */
    private static final int RADIUS_TYPE_PIXELS = 0;

    /** Radius is a fraction of the base size. */
    private static final int RADIUS_TYPE_FRACTION = 1;

    /** Radius is a fraction of the bounds size. */
    private static final int RADIUS_TYPE_FRACTION_PARENT = 2;

    private static final float DEFAULT_INNER_RADIUS_RATIO = 3.0f;
    private static final float DEFAULT_THICKNESS_RATIO = 9.0f;

    private GradientState mGradientState;
    
    private final Paint mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Rect mPadding;
    private Paint mStrokePaint;   // optional, set by the caller
    private ColorFilter mColorFilter;   // optional, set by the caller
    private int mAlpha = 0xFF;  // modified by the caller
    private boolean mDither;

    private final Path mPath = new Path();
    private final RectF mRect = new RectF();

    private Paint mLayerPaint;    // internal, used if we use saveLayer()
    private boolean mRectIsDirty;   // internal state
    private boolean mMutated;
    private Path mRingPath;
    private boolean mPathIsDirty = true;

    /** Current gradient radius, valid when {@link #mRectIsDirty} is false. */
    private float mGradientRadius;

    /**
     * Controls how the gradient is oriented relative to the drawable's bounds
     */
    public enum Orientation {
        /** draw the gradient from the top to the bottom */
        TOP_BOTTOM,
        /** draw the gradient from the top-right to the bottom-left */
        TR_BL,
        /** draw the gradient from the right to the left */
        RIGHT_LEFT,
        /** draw the gradient from the bottom-right to the top-left */
        BR_TL,
        /** draw the gradient from the bottom to the top */
        BOTTOM_TOP,
        /** draw the gradient from the bottom-left to the top-right */
        BL_TR,
        /** draw the gradient from the left to the right */
        LEFT_RIGHT,
        /** draw the gradient from the top-left to the bottom-right */
        TL_BR,
    }

    public GradientDrawable() {
        this(new GradientState(Orientation.TOP_BOTTOM, null), null);
    }
    
    /**
     * Create a new gradient drawable given an orientation and an array
     * of colors for the gradient.
     */
    public GradientDrawable(Orientation orientation, int[] colors) {
        this(new GradientState(orientation, colors), null);
    }
    
    @Override
    public boolean getPadding(Rect padding) {
        if (mPadding != null) {
            padding.set(mPadding);
            return true;
        } else {
            return super.getPadding(padding);
        }
    }

    /**
     * <p>Specify radii for each of the 4 corners. For each corner, the array
     * contains 2 values, <code>[X_radius, Y_radius]</code>. The corners are ordered
     * top-left, top-right, bottom-right, bottom-left. This property
     * is honored only when the shape is of type {@link #RECTANGLE}.</p>
     * <p><strong>Note</strong>: changing this property will affect all instances
     * of a drawable loaded from a resource. It is recommended to invoke
     * {@link #mutate()} before changing this property.</p>
     *
     * @param radii 4 pairs of X and Y radius for each corner, specified in pixels.
     *              The length of this array must be >= 8
     *
     * @see #mutate()
     * @see #setCornerRadii(float[])
     * @see #setShape(int)
     */
    public void setCornerRadii(float[] radii) {
        mGradientState.setCornerRadii(radii);
        mPathIsDirty = true;
        invalidateSelf();
    }
    
    /**
     * <p>Specify radius for the corners of the gradient. If this is > 0, then the
     * drawable is drawn in a round-rectangle, rather than a rectangle. This property
     * is honored only when the shape is of type {@link #RECTANGLE}.</p>
     * <p><strong>Note</strong>: changing this property will affect all instances
     * of a drawable loaded from a resource. It is recommended to invoke
     * {@link #mutate()} before changing this property.</p>
     *
     * @param radius The radius in pixels of the corners of the rectangle shape
     *
     * @see #mutate()
     * @see #setCornerRadii(float[])
     * @see #setShape(int) 
     */
    public void setCornerRadius(float radius) {
        mGradientState.setCornerRadius(radius);
        mPathIsDirty = true;
        invalidateSelf();
    }

    /**
     * <p>Set the stroke width and color for the drawable. If width is zero,
     * then no stroke is drawn.</p>
     * <p><strong>Note</strong>: changing this property will affect all instances
     * of a drawable loaded from a resource. It is recommended to invoke
     * {@link #mutate()} before changing this property.</p>
     *
     * @param width The width in pixels of the stroke
     * @param color The color of the stroke
     *
     * @see #mutate()
     * @see #setStroke(int, int, float, float) 
     */
    public void setStroke(int width, int color) {
        setStroke(width, color, 0, 0);
    }

    /**
     * <p>Set the stroke width and color state list for the drawable. If width
     * is zero, then no stroke is drawn.</p>
     * <p><strong>Note</strong>: changing this property will affect all instances
     * of a drawable loaded from a resource. It is recommended to invoke
     * {@link #mutate()} before changing this property.</p>
     *
     * @param width The width in pixels of the stroke
     * @param colorStateList The color state list of the stroke
     *
     * @see #mutate()
     * @see #setStroke(int, ColorStateList, float, float)
     */
    public void setStroke(int width, ColorStateList colorStateList) {
        setStroke(width, colorStateList, 0, 0);
    }

    /**
     * <p>Set the stroke width and color for the drawable. If width is zero,
     * then no stroke is drawn. This method can also be used to dash the stroke.</p>
     * <p><strong>Note</strong>: changing this property will affect all instances
     * of a drawable loaded from a resource. It is recommended to invoke
     * {@link #mutate()} before changing this property.</p>
     *
     * @param width The width in pixels of the stroke
     * @param color The color of the stroke
     * @param dashWidth The length in pixels of the dashes, set to 0 to disable dashes 
     * @param dashGap The gap in pixels between dashes
     *
     * @see #mutate()
     * @see #setStroke(int, int) 
     */
    public void setStroke(int width, int color, float dashWidth, float dashGap) {
        mGradientState.setStroke(width, ColorStateList.valueOf(color), dashWidth, dashGap);
        setStrokeInternal(width, color, dashWidth, dashGap);
    }

    /**
     * <p>Set the stroke width and color state list for the drawable. If width
     * is zero, then no stroke is drawn. This method can also be used to dash
     * the stroke.</p>
     * <p><strong>Note</strong>: changing this property will affect all instances
     * of a drawable loaded from a resource. It is recommended to invoke
     * {@link #mutate()} before changing this property.</p>
     *
     * @param width The width in pixels of the stroke
     * @param colorStateList The color state list of the stroke
     * @param dashWidth The length in pixels of the dashes, set to 0 to disable dashes
     * @param dashGap The gap in pixels between dashes
     *
     * @see #mutate()
     * @see #setStroke(int, ColorStateList)
     */
    public void setStroke(
            int width, ColorStateList colorStateList, float dashWidth, float dashGap) {
        mGradientState.setStroke(width, colorStateList, dashWidth, dashGap);
        final int color;
        if (colorStateList == null) {
            color = Color.TRANSPARENT;
        } else {
            final int[] stateSet = getState();
            color = colorStateList.getColorForState(stateSet, 0);
        }
        setStrokeInternal(width, color, dashWidth, dashGap);
    }

    private void setStrokeInternal(int width, int color, float dashWidth, float dashGap) {
        if (mStrokePaint == null)  {
            mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mStrokePaint.setStyle(Paint.Style.STROKE);
        }
        mStrokePaint.setStrokeWidth(width);
        mStrokePaint.setColor(color);
        
        DashPathEffect e = null;
        if (dashWidth > 0) {
            e = new DashPathEffect(new float[] { dashWidth, dashGap }, 0);
        }
        mStrokePaint.setPathEffect(e);
        invalidateSelf();
    }


    /**
     * <p>Sets the size of the shape drawn by this drawable.</p>
     * <p><strong>Note</strong>: changing this property will affect all instances
     * of a drawable loaded from a resource. It is recommended to invoke
     * {@link #mutate()} before changing this property.</p>
     *
     * @param width The width of the shape used by this drawable
     * @param height The height of the shape used by this drawable
     *
     * @see #mutate()
     * @see #setGradientType(int)
     */
    public void setSize(int width, int height) {
        mGradientState.setSize(width, height);
        mPathIsDirty = true;
        invalidateSelf();
    }

    /**
     * <p>Sets the type of shape used to draw the gradient.</p>
     * <p><strong>Note</strong>: changing this property will affect all instances
     * of a drawable loaded from a resource. It is recommended to invoke
     * {@link #mutate()} before changing this property.</p>
     *
     * @param shape The desired shape for this drawable: {@link #LINE},
     *              {@link #OVAL}, {@link #RECTANGLE} or {@link #RING}
     *
     * @see #mutate()
     */
    public void setShape(int shape) {
        mRingPath = null;
        mPathIsDirty = true;
        mGradientState.setShape(shape);
        invalidateSelf();
    }

    /**
     * <p>Sets the type of gradient used by this drawable..</p>
     * <p><strong>Note</strong>: changing this property will affect all instances
     * of a drawable loaded from a resource. It is recommended to invoke
     * {@link #mutate()} before changing this property.</p>
     *
     * @param gradient The type of the gradient: {@link #LINEAR_GRADIENT},
     *                 {@link #RADIAL_GRADIENT} or {@link #SWEEP_GRADIENT}
     *
     * @see #mutate()
     */
    public void setGradientType(int gradient) {
        mGradientState.setGradientType(gradient);
        mRectIsDirty = true;
        invalidateSelf();
    }

    /**
     * <p>Sets the center location of the gradient. The radius is honored only when 
     * the gradient type is set to {@link #RADIAL_GRADIENT} or {@link #SWEEP_GRADIENT}.</p>
     * <p><strong>Note</strong>: changing this property will affect all instances
     * of a drawable loaded from a resource. It is recommended to invoke
     * {@link #mutate()} before changing this property.</p>
     *
     * @param x The x coordinate of the gradient's center
     * @param y The y coordinate of the gradient's center
     *
     * @see #mutate()
     * @see #setGradientType(int)
     */
    public void setGradientCenter(float x, float y) {
        mGradientState.setGradientCenter(x, y);
        mRectIsDirty = true;
        invalidateSelf();
    }

    /**
     * <p>Sets the radius of the gradient. The radius is honored only when the
     * gradient type is set to {@link #RADIAL_GRADIENT}.</p>
     * <p><strong>Note</strong>: changing this property will affect all instances
     * of a drawable loaded from a resource. It is recommended to invoke
     * {@link #mutate()} before changing this property.</p>
     *
     * @param gradientRadius The radius of the gradient in pixels
     *
     * @see #mutate()
     * @see #setGradientType(int) 
     */
    public void setGradientRadius(float gradientRadius) {
        mGradientState.setGradientRadius(gradientRadius, TypedValue.COMPLEX_UNIT_PX);
        mRectIsDirty = true;
        invalidateSelf();
    }

    /**
     * Returns the radius of the gradient in pixels. The radius is valid only
     * when the gradient type is set to {@link #RADIAL_GRADIENT}.
     *
     * @return Radius in pixels.
     */
    public float getGradientRadius() {
        if (mGradientState.mGradient != RADIAL_GRADIENT) {
            return 0;
        }

        ensureValidRect();
        return mGradientRadius;
    }

    /**
     * <p>Sets whether or not this drawable will honor its <code>level</code>
     * property.</p>
     * <p><strong>Note</strong>: changing this property will affect all instances
     * of a drawable loaded from a resource. It is recommended to invoke
     * {@link #mutate()} before changing this property.</p>
     *
     * @param useLevel True if this drawable should honor its level, false otherwise
     *
     * @see #mutate()
     * @see #setLevel(int) 
     * @see #getLevel() 
     */
    public void setUseLevel(boolean useLevel) {
        mGradientState.mUseLevel = useLevel;
        mRectIsDirty = true;
        invalidateSelf();
    }
    
    private int modulateAlpha(int alpha) {
        int scale = mAlpha + (mAlpha >> 7);
        return alpha * scale >> 8;
    }

    /**
     * Returns the orientation of the gradient defined in this drawable.
     */
    public Orientation getOrientation() {
        return mGradientState.mOrientation;
    }

    /**
     * <p>Changes the orientation of the gradient defined in this drawable.</p>
     * <p><strong>Note</strong>: changing orientation will affect all instances
     * of a drawable loaded from a resource. It is recommended to invoke
     * {@link #mutate()} before changing the orientation.</p>
     * 
     * @param orientation The desired orientation (angle) of the gradient
     *                    
     * @see #mutate() 
     */
    public void setOrientation(Orientation orientation) {
        mGradientState.mOrientation = orientation;
        mRectIsDirty = true;
        invalidateSelf();
    }

    /**
     * <p>Sets the colors used to draw the gradient. Each color is specified as an
     * ARGB integer and the array must contain at least 2 colors.</p>
     * <p><strong>Note</strong>: changing orientation will affect all instances
     * of a drawable loaded from a resource. It is recommended to invoke
     * {@link #mutate()} before changing the orientation.</p>
     *
     * @param colors 2 or more ARGB colors
     *
     * @see #mutate()
     * @see #setColor(int) 
     */
    public void setColors(int[] colors) {
        mGradientState.setColors(colors);
        mRectIsDirty = true;
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        if (!ensureValidRect()) {
            // nothing to draw
            return;
        }

        // remember the alpha values, in case we temporarily overwrite them
        // when we modulate them with mAlpha
        final int prevFillAlpha = mFillPaint.getAlpha();
        final int prevStrokeAlpha = mStrokePaint != null ? mStrokePaint.getAlpha() : 0;
        // compute the modulate alpha values
        final int currFillAlpha = modulateAlpha(prevFillAlpha);
        final int currStrokeAlpha = modulateAlpha(prevStrokeAlpha);

        final boolean haveStroke = currStrokeAlpha > 0 && mStrokePaint != null &&
                mStrokePaint.getStrokeWidth() > 0;
        final boolean haveFill = currFillAlpha > 0;
        final GradientState st = mGradientState;
        /*  we need a layer iff we're drawing both a fill and stroke, and the
            stroke is non-opaque, and our shapetype actually supports
            fill+stroke. Otherwise we can just draw the stroke (if any) on top
            of the fill (if any) without worrying about blending artifacts.
         */
         final boolean useLayer = haveStroke && haveFill && st.mShape != LINE &&
                 currStrokeAlpha < 255 && (mAlpha < 255 || mColorFilter != null);

        /*  Drawing with a layer is slower than direct drawing, but it
            allows us to apply paint effects like alpha and colorfilter to
            the result of multiple separate draws. In our case, if the user
            asks for a non-opaque alpha value (via setAlpha), and we're
            stroking, then we need to apply the alpha AFTER we've drawn
            both the fill and the stroke.
        */
        if (useLayer) {
            if (mLayerPaint == null) {
                mLayerPaint = new Paint();
            }
            mLayerPaint.setDither(mDither);
            mLayerPaint.setAlpha(mAlpha);
            mLayerPaint.setColorFilter(mColorFilter);

            float rad = mStrokePaint.getStrokeWidth();
            canvas.saveLayer(mRect.left - rad, mRect.top - rad,
                             mRect.right + rad, mRect.bottom + rad,
                             mLayerPaint, Canvas.HAS_ALPHA_LAYER_SAVE_FLAG);

            // don't perform the filter in our individual paints
            // since the layer will do it for us
            mFillPaint.setColorFilter(null);
            mStrokePaint.setColorFilter(null);
        } else {
            /*  if we're not using a layer, apply the dither/filter to our
                individual paints
            */
            mFillPaint.setAlpha(currFillAlpha);
            mFillPaint.setDither(mDither);
            mFillPaint.setColorFilter(mColorFilter);
            if (mColorFilter != null && mGradientState.mColorStateList == null) {
                mFillPaint.setColor(mAlpha << 24);
            }
            if (haveStroke) {
                mStrokePaint.setAlpha(currStrokeAlpha);
                mStrokePaint.setDither(mDither);
                mStrokePaint.setColorFilter(mColorFilter);
            }
        }

        switch (st.mShape) {
            case RECTANGLE:
                if (st.mRadiusArray != null) {
                    buildPathIfDirty();
                    canvas.drawPath(mPath, mFillPaint);
                    if (haveStroke) {
                        canvas.drawPath(mPath, mStrokePaint);
                    }
                } else if (st.mRadius > 0.0f) {
                    // since the caller is only giving us 1 value, we will force
                    // it to be square if the rect is too small in one dimension
                    // to show it. If we did nothing, Skia would clamp the rad
                    // independently along each axis, giving us a thin ellipse
                    // if the rect were very wide but not very tall
                    float rad = Math.min(st.mRadius,
                            Math.min(mRect.width(), mRect.height()) * 0.5f);
                    canvas.drawRoundRect(mRect, rad, rad, mFillPaint);
                    if (haveStroke) {
                        canvas.drawRoundRect(mRect, rad, rad, mStrokePaint);
                    }
                } else {
                    if (mFillPaint.getColor() != 0 || mColorFilter != null ||
                            mFillPaint.getShader() != null) {
                        canvas.drawRect(mRect, mFillPaint);
                    }
                    if (haveStroke) {
                        canvas.drawRect(mRect, mStrokePaint);
                    }
                }
                break;
            case OVAL:
                canvas.drawOval(mRect, mFillPaint);
                if (haveStroke) {
                    canvas.drawOval(mRect, mStrokePaint);
                }
                break;
            case LINE: {
                RectF r = mRect;
                float y = r.centerY();
                canvas.drawLine(r.left, y, r.right, y, mStrokePaint);
                break;
            }
            case RING:
                Path path = buildRing(st);
                canvas.drawPath(path, mFillPaint);
                if (haveStroke) {
                    canvas.drawPath(path, mStrokePaint);
                }
                break;
        }
        
        if (useLayer) {
            canvas.restore();
        } else {
            mFillPaint.setAlpha(prevFillAlpha);
            if (haveStroke) {
                mStrokePaint.setAlpha(prevStrokeAlpha);
            }
        }
    }

    private void buildPathIfDirty() {
        final GradientState st = mGradientState;
        if (mPathIsDirty || mRectIsDirty) {
            mPath.reset();
            mPath.addRoundRect(mRect, st.mRadiusArray, Path.Direction.CW);
            mPathIsDirty = mRectIsDirty = false;
        }
    }

    private Path buildRing(GradientState st) {
        if (mRingPath != null && (!st.mUseLevelForShape || !mPathIsDirty)) return mRingPath;
        mPathIsDirty = false;

        float sweep = st.mUseLevelForShape ? (360.0f * getLevel() / 10000.0f) : 360f;
        
        RectF bounds = new RectF(mRect);

        float x = bounds.width() / 2.0f;
        float y = bounds.height() / 2.0f;

        float thickness = st.mThickness != -1 ?
                st.mThickness : bounds.width() / st.mThicknessRatio;
        // inner radius
        float radius = st.mInnerRadius != -1 ?
                st.mInnerRadius : bounds.width() / st.mInnerRadiusRatio;

        RectF innerBounds = new RectF(bounds);
        innerBounds.inset(x - radius, y - radius);

        bounds = new RectF(innerBounds);
        bounds.inset(-thickness, -thickness);

        if (mRingPath == null) {
            mRingPath = new Path();
        } else {
            mRingPath.reset();
        }

        final Path ringPath = mRingPath;
        // arcTo treats the sweep angle mod 360, so check for that, since we
        // think 360 means draw the entire oval
        if (sweep < 360 && sweep > -360) {
            ringPath.setFillType(Path.FillType.EVEN_ODD);
            // inner top
            ringPath.moveTo(x + radius, y);
            // outer top
            ringPath.lineTo(x + radius + thickness, y);
            // outer arc
            ringPath.arcTo(bounds, 0.0f, sweep, false);
            // inner arc
            ringPath.arcTo(innerBounds, sweep, -sweep, false);
            ringPath.close();
        } else {
            // add the entire ovals
            ringPath.addOval(bounds, Path.Direction.CW);
            ringPath.addOval(innerBounds, Path.Direction.CCW);
        }

        return ringPath;
    }

    /**
     * <p>Changes this drawable to use a single color instead of a gradient.</p>
     * <p><strong>Note</strong>: changing color will affect all instances
     * of a drawable loaded from a resource. It is recommended to invoke
     * {@link #mutate()} before changing the color.</p>
     *
     * @param argb The color used to fill the shape
     *
     * @see #mutate()
     * @see #setColors(int[]) 
     */
    public void setColor(int argb) {
        mGradientState.setColorStateList(ColorStateList.valueOf(argb));
        mFillPaint.setColor(argb);
        invalidateSelf();
    }

    /**
     * Changes this drawable to use a single color state list instead of a
     * gradient. Calling this method with a null argument will clear the color
     * and is equivalent to calling {@link #setColor(int)} with the argument
     * {@link Color#TRANSPARENT}.
     * <p>
     * <strong>Note</strong>: changing color will affect all instances of a
     * drawable loaded from a resource. It is recommended to invoke
     * {@link #mutate()} before changing the color.</p>
     *
     * @param colorStateList The color state list used to fill the shape
     * @see #mutate()
     */
    public void setColor(ColorStateList colorStateList) {
        mGradientState.setColorStateList(colorStateList);
        final int color;
        if (colorStateList == null) {
            color = Color.TRANSPARENT;
        } else {
            final int[] stateSet = getState();
            color = colorStateList.getColorForState(stateSet, 0);
        }
        mFillPaint.setColor(color);
        invalidateSelf();
    }

    @Override
    protected boolean onStateChange(int[] stateSet) {
        boolean invalidateSelf = false;

        final GradientState s = mGradientState;
        final ColorStateList stateList = s.mColorStateList;
        if (stateList != null) {
            final int newColor = stateList.getColorForState(stateSet, 0);
            final int oldColor = mFillPaint.getColor();
            if (oldColor != newColor) {
                mFillPaint.setColor(newColor);
                invalidateSelf = true;
            }
        }

        final Paint strokePaint = mStrokePaint;
        if (strokePaint != null) {
            final ColorStateList strokeStateList = s.mStrokeColorStateList;
            if (strokeStateList != null) {
                final int newStrokeColor = strokeStateList.getColorForState(stateSet, 0);
                final int oldStrokeColor = strokePaint.getColor();
                if (oldStrokeColor != newStrokeColor) {
                    strokePaint.setColor(newStrokeColor);
                    invalidateSelf = true;
                }
            }
        }

        if (invalidateSelf) {
            invalidateSelf();
            return true;
        }

        return false;
    }

    @Override
    public boolean isStateful() {
        final GradientState s = mGradientState;
        return super.isStateful()
                || (s.mColorStateList != null && s.mColorStateList.isStateful())
                || (s.mStrokeColorStateList != null && s.mStrokeColorStateList.isStateful());
    }

    @Override
    public int getChangingConfigurations() {
        return super.getChangingConfigurations() | mGradientState.mChangingConfigurations;
    }

    @Override
    public void setAlpha(int alpha) {
        if (alpha != mAlpha) {
            mAlpha = alpha;
            invalidateSelf();
        }
    }

    @Override
    public int getAlpha() {
        return mAlpha;
    }

    @Override
    public void setDither(boolean dither) {
        if (dither != mDither) {
            mDither = dither;
            invalidateSelf();
        }
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        if (cf != mColorFilter) {
            mColorFilter = cf;
            invalidateSelf();
        }
    }

    @Override
    public int getOpacity() {
        return mGradientState.mOpaque ? PixelFormat.OPAQUE : PixelFormat.TRANSLUCENT;
    }

    @Override
    protected void onBoundsChange(Rect r) {
        super.onBoundsChange(r);
        mRingPath = null;
        mPathIsDirty = true;
        mRectIsDirty = true;
    }

    @Override
    protected boolean onLevelChange(int level) {
        super.onLevelChange(level);
        mRectIsDirty = true;
        mPathIsDirty = true;
        invalidateSelf();
        return true;
    }

    /**
     * This checks mRectIsDirty, and if it is true, recomputes both our drawing
     * rectangle (mRect) and the gradient itself, since it depends on our
     * rectangle too.
     * @return true if the resulting rectangle is not empty, false otherwise
     */
    private boolean ensureValidRect() {
        if (mRectIsDirty) {
            mRectIsDirty = false;

            Rect bounds = getBounds();
            float inset = 0;
            
            if (mStrokePaint != null) {
                inset = mStrokePaint.getStrokeWidth() * 0.5f;
            }

            final GradientState st = mGradientState;

            mRect.set(bounds.left + inset, bounds.top + inset,
                      bounds.right - inset, bounds.bottom - inset);

            final int[] colors = st.mColors;
            if (colors != null) {
                RectF r = mRect;
                float x0, x1, y0, y1;

                if (st.mGradient == LINEAR_GRADIENT) {
                    final float level = st.mUseLevel ? getLevel() / 10000.0f : 1.0f;                    
                    switch (st.mOrientation) {
                    case TOP_BOTTOM:
                        x0 = r.left;            y0 = r.top;
                        x1 = x0;                y1 = level * r.bottom;
                        break;
                    case TR_BL:
                        x0 = r.right;           y0 = r.top;
                        x1 = level * r.left;    y1 = level * r.bottom;
                        break;
                    case RIGHT_LEFT:
                        x0 = r.right;           y0 = r.top;
                        x1 = level * r.left;    y1 = y0;
                        break;
                    case BR_TL:
                        x0 = r.right;           y0 = r.bottom;
                        x1 = level * r.left;    y1 = level * r.top;
                        break;
                    case BOTTOM_TOP:
                        x0 = r.left;            y0 = r.bottom;
                        x1 = x0;                y1 = level * r.top;
                        break;
                    case BL_TR:
                        x0 = r.left;            y0 = r.bottom;
                        x1 = level * r.right;   y1 = level * r.top;
                        break;
                    case LEFT_RIGHT:
                        x0 = r.left;            y0 = r.top;
                        x1 = level * r.right;   y1 = y0;
                        break;
                    default:/* TL_BR */
                        x0 = r.left;            y0 = r.top;
                        x1 = level * r.right;   y1 = level * r.bottom;
                        break;
                    }

                    mFillPaint.setShader(new LinearGradient(x0, y0, x1, y1,
                            colors, st.mPositions, Shader.TileMode.CLAMP));
                } else if (st.mGradient == RADIAL_GRADIENT) {
                    x0 = r.left + (r.right - r.left) * st.mCenterX;
                    y0 = r.top + (r.bottom - r.top) * st.mCenterY;

                    float radius = st.mGradientRadius;
                    if (st.mGradientRadiusType == RADIUS_TYPE_FRACTION) {
                        radius *= Math.min(st.mWidth, st.mHeight);
                    } else if (st.mGradientRadiusType == RADIUS_TYPE_FRACTION_PARENT) {
                        radius *= Math.min(r.width(), r.height());
                    }

                    if (st.mUseLevel) {
                        radius *= getLevel() / 10000.0f;
                    }

                    mGradientRadius = radius;

                    if (radius == 0) {
                        // We can't have a shader with zero radius, so let's
                        // have a very, very small radius.
                        radius = 0.001f;
                    }

                    mFillPaint.setShader(new RadialGradient(
                            x0, y0, radius, colors, null, Shader.TileMode.CLAMP));
                } else if (st.mGradient == SWEEP_GRADIENT) {
                    x0 = r.left + (r.right - r.left) * st.mCenterX;
                    y0 = r.top + (r.bottom - r.top) * st.mCenterY;

                    int[] tempColors = colors;
                    float[] tempPositions = null;

                    if (st.mUseLevel) {
                        tempColors = st.mTempColors;
                        final int length = colors.length;
                        if (tempColors == null || tempColors.length != length + 1) {
                            tempColors = st.mTempColors = new int[length + 1];
                        }
                        System.arraycopy(colors, 0, tempColors, 0, length);
                        tempColors[length] = colors[length - 1];

                        tempPositions = st.mTempPositions;
                        final float fraction = 1.0f / (length - 1);
                        if (tempPositions == null || tempPositions.length != length + 1) {
                            tempPositions = st.mTempPositions = new float[length + 1];
                        }

                        final float level = getLevel() / 10000.0f;
                        for (int i = 0; i < length; i++) {
                            tempPositions[i] = i * fraction * level;
                        }
                        tempPositions[length] = 1.0f;

                    }
                    mFillPaint.setShader(new SweepGradient(x0, y0, tempColors, tempPositions));
                }

                // If we don't have a solid color, the alpha channel must be
                // maxed out so that alpha modulation works correctly.
                if (st.mColorStateList == null) {
                    mFillPaint.setColor(Color.BLACK);
                }
            }
        }
        return !mRect.isEmpty();
    }

    @Override
    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, Theme theme)
            throws XmlPullParserException, IOException {
        final TypedArray a = obtainAttributes(r, theme, attrs, R.styleable.GradientDrawable);
        super.inflateWithAttributes(r, parser, a, R.styleable.GradientDrawable_visible);

        inflateStateFromTypedArray(a);
        a.recycle();

        inflateChildElements(r, parser, attrs, theme);

        mGradientState.computeOpacity();
    }

    /**
     * Initializes the constant state from the values in the typed array.
     */
    private void inflateStateFromTypedArray(TypedArray a) {
        final GradientState state = mGradientState;

        // Extract the theme attributes, if any.
        final int[] themeAttrs = a.extractThemeAttrs();
        state.mThemeAttrs = themeAttrs;

        final boolean needsRingAttrs;
        if (themeAttrs == null || themeAttrs[R.styleable.GradientDrawable_shape] == 0) {
            final int shapeType = a.getInt(R.styleable.GradientDrawable_shape, RECTANGLE);
            setShape(shapeType);
            needsRingAttrs = shapeType == RING;
        } else {
            needsRingAttrs = true;
        }

        // We only need to load ring attributes if the shape type is a theme
        // attribute (e.g. unknown) or defined in XML as RING.
        if (needsRingAttrs) {
            if (themeAttrs == null || themeAttrs[R.styleable.GradientDrawable_innerRadius] == 0) {
                state.mInnerRadius = a.getDimensionPixelSize(
                        R.styleable.GradientDrawable_innerRadius, -1);
            }

            if (state.mInnerRadius == -1
                    && (themeAttrs == null || themeAttrs[R.styleable.GradientDrawable_thicknessRatio] == 0)) {
                state.mInnerRadiusRatio = a.getFloat(
                        R.styleable.GradientDrawable_innerRadiusRatio, DEFAULT_INNER_RADIUS_RATIO);
            }

            if (themeAttrs == null || themeAttrs[R.styleable.GradientDrawable_thickness] == 0) {
                state.mThickness = a.getDimensionPixelSize(
                        R.styleable.GradientDrawable_thickness, -1);
            }

            if (state.mThickness == -1
                    && (themeAttrs == null || themeAttrs[R.styleable.GradientDrawable_thicknessRatio] == 0)) {
                state.mThicknessRatio = a.getFloat(
                        R.styleable.GradientDrawable_thicknessRatio, DEFAULT_THICKNESS_RATIO);
            }

            if (themeAttrs == null || themeAttrs[R.styleable.GradientDrawable_useLevel] == 0) {
                state.mUseLevelForShape = a.getBoolean(
                        R.styleable.GradientDrawable_useLevel, true);
            }
        }

        if (themeAttrs == null || themeAttrs[R.styleable.GradientDrawable_dither] == 0) {
            final boolean dither = a.getBoolean(R.styleable.GradientDrawable_dither, false);
            setDither(dither);
        }
    }

    @Override
    public void applyTheme(Theme t) {
        super.applyTheme(t);

        final GradientState state = mGradientState;
        if (state == null) {
            throw new RuntimeException("Can't apply theme to <shape> with no constant state");
        }

        final int[] themeAttrs = state.mThemeAttrs;
        if (themeAttrs != null) {
            final TypedArray a = t.resolveAttributes(
                    themeAttrs, R.styleable.GradientDrawable, 0, 0);
            updateStateFromTypedArray(a);
            a.recycle();

            applyThemeChildElements(t);

            mGradientState.computeOpacity();
        }
    }

    /**
     * Updates the constant state from the values in the typed array.
     */
    private void updateStateFromTypedArray(TypedArray a) {
        final GradientState state = mGradientState;

        if (a.hasValue(R.styleable.GradientDrawable_shape)) {
            final int shapeType = a.getInt(R.styleable.GradientDrawable_shape, RECTANGLE);
            setShape(shapeType);
        }

        if (a.hasValue(R.styleable.GradientDrawable_dither)) {
            final boolean dither = a.getBoolean(R.styleable.GradientDrawable_dither, false);
            setDither(dither);
        }

        if (state.mShape == RING) {
            if (a.hasValue(R.styleable.GradientDrawable_innerRadius)) {
                state.mInnerRadius = a.getDimensionPixelSize(
                        R.styleable.GradientDrawable_innerRadius, -1);
            }

            if (state.mInnerRadius == -1 && a.hasValue(
                    R.styleable.GradientDrawable_innerRadiusRatio)) {
                state.mInnerRadiusRatio = a.getFloat(
                        R.styleable.GradientDrawable_innerRadiusRatio, DEFAULT_INNER_RADIUS_RATIO);
            }

            if (a.hasValue(R.styleable.GradientDrawable_thickness)) {
                state.mThickness = a.getDimensionPixelSize(
                        R.styleable.GradientDrawable_thickness, -1);
            }

            if (state.mThickness == -1 && a.hasValue(
                    R.styleable.GradientDrawable_thicknessRatio)) {
                state.mThicknessRatio = a.getFloat(
                        R.styleable.GradientDrawable_thicknessRatio, DEFAULT_THICKNESS_RATIO);
            }

            if (a.hasValue(R.styleable.GradientDrawable_useLevel)) {
                state.mUseLevelForShape = a.getBoolean(
                        R.styleable.GradientDrawable_useLevel, true);
            }
        }
    }

    @Override
    public boolean canApplyTheme() {
        final GradientState state = mGradientState;
        return state != null && (state.mThemeAttrs != null || state.mAttrSize != null
                || state.mAttrGradient != null || state.mAttrSolid != null
                || state.mAttrStroke != null || state.mAttrCorners != null
                || state.mAttrPadding != null);
    }

    private void applyThemeChildElements(Theme t) {
        final GradientState state = mGradientState;
        TypedArray a;

        if (state.mAttrSize != null) {
            a = t.resolveAttributes(state.mAttrSize, R.styleable.GradientDrawableSize, 0, 0);
            // TODO: updateGradientDrawableSize(a);
            a.recycle();
        }

        if (state.mAttrGradient != null) {
            a = t.resolveAttributes(state.mAttrGradient, R.styleable.GradientDrawableGradient, 0, 0);
            // TODO: updateGradientDrawableGradient(a);
            a.recycle();
        }

        if (state.mAttrSolid != null) {
            a = t.resolveAttributes(state.mAttrSolid, R.styleable.GradientDrawableSolid, 0, 0);
            // TODO: updateGradientDrawableSolid(a);
            a.recycle();
        }

        if (state.mAttrStroke != null) {
            a = t.resolveAttributes(state.mAttrStroke, R.styleable.GradientDrawableStroke, 0, 0);
            // TODO: updateGradientDrawableStroke(a);
            a.recycle();
        }

        if (state.mAttrCorners != null) {
            a = t.resolveAttributes(state.mAttrCorners, R.styleable.DrawableCorners, 0, 0);
            // TODO: updateDrawableCorners(a);
            a.recycle();
        }

        if (state.mAttrPadding != null) {
            a = t.resolveAttributes(state.mAttrPadding, R.styleable.GradientDrawablePadding, 0, 0);
            // TODO: updateGradientDrawablePadding(a);
            a.recycle();
        }
    }

    private void inflateChildElements(Resources r, XmlPullParser parser, AttributeSet attrs,
            Theme theme) throws XmlPullParserException, IOException {
        TypedArray a;
        int type;

        final int innerDepth = parser.getDepth() + 1;
        int depth;
        while ((type=parser.next()) != XmlPullParser.END_DOCUMENT
               && ((depth=parser.getDepth()) >= innerDepth
                       || type != XmlPullParser.END_TAG)) {
            if (type != XmlPullParser.START_TAG) {
                continue;
            }

            if (depth > innerDepth) {
                continue;
            }

            String name = parser.getName();
            
            if (name.equals("size")) {
                a = obtainAttributes(
                        r, theme, attrs, R.styleable.GradientDrawableSize);
                applyGradientDrawableSize(a);
                a.recycle();
            } else if (name.equals("gradient")) {
                a = obtainAttributes(
                        r, theme, attrs, R.styleable.GradientDrawableGradient);
                applyGradientDrawableGradient(r, a);
                a.recycle();
            } else if (name.equals("solid")) {
                a = obtainAttributes(
                        r, theme, attrs, R.styleable.GradientDrawableSolid);
                applyGradientDrawableSolid(a);
                a.recycle();
            } else if (name.equals("stroke")) {
                a = obtainAttributes(
                        r, theme, attrs, R.styleable.GradientDrawableStroke);
                applyGradientDrawableStroke(a);
                a.recycle();
            } else if (name.equals("corners")) {
                a = obtainAttributes(r
                        , theme, attrs, R.styleable.DrawableCorners);
                applyDrawableCorners(a);
                a.recycle();
            } else if (name.equals("padding")) {
                a = obtainAttributes(
                        r, theme, attrs, R.styleable.GradientDrawablePadding);
                applyGradientDrawablePadding(a);
                a.recycle();
            } else {
                Log.w("drawable", "Bad element under <shape>: " + name);
            }
        }
    }

    private void applyGradientDrawablePadding(TypedArray a) {
        mPadding = new Rect(
                a.getDimensionPixelOffset(
                        R.styleable.GradientDrawablePadding_left, 0),
                a.getDimensionPixelOffset(
                        R.styleable.GradientDrawablePadding_top, 0),
                a.getDimensionPixelOffset(
                        R.styleable.GradientDrawablePadding_right, 0),
                a.getDimensionPixelOffset(
                        R.styleable.GradientDrawablePadding_bottom, 0));
        mGradientState.mPadding = mPadding;

        // Extract the theme attributes, if any.
        mGradientState.mAttrPadding = a.extractThemeAttrs();
    }

    private void applyDrawableCorners(TypedArray a) {
        int radius = a.getDimensionPixelSize(
                R.styleable.DrawableCorners_radius, 0);
        setCornerRadius(radius);
        int topLeftRadius = a.getDimensionPixelSize(
                R.styleable.DrawableCorners_topLeftRadius, radius);
        int topRightRadius = a.getDimensionPixelSize(
                R.styleable.DrawableCorners_topRightRadius, radius);
        int bottomLeftRadius = a.getDimensionPixelSize(
                R.styleable.DrawableCorners_bottomLeftRadius, radius);
        int bottomRightRadius = a.getDimensionPixelSize(
                R.styleable.DrawableCorners_bottomRightRadius, radius);
        if (topLeftRadius != radius || topRightRadius != radius ||
                bottomLeftRadius != radius || bottomRightRadius != radius) {
            // The corner radii are specified in clockwise order (see Path.addRoundRect())
            setCornerRadii(new float[] {
                    topLeftRadius, topLeftRadius,
                    topRightRadius, topRightRadius,
                    bottomRightRadius, bottomRightRadius,
                    bottomLeftRadius, bottomLeftRadius
            });
        }

        // Extract the theme attributes, if any.
        mGradientState.mAttrCorners = a.extractThemeAttrs();
    }

    private void applyGradientDrawableStroke(TypedArray a) {
        final int width = a.getDimensionPixelSize(
                R.styleable.GradientDrawableStroke_width, 0);
        final ColorStateList colorStateList = a.getColorStateList(
                R.styleable.GradientDrawableStroke_color);
        final float dashWidth = a.getDimension(
                R.styleable.GradientDrawableStroke_dashWidth, 0);
        if (dashWidth != 0.0f) {
            final float dashGap = a.getDimension(
                    R.styleable.GradientDrawableStroke_dashGap, 0);
            setStroke(width, colorStateList, dashWidth, dashGap);
        } else {
            setStroke(width, colorStateList);
        }

        // Extract the theme attributes, if any.
        mGradientState.mAttrStroke = a.extractThemeAttrs();
    }

    private void applyGradientDrawableSolid(TypedArray a) {
        final ColorStateList colorStateList = a.getColorStateList(
                R.styleable.GradientDrawableSolid_color);
        setColor(colorStateList);

        // Extract the theme attributes, if any.
        mGradientState.mAttrSolid = a.extractThemeAttrs();
    }

    private void applyGradientDrawableGradient(Resources r, TypedArray a)
            throws XmlPullParserException {
        final GradientState st = mGradientState;
        final int startColor = a.getColor(
                R.styleable.GradientDrawableGradient_startColor, 0);
        final boolean hasCenterColor = a.hasValue(
                R.styleable.GradientDrawableGradient_centerColor);
        final int centerColor = a.getColor(
                R.styleable.GradientDrawableGradient_centerColor, 0);
        final int endColor = a.getColor(
                R.styleable.GradientDrawableGradient_endColor, 0);

        if (hasCenterColor) {
            st.mColors = new int[3];
            st.mColors[0] = startColor;
            st.mColors[1] = centerColor;
            st.mColors[2] = endColor;
            
            st.mPositions = new float[3];
            st.mPositions[0] = 0.0f;
            // Since 0.5f is default value, try to take the one that isn't 0.5f
            st.mPositions[1] = st.mCenterX != 0.5f ? st.mCenterX : st.mCenterY;
            st.mPositions[2] = 1f;
        } else {
            st.mColors = new int[2];
            st.mColors[0] = startColor;
            st.mColors[1] = endColor;
        }

        st.mCenterX = getFloatOrFraction(
                a, R.styleable.GradientDrawableGradient_centerX, 0.5f);
        st.mCenterY = getFloatOrFraction(
                a, R.styleable.GradientDrawableGradient_centerY, 0.5f);
        st.mUseLevel = a.getBoolean(
                R.styleable.GradientDrawableGradient_useLevel, false);
        st.mGradient = a.getInt(
                R.styleable.GradientDrawableGradient_type, LINEAR_GRADIENT);

        if (st.mGradient == LINEAR_GRADIENT) {
            int angle = (int) a.getFloat(
                    R.styleable.GradientDrawableGradient_angle, 0);
            angle %= 360;

            if (angle % 45 != 0) {
                throw new XmlPullParserException(a.getPositionDescription()
                        + "<gradient> tag requires 'angle' attribute to "
                        + "be a multiple of 45");
            }

            switch (angle) {
                case 0:
                    st.mOrientation = Orientation.LEFT_RIGHT;
                    break;
                case 45:
                    st.mOrientation = Orientation.BL_TR;
                    break;
                case 90:
                    st.mOrientation = Orientation.BOTTOM_TOP;
                    break;
                case 135:
                    st.mOrientation = Orientation.BR_TL;
                    break;
                case 180:
                    st.mOrientation = Orientation.RIGHT_LEFT;
                    break;
                case 225:
                    st.mOrientation = Orientation.TR_BL;
                    break;
                case 270:
                    st.mOrientation = Orientation.TOP_BOTTOM;
                    break;
                case 315:
                    st.mOrientation = Orientation.TL_BR;
                    break;
            }
        } else {
            final TypedValue tv = a.peekValue(
                    R.styleable.GradientDrawableGradient_gradientRadius);
            if (tv != null) {
                final float radius;
                final int radiusType;
                if (tv.type == TypedValue.TYPE_FRACTION) {
                    radius = tv.getFraction(1.0f, 1.0f);

                    final int unit = (tv.data >> TypedValue.COMPLEX_UNIT_SHIFT)
                            & TypedValue.COMPLEX_UNIT_MASK;
                    if (unit == TypedValue.COMPLEX_UNIT_FRACTION_PARENT) {
                        radiusType = RADIUS_TYPE_FRACTION_PARENT;
                    } else {
                        radiusType = RADIUS_TYPE_FRACTION;
                    }
                } else {
                    radius = tv.getDimension(r.getDisplayMetrics());
                    radiusType = RADIUS_TYPE_PIXELS;
                }

                st.mGradientRadius = radius;
                st.mGradientRadiusType = radiusType;
            } else if (st.mGradient == RADIAL_GRADIENT) {
                throw new XmlPullParserException(
                        a.getPositionDescription()
                        + "<gradient> tag requires 'gradientRadius' "
                        + "attribute with radial type");
            }
        }

        // Extract the theme attributes, if any.
        mGradientState.mAttrGradient = a.extractThemeAttrs();
    }

    private void applyGradientDrawableSize(TypedArray a) {
        int width = a.getDimensionPixelSize(R.styleable.GradientDrawableSize_width, -1);
        int height = a.getDimensionPixelSize(R.styleable.GradientDrawableSize_height, -1);
        setSize(width, height);

        // Extract the theme attributes, if any.
        mGradientState.mAttrSize = a.extractThemeAttrs();
    }

    private static float getFloatOrFraction(TypedArray a, int index, float defaultValue) {
        TypedValue tv = a.peekValue(index);
        float v = defaultValue;
        if (tv != null) {
            boolean vIsFraction = tv.type == TypedValue.TYPE_FRACTION;
            v = vIsFraction ? tv.getFraction(1.0f, 1.0f) : tv.getFloat();
        }
        return v;
    }
    
    @Override
    public int getIntrinsicWidth() {
        return mGradientState.mWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return mGradientState.mHeight;
    }
    
    @Override
    public ConstantState getConstantState() {
        mGradientState.mChangingConfigurations = getChangingConfigurations();
        return mGradientState;
    }

    @Override
    public boolean getOutline(Outline outline) {
        final GradientState st = mGradientState;
        final Rect bounds = getBounds();

        switch (st.mShape) {
            case RECTANGLE:
                if (st.mRadiusArray != null) {
                    buildPathIfDirty();
                    outline.setConvexPath(mPath);
                    return true;
                }

                float rad = 0;
                if (st.mRadius > 0.0f) {
                    // clamp the radius based on width & height, matching behavior in draw()
                    rad = Math.min(st.mRadius,
                            Math.min(bounds.width(), bounds.height()) * 0.5f);
                }
                outline.setRoundRect(bounds.left, bounds.top,
                        bounds.right, bounds.bottom, rad);
                return true;
            case LINE: {
                float halfStrokeWidth = mStrokePaint.getStrokeWidth() * 0.5f;
                float centerY = bounds.centerY();
                int top = (int) Math.floor(centerY - halfStrokeWidth);
                int bottom = (int) Math.ceil(centerY + halfStrokeWidth);

                outline.setRect(bounds.left, top, bounds.right, bottom);
                return true;
            }
            default:
                // TODO: investigate
                return false;
        }
    }

    @Override
    public Drawable mutate() {
        if (!mMutated && super.mutate() == this) {
            mGradientState = new GradientState(mGradientState);
            initializeWithState(mGradientState);
            mMutated = true;
        }
        return this;
    }

    final static class GradientState extends ConstantState {
        public int mChangingConfigurations;
        public int mShape = RECTANGLE;
        public int mGradient = LINEAR_GRADIENT;
        public Orientation mOrientation;
        public ColorStateList mColorStateList;
        public ColorStateList mStrokeColorStateList;
        public int[] mColors;
        public int[] mTempColors; // no need to copy
        public float[] mTempPositions; // no need to copy
        public float[] mPositions;
        public int mStrokeWidth = -1;   // if >= 0 use stroking.
        public float mStrokeDashWidth;
        public float mStrokeDashGap;
        public float mRadius;    // use this if mRadiusArray is null
        public float[] mRadiusArray;
        public Rect mPadding;
        public int mWidth = -1;
        public int mHeight = -1;
        public float mInnerRadiusRatio;
        public float mThicknessRatio;
        public int mInnerRadius;
        public int mThickness;
        private float mCenterX = 0.5f;
        private float mCenterY = 0.5f;
        private float mGradientRadius = 0.5f;
        private int mGradientRadiusType = RADIUS_TYPE_PIXELS;
        private boolean mUseLevel;
        private boolean mUseLevelForShape;
        private boolean mOpaque;

        int[] mThemeAttrs;
        int[] mAttrSize;
        int[] mAttrGradient;
        int[] mAttrSolid;
        int[] mAttrStroke;
        int[] mAttrCorners;
        int[] mAttrPadding;

        GradientState(Orientation orientation, int[] colors) {
            mOrientation = orientation;
            setColors(colors);
        }

        public GradientState(GradientState state) {
            mChangingConfigurations = state.mChangingConfigurations;
            mShape = state.mShape;
            mGradient = state.mGradient;
            mOrientation = state.mOrientation;
            mColorStateList = state.mColorStateList;
            if (state.mColors != null) {
                mColors = state.mColors.clone();
            }
            if (state.mPositions != null) {
                mPositions = state.mPositions.clone();
            }
            mStrokeColorStateList = state.mStrokeColorStateList;
            mStrokeWidth = state.mStrokeWidth;
            mStrokeDashWidth = state.mStrokeDashWidth;
            mStrokeDashGap = state.mStrokeDashGap;
            mRadius = state.mRadius;
            if (state.mRadiusArray != null) {
                mRadiusArray = state.mRadiusArray.clone();
            }
            if (state.mPadding != null) {
                mPadding = new Rect(state.mPadding);
            }
            mWidth = state.mWidth;
            mHeight = state.mHeight;
            mInnerRadiusRatio = state.mInnerRadiusRatio;
            mThicknessRatio = state.mThicknessRatio;
            mInnerRadius = state.mInnerRadius;
            mThickness = state.mThickness;
            mCenterX = state.mCenterX;
            mCenterY = state.mCenterY;
            mGradientRadius = state.mGradientRadius;
            mGradientRadiusType = state.mGradientRadiusType;
            mUseLevel = state.mUseLevel;
            mUseLevelForShape = state.mUseLevelForShape;
            mOpaque = state.mOpaque;
            mThemeAttrs = state.mThemeAttrs;
            mAttrSize = state.mAttrSize;
            mAttrGradient = state.mAttrGradient;
            mAttrSolid = state.mAttrSolid;
            mAttrStroke = state.mAttrStroke;
            mAttrCorners = state.mAttrCorners;
            mAttrPadding = state.mAttrPadding;
        }

        @Override
        public boolean canApplyTheme() {
            return mThemeAttrs != null;
        }

        @Override
        public Drawable newDrawable() {
            return new GradientDrawable(this, null);
        }
        
        @Override
        public Drawable newDrawable(Resources res) {
            return new GradientDrawable(this, null);
        }
        
        @Override
        public Drawable newDrawable(Resources res, Theme theme) {
            return new GradientDrawable(this, theme);
        }
        
        @Override
        public int getChangingConfigurations() {
            return mChangingConfigurations;
        }

        public void setShape(int shape) {
            mShape = shape;
            computeOpacity();
        }

        public void setGradientType(int gradient) {
            mGradient = gradient;
        }

        public void setGradientCenter(float x, float y) {
            mCenterX = x;
            mCenterY = y;
        }

        public void setColors(int[] colors) {
            mColors = colors;
            mColorStateList = null;
            computeOpacity();
        }

        public void setColorStateList(ColorStateList colorStateList) {
            mColors = null;
            mColorStateList = colorStateList;
            computeOpacity();
        }

        private void computeOpacity() {
            if (mShape != RECTANGLE) {
                mOpaque = false;
                return;
            }

            if (mRadius > 0 || mRadiusArray != null) {
                mOpaque = false;
                return;
            }

            if (mStrokeWidth > 0) {
                if (mStrokeColorStateList != null) {
                    if (!mStrokeColorStateList.isOpaque()) {
                        mOpaque = false;
                        return;
                    }
                }
            }

            if (mColorStateList != null && !mColorStateList.isOpaque()) {
                mOpaque = false;
                return;
            }

            if (mColors != null) {
                for (int i = 0; i < mColors.length; i++) {
                    if (!isOpaque(mColors[i])) {
                        mOpaque = false;
                        return;
                    }
                }
            }

            mOpaque = true;
        }

        private static boolean isOpaque(int color) {
            return ((color >> 24) & 0xff) == 0xff;
        }

        public void setStroke(
                int width, ColorStateList colorStateList, float dashWidth, float dashGap) {
            mStrokeWidth = width;
            mStrokeColorStateList = colorStateList;
            mStrokeDashWidth = dashWidth;
            mStrokeDashGap = dashGap;
            computeOpacity();
        }

        public void setCornerRadius(float radius) {
            if (radius < 0) {
                radius = 0;
            }
            mRadius = radius;
            mRadiusArray = null;
        }

        public void setCornerRadii(float[] radii) {
            mRadiusArray = radii;
            if (radii == null) {
                mRadius = 0;
            }
        }

        public void setSize(int width, int height) {
            mWidth = width;
            mHeight = height;
        }

        public void setGradientRadius(float gradientRadius, int type) {
            mGradientRadius = gradientRadius;
            mGradientRadiusType = type;
        }
    }

    /**
     * Creates a new themed GradientDrawable based on the specified constant state.
     * <p>
     * The resulting drawable is guaranteed to have a new constant state.
     *
     * @param state Constant state from which the drawable inherits
     * @param theme Theme to apply to the drawable
     */
    private GradientDrawable(GradientState state, Theme theme) {
        mGradientState = new GradientState(state);
        if (theme != null && state.canApplyTheme()) {
            applyTheme(theme);
        }

        initializeWithState(state);
        mRectIsDirty = true;
        mMutated = false;
    }

    private void initializeWithState(GradientState state) {
        if (state.mColorStateList != null) {
            final int[] currentState = getState();
            final int stateColor = state.mColorStateList.getColorForState(currentState, 0);
            mFillPaint.setColor(stateColor);
        } else if (state.mColors == null) {
            // If we don't have a solid color and we don't have a gradient,
            // the app is stroking the shape, set the color to the default
            // value of state.mSolidColor
            mFillPaint.setColor(0);
        } else {
            // Otherwise, make sure the fill alpha is maxed out.
            mFillPaint.setColor(Color.BLACK);
        }
        mPadding = state.mPadding;
        if (state.mStrokeWidth >= 0) {
            mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mStrokePaint.setStyle(Paint.Style.STROKE);
            mStrokePaint.setStrokeWidth(state.mStrokeWidth);
            if (state.mStrokeColorStateList != null) {
                final int[] currentState = getState();
                final int strokeStateColor = state.mStrokeColorStateList.getColorForState(
                        currentState, 0);
                mStrokePaint.setColor(strokeStateColor);
            }

            if (state.mStrokeDashWidth != 0.0f) {
                DashPathEffect e = new DashPathEffect(
                        new float[] { state.mStrokeDashWidth, state.mStrokeDashGap }, 0);
                mStrokePaint.setPathEffect(e);
            }
        }
    }
}
