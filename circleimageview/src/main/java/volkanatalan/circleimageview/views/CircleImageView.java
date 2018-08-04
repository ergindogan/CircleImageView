package volkanatalan.circleimageview.views;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.animation.CycleInterpolator;

import volkanatalan.circleimageview.R;
import volkanatalan.library.Calc;

public class CircleImageView extends AppCompatImageView {
  // "f" means the variable is a field.
  // "m" means the variable is a field, it has getter-setter methods and an attribute.
  private Context fContext;
  private TypedArray fTypedArray;
  private Bitmap fBitmapImage, fBitmapCircleImageAndBorder, fBitmapCircleMask, fBitmapAnimated;
  private Canvas fCanvasAnimated;
  private Paint fPaint, fLightPaint;
  private Path fPathReflection = new Path();
  private Handler fHandler;
  private Runnable fRunnable;
  private ValueAnimator fShadowXAnimator, fShadowReverseAnimation, fShadowAlphaAnimator;
  private AnimatorSet fAnimatorSet;
  private PorterDuffXfermode DST_OUT = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);
  private PorterDuffXfermode CLR = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
  
  private int fViewWidth, fViewHeight, fDiameter;
  
  private int mShadowXDiff, mShadowYDiff, mShadowSize = 0;
  private int mShadowColor = Color.BLACK, mShadowAlpha = 50;
  private int fShadowCY, fShadowRadius, fShadowAnimatedCX, fShadowAlphaAdd;
  private boolean mShowShadow = true;
  
  private int mBorderColor = Color.BLACK;
  private int fBorderCX, fBorderCY, fBorderRadius, mBorderSize;
  
  private int mReflectionColor = Color.WHITE, mReflectionAlpha = 220;
  private int fReflectionPos, fLightAlpha = 0;
  private boolean mShowReflection = true;
  
  private int mLightPassDuration = 1000, mAnimationRepeatDelay = 10000;
  private int mMinShadowAlpha = 0, mMaxShadowAlpha = 50;
  private int mShadowReverseAnimationDelay = 0, mShadowReverseAnimationDuration = mLightPassDuration;
  private int mMinLightAlpha = 0, mMaxLightAlpha = 100;
  
  private LightDirection mLightDirection = LightDirection.LEFT;
  
  public enum LightDirection {
    LEFT(0), RIGHT(1);
    int id;
  
    LightDirection(int id) { this.id = id;}
  
    static LightDirection fromId(int id) {
      for (LightDirection l : LightDirection.values()) {
        if (l.id == id) return l;
      }
      throw new IllegalArgumentException();
    }
  }
  
  public CircleImageView(Context context) {
    super(context);
    fContext = context;
    start();
  }
  
  public CircleImageView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    fContext = context;
    fTypedArray = context.obtainStyledAttributes(attrs, R.styleable.CircleImageView);
    start();
  }
  
  public CircleImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    fContext = context;
    fTypedArray = context.obtainStyledAttributes(attrs, R.styleable.CircleImageView, defStyleAttr, 0);
    start();
  }
  
  private void start() {
    mBorderSize = Calc.dpToPx(fContext, 5);
    mShadowXDiff = Calc.dpToPx(fContext, 5);
    mShadowYDiff = Calc.dpToPx(fContext, 5);
    fContext = null;
    
    getAttrs();
  
    final int animationRepeatDelay = mAnimationRepeatDelay + mLightPassDuration;
  
    fPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    fPaint.setDither(true);
    fPaint.setStyle(Paint.Style.FILL);
  
    if (mShowReflection) {
      fLightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
      fLightPaint.setDither(true);
      fLightPaint.setStyle(Paint.Style.FILL);
      fLightPaint.setColor(mReflectionColor);
      fLightPaint.setAlpha(0);
  
      fHandler = new Handler();
  
      fRunnable = new Runnable() {
        @Override
        public void run() {
          fAnimatorSet.start();
      
          // Repeat
          fHandler.postDelayed(fRunnable, animationRepeatDelay);
        }
      };
    }
    
    setDrawingCacheEnabled(false);
  }
  
  
  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    loadBitmap();
    measureDimensions(widthMeasureSpec, heightMeasureSpec);
    setMeasuredDimension(fViewWidth, fViewHeight);
  }
  
  
  private void measureDimensions(int widthMeasureSpec, int heightMeasureSpec) {
    int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    int widthSize = MeasureSpec.getSize(widthMeasureSpec);
    int heightMode = MeasureSpec.getMode(heightMeasureSpec);
    int heightSize = MeasureSpec.getSize(heightMeasureSpec);
    float smallDimension = Math.min(widthSize, heightSize);
    
  
    // If both width and height are exact values.
    if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
      fViewWidth = widthSize;
      fViewHeight = heightSize;
      
      
      // If width is an exact value and height is wrap_content.
    } else if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.AT_MOST) {
      fViewWidth = widthSize;
      fViewHeight = widthSize > heightSize ? heightSize : widthSize;
      
  
      // If width is wrap_content and height is an exact value.
    } else if (widthMode == MeasureSpec.AT_MOST && heightMode == MeasureSpec.EXACTLY) {
      fViewWidth = widthSize > heightSize ? heightSize : widthSize;
      fViewHeight = heightSize;
      
      
      // If both width and height are wrap_content.
    } else if (widthMode == MeasureSpec.AT_MOST && heightMode == MeasureSpec.AT_MOST) {
      fViewWidth = (int) smallDimension + mBorderSize + getPaddingLeft() + getPaddingRight();
      fViewHeight = (int) smallDimension + mBorderSize + getPaddingTop() + getPaddingBottom();
      fViewWidth = fViewWidth > widthSize ? widthSize : fViewWidth;
      fViewHeight = fViewWidth > heightSize ? heightSize : fViewHeight;
    }
  }
  
  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    int paddingLeft = getPaddingLeft();
    int paddingTop = getPaddingTop();
    int paddingRight = getPaddingRight();
    int paddingBottom = getPaddingBottom();
    
    fDiameter = Math.min(w, h);
    int centerX = w / 2;
    int centerY = h / 2;
  
    fBorderCX = centerX + paddingLeft - paddingRight;
    fBorderCY = centerY + paddingTop - paddingBottom;
    int borderDiameter = fDiameter - Math.max(paddingLeft + paddingRight, paddingTop + paddingBottom);
    fBorderRadius = borderDiameter / 2;
  
    float imageDiameter = borderDiameter - mBorderSize * 2;
    int imageRadius = (int) (imageDiameter / 2);
    float imageWidth = fBitmapImage.getWidth();
    float imageHeight = fBitmapImage.getHeight();
    float smallDimension = Math.min(imageWidth, imageHeight);
    float ratio = imageDiameter / smallDimension;
    int fScaledImageWidth = (int) (imageWidth * ratio);
    int fScaledImageHeight = (int) (imageHeight * ratio);
    int bitmapLeft = (fViewWidth - fScaledImageWidth) / 2;
    int bitmapTop = (fViewHeight - fScaledImageHeight) / 2;
  
    if (fBitmapImage != null) {
      fBitmapImage = Bitmap.createScaledBitmap(
          fBitmapImage, fScaledImageWidth, fScaledImageHeight, false);
    }
    
    
  
    Bitmap bitmap = Bitmap.createBitmap(fViewWidth, fViewHeight, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
  
    fPaint.setXfermode(null);
    
    // Draw image
    canvas.drawBitmap(fBitmapImage, bitmapLeft, bitmapTop, fPaint);
  
    // Draw circle image mask
    fPaint.setXfermode(DST_OUT);
    canvas.drawBitmap(
        generateCircleMaskBitmap(fViewWidth, fViewHeight, fBorderCX, fBorderCY, imageRadius),
        0, 0, fPaint);
    
    
    
  
    fBitmapCircleImageAndBorder = Bitmap.createBitmap(fViewWidth, fViewHeight, Bitmap.Config.ARGB_8888);
    Canvas imageCanvas = new Canvas(fBitmapCircleImageAndBorder);
    fPaint.setXfermode(null);
  
    // Draw border
    if (mBorderSize > 0) {
      fPaint.setColor(mBorderColor);
      imageCanvas.drawCircle(fBorderCX, fBorderCY, fBorderRadius, fPaint);
    }
    
    // Draw circle image
    imageCanvas.drawBitmap(bitmap, 0, 0, fPaint);
  
    
    
    int shadowCX = fBorderCX + mShadowXDiff;
    fShadowCY = fBorderCY + mShadowYDiff;
    fShadowRadius = fBorderRadius + mShadowSize;
    int shadowAnimationStart;
    int shadowAnimationEnd;
  
    int reflectionWidth = fDiameter;
    int reflectionHeight = fDiameter;
    int reflectionPosStart;
    int reflectionPosEnd;
    int lightAlphaAnimationRepeatDelayDuration;
    
    if (mLightDirection == LightDirection.LEFT) {
      reflectionPosStart = borderDiameter;
      reflectionPosEnd = - reflectionWidth;
  
      shadowAnimationStart = centerX - (shadowCX - centerX);
      shadowAnimationEnd = shadowCX;
      
      lightAlphaAnimationRepeatDelayDuration = mLightPassDuration - (mLightPassDuration * 40 / 100);
    } else {
      reflectionPosStart = 0 - reflectionWidth;
      reflectionPosEnd = borderDiameter;
  
      shadowAnimationStart = shadowCX;
      shadowAnimationEnd = centerX - (shadowCX - centerX);
  
      lightAlphaAnimationRepeatDelayDuration = mLightPassDuration;
    }
  
    fShadowAnimatedCX = shadowAnimationStart;
    fReflectionPos = reflectionPosStart;
    int fCircleMaskRadius = fBorderRadius;
  
    ValueAnimator reflectionXAnimator;
    ValueAnimator lightAlphaAnimator;
    
    if (mShowReflection) {
      fBitmapAnimated = Bitmap.createBitmap(fDiameter, fDiameter, Bitmap.Config.ARGB_8888);
      fCanvasAnimated = new Canvas(fBitmapAnimated);
    
      fBitmapCircleMask = generateCircleMaskBitmap(
          reflectionWidth, reflectionHeight, fBorderCX, fBorderCY, fCircleMaskRadius);
  
      FastOutSlowInInterpolator fastOutSlowInInterpolator = new FastOutSlowInInterpolator();
      CycleInterpolator cycleInterpolator = new CycleInterpolator(0.5f);
  
      reflectionXAnimator = ValueAnimator.ofInt(reflectionPosStart, reflectionPosEnd);
      reflectionXAnimator.setDuration(mLightPassDuration);
      reflectionXAnimator.setInterpolator(fastOutSlowInInterpolator);
      reflectionXAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
          fReflectionPos = (int)valueAnimator.getAnimatedValue();
          invalidate();
        }
      });
  
      lightAlphaAnimator = ValueAnimator.ofInt(mMinLightAlpha, mMaxLightAlpha);
      lightAlphaAnimator.setDuration(lightAlphaAnimationRepeatDelayDuration);
      lightAlphaAnimator.setInterpolator(cycleInterpolator);
      lightAlphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
          fLightAlpha = (int) valueAnimator.getAnimatedValue();
        }
      });
  
      if (mShowShadow) {
        fShadowAlphaAnimator = ValueAnimator.ofInt(mMinShadowAlpha, mMaxShadowAlpha);
        fShadowAlphaAnimator.setDuration(lightAlphaAnimationRepeatDelayDuration);
        fShadowAlphaAnimator.setInterpolator(cycleInterpolator);
        fShadowAlphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
          @Override
          public void onAnimationUpdate(ValueAnimator valueAnimator) {
            fShadowAlphaAdd = (int) valueAnimator.getAnimatedValue();
          }
        });
        
        fShadowXAnimator = ValueAnimator.ofInt(shadowAnimationStart, shadowAnimationEnd);
        fShadowXAnimator.setDuration(mLightPassDuration);
        fShadowXAnimator.setInterpolator(fastOutSlowInInterpolator);
        fShadowXAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
          @Override
          public void onAnimationUpdate(ValueAnimator valueAnimator) {
            fShadowAnimatedCX = (int) valueAnimator.getAnimatedValue();
          }
        });
  
        fShadowReverseAnimation = ValueAnimator.ofInt(shadowAnimationEnd, shadowAnimationStart);
        fShadowReverseAnimation.setDuration(mShadowReverseAnimationDuration);
        fShadowReverseAnimation.setStartDelay(mLightPassDuration + mShadowReverseAnimationDelay);
        fShadowReverseAnimation.setInterpolator(fastOutSlowInInterpolator);
        fShadowReverseAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
          @Override
          public void onAnimationUpdate(ValueAnimator valueAnimator) {
            fShadowAnimatedCX = (int) valueAnimator.getAnimatedValue();
            invalidate();
          }
        });
      }
      
      fAnimatorSet = new AnimatorSet();
      fAnimatorSet.play(reflectionXAnimator);
      fAnimatorSet.play(lightAlphaAnimator);
      if (mShowShadow) {
        fAnimatorSet.play(fShadowXAnimator);
        fAnimatorSet.play(fShadowAlphaAnimator);
        fAnimatorSet.play(fShadowReverseAnimation);
      }
      
      // Start the Runnable
      if (mShowReflection)
        fHandler.post(fRunnable);
      
    }
  }
  
  @Override
  protected void onDraw(Canvas canvas) {
    
    if (fBitmapImage != null) {
      if (mShowReflection) getAnimatedBitmap();
      
      fPaint.setXfermode(null);
  
      // Draw shadow
      if (mShowShadow) {
        fPaint.setColor(mShadowColor);
        fPaint.setAlpha(mShadowAlpha + fShadowAlphaAdd);
        canvas.drawCircle(fShadowAnimatedCX, fShadowCY, fShadowRadius, fPaint);
        fPaint.setAlpha(255);
        fPaint.setMaskFilter(null);
      }
      
      // Draw border.
      
  
      // Draw image.
      canvas.drawBitmap(fBitmapCircleImageAndBorder, 0, 0, fPaint);
      
      // Draw reflection.
      if (mShowReflection) {
        fPaint.setColor(mReflectionColor);
        fPaint.setAlpha(mReflectionAlpha);
        fPaint.setXfermode(null);
        fLightPaint.setAlpha(fLightAlpha);
        canvas.drawCircle(fBorderCX, fBorderCY, fBorderRadius, fLightPaint);
        fPaint.setXfermode(null);
        canvas.drawBitmap(fBitmapAnimated, 0, 0, fPaint);
      }
    }
  }
  
  private void getAnimatedBitmap(){
    // Clear canvas
    fPaint.setXfermode(CLR);
    fPaint.setColor(Color.TRANSPARENT);
    fCanvasAnimated.drawPaint(fPaint);
    
    // Draw reflection
    fPaint.setXfermode(null);
    fPaint.setColor(mReflectionColor);
    fCanvasAnimated.drawPath(generateReflectionPath(fDiameter, fDiameter), fPaint);
  
    // Draw mask
    fPaint.setXfermode(DST_OUT);
    fCanvasAnimated.drawBitmap(fBitmapCircleMask, 0, 0, fPaint);
  }
  
  private Path generateReflectionPath(int w, int h) {
    fPathReflection.reset();
    // Left bottom corner
    fPathReflection.moveTo(fReflectionPos, h);
    // Right bottom corner
    fPathReflection.lineTo(w / 4 + fReflectionPos, h);
    // Right top corner
    fPathReflection.lineTo(w / 2 + fReflectionPos, 0);
    // Left top corner
    fPathReflection.lineTo(w / 4 + fReflectionPos, 0);
    fPathReflection.close();
    
    return fPathReflection;
  }
  
  private Bitmap generateCircleMaskBitmap(int w, int h, int cx, int cy, int radius) {
    Bitmap bitmapCircleMask = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmapCircleMask);
    
    fPaint.setXfermode(null);
    canvas.drawPaint(fPaint);
  
    fPaint.setXfermode(DST_OUT);
    canvas.drawCircle(cx, cy, radius, fPaint);
    return bitmapCircleMask;
  }
  
  private void loadBitmap() {
    Drawable drawable = this.getDrawable();
    
    if (drawable != null) {
      if (drawable instanceof RoundedBitmapDrawable) {
        RoundedBitmapDrawable roundedBitmapDrawable = (RoundedBitmapDrawable) drawable;
        fBitmapImage = roundedBitmapDrawable.getBitmap();
      } else if (drawable instanceof BitmapDrawable) {
        BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
        fBitmapImage = bitmapDrawable.getBitmap();
      }
    }
  }
  
  private void getAttrs() {
    mShowShadow = fTypedArray.getBoolean(R.styleable.CircleImageView_showShadow, mShowShadow);
    mShadowColor = fTypedArray.getColor(R.styleable.CircleImageView_shadowColor, mShadowColor);
    mShadowXDiff = fTypedArray.getDimensionPixelSize(R.styleable.CircleImageView_shadowXDiff, mShadowXDiff);
    mShadowYDiff = fTypedArray.getDimensionPixelSize(R.styleable.CircleImageView_shadowYDiff, mShadowYDiff);
    mShadowSize = fTypedArray.getDimensionPixelSize(R.styleable.CircleImageView_shadowSize, mShadowSize);
    mShadowAlpha = fTypedArray.getInt(R.styleable.CircleImageView_shadowAlpha, mShadowAlpha);
  
    mBorderColor = fTypedArray.getColor(R.styleable.CircleImageView_borderColor, mBorderColor);
    mBorderSize = fTypedArray.getDimensionPixelSize(R.styleable.CircleImageView_borderSize, mBorderSize);
  
    mShowReflection = fTypedArray.getBoolean(R.styleable.CircleImageView_showReflection, mShowReflection);
    mReflectionColor = fTypedArray.getColor(R.styleable.CircleImageView_reflectionColor, mReflectionColor);
    mReflectionAlpha = fTypedArray.getInt(R.styleable.CircleImageView_reflectionAlpha, mReflectionAlpha);
  
    mLightPassDuration = fTypedArray.getInt(R.styleable.CircleImageView_lightPassDuration, mLightPassDuration);
    mAnimationRepeatDelay = fTypedArray.getInt(R.styleable.CircleImageView_animationRepeatDelay, mAnimationRepeatDelay);
    mMinLightAlpha = fTypedArray.getInt(R.styleable.CircleImageView_minLightAlpha, mMinLightAlpha);
    mMaxLightAlpha = fTypedArray.getInt(R.styleable.CircleImageView_maxLightAlpha, mMaxLightAlpha);
    mMinShadowAlpha = fTypedArray.getInt(R.styleable.CircleImageView_minShadowAlpha, mMinShadowAlpha);
    mMaxShadowAlpha = fTypedArray.getInt(R.styleable.CircleImageView_maxShadowAlpha, mMaxShadowAlpha);
    mShadowReverseAnimationDuration = fTypedArray.getInt(
        R.styleable.CircleImageView_shadowReverseAnimationDuration, mShadowReverseAnimationDuration);
    mShadowReverseAnimationDelay = fTypedArray.getInt(
        R.styleable.CircleImageView_shadowReverseAnimationDelay, mShadowReverseAnimationDelay);
  
    mLightDirection = LightDirection.fromId(
        fTypedArray.getInt(R.styleable.CircleImageView_lightDirection, mLightDirection.id));
    
    fTypedArray.recycle();
  }
  
  public int getShadowXDiff() {
    return mShadowXDiff;
  }
  
  public CircleImageView setShadowXDiff(int shadowXDiff) {
    mShadowXDiff = shadowXDiff;
    invalidate();
    return this;
  }
  
  public int getShadowYDiff() {
    return mShadowYDiff;
  }
  
  public CircleImageView setShadowYDiff(int shadowYDiff) {
    mShadowYDiff = shadowYDiff;
    invalidate();
    return this;
  }
  
  public int getShadowSize() {
    return mShadowSize;
  }
  
  public CircleImageView setShadowSize(int shadowSize) {
    mShadowSize = shadowSize;
    invalidate();
    return this;
  }
  
  public int getShadowColor() {
    return mShadowColor;
  }
  
  public CircleImageView setShadowColor(int shadowColor) {
    mShadowColor = shadowColor;
    invalidate();
    return this;
  }
  
  public int getShadowAlpha() {
    return mShadowAlpha;
  }
  
  public CircleImageView setShadowAlpha(int shadowAlpha) {
    mShadowAlpha = shadowAlpha;
    invalidate();
    return this;
  }
  
  public boolean isShowShadow() {
    return mShowShadow;
  }
  
  public CircleImageView setShowShadow(boolean showShadow) {
    mShowShadow = showShadow;
    invalidate();
    return this;
  }
  
  public int getBorderColor() {
    return mBorderColor;
  }
  
  public CircleImageView setBorderColor(int borderColor) {
    mBorderColor = borderColor;
    invalidate();
    return this;
  }
  
  public int getBorderSize() {
    return mBorderSize;
  }
  
  public CircleImageView setBorderSize(int borderSize) {
    mBorderSize = borderSize;
    invalidate();
    return this;
  }
  
  public int getReflectionColor() {
    return mReflectionColor;
  }
  
  public CircleImageView setReflectionColor(int reflectionColor) {
    mReflectionColor = reflectionColor;
    invalidate();
    return this;
  }
  
  public int getReflectionAlpha() {
    return mReflectionAlpha;
  }
  
  public CircleImageView setReflectionAlpha(int reflectionAlpha) {
    mReflectionAlpha = reflectionAlpha;
    invalidate();
    return this;
  }
  
  public boolean isShowReflection() {
    return mShowReflection;
  }
  
  public CircleImageView setShowReflection(boolean showReflection) {
    mShowReflection = showReflection;
    invalidate();
    return this;
  }
  
  public int getLightPassDuration() {
    return mLightPassDuration;
  }
  
  public CircleImageView setLightPassDuration(int lightPassDuration) {
    mLightPassDuration = lightPassDuration;
    invalidate();
    return this;
  }
  
  public int getAnimationRepeatDelay() {
    return mAnimationRepeatDelay;
  }
  
  public CircleImageView setAnimationRepeatDelay(int animationRepeatDelay) {
    mAnimationRepeatDelay = animationRepeatDelay;
    invalidate();
    return this;
  }
  
  public int getMinShadowAlpha() {
    return mMinShadowAlpha;
  }
  
  public CircleImageView setMinShadowAlpha(int minShadowAlpha) {
    mMinShadowAlpha = minShadowAlpha;
    invalidate();
    return this;
  }
  
  public int getMaxShadowAlpha() {
    return mMaxShadowAlpha;
  }
  
  public CircleImageView setMaxShadowAlpha(int maxShadowAlpha) {
    mMaxShadowAlpha = maxShadowAlpha;
    invalidate();
    return this;
  }
  
  public int getShadowReverseAnimationDelay() {
    return mShadowReverseAnimationDelay;
  }
  
  public CircleImageView setShadowReverseAnimationDelay(int shadowReverseAnimationDelay) {
    mShadowReverseAnimationDelay = shadowReverseAnimationDelay;
    invalidate();
    return this;
  }
  
  public int getShadowReverseAnimationDuration() {
    return mShadowReverseAnimationDuration;
  }
  
  public CircleImageView setShadowReverseAnimationDuration(int shadowReverseAnimationDuration) {
    mShadowReverseAnimationDuration = shadowReverseAnimationDuration;
    invalidate();
    return this;
  }
  
  public int getMinLightAlpha() {
    return mMinLightAlpha;
  }
  
  public CircleImageView setMinLightAlpha(int minLightAlpha) {
    mMinLightAlpha = minLightAlpha;
    invalidate();
    return this;
  }
  
  public int getMaxLightAlpha() {
    return mMaxLightAlpha;
  }
  
  public CircleImageView setMaxLightAlpha(int maxLightAlpha) {
    mMaxLightAlpha = maxLightAlpha;
    invalidate();
    return this;
  }
  
  public LightDirection getLightDirection() {
    return mLightDirection;
  }
  
  public CircleImageView setLightDirection(LightDirection lightDirection) {
    mLightDirection = lightDirection;
    invalidate();
    return this;
  }
}
