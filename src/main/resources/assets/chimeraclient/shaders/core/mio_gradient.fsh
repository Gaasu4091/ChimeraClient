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

bool decorator(vec4 color) {
    if (u_Dots == 0) return false;
    if (color.a == 0) return false;
    if (u_Dots == 1)
    return int(gl_FragCoord.x) - (u_DotsRadius * int(gl_FragCoord.x / u_DotsRadius)) == 0
        && int(gl_FragCoord.y) - (u_DotsRadius * int(gl_FragCoord.y / u_DotsRadius)) == 0;
    return int(gl_FragCoord.x) % u_DotsRadius == 0 || int(gl_FragCoord.y) % u_DotsRadius == 0;
}

vec4 gradient(vec4 rgb, vec4 rgb2, float step, float time) {
    vec2 frag = vec2(gl_FragCoord.xy);
    float distance = sqrt(frag.x * frag.x + frag.y * frag.y);

    distance = distance / step;

    distance = ((sin(distance + time) + 1.0) / 2.0);

    float distanceInv = 1. - distance;
    float r = rgb.r * distance + rgb2.r * distanceInv;
    float g = rgb.g * distance + rgb2.g * distanceInv;
    float b = rgb.b * distance + rgb2.b * distanceInv;
    float a = rgb.a * distance + rgb2.a * distanceInv;
    return vec4(r, g, b, a);
}

void mio_main() {
    vec4 center = texture(u_Texture, v_TexCoord);
    vec4 overlay = texture(u_Overlay, v_TexCoord * vec2(1.0, -1.0));

    if (center.a != 0.0) {
        if (u_ShapeMode == 0 || overlay.a == 0.0) discard;
        vec4 fill = gradient(u_FillColor, u_FillColor2, u_Step * 300., u_Time);
        if (decorator(fill)) {
            center = vec4(fill.rgb, u_DotsAlpha);
        } else if (u_Image) {
            center.rgb = mix(overlay.rgb, fill.rgb, fill.a);
            center.a = u_OverlayAlpha;
        } else {
            center = fill;
        }
    }
    else {
        if (u_ShapeMode == 1 || u_Width == 0.0) discard;

        float dist = u_Width * u_Width + 1.0;

        if (u_FastLines && u_Width > 2) {
            bool present = false;

            for (int x = -u_Width; x <= u_Width; x += u_Width) {
                for (int y = -u_Width; y <= u_Width; y += u_Width) {
                    vec4 offset = texture(u_Texture, v_TexCoord + v_OneTexel * vec2(x, y));
                    if (offset.a != 0.0) {
                        present = true;
                        break;
                    }
                }
            }

            if (!present) discard;
        }


        for (int x = -u_Width; x <= u_Width; x++) {
            for (int y = -u_Width; y <= u_Width; y++) {
                vec4 offset = texture(u_Texture, v_TexCoord + v_OneTexel * vec2(x, y));

                if (offset.a != 0) {
                    dist = min(x * x + y * y - 1.0, dist);
                    center = offset;
                }
            }
        }

        float minDist = u_Width * u_Width;

        if (dist > minDist)
            center.a = 0.0;
        else {
            vec4 outline = gradient(u_OutlineColor, u_OutlineColor2, u_Step * 300., u_Time);
            center.rgb = outline.rgb;
            center.a = min((1.0 - (dist / minDist)) * u_GlowMultiplier, 1.0) * outline.a;
            if (dist <= 1.5 && u_GlowMultiplier > 0)
                center.a = 0.8;
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
