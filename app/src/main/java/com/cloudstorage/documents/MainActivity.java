package com.cloudstorage.documents;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private TextView sourcePathText;
    private TextView targetPathText;
    private TextView copyStatus;
    private Button btnCopy;

    private Uri sourceUri;
    private Uri targetTreeUri;

    private final ActivityResultLauncher<Intent> selectSourceLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    sourceUri = result.getData().getData();
                    sourcePathText.setText("Source: " + sourceUri.toString());
                    checkReady();
                }
            }
    );

    private final ActivityResultLauncher<Intent> selectTargetLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    targetTreeUri = result.getData().getData();
                    // Persist access
                    getContentResolver().takePersistableUriPermission(targetTreeUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    targetPathText.setText("Target: " + targetTreeUri.toString());
                    checkReady();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sourcePathText = findViewById(R.id.source_path_text);
        targetPathText = findViewById(R.id.target_path_text);
        copyStatus = findViewById(R.id.copy_status);
        btnCopy = findViewById(R.id.btn_copy);

        findViewById(R.id.btn_select_source).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            selectSourceLauncher.launch(intent);
        });

        findViewById(R.id.btn_select_target).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            selectTargetLauncher.launch(intent);
        });

        btnCopy.setOnClickListener(v -> executeCopy());
    }

    private void checkReady() {
        btnCopy.setEnabled(sourceUri != null && targetTreeUri != null);
    }

    private void executeCopy() {
        if (sourceUri == null || targetTreeUri == null) return;

        btnCopy.setEnabled(false);
        copyStatus.setText("Copying...");

        new Thread(() -> {
            boolean success = false;
            String errorMessage = "";
            try {
                String fileName = getFileName(sourceUri);
                DocumentFile targetDir = DocumentFile.fromTreeUri(this, targetTreeUri);
                
                // Get mime type
                String mimeType = getContentResolver().getType(sourceUri);
                if (mimeType == null) mimeType = "application/octet-stream";

                DocumentFile newFile = targetDir.createFile(mimeType, fileName);
                if (newFile != null) {
                    try (InputStream is = getContentResolver().openInputStream(sourceUri);
                         OutputStream os = getContentResolver().openOutputStream(newFile.getUri())) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = is.read(buffer)) != -1) {
                            os.write(buffer, 0, read);
                        }
                        success = true;
                    }
                } else {
                    errorMessage = "Could not create target file";
                }
            } catch (Exception e) {
                errorMessage = e.getMessage();
            }

            final boolean finalSuccess = success;
            final String finalError = errorMessage;
            runOnUiThread(() -> {
                btnCopy.setEnabled(true);
                if (finalSuccess) {
                    copyStatus.setText("Copy Successful!");
                    Toast.makeText(this, "File copied successfully", Toast.LENGTH_SHORT).show();
                } else {
                    copyStatus.setText("Error: " + finalError);
                }
            });
        }).start();
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
}
