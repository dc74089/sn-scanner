package com.vegetarianbaconite.snscanner.macbook;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.android.BeepManager;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;
import com.vegetarianbaconite.snscanner.R;

import java.util.Collection;
import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    DecoratedBarcodeView barcodeView;
    RecyclerView recyclerView;
    Button copy, clear, undo;

    Adapter adapter;
    BeepManager beepManager;
    ClipboardManager clipboardManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        barcodeView = findViewById(R.id.barcodeScanner);
        recyclerView = findViewById(R.id.mainRecycler);
        copy = findViewById(R.id.copy);
        clear = findViewById(R.id.clear);
        undo = findViewById(R.id.undo);

        Collection<BarcodeFormat> formats = Collections.singletonList(BarcodeFormat.CODE_39);
        barcodeView.getBarcodeView().setDecoderFactory(new DefaultDecoderFactory(formats));
        barcodeView.setStatusText("Scan Serial Number from MacBook Box");
        barcodeView.initializeFromIntent(getIntent());
        barcodeView.decodeContinuous(this::barcodeResult);

        adapter = new Adapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        copy.setOnClickListener(this::doCopy);
        clear.setOnClickListener(this::doClear);
        undo.setOnClickListener(this::doUndo);

        beepManager = new BeepManager(this);
        beepManager.setVibrateEnabled(true);

        clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    }

    private void doCopy(View view) {
        ClipData clip = ClipData.newPlainText("Serials", adapter.getSerialsAsText());
        clipboardManager.setPrimaryClip(clip);
        Toast.makeText(this, "Copied all SN's to clipboard", Toast.LENGTH_SHORT).show();
    }

    private void doClear(View view) {
        runOnUiThread(() -> adapter.clear());
    }

    private void doUndo(View view) {
        runOnUiThread(() -> adapter.undo());
    }

    public void barcodeResult(BarcodeResult result) {
        String text = result.getText();
        String serial = text.substring(1);

        if (text.startsWith("S") && serial.length() == 12 && !adapter.contains(serial)) {
            runOnUiThread(() -> adapter.insert(serial));
            beepManager.playBeepSoundAndVibrate();
        } else {
            Log.v("MainActivity", "Wrong code: " + text);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        barcodeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        barcodeView.pause();
    }

    public void pause(View view) {
        barcodeView.pause();
    }

    public void resume(View view) {
        barcodeView.resume();
    }

    public void triggerScan(View view) {
        barcodeView.decodeSingle(this::barcodeResult);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return barcodeView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }
}
