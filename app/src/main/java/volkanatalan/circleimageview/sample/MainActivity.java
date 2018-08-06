package volkanatalan.circleimageview.sample;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import java.io.IOException;

import volkanatalan.circleimageview.views.CircleImageView;

public class MainActivity extends AppCompatActivity {
  private CircleImageView circleImageView;
  private int PICK_IMAGE_REQUEST = 1;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    
    int civDiameter = getApplicationContext().getResources().getDimensionPixelSize(R.dimen.civ_diameter);
    int colorBlue = getApplicationContext().getResources().getColor(R.color.blue);
    int colorGreen = getApplicationContext().getResources().getColor(R.color.blue_light);
    int[] colors = {colorBlue, colorGreen, colorBlue};
    float[] positions = {0.2f, 0.5f, 0.8f};
    
    circleImageView = findViewById(R.id.circleImageView);
    circleImageView.setBorderLinearGradient(civDiameter, civDiameter, colors, positions, 45);
    //.setAutoAnimate(false);
    //.setBorderColor(getResources().getColor(R.color.image_border));
    //.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.volkanatalanprofileimage));
    //.setImageResource(R.drawable.volkanatalanprofileimage);
    
  }
  
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    
    if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
      
      Uri uri = data.getData();
      
      try {
        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
  
        circleImageView.setImageBitmap(bitmap);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
  
  public void animate(View view) {
    circleImageView.reflect();
  }
  
  public void chooseImage(View view) {
    Intent intent = new Intent();
// Show only images, no videos or anything else
    intent.setType("image/*");
    intent.setAction(Intent.ACTION_GET_CONTENT);
// Always show the chooser (if there are multiple options available)
    startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
  }
}
