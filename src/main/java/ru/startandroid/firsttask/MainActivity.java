package ru.startandroid.firsttask;

import android.Manifest;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.graphics.BitmapFactory.decodeFile;
import static android.provider.MediaStore.Images.Media.getBitmap;

public class MainActivity extends Activity implements View.OnTouchListener {

    GLSurfaceView glSurfaceView;
    static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 0;
    public static final int PICK_IMAGE = 1;

    OpenGLRenderer openGLRenderer;

    float touchX;
    float touchY;

    Button buttonLoad;
    Button buttonMode;
    Button buttonSave;
    ImageView imageView;
    public Bitmap loadedPic;
    public static String loadedPicPath;
    public static int isClicked = 0;
    public static boolean resChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!supportES2()) {
            Toast.makeText(this, "OpenGl ES 2.0 is not supported", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        openGLRenderer = new OpenGLRenderer(this);
        setContentView(R.layout.activity_main);
        glSurfaceView = findViewById(R.id.glView);
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setPreserveEGLContextOnPause(true);
        glSurfaceView.setRenderer(openGLRenderer);

        buttonLoad = findViewById(R.id.buttonLoad);
        buttonMode = findViewById(R.id.buttonMode);
        buttonSave = findViewById(R.id.buttonSave);

        View.OnClickListener oclBtn = new View.OnClickListener() {
            @Override
            public void onClick( View v) {
                switch (v.getId()) {
                    case R.id.buttonLoad:
                        //Load image from gallery
                        /*if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                        }
                        else {
                            Intent intent = new Intent();
                            intent.setType("image/*");
                            intent.setAction(Intent.ACTION_PICK);
                            startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
                        }*/

                        resChanged = !resChanged;
                        glSurfaceView.requestRender();
                        break;
                    case R.id.buttonMode:
                        //Make changes
                        isClicked = (isClicked + 1) % 2;
                        glSurfaceView.requestRender();
                        break;
                    case R.id.buttonSave:
                        //Save image to gallery
                        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "savedBitmap.png");
                        try {
                            FileOutputStream fos = null;
                            try {
                                fos = new FileOutputStream(file);
                                //loadedPic.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                            } finally {
                                if (fos != null) fos.close();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;
                }
            }
        };

        buttonSave.setOnClickListener(oclBtn);
        buttonMode.setOnClickListener(oclBtn);
        buttonLoad.setOnClickListener(oclBtn);
        glSurfaceView.setOnTouchListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == PICK_IMAGE) {
            if (resultCode == RESULT_OK) {
                Uri imageUri = data.getData();
                loadedPicPath = getRealPathFromURI(this, data.getData());
                loadedPic = decodeFile(loadedPicPath);
                //loadedPic = getBitmap(this.getContentResolver(), imageUri);
                //openGLRenderer.setBitmap(loadedPic);
            }
            //loadedPicPath = getRealPathFromURI(this, data.getData());
            //loadedPic = decodeFile(loadedPicPath);
        }

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        touchX = event.getX();
        touchY = event.getY();
        boolean drag = false;
        boolean isInside = false;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // нажатие
                float[] x = new float[FBORenderer.verticesMask.length];
                float[] y = new float[FBORenderer.verticesMask.length];
                for (int i = 0; i < FBORenderer.verticesMask.length; i++) {
                    x[i] = FBORenderer.verticesMask[2*i];
                    y[i] = FBORenderer.verticesMask[2*i + 1];
                }
                float[] sides = new float[FBORenderer.verticesMask.length/2];


                sides[0] = (x[0] - touchX) * (y[1] - y[0]) - (x[1] - x[0]) * (y[0] - touchY);
                sides[1] = (x[1] - touchX) * (y[2] - y[1]) - (x[2] - x[1]) * (y[1] - touchY);
                sides[2] = (x[2] - touchX) * (y[0] - y[2]) - (x[0] - x[2]) * (y[2] - touchY);

                Toast.makeText(MainActivity.this, "Down: " + touchX + "," + touchY, Toast.LENGTH_LONG).show();

                // если касание было начато в пределах квадрата
                //if (((sides[0] * sides[1] >= 0) && (sides[0] * sides[2] >= 0) && (sides[1] * sides[2] >= 0))) {
                // включаем режим перетаскивания
                //    drag = true;
                //    isClicked = (isClicked + 1) % 2;
                //    glSurfaceView.requestRender();
                //}
                break;
            case MotionEvent.ACTION_MOVE:
                // движение
                Toast.makeText(MainActivity.this, "Move: " + touchX + "," + touchY, Toast.LENGTH_LONG).show();
                break;
            case MotionEvent.ACTION_UP:
                // отпускание
                glSurfaceView.performClick();
                Toast.makeText(MainActivity.this, "Up: " + touchX + "," + touchY, Toast.LENGTH_LONG).show();
            case MotionEvent.ACTION_CANCEL:
                // отмена
                break;
        }
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        glSurfaceView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        glSurfaceView.onResume();
    }

    private boolean supportES2() {
        ActivityManager activityManager =
                (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        return (configurationInfo.reqGlEsVersion >= 0x20000);
    }

    public String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /*@Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //getImage();
                }
            }
            return;
        }
    }*/
}