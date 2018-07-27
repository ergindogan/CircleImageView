package volkanatalan.circleimageview.views;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
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
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.CycleInterpolator;
import android.view.animation.DecelerateInterpolator;

import volkanatalan.library.Calc;

public class CircleImageView extends AppCompatImageView {
  Context context;
  private Bitmap uBitmapImage, uBitmapReflection;
  private Paint uPaint, uPaintImage;
  private Handler uHandler;
  private Runnable uRunnable;
  private ValueAnimator uReflectionXAnimator, uReflectionWidthAnimator;
  private PorterDuffXfermode DST_OUT = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);
  private boolean uIsImageSet = false;
  
  private int uShadowXDiff = 10, uShadowYDiff = 10, uShadowSize = 0;
  private int uShadowCX, uShadowCY, uShadowRadius, uShadowDiameter;
  private int uShadowColor = Color.BLACK, uShadowAlpha = 127;
  private boolean uShowShadow = true;
  
  private int uBorderColor = Color.BLACK;
  private int uBorderCX;
  private int uBorderCY;
  private int uBorderRadius;
  private int uBorderThickness;
  private boolean uShowBorder = true;
  
  private int uImageCX, uImageCY, uImageRadius;
  
  private int uCircleMaskCX, uCircleMaskCY, uCircleMaskRadius;
  
  private int uReflectionColor = Color.WHITE;
  private int uReflectionAlpha = 245;
  private int uReflectionWidth, uReflectionHeight, uReflectionWidthAdd, uReflectionPos;
  private boolean uShowReflection = true;
  private int uAnimationDuration = 1500, uAnimationRepeatDelay = 3000;
  
  public CircleImageView(Context context) {
    super(context);
    this.context = context;
    start();
  }
  
  public CircleImageView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    this.context = context;
    start();
  }
  
  public CircleImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    this.context = context;
    start();
  }
  
  private void start() {
    uBorderThickness = Calc.dpToPx(context, 10);
    
    setAttrs();
  
    loadBitmap();
  
    uPaint = new Paint();
    uPaint.setAntiAlias(true);
    uPaint.setDither(true);
    uPaint.setStyle(Paint.Style.FILL);
    
    uPaintImage = new Paint(Paint.ANTI_ALIAS_FLAG);
    uPaintImage.setDither(true);
    
    uHandler = new Handler();
  
    uRunnable = new Runnable() {
      @Override
      public void run() {
        uReflectionWidthAnimator.start();
        uReflectionXAnimator.start();
      
        // Repeat
        uHandler.postDelayed(uRunnable, uAnimationRepeatDelay);
      }
    };
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
    int diameter = Math.min(w, h);
    int radius = diameter / 2;
    int centerX = radius;
    int centerY = radius;
  
    uShadowCX = centerX + uShadowXDiff;
    uShadowCY = centerY + uShadowYDiff;
    uShadowRadius = radius + uShadowSize;
    uShadowDiameter = diameter + uShadowSize;
  
    // TODO uBorderCX = centerX +- (PADDINGS);
    uBorderCX = centerX;
    // TODO uBorderCY = centerY +- (PADDINGS);
    uBorderCY = centerY;
    // TODO borderDiameter = diameter - (PADDINGS);
    int borderDiameter = diameter;
    uBorderRadius = borderDiameter / 2;
  
    uImageCX = uBorderCX;
    uImageCY = uBorderCY;
    uImageRadius = uBorderRadius - uBorderThickness;
  
    uReflectionWidth = borderDiameter;
    uReflectionHeight = borderDiameter;
    int reflectionPosStart = borderDiameter;
    int reflectionPosEnd = 0 - uReflectionWidth;
    uReflectionPos = reflectionPosStart;
  
    uCircleMaskCX = uBorderCX;
    uCircleMaskCY = uBorderCY;
    uCircleMaskRadius = uBorderRadius;
    
    if (uBitmapImage != null) {
      float bitmapWidth = uBitmapImage.getWidth();
      float bitmapHeight = uBitmapImage.getHeight();
      float rateOfMin = diameter / Math.min(bitmapHeight, bitmapWidth);
      float scaledBitmapWidth = rateOfMin * bitmapWidth;
      float scaledBitmapHeight = rateOfMin * bitmapHeight;
  
      uBitmapImage = Bitmap.createScaledBitmap(
          uBitmapImage, (int) scaledBitmapWidth, (int) scaledBitmapHeight, false);
      BitmapShader uBitmapShader = new BitmapShader(uBitmapImage, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
      uPaintImage.setShader(uBitmapShader);
    }
  
    if (uShowReflection) {
      uReflectionWidthAnimator = ValueAnimator.ofInt(0, uReflectionWidth / 3);
      uReflectionWidthAnimator.setDuration(uAnimationDuration);
      uReflectionWidthAnimator.setInterpolator(new CycleInterpolator(0.8f));
      uReflectionWidthAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
          uReflectionWidthAdd = (int) valueAnimator.getAnimatedValue();
        }
      });
      
      uReflectionXAnimator = ValueAnimator.ofInt(reflectionPosStart, reflectionPosEnd);
      uReflectionXAnimator.setDuration(uAnimationDuration);
      uReflectionXAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
      uReflectionXAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
          uReflectionPos = (int) valueAnimator.getAnimatedValue();
          invalidate();
        }
      });
  
      // Start the Runnable
      uHandler.post(uRunnable);
    }
  }
  
  @SuppressLint("DrawAllocation")
  @Override
  protected void onDraw(Canvas canvas) {
    
    if (uBitmapImage != null) {
      if (uShowReflection) {
        uBitmapReflection = generateReflectionBitmap(uReflectionWidth, uReflectionHeight);
      }
      uPaint.setXfermode(null);
      
      // Draw border.
      if (uShowBorder) {
        uPaint.setColor(uBorderColor);
        canvas.drawCircle(uBorderCX, uBorderCY, uBorderRadius, uPaint);
      }
      
      // Draw image.
      canvas.drawCircle(uImageCX, uImageCY, uImageRadius, uPaintImage);
      
      // Draw reflection.
      if (uShowReflection) {
      uPaint.setColor(uReflectionColor);
      canvas.drawBitmap(uBitmapReflection, 0, 0, uPaint);
      }
    }
  }
  
  private Bitmap generateReflectionBitmap(int w, int h) {
    Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
    Canvas canvasReflection = new Canvas(bitmap);
  
    Path uPathReflection = new Path();
    // Left bottom corner
    uPathReflection.moveTo(uReflectionPos, h);
    // Right bottom corner
    uPathReflection.lineTo(uReflectionPos + uReflectionWidthAdd, h);
    // Right top corner
    uPathReflection.lineTo(uReflectionPos + w / 4 + uReflectionWidthAdd, 0);
    // Left top corner
    uPathReflection.lineTo(uReflectionPos + w / 4, 0);
    uPathReflection.close();
    
    uPaint.setXfermode(null);
    uPaint.setColor(uReflectionColor);
    uPaint.setAlpha(uReflectionAlpha);
    canvasReflection.drawPath(uPathReflection, uPaint);
    
    uPaint.setXfermode(DST_OUT);
    canvasReflection.drawBitmap(generateCircleMaskBitmap(w, h), 0, 0, uPaint);
    
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
    return this;
  }
  
  public int getBorderColor() {
    return uBorderColor;
  }
  
  public CircleImageView setBorderColor(int borderColor) {
    uBorderColor = borderColor;
    this.invalidate();
    return this;
  }
  
  public boolean isShowBorder() {
    return uShowBorder;
  }
  
  public CircleImageView setShowBorder(boolean show) {
    uShowBorder = show;
    return this;
  }
  
  public boolean isShowReflection() {
    return uShowReflection;
  }
  
  public CircleImageView setShowReflection(boolean show) {
    this.uShowReflection = show;
    return this;
  }
}
