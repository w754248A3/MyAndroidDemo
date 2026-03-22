package com.cloudstorage.documents;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private TextView sourcePathText;
    private TextView targetPathText;
    private TextView copyStatus;
    private Button btnCopy;

    private Uri sourceUri;
    private Uri targetTreeUri;

    private final ActivityResultLauncher<Intent> selectSourceFileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri selectedUri = result.getData().getData();
                    if (selectedUri != null) {
                        sourceUri = selectedUri;
                        persistUriPermissionIfPossible(selectedUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        sourcePathText.setText("Source File: " + sourceUri);
                        checkReady();
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> selectSourceDirectoryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri selectedUri = result.getData().getData();
                    if (selectedUri != null) {
                        sourceUri = selectedUri;
                        persistUriPermissionIfPossible(selectedUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        sourcePathText.setText("Source Directory: " + sourceUri);
                        checkReady();
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> selectTargetLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    targetTreeUri = result.getData().getData();
                    if (targetTreeUri != null) {
                        persistUriPermissionIfPossible(targetTreeUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        targetPathText.setText("Target: " + targetTreeUri);
                        checkReady();
                    }
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

        findViewById(R.id.btn_select_source_file).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            selectSourceFileLauncher.launch(intent);
        });

        findViewById(R.id.btn_select_source_directory).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            selectSourceDirectoryLauncher.launch(intent);
        });

        findViewById(R.id.btn_select_target).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
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
            String statusMsg = "";
            String copyMethod = "Stream";

            try {
                DocumentFile sourceDocument = getSourceDocument(sourceUri);
                DocumentFile targetDir = DocumentFile.fromTreeUri(this, targetTreeUri);
                if (sourceDocument == null || !sourceDocument.exists()) {
                    throw new FileNotFoundException("Source item not found");
                }
                if (targetDir == null || !targetDir.isDirectory()) {
                    throw new FileNotFoundException("Target directory not found");
                }

                if (sourceDocument.isFile() && canUseNativeCopy(sourceUri, targetTreeUri)) {
                    try {
                        String treeId = DocumentsContract.getTreeDocumentId(targetTreeUri);
                        Uri targetParentUri = DocumentsContract.buildDocumentUriUsingTree(targetTreeUri, treeId);

                        Uri result = DocumentsContract.copyDocument(getContentResolver(), sourceUri, targetParentUri);
                        if (result != null) {
                            success = true;
                            copyMethod = "Native (Optimized/CoW)";
                        }
                    } catch (UnsupportedOperationException e) {
                        // Provider 不支持原生复制，回退到流式传输
                    } catch (Exception e) {
                        // 记录异常并尝试回退
                    }
                }

                if (!success) {
                    copyDocumentRecursively(sourceDocument, targetDir);
                    success = true;
                    copyMethod = sourceDocument.isDirectory() ? "Recursive Stream" : "Standard Stream";
                }

                statusMsg = "Copy Successful! (" + copyMethod + ")";

            } catch (SecurityException e) {
                statusMsg = "Error: Permission denied. " + e.getMessage();
            } catch (FileNotFoundException e) {
                statusMsg = "Error: File or path not found. " + e.getMessage();
            } catch (IOException e) {
                statusMsg = "Error: IO failure. " + e.getMessage();
            } catch (Exception e) {
                statusMsg = "Unexpected Error: " + e;
            }

            final boolean finalSuccess = success;
            final String finalMsg = statusMsg;
            runOnUiThread(() -> {
                btnCopy.setEnabled(true);
                copyStatus.setText(finalMsg);
                if (finalSuccess) {
                    Toast.makeText(this, "Item copied successfully", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private DocumentFile getSourceDocument(Uri uri) {
        if (DocumentsContract.isTreeUri(uri)) {
            return DocumentFile.fromTreeUri(this, uri);
        }
        return DocumentFile.fromSingleUri(this, uri);
    }

    private boolean canUseNativeCopy(Uri sourceUri, Uri targetUri) {
        return sourceUri.getAuthority() != null
                && sourceUri.getAuthority().equals(targetUri.getAuthority())
                && !DocumentsContract.isTreeUri(sourceUri);
    }

    private void copyDocumentRecursively(DocumentFile source, DocumentFile targetDir) throws IOException {
        if (source.isDirectory()) {
            String directoryName = source.getName();
            if (directoryName == null || directoryName.isEmpty()) {
                throw new IOException("Source directory name is missing");
            }

            DocumentFile destinationDir = findFile(targetDir, directoryName);
            if (destinationDir == null) {
                destinationDir = targetDir.createDirectory(directoryName);
            }
            if (destinationDir == null || !destinationDir.isDirectory()) {
                throw new IOException("Could not create target directory: " + directoryName);
            }

            for (DocumentFile child : source.listFiles()) {
                copyDocumentRecursively(child, destinationDir);
            }
            return;
        }

        copySingleFile(source, targetDir);
    }

    private void copySingleFile(DocumentFile sourceFile, DocumentFile targetDir) throws IOException {
        String fileName = sourceFile.getName();
        if (fileName == null || fileName.isEmpty()) {
            fileName = getFileName(sourceFile.getUri());
        }

        String mimeType = sourceFile.getType();
        if (mimeType == null) {
            mimeType = getContentResolver().getType(sourceFile.getUri());
        }
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }

        DocumentFile existingFile = findFile(targetDir, fileName);
        if (existingFile != null && existingFile.exists()) {
            existingFile.delete();
        }

        DocumentFile newFile = targetDir.createFile(mimeType, fileName);
        if (newFile == null) {
            throw new IOException("Could not create target file in the selected directory: " + fileName);
        }

        try (InputStream is = getContentResolver().openInputStream(sourceFile.getUri());
             OutputStream os = getContentResolver().openOutputStream(newFile.getUri())) {
            if (is == null || os == null) {
                throw new IOException("Could not open source or target stream for: " + fileName);
            }
            byte[] buffer = new byte[65536];
            int read;
            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
            os.flush();
        }
    }

    private DocumentFile findFile(DocumentFile directory, String name) {
        for (DocumentFile file : directory.listFiles()) {
            if (name.equals(file.getName())) {
                return file;
            }
        }
        return null;
    }

    private void persistUriPermissionIfPossible(Uri uri, int permissionFlags) {
        try {
            getContentResolver().takePersistableUriPermission(uri, permissionFlags);
        } catch (SecurityException | UnsupportedOperationException ignored) {
            // 某些 provider 不支持持久化授权，忽略即可
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
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
            int cut = result != null ? result.lastIndexOf('/') : -1;
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result != null ? result : "unknown";
    }
}
