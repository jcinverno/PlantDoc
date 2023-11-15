package com.example.bottomnavigationdemo;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.View;
import android.widget.Toast;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.example.bottomnavigationdemo.ml.TfliteModel;



public class CameraFragment extends Fragment {

    private static final int PERMISSION_CODE = 1000;
    private static final int IMAGE_CAPTURE_CODE = 1001;
    private Uri image_uri;
    private Bitmap bitmap;
    int imageSize = 64;
    ImageView mImageView;
    Button mCaptureBtn;
    Button mPredictBtn;

    String[] classes = {"bacspot",
            "black_measles",
            "black_spot",
            "cedar_rust",
            "commonRust",
            "early_blight",
            "gray_leaf_spot",
            "healthy",
            "isariopsis_leaf_spot",
            "late_blight",
            "leaf_mold",
            "leaf_scorch",
            "mosaic_virus",
            "northern_leaf_blight",
            "powdery_mildew",
            "scab",
            "septoria_leaf_spot",
            "target_spot",
            "two_spotted_spider_mite",
            "yellow_leaf_curl_virus"};

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_camera, container, false);

        mImageView = view.findViewById(R.id.image_view);
        mCaptureBtn = view.findViewById(R.id.capture_image_btn);
        mPredictBtn = view.findViewById(R.id.predict_image_btn);

        mCaptureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
                            PackageManager.PERMISSION_DENIED ||
                            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                                    PackageManager.PERMISSION_DENIED) {
                        String[] permission = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                        requestPermissions(permission, PERMISSION_CODE);
                    } else {
                        openCamera();
                    }
                } else {
                    openCamera();
                }
            }
        });

        mPredictBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    TfliteModel model = TfliteModel.newInstance(requireContext());

                    // Creates inputs for reference.
                    TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 64, 64, 3}, DataType.FLOAT32);
                    ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
                    byteBuffer.order(ByteOrder.nativeOrder());

                    int[] intValues = new int[bitmap.getWidth()*bitmap.getHeight()];
                    bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
                    int pixel = 0;

                    for (int i = 0; i < imageSize; i++) {
                        for (int j = 0; j < imageSize; j++) {

                            if (pixel < intValues.length) {
                                int val = intValues[pixel++];
                                byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 1));
                                byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 1));
                                byteBuffer.putFloat((val & 0xFF) * (1.f / 1));
                            }
                        }
                    }

                    inputFeature0.loadBuffer(byteBuffer);

                    // Runs model inference and gets the result.
                    TfliteModel.Outputs outputs = model.process(inputFeature0);
                    TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

                    // Process the output (you may need to adjust this based on your model's output)
                    prediction(outputFeature0.getFloatArray());

                    // Release model resources.
                    model.close();

                } catch (Exception e) {
                    Toast.makeText(requireContext(), "" + e, Toast.LENGTH_SHORT).show();
                }
            }
        });

        return view;
    }

    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "NewPicture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera");
        image_uri = requireContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent, IMAGE_CAPTURE_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(requireContext(), "Permission Denied...", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IMAGE_CAPTURE_CODE && resultCode == AppCompatActivity.RESULT_OK) {
            try {
                // Set the captured image to the ImageView
                bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), image_uri);
                mImageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void prediction(float[] probabilities) {

        int maxPos = -1;
        float maxProb = 0;

        for (int i = 0; i < probabilities.length; i++) {
            if (probabilities[i] > maxProb) {
                maxProb = probabilities[i];
                maxPos = i;
            }
        }
        Toast.makeText(requireContext(), "Prediction: " + classes[maxPos], Toast.LENGTH_SHORT).show();
    }
}
