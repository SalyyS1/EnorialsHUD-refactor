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

flat in int sfKind;
flat in vec3 sfTiming;
flat in ivec2 sfAtlasOrigin;
in vec2 sfLocal;
flat in int egKind;
flat in int egVariant;
flat in int egAlias;
flat in vec4 egColor;
flat in vec2 egCellMin;
flat in vec2 egCellMax;
flat in vec4 egCanvas;
flat in ivec4 egSettings;
flat in float egFade;
flat in ivec4 egSprite;
flat in vec4 egMotion;
flat in vec4 egColor2;
flat in vec4 egColor3;
flat in int egBundleLength;
#moj_import <minecraft:guiminimap/frame.glsl>
#moj_import <minecraft:guiminimap/sprite_util.glsl>

void main() {
    #moj_import <minecraft:guiminimap/fragment_main.glsl>
    #moj_import <minecraft:spellfx/fragment_main.glsl>

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
