package volkanatalan.circleimageview.views;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
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

import volkanatalan.library.Calc;

public class CircleImageView extends AppCompatImageView {
  Context uContext;
  private Bitmap uBitmap, uBitmapImage, uBitmapReflection, uBitmapCircleMask;
  private Paint uPaint, uPaintImage, uLightPaint;
  private Handler uHandler;
  private Runnable uRunnable;
  private ValueAnimator uReflectionXAnimator, uLightAlphaAnimator;
  private ValueAnimator uShadowAnimator, uShadowReverseAnimation;
  private AnimatorSet uAnimatorSet;
  private PorterDuffXfermode DST_OUT = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);
  private boolean uIsImageSet = false;
  
  private int uDiameter;
  
  private int uShadowXDiff = 12, uShadowYDiff = 12, uShadowSize = 0;
  private int SHADOW_COLOR = Color.BLACK, SHADOW_ALPHA = 50;
  private int uShadowCX, uShadowCY, uShadowRadius, uShadowDiameter, uShadowAnimatedCX;
  private int uShadowAnimationStart, uShadowAnimationEnd, uShadowReverseAnimationDelay;
  private boolean uShowShadow = true;
  
  private int uBorderColor = Color.BLACK;
  private int uShadowAlpha = 100;
  private int uBorderCX, uBorderCY, uBorderRadius, uBorderThickness;
  private boolean uShowBorder = true;
  
  private int uCircleMaskCX, uCircleMaskCY, uCircleMaskRadius;
  
  private int uReflectionColor = Color.WHITE, uReflectionAlpha = 220;
  private int uReflectionWidth, uReflectionHeight, uReflectionPos, uLightAlpha;
  private boolean uShowReflection = true;
  private int uLightAlphaAnimationStart = 0, uLightAlphaAnimationEnd = 100;
  private int uAnimationDuration = 1000, uAnimationRepeatDelay = 5000;
  
  public CircleImageView(Context context) {
    super(context);
    uContext = context;
    start();
  }
  
  public CircleImageView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    uContext = context;
    start();
  }
  
  public CircleImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    uContext = context;
    start();
  }
  
  private void start() {
    uBorderThickness = Calc.dpToPx(uContext, 5);
    final int animationRepeatDelay = uAnimationRepeatDelay + uAnimationDuration;
    
    setAttrs();
    loadBitmap();
  
    uPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    uPaint.setDither(true);
    uPaint.setStyle(Paint.Style.FILL);
  
    uPaintImage = new Paint(Paint.ANTI_ALIAS_FLAG);
    uPaintImage.setDither(true);
  
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
    uShadowDiameter = borderDiameter + uShadowSize;
    uShadowAnimationStart = centerX - (uShadowCX - centerX);
    uShadowAnimationEnd = uShadowCX;
  
    int uImageCX = uBorderCX;
    int uImageCY = uBorderCY;
    int uImageRadius = uBorderRadius - uBorderThickness;
  
    uReflectionWidth = uDiameter;
    uReflectionHeight = uDiameter;
    int reflectionPosStart = borderDiameter;
    int reflectionPosEnd = 0 - uReflectionWidth;
    int reflectionWidthStart = 0;
    final int reflectionWidthEnd = uReflectionWidth / 3;
    uReflectionPos = reflectionPosStart;
    int lightAlphaAnimationRepeatDelayDuration = uAnimationDuration - (uAnimationDuration * 40 / 100);
  
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
      uPaintImage.setShader(uBitmapShader);
      
      uBitmap = Bitmap.createBitmap(uDiameter, uDiameter, Bitmap.Config.ARGB_8888);
      Canvas canvas = new Canvas(uBitmap);
      
      // Draw border.
      if (uShowBorder) {
        uPaint.setColor(uBorderColor);
        canvas.drawCircle(uBorderCX, uBorderCY, uBorderRadius, uPaint);
      }
  
      // Draw image.
      canvas.drawCircle(uImageCX, uImageCY, uImageRadius, uPaintImage);
    }
  
    if (uShowReflection) {
      uBitmapReflection = generateReflectionBitmap(uReflectionWidth, uReflectionHeight);
      uBitmapCircleMask = generateCircleMaskBitmap(uReflectionWidth, uReflectionHeight);
  
      uReflectionXAnimator = ValueAnimator.ofInt(reflectionPosStart, reflectionPosEnd);
      uReflectionXAnimator.setDuration(uAnimationDuration);
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
  
      uShadowAnimator = ValueAnimator.ofInt(uShadowAnimationStart, uShadowAnimationEnd);
      uShadowAnimator.setDuration(uAnimationDuration);
      uShadowAnimator.setInterpolator(new FastOutSlowInInterpolator());
      uShadowAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
          uShadowAnimatedCX = (int) valueAnimator.getAnimatedValue();
          invalidate();
        }
      });
  
      uShadowReverseAnimation = ValueAnimator.ofInt(uShadowAnimationEnd, uShadowAnimationStart);
      uShadowReverseAnimation.setDuration(uAnimationDuration);
      uShadowReverseAnimation.setStartDelay(uAnimationDuration + uShadowReverseAnimationDelay);
      uShadowReverseAnimation.setInterpolator(new FastOutSlowInInterpolator());
      uShadowReverseAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
          uShadowAnimatedCX = (int) valueAnimator.getAnimatedValue();
          invalidate();
        }
      });
      
      uAnimatorSet = new AnimatorSet();
      uAnimatorSet.play(uReflectionXAnimator);
      uAnimatorSet.play(uLightAlphaAnimator);
      uAnimatorSet.play(uShadowAnimator);
      uAnimatorSet.play(uShadowReverseAnimation);
  
      // Start the Runnable
      uHandler.post(uRunnable);
      
    } else {
      uReflectionXAnimator = null;
      uLightAlphaAnimator = null;
      uShadowAnimator = null;
    }
  }
  
  @Override
  protected void onDraw(Canvas canvas) {
    
    if (uBitmapImage != null) {
      Bitmap animatedBitmap = getAnimatedBitmap();
      
      // Draw shadow
      uPaint.setXfermode(null);
      uPaint.setColor(Color.BLACK);
      uPaint.setAlpha(uShadowAlpha);
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
        canvas.drawBitmap(animatedBitmap, 0, 0, uPaint);
      }
    }
  }
  
  private Bitmap getAnimatedBitmap(){
    Bitmap bitmap = Bitmap.createBitmap(uReflectionWidth, uReflectionHeight,
        Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    
    // Draw reflection
    uPaint.setXfermode(null);
    uPaint.setColor(uReflectionColor);
    canvas.drawBitmap(Bitmap.createScaledBitmap(
          uBitmapReflection, uReflectionWidth, uReflectionHeight, false),
        uReflectionPos, 0, uPaint);
  
    // Draw mask
    uPaint.setXfermode(DST_OUT);
    canvas.drawBitmap(uBitmapCircleMask, 0, 0, uPaint);
    
    return bitmap;
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
      if (drawable instanceof BitmapDrawable) {
        BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
        uBitmapImage = bitmapDrawable.getBitmap();
      } else if (drawable instanceof RoundedBitmapDrawable) {
        RoundedBitmapDrawable roundedBitmapDrawable = (RoundedBitmapDrawable) drawable;
        uBitmapImage = roundedBitmapDrawable.getBitmap();
      }
    }
  }
  
  private void setAttrs() {
    //TODO setAttrs
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
  
  public boolean isShowBorder() {
    return uShowBorder;
  }
  
  public CircleImageView setShowBorder(boolean show) {
    uShowBorder = show;
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
