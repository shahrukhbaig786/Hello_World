package com.example.intel.helloworld;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nbsp.materialfilepicker.MaterialFilePicker;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    ImageView ivAttachment;
    Button bUpload;
    String content_type=null;
    TextView tvFileName;
    private ProgressDialog dialog;
    private String selectedFilePath;
    private static final String TAG = MainActivity.class.getSimpleName();
    private String SERVER_URL = "http://82.145.38.202/sachin/pannel/api/user_doc.php?uid=27&id_proof=VOTERID";
    private static final int PICK_FILE_REQUEST = 1;
    String PathIS = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        ivAttachment = (ImageView) findViewById(R.id.ivAttachment);
        bUpload = (Button) findViewById(R.id.b_upload1);
        tvFileName = (TextView) findViewById(R.id.tv_file_name);
/*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
                return;
            }


        }
*/
        ivAttachment.setOnClickListener(this);
        bUpload.setOnClickListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {


        if (requestCode == 100 && (grantResults[0]) == PackageManager.PERMISSION_GRANTED) {
            /*ivAttachment.setOnClickListener(this);
            bUpload.setOnClickListener(this);*/
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
            }

        }
    }


    private void showFileChooser() {
        try {
            new MaterialFilePicker().withActivity(MainActivity.this).
                    withRequestCode(PICK_FILE_REQUEST)
                    .withFilterDirectories(true)

                    .start();
            // String pattern = ".*is.*";
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PICK_FILE_REQUEST) {

                if (data == null) {
                    //no data present


                    return;
                }
                File f = new File(data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH));
                String filePath = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
                content_type=getMimiType(f.getPath());
                //Both filepath and f will give same result
                // Toast.makeText(this, "File Path 0 " +  f.getPath() +"/n /n File Path 122 "+filePath, Toast.LENGTH_LONG).show();

                System.out.println("File Path is " + filePath);
                Toast.makeText(this, "File Path is " + filePath, Toast.LENGTH_LONG).show();
                selectedFilePath = filePath;
                if (selectedFilePath != null && !selectedFilePath.equals("")) {
                    tvFileName.setText(selectedFilePath);
                } else {

                    Toast.makeText(this, "Cannot upload file to server", Toast.LENGTH_SHORT).show();
                }

            }

        }

    }

    private String getMimiType(String path) {
    String extension= MimeTypeMap.getFileExtensionFromUrl(path);
        System.out.println("Extension is "+extension);

        String n=MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        System.out.println("Finally Extension is  "+n);
        return n;

    }


    @Override
    public void onClick(View view) {

          String content_img1="image/png",content_img2="image/jpg",content_img3="image/jpeg";
          String content_doc1="application/pdf",content_doc2="application/msword",
                  content_doc3="application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (view.getId() == R.id.ivAttachment) {

            //on attachment icon click
            showFileChooser();

        }
        if (view.getId() == R.id.b_upload1) {
            if (content_type!=null)
            {
                 if (content_type.equals(content_doc1)||content_type.equals(content_doc2)||content_type.equals(content_doc3)
                         ||content_type.equals(content_img1)||content_type.equals(content_img2)||content_type.equals(content_img3)
                         )
                 {
                     if (selectedFilePath != null) {
                         dialog = ProgressDialog.show(MainActivity.this, "", "Uploading File...", true);

                         new Thread(new Runnable() {
                             @Override
                             public void run() {
                                 //creating new thread to handle Http Operations
                                 uploadFile(selectedFilePath);
                             }
                         }).start();

                     }

                 }
                else
                 {
                     Toast.makeText(this, "Select PDF, DOC or IMAGE ",Toast.LENGTH_LONG).show();

                 }




            }
            Toast.makeText(this, "Select PDF, DOC or IMAGE ",Toast.LENGTH_LONG).show();


        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();


    }


    //android upload file to server
    public int uploadFile(final String selectedFilePath) {
        int serverResponseCode = 0;
        HttpURLConnection connection;
        DataOutputStream dataOutputStream;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        File selectedFile = new File(selectedFilePath);


        String[] parts = selectedFilePath.split("/");
        final String fileName = parts[parts.length - 1];
        final String fileName1 = fileName;
        if (!selectedFile.isFile()) {
            dialog.dismiss();


            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvFileName.setText("Source File Doesn't Exist: " + selectedFilePath);
                }
            });
            return 0;
        } else {
            try {
                FileInputStream fileInputStream = new FileInputStream(selectedFile);
                URL url = new URL(SERVER_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);//Allow Inputs
                connection.setDoOutput(true);//Allow Outputs
                connection.setUseCaches(false);//Don't use a cached Copy
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.setRequestProperty("ENCTYPE", "multipart/form-data");
                connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                connection.setRequestProperty("uploaded_file", selectedFilePath);

                //creating new dataoutputstream
                dataOutputStream = new DataOutputStream(connection.getOutputStream());

                //writing bytes to data outputstream
                dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd);
                dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
                        + selectedFilePath + "\"" + lineEnd);

                dataOutputStream.writeBytes(lineEnd);

                //returns no. of bytes present in fileInputStream
                bytesAvailable = fileInputStream.available();
                //selecting the buffer size as minimum of available bytes or 1 MB
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                //setting the buffer as byte array of size of bufferSize
                buffer = new byte[bufferSize];

                //reads bytes from FileInputStream(from 0th index of buffer to buffersize)
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                //loop repeats till bytesRead = -1, i.e., no bytes are left to read
                while (bytesRead > 0) {
                    //write the bytes read from inputstream
                    dataOutputStream.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }

                dataOutputStream.writeBytes(lineEnd);
                dataOutputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                serverResponseCode = connection.getResponseCode();
                String serverResponseMessage = connection.getResponseMessage();

                Log.i(TAG, "Server Response is: " + serverResponseMessage + ": " + serverResponseCode);

                //response code of 200 indicates the server status OK
                if (serverResponseCode == 200) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvFileName.setText("File Upload completed.\n\n You can see the uploaded file here: \n\n" + "http://coderefer.com/extras/uploads/" + fileName1);
                        }
                    });
                }

                //closing the input and output streams
                fileInputStream.close();
                dataOutputStream.flush();
                dataOutputStream.close();


            } catch (FileNotFoundException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "File Not Found", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (MalformedURLException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "URL error!", Toast.LENGTH_SHORT).show();

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Cannot Read/Write File!", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
            return serverResponseCode;
        }

    }
}
