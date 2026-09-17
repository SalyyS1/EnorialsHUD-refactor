#version 330
#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:sample_lightmap.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
// World labels and mob HP bars use Minecraft's small native text pipeline.
#ifndef IS_GUI

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)


#endif




in vec3 Position;
in vec4 Color;
in vec2 UV0;
#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
in ivec2 UV2;
#endif

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
uniform sampler2D Sampler2;
out float sphericalVertexDistance;
out float cylindricalVertexDistance;
#endif

out vec4 vertexColor;
out vec2 texCoord0;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
    vertexColor = Color * sample_lightmap(Sampler2, UV2);
#else
    vertexColor = Color;
#endif
    texCoord0 = UV0;
}

#else

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)


#endif




in vec3 Position;
in vec4 Color;
in vec2 UV0;
#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
in ivec2 UV2;
#endif

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
uniform sampler2D Sampler2;
out float sphericalVertexDistance;
out float cylindricalVertexDistance;
#endif

out vec4 vertexColor;
out vec2 texCoord0;

#moj_import <minecraft:globals.glsl>
uniform sampler2D Sampler0;
out vec2 texCoord1;
out vec2 texCoord2;
out vec3 minimapCarrierPosition;
out float minimap;
out float minimapShape;
out float keepEdges;
out float frameStyle;
out float frameThickness;
out float colorProfile;
out float transition;
out float fullscreenMinimap;
out float sx;
out float sy;
#moj_import <minecraft:minimap/vertex_util.glsl>
#define EH_VERSION 4
#define EH_OFFSET %BOSSBAR_OFFSET%

float get_id(float offset) {
    if (offset <= 0.0) return 0.0;
    return trunc(offset / 1000.0);
}


flat out int egKind;
flat out int egVariant;
flat out int egAlias;
flat out vec4 egColor;
flat out vec2 egCellMin;
flat out vec2 egCellMax;
flat out vec4 egCanvas;
flat out ivec4 egSettings;
flat out float egFade;
flat out ivec4 egSprite;
flat out vec4 egMotion;
flat out vec4 egColor2;
flat out vec4 egColor3;
flat out int egBundleLength;
#moj_import <minecraft:guiminimap/color.glsl>
#moj_import <minecraft:guiminimap/data_util.glsl>
const vec4 EG_RADAR_COLORS[3] = vec4[3](vec4(189.0/255.0,48.0/255.0,49.0/255.0,1.0),vec4(229.0/255.0,229.0/255.0,51.0/255.0,1.0),vec4(0.0/255.0,217.0/255.0,58.0/255.0,1.0));
const vec4 EG_PALETTE[256] = vec4[256](vec4(0.0/255.0,0.0/255.0,0.0/255.0,1.0),vec4(0.0/255.0,0.0/255.0,0.0/255.0,1.0),vec4(0.0/255.0,0.0/255.0,0.0/255.0,1.0),vec4(0.0/255.0,0.0/255.0,0.0/255.0,1.0),vec4(89.0/255.0,125.0/255.0,39.0/255.0,1.0),vec4(109.0/255.0,153.0/255.0,48.0/255.0,1.0),vec4(127.0/255.0,178.0/255.0,56.0/255.0,1.0),vec4(67.0/255.0,94.0/255.0,29.0/255.0,1.0),vec4(174.0/255.0,164.0/255.0,115.0/255.0,1.0),vec4(213.0/255.0,201.0/255.0,140.0/255.0,1.0),vec4(247.0/255.0,233.0/255.0,163.0/255.0,1.0),vec4(130.0/255.0,123.0/255.0,86.0/255.0,1.0),vec4(140.0/255.0,140.0/255.0,140.0/255.0,1.0),vec4(171.0/255.0,171.0/255.0,171.0/255.0,1.0),vec4(199.0/255.0,199.0/255.0,199.0/255.0,1.0),vec4(105.0/255.0,105.0/255.0,105.0/255.0,1.0),vec4(180.0/255.0,0.0/255.0,0.0/255.0,1.0),vec4(220.0/255.0,0.0/255.0,0.0/255.0,1.0),vec4(255.0/255.0,0.0/255.0,0.0/255.0,1.0),vec4(135.0/255.0,0.0/255.0,0.0/255.0,1.0),vec4(112.0/255.0,112.0/255.0,180.0/255.0,1.0),vec4(138.0/255.0,138.0/255.0,220.0/255.0,1.0),vec4(160.0/255.0,160.0/255.0,255.0/255.0,1.0),vec4(84.0/255.0,84.0/255.0,135.0/255.0,1.0),vec4(117.0/255.0,117.0/255.0,117.0/255.0,1.0),vec4(144.0/255.0,144.0/255.0,144.0/255.0,1.0),vec4(167.0/255.0,167.0/255.0,167.0/255.0,1.0),vec4(88.0/255.0,88.0/255.0,88.0/255.0,1.0),vec4(0.0/255.0,87.0/255.0,0.0/255.0,1.0),vec4(0.0/255.0,106.0/255.0,0.0/255.0,1.0),vec4(0.0/255.0,124.0/255.0,0.0/255.0,1.0),vec4(0.0/255.0,65.0/255.0,0.0/255.0,1.0),vec4(180.0/255.0,180.0/255.0,180.0/255.0,1.0),vec4(220.0/255.0,220.0/255.0,220.0/255.0,1.0),vec4(255.0/255.0,255.0/255.0,255.0/255.0,1.0),vec4(135.0/255.0,135.0/255.0,135.0/255.0,1.0),vec4(115.0/255.0,118.0/255.0,129.0/255.0,1.0),vec4(141.0/255.0,144.0/255.0,158.0/255.0,1.0),vec4(164.0/255.0,168.0/255.0,184.0/255.0,1.0),vec4(86.0/255.0,88.0/255.0,97.0/255.0,1.0),vec4(106.0/255.0,76.0/255.0,54.0/255.0,1.0),vec4(130.0/255.0,94.0/255.0,66.0/255.0,1.0),vec4(151.0/255.0,109.0/255.0,77.0/255.0,1.0),vec4(79.0/255.0,57.0/255.0,40.0/255.0,1.0),vec4(79.0/255.0,79.0/255.0,79.0/255.0,1.0),vec4(96.0/255.0,96.0/255.0,96.0/255.0,1.0),vec4(112.0/255.0,112.0/255.0,112.0/255.0,1.0),vec4(59.0/255.0,59.0/255.0,59.0/255.0,1.0),vec4(45.0/255.0,45.0/255.0,180.0/255.0,1.0),vec4(55.0/255.0,55.0/255.0,220.0/255.0,1.0),vec4(64.0/255.0,64.0/255.0,255.0/255.0,1.0),vec4(33.0/255.0,33.0/255.0,135.0/255.0,1.0),vec4(100.0/255.0,84.0/255.0,50.0/255.0,1.0),vec4(123.0/255.0,102.0/255.0,62.0/255.0,1.0),vec4(143.0/255.0,119.0/255.0,72.0/255.0,1.0),vec4(75.0/255.0,63.0/255.0,38.0/255.0,1.0),vec4(180.0/255.0,177.0/255.0,172.0/255.0,1.0),vec4(220.0/255.0,217.0/255.0,211.0/255.0,1.0),vec4(255.0/255.0,252.0/255.0,245.0/255.0,1.0),vec4(135.0/255.0,133.0/255.0,129.0/255.0,1.0),vec4(152.0/255.0,89.0/255.0,36.0/255.0,1.0),vec4(186.0/255.0,109.0/255.0,44.0/255.0,1.0),vec4(216.0/255.0,127.0/255.0,51.0/255.0,1.0),vec4(114.0/255.0,67.0/255.0,27.0/255.0,1.0),vec4(125.0/255.0,53.0/255.0,152.0/255.0,1.0),vec4(153.0/255.0,65.0/255.0,186.0/255.0,1.0),vec4(178.0/255.0,76.0/255.0,216.0/255.0,1.0),vec4(94.0/255.0,40.0/255.0,114.0/255.0,1.0),vec4(72.0/255.0,108.0/255.0,152.0/255.0,1.0),vec4(88.0/255.0,132.0/255.0,186.0/255.0,1.0),vec4(102.0/255.0,153.0/255.0,216.0/255.0,1.0),vec4(54.0/255.0,81.0/255.0,114.0/255.0,1.0),vec4(161.0/255.0,161.0/255.0,36.0/255.0,1.0),vec4(197.0/255.0,197.0/255.0,44.0/255.0,1.0),vec4(229.0/255.0,229.0/255.0,51.0/255.0,1.0),vec4(121.0/255.0,121.0/255.0,27.0/255.0,1.0),vec4(89.0/255.0,144.0/255.0,17.0/255.0,1.0),vec4(109.0/255.0,176.0/255.0,21.0/255.0,1.0),vec4(127.0/255.0,204.0/255.0,25.0/255.0,1.0),vec4(67.0/255.0,108.0/255.0,13.0/255.0,1.0),vec4(170.0/255.0,89.0/255.0,116.0/255.0,1.0),vec4(208.0/255.0,109.0/255.0,142.0/255.0,1.0),vec4(242.0/255.0,127.0/255.0,165.0/255.0,1.0),vec4(128.0/255.0,67.0/255.0,87.0/255.0,1.0),vec4(53.0/255.0,53.0/255.0,53.0/255.0,1.0),vec4(65.0/255.0,65.0/255.0,65.0/255.0,1.0),vec4(76.0/255.0,76.0/255.0,76.0/255.0,1.0),vec4(40.0/255.0,40.0/255.0,40.0/255.0,1.0),vec4(108.0/255.0,108.0/255.0,108.0/255.0,1.0),vec4(132.0/255.0,132.0/255.0,132.0/255.0,1.0),vec4(153.0/255.0,153.0/255.0,153.0/255.0,1.0),vec4(81.0/255.0,81.0/255.0,81.0/255.0,1.0),vec4(53.0/255.0,89.0/255.0,108.0/255.0,1.0),vec4(65.0/255.0,109.0/255.0,132.0/255.0,1.0),vec4(76.0/255.0,127.0/255.0,153.0/255.0,1.0),vec4(40.0/255.0,67.0/255.0,81.0/255.0,1.0),vec4(89.0/255.0,44.0/255.0,125.0/255.0,1.0),vec4(109.0/255.0,54.0/255.0,153.0/255.0,1.0),vec4(127.0/255.0,63.0/255.0,178.0/255.0,1.0),vec4(67.0/255.0,33.0/255.0,94.0/255.0,1.0),vec4(36.0/255.0,53.0/255.0,125.0/255.0,1.0),vec4(44.0/255.0,65.0/255.0,153.0/255.0,1.0),vec4(51.0/255.0,76.0/255.0,178.0/255.0,1.0),vec4(27.0/255.0,40.0/255.0,94.0/255.0,1.0),vec4(72.0/255.0,53.0/255.0,36.0/255.0,1.0),vec4(88.0/255.0,65.0/255.0,44.0/255.0,1.0),vec4(102.0/255.0,76.0/255.0,51.0/255.0,1.0),vec4(54.0/255.0,40.0/255.0,27.0/255.0,1.0),vec4(72.0/255.0,89.0/255.0,36.0/255.0,1.0),vec4(88.0/255.0,109.0/255.0,44.0/255.0,1.0),vec4(102.0/255.0,127.0/255.0,51.0/255.0,1.0),vec4(54.0/255.0,67.0/255.0,27.0/255.0,1.0),vec4(108.0/255.0,36.0/255.0,36.0/255.0,1.0),vec4(132.0/255.0,44.0/255.0,44.0/255.0,1.0),vec4(153.0/255.0,51.0/255.0,51.0/255.0,1.0),vec4(81.0/255.0,27.0/255.0,27.0/255.0,1.0),vec4(17.0/255.0,17.0/255.0,17.0/255.0,1.0),vec4(21.0/255.0,21.0/255.0,21.0/255.0,1.0),vec4(25.0/255.0,25.0/255.0,25.0/255.0,1.0),vec4(13.0/255.0,13.0/255.0,13.0/255.0,1.0),vec4(176.0/255.0,168.0/255.0,54.0/255.0,1.0),vec4(215.0/255.0,205.0/255.0,66.0/255.0,1.0),vec4(250.0/255.0,238.0/255.0,77.0/255.0,1.0),vec4(132.0/255.0,126.0/255.0,40.0/255.0,1.0),vec4(64.0/255.0,154.0/255.0,150.0/255.0,1.0),vec4(79.0/255.0,188.0/255.0,183.0/255.0,1.0),vec4(92.0/255.0,219.0/255.0,213.0/255.0,1.0),vec4(48.0/255.0,115.0/255.0,112.0/255.0,1.0),vec4(52.0/255.0,90.0/255.0,180.0/255.0,1.0),vec4(63.0/255.0,110.0/255.0,220.0/255.0,1.0),vec4(74.0/255.0,128.0/255.0,255.0/255.0,1.0),vec4(39.0/255.0,67.0/255.0,135.0/255.0,1.0),vec4(0.0/255.0,153.0/255.0,40.0/255.0,1.0),vec4(0.0/255.0,187.0/255.0,50.0/255.0,1.0),vec4(0.0/255.0,217.0/255.0,58.0/255.0,1.0),vec4(0.0/255.0,114.0/255.0,30.0/255.0,1.0),vec4(91.0/255.0,60.0/255.0,34.0/255.0,1.0),vec4(111.0/255.0,74.0/255.0,42.0/255.0,1.0),vec4(129.0/255.0,86.0/255.0,49.0/255.0,1.0),vec4(68.0/255.0,45.0/255.0,25.0/255.0,1.0),vec4(79.0/255.0,1.0/255.0,0.0/255.0,1.0),vec4(96.0/255.0,1.0/255.0,0.0/255.0,1.0),vec4(112.0/255.0,2.0/255.0,0.0/255.0,1.0),vec4(59.0/255.0,1.0/255.0,0.0/255.0,1.0),vec4(147.0/255.0,124.0/255.0,113.0/255.0,1.0),vec4(180.0/255.0,152.0/255.0,138.0/255.0,1.0),vec4(209.0/255.0,177.0/255.0,161.0/255.0,1.0),vec4(110.0/255.0,93.0/255.0,85.0/255.0,1.0),vec4(112.0/255.0,57.0/255.0,25.0/255.0,1.0),vec4(137.0/255.0,70.0/255.0,31.0/255.0,1.0),vec4(159.0/255.0,82.0/255.0,36.0/255.0,1.0),vec4(84.0/255.0,43.0/255.0,19.0/255.0,1.0),vec4(105.0/255.0,61.0/255.0,76.0/255.0,1.0),vec4(128.0/255.0,75.0/255.0,93.0/255.0,1.0),vec4(149.0/255.0,87.0/255.0,108.0/255.0,1.0),vec4(78.0/255.0,46.0/255.0,57.0/255.0,1.0),vec4(79.0/255.0,76.0/255.0,97.0/255.0,1.0),vec4(96.0/255.0,93.0/255.0,119.0/255.0,1.0),vec4(112.0/255.0,108.0/255.0,138.0/255.0,1.0),vec4(59.0/255.0,57.0/255.0,73.0/255.0,1.0),vec4(131.0/255.0,93.0/255.0,25.0/255.0,1.0),vec4(160.0/255.0,114.0/255.0,31.0/255.0,1.0),vec4(186.0/255.0,133.0/255.0,36.0/255.0,1.0),vec4(98.0/255.0,70.0/255.0,19.0/255.0,1.0),vec4(72.0/255.0,82.0/255.0,37.0/255.0,1.0),vec4(88.0/255.0,100.0/255.0,45.0/255.0,1.0),vec4(103.0/255.0,117.0/255.0,53.0/255.0,1.0),vec4(54.0/255.0,61.0/255.0,28.0/255.0,1.0),vec4(112.0/255.0,54.0/255.0,55.0/255.0,1.0),vec4(138.0/255.0,66.0/255.0,67.0/255.0,1.0),vec4(160.0/255.0,77.0/255.0,78.0/255.0,1.0),vec4(84.0/255.0,40.0/255.0,41.0/255.0,1.0),vec4(40.0/255.0,28.0/255.0,24.0/255.0,1.0),vec4(49.0/255.0,35.0/255.0,30.0/255.0,1.0),vec4(57.0/255.0,41.0/255.0,35.0/255.0,1.0),vec4(30.0/255.0,21.0/255.0,18.0/255.0,1.0),vec4(95.0/255.0,75.0/255.0,69.0/255.0,1.0),vec4(116.0/255.0,92.0/255.0,84.0/255.0,1.0),vec4(135.0/255.0,107.0/255.0,98.0/255.0,1.0),vec4(71.0/255.0,56.0/255.0,51.0/255.0,1.0),vec4(61.0/255.0,64.0/255.0,64.0/255.0,1.0),vec4(75.0/255.0,79.0/255.0,79.0/255.0,1.0),vec4(87.0/255.0,92.0/255.0,92.0/255.0,1.0),vec4(46.0/255.0,48.0/255.0,48.0/255.0,1.0),vec4(86.0/255.0,51.0/255.0,62.0/255.0,1.0),vec4(105.0/255.0,62.0/255.0,75.0/255.0,1.0),vec4(122.0/255.0,73.0/255.0,88.0/255.0,1.0),vec4(64.0/255.0,38.0/255.0,46.0/255.0,1.0),vec4(53.0/255.0,43.0/255.0,64.0/255.0,1.0),vec4(65.0/255.0,53.0/255.0,79.0/255.0,1.0),vec4(76.0/255.0,62.0/255.0,92.0/255.0,1.0),vec4(40.0/255.0,32.0/255.0,48.0/255.0,1.0),vec4(53.0/255.0,35.0/255.0,24.0/255.0,1.0),vec4(65.0/255.0,43.0/255.0,30.0/255.0,1.0),vec4(76.0/255.0,50.0/255.0,35.0/255.0,1.0),vec4(40.0/255.0,26.0/255.0,18.0/255.0,1.0),vec4(53.0/255.0,57.0/255.0,29.0/255.0,1.0),vec4(65.0/255.0,70.0/255.0,36.0/255.0,1.0),vec4(76.0/255.0,82.0/255.0,42.0/255.0,1.0),vec4(40.0/255.0,43.0/255.0,22.0/255.0,1.0),vec4(100.0/255.0,42.0/255.0,32.0/255.0,1.0),vec4(122.0/255.0,51.0/255.0,39.0/255.0,1.0),vec4(142.0/255.0,60.0/255.0,46.0/255.0,1.0),vec4(75.0/255.0,31.0/255.0,24.0/255.0,1.0),vec4(26.0/255.0,15.0/255.0,11.0/255.0,1.0),vec4(31.0/255.0,18.0/255.0,13.0/255.0,1.0),vec4(37.0/255.0,22.0/255.0,16.0/255.0,1.0),vec4(19.0/255.0,11.0/255.0,8.0/255.0,1.0),vec4(133.0/255.0,33.0/255.0,34.0/255.0,1.0),vec4(163.0/255.0,41.0/255.0,42.0/255.0,1.0),vec4(189.0/255.0,48.0/255.0,49.0/255.0,1.0),vec4(100.0/255.0,25.0/255.0,25.0/255.0,1.0),vec4(104.0/255.0,44.0/255.0,68.0/255.0,1.0),vec4(127.0/255.0,54.0/255.0,83.0/255.0,1.0),vec4(148.0/255.0,63.0/255.0,97.0/255.0,1.0),vec4(78.0/255.0,33.0/255.0,51.0/255.0,1.0),vec4(64.0/255.0,17.0/255.0,20.0/255.0,1.0),vec4(79.0/255.0,21.0/255.0,25.0/255.0,1.0),vec4(92.0/255.0,25.0/255.0,29.0/255.0,1.0),vec4(48.0/255.0,13.0/255.0,15.0/255.0,1.0),vec4(15.0/255.0,88.0/255.0,94.0/255.0,1.0),vec4(18.0/255.0,108.0/255.0,115.0/255.0,1.0),vec4(22.0/255.0,126.0/255.0,134.0/255.0,1.0),vec4(11.0/255.0,66.0/255.0,70.0/255.0,1.0),vec4(40.0/255.0,100.0/255.0,98.0/255.0,1.0),vec4(50.0/255.0,122.0/255.0,120.0/255.0,1.0),vec4(58.0/255.0,142.0/255.0,140.0/255.0,1.0),vec4(30.0/255.0,75.0/255.0,74.0/255.0,1.0),vec4(60.0/255.0,31.0/255.0,43.0/255.0,1.0),vec4(74.0/255.0,37.0/255.0,53.0/255.0,1.0),vec4(86.0/255.0,44.0/255.0,62.0/255.0,1.0),vec4(45.0/255.0,23.0/255.0,32.0/255.0,1.0),vec4(14.0/255.0,127.0/255.0,93.0/255.0,1.0),vec4(17.0/255.0,155.0/255.0,114.0/255.0,1.0),vec4(20.0/255.0,180.0/255.0,133.0/255.0,1.0),vec4(10.0/255.0,95.0/255.0,70.0/255.0,1.0),vec4(70.0/255.0,70.0/255.0,70.0/255.0,1.0),vec4(86.0/255.0,86.0/255.0,86.0/255.0,1.0),vec4(100.0/255.0,100.0/255.0,100.0/255.0,1.0),vec4(52.0/255.0,52.0/255.0,52.0/255.0,1.0),vec4(152.0/255.0,123.0/255.0,103.0/255.0,1.0),vec4(186.0/255.0,150.0/255.0,126.0/255.0,1.0),vec4(216.0/255.0,175.0/255.0,147.0/255.0,1.0),vec4(114.0/255.0,92.0/255.0,77.0/255.0,1.0),vec4(89.0/255.0,117.0/255.0,105.0/255.0,1.0),vec4(109.0/255.0,144.0/255.0,129.0/255.0,1.0),vec4(127.0/255.0,167.0/255.0,150.0/255.0,1.0),vec4(67.0/255.0,88.0/255.0,79.0/255.0,1.0),vec4(0.0/255.0,0.0/255.0,0.0/255.0,1.0),vec4(0.0/255.0,0.0/255.0,0.0/255.0,1.0),vec4(0.0/255.0,0.0/255.0,0.0/255.0,1.0),vec4(0.0/255.0,0.0/255.0,0.0/255.0,1.0),vec4(0.0/255.0,0.0/255.0,0.0/255.0,1.0),vec4(0.0/255.0,0.0/255.0,0.0/255.0,1.0),vec4(0.0/255.0,0.0/255.0,0.0/255.0,1.0),vec4(0.0/255.0,0.0/255.0,0.0/255.0,1.0));

flat out int gpsActive;
flat out vec4 gpsAngles;
flat out float gpsClock;
out vec2 gpsLocal;

void main() {
    gpsActive = 0;
    gpsAngles = vec4(0.0);
    gpsClock = 0.0;
    gpsLocal = vec2(0.0);
    #moj_import <minecraft:guiminimap/vertex_main.glsl>
    vec3 pos = Position;
#ifdef IS_GUI


    vec2 pixel = vec2(ProjMat[0][0], ProjMat[1][1]) / 2.0;
    int guiScale = max(1, int(round(pixel.x * ScreenSize.x)));
    vec2 guiSize = ScreenSize / guiScale;

    float id = get_id((round(EH_OFFSET - Position.y)) * -1.0);



    // Conversation design 37: complete dynamic extent, shared art and RGB565 NPC portrait.
    if ((id == 93.0 || id == 94.0) && Color.a != 0.0) {
        int code = int(round(Color.r * 255.0)) * 65536 + int(round(Color.g * 255.0)) * 256 + int(round(Color.b * 255.0));
        float height = float((code >> 16) & 255) * 4.0;
        float glyphY = pos.y - (id * 1000.0 + 500.0 + EH_OFFSET);
        float localY = glyphY;
        if (id == 93.0) localY += float((code >> 4) & 1023) - 512.0;
        float fit = max(0.05, min(1.0, min((guiSize.x - 24.0) / 416.0, (guiSize.y - 54.0) / max(height, 1.0))));
        pos = vec3((pos.x - guiSize.x * 0.5) * fit + guiSize.x * 0.5, localY * fit + guiSize.y - 30.0 - 24.0 * fit, pos.z + 8.0);
        vec3 ink = vec3(1.0);
        if (id == 94.0) ink = vec3(float((code >> 11) & 31) / 31.0, float((code >> 5) & 63) / 63.0, float(code & 31) / 31.0);
        else {
            int role = code & 15;
            float shade = clamp(glyphY / 9.0, 0.0, 1.0);
            if (role == 2) ink = mix(vec3(1.0, 0.91, 0.47), vec3(0.96, 0.66, 0.13), shade);
            if (role == 3) ink = mix(vec3(0.91, 0.96, 1.0), vec3(0.63, 0.72, 0.80), shade);
            if (role == 4) ink = vec3(0.025, 0.035, 0.045);
        }
        vertexColor = vec4(ink, Color.a);
        minimap = 0.0; minimapShape = 0.0; keepEdges = 0.0;
        frameStyle = 0.0; frameThickness = 0.0; colorProfile = 0.0;
        transition = 0.0; fullscreenMinimap = 0.0; sx = 0.0; sy = 0.0;
        texCoord1 = vec2(0.0); texCoord2 = vec2(0.0); minimapCarrierPosition = Position;
        texCoord0 = UV0; gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0); return;
    }

    // Enorials conversations: shared fonts, independent of GUI scale and world projection.
    // 90 = text/frame with a compact Y/palette code; 91 = RGB portrait pixels.
    if ((id == 90.0 || id == 91.0 || id == 92.0) && Color.a != 0.0) {
        int code = int(round(Color.r * 255.0)) * 65536 + int(round(Color.g * 255.0)) * 256 + int(round(Color.b * 255.0));
        float localY = pos.y - (id * 1000.0 + 500.0 + EH_OFFSET);
        if (id == 90.0) localY += float((code >> 4) & 511) - 256.0;
        float fit = min(1.0, min((guiSize.x - 12.0) / 320.0, (guiSize.y - 64.0) / 220.0));

        if (id == 92.0) {
            // HUD 34: shared sprites; layout, height, Y, size and palette encoded in the color.
            int mapLayout = (code >> 21) & 7;
            float height = float((code >> 14) & 127) * 4.0;
            float glyphY = localY;
            localY += float((code >> 5) & 511);
            float top = 16.0;
            if (mapLayout > 0) {
                int size = (mapLayout - 1) % 3;
                float mapScale = size == 0 ? 0.52 : size == 1 ? 0.76 : 1.0;
                float mapBottom = 0.04 + 0.66 * mapScale;
                if (mapLayout > 3) mapBottom += 0.015 + 0.16 * mapScale;
                top = min(guiSize.x, guiSize.y) * mapBottom * 0.5 + 8.0;
            }
            fit = max(0.05, min(1.0, min((guiSize.y - top - 12.0) / max(height, 1.0), (guiSize.x * 0.43) / 184.0)));
            int sizeIndex = (code >> 3) & 3;
            float requestedScale = sizeIndex == 1 ? 0.75 : sizeIndex == 2 ? 1.25 : sizeIndex == 3 ? 0.5 : 1.0;
            // Keep 100% identical to HUD 33, scale everything together, then cap to the actual free viewport.
            fit = min(fit * requestedScale, max(0.05, min((guiSize.y - top - 12.0) / max(height, 1.0), (guiSize.x - 16.0) / 184.0)));
            top = max(top, guiSize.y * 0.5 - height * fit * 0.5);
            pos = vec3((pos.x - guiSize.x * 0.5) * fit + guiSize.x - 8.0, localY * fit + top, pos.z + 7.0);
            int role = code & 7;
            float shade = clamp(glyphY / (role == 3 ? 7.0 : 8.0), 0.0, 1.0);
            vec3 ink = vec3(1.0);
            if (role == 1) ink = vec3(0.035, 0.045, 0.045);
            if (role == 2) ink = mix(vec3(1.0, 0.88, 0.49), vec3(0.94, 0.56, 0.09), shade);
            if (role == 3) ink = mix(vec3(0.94, 0.97, 0.98), vec3(0.65, 0.73, 0.76), shade);
            if (role == 4) ink = mix(vec3(0.98, 0.89, 0.65), vec3(0.84, 0.68, 0.36), shade);
            vertexColor = vec4(ink, Color.a);
        } else {
        if (id == 90.0 && (code & 0xf00000) == 0xe00000) {
            // Quest log shares the tiny dialogue fonts, anchored to the right sidebar area.
            int mapLayout = (code >> 13) & 7;
            float top = 16.0;
            if (mapLayout > 0) {
                int size = (mapLayout - 1) % 3;
                float mapScale = size == 0 ? 0.52 : size == 1 ? 0.76 : 1.0;
                float mapBottom = 0.04 + 0.66 * mapScale;
                if (mapLayout > 3) mapBottom += 0.015 + 0.16 * mapScale;
                top = min(guiSize.x, guiSize.y) * mapBottom * 0.5 + 8.0;
            }
            fit = min(1.0, min((guiSize.y - top - 12.0) / 256.0, (guiSize.x * 0.43) / 184.0));
            top = max(top, guiSize.y * 0.5 - 124.0 * fit);
            pos = vec3((pos.x - guiSize.x * 0.5) * fit + guiSize.x - 8.0,
                       localY * fit + top, pos.z + 7.0);
        } else {
            pos = vec3((pos.x - guiSize.x * 0.5) * fit + guiSize.x * 0.5,
                       localY * fit + guiSize.y - 42.0, pos.z + 8.0);
        }
        vertexColor = Color;
        if (id == 90.0) {
            int role = code & 15;
            vec3 ink = role == 2 ? vec3(1.0, 0.78, 0.31) : role == 3 ? vec3(0.70, 0.77, 0.79) : role == 4 ? vec3(0.94, 0.85, 0.61) : vec3(1.0);
            vertexColor = vec4(ink, Color.a);
        }
        } // Original dialogue/portrait palette.
        minimap = 0.0; minimapShape = 0.0; keepEdges = 0.0;
        frameStyle = 0.0; frameThickness = 0.0; colorProfile = 0.0;
        transition = 0.0; fullscreenMinimap = 0.0; sx = 0.0; sy = 0.0;
        texCoord1 = vec2(0.0); texCoord2 = vec2(0.0); minimapCarrierPosition = Position;
        texCoord0 = UV0; gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0); return;
    }

    if (id > 99.0 && id < 500.0 && Color.a != 0.0) {
#ifdef ENO_LEGACY_GPS
        // ENO_HUD_SHADOW_FIX_V1
        // Older Font renderers submit a second, offset shadow quad even when
        // the server requests no shadow. Only discard our encoded HUD glyphs;
        // normal chat and other plugins retain their native text shadows.
        if (fract(Position.z + 0.001) < 0.02) {
            gl_Position = vec4(2.0, 2.0, 2.0, 1.0);
            return;
        }
#endif
        float yOffset = 0.0;
        float xOffset = 0.0;
        float layer = 0.0;
        vec2 scale = vec2(1.0, 1.0);
        bool outlined = false;

        switch (int(id)) {
%POSITIONS%
        }

        if (true) {
            pos.y -= (id * 1000.0) + 500.0 + EH_OFFSET;
            pos.x -= (guiSize.x * 0.5);

            pos.x *= scale.x;
            pos.y *= scale.y;

            pos.y += guiSize.y;
            if (guiScale == 3) {
                pos.x += 1.45;
            }

            pos -= vec3(xOffset, yOffset, 0.0);
            pos.z += layer;
        }
    }
    // HappyHUD action-bar glyphs use a separate ID range from AnchorHUD.
    float happyId = get_id((round(guiSize.y - Position.y + 64.0)) * -1.0);
    if (happyId >= 500.0 && Color.a != 0.0) {
        float yOffset = 0.0;
        float xOffset = 0.0;
        int layer = 0;
        bool outlined = false;
        switch (int(happyId)) {
            case 500:
                xOffset = int(guiSize.x * (-50.0/100))-290;
                yOffset = int(guiSize.y * (0.0/100))+11;
                layer = 1;
                break;
            case 501:
                xOffset = int(guiSize.x * (-50.0/100))-290;
                yOffset = int(guiSize.y * (0.0/100))+14;
                layer = 1;
                break;
            case 502:
                xOffset = int(guiSize.x * (-50.0/100))-279;
                yOffset = int(guiSize.y * (0.0/100))+12;
                layer = 2;
                break;
            case 503:
                xOffset = int(guiSize.x * (-50.0/100))-290;
                yOffset = int(guiSize.y * (0.0/100))+21;
                layer = 1;
                break;
            case 504:
                xOffset = int(guiSize.x * (-50.0/100))-290;
                yOffset = int(guiSize.y * (0.0/100))+24;
                layer = 1;
                break;
            case 505:
                xOffset = int(guiSize.x * (-50.0/100))-279;
                yOffset = int(guiSize.y * (0.0/100))+22;
                layer = 2;
                break;
            case 506:
                xOffset = int(guiSize.x * (-50.0/100))-260;
                yOffset = int(guiSize.y * (0.0/100))+11;
                layer = 1;
                break;
            case 507:
                xOffset = int(guiSize.x * (-50.0/100))-260;
                yOffset = int(guiSize.y * (0.0/100))+14;
                layer = 1;
                break;
            case 508:
                xOffset = int(guiSize.x * (-50.0/100))-249;
                yOffset = int(guiSize.y * (0.0/100))+12;
                layer = 2;
                break;
            case 509:
                xOffset = int(guiSize.x * (-50.0/100))-260;
                yOffset = int(guiSize.y * (0.0/100))+21;
                layer = 1;
                break;
            case 510:
                xOffset = int(guiSize.x * (-50.0/100))-260;
                yOffset = int(guiSize.y * (0.0/100))+24;
                layer = 1;
                break;
            case 511:
                xOffset = int(guiSize.x * (-50.0/100))-249;
                yOffset = int(guiSize.y * (0.0/100))+22;
                layer = 2;
                break;
            case 512:
                xOffset = int(guiSize.x * (-50.0/100))-216;
                yOffset = int(guiSize.y * (0.0/100))+11;
                layer = 1;
                break;
            case 513:
                xOffset = int(guiSize.x * (-50.0/100))-216;
                yOffset = int(guiSize.y * (0.0/100))+14;
                layer = 1;
                break;
            case 514:
                xOffset = int(guiSize.x * (-50.0/100))-205;
                yOffset = int(guiSize.y * (0.0/100))+12;
                layer = 2;
                break;
            case 515:
                xOffset = int(guiSize.x * (-50.0/100))-216;
                yOffset = int(guiSize.y * (0.0/100))+21;
                layer = 1;
                break;
            case 516:
                xOffset = int(guiSize.x * (-50.0/100))-216;
                yOffset = int(guiSize.y * (0.0/100))+24;
                layer = 1;
                break;
            case 517:
                xOffset = int(guiSize.x * (-50.0/100))-205;
                yOffset = int(guiSize.y * (0.0/100))+22;
                layer = 2;
                break;
            case 518:
                xOffset = int(guiSize.x * (-50.0/100))-172;
                yOffset = int(guiSize.y * (0.0/100))+23;
                layer = 1;
                break;
            case 519:
                xOffset = int(guiSize.x * (-50.0/100))-173;
                yOffset = int(guiSize.y * (0.0/100))+22;
                layer = 2;
                break;
            case 520:
                xOffset = int(guiSize.x * (-50.0/100))-162;
                yOffset = int(guiSize.y * (0.0/100))+22;
                layer = 3;
                break;
            case 521:
                xOffset = int(guiSize.x * (-50.0/100))-130;
                yOffset = int(guiSize.y * (0.0/100))+23;
                layer = 1;
                break;
            case 522:
                xOffset = int(guiSize.x * (-50.0/100))-131;
                yOffset = int(guiSize.y * (0.0/100))+22;
                layer = 2;
                break;
            case 523:
                xOffset = int(guiSize.x * (-50.0/100))-120;
                yOffset = int(guiSize.y * (0.0/100))+22;
                layer = 3;
                break;
            case 524:
                xOffset = int(guiSize.x * (-50.0/100))+210;
                yOffset = int(guiSize.y * (0.0/100))+23;
                layer = 1;
                break;
            case 525:
                xOffset = int(guiSize.x * (-50.0/100))+209;
                yOffset = int(guiSize.y * (0.0/100))+22;
                layer = 2;
                break;
            case 526:
                xOffset = int(guiSize.x * (-50.0/100))+209;
                yOffset = int(guiSize.y * (0.0/100))+22;
                layer = 3;
                break;
            case 527:
                xOffset = int(guiSize.x * (-50.0/100))+189;
                yOffset = int(guiSize.y * (0.0/100))+21;
                layer = 4;
                break;
            case 528:
                xOffset = int(guiSize.x * (-50.0/100))+189;
                yOffset = int(guiSize.y * (0.0/100))+24;
                layer = 4;
                break;
            case 529:
                xOffset = int(guiSize.x * (-50.0/100))+220;
                yOffset = int(guiSize.y * (0.0/100))+22;
                layer = 5;
                break;
            case 530:
                xOffset = int(guiSize.x * (-50.0/100))-130;
                yOffset = int(guiSize.y * (0.0/100))+12;
                layer = 1;
                break;
            case 531:
                xOffset = int(guiSize.x * (-50.0/100))-131;
                yOffset = int(guiSize.y * (0.0/100))+11;
                layer = 2;
                break;
            case 532:
                xOffset = int(guiSize.x * (-50.0/100))-151;
                yOffset = int(guiSize.y * (0.0/100))+10;
                layer = 3;
                break;
            case 533:
                xOffset = int(guiSize.x * (-50.0/100))-151;
                yOffset = int(guiSize.y * (0.0/100))+13;
                layer = 3;
                break;
            case 534:
                xOffset = int(guiSize.x * (-50.0/100))-120;
                yOffset = int(guiSize.y * (0.0/100))+11;
                layer = 4;
                break;
            case 535:
                xOffset = int(guiSize.x * (-50.0/100))+210;
                yOffset = int(guiSize.y * (0.0/100))+12;
                layer = 1;
                break;
            case 536:
                xOffset = int(guiSize.x * (-50.0/100))+209;
                yOffset = int(guiSize.y * (0.0/100))+11;
                layer = 2;
                break;
            case 537:
                xOffset = int(guiSize.x * (-50.0/100))+189;
                yOffset = int(guiSize.y * (0.0/100))+10;
                layer = 3;
                break;
            case 538:
                xOffset = int(guiSize.x * (-50.0/100))+189;
                yOffset = int(guiSize.y * (0.0/100))+13;
                layer = 3;
                break;
            case 539:
                xOffset = int(guiSize.x * (-50.0/100))+220;
                yOffset = int(guiSize.y * (0.0/100))+11;
                layer = 4;
                break;
            case 540:
                xOffset = int(guiSize.x * (-50.0/100))-138;
                yOffset = int(guiSize.y * (0.0/100))+73;
                layer = 1;
                break;
            case 541:
                xOffset = int(guiSize.x * (-50.0/100))-136;
                yOffset = int(guiSize.y * (0.0/100))+75;
                layer = 2;
                break;
            case 542:
                xOffset = int(guiSize.x * (-50.0/100))-138;
                yOffset = int(guiSize.y * (0.0/100))+73;
                layer = 3;
                break;
            case 543:
                xOffset = int(guiSize.x * (-50.0/100))-138;
                yOffset = int(guiSize.y * (0.0/100))+73;
                layer = 4;
                break;
            case 544:
                xOffset = int(guiSize.x * (-50.0/100))-146;
                yOffset = int(guiSize.y * (0.0/100))+69;
                layer = 5;
                break;
            case 545:
                xOffset = int(guiSize.x * (-50.0/100))-146;
                yOffset = int(guiSize.y * (0.0/100))+72;
                layer = 5;
                break;
            case 546:
                xOffset = int(guiSize.x * (-50.0/100))-111;
                yOffset = int(guiSize.y * (0.0/100))+73;
                layer = 6;
                break;
            case 547:
                xOffset = int(guiSize.x * (-50.0/100))-109;
                yOffset = int(guiSize.y * (0.0/100))+75;
                layer = 7;
                break;
            case 548:
                xOffset = int(guiSize.x * (-50.0/100))-111;
                yOffset = int(guiSize.y * (0.0/100))+73;
                layer = 8;
                break;
            case 549:
                xOffset = int(guiSize.x * (-50.0/100))-111;
                yOffset = int(guiSize.y * (0.0/100))+73;
                layer = 9;
                break;
            case 550:
                xOffset = int(guiSize.x * (-50.0/100))-119;
                yOffset = int(guiSize.y * (0.0/100))+69;
                layer = 10;
                break;
            case 551:
                xOffset = int(guiSize.x * (-50.0/100))-119;
                yOffset = int(guiSize.y * (0.0/100))+72;
                layer = 10;
                break;
            case 552:
                xOffset = int(guiSize.x * (-50.0/100))-84;
                yOffset = int(guiSize.y * (0.0/100))+73;
                layer = 11;
                break;
            case 553:
                xOffset = int(guiSize.x * (-50.0/100))-82;
                yOffset = int(guiSize.y * (0.0/100))+75;
                layer = 12;
                break;
            case 554:
                xOffset = int(guiSize.x * (-50.0/100))-84;
                yOffset = int(guiSize.y * (0.0/100))+73;
                layer = 13;
                break;
            case 555:
                xOffset = int(guiSize.x * (-50.0/100))-84;
                yOffset = int(guiSize.y * (0.0/100))+73;
                layer = 14;
                break;
            case 556:
                xOffset = int(guiSize.x * (-50.0/100))-92;
                yOffset = int(guiSize.y * (0.0/100))+69;
                layer = 15;
                break;
            case 557:
                xOffset = int(guiSize.x * (-50.0/100))-92;
                yOffset = int(guiSize.y * (0.0/100))+72;
                layer = 15;
                break;
            case 558:
                xOffset = int(guiSize.x * (-50.0/100))-57;
                yOffset = int(guiSize.y * (0.0/100))+73;
                layer = 16;
                break;
            case 559:
                xOffset = int(guiSize.x * (-50.0/100))-55;
                yOffset = int(guiSize.y * (0.0/100))+75;
                layer = 17;
                break;
            case 560:
                xOffset = int(guiSize.x * (-50.0/100))-57;
                yOffset = int(guiSize.y * (0.0/100))+73;
                layer = 18;
                break;
            case 561:
                xOffset = int(guiSize.x * (-50.0/100))-57;
                yOffset = int(guiSize.y * (0.0/100))+73;
                layer = 19;
                break;
            case 562:
                xOffset = int(guiSize.x * (-50.0/100))-65;
                yOffset = int(guiSize.y * (0.0/100))+69;
                layer = 20;
                break;
            case 563:
                xOffset = int(guiSize.x * (-50.0/100))-65;
                yOffset = int(guiSize.y * (0.0/100))+72;
                layer = 20;
                break;
            case 564:
                xOffset = int(guiSize.x * (-50.0/100))-30;
                yOffset = int(guiSize.y * (0.0/100))+73;
                layer = 21;
                break;
            case 565:
                xOffset = int(guiSize.x * (-50.0/100))-28;
                yOffset = int(guiSize.y * (0.0/100))+75;
                layer = 22;
                break;
            case 566:
                xOffset = int(guiSize.x * (-50.0/100))-30;
                yOffset = int(guiSize.y * (0.0/100))+73;
                layer = 23;
                break;
            case 567:
                xOffset = int(guiSize.x * (-50.0/100))-30;
                yOffset = int(guiSize.y * (0.0/100))+73;
                layer = 24;
                break;
            case 568:
                xOffset = int(guiSize.x * (-50.0/100))-38;
                yOffset = int(guiSize.y * (0.0/100))+69;
                layer = 25;
                break;
            case 569:
                xOffset = int(guiSize.x * (-50.0/100))-38;
                yOffset = int(guiSize.y * (0.0/100))+72;
                layer = 25;
                break;
            case 570:
                xOffset = int(guiSize.x * (-50.0/100))-3;
                yOffset = int(guiSize.y * (0.0/100))+73;
                layer = 26;
                break;
            case 571:
                xOffset = int(guiSize.x * (-50.0/100))-1;
                yOffset = int(guiSize.y * (0.0/100))+75;
                layer = 27;
                break;
            case 572:
                xOffset = int(guiSize.x * (-50.0/100))-3;
                yOffset = int(guiSize.y * (0.0/100))+73;
                layer = 28;
                break;
            case 573:
                xOffset = int(guiSize.x * (-50.0/100))-3;
                yOffset = int(guiSize.y * (0.0/100))+73;
                layer = 29;
                break;
            case 574:
                xOffset = int(guiSize.x * (-50.0/100))-11;
                yOffset = int(guiSize.y * (0.0/100))+69;
                layer = 30;
                break;
            case 575:
                xOffset = int(guiSize.x * (-50.0/100))-11;
                yOffset = int(guiSize.y * (0.0/100))+72;
                layer = 30;
                break;
            case 576:
                xOffset = int(guiSize.x * (-50.0/100))-144;
                yOffset = int(guiSize.y * (0.0/100))+59;
                layer = 31;
                break;
            case 577:
                xOffset = int(guiSize.x * (-50.0/100))-117;
                yOffset = int(guiSize.y * (0.0/100))+59;
                layer = 32;
                break;
            case 578:
                xOffset = int(guiSize.x * (-50.0/100))-90;
                yOffset = int(guiSize.y * (0.0/100))+59;
                layer = 33;
                break;
            case 579:
                xOffset = int(guiSize.x * (-50.0/100))-63;
                yOffset = int(guiSize.y * (0.0/100))+59;
                layer = 34;
                break;
            case 580:
                xOffset = int(guiSize.x * (-50.0/100))-36;
                yOffset = int(guiSize.y * (0.0/100))+59;
                layer = 35;
                break;
            case 581:
                xOffset = int(guiSize.x * (-50.0/100))-9;
                yOffset = int(guiSize.y * (0.0/100))+59;
                layer = 36;
                break;
            case 582:
                xOffset = int(guiSize.x * (-50.0/100))-7;
                yOffset = int(guiSize.y * (0.0/100))+48;
                layer = 1;
                break;
            case 583:
                xOffset = int(guiSize.x * (-50.0/100))-7;
                yOffset = int(guiSize.y * (0.0/100))+51;
                layer = 1;
                break;
            case 584:
                xOffset = int(guiSize.x * (-50.0/100))+4;
                yOffset = int(guiSize.y * (0.0/100))+49;
                layer = 2;
                break;
            case 585:
                xOffset = int(guiSize.x * (-50.0/100))+25;
                yOffset = int(guiSize.y * (0.0/100))+48;
                layer = 1;
                break;
            case 586:
                xOffset = int(guiSize.x * (-50.0/100))+25;
                yOffset = int(guiSize.y * (0.0/100))+51;
                layer = 1;
                break;
            case 587:
                xOffset = int(guiSize.x * (-50.0/100))+36;
                yOffset = int(guiSize.y * (0.0/100))+49;
                layer = 2;
                break;
            case 588:
                xOffset = int(guiSize.x * (-50.0/100))+57;
                yOffset = int(guiSize.y * (0.0/100))+48;
                layer = 1;
                break;
            case 589:
                xOffset = int(guiSize.x * (-50.0/100))+57;
                yOffset = int(guiSize.y * (0.0/100))+51;
                layer = 1;
                break;
            case 590:
                xOffset = int(guiSize.x * (-50.0/100))+68;
                yOffset = int(guiSize.y * (0.0/100))+49;
                layer = 2;
                break;
            case 591:
                xOffset = int(guiSize.x * (-50.0/100))+89;
                yOffset = int(guiSize.y * (0.0/100))+48;
                layer = 1;
                break;
            case 592:
                xOffset = int(guiSize.x * (-50.0/100))+89;
                yOffset = int(guiSize.y * (0.0/100))+51;
                layer = 1;
                break;
            case 593:
                xOffset = int(guiSize.x * (-50.0/100))+100;
                yOffset = int(guiSize.y * (0.0/100))+49;
                layer = 2;
                break;
            case 594:
                xOffset = int(guiSize.x * (-50.0/100))+209;
                yOffset = int(guiSize.y * (0.0/100))+11;
                layer = 3;
                break;
            case 595:
                xOffset = int(guiSize.x * (-50.0/100))+189;
                yOffset = int(guiSize.y * (0.0/100))+10;
                layer = 4;
                break;
            case 596:
                xOffset = int(guiSize.x * (-50.0/100))+189;
                yOffset = int(guiSize.y * (0.0/100))+13;
                layer = 4;
                break;
            case 597:
                xOffset = int(guiSize.x * (-50.0/100))+220;
                yOffset = int(guiSize.y * (0.0/100))+11;
                layer = 5;
                break;
            case 598:
                xOffset = int(guiSize.x * (-50.0/100))-120;
                yOffset = int(guiSize.y * (0.0/100))+11;
                layer = 3;
                break;
            case 599:
                xOffset = int(guiSize.x * (-50.0/100))+220;
                yOffset = int(guiSize.y * (0.0/100))+22;
                layer = 3;
                break;
            case 600:
                xOffset = int(guiSize.x * (-50.0/100))-143;
                yOffset = int(guiSize.y * (0.0/100))+8;
                layer = 1;
                break;
            case 601:
                xOffset = int(guiSize.x * (-50.0/100))-143;
                yOffset = int(guiSize.y * (0.0/100))+11;
                layer = 1;
                break;
            case 602:
                xOffset = int(guiSize.x * (-50.0/100))-132;
                yOffset = int(guiSize.y * (0.0/100))+9;
                layer = 2;
                break;
            case 603:
                xOffset = int(guiSize.x * (-50.0/100))-109;
                yOffset = int(guiSize.y * (0.0/100))+8;
                layer = 1;
                break;
            case 604:
                xOffset = int(guiSize.x * (-50.0/100))-109;
                yOffset = int(guiSize.y * (0.0/100))+11;
                layer = 1;
                break;
            case 605:
                xOffset = int(guiSize.x * (-50.0/100))-98;
                yOffset = int(guiSize.y * (0.0/100))+9;
                layer = 2;
                break;
            case 606:
                xOffset = int(guiSize.x * (-50.0/100))-143;
                yOffset = int(guiSize.y * (0.0/100))+28;
                layer = 1;
                break;
            case 607:
                xOffset = int(guiSize.x * (-50.0/100))-143;
                yOffset = int(guiSize.y * (0.0/100))+31;
                layer = 1;
                break;
            case 608:
                xOffset = int(guiSize.x * (-50.0/100))-132;
                yOffset = int(guiSize.y * (0.0/100))+29;
                layer = 2;
                break;
            case 609:
                xOffset = int(guiSize.x * (-50.0/100))-109;
                yOffset = int(guiSize.y * (0.0/100))+28;
                layer = 1;
                break;
            case 610:
                xOffset = int(guiSize.x * (-50.0/100))-109;
                yOffset = int(guiSize.y * (0.0/100))+31;
                layer = 1;
                break;
            case 611:
                xOffset = int(guiSize.x * (-50.0/100))-98;
                yOffset = int(guiSize.y * (0.0/100))+29;
                layer = 2;
                break;
            case 612:
                xOffset = int(guiSize.x * (-50.0/100))-143;
                yOffset = int(guiSize.y * (0.0/100))+48;
                layer = 1;
                break;
            case 613:
                xOffset = int(guiSize.x * (-50.0/100))-143;
                yOffset = int(guiSize.y * (0.0/100))+51;
                layer = 1;
                break;
            case 614:
                xOffset = int(guiSize.x * (-50.0/100))-132;
                yOffset = int(guiSize.y * (0.0/100))+49;
                layer = 2;
                break;
            case 615:
                xOffset = int(guiSize.x * (-50.0/100))-109;
                yOffset = int(guiSize.y * (0.0/100))+48;
                layer = 1;
                break;
            case 616:
                xOffset = int(guiSize.x * (-50.0/100))-109;
                yOffset = int(guiSize.y * (0.0/100))+51;
                layer = 1;
                break;
            case 617:
                xOffset = int(guiSize.x * (-50.0/100))-98;
                yOffset = int(guiSize.y * (0.0/100))+49;
                layer = 2;
                break;
            case 618:
                xOffset = int(guiSize.x * (-50.0/100))-52;
                yOffset = int(guiSize.y * (0.0/100))+50;
                layer = 1;
                break;
            case 619:
                xOffset = int(guiSize.x * (-50.0/100))-53;
                yOffset = int(guiSize.y * (0.0/100))+49;
                layer = 2;
                break;
            case 620:
                xOffset = int(guiSize.x * (-50.0/100))-42;
                yOffset = int(guiSize.y * (0.0/100))+49;
                layer = 3;
                break;
            case 621:
                xOffset = int(guiSize.x * (-50.0/100))-10;
                yOffset = int(guiSize.y * (0.0/100))+50;
                layer = 1;
                break;
            case 622:
                xOffset = int(guiSize.x * (-50.0/100))-11;
                yOffset = int(guiSize.y * (0.0/100))+49;
                layer = 2;
                break;
            case 623:
                xOffset = int(guiSize.x * (-50.0/100));
                yOffset = int(guiSize.y * (0.0/100))+49;
                layer = 3;
                break;
            case 624:
                xOffset = int(guiSize.x * (-50.0/100))+90;
                yOffset = int(guiSize.y * (0.0/100))+50;
                layer = 1;
                break;
            case 625:
                xOffset = int(guiSize.x * (-50.0/100))+89;
                yOffset = int(guiSize.y * (0.0/100))+49;
                layer = 2;
                break;
            case 626:
                xOffset = int(guiSize.x * (-50.0/100))+89;
                yOffset = int(guiSize.y * (0.0/100))+49;
                layer = 3;
                break;
            case 627:
                xOffset = int(guiSize.x * (-50.0/100))+69;
                yOffset = int(guiSize.y * (0.0/100))+48;
                layer = 4;
                break;
            case 628:
                xOffset = int(guiSize.x * (-50.0/100))+69;
                yOffset = int(guiSize.y * (0.0/100))+51;
                layer = 4;
                break;
            case 629:
                xOffset = int(guiSize.x * (-50.0/100))+100;
                yOffset = int(guiSize.y * (0.0/100))+49;
                layer = 5;
                break;
            case 630:
                xOffset = int(guiSize.x * (-50.0/100))-10;
                yOffset = int(guiSize.y * (0.0/100))+39;
                layer = 1;
                break;
            case 631:
                xOffset = int(guiSize.x * (-50.0/100))-11;
                yOffset = int(guiSize.y * (0.0/100))+38;
                layer = 2;
                break;
            case 632:
                xOffset = int(guiSize.x * (-50.0/100))-31;
                yOffset = int(guiSize.y * (0.0/100))+37;
                layer = 3;
                break;
            case 633:
                xOffset = int(guiSize.x * (-50.0/100))-31;
                yOffset = int(guiSize.y * (0.0/100))+40;
                layer = 3;
                break;
            case 634:
                xOffset = int(guiSize.x * (-50.0/100));
                yOffset = int(guiSize.y * (0.0/100))+38;
                layer = 4;
                break;
            case 635:
                xOffset = int(guiSize.x * (-50.0/100))+90;
                yOffset = int(guiSize.y * (0.0/100))+39;
                layer = 1;
                break;
            case 636:
                xOffset = int(guiSize.x * (-50.0/100))+89;
                yOffset = int(guiSize.y * (0.0/100))+38;
                layer = 2;
                break;
            case 637:
                xOffset = int(guiSize.x * (-50.0/100))+69;
                yOffset = int(guiSize.y * (0.0/100))+37;
                layer = 3;
                break;
            case 638:
                xOffset = int(guiSize.x * (-50.0/100))+69;
                yOffset = int(guiSize.y * (0.0/100))+40;
                layer = 3;
                break;
            case 639:
                xOffset = int(guiSize.x * (-50.0/100))+100;
                yOffset = int(guiSize.y * (0.0/100))+38;
                layer = 4;
                break;
            case 640:
                xOffset = int(guiSize.x * (-50.0/100))-10;
                yOffset = int(guiSize.y * (0.0/100))+65;
                layer = 1;
                break;
            case 641:
                xOffset = int(guiSize.x * (-50.0/100))-10;
                yOffset = int(guiSize.y * (0.0/100))+65;
                layer = 2;
                break;
            case 642:
                xOffset = int(guiSize.x * (-50.0/100))-5;
                yOffset = int(guiSize.y * (0.0/100))+67;
                layer = 3;
                break;
            case 643:
                xOffset = int(guiSize.x * (-50.0/100))+10;
                yOffset = int(guiSize.y * (0.0/100))+65;
                layer = 1;
                break;
            case 644:
                xOffset = int(guiSize.x * (-50.0/100))+10;
                yOffset = int(guiSize.y * (0.0/100))+65;
                layer = 2;
                break;
            case 645:
                xOffset = int(guiSize.x * (-50.0/100))+15;
                yOffset = int(guiSize.y * (0.0/100))+67;
                layer = 3;
                break;
            case 646:
                xOffset = int(guiSize.x * (-50.0/100))-10;
                yOffset = int(guiSize.y * (0.0/100))+55;
                layer = 1;
                break;
            case 647:
                xOffset = int(guiSize.x * (-50.0/100))-10;
                yOffset = int(guiSize.y * (0.0/100))+55;
                layer = 2;
                break;
            case 648:
                xOffset = int(guiSize.x * (-50.0/100))-5;
                yOffset = int(guiSize.y * (0.0/100))+57;
                layer = 3;
                break;
            case 649:
                xOffset = int(guiSize.x * (-50.0/100))+10;
                yOffset = int(guiSize.y * (0.0/100))+55;
                layer = 1;
                break;
            case 650:
                xOffset = int(guiSize.x * (-50.0/100))+10;
                yOffset = int(guiSize.y * (0.0/100))+55;
                layer = 2;
                break;
            case 651:
                xOffset = int(guiSize.x * (-50.0/100))+15;
                yOffset = int(guiSize.y * (0.0/100))+57;
                layer = 3;
                break;
            case 652:
                xOffset = int(guiSize.x * (-0.0/100))-25;
                yOffset = int(guiSize.y * (100.0/100))-25;
                layer = 1;
                break;
            case 653:
                xOffset = int(guiSize.x * (-0.0/100))-27;
                yOffset = int(guiSize.y * (100.0/100))-37;
                layer = 2;
                break;
            case 654:
                xOffset = int(guiSize.x * (-0.0/100))-28;
                yOffset = int(guiSize.y * (100.0/100))-45;
                layer = 3;
                outlined = true;
                break;
            case 655:
                xOffset = int(guiSize.x * (-0.0/100))-28;
                yOffset = int(guiSize.y * (100.0/100))-42;
                layer = 3;
                outlined = true;
                break;
            case 656:
                xOffset = int(guiSize.x * (-0.0/100))-27;
                yOffset = int(guiSize.y * (100.0/100))-31;
                layer = 4;
                break;
            case 657:
                xOffset = int(guiSize.x * (-0.0/100))-28;
                yOffset = int(guiSize.y * (100.0/100))-21;
                layer = 5;
                outlined = true;
                break;
            case 658:
                xOffset = int(guiSize.x * (-0.0/100))-28;
                yOffset = int(guiSize.y * (100.0/100))-18;
                layer = 5;
                outlined = true;
                break;
            case 659:
                xOffset = int(guiSize.x * (-50.0/100))-11;
                yOffset = int(guiSize.y * (0.0/100))+50;
                layer = 1;
                break;
            case 660:
                xOffset = int(guiSize.x * (-50.0/100))-12;
                yOffset = int(guiSize.y * (0.0/100))+49;
                layer = 2;
                break;
            case 661:
                xOffset = int(guiSize.x * (-50.0/100))-1;
                yOffset = int(guiSize.y * (0.0/100))+49;
                layer = 3;
                break;
            case 662:
                xOffset = int(guiSize.x * (-50.0/100))+91;
                yOffset = int(guiSize.y * (0.0/100))+39;
                layer = 1;
                break;
            case 663:
                xOffset = int(guiSize.x * (-50.0/100))+90;
                yOffset = int(guiSize.y * (0.0/100))+38;
                layer = 2;
                break;
            case 664:
                xOffset = int(guiSize.x * (-50.0/100))+90;
                yOffset = int(guiSize.y * (0.0/100))+38;
                layer = 3;
                break;
            case 665:
                xOffset = int(guiSize.x * (-50.0/100))+70;
                yOffset = int(guiSize.y * (0.0/100))+37;
                layer = 4;
                break;
            case 666:
                xOffset = int(guiSize.x * (-50.0/100))+70;
                yOffset = int(guiSize.y * (0.0/100))+40;
                layer = 4;
                break;
            case 667:
                xOffset = int(guiSize.x * (-50.0/100))+101;
                yOffset = int(guiSize.y * (0.0/100))+38;
                layer = 5;
                break;
            case 668:
                xOffset = int(guiSize.x * (-50.0/100))-11;
                yOffset = int(guiSize.y * (0.0/100))+39;
                layer = 1;
                break;
            case 669:
                xOffset = int(guiSize.x * (-50.0/100))-12;
                yOffset = int(guiSize.y * (0.0/100))+38;
                layer = 2;
                break;
            case 670:
                xOffset = int(guiSize.x * (-50.0/100))-1;
                yOffset = int(guiSize.y * (0.0/100))+38;
                layer = 3;
                break;
            case 671:
                xOffset = int(guiSize.x * (-50.0/100))+91;
                yOffset = int(guiSize.y * (0.0/100))+50;
                layer = 1;
                break;
            case 672:
                xOffset = int(guiSize.x * (-50.0/100))+90;
                yOffset = int(guiSize.y * (0.0/100))+49;
                layer = 2;
                break;
            case 673:
                xOffset = int(guiSize.x * (-50.0/100))+101;
                yOffset = int(guiSize.y * (0.0/100))+49;
                layer = 3;
                break;
            case 674:
                xOffset = int(guiSize.x * (-50.0/100));
                yOffset = int(guiSize.y * (0.0/100))+85;
                layer = 1;
                outlined = true;
                break;
            case 675:
                xOffset = int(guiSize.x * (-50.0/100));
                yOffset = int(guiSize.y * (0.0/100))+88;
                layer = 1;
                outlined = true;
                break;
            case 676:
                xOffset = int(guiSize.x * (-50.0/100));
                yOffset = int(guiSize.y * (0.0/100))+75;
                layer = 1;
                outlined = true;
                break;
            case 677:
                xOffset = int(guiSize.x * (-50.0/100));
                yOffset = int(guiSize.y * (0.0/100))+78;
                layer = 1;
                outlined = true;
                break;
            case 678:
                xOffset = int(guiSize.x * (-50.0/100));
                yOffset = int(guiSize.y * (0.0/100))+65;
                layer = 1;
                outlined = true;
                break;
            case 679:
                xOffset = int(guiSize.x * (-50.0/100));
                yOffset = int(guiSize.y * (0.0/100))+68;
                layer = 1;
                outlined = true;
                break;
            case 680:
                xOffset = int(guiSize.x * (-100.0/100))+20;
                yOffset = int(guiSize.y * (100.0/100))-20;
                layer = 1;
                break;
            case 681:
                xOffset = int(guiSize.x * (-100.0/100))-80;
                yOffset = int(guiSize.y * (100.0/100))-22;
                layer = 2;
                outlined = true;
                break;
            case 682:
                xOffset = int(guiSize.x * (-100.0/100))-80;
                yOffset = int(guiSize.y * (100.0/100))-19;
                layer = 2;
                outlined = true;
                break;
            case 683:
                xOffset = int(guiSize.x * (-100.0/100))-60;
                yOffset = int(guiSize.y * (100.0/100))-22;
                layer = 3;
                outlined = true;
                break;
            case 684:
                xOffset = int(guiSize.x * (-100.0/100))-60;
                yOffset = int(guiSize.y * (100.0/100))-19;
                layer = 3;
                outlined = true;
                break;
            case 685:
                xOffset = int(guiSize.x * (-100.0/100))-66;
                yOffset = int(guiSize.y * (100.0/100))-22;
                layer = 4;
                break;
            case 686:
                xOffset = int(guiSize.x * (-100.0/100))+20;
                yOffset = int(guiSize.y * (100.0/100))-40;
                layer = 1;
                break;
            case 687:
                xOffset = int(guiSize.x * (-100.0/100))-80;
                yOffset = int(guiSize.y * (100.0/100))-42;
                layer = 2;
                outlined = true;
                break;
            case 688:
                xOffset = int(guiSize.x * (-100.0/100))-80;
                yOffset = int(guiSize.y * (100.0/100))-39;
                layer = 2;
                outlined = true;
                break;
            case 689:
                xOffset = int(guiSize.x * (-100.0/100))-60;
                yOffset = int(guiSize.y * (100.0/100))-42;
                layer = 3;
                outlined = true;
                break;
            case 690:
                xOffset = int(guiSize.x * (-100.0/100))-60;
                yOffset = int(guiSize.y * (100.0/100))-39;
                layer = 3;
                outlined = true;
                break;
            case 691:
                xOffset = int(guiSize.x * (-100.0/100))-66;
                yOffset = int(guiSize.y * (100.0/100))-42;
                layer = 4;
                break;
            case 692:
                xOffset = int(guiSize.x * (-100.0/100))+20;
                yOffset = int(guiSize.y * (100.0/100))-60;
                layer = 1;
                break;
            case 693:
                xOffset = int(guiSize.x * (-100.0/100))-80;
                yOffset = int(guiSize.y * (100.0/100))-62;
                layer = 2;
                outlined = true;
                break;
            case 694:
                xOffset = int(guiSize.x * (-100.0/100))-80;
                yOffset = int(guiSize.y * (100.0/100))-59;
                layer = 2;
                outlined = true;
                break;
            case 695:
                xOffset = int(guiSize.x * (-100.0/100))-60;
                yOffset = int(guiSize.y * (100.0/100))-62;
                layer = 3;
                outlined = true;
                break;
            case 696:
                xOffset = int(guiSize.x * (-100.0/100))-60;
                yOffset = int(guiSize.y * (100.0/100))-59;
                layer = 3;
                outlined = true;
                break;
            case 697:
                xOffset = int(guiSize.x * (-100.0/100))-66;
                yOffset = int(guiSize.y * (100.0/100))-62;
                layer = 4;
                break;
            case 698:
                xOffset = int(guiSize.x * (-100.0/100))+20;
                yOffset = int(guiSize.y * (100.0/100))-80;
                layer = 1;
                break;
            case 699:
                xOffset = int(guiSize.x * (-100.0/100))-80;
                yOffset = int(guiSize.y * (100.0/100))-82;
                layer = 2;
                outlined = true;
                break;
            case 700:
                xOffset = int(guiSize.x * (-100.0/100))-80;
                yOffset = int(guiSize.y * (100.0/100))-79;
                layer = 2;
                outlined = true;
                break;
            case 701:
                xOffset = int(guiSize.x * (-100.0/100))-60;
                yOffset = int(guiSize.y * (100.0/100))-82;
                layer = 3;
                outlined = true;
                break;
            case 702:
                xOffset = int(guiSize.x * (-100.0/100))-60;
                yOffset = int(guiSize.y * (100.0/100))-79;
                layer = 3;
                outlined = true;
                break;
            case 703:
                xOffset = int(guiSize.x * (-100.0/100))-66;
                yOffset = int(guiSize.y * (100.0/100))-82;
                layer = 4;
                break;
            case 704:
                xOffset = int(guiSize.x * (-100.0/100))+20;
                yOffset = int(guiSize.y * (100.0/100))-100;
                layer = 1;
                break;
            case 705:
                xOffset = int(guiSize.x * (-100.0/100))-80;
                yOffset = int(guiSize.y * (100.0/100))-102;
                layer = 2;
                outlined = true;
                break;
            case 706:
                xOffset = int(guiSize.x * (-100.0/100))-80;
                yOffset = int(guiSize.y * (100.0/100))-99;
                layer = 2;
                outlined = true;
                break;
            case 707:
                xOffset = int(guiSize.x * (-100.0/100))-60;
                yOffset = int(guiSize.y * (100.0/100))-102;
                layer = 3;
                outlined = true;
                break;
            case 708:
                xOffset = int(guiSize.x * (-100.0/100))-60;
                yOffset = int(guiSize.y * (100.0/100))-99;
                layer = 3;
                outlined = true;
                break;
            case 709:
                xOffset = int(guiSize.x * (-100.0/100))-66;
                yOffset = int(guiSize.y * (100.0/100))-102;
                layer = 4;
                break;
        }
        if (true) {
            pos.y -= (happyId * 1000.0) + 500.0 - 64.0;
            pos.x -= guiSize.x * 0.5;
            pos -= vec3(xOffset, yOffset, 0.0);
            pos.z += float(layer);
        }
    }

#endif
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
    vertexColor = Color * sample_lightmap(Sampler2, UV2);
#else
    vertexColor = Color;
#endif
    texCoord0 = UV0;

    vec4 vcolor = vertexColor;
    #moj_import <minecraft:minimap/vertex_main.glsl>
    minimapCarrierPosition = Position;
    #moj_import <minecraft:gps3d/vertex_main.glsl>
}

#endif
