package volkanatalan.circleimageview.sample;

import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import volkanatalan.circleimageview.views.CircleImageView;
import volkanatalan.sample.R;

public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
  
    CircleImageView circleImageView = findViewById(R.id.circleImageView);
    //circleImageView.setBorderColor(getResources().getColor(R.color.image_border));
    //circleImageView.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.volkanatalanprofileimage));
    //circleImageView.setImageResource(R.drawable.volkanatalanprofileimage);
  }
}
