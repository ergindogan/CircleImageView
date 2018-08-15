# CircleImageView
![CircleImageView sample 1](https://github.com/thehorsebuyer/CircleImageView/blob/master/images/CircleImageView_sample1.gif)
![CircleImageView sample 2](https://github.com/thehorsebuyer/CircleImageView/blob/master/images/CircleImageView_sample2.gif)

CircleImageView is a good-looking view for android projects. It allows you to show your images with a circle shape. It also has a reflection animation which makes user to take attention to itself.

## Difference
Differences of CircleImageView from other circle image views are those:
##### 1- It has a reflection animation.
CircleImageView has a reflection animation. As you can play the animation automatically, you can also play it any time you want. Only thing you need to do is to call `reflect()` method when you want CircleImageView to reflect. When CircleImageView reflects, its shadow also moves and becomes darker. When reflection ends, shadow comes to its real position back and it gets lighter. You can also change the direction of the reflection to `right` or `left`. To do that write `app:lightDirection="Right"` to your xml file or call `setLightDirection(CircleImageView.LightDirection.RIGHT)` method in java file. Light moves to left as default.

##### 2- You can add gradient to its border easily.
You can adjust the border thickness of CircleImageView and add gradient to it easily. To adjust the thickness write `app:borderSize="5dp"` in xml file or call `setBorderSize(int borderSize)` method in java file. To set gradient to border call `setBorderLinearGradient(int w, int h, int[] colors, float[] positions, float angle)` method in java file.

## Usage
Add the JitPack repository to your build file
```
allprojects {
   repositories {
      ...
      maven { url 'https://jitpack.io' }
   }
}
```

<br>

Add the dependency
```
dependencies {
    implementation 'com.github.thehorsebuyer:CircleImageView:v1.0'
}
```

### XML
```XML
<volkanatalan.circleimageview.views.CircleImageView
        android:id="@+id/circleImageView"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:padding="10dp"
        app:image="@drawable/image"
        app:borderSize="3dp"
        app:borderColor="@color/blue"
        app:lightDirection="Right"
        app:shadowXDiff="3dp"
        app:shadowYDiff="3dp"/>
```

### Java
```Java
CircleImageView circleImageView = findViewById(R.id.circleImageView);
circleImageView.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.image));
```

### Attributes
```
showShadow (boolean)
shadowColor (color)
shadowXDiff (dimension)
shadowYDiff (dimension)
shadowSize (dimension)
shadowAlpha (integer) (0-255)
borderColor (color)
borderSize (dimension)
autoAnimate (boolean)
reflectionColor (color)
reflectionAlpha (integer) (0-255)
lightPassDuration (integer) (milliseconds)
animationRepeatDelay (integer) (milliseconds)
minLightAlpha (integer) (0-255)
maxLightAlpha (integer) (0-255)
minShadowAlpha (integer) (0-255)
maxShadowAlpha (integer) (0-255)
shadowReverseAnimationDuration (integer) (milliseconds)
shadowReverseAnimationDelay (integer) (milliseconds)
lightDirection (enum) (Left, Right)
```

### Setters
```
setImageUri(Context context, Uri uri)
setImageBitmap(Bitmap bm)
setShadowXDiff(int shadowXDiff)
setShadowYDiff(int shadowYDiff)
setShadowSize(int shadowSize)
setShadowColor(int shadowColor)
setShadowAlpha(int shadowAlpha)
setShowShadow(boolean showShadow)
setBorderLinearGradient(int w, int h, int[] colors, float[] positions, float angle)
setBorderColor(int borderColor)
setBorderSize(int borderSize)
setReflectionColor(int reflectionColor)
setReflectionAlpha(int reflectionAlpha)
setAutoAnimate(boolean autoAnimate)
setLightPassDuration(int lightPassDuration)
setAnimationRepeatDelay(int animationRepeatDelay)
setMinShadowAlpha(int minShadowAlpha)
setMaxShadowAlpha(int maxShadowAlpha)
setShadowReverseAnimationDelay(int shadowReverseAnimationDelay)
setShadowReverseAnimationDuration(int shadowReverseAnimationDuration)
setMinLightAlpha(int minLightAlpha)
setMaxLightAlpha(int maxLightAlpha)
setLightDirection(LightDirection lightDirection)
```

### Getters
```
int getShadowXDiff()
int getShadowYDiff()
int getShadowSize()
int getShadowColor()
int getShadowAlpha()
boolean isShowShadow()
int getBorderColor()
int getBorderSize()
int getBorderDiameter()
int getReflectionColor()
int getReflectionAlpha()
boolean isAutoAnimate()
int getLightPassDuration()
int getAnimationRepeatDelay()
int getMinShadowAlpha()
int getMaxShadowAlpha()
int getShadowReverseAnimationDelay()
int getShadowReverseAnimationDuration()
int getMinLightAlpha()
int getMaxLightAlpha()
LightDirection getLightDirection()
```
