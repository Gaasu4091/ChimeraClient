#version 330
#moj_import <minecraft:dynamictransforms.glsl>
uniform sampler2D Sampler0;
in vec2 texCoord0;
#ifdef PER_FACE_LIGHTING
in vec4 vertexPerFaceColorBack;
in vec4 vertexPerFaceColorFront;
#else
in vec4 vertexColor;
#endif
out vec4 fragColor;
void main() {
    if(texture(Sampler0,texCoord0).a<0.1)discard;
#ifdef PER_FACE_LIGHTING
    fragColor=(gl_FrontFacing?vertexPerFaceColorFront:vertexPerFaceColorBack)*ColorModulator;
#else
    fragColor=vertexColor*ColorModulator;
#endif
}
