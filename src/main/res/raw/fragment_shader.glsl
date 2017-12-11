precision mediump float;

uniform sampler2D u_TextureUnit;
uniform sampler2D u_Mask;
uniform int a_isClicked;
varying vec2 v_Texture;


void main()
{
    if (a_isClicked == 1) {
        if (texture2D(u_Mask, v_Texture).r == 1.0f) {
                gl_FragColor = texture2D(u_TextureUnit, v_Texture);
        }
        else gl_FragColor = vec4(1.0f, 1.0f, 1.0f, 1.0f);
    }
    else {
        gl_FragColor = texture2D(u_TextureUnit, v_Texture);
    }
}