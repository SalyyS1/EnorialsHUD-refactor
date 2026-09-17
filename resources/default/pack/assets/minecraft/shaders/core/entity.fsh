#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

#ifdef DISSOLVE
uniform sampler2D DissolveMaskSampler;
#endif

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
#ifdef PER_FACE_LIGHTING
in vec4 vertexPerFaceColorBack;
in vec4 vertexPerFaceColorFront;
#else
in vec4 vertexColor;
#endif

#ifndef EMISSIVE
in vec4 lightMapColor;
#endif

#ifndef NO_OVERLAY
in vec4 overlayColor;
#endif

in vec2 texCoord0;

out vec4 fragColor;

in vec2 texCoord1;
in vec2 texCoord2;
in vec3 minimapCarrierPosition;
in float minimap;
in float minimapShape;
in float keepEdges;
in float frameStyle;
in float frameThickness;
in float colorProfile;
in float transition;
in float fullscreenMinimap;
in float sx;
in float sy;
#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:minimap/vertex_util.glsl>
#moj_import <minecraft:minimap/fragment_util.glsl>

void main() {
#ifndef PER_FACE_LIGHTING
    #moj_import <minecraft:minimap/fragment_main.glsl>
#endif

    vec4 color = texture(Sampler0, texCoord0);
#ifdef ALPHA_CUTOUT
    if (color.a < ALPHA_CUTOUT) {
        discard;
    }
#endif

#ifdef PER_FACE_LIGHTING
    vec4 faceVertexColor = gl_FrontFacing ? vertexPerFaceColorFront : vertexPerFaceColorBack;
#else
    vec4 faceVertexColor = vertexColor;
#endif

#ifdef DISSOLVE
    if (faceVertexColor.a < texture(DissolveMaskSampler, texCoord0).a) {
        discard;
    }
    // The dissolve effect entirely replaces translucency
    faceVertexColor.a = 1.0;
#endif

    color *= faceVertexColor * ColorModulator;
#ifndef NO_OVERLAY
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
#endif
#ifndef EMISSIVE
    color *= lightMapColor;
#endif

    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
