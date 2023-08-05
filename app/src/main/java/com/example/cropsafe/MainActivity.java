package com.example.cropsafe;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.cropsafe.ml.Potatoandtomatomodel;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

	Button selectBtn, predictBtn, captureBtn;
	ImageView imageView;
	TextView result;
	Bitmap bitmap;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Permission for camera
		getPermission();

		// Load labels for Potatoandtomatomodel
		String[] labels = loadLabels("labels.txt");

		selectBtn = findViewById(R.id.selectBtn);
		captureBtn = findViewById(R.id.captureBtn);
		predictBtn = findViewById(R.id.predictBtn);
		result = findViewById(R.id.resView);
		imageView = findViewById(R.id.imageView);

		// Set up the selectBtn and captureBtn click listeners
		selectBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_GET_CONTENT);
				intent.setType("image/*");
				startActivityForResult(intent, 10);
			}
		});

		captureBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				startActivityForResult(intent, 12);
			}
		});

		// Set up the predictBtn click listener for Potatoandtomatomodel
		predictBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					Potatoandtomatomodel model = Potatoandtomatomodel.newInstance(MainActivity.this);

					int batchSize = 1;
					int imageHeight = 224;
					int imageWidth = 224;
					int channels = 3;
					TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{batchSize, imageHeight, imageWidth, channels}, DataType.FLOAT32);

					if (bitmap == null) {
						Toast.makeText(MainActivity.this, "Please select or capture an image.", Toast.LENGTH_SHORT).show();
						return;
					}

					bitmap = Bitmap.createScaledBitmap(bitmap, imageWidth, imageHeight, true);
					TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
					tensorImage.load(bitmap);
					inputFeature0.loadBuffer(tensorImage.getBuffer());

					Potatoandtomatomodel.Outputs outputs = model.process(inputFeature0);
					TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

					int maxIndex = getMax(outputFeature0.getFloatArray());

					// Display result
					String resultText = "Prediction: " + labels[maxIndex];
					result.setText(resultText);

					// Close the model
					model.close();
				} catch (IOException e) {
					Toast.makeText(MainActivity.this, "An error occurred during prediction.", Toast.LENGTH_SHORT).show();
					e.printStackTrace();
				}
			}
		});
	}

	private String[] loadLabels(String filename) {
		try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(getAssets().open(filename)))) {
			String line;
			int numLabels = 38;
			String[] labels = new String[numLabels];
			int cnt = 0;
			while ((line = bufferedReader.readLine()) != null) {
				labels[cnt] = line;
				cnt++;
			}
			return labels;
		} catch (IOException e) {
			e.printStackTrace();
			return new String[0];
		}
	}

	int getMax(float[] arr) {
		int max = 0;
		for (int i = 0; i < arr.length; i++) {
			if (arr[i] > arr[max]) max = i;
		}
		return max;
	}

	void getPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
				ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 11);
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if (requestCode == 1) {
			if (grantResults.length > 0) {
				if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
					this.getPermission();
				}
			}
		}
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		if (requestCode == 10) {
			if (data != null) {
				Uri uri = data.getData();
				try {
					bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
					imageView.setImageBitmap(bitmap);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		} else if (requestCode == 12) {
			bitmap = (Bitmap) data.getExtras().get("data");
			imageView.setImageBitmap(bitmap);
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
}
