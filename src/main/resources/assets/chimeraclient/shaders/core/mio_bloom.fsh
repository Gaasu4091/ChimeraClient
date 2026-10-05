#version 330 core
layout(std140) uniform MioSettings {
    vec4 SizeTimeStep;
    vec4 FillColor;
    vec4 OutlineColor;
    vec4 FillColor2;
    vec4 OutlineColor2;
    vec4 Floats0;
    vec4 Floats1;
    ivec4 Ints0;
    ivec4 Ints1;
};
#define u_Size (SizeTimeStep.xy)
#define u_Time (SizeTimeStep.z)
#define u_Step (SizeTimeStep.w)
#define u_FillColor FillColor
#define u_Fill FillColor
#define u_OutlineColor OutlineColor
#define u_Outline OutlineColor
#define u_FillColor2 FillColor2
#define u_OutlineColor2 OutlineColor2
#define u_OverlayAlpha (Floats0.x)
#define u_GlowMultiplier (Floats0.y)
#define u_Fill_Offset (Floats0.z)
#define u_Outline_Offset (Floats0.w)
#define u_Fill_Strength (Floats1.x)
#define u_Outline_Strength (Floats1.y)
#define u_DotsAlpha (Floats1.z)
#define u_Width (Ints0.x)
#define u_Radius (Ints0.x)
#define u_FastLines (Ints0.y != 0)
#define u_ShapeMode (Ints0.z)
#define u_Image (Ints0.w != 0)
#define u_Dots (Ints1.x)
#define u_DotsRadius (Ints1.y)
#define u_GlowQuality (Ints1.z)


in vec2 v_TexCoord;
in vec2 v_OneTexel;

uniform sampler2D u_Texture;
uniform sampler2D u_Overlay;
out vec4 color;

bool decorator() {
    if (u_Dots == 0) return false;
    if (u_FillColor.a == 0) return false;
    if (u_Dots == 1)
    return int(gl_FragCoord.x) - (u_DotsRadius * int(gl_FragCoord.x / u_DotsRadius)) == 0
        && int(gl_FragCoord.y) - (u_DotsRadius * int(gl_FragCoord.y / u_DotsRadius)) == 0;
    return int(gl_FragCoord.x) % u_DotsRadius == 0 || int(gl_FragCoord.y) % u_DotsRadius == 0;
}

float blur(vec4 center, bool outline) {
    if (u_Width == 0.0) return 0.0;

    int w = u_GlowQuality * u_Width;
    float blurred = 0.0;

    blurred += sign(texture(u_Texture, v_TexCoord + v_OneTexel * vec2(w, 0)).a);
    blurred += sign(texture(u_Texture, v_TexCoord + v_OneTexel * vec2(-w, 0)).a);
    blurred += sign(texture(u_Texture, v_TexCoord + v_OneTexel * vec2(0, w)).a);
    blurred += sign(texture(u_Texture, v_TexCoord + v_OneTexel * vec2(0, -w)).a);

    blurred += sign(texture(u_Texture, v_TexCoord + v_OneTexel * vec2(w, w)).a);
    blurred += sign(texture(u_Texture, v_TexCoord + v_OneTexel * vec2(w, -w)).a);
    blurred += sign(texture(u_Texture, v_TexCoord + v_OneTexel * vec2(-w, w)).a);
    blurred += sign(texture(u_Texture, v_TexCoord + v_OneTexel * vec2(-w, -w)).a);

    if (u_FastLines && u_Width > 2 && blurred == 0.0) {
        return 0.0;
    }

    for (int x = -w; x <= w; x += u_GlowQuality) {
        for (int y = -w; y <= w; y += u_GlowQuality) {
            if (x == 0 && y == 0) {
                continue;
            }
            if (sign(x) == w && sign(y) == w
                || sign(x) == w && y == 0
                || sign(y) == 0 && x == 0) {
                continue;
            }

            blurred += sign(texture(u_Texture, v_TexCoord + v_OneTexel * vec2(x, y)).a);
        }
    }

    return clamp(blurred / (((u_Width * u_Width) + u_Width) * 4), 0.0, 1.0) * u_GlowMultiplier;
}

void mio_main() {
    vec4 center = texture(u_Texture, v_TexCoord);
    vec4 overlay = texture(u_Overlay, v_TexCoord * vec2(1.0, -1.0));

    if (center.a != 0.0) {
        if (u_ShapeMode == 0.0 || overlay.a == 0.0) discard;
        if (decorator()) {
            center = vec4(u_FillColor.rgb, u_DotsAlpha);
        } else if (u_Image) {
            center.rgb = mix(overlay.rgb, u_FillColor.rgb, u_FillColor.a);
            center.a = u_OverlayAlpha;
        } else {
            center = u_FillColor;
        }
        if (u_Width != 0) {
            center = mix(center, u_OutlineColor, u_GlowMultiplier - blur(center, false));
        }
    } else {
        if (u_ShapeMode == 1 || u_Width == 0.0) discard;

        float blurFactor = blur(center, true);

        if (blurFactor == 0.0) discard;

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                if (x == 0 && y == 0)
                    continue;

                if (texture(u_Texture, v_TexCoord + v_OneTexel * vec2(x, y)).a > 0.0) {
                    center = u_OutlineColor;
                    center.a = 1.0;
                }
            }
        }

        if (center.a == 0.0) {
            center = u_OutlineColor;
            center.a = blurFactor;
        }
    }

    color = center;
}

void main(){
    mio_main();
    if(Floats1.w>0.5){
        vec4 mask=texture(u_Texture,v_TexCoord);
        float opacity=mask.a>0.0?mask.r:0.0;
        if(mask.a==0.0){
            for(int x=-u_Width;x<=u_Width;x++)
                for(int y=-u_Width;y<=u_Width;y++){
                    vec4 nearby=texture(u_Texture,v_TexCoord+v_OneTexel*vec2(x,y));
                    if(nearby.a>0.0)opacity=max(opacity,nearby.r);
                }
        }
        color.a*=opacity;
    }
}
