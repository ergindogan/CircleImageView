package volkanatalan.circleimageview.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
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
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

import volkanatalan.library.Calc;

public class CircleImageView extends AppCompatImageView {
  Context context;
  private Bitmap bitmap, bitmapReflection;
  private Path pathReflection;
  private Paint paintImage, paintBorder, paintReflection, paintReflectionCircle;
  private BitmapShader bitmapShader;
  private float imageWidth, imageHeight, borderWidth, diameter, center;
  private float shadowRadius = 4f, shadowDX = 2f, shadowDY = 2f;
  private int animationDuration = 500;
  private int animationRepeatDelay = 5000;
  private int reflectionLeft = 0;
  private int shadowColor = Color.BLACK;
  private boolean isImageSet = false;
  private Handler handler;
  private Runnable runnable;
  
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
    
    paintImage = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintImage.setDither(true);
    
    paintBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintBorder.setDither(true);
    paintBorder.setColor(Color.BLACK);
    this.setLayerType(LAYER_TYPE_SOFTWARE, paintBorder);
    paintBorder.setShadowLayer(shadowRadius, shadowDX, shadowDY, shadowColor);
  
    paintReflection = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintReflection.setDither(true);
    paintReflection.setColor(Color.WHITE);
  
    paintReflectionCircle = new Paint(Paint.ANTI_ALIAS_FLAG);
    paintReflectionCircle.setDither(true);
    paintReflectionCircle.setColor(Color.WHITE);
    paintReflectionCircle.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
  
    handler = new Handler();
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
    
    bitmapReflection = Bitmap.createBitmap(w / 2, h, Bitmap.Config.ALPHA_8);
    Canvas canvasReflection = new Canvas(bitmapReflection);
    
    pathReflection = new Path();
    pathReflection.moveTo(0, h);
    pathReflection.lineTo(bitmapReflection.getWidth() - bitmapReflection.getWidth()/2, 0);
    pathReflection.lineTo(bitmapReflection.getWidth(), 0);
    pathReflection.lineTo(bitmapReflection.getWidth() / 2, h);
    pathReflection.close();
  
    canvasReflection.drawCircle(center, center, center, paintReflectionCircle);
    canvasReflection.drawPath(pathReflection, paintReflection);
  
    final ValueAnimator reflectionXAnimator = ValueAnimator.ofInt((int) imageWidth, 0 - bitmapReflection.getWidth());
    reflectionXAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      @Override
      public void onAnimationUpdate(ValueAnimator valueAnimator) {
        reflectionLeft = (int) valueAnimator.getAnimatedValue();
        invalidate();
      }
    });
    reflectionXAnimator.setDuration(animationDuration);

    // Define the code block to be executed
    runnable = new Runnable() {
      @Override
      public void run() {
        // Insert custom code here
        reflectionXAnimator.start();
        // Repeat every 2 seconds
        handler.postDelayed(runnable, animationRepeatDelay);
      }
    };

    // Start the Runnable immediately
    handler.post(runnable);
  }
  
  @SuppressLint("DrawAllocation")
  @Override
  protected void onDraw(Canvas canvas) {
    loadBitmap();
    
    if (bitmap != null) {
      float bitmapWidth = bitmap.getWidth();
      float bitmapHeight = bitmap.getHeight();
      float rateOfMin = diameter / Math.min(bitmapHeight, bitmapWidth);
      float scaledBitmapWidth = rateOfMin * bitmapWidth;
      float scaledBitmapHeight = rateOfMin * bitmapHeight;
      
      bitmap = Bitmap.createScaledBitmap(
          bitmap, (int) scaledBitmapWidth, (int) scaledBitmapHeight, false);
      bitmapShader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
      paintImage.setShader(bitmapShader);
  
      float radius = center;
      canvas.drawCircle(center + borderWidth, center + borderWidth, radius + borderWidth - shadowRadius, paintBorder);
      canvas.drawCircle(center + borderWidth, center + borderWidth, radius - shadowRadius, paintImage);
      canvas.drawBitmap(bitmapReflection, reflectionLeft, 0, paintReflectionCircle);
    }
  }
  
  private void loadBitmap() {
    Drawable drawable = this.getDrawable();
    
    if (drawable != null && !isImageSet) {
      if (drawable instanceof BitmapDrawable) {
        BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
        bitmap = bitmapDrawable.getBitmap();
      } else if (drawable instanceof RoundedBitmapDrawable) {
        RoundedBitmapDrawable roundedBitmapDrawable = (RoundedBitmapDrawable) drawable;
        bitmap = roundedBitmapDrawable.getBitmap();
      }
    }
  }
  
  private void setAttrs() {
    //TODO setAttrs
  }
  
  public void setImage(String imagePath) {
    bitmap = BitmapFactory.decodeFile(imagePath);
    isImageSet = true;
  }
  
  public void setBorderColor(int borderColor) {
    if (paintBorder != null)
      paintBorder.setColor(borderColor);
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
  
  public void modifyShadow(int radius, int dx, int dy, int color) {
    paintBorder.setShadowLayer(radius, dx, dy, color);
    this.invalidate();
  }
}
