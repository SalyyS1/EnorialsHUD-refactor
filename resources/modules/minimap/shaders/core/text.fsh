#version 330

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
#moj_import <minecraft:fog.glsl>
#endif

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
in float sphericalVertexDistance;
in float cylindricalVertexDistance;
#endif

in vec4 vertexColor;
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
    #moj_import <minecraft:minimap/fragment_main.glsl>

#ifdef IS_GRAYSCALE
    vec4 texColor = texture(Sampler0, texCoord0).rrrr;
#else
    vec4 texColor = texture(Sampler0, texCoord0);
#endif

#ifdef IS_SEE_THROUGH
    vec4 color = texColor * vertexColor;
#else
    vec4 color = texColor * vertexColor * ColorModulator;
#endif
    if (color.a < 0.1) {
        discard;
    }

#ifdef IS_SEE_THROUGH
    fragColor = color * ColorModulator;
#elif defined(IS_GUI)
    fragColor = color;
#else
    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
#endif
}
