package volkanatalan.circleimageview.views;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.media.ExifInterface;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.CycleInterpolator;

import java.io.IOException;
import java.io.InputStream;

import volkanatalan.circleimageview.R;
import volkanatalan.circleimageview.models.Calc;

public class CircleImageView extends View {
  // "f" means the variable is a field.
  // "m" means the variable is a field, it has getter-setter methods and an attribute.
  private Context fContext;
  private TypedArray fTypedArray;
  private Drawable drawable;
  private Bitmap fBitmapImage, fBitmapCircleImageAndBorder, fBitmapCircleMask, fBitmapAnimated;
  private Canvas fCanvasAnimated;
  private Paint fPaint, fBorderPaint, fLightPaint;
  private Path fPathReflection = new Path();
  private Handler fHandler;
  private Runnable fRunnable;
  private ValueAnimator fShadowXAnimator, fShadowReverseAnimation, fShadowAlphaAnimator;
  private AnimatorSet fAnimatorSet;
  private PorterDuffXfermode DST_OUT = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);
  private PorterDuffXfermode CLR = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
  int MAX_IMAGE_DIMENSION;
  
  private int fViewWidth, fViewHeight, fDiameter;
  
  private int mShadowXDiff, mShadowYDiff, mShadowSize = 0;
  private int mShadowColor = Color.BLACK, mShadowAlpha = 50;
  private int fShadowCY, fShadowRadius, fShadowAnimatedCX, fShadowAlphaAdd;
  private boolean mShowShadow = true;
  
  private int mBorderColor = Color.BLACK;
  private int fBorderCX, fBorderCY, fBorderRadius, mBorderSize, fBorderDiameter;
  
  private int mReflectionColor = Color.WHITE, mReflectionAlpha = 220;
  private int fReflectionPos, fLightAlpha = 0;
  private boolean mAutoAnimate = true;
  
  private int mLightPassDuration = 700, mAnimationRepeatDelay = 10000;
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
    MAX_IMAGE_DIMENSION = Math.min(fViewWidth, fViewHeight);
    mBorderSize = Calc.dpToPx(fContext, 5);
    mShadowXDiff = Calc.dpToPx(fContext, 5);
    mShadowYDiff = Calc.dpToPx(fContext, 5);
    fContext = null;
    
    getAttrs();
  
    final int animationRepeatDelay = mAnimationRepeatDelay + mLightPassDuration;
  
    fPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    fPaint.setDither(true);
    fPaint.setStyle(Paint.Style.FILL);
  
    fBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    fBorderPaint.setDither(true);
    fBorderPaint.setStyle(Paint.Style.FILL);
    fBorderPaint.setColor(mBorderColor);
  
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
  
    loadBitmap();
  }
  
  
  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
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
      fViewWidth = (int) smallDimension + mBorderSize * 2 + getPaddingLeft() + getPaddingRight();
      fViewHeight = (int) smallDimension + mBorderSize * 2 + getPaddingTop() + getPaddingBottom();
      fViewWidth = fViewWidth > widthSize ? widthSize : fViewWidth;
      fViewHeight = fViewWidth > heightSize ? heightSize : fViewHeight;
    }
  }
  
  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    
    fDiameter = Math.min(w, h);
    int centerX = w / 2;
  
    generateBorderAndImageBitmap();
  
    int shadowCX = fBorderCX + mShadowXDiff;
    fShadowCY = fBorderCY + mShadowYDiff;
    fShadowRadius = fBorderRadius + mShadowSize;
    fShadowAnimatedCX = shadowCX;
    
    if (!isInEditMode()) {
      int shadowAnimationStart;
      int shadowAnimationEnd;
  
      int reflectionWidth = fViewWidth;
      int reflectionHeight = fViewHeight;
      int reflectionPosStart;
      int reflectionPosEnd;
      int lightAlphaAnimationRepeatDelayDuration;
  
      if (mLightDirection == LightDirection.LEFT) {
        reflectionPosStart = fBorderDiameter;
        reflectionPosEnd = -reflectionWidth;
    
        shadowAnimationStart = centerX - (shadowCX - centerX);
        shadowAnimationEnd = shadowCX;
    
        lightAlphaAnimationRepeatDelayDuration = mLightPassDuration - (mLightPassDuration * 40 / 100);
      } else {
        reflectionPosStart = 0 - reflectionWidth;
        reflectionPosEnd = fBorderDiameter;
    
        shadowAnimationStart = shadowCX;
        shadowAnimationEnd = centerX - (shadowCX - centerX);
    
        lightAlphaAnimationRepeatDelayDuration = mLightPassDuration;
      }
  
      fShadowAnimatedCX = shadowAnimationStart;
      fReflectionPos = reflectionPosStart;
      int fCircleMaskRadius = fBorderRadius;
  
      ValueAnimator reflectionXAnimator;
      ValueAnimator lightAlphaAnimator;
  
      fBitmapAnimated = Bitmap.createBitmap(fViewWidth, fViewHeight, Bitmap.Config.ARGB_8888);
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
          fReflectionPos = (int) valueAnimator.getAnimatedValue();
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
      if (mAutoAnimate)
        fHandler.post(fRunnable);
    }
  }
  
  @Override
  protected void onDraw(Canvas canvas) {
    if (fBitmapImage != null) {
      fPaint.setXfermode(null);
      
      // Draw shadow.
      if (mShowShadow) {
        fPaint.setColor(mShadowColor);
        fPaint.setAlpha(mShadowAlpha + fShadowAlphaAdd);
        canvas.drawCircle(fShadowAnimatedCX, fShadowCY, fShadowRadius, fPaint);
        fPaint.setAlpha(255);
      }
  
      // Draw border and image.
      canvas.drawBitmap(fBitmapCircleImageAndBorder, 0, 0, fPaint);
      
      if (!isInEditMode()) {
  
        // Draw reflection.
        if (mAutoAnimate) {
          getAnimatedBitmap();
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
  }
  
  private void generateBorderAndImageBitmap() {
    int paddingLeft = getPaddingLeft();
    int paddingTop = getPaddingTop();
    int paddingRight = getPaddingRight();
    int paddingBottom = getPaddingBottom();
    
    int centerX = fViewWidth / 2;
    int centerY = fViewHeight / 2;
    
    fBorderCX = centerX + paddingLeft - paddingRight;
    fBorderCY = centerY + paddingTop - paddingBottom;
    fBorderDiameter = fDiameter - Math.max(paddingLeft + paddingRight, paddingTop + paddingBottom);
    fBorderRadius = fBorderDiameter / 2;
    
    float imageDiameter = fBorderDiameter - mBorderSize * 2;
    int imageRadius = (int)(imageDiameter / 2);
    float imageWidth = fBitmapImage.getWidth();
    float imageHeight = fBitmapImage.getHeight();
    float smallDimension = Math.min(imageWidth, imageHeight);
    float ratio = imageDiameter / smallDimension;
    int fScaledImageWidth = (int)(imageWidth * ratio);
    int fScaledImageHeight = (int)(imageHeight * ratio);
    int bitmapLeft = (fViewWidth - fScaledImageWidth) / 2;
    int bitmapTop = (fViewHeight - fScaledImageHeight) / 2;
    
    if (fBitmapImage != null) {
      fBitmapImage = Bitmap.createScaledBitmap(
          fBitmapImage, fScaledImageWidth, fScaledImageHeight, false);
    }
    
    
    Bitmap bitmap = Bitmap.createBitmap(fViewWidth, fViewHeight, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    
    fPaint.setXfermode(null);
    fPaint.setAlpha(255);
    
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
      imageCanvas.drawCircle(fBorderCX, fBorderCY, fBorderRadius, fBorderPaint);
    }
    
    // Draw circle image
    imageCanvas.drawBitmap(bitmap, 0, 0, fPaint);
  }
  
  private void getAnimatedBitmap(){
    // Clear canvas
    fPaint.setXfermode(CLR);
    fPaint.setColor(Color.TRANSPARENT);
    fCanvasAnimated.drawPaint(fPaint);
    
    // Draw reflection
    fPaint.setXfermode(null);
    fPaint.setColor(mReflectionColor);
    fCanvasAnimated.drawPath(generateReflectionPath(fViewWidth, fViewHeight), fPaint);
  
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
    canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), fPaint);
  
    fPaint.setXfermode(DST_OUT);
    canvas.drawCircle(cx, cy, radius, fPaint);
    return bitmapCircleMask;
  }
  
  private void loadBitmap() {
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
  
  public void reflect() {
    mAutoAnimate = true;
    fAnimatorSet.start();
  }
  
  private void getAttrs() {
    drawable = fTypedArray.getDrawable(R.styleable.CircleImageView_image);
    
    mShowShadow = fTypedArray.getBoolean(R.styleable.CircleImageView_showShadow, mShowShadow);
    mShadowColor = fTypedArray.getColor(R.styleable.CircleImageView_shadowColor, mShadowColor);
    mShadowXDiff = fTypedArray.getDimensionPixelSize(R.styleable.CircleImageView_shadowXDiff, mShadowXDiff);
    mShadowYDiff = fTypedArray.getDimensionPixelSize(R.styleable.CircleImageView_shadowYDiff, mShadowYDiff);
    mShadowSize = fTypedArray.getDimensionPixelSize(R.styleable.CircleImageView_shadowSize, mShadowSize);
    mShadowAlpha = fTypedArray.getInt(R.styleable.CircleImageView_shadowAlpha, mShadowAlpha);
  
    mBorderColor = fTypedArray.getColor(R.styleable.CircleImageView_borderColor, mBorderColor);
    mBorderSize = fTypedArray.getDimensionPixelSize(R.styleable.CircleImageView_borderSize, mBorderSize);
  
    mAutoAnimate = fTypedArray.getBoolean(R.styleable.CircleImageView_autoAnimate, mAutoAnimate);
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
  
  public String getRealPathFromURI(Context context, Uri contentURI) {
    String path= contentURI.getPath();
    try {
      Cursor cursor = context.getContentResolver().query(contentURI,
          null, null, null, null);
      cursor.moveToFirst();
      String document_id = cursor.getString(0);
      document_id = document_id.substring(document_id.lastIndexOf(":") + 1);
      cursor.close();
      
      cursor = context.getContentResolver().query(
          android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
          null, MediaStore.Images.Media._ID + " = ? ", new String[]{document_id}, null);
      cursor.moveToFirst();
      path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
      cursor.close();
    }
    catch(Exception e)
    {
      return path;
    }
    return path;
  }
  
  public int getOrientation(Context context, Uri uri) {
    ExifInterface exif;
    try {
      exif = new ExifInterface(getRealPathFromURI(context, uri));
    return exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
    
    } catch (IOException e) {
      e.printStackTrace();
      return -1;
    }
  }
  
  public Bitmap getCorrectlyOrientedImage(Context context, Uri uri, int maxImageDimension) throws IOException {
    InputStream is = context.getContentResolver().openInputStream(uri);
    BitmapFactory.Options dbo = new BitmapFactory.Options();
    dbo.inJustDecodeBounds = true;
    BitmapFactory.decodeStream(is, null, dbo);
    is.close();
    
    int rotatedWidth, rotatedHeight, rotation;
    int orientation = getOrientation(context, uri);
    Log.d("orientation", orientation + "");
  
    if (orientation == 6) {
      rotation = 90;
    }
    else if (orientation == 3) {
      rotation = 180;
    }
    else if (orientation == 8) {
      rotation = 270;
    }
    else rotation = 0;
    
    if (rotation == 90 || rotation == 270) {
      rotatedWidth = dbo.outHeight;
      rotatedHeight = dbo.outWidth;
    } else {
      rotatedWidth = dbo.outWidth;
      rotatedHeight = dbo.outHeight;
    }
    
    Bitmap srcBitmap;
    is = context.getContentResolver().openInputStream(uri);
    if (rotatedWidth > maxImageDimension || rotatedHeight > maxImageDimension) {
      float widthRatio = ((float) rotatedWidth) / ((float) maxImageDimension);
      float heightRatio = ((float) rotatedHeight) / ((float) maxImageDimension);
      float maxRatio = Math.max(widthRatio, heightRatio);
      
      // Create the bitmap from file
      BitmapFactory.Options options = new BitmapFactory.Options();
      options.inSampleSize = (int) maxRatio;
      srcBitmap = BitmapFactory.decodeStream(is, null, options);
    } else {
      srcBitmap = BitmapFactory.decodeStream(is);
    }
    is.close();
    
    /*
     * if the orientation is not 0 (or -1, which means we don't know), we
     * have to do a rotation.
     */
    if (rotation > 0) {
      Matrix matrix = new Matrix();
      matrix.postRotate(rotation);
      
      srcBitmap = Bitmap.createBitmap(srcBitmap, 0, 0, srcBitmap.getWidth(),
          srcBitmap.getHeight(), matrix, true);
    }
    
    return srcBitmap;
  }
  
  public void setImageUri(Context context, Uri uri) {
    try {
      fBitmapImage = getCorrectlyOrientedImage(context, uri, MAX_IMAGE_DIMENSION);
    } catch (IOException e) {
      e.printStackTrace();
    }
    generateBorderAndImageBitmap();
    invalidate();
  }
  
  public void setImageBitmap(Bitmap bm) {
    fBitmapImage = bm;
    generateBorderAndImageBitmap();
    invalidate();
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
  
  public CircleImageView setBorderLinearGradient(int w, int h, int[] colors, float[] positions, float angle) {
    int x0, x1, y0, y1;
    float centerY = h / 2;
    
    if (angle > 0 && angle <= 45) {
      x0 = w; x1 = 0;
      y0 = (int)(centerY - (h / 90 * angle));
      y1 = (int)(centerY + (h / 90 * angle));
      
    } else if (angle > 45 && angle <= 135) {
      angle = angle - 45;
      y0 = 0; y1 = h;
      x0 = (int)(w - (w / 90 * angle));
      x1 = (int)(w / 90 * angle);
  
    } else if (angle > 135 && angle <= 225) {
      angle = angle - 135;
      x0 = 0; x1 = w;
      y0 = (int)(h / 90 * angle);
      y1 = (int)(h - (h / 90 * angle));
      
    } else if (angle > 225 && angle <= 315) {
      angle = angle - 225;
      y0 = h; y1 = 0;
      x0 = (int)(w / 90 * angle);
      x1 = (int)(w - (w / 90 * angle));
      
    } else {
      angle = angle - 315;
      x0 = w; x1 = 0;
      y0 = (int)(h - (h / 90 * angle));
      y1 = (int)(h / 90 * angle);
      
    }
    
    LinearGradient linearGradient = new LinearGradient(
        x0, y0, x1, y1, colors, positions, Shader.TileMode.CLAMP);
    fBorderPaint.setShader(linearGradient);
    requestLayout();
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
  
  public int getBorderDiameter() {
    return fBorderDiameter;
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
  
  public boolean isAutoAnimate() {
    return mAutoAnimate;
  }
  
  public CircleImageView setAutoAnimate(boolean autoAnimate) {
    mAutoAnimate = autoAnimate;
    requestLayout();
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
