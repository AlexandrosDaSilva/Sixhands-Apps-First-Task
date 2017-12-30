package ru.startandroid.firsttask;

/**
 * Created by Alexander on 29.11.2017.
 */

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_DEPTH_BUFFER_BIT;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_LINE_LOOP;
import static android.opengl.GLES20.GL_TEXTURE;
import static android.opengl.GLES20.GL_TEXTURE1;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform4f;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import static android.opengl.GLES20.GL_DEPTH_TEST;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;


import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;

import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glUniform1i;

public class OpenGLRenderer implements GLSurfaceView.Renderer {

    private final static int POSITION_COUNT = 3;
    private static final int TEXTURE_COUNT = 2;
    private static final int STRIDE = (POSITION_COUNT
            + TEXTURE_COUNT) * 4;

    private static Context context;

    private FloatBuffer vertexData;

    //public static int scrHeight;
    //public static int scrWidth;
    public static float glViewHeight = 18.6f;
    public static float glViewWidth = 12;
    public static int imageHeight;
    public static int imageWidth;

    public static int bmWidth;
    public static int bmHeight;

    public static float[] vertices;

    private int aPositionLocation;
    private int aTextureLocation;
    private int uTextureUnitLocation;
    private int uMatrixLocation;
    private int uMaskLocation;
    private int aIsClicked;

    private int programId;

    public static Bitmap bitmap;

    private float scaling;

    private float[] mProjectionMatrix = new float[16];
    private float[] mViewMatrix = new float[16];
    private float[] mModelMatrix = new float[16];
    private float[] mMatrix = new float[16];

    public static int resId;

    private int texture;

    private FBORenderer fboRenderer;

    public OpenGLRenderer(Context context) {
        this.context = context;
    }

    @Override
    public void onSurfaceCreated(GL10 arg0, EGLConfig arg1) {
        glClearColor(0f, 0f, 0f, 1f);
        glEnable(GL_DEPTH_TEST);
    }

    @Override
    public void onSurfaceChanged(GL10 arg0, int width, int height) {
        bmHeight = height;
        bmWidth = width;

        glViewport(0, 0, width, height);

        fboRenderer = new FBORenderer(context);
        FBORenderer.fboInit(bmWidth, bmHeight);
    }

    private void createAndUseProgram() {
        int vertexShaderId = ShaderUtils.createShader(context, GL_VERTEX_SHADER, R.raw.vertex_shader);
        int fragmentShaderId = ShaderUtils.createShader(context, GL_FRAGMENT_SHADER, R.raw.fragment_shader);
        programId = ShaderUtils.createProgram(vertexShaderId, fragmentShaderId);
        glUseProgram(programId);
    }

    private void getLocations() {
        aPositionLocation = glGetAttribLocation(programId, "a_Position");
        aTextureLocation = glGetAttribLocation(programId, "a_Texture");
        uTextureUnitLocation = glGetUniformLocation(programId, "u_TextureUnit");
        uMatrixLocation = glGetUniformLocation(programId, "u_Matrix");
        uMaskLocation = glGetUniformLocation(programId, "u_Mask");
        aIsClicked = glGetUniformLocation(programId, "a_isClicked");
    }

    private void prepareData() {

        //getScreenSize(context);

        if (MainActivity.resChanged) {
            resId = R.drawable.coco_pills;
        }
        else {
            resId = R.drawable.drones_full;
        }

        float[] vertices = getVertices(resId);
        /*for (int i = 0; i < 3; i++) {
            vertices[vertices.length - 1 + i] = verticesMask[i];
        }*/

        vertexData = ByteBuffer
                .allocateDirect(vertices.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexData.put(vertices);

        texture = TextureUtils.loadTexture(context, resId);
        //texture = TextureUtils.loadTexture(context, bitmap);

    }

    private void bindData() {
        // координаты вершин
        vertexData.position(0);
        glVertexAttribPointer(aPositionLocation, POSITION_COUNT, GL_FLOAT,
                false, STRIDE, vertexData);
        glEnableVertexAttribArray(aPositionLocation);

        // координаты текстур
        vertexData.position(POSITION_COUNT);
        glVertexAttribPointer(aTextureLocation, TEXTURE_COUNT, GL_FLOAT,
                false, STRIDE, vertexData);
        glEnableVertexAttribArray(aTextureLocation);

        glUniform1i(aIsClicked, MainActivity.isClicked);

        //юнит маски
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, FBORenderer.fboTex);
        glUniform1i(uMaskLocation, 1);

        // помещаем текстуру в target 2D юнита 0
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, texture);

        // юнит текстуры
        glUniform1i(uTextureUnitLocation, FBORenderer.oldTex[0]);
    }

    private void createProjectionMatrix(int width, int height) {
        float ratio = 1;
        float left = -1;
        float right = 1;
        float bottom = -1;
        float top = 1;
        float near = 1;
        float far = 12;
        if (width > height) {
            ratio = (float) width / height;
            left *= ratio;
            right *= ratio;
        } else {
            ratio = (float) height / width;
            bottom *= ratio;
            top *= ratio;
        }

        Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
    }

    private void createViewMatrix() {
        // точка положения камеры
        float eyeX = 0;
        float eyeY = 0;
        float eyeZ = 7;

        // точка направления камеры
        float centerX = 0;
        float centerY = 0;
        float centerZ = 0;

        // up-вектор
        float upX = 0;
        float upY = 1;
        float upZ = 0;

        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
    }

    private void createModelMatrix() {
        scaling = 1.0f;
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.scaleM(mModelMatrix, 0, scaling, scaling, 1);
    }

    private void bindMatrix() {
        Matrix.multiplyMM(mMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        Matrix.multiplyMM(mMatrix, 0, mProjectionMatrix, 0, mMatrix, 0);
        //Matrix.multiplyMM(mMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
        glUniformMatrix4fv(uMatrixLocation, 1, false, mMatrix, 0);
    }

    @Override
    public void onDrawFrame(GL10 arg0) {
        //if (bitmap != null) {

            //Log.d("Bitmap", "isn't null!");

            //bmWidth = bitmap.getWidth();
            //bmHeight = bitmap.getHeight();

            FBORenderer.fboDraw();

            FBORenderer.drawLines(MainActivity.inTriangle);

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, FBORenderer.oldFBO[0]);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, FBORenderer.oldTex[0]);

            Log.d("Drew", "Triangles");
            createAndUseProgram();
            getLocations();
            prepareData();
            bindData();
            createViewMatrix();
            //createProjectionMatrix(bitmap.getWidth(), bitmap.getHeight());
            createProjectionMatrix(bmWidth, bmHeight);
            createModelMatrix();
            bindMatrix();

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        /*}
        else {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        }*/
    }

    public static float[] getVertices(int resourceId) {

        float[] vertices = {
                -glViewWidth / 2.0f,  glViewHeight / 2.0f, 1,   0, 0,
                -glViewWidth / 2.0f, -glViewHeight / 2.0f, 1,   0, 1,
                glViewWidth / 2.0f,  glViewHeight / 2.0f, 1,   1, 0,
                glViewWidth / 2.0f, -glViewHeight / 2.0f, 1,   1, 1,
        };

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inJustDecodeBounds = true;

        final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);

        imageHeight = options.outHeight;
        imageWidth = options.outWidth;

        //imageWidth = bitmap.getWidth();
        //imageHeight = bitmap.getHeight();

        if (imageHeight/imageWidth >= glViewHeight/glViewWidth) {
            vertices[0] = -(imageWidth * glViewHeight / imageHeight) / 2.0f;
            vertices[5] = -(imageWidth * glViewHeight / imageHeight) / 2.0f;
            vertices[10] = (imageWidth * glViewHeight / imageHeight) / 2.0f;
            vertices[15] = (imageWidth * glViewHeight / imageHeight) / 2.0f;
        }
        else {
            vertices[1] = (imageHeight * glViewWidth / imageWidth) / 2.0f;
            vertices[6] = -(imageHeight * glViewWidth / imageWidth) / 2.0f;
            vertices[11] = (imageHeight * glViewWidth / imageWidth) / 2.0f;
            vertices[16] = -(imageHeight * glViewWidth / imageWidth) / 2.0f;
        }

        return vertices;
    }

    public static float[] coordChange(float x, float y) {
        //Log.d("Current coordinates", x + ", " + y);
        float[] newXY = new float[2];
        newXY[0] = MainActivity.scrWidth/2 + (imageWidth/2)*x;
        newXY[1] = MainActivity.scrHeight/2 - 19 + (imageHeight/2)*y;
        //Log.d("Changed coordinates", newXY[0] + ", " + newXY[1]);
        return newXY;
    }

    public static float[] coordRevChange(float x, float y) {
        float[] newXY = new float[2];
        newXY[0] = (x - MainActivity.scrWidth/2) / (imageWidth/2);
        newXY[1] = (y - MainActivity.scrHeight/2 + 19) / (imageHeight/2);
        return newXY;
    }

    public void setBitmap(Bitmap bitmapImage) {
        this.bitmap = bitmapImage;
    }
}