package ru.startandroid.firsttask;

import android.Manifest;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Path;
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
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.graphics.BitmapFactory.decodeFile;
import static android.provider.MediaStore.Images.Media.getBitmap;

public class MainActivity extends Activity {
    //implements OnTouchListener
    //OpenGLSurfaceView glSurfaceView;
    GLSurfaceView glSurfaceView;
    static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 0;
    public static final int PICK_IMAGE = 1;

    OpenGLRenderer openGLRenderer;

    float touchX;
    float touchY;

    float multiX;
    float multiY;
    float multiXend;
    float multiYend;

    float deltaX;
    float deltaY;

    float scaleBeg = 1;
    float scaleEnd = 1;
    float scale;

    float[] beg;

    boolean inTriangle = false;
    boolean inSquare = false;
    boolean isInside = false;

    public static boolean inActMove = false;

    Button buttonLoad;
    Button buttonMode;
    Button buttonSave;
    ImageView imageView;
    public Bitmap loadedPic;
    public static String loadedPicPath;
    public static int isClicked = 0;
    public static int resId;
    public static int imageHeight;
    public static int imageWidth;
    public static int scrHeight;
    public static int scrWidth;
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
        glSurfaceView = new GLSurfaceView(this);
        glSurfaceView = findViewById(R.id.glView);
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setPreserveEGLContextOnPause(true);
        glSurfaceView.setRenderer(openGLRenderer);

        //Toast.makeText(this, "Toast is here!", Toast.LENGTH_LONG).show();

        buttonLoad = findViewById(R.id.buttonLoad);
        buttonMode = findViewById(R.id.buttonMode);
        buttonSave = findViewById(R.id.buttonSave);

        //imageView = findViewById(R.id.imageView);

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
                            intent.setAction(Intent.ACTION_PICK);
                            intent.setType("image/*");
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
                        /*if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                        }
                        else {
                            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "savedBitmap.png");
                            try {
                                FileOutputStream fos = null;
                                try {
                                    fos = new FileOutputStream(file);
                                    loadedPic.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                                } finally {
                                    if (fos != null) fos.close();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        break;*/
                }
            }
        };

        View.OnTouchListener otView = new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                touchX = event.getX();
                touchY = event.getY();

                getScreenSize(MainActivity.this);

                if (resChanged) {
                    resId = R.drawable.coco_pills;
                }
                else {
                    resId = R.drawable.drones_full;
                }

                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inScaled = false;
                options.inJustDecodeBounds = true;

                final Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId, options);

                imageHeight = options.outHeight;
                imageWidth = options.outWidth;

                //Log.d("Image metrics (coord)", imageWidth + ", " + imageHeight);
                Log.d("Touch coordinates", touchX + ", " + touchY);

                //imageWidth = loadedPic.getWidth();
                //imageHeight = loadedPic.getHeight();

                float[] x = new float[FBORenderer.verticesMask.length];
                float[] y = new float[FBORenderer.verticesMask.length];
                float[] sides = new float[FBORenderer.verticesMask.length/2];

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // нажатие
                        multiX = touchX;
                        multiY = touchY;

                        for (int i = 0; i < FBORenderer.verticesMask.length/2; i++) {
                            float[] temp = OpenGLRenderer.coordChange(FBORenderer.verticesMask[2*i], FBORenderer.verticesMask[2*i + 1]);
                            x[i] = temp[0];
                            y[i] = temp[1];
                            Log.d("Triangle coordinates", x[i] + ", " + y[i]);
                        }

                        sides[0] = (x[0] - touchX) * (y[1] - y[0]) - (x[1] - x[0]) * (y[0] - touchY);
                        sides[1] = (x[1] - touchX) * (y[2] - y[1]) - (x[2] - x[1]) * (y[1] - touchY);
                        sides[2] = (x[2] - touchX) * (y[0] - y[2]) - (x[0] - x[2]) * (y[2] - touchY);

                        Log.d("Check sides", sides[0] + ", " + sides[1] + ", " + sides[2]);

                        //Toast.makeText(MainActivity.this, "Down: " + touchX + "," + touchY, Toast.LENGTH_SHORT).show();

                        // если касание было начато в пределах треугольника
                        if (((sides[0] * sides[1] >= 0) && (sides[0] * sides[2] >= 0) && (sides[1] * sides[2] >= 0))) {
                        // включаем режим перетаскивания
                            Log.d("Touch", "in the triangle!");
                            Toast.makeText(MainActivity.this, "Right into the triangle!", Toast.LENGTH_SHORT).show();
                            inTriangle = true;
                            //isClicked = (isClicked + 1) % 2;
                            beg = OpenGLRenderer.coordRevChange(touchX, touchY);
                        }

                        // если касание было начато в пределах квадрата
                        if (((x[3]) * (imageWidth / 2) + (scrWidth)/2 < touchX) && (((x[5]) * (imageWidth / 2) + (scrWidth)/2 > touchX)) && ((y[3]) * (imageHeight / 2) - 68 + (scrHeight)/2 < touchY) && ((y[5]) * (imageHeight / 2) - 68 + (scrHeight)/2 > touchY)){
                            Toast.makeText(MainActivity.this, "Right into the square!", Toast.LENGTH_SHORT).show();
                            inSquare = true;
                            //deltaX = touchX - x[3];
                            //deltaY = touchY - y[3];
                        }

                    /*case MotionEvent.ACTION_POINTER_DOWN:
                        Toast.makeText(MainActivity.this, "Second finger!", Toast.LENGTH_SHORT).show();
                        //scaleBeg = (float) Math.sqrt(Math.pow((event.getX() - multiX), 2) + Math.pow((event.getY() - multiY), 2));*/

                        break;
                    case MotionEvent.ACTION_MOVE:
                        // движение
                        //Toast.makeText(MainActivity.this, "Move: " + touchX + "," + touchY, Toast.LENGTH_SHORT).show();
                        if (inTriangle) {
                            inActMove = true;
                            //Toast.makeText(MainActivity.this, "Into the triangle!", Toast.LENGTH_SHORT).show();
                            Log.d("We made it", "into the triangle");
                            float[] temp1 = OpenGLRenderer.coordRevChange(touchX, touchY);
                            deltaX = temp1[0] - beg[0];
                            deltaY = temp1[1] - beg[1];
                            FBORenderer.setParams(deltaX, deltaY);
                            glSurfaceView.requestRender();
                        }
                        if (inSquare) {

                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        // отпускание

                        multiXend = touchX;
                        multiYend = touchY;

                        if (inTriangle) {
                            Log.d("In ActionUp", "Drawing again");

                        }
                        if (inSquare) Toast.makeText(MainActivity.this, "Moved...", Toast.LENGTH_SHORT).show();
                        inTriangle = false;
                        inSquare = false;
                        //glSurfaceView.requestRender();
                    /*case MotionEvent.ACTION_POINTER_UP:
                        //scaleEnd = (float) Math.sqrt(Math.pow((event.getX() - multiXend), 2) + Math.pow((event.getY() - multiYend), 2));
                        //scale = scaleEnd/scaleBeg;
                        //scale = 0.5f;
                        FBORenderer.scaling = scale;
                        glSurfaceView.requestRender();*/
                        break;

                    case MotionEvent.ACTION_CANCEL:
                        // отмена
                        break;
                }
                v.performClick();
                return true;
            }
        };

        buttonSave.setOnClickListener(oclBtn);
        buttonMode.setOnClickListener(oclBtn);
        buttonLoad.setOnClickListener(oclBtn);
        glSurfaceView.setOnTouchListener(otView);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //loadedPic = null;

        if (requestCode == PICK_IMAGE) {
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    Uri imageUri = data.getData();
                    try {
                        loadedPic = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                        openGLRenderer.setBitmap(loadedPic);
                        Toast.makeText(MainActivity.this, "Pic taken!", Toast.LENGTH_SHORT).show();
                        Log.d("We got there", "We took bitmap");
                        //glSurfaceView.requestRender();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //loadedPicPath = getRealPathFromURI(this, data.getData());
                    //loadedPic = decodeFile(loadedPicPath);
                    //loadedPic = getBitmap(this.getContentResolver(), imageUri);

                    //imageView.setImageBitmap(loadedPic);
                }
            }
            //loadedPicPath = getRealPathFromURI(this, data.getData());
            //loadedPic = decodeFile(loadedPicPath);
        }

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

    public void getScreenSize(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        scrHeight = metrics.heightPixels;
        scrWidth = metrics.widthPixels;
    }

    /*public String getRealPathFromURI(Context context, Uri contentUri) {
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
    }*/

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