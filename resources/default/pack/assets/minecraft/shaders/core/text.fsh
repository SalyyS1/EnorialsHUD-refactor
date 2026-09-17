#version 330
#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
// World labels and mob HP bars use Minecraft's small native text pipeline.
#ifndef IS_GUI

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)

#endif



uniform sampler2D Sampler0;

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
in float sphericalVertexDistance;
in float cylindricalVertexDistance;
#endif

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
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

#else

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)

#endif



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

flat in int gpsActive;
flat in vec4 gpsAngles;
flat in float gpsClock;
in vec2 gpsLocal;
#moj_import <minecraft:gps3d/model.glsl>

void main() {
    if (gpsActive < 0) discard;
    if (gpsActive > 0) {
        // Four subpixel samples keep the silhouette clean at small GUI sizes.
        vec2 dx=dFdx(gpsLocal)*.25, dy=dFdy(gpsLocal)*.25;
        vec4 a=gpsRender(gpsLocal-dx-dy,gpsAngles,gpsClock,gpsActive);
        vec4 b=gpsRender(gpsLocal+dx-dy,gpsAngles,gpsClock,gpsActive);
        vec4 c=gpsRender(gpsLocal-dx+dy,gpsAngles,gpsClock,gpsActive);
        vec4 d=gpsRender(gpsLocal+dx+dy,gpsAngles,gpsClock,gpsActive);
        float alpha=a.a+b.a+c.a+d.a;
        fragColor = vec4((a.rgb*a.a+b.rgb*b.a+c.rgb*c.a+d.rgb*d.a)/max(alpha,.0001),alpha*.25) * ColorModulator;
        if (fragColor.a < .005) discard;
        return;
    }
    #moj_import <minecraft:guiminimap/fragment_main.glsl>
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

#endif
