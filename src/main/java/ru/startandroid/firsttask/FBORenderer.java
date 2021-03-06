package ru.startandroid.firsttask;

/**
 * Created by Alexander on 07.12.2017.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Path;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES10.GL_UNSIGNED_BYTE;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_LINE_LOOP;
import static android.opengl.GLES20.GL_RGBA;
import static android.opengl.GLES20.GL_TEXTURE1;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TRIANGLES;
import static android.opengl.GLES20.GL_TRIANGLE_FAN;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glLineWidth;
import static android.opengl.GLES20.glUniform4f;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;

public class FBORenderer {

    private static Context context;

    private static int uColorLocation;
    private static int aPositionLocationMask;
    private static int uColorLocationMask;
    private static int uMatrixLocationMask;

    public static int imageHeight;
    public static int imageWidth;

    private static int programIdMask;

    public static float scaling = 1.0f;

    static float deltaX = 0.0f;
    static float deltaY = 0.0f;

    static int fboId;
    static int fboTex;
    static int resId;
    static int[] temp = new int[1];
    static int[] oldFBO = new int[1];
    static int[] oldTex = new int[1];

    public static float[] verticesMask = {
            -0.5f, (float) -Math.sqrt(3)/6,
            0.5f, (float) -Math.sqrt(3)/6,
            0.0f, (float) Math.sqrt(3)/3
            /*-1.0f, 0.0f,
            -0.5f, 0.0f,
            -0.5f, -0.5f,
            -1.0f, -0.5f*/
    };

    public static float[] verticesMaskNew = verticesMask;

    private static float[] mProjectionMatrix = new float[16];
    private static float[] mViewMatrix = new float[16];
    private static float[] mMatrix = new float[16];
    private static float[] mModelMatrix = new float[16];

    private static FloatBuffer vertexDataMask;

    public FBORenderer (Context context) {
        this.context = context;
    }

    public static void fboInit (int width, int height) {
        Log.d("FBO Initialization","We entered fboInit");

        GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING, oldFBO, 0);
        GLES20.glGetIntegerv(GLES20.GL_TEXTURE_BINDING_2D, oldTex, 0);

        GLES20.glGenFramebuffers(1, temp, 0);
        fboId = temp[0];

        GLES20.glGenTextures(1, temp, 0);
        fboTex = temp[0];

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId);
        GLES20.glBindTexture(GL_TEXTURE_2D, fboTex);
        //GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 1);

        GLES20.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, null);

        GLES20.glTexParameteri(GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glTexParameteri(GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        GLES20.glFramebufferTexture2D(GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, fboTex, 0);

        createAndUseProgramMask();
    }

    private static void createAndUseProgramMask() {
        int vertexShaderIdMask = ShaderUtils.createShader(context, GL_VERTEX_SHADER, R.raw.vertex_shader_mask);
        int fragmentShaderIdMask = ShaderUtils.createShader(context, GL_FRAGMENT_SHADER, R.raw.fragment_shader_mask);
        programIdMask = ShaderUtils.createProgram(vertexShaderIdMask, fragmentShaderIdMask);
        glUseProgram(programIdMask);
    }

    private static void getLocationsMask() {
        aPositionLocationMask = glGetAttribLocation(programIdMask, "a_Position");
        //uMatrixLocationMask = glGetUniformLocation(programIdMask, "u_Matrix");
        uColorLocationMask = glGetUniformLocation(programIdMask, "u_Color");
        glUniform4f(uColorLocationMask, 1.0f, 0.0f, 0.0f, 1.0f);
    }

    public static void prepareDataMask() {

        if (MainActivity.resChanged) {
            resId = R.drawable.coco_pills;
        }
        else {
            resId = R.drawable.drones_full;
        }

        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inJustDecodeBounds = true;

        final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resId, options);

        imageHeight = options.outHeight;
        imageWidth = options.outWidth;

        vertexDataMask = ByteBuffer
                .allocateDirect(verticesMask.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexDataMask.put(verticesMask);

    }

    public static void fboDraw() {

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTex);

        if (MainActivity.inActMove) {
            Log.d("In FBO", "Drawing again");
        }
        getLocationsMask();
        prepareDataMask();
        bindDataMask();
        createModelMatrixMask();
        bindMatrixMask();

        glClear(GL_COLOR_BUFFER_BIT);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        //glDrawArrays(GL_TRIANGLE_FAN, 3,4);
    }

    public static void drawLines(boolean isPicked) {
        float alpha = 0.5f;

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, FBORenderer.oldFBO[0]);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTex);

        getLocationsMask();
        prepareDataMask();
        bindDataMask();
        createModelMatrixMask();
        bindMatrixMask();

        if (isPicked) {
            alpha = 1.0f;
        }

        glClear(GL_COLOR_BUFFER_BIT);
        glUniform4f(uColorLocation, 1.0f, 0.0f, 0.0f, alpha);
        glLineWidth(5);
        glDrawArrays(GL_LINE_LOOP, 0, 3);
    }

    private static void bindMatrixMask() {
        //Matrix.multiplyMM(mMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        //Matrix.multiplyMM(mMatrix, 0, mProjectionMatrix, 0, mMatrix, 0);
        //Matrix.multiplyMM(mMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
        glUniformMatrix4fv(uMatrixLocationMask, 1, false, mModelMatrix, 0);
    }

    private static void createModelMatrixMask() {
        //Log.d("Multi", "scaling = " + scaling);
        Matrix.setIdentityM(mModelMatrix, 0);
        Matrix.scaleM(mModelMatrix, 0, scaling, scaling, 1);
        Matrix.translateM(mModelMatrix, 0, deltaX, deltaY, 0);
    }

    public static void setParams(float x, float y, float s) {
        deltaX = x;
        deltaY = y;
        scaling = s;
        /*
        for (int i = 0; i < verticesMask.length/2; i++) {
            verticesMaskNew[2*i] = verticesMask[2*i] + deltaX;
            verticesMaskNew[2*i + 1] = verticesMask[2*i + 1] + deltaY;
        }
        verticesMask = verticesMaskNew;

        verticesMaskNew[0] = (verticesMask[0] + verticesMask[2] + verticesMask[4])/3 + scaling*(2*verticesMask[0] - verticesMask[2] - verticesMask[4])/3;
        verticesMaskNew[2] = (verticesMask[0] + verticesMask[2] + verticesMask[4])/3 + scaling*(2*verticesMask[2] - verticesMask[0] - verticesMask[4])/3;
        verticesMaskNew[4] = (verticesMask[0] + verticesMask[2] + verticesMask[4])/3 + scaling*(2*verticesMask[4] - verticesMask[2] - verticesMask[0])/3;
        verticesMaskNew[1] = (verticesMask[1] + verticesMask[3] + verticesMask[5])/3 + scaling*(2*verticesMask[1] - verticesMask[3] - verticesMask[5])/3;
        verticesMaskNew[3] = (verticesMask[1] + verticesMask[3] + verticesMask[5])/3 + scaling*(2*verticesMask[3] - verticesMask[1] - verticesMask[5])/3;
        verticesMaskNew[5] = (verticesMask[1] + verticesMask[3] + verticesMask[5])/3 + scaling*(2*verticesMask[5] - verticesMask[3] - verticesMask[1])/3;
        */
    }

    private static void bindDataMask() {
        vertexDataMask.position(0);
        glVertexAttribPointer(aPositionLocationMask, 2, GL_FLOAT,
                false, 0, vertexDataMask);
        glEnableVertexAttribArray(aPositionLocationMask);
        glUniform4f(uColorLocationMask, 1.0f, 0.0f, 0.0f, 1.0f);

        glActiveTexture(GL_TEXTURE1);
    }
}
