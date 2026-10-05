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


out vec4 color;

in vec2 v_TexCoord;
in vec2 v_OneTexel;

uniform sampler2D u_Texture;
uniform sampler2D u_Overlay;
bool decorator() {
    if (u_Dots == 0) return false;
    if (u_Fill.a == 0) return false;
    if (u_Dots == 1)
    return int(gl_FragCoord.x) - (u_DotsRadius * int(gl_FragCoord.x / u_DotsRadius)) == 0
        && int(gl_FragCoord.y) - (u_DotsRadius * int(gl_FragCoord.y / u_DotsRadius)) == 0;
    return int(gl_FragCoord.x) % u_DotsRadius == 0 || int(gl_FragCoord.y) % u_DotsRadius == 0;
}

vec3 calculate(vec3 c, float hue){
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    vec4 L = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 f = vec3(hue, d / (q.x + e), q.x);
    vec3 r = abs(fract(f.xxx + L.xyz) * 6.0 - L.www);
    return f.z * mix(L.xxx, clamp(r - L.xxx, 0.0, 1.0), f.y);
}

void mio_main() {
    vec4 s = texture(u_Texture, v_TexCoord);
    vec4 overlay = texture(u_Overlay, v_TexCoord * vec2(1.0, -1.0));

    if (s.a == 1.0) {
        if (u_Fill.a == 0.0 || overlay.a == 0.0) discard;
        vec4 fillColor = u_Fill;
        if (u_Fill_Offset != 0.0) {
            vec2 strength = (v_TexCoord * 3.0 * vec2(-u_Fill_Strength, u_Fill_Strength));
            float hue = float(mod (((strength.x + strength.y) + u_Fill_Offset), 1.0));
            fillColor = vec4(calculate(u_Fill.rgb, hue), u_Fill.w);
        }
        color = fillColor;

        if (decorator()) {
            color = vec4(fillColor.rgb, u_DotsAlpha);
        } else if (u_Image) {
            color.rgb = mix(overlay.rgb, fillColor.rgb, fillColor.a);
            color.a = u_OverlayAlpha;
        } else {
            color = fillColor;
        }
    } else if (u_Radius > 0) {
        float dist = u_Radius * u_Radius + 1.0;

        if (u_FastLines && u_Radius > 2) {
            bool present = false;

            for (int x = -u_Radius; x <= u_Radius; x += u_Radius) {
                for (int y = -u_Radius; y <= u_Radius; y += u_Radius) {
                    vec4 offset = texture(u_Texture, v_TexCoord + v_OneTexel * vec2(x, y));
                    if (offset.a != 0.0) {
                        present = true;
                        break;
                    }
                }
            }

            if (!present) discard;
        }

        for (int x = -u_Radius; x <= u_Radius; x++) {
            for (int y = -u_Radius; y <= u_Radius; y++) {
                vec4 offset = texture(u_Texture, v_TexCoord + v_OneTexel * vec2(x, y));

                if (offset.a == 1.0) {
                    dist = min(x * x + y * y - 1.0, dist);
                }
            }
        }

        float limit = u_Radius * u_Radius;

        if (dist <= limit) {
            vec4 outlineColor = u_Outline;
            if (u_Outline_Offset != 0.0) {
                vec2 strength = (v_TexCoord * 3.0 * vec2(-u_Outline_Strength, u_Outline_Strength));
                float hue = float(mod (((strength.x + strength.y) + u_Outline_Offset), 1.0));
                outlineColor = vec4(calculate(u_Outline.rgb, hue), u_Outline.w);
            }

            color.rgb = outlineColor.rgb;
            color.a = min((1.0 - (dist / limit)) * u_GlowMultiplier, 1.0);
            if (dist <= 1.5 && u_GlowMultiplier > 0)
                color.a = 0.8;
        } else {
            discard;
        }
    }
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
