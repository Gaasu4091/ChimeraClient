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
out vec2 v_TexCoord;
out vec2 v_OneTexel;
void main(){
    vec2 pos=vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
    gl_Position=vec4(pos*2.0-1.0,0.0,1.0);
    v_TexCoord=pos;
    v_OneTexel=1.0/SizeTimeStep.xy;
}
