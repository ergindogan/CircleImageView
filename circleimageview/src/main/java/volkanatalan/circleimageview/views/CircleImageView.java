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

import volkanatalan.library.Calc;

public class CircleImageView extends AppCompatImageView {
  Context context;
  private Bitmap bitmapImage, bitmapReflection;
  private Path pathReflection;
  private Paint paint, paintImage;
  private BitmapShader bitmapShader;
  private float imageWidth, imageHeight, borderWidth, diameter, center;
  private float shadowRadius = 4f, shadowDX = 2f, shadowDY = 2f;
  private int animationDuration = 2000;
  private int animationRepeatDelay = 3000;
  private int reflectionLeft = 0;
  private int shadowColor = Color.BLACK, borderColor = Color.BLACK, reflectionColor = Color.WHITE;
  private boolean isImageSet = false;
  private Handler handler;
  private Runnable runnable;
  private ValueAnimator reflectionXAnimator;
  private PorterDuffXfermode DST_OUT = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);
  
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
    borderWidth = Calc.dpToPx(context, 10);
    
    setAttrs();
  
    paint = new Paint();
    paint.setAntiAlias(true);
    paint.setDither(true);
    paint.setStyle(Paint.Style.FILL);
    
    paintImage = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintImage.setDither(true);
    
    handler = new Handler();
  
    runnable = new Runnable() {
      @Override
      public void run() {
        reflectionXAnimator.start();
      
        // Repeat
        handler.postDelayed(runnable, animationRepeatDelay);
      }
    };
  }
  
  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int width = measureDimensions(widthMeasureSpec, heightMeasureSpec);
    int height = measureDimensions(heightMeasureSpec, widthMeasureSpec);
    diameter = Math.min(width, height);
    
    imageWidth = width - (borderWidth * 2) - shadowRadius;
    imageHeight = height - (borderWidth * 2) - shadowRadius;
    
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
    super.onSizeChanged(w, h, oldw, oldh);
  
    float centerX = imageWidth / 2;
    float centerY = imageHeight / 2;
    center = Math.min(centerX, centerY);
  
    /*reflectionXAnimator = ValueAnimator.ofInt((int) imageWidth, 0 - bitmapReflection.getWidth());
    reflectionXAnimator.setDuration(animationDuration);
    reflectionXAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      @Override
      public void onAnimationUpdate(ValueAnimator valueAnimator) {
        reflectionLeft = (int) valueAnimator.getAnimatedValue();
        invalidate();
      }
    });*/

    // Start the Runnable
    //handler.post(runnable);
  }
  
  @SuppressLint("DrawAllocation")
  @Override
  protected void onDraw(Canvas canvas) {
    loadBitmap();
    
    if (bitmapImage != null) {
      float bitmapWidth = bitmapImage.getWidth();
      float bitmapHeight = bitmapImage.getHeight();
      float rateOfMin = diameter / Math.min(bitmapHeight, bitmapWidth);
      float scaledBitmapWidth = rateOfMin * bitmapWidth;
      float scaledBitmapHeight = rateOfMin * bitmapHeight;
      
      bitmapImage = Bitmap.createScaledBitmap(
          bitmapImage, (int) scaledBitmapWidth, (int) scaledBitmapHeight, false);
      bitmapShader = new BitmapShader(bitmapImage, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
      paintImage.setShader(bitmapShader);
      bitmapReflection = generateReflectionBitmap((int) scaledBitmapWidth, (int) scaledBitmapHeight);
  
      float radius = center;
  
      paint.setColor(Color.GRAY);
      paint.setXfermode(null);
      canvas.drawPaint(paint);
      
      // Draw border.
      paint.setColor(borderColor);
      paint.setXfermode(null);
      canvas.drawCircle(center + borderWidth, center + borderWidth,
          radius + borderWidth - shadowRadius, paint);
      
      // Draw image.
      canvas.drawCircle(center + borderWidth, center + borderWidth,
          radius - shadowRadius, paintImage);
      
      // Draw reflection.
      paint.setColor(reflectionColor);
      canvas.drawBitmap(bitmapReflection, 0, 0, paint);
    }
  }
  
  private Bitmap generateCircleMaskBitmap(int w, int h) {
    Bitmap bitmapCircleMask = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmapCircleMask);
    
    paint.setXfermode(null);
    canvas.drawPaint(paint);
  
    paint.setXfermode(DST_OUT);
    canvas.drawCircle(center + borderWidth, center + borderWidth,
        center + borderWidth - shadowRadius, paint);
    return bitmapCircleMask;
  }
  
  private Bitmap generateReflectionBitmap(int w, int h) {
    Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
    Canvas canvasReflection = new Canvas(bitmap);
  
    pathReflection = new Path();
    pathReflection.moveTo(0, h);
    pathReflection.lineTo(w / 4, h);
    pathReflection.lineTo(w / 2, 0);
    pathReflection.lineTo(w / 4, 0);
    pathReflection.close();
  
    paint.setXfermode(null);
    paint.setColor(reflectionColor);
    canvasReflection.drawPath(pathReflection, paint);
  
    paint.setXfermode(DST_OUT);
    canvasReflection.drawBitmap(generateCircleMaskBitmap(w, h), 0, 0, paint);
    
    return bitmap;
  }
  
  private void loadBitmap() {
    Drawable drawable = this.getDrawable();
    
    if (drawable != null && !isImageSet) {
      if (drawable instanceof BitmapDrawable) {
        BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
        bitmapImage = bitmapDrawable.getBitmap();
      } else if (drawable instanceof RoundedBitmapDrawable) {
        RoundedBitmapDrawable roundedBitmapDrawable = (RoundedBitmapDrawable) drawable;
        bitmapImage = roundedBitmapDrawable.getBitmap();
      }
    }
  }
  
  private void setAttrs() {
    //TODO setAttrs
  }
  
  public void setImage(String imagePath) {
    bitmapImage = BitmapFactory.decodeFile(imagePath);
    isImageSet = true;
  }
  
  public void setBorderColor(int borderColor) {
    this.borderColor = borderColor;
    this.invalidate();
  }
  
  public void setBorderWidthPx(int px) {
    borderWidth = px;
    this.invalidate();
  }
  
  public void setBorderWidthDp(int dp) {
    borderWidth = Calc.dpToPx(context, dp);
    this.invalidate();
  }
  
  /*public void modifyShadow(int radius, int dx, int dy, int color) {
    paintBorder.setShadowLayer(radius, dx, dy, color);
    this.invalidate();
  }*/
}
