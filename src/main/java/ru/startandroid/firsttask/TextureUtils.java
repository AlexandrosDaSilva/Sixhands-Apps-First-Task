package ru.startandroid.firsttask;

/**
 * Created by Alexander on 30.11.2017.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindFramebuffer;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glDeleteTextures;
import static android.opengl.GLES20.glGenTextures;

public class TextureUtils {

    static int fboId;
    static int fboTex;
    static int[] temp = new int[1];

    public static int loadTexture(Context context, int resourceId) {
        // создание объекта текстуры
        final int[] textureIds = new int[1];
        glGenTextures(1, textureIds, 0);
        if (textureIds[0] == 0) {
            return 0;
        }

        // получение Bitmap
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;

        final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);

        if (bitmap == null) {
            glDeleteTextures(1, textureIds, 0);
            return 0;
        }

        // настройка объекта текстуры
        glActiveTexture(GL_TEXTURE0);

        glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0]);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

        //GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        //GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, oldRB[0]);
        //glBindFramebuffer(GL_FRAMEBUFFER, oldFBO[0]);
        //glClear(GL_COLOR_BUFFER_BIT);

        //glBindFramebuffer(GL_FRAMEBUFFER, 0);

        bitmap.recycle();

        // сброс target
        //glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        //GLES20.glDeleteFramebuffers(1, new int[]{fboId}, 0);
        //GLES20.glDeleteTextures(1, new int[]{fboTex}, 0);

        return textureIds[0];
    }
}
