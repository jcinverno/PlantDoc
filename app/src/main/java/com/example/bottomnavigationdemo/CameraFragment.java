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

import android.widget.LinearLayout;
import android.util.TypedValue;

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
import android.widget.TextView;

import com.example.bottomnavigationdemo.ml.TfliteModel;



public class CameraFragment extends Fragment {

    private static final int IMAGE_PERMISSION_CODE = 1000;
    private static final int IMAGE_CAPTURE_CODE = 1001;
    private static final int GALLERY_PERMISSION_CODE = 2;
    private Uri image_uri;
    private Bitmap bitmap;
    int imageSize = 64;
    ImageView mImageView;
    Button mCaptureBtn;
    Button mGalleryBtn;
    TextView predictionTextView;

    String[] classes = {"Bacterial Spot",
            "Black measles",
            "Black Rot",
            "Cedar Rust",
            "Common Rust",
            "Early Blight",
            "Gray Leaf Spot",
            "Healthy",
            "Isariopsis Leaf Spot",
            "Late Blight",
            "Leaf Mold",
            "Leaf Scorch",
            "Mosaic Virus",
            "Northern Leaf Blight",
            "Powdery Mildew",
            "Scab",
            "Septoria Leaf Spot",
            "Target Spot",
            "Two Spotted Spider Mite",
            "Yellow Leaf Curl Virus"};

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_camera, container, false);

        mImageView = view.findViewById(R.id.image_view);
        mCaptureBtn = view.findViewById(R.id.capture_image_btn);
        mGalleryBtn = view.findViewById(R.id.gallery_image_btn);

        predictionTextView = view.findViewById(R.id.plant_text);

        mCaptureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
                            PackageManager.PERMISSION_DENIED ||
                            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                                    PackageManager.PERMISSION_DENIED) {
                        String[] permission = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                        requestPermissions(permission, IMAGE_PERMISSION_CODE);
                    } else {
                        openCamera();
                    }
                } else {
                    openCamera();
                }
            }
        });

        mGalleryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
                            PackageManager.PERMISSION_DENIED ||
                            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                                    PackageManager.PERMISSION_DENIED ||
                            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) ==
                                    PackageManager.PERMISSION_DENIED) {
                        String[] permission = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
                        requestPermissions(permission, GALLERY_PERMISSION_CODE);
                    } else {
                        openGallery();
                    }
                } else {
                    openGallery();
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

    private void openGallery() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Gallery");

        image_uri = requireContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent i = new Intent();
        i.setType("image/*");
        i.setAction(Intent.ACTION_GET_CONTENT);

        startActivityForResult(Intent.createChooser(i, "Select Picture"), GALLERY_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case IMAGE_CAPTURE_CODE:
                handleCameraPermissionResult(grantResults);
                break;
            case GALLERY_PERMISSION_CODE:
                handleGalleryPermissionResult(grantResults);
                break;
        }
    }

    private void handleCameraPermissionResult(int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            Toast.makeText(requireContext(), "Camera Permission Denied...", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleGalleryPermissionResult(int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            Toast.makeText(requireContext(), "Gallery Permission Denied...", Toast.LENGTH_SHORT).show();
        }
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IMAGE_CAPTURE_CODE && resultCode == AppCompatActivity.RESULT_OK) {
            try {
                bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), image_uri);
                mImageView.setImageBitmap(bitmap);
                makePrediction();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if (requestCode == GALLERY_PERMISSION_CODE && resultCode == AppCompatActivity.RESULT_OK) {
            try {
                image_uri = data.getData();
                bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), image_uri);
                mImageView.setImageBitmap(bitmap);
                makePrediction();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void makePrediction(){
        try {
            TfliteModel model = TfliteModel.newInstance(requireContext());

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 64, 64, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            Bitmap reducedBitmap = getResizedBitmap(bitmap, imageSize);

            int[] intValues = new int[reducedBitmap.getWidth()*reducedBitmap.getHeight()];
            reducedBitmap.getPixels(intValues, 0, reducedBitmap.getWidth(), 0, 0, reducedBitmap.getWidth(), reducedBitmap.getHeight());
            int pixel = 0;

            for (int i = 0; i < imageSize; i++) {
                for (int j = 0; j < imageSize; j++) {
                    if (pixel < intValues.length) {
                        int val = intValues[pixel++];
                        byteBuffer.putFloat((val & 0xFF) * (1.f / 1)); // Blue component
                        byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 1)); // Green component
                        byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 1)); // Red component
                    }
                }
            }

            inputFeature0.loadBuffer(byteBuffer);

            TfliteModel.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            prediction(outputFeature0.getFloatArray());

            model.close();

        } catch (Exception e) {
            Toast.makeText(requireContext(), "" + e, Toast.LENGTH_SHORT).show();
        }
    }

    public Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float)width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
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

        mCaptureBtn.setVisibility(View.INVISIBLE);
        mGalleryBtn.setVisibility(View.INVISIBLE);

        String prediction = "Prediction: " + classes[maxPos];

        predictionTextView.setText(prediction);
        predictionTextView.setTextSize(30);
        predictionTextView.setVisibility(View.VISIBLE);
    }
}
