package volkanatalan.circleimageview.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;

import volkanatalan.library.Calc;

public class CircleImageView extends AppCompatImageView {
  Context context;
  private Bitmap bitmap;
  private Paint paintImage, paintBorder;
  private BitmapShader bitmapShader;
  private int imageWidth, imageHeight, borderWidth, diameter;
  private float shadowRadius = 4f, shadowDX = 2f, shadowDY = 2f;
  private int shadowColor = Color.BLACK;
  private boolean isImageSet = false;
  
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
    
    paintImage = new Paint();
    paintImage.setAntiAlias(true);
    
    paintBorder = new Paint();
    paintBorder.setColor(Color.BLACK);
    paintBorder.setAntiAlias(true);
    this.setLayerType(LAYER_TYPE_SOFTWARE, paintBorder);
    paintBorder.setShadowLayer(shadowRadius, shadowDX, shadowDY, shadowColor);
  }
  
  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int width = measureDimensions(widthMeasureSpec, heightMeasureSpec);
    int height = measureDimensions(heightMeasureSpec, widthMeasureSpec);
    diameter = Math.min(width, height);
    
    imageWidth = width - (borderWidth * 2);
    imageHeight = height - (borderWidth * 2);
    
    setMeasuredDimension(width, height);
  }
  
  private int measureDimensions(int requiredMeasureSpec, int otherMeasureSpec) {
    int requiredMeasureSpecMode = MeasureSpec.getMode(requiredMeasureSpec);
    int requiredMeasureSpecSize = MeasureSpec.getSize(requiredMeasureSpec);
  
    int otherMeasureSpecMode = MeasureSpec.getMode(otherMeasureSpec);
    int otherMeasureSpecSize = MeasureSpec.getSize(otherMeasureSpec);
  
    if (otherMeasureSpecMode == MeasureSpec.EXACTLY &&
            requiredMeasureSpecMode == MeasureSpec.AT_MOST) {
      return otherMeasureSpecSize;
    } else if (requiredMeasureSpecMode == MeasureSpec.EXACTLY) {
      return requiredMeasureSpecSize;
    } else if (otherMeasureSpecMode == MeasureSpec.AT_MOST && requiredMeasureSpecMode == MeasureSpec.AT_MOST) {
      return Math.min(requiredMeasureSpecSize, otherMeasureSpecSize);
    } else {
      return requiredMeasureSpecSize;
    }
  }
  
  @SuppressLint("DrawAllocation")
  @Override
  protected void onDraw(Canvas canvas) {
    loadBitmap();
    
    if (bitmap != null) {
      int centerX = imageWidth / 2;
      int centerY = imageHeight / 2;
      int bitmapWidth = bitmap.getWidth();
      int bitmapHeight = bitmap.getHeight();
      
      bitmapShader = new BitmapShader(Bitmap.createBitmap(bitmap), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
      paintImage.setShader(bitmapShader);
      
      int radius = Math.min(centerX, centerY);
      canvas.drawCircle(centerX + borderWidth, centerY + borderWidth, radius + borderWidth - shadowRadius, paintBorder);
      canvas.drawCircle(centerX + borderWidth, centerY + borderWidth, radius - shadowRadius, paintImage);
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
