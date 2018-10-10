package com.xpn.spellnote.ui.document.edit.imagetextrecognition;

import android.Manifest;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.media.ExifInterface;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.google.android.cameraview.AspectRatio;
import com.google.android.cameraview.CameraView;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.document.FirebaseVisionCloudDocumentRecognizerOptions;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentText;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentTextRecognizer;
import com.xpn.spellnote.R;
import com.xpn.spellnote.databinding.ActivityCameraBinding;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import timber.log.Timber;


public class CameraActivity extends AppCompatActivity implements CameraVM.ViewContract {

    private ActivityCameraBinding binding;
    private CameraVM viewModel;
    private Handler mBackgroundHandler;
    private static final int REQUEST_CAMERA_PERMISSION = 1001;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION:
                if (permissions.length != 1 || grantResults.length != 1) {
                    throw new RuntimeException("Error on requesting camera permission.");
                }
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Timber.d("Permission not granted");
                }
                // No need to start camera here; it is handled by onResume
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_camera);
        viewModel = new CameraVM(this);
        binding.setViewModel(viewModel);
        binding.camera.addCallback(new CameraView.Callback() {

            @Override
            public void onCameraOpened(CameraView cameraView) {
            }

            @Override
            public void onCameraClosed(CameraView cameraView) {
            }

            @Override
            public void onPictureTaken(CameraView cameraView, final byte[] data) {
                ExifInterface exifInterface;
                try {
                    exifInterface = new ExifInterface(new ByteArrayInputStream(data));
                } catch (IOException e) {
                    Toast.makeText(CameraActivity.this, "Couldn\'t display the image", Toast.LENGTH_SHORT).show();
                    Timber.e(e);
                    return;
                }
                int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

                Matrix matrix = new Matrix();
                switch (orientation) {
                    case ExifInterface.ORIENTATION_NORMAL:          break;
                    case ExifInterface.ORIENTATION_ROTATE_180:      matrix.setRotate(180);          break;
                    case ExifInterface.ORIENTATION_ROTATE_90:       matrix.setRotate(90);           break;
                    case ExifInterface.ORIENTATION_ROTATE_270:      matrix.setRotate(-90);          break;
                    case ExifInterface.ORIENTATION_FLIP_HORIZONTAL: matrix.setScale(-1, 1); break;
                    case ExifInterface.ORIENTATION_FLIP_VERTICAL:   matrix.setRotate(180);  matrix.postScale(-1, 1);    break;
                    case ExifInterface.ORIENTATION_TRANSPOSE:       matrix.setRotate(90);   matrix.postScale(-1, 1);    break;
                    case ExifInterface.ORIENTATION_TRANSVERSE:      matrix.setRotate(-90);  matrix.postScale(-1, 1);    break;
                    default:    break;
                }

                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                viewModel.onCaptured(rotatedBitmap);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        viewModel.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            binding.camera.start();
            for(AspectRatio ratio : binding.camera.getSupportedAspectRatios()) {
                Timber.d(ratio.toString());
            }
        }
        else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    protected void onPause() {
        binding.camera.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        viewModel.onDestroy();
        if (mBackgroundHandler != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)    mBackgroundHandler.getLooper().quitSafely();
            else                                                                mBackgroundHandler.getLooper().quit();
            mBackgroundHandler = null;
        }
        super.onDestroy();
    }

    private void processCloudTextRecognitionResult(FirebaseVisionDocumentText text) {
        // Task completed successfully
        if (text == null) {
            Toast.makeText(getApplicationContext(), "No text found", Toast.LENGTH_SHORT).show();
            return;
        }
        binding.graphicOverlay.clear();
        List<FirebaseVisionDocumentText.Block> blocks = text.getBlocks();
        for (int i = 0; i < blocks.size(); i++) {
            List<FirebaseVisionDocumentText.Paragraph> paragraphs = blocks.get(i).getParagraphs();
            for (int j = 0; j < paragraphs.size(); j++) {
                List<FirebaseVisionDocumentText.Word> words = paragraphs.get(j).getWords();
                for (int l = 0; l < words.size(); l++) {
                    CloudTextGraphic cloudDocumentTextGraphic = new CloudTextGraphic(binding.graphicOverlay, words.get(l));
                    binding.graphicOverlay.add(cloudDocumentTextGraphic);
                }
            }
        }
    }

    @Override
    public void onCaptureImage() {
        binding.camera.takePicture();
    }

    @Override
    public void onRecognizeText(Bitmap picture) {
        FirebaseVisionCloudDocumentRecognizerOptions options = new FirebaseVisionCloudDocumentRecognizerOptions.Builder()
                        .setLanguageHints(Arrays.asList("en", "ru"))
                        .build();
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(picture);
        FirebaseVisionDocumentTextRecognizer detector = FirebaseVision.getInstance().getCloudDocumentTextRecognizer(options);

        detector.processImage(image)
                .addOnSuccessListener(this::processCloudTextRecognitionResult)
                .addOnFailureListener(Timber::e);
    }
}
