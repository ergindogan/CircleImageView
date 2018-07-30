package volkanatalan.circleimageview.views;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
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
  private Context uContext;
  private TypedArray uTypedArray;
  private Bitmap uBitmap, uBitmapImage, uBitmapReflection, uBitmapCircleMask, uBitmapAnimated;
  private Canvas uCanvasAnimated;
  private Paint uPaint, uLightPaint;
  private Handler uHandler;
  private Runnable uRunnable;
  private ValueAnimator uReflectionXAnimator, uLightAlphaAnimator;
  private ValueAnimator uShadowXAnimator, uShadowReverseAnimation, uShadowAlphaAnimator;
  private AnimatorSet uAnimatorSet;
  private PorterDuffXfermode DST_OUT = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);
  private PorterDuffXfermode CLR = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
  private boolean uIsImageSet = false;
  
  private int uDiameter;
  
  private int uShadowXDiff, uShadowYDiff, uShadowSize = 0;
  private int uShadowColor = Color.BLACK, uShadowAlpha = 50;
  private int uShadowCX, uShadowCY, uShadowRadius, uShadowAnimatedCX;
  private int uShadowAlphaAdd;
  private boolean uShowShadow = true;
  
  private int uBorderColor = Color.BLACK;
  private int uBorderCX, uBorderCY, uBorderRadius, uBorderSize;
  
  private int uCircleMaskCX, uCircleMaskCY, uCircleMaskRadius;
  
  private int uReflectionColor = Color.WHITE, uReflectionAlpha = 220;
  private int uReflectionPos, uLightAlpha = 0;
  private boolean uShowReflection = true;
  
  private int uLightAnimationDuration = 1000, uAnimationRepeatDelay = 5000;
  private int uShadowAlphaAnimationStart = 0, uShadowAlphaAnimationEnd = 50;
  private int uShadowReverseAnimationDelay = 0, uShadowReverseAnimationDuration = uLightAnimationDuration;
  private int uLightAlphaAnimationStart = 0, uLightAlphaAnimationEnd = 100;
  
  public enum lightDirection {LEFT, RIGHT}
  
  public CircleImageView(Context context) {
    super(context);
    uContext = context;
    start();
  }
  
  public CircleImageView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    uContext = context;
    uTypedArray = context.obtainStyledAttributes(attrs, R.styleable.CircleImageView);
    start();
  }
  
  public CircleImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    uContext = context;
    uTypedArray = context.obtainStyledAttributes(attrs, R.styleable.CircleImageView, defStyleAttr, 0);
    start();
  }
  
  private void start() {
    uBorderSize = Calc.dpToPx(uContext, 5);
    uShadowXDiff = Calc.dpToPx(uContext, 5);
    uShadowYDiff = Calc.dpToPx(uContext, 5);
    
    final int animationRepeatDelay = uAnimationRepeatDelay + uLightAnimationDuration;
    
    getAttrs();
  
    uPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    uPaint.setDither(true);
    uPaint.setStyle(Paint.Style.FILL);
  
    if (isShowReflection()) {
      uLightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
      uLightPaint.setDither(true);
      uLightPaint.setStyle(Paint.Style.FILL);
      uLightPaint.setColor(uReflectionColor);
      uLightPaint.setAlpha(0);
  
      uHandler = new Handler();
  
      uRunnable = new Runnable() {
        @Override
        public void run() {
//          uReflectionWidthAnimator.start();
//          uReflectionXAnimator.start();
//          uLightAlphaAnimator.start();
          uAnimatorSet.start();
      
          // Repeat
          uHandler.postDelayed(uRunnable, animationRepeatDelay);
        }
      };
    }
    
    setDrawingCacheEnabled(false);
  }
  
  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    loadBitmap();
  
    int width = measureDimensions(widthMeasureSpec, heightMeasureSpec);
    int height = measureDimensions(heightMeasureSpec, widthMeasureSpec);
    
    setMeasuredDimension(width, height);
  }
  
  private int measureDimensions(int requiredMeasureSpec, int otherMeasureSpec) {
    int requiredMeasureSpecMode = MeasureSpec.getMode(requiredMeasureSpec);
    int requiredMeasureSpecSize = MeasureSpec.getSize(requiredMeasureSpec);
    
    int otherMeasureSpecMode = MeasureSpec.getMode(otherMeasureSpec);
    int otherMeasureSpecSize = MeasureSpec.getSize(otherMeasureSpec);
    
    if (otherMeasureSpecMode == MeasureSpec.EXACTLY &&
            requiredMeasureSpecMode == MeasureSpec.AT_MOST) {
      if (otherMeasureSpecSize < requiredMeasureSpecSize) {
        return otherMeasureSpecSize;
      } else { return requiredMeasureSpecSize; }
    } else if (requiredMeasureSpecMode == MeasureSpec.EXACTLY) {
      return requiredMeasureSpecSize;
    } else if (otherMeasureSpecMode == MeasureSpec.AT_MOST &&
            requiredMeasureSpecMode == MeasureSpec.AT_MOST) {
      return Math.min(requiredMeasureSpecSize, otherMeasureSpecSize);
    } else {
      return requiredMeasureSpecSize;
    }
  }
  
  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    int paddingLeft = getPaddingLeft();
    int paddingTop = getPaddingTop();
    int paddingRight = getPaddingRight();
    int paddingBottom = getPaddingBottom();
    
    uDiameter = Math.min(w, h);
    int radius = uDiameter / 2;
    int centerX = radius;
    int centerY = radius;
  
    uBorderCX = centerX + paddingLeft - paddingRight;
    uBorderCY = centerY + paddingTop - paddingBottom;
    int borderDiameter = uDiameter - Math.max(paddingLeft + paddingRight, paddingTop + paddingBottom);
    uBorderRadius = borderDiameter / 2;
  
    uShadowCX = uBorderCX + uShadowXDiff;
    uShadowCY = uBorderCY + uShadowYDiff;
    uShadowRadius = uBorderRadius + uShadowSize;
    int uShadowAnimationStart = centerX - (uShadowCX - centerX);
    int uShadowAnimationEnd = uShadowCX;
    uShadowAnimatedCX = uShadowAnimationStart;
  
    int uImageCX = uBorderCX;
    int uImageCY = uBorderCY;
    int uImageRadius = uBorderRadius - uBorderSize;
  
    int reflectionWidth = uDiameter;
    int reflectionHeight = uDiameter;
    int reflectionPosStart = borderDiameter;
    int reflectionPosEnd = 0 - reflectionWidth;
    uReflectionPos = reflectionPosStart;
    int lightAlphaAnimationRepeatDelayDuration = uLightAnimationDuration - (uLightAnimationDuration * 40 / 100);
  
    uCircleMaskCX = uBorderCX;
    uCircleMaskCY = uBorderCY;
    uCircleMaskRadius = uBorderRadius;
    
    if (uBitmapImage != null) {
      float bitmapWidth = uBitmapImage.getWidth();
      float bitmapHeight = uBitmapImage.getHeight();
      float rateOfMin = uDiameter / Math.min(bitmapHeight, bitmapWidth);
      float scaledBitmapWidth = rateOfMin * bitmapWidth;
      float scaledBitmapHeight = rateOfMin * bitmapHeight;
  
      uBitmapImage = Bitmap.createScaledBitmap(
          uBitmapImage, (int) scaledBitmapWidth, (int) scaledBitmapHeight, false);
      BitmapShader uBitmapShader = new BitmapShader(uBitmapImage, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
  
      Paint paintImage = new Paint(Paint.ANTI_ALIAS_FLAG);
      paintImage.setDither(true);
      paintImage.setShader(uBitmapShader);
      
      uBitmap = Bitmap.createBitmap(uDiameter, uDiameter, Bitmap.Config.ARGB_8888);
      Canvas canvas = new Canvas(uBitmap);
      
      // Draw border.
      if (uBorderSize > 0) {
        uPaint.setColor(uBorderColor);
        canvas.drawCircle(uBorderCX, uBorderCY, uBorderRadius, uPaint);
      }
  
      // Draw image.
      canvas.drawCircle(uImageCX, uImageCY, uImageRadius, paintImage);
    }
  
    if (uShowReflection) {
      uBitmapAnimated = Bitmap.createBitmap(uDiameter, uDiameter, Bitmap.Config.ARGB_8888);
      uCanvasAnimated = new Canvas(uBitmapAnimated);
    
      uBitmapReflection = generateReflectionBitmap(reflectionWidth, reflectionHeight);
      uBitmapCircleMask = generateCircleMaskBitmap(reflectionWidth, reflectionHeight);
  
      uReflectionXAnimator = ValueAnimator.ofInt(reflectionPosStart, reflectionPosEnd);
      uReflectionXAnimator.setDuration(uLightAnimationDuration);
      uReflectionXAnimator.setInterpolator(new FastOutSlowInInterpolator());
      uReflectionXAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
          uReflectionPos = (int)valueAnimator.getAnimatedValue();
        }
      });
  
      uLightAlphaAnimator = ValueAnimator.ofInt(uLightAlphaAnimationStart, uLightAlphaAnimationEnd);
      uLightAlphaAnimator.setDuration(lightAlphaAnimationRepeatDelayDuration);
      uLightAlphaAnimator.setInterpolator(new CycleInterpolator(0.5f));
      uLightAlphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
          uLightAlpha = (int) valueAnimator.getAnimatedValue();
        }
      });
  
      if (uBorderSize > 0) {
        uShadowXAnimator = ValueAnimator.ofInt(uShadowAnimationStart, uShadowAnimationEnd);
        uShadowXAnimator.setDuration(uLightAnimationDuration);
        uShadowXAnimator.setInterpolator(new FastOutSlowInInterpolator());
        uShadowXAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
          @Override
          public void onAnimationUpdate(ValueAnimator valueAnimator) {
            uShadowAnimatedCX = (int) valueAnimator.getAnimatedValue();
            invalidate();
          }
        });
  
        uShadowAlphaAnimator = ValueAnimator.ofInt(uShadowAlphaAnimationStart, uShadowAlphaAnimationEnd);
        uShadowAlphaAnimator.setDuration(lightAlphaAnimationRepeatDelayDuration);
        uShadowAlphaAnimator.setInterpolator(new CycleInterpolator(0.5f));
        uShadowAlphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
          @Override
          public void onAnimationUpdate(ValueAnimator valueAnimator) {
            uShadowAlphaAdd = (int) valueAnimator.getAnimatedValue();
          }
        });
  
        uShadowReverseAnimation = ValueAnimator.ofInt(uShadowAnimationEnd, uShadowAnimationStart);
        uShadowReverseAnimation.setDuration(uShadowReverseAnimationDuration);
        uShadowReverseAnimation.setStartDelay(uLightAnimationDuration + uShadowReverseAnimationDelay);
        uShadowReverseAnimation.setInterpolator(new FastOutSlowInInterpolator());
        uShadowReverseAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
          @Override
          public void onAnimationUpdate(ValueAnimator valueAnimator) {
            uShadowAnimatedCX = (int) valueAnimator.getAnimatedValue();
            invalidate();
          }
        });
      }
      
      uAnimatorSet = new AnimatorSet();
      uAnimatorSet.play(uReflectionXAnimator);
      uAnimatorSet.play(uLightAlphaAnimator);
      if (uBorderSize > 0) {
        uAnimatorSet.play(uShadowXAnimator);
        uAnimatorSet.play(uShadowAlphaAnimator);
        uAnimatorSet.play(uShadowReverseAnimation);
      }
      
      // Start the Runnable
      if (uShowReflection)
        uHandler.post(uRunnable);
      
    } else {
      uReflectionXAnimator = null;
      uLightAlphaAnimator = null;
      uShadowXAnimator = null;
      uShadowAlphaAnimator = null;
      uShadowReverseAnimation = null;
    }
  }
  
  @Override
  protected void onDraw(Canvas canvas) {
    
    if (uBitmapImage != null) {
      if (uShowReflection) getAnimatedBitmap();
  
      // Draw shadow
      uPaint.setXfermode(null);
      uPaint.setColor(uShadowColor);
      uPaint.setAlpha(uShadowAlpha + uShadowAlphaAdd);
      canvas.drawCircle(uShadowAnimatedCX, uShadowCY, uShadowRadius, uPaint);
      
      // Draw border and image
      uPaint.setAlpha(255);
      canvas.drawBitmap(uBitmap, 0, 0, uPaint);
      
      // Draw reflection.
      if (uShowReflection) {
        uPaint.setColor(uReflectionColor);
        uPaint.setAlpha(uReflectionAlpha);
        uLightPaint.setAlpha(uLightAlpha);
        uPaint.setXfermode(null);
        canvas.drawCircle(uBorderCX, uBorderCY, uBorderRadius, uLightPaint);
        uPaint.setXfermode(null);
        canvas.drawBitmap(uBitmapAnimated, 0, 0, uPaint);
      }
    }
  }
  
  private void getAnimatedBitmap(){
    // Clear canvas
    uPaint.setXfermode(CLR);
    uPaint.setColor(Color.TRANSPARENT);
    uCanvasAnimated.drawPaint(uPaint);
    
    // Draw reflection
    uPaint.setXfermode(null);
    uPaint.setColor(uReflectionColor);
    uCanvasAnimated.drawBitmap(uBitmapReflection, uReflectionPos, 0, uPaint);
  
    // Draw mask
    uPaint.setXfermode(DST_OUT);
    uCanvasAnimated.drawBitmap(uBitmapCircleMask, 0, 0, uPaint);
  }
  
  private Bitmap generateReflectionBitmap(int w, int h) {
    Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
    Canvas canvasReflection = new Canvas(bitmap);
  
    Path uPathReflection = new Path();
    // Left bottom corner
    uPathReflection.moveTo(0, h);
    // Right bottom corner
    uPathReflection.lineTo(w / 4, h);
    // Right top corner
    uPathReflection.lineTo(w / 2, 0);
    // Left top corner
    uPathReflection.lineTo(w / 4, 0);
    uPathReflection.close();
    
    uPaint.setXfermode(null);
    uPaint.setColor(uReflectionColor);
    canvasReflection.drawPath(uPathReflection, uPaint);
    
    return bitmap;
  }
  
  private Bitmap generateCircleMaskBitmap(int w, int h) {
    Bitmap bitmapCircleMask = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmapCircleMask);
    
    uPaint.setXfermode(null);
    canvas.drawPaint(uPaint);
  
    uPaint.setXfermode(DST_OUT);
    canvas.drawCircle(uCircleMaskCX, uCircleMaskCY, uCircleMaskRadius, uPaint);
    return bitmapCircleMask;
  }
  
  private void loadBitmap() {
    Drawable drawable = this.getDrawable();
    
    if (drawable != null && !uIsImageSet) {
      if (drawable instanceof RoundedBitmapDrawable) {
        RoundedBitmapDrawable roundedBitmapDrawable = (RoundedBitmapDrawable) drawable;
        uBitmapImage = roundedBitmapDrawable.getBitmap();
      } else if (drawable instanceof BitmapDrawable) {
        BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
        uBitmapImage = bitmapDrawable.getBitmap();
      }
    }
  }
  
  private void getAttrs() {
    uShowShadow = uTypedArray.getBoolean(R.styleable.CircleImageView_showShadow, uShowShadow);
    uShadowColor = uTypedArray.getColor(R.styleable.CircleImageView_shadowColor, uShadowColor);
    uShadowXDiff = uTypedArray.getDimensionPixelSize(R.styleable.CircleImageView_shadowXDiff, uShadowXDiff);
    uShadowYDiff = uTypedArray.getDimensionPixelSize(R.styleable.CircleImageView_shadowYDiff, uShadowYDiff);
    uShadowSize = uTypedArray.getDimensionPixelSize(R.styleable.CircleImageView_shadowSize, uShadowSize);
    uShadowAlpha = uTypedArray.getInt(R.styleable.CircleImageView_shadowAlpha, uShadowAlpha);
  
    uBorderColor = uTypedArray.getColor(R.styleable.CircleImageView_borderColor, uBorderColor);
    uBorderSize = uTypedArray.getDimensionPixelSize(R.styleable.CircleImageView_borderSize, uBorderSize);
  
    uShowReflection = uTypedArray.getBoolean(R.styleable.CircleImageView_showReflection, uShowReflection);
    uReflectionColor = uTypedArray.getColor(R.styleable.CircleImageView_reflectionColor, uReflectionColor);
    uReflectionAlpha = uTypedArray.getInt(R.styleable.CircleImageView_reflectionAlpha, uReflectionAlpha);
  
    uLightAnimationDuration = uTypedArray.getInt(R.styleable.CircleImageView_lightAnimationDuration, uLightAnimationDuration);
    uAnimationRepeatDelay = uTypedArray.getInt(R.styleable.CircleImageView_animationRepeatDelay, uAnimationRepeatDelay);
    uLightAlphaAnimationStart = uTypedArray.getInt(R.styleable.CircleImageView_lightAlphaAnimationStart, uLightAlphaAnimationStart);
    uLightAlphaAnimationEnd = uTypedArray.getInt(R.styleable.CircleImageView_lightAlphaAnimationEnd, uLightAlphaAnimationEnd);
    uShadowAlphaAnimationStart = uTypedArray.getInt(R.styleable.CircleImageView_shadowAlphaAnimationStart, uShadowAlphaAnimationStart);
    uShadowAlphaAnimationEnd = uTypedArray.getInt(R.styleable.CircleImageView_shadowAlphaAnimationEnd, uShadowAlphaAnimationEnd);
    uShadowReverseAnimationDuration = uTypedArray.getInt(R.styleable.CircleImageView_shadowReverseAnimationDuration, uShadowReverseAnimationDuration);
    uShadowReverseAnimationDelay = uTypedArray.getInt(R.styleable.CircleImageView_shadowReverseAnimationDelay, uShadowReverseAnimationDelay);
    
    uTypedArray.recycle();
  }
  
  public CircleImageView setImage(String imagePath) {
    uBitmapImage = BitmapFactory.decodeFile(imagePath);
    uIsImageSet = true;
    invalidate();
    return this;
  }
  
  public int getBorderColor() {
    return uBorderColor;
  }
  
  public CircleImageView setBorderColor(int borderColor) {
    uBorderColor = borderColor;
    invalidate();
    return this;
  }
  
  public boolean isShowReflection() {
    return uShowReflection;
  }
  
  public CircleImageView setShowReflection(boolean show) {
    this.uShowReflection = show;
    requestLayout();
    return this;
  }
}
