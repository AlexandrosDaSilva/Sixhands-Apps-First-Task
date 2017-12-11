package ru.startandroid.firsttask;

/**
 * Created by Alexander on 07.12.2017.
 */

import android.content.Context;
import android.graphics.Path;
import android.opengl.GLES20;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES10.GL_UNSIGNED_BYTE;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
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

    private static int programIdMask;

    static int fboId;
    static int fboTex;
    static int[] temp = new int[1];
    static int[] oldFBO = new int[1];
    static int[] oldTex = new int[1];

    public static float[] verticesMask = {
            0.0f, 0.0f,
            0.0f, 0.5f,
            1.0f, 0.0f,
            -1.0f, 0.0f,
            -0.5f, 0.0f,
            -0.5f, -0.5f,
            -1.0f, -0.5f
    };

    private static float[] mProjectionMatrix = new float[16];
    private static float[] mViewMatrix = new float[16];
    private static float[] mMatrix = new float[16];

    private static FloatBuffer vertexDataMask;

    public FBORenderer (Context context) {
        this.context = context;
    }

    public static void fboInit (int width, int height) {
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
        getLocationsMask();
        prepareDataMask();
        bindDataMask();

        glClear(GL_COLOR_BUFFER_BIT);
        glDrawArrays(GL_TRIANGLES, 0, 3);
        glDrawArrays(GL_TRIANGLE_FAN, 3,4);
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

    private static void prepareDataMask() {
        float[] verticesMaskNew = new float[verticesMask.length];

        for (int i = 0; i < verticesMask.length/2; i++) {
            //verticesMask[2*i + 1] = - verticesMask[2*i + 1];
            verticesMaskNew[2*i + 1] = - verticesMask[2*i + 1]*OpenGLRenderer.glViewHeight/OpenGLRenderer.glViewWidth;
            verticesMaskNew[2*i] = verticesMask[2*i]*OpenGLRenderer.glViewWidth/OpenGLRenderer.glViewHeight;
        }

        verticesMask = verticesMaskNew;

        vertexDataMask = ByteBuffer
                .allocateDirect(verticesMask.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexDataMask.put(verticesMask);
    }

    private static void bindDataMask() {
        vertexDataMask.position(0);
        glVertexAttribPointer(aPositionLocationMask, 2, GL_FLOAT,
                false, 0, vertexDataMask);
        glEnableVertexAttribArray(aPositionLocationMask);
        glUniform4f(uColorLocationMask, 1.0f, 0.0f, 0.0f, 1.0f);

        glActiveTexture(GL_TEXTURE1);
    }
/*
    private static void createProjectionMatrixMask(int width, int height) {
        float ratio = 1;
        float left = -1;
        float right = 1;
        float bottom = -1;
        float top = 1;
        float near = 2;
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

    private static void createViewMatrixMask() {
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

    private static void bindMatrixMask() {
        Matrix.multiplyMM(mMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
        glUniformMatrix4fv(uMatrixLocationMask, 1, false, mMatrix, 0);
    }
*/
}
