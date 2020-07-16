package com.vegetarianbaconite.snscanner.ipad;

import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.zxing.client.android.BeepManager;
import com.vegetarianbaconite.snscanner.R;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity implements ImageAnalysis.Analyzer {

    TextView serialTV, barcodeTV;
    RecyclerView recycler;
    Adapter adapter;
    Button saveButton;

    String serial, tag;

    Preview preview;
    Camera camera;
    PreviewView pv;

    BeepManager beepManager;

    ExecutorService cameraExecutor;
    ImageAnalysis analysis;
    TextRecognizer textRecognizer;
    BarcodeScanner barcodeScanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_ipad);

        cameraExecutor = Executors.newSingleThreadExecutor();

        serialTV = findViewById(R.id.serial);
        barcodeTV = findViewById(R.id.tag);
        recycler = findViewById(R.id.mainRecycler);
        saveButton = findViewById(R.id.save);
        pv = findViewById(R.id.previewView);

        adapter = new Adapter();

        beepManager = new BeepManager(this);
        beepManager.setVibrateEnabled(true);

        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build();

        textRecognizer = TextRecognition.getClient();
        barcodeScanner = BarcodeScanning.getClient(options);

        ListenableFuture<ProcessCameraProvider> cameraProvider = ProcessCameraProvider.getInstance(getApplicationContext());

        cameraProvider.addListener(() -> {
            try {
                ProcessCameraProvider processCameraProvider = cameraProvider.get();
                processCameraProvider.unbindAll();

                CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

                preview = new Preview.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                        .build();

                analysis = new ImageAnalysis.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                analysis.setAnalyzer(cameraExecutor, this);

                camera = processCameraProvider.bindToLifecycle(this, cameraSelector, analysis, preview);

                if (preview != null) {
                    preview.setSurfaceProvider(pv.createSurfaceProvider());
                }
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));

    }

    @Override
    @ExperimentalGetImage
    public void analyze(@NonNull ImageProxy imageProxy) {
        //Log.d("Analyze", "Boop");
        AtomicBoolean textDone = new AtomicBoolean(false);
        AtomicBoolean barcodeDone = new AtomicBoolean(false);
        Image mediaImage = imageProxy.getImage();

        if (mediaImage != null) {
            //Log.d("Analyze", "Beep");
            InputImage inputImage =
                    InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

            barcodeScanner.process(inputImage)
                    .addOnSuccessListener((barcodes) -> {
                        for (Barcode b : barcodes) {
                            beepManager.playBeepSoundAndVibrate();
                            Log.d("Analyze", b.getDisplayValue() + ", " + b.getRawValue());
                        }

                        if (textDone.get())
                            imageProxy.close();
                        else barcodeDone.set(true);
                    })
                    .addOnFailureListener((ex) -> {
                        Log.w("Analyze", "Barcode Error", ex);
                        if (textDone.get())
                            imageProxy.close();
                        else barcodeDone.set(true);
                    });

            textRecognizer.process(inputImage)
                    .addOnSuccessListener((text) -> {
                        //Log.d("Analyze", "Bop " + text.getText());

                        for (Text.TextBlock block : text.getTextBlocks()) {
                            for (Text.Line line : block.getLines()) {
                                for (Text.Element element : line.getElements()) {
                                    String elementText = element.getText();
                                    if (SerialRecognizer.contains(elementText)) {
                                        beepManager.playBeepSoundAndVibrate();
                                        serial = elementText;
                                        runOnUiThread(() -> serialTV.setText(elementText));
                                        Log.i("Analyze", "Success: " + elementText);
                                    }
                                }
                            }
                        }

                        if (barcodeDone.get())
                            imageProxy.close();
                        else textDone.set(true);
                    })
                    .addOnFailureListener((ex) -> {
                        Log.w("Analyze", "OCR Error", ex);

                        if (barcodeDone.get())
                            imageProxy.close();
                        else textDone.set(true);
                    });
        }
    }
}