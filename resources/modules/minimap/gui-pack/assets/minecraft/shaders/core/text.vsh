#version 330

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:sample_lightmap.glsl>
#endif

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

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
#define EH_OFFSET 29

float get_id(float offset) {
    if (offset <= 0.0) return 0.0;
    return trunc(offset / 1000.0);
}


flat out int sfKind;
flat out vec3 sfTiming;
flat out ivec2 sfAtlasOrigin;
out vec2 sfLocal;
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

void main() {
    #moj_import <minecraft:guiminimap/vertex_main.glsl>
    #moj_import <minecraft:spellfx/vertex_main.glsl>

    vec3 pos = Position;
#ifdef IS_GUI


    vec2 pixel = vec2(ProjMat[0][0], ProjMat[1][1]) / 2.0;
    int guiScale = max(1, int(round(pixel.x * ScreenSize.x)));
    vec2 guiSize = ScreenSize / guiScale;

    float id = get_id((round(EH_OFFSET - Position.y)) * -1.0);

    if (id > 99.0 && id < 500.0 && Color.a != 0.0) {
        float yOffset = 0.0;
        float xOffset = 0.0;
        float layer = 0.0;
        vec2 scale = vec2(1.0, 1.0);
        bool outlined = false;

        switch (int(id)) {
            case 100:
                xOffset = int(guiSize.x * (-7.0/100))-53;
                yOffset = int(guiSize.y * (1.0/100))+10;
                layer = 0.0;
                outlined = true;
                break;
            case 101:
                xOffset = int(guiSize.x * (-7.0/100))-69;
                yOffset = int(guiSize.y * (1.0/100));
                layer = 0.0;
                outlined = true;
                break;
            case 102:
                xOffset = int(guiSize.x * (-0.3/100))-65;
                yOffset = int(guiSize.y * (100.0/100))-26;
                layer = 0.0;
                break;
            case 103:
                xOffset = int(guiSize.x * (-0.3/100))-125;
                yOffset = int(guiSize.y * (100.0/100))-26;
                layer = 0.0;
                break;
            case 104:
                xOffset = int(guiSize.x * (-0.3/100))-65;
                yOffset = int(guiSize.y * (100.0/100))-86;
                layer = 0.0;
                break;
            case 105:
                xOffset = int(guiSize.x * (-0.3/100))-125;
                yOffset = int(guiSize.y * (100.0/100))-86;
                layer = 0.0;
                break;
            case 106:
                xOffset = int(guiSize.x * (-0.3/100))-102;
                yOffset = int(guiSize.y * (100.0/100))-76;
                layer = 0.0;
                break;
            case 107:
                xOffset = int(guiSize.x * (-50.0/100));
                yOffset = int(guiSize.y * (0.0/100))+44;
                layer = 6.0;
                break;
            case 108:
                xOffset = int(guiSize.x * (-50.0/100))-60;
                yOffset = int(guiSize.y * (0.0/100))+44;
                layer = 6.0;
                break;
            case 109:
                xOffset = int(guiSize.x * (-50.0/100))-120;
                yOffset = int(guiSize.y * (0.0/100))+44;
                layer = 6.0;
                break;
            case 110:
                xOffset = int(guiSize.x * (-50.0/100));
                yOffset = int(guiSize.y * (0.0/100))+46;
                layer = 4.0;
                break;
            case 111:
                xOffset = int(guiSize.x * (-50.0/100))-60;
                yOffset = int(guiSize.y * (0.0/100))+46;
                layer = 4.0;
                break;
            case 112:
                xOffset = int(guiSize.x * (-50.0/100))-120;
                yOffset = int(guiSize.y * (0.0/100))+46;
                layer = 4.0;
                break;
            case 113:
                xOffset = int(guiSize.x * (-50.0/100));
                yOffset = int(guiSize.y * (0.0/100))+44;
                layer = 8.0;
                break;
            case 114:
                xOffset = int(guiSize.x * (-0.3/100))-43;
                yOffset = int(guiSize.y * (100.0/100))-9;
                layer = 0.0;
                break;
            case 115:
                xOffset = int(guiSize.x * (-0.3/100))-55;
                yOffset = int(guiSize.y * (100.0/100))-49;
                layer = 12.0;
                break;
            case 116:
                xOffset = int(guiSize.x * (-0.3/100))-55;
                yOffset = int(guiSize.y * (100.0/100))-38;
                layer = 12.0;
                break;
            case 117:
                xOffset = int(guiSize.x * (-0.3/100))-55;
                yOffset = int(guiSize.y * (100.0/100))-61;
                layer = 12.0;
                break;
            case 118:
                xOffset = int(guiSize.x * (-0.3/100))-55;
                yOffset = int(guiSize.y * (100.0/100))-50;
                layer = 12.0;
                break;
            case 119:
                xOffset = int(guiSize.x * (-0.3/100))-55;
                yOffset = int(guiSize.y * (100.0/100))-73;
                layer = 12.0;
                break;
            case 120:
                xOffset = int(guiSize.x * (-0.3/100))-55;
                yOffset = int(guiSize.y * (100.0/100))-62;
                layer = 12.0;
                break;
            case 121:
                xOffset = int(guiSize.x * (-0.3/100))-55;
                yOffset = int(guiSize.y * (100.0/100))-85;
                layer = 12.0;
                break;
            case 122:
                xOffset = int(guiSize.x * (-0.3/100))-55;
                yOffset = int(guiSize.y * (100.0/100))-74;
                layer = 12.0;
                break;
            case 123:
                xOffset = int(guiSize.x * (-0.3/100))-55;
                yOffset = int(guiSize.y * (100.0/100))-52;
                layer = 14.0;
                break;
            case 124:
                xOffset = int(guiSize.x * (-0.3/100))-55;
                yOffset = int(guiSize.y * (100.0/100))-41;
                layer = 14.0;
                break;
            case 125:
                xOffset = int(guiSize.x * (-0.3/100))-55;
                yOffset = int(guiSize.y * (100.0/100))-64;
                layer = 14.0;
                break;
            case 126:
                xOffset = int(guiSize.x * (-0.3/100))-55;
                yOffset = int(guiSize.y * (100.0/100))-53;
                layer = 14.0;
                break;
            case 127:
                xOffset = int(guiSize.x * (-0.3/100))-55;
                yOffset = int(guiSize.y * (100.0/100))-76;
                layer = 14.0;
                break;
            case 128:
                xOffset = int(guiSize.x * (-0.3/100))-55;
                yOffset = int(guiSize.y * (100.0/100))-65;
                layer = 14.0;
                break;
            case 129:
                xOffset = int(guiSize.x * (-0.3/100))-55;
                yOffset = int(guiSize.y * (100.0/100))-88;
                layer = 14.0;
                break;
            case 130:
                xOffset = int(guiSize.x * (-0.3/100))-55;
                yOffset = int(guiSize.y * (100.0/100))-77;
                layer = 14.0;
                break;
            case 131:
                xOffset = int(guiSize.x * (-0.3/100))-205;
                yOffset = int(guiSize.y * (100.0/100))-37;
                layer = 8.0;
                break;
            case 132:
                xOffset = int(guiSize.x * (-0.3/100))-205;
                yOffset = int(guiSize.y * (100.0/100))-26;
                layer = 8.0;
                break;
            case 133:
                xOffset = int(guiSize.x * (-0.3/100))-247;
                yOffset = int(guiSize.y * (100.0/100))-14;
                layer = 0.0;
                break;
            case 134:
                xOffset = int(guiSize.x * (-0.3/100))-307;
                yOffset = int(guiSize.y * (100.0/100))-14;
                layer = 0.0;
                break;
            case 135:
                xOffset = int(guiSize.x * (-0.3/100))-367;
                yOffset = int(guiSize.y * (100.0/100))-14;
                layer = 0.0;
                break;
            case 136:
                xOffset = int(guiSize.x * (-0.3/100))-216;
                yOffset = int(guiSize.y * (100.0/100))-16;
                layer = 8.0;
                break;
            case 137:
                xOffset = int(guiSize.x * (-0.3/100))-237;
                yOffset = int(guiSize.y * (100.0/100))-37;
                layer = 0.0;
                break;
            case 138:
                xOffset = int(guiSize.x * (-0.3/100))-297;
                yOffset = int(guiSize.y * (100.0/100))-37;
                layer = 0.0;
                break;
            case 139:
                xOffset = int(guiSize.x * (-0.3/100))-357;
                yOffset = int(guiSize.y * (100.0/100))-37;
                layer = 0.0;
                break;
            case 140:
                xOffset = int(guiSize.x * (-0.3/100))-237;
                yOffset = int(guiSize.y * (100.0/100))-26;
                layer = 0.0;
                break;
            case 141:
                xOffset = int(guiSize.x * (-0.3/100))-297;
                yOffset = int(guiSize.y * (100.0/100))-26;
                layer = 0.0;
                break;
            case 142:
                xOffset = int(guiSize.x * (-0.3/100))-357;
                yOffset = int(guiSize.y * (100.0/100))-26;
                layer = 0.0;
                break;
            case 143:
                xOffset = int(guiSize.x * (-0.3/100))-243;
                yOffset = int(guiSize.y * (100.0/100))-27;
                layer = 0.0;
                break;
            case 144:
                xOffset = int(guiSize.x * (-0.3/100))-303;
                yOffset = int(guiSize.y * (100.0/100))-27;
                layer = 0.0;
                break;
            case 145:
                xOffset = int(guiSize.x * (-0.3/100))-363;
                yOffset = int(guiSize.y * (100.0/100))-27;
                layer = 0.0;
                break;
            case 146:
                xOffset = int(guiSize.x * (-0.3/100))-208;
                yOffset = int(guiSize.y * (100.0/100))-27;
                layer = 8.0;
                break;
            case 147:
                xOffset = int(guiSize.x * (-0.3/100))-196;
                yOffset = int(guiSize.y * (100.0/100))+1;
                layer = 0.0;
                break;
            case 148:
                xOffset = int(guiSize.x * (-0.3/100))-256;
                yOffset = int(guiSize.y * (100.0/100))+1;
                layer = 0.0;
                break;
            case 149:
                xOffset = int(guiSize.x * (-0.3/100))-316;
                yOffset = int(guiSize.y * (100.0/100))+1;
                layer = 0.0;
                break;
            case 150:
                xOffset = int(guiSize.x * (-0.3/100))-376;
                yOffset = int(guiSize.y * (100.0/100))+1;
                layer = 0.0;
                break;
            case 151:
                xOffset = int(guiSize.x * (-48.0/100))-8;
                yOffset = int(guiSize.y * (95.0/100))+3;
                layer = 0.0;
                outlined = true;
                break;
            case 152:
                xOffset = int(guiSize.x * (-94.0/100))-55;
                yOffset = int(guiSize.y * (95.0/100))-12;
                layer = 0.0;
                break;
            case 153:
                xOffset = int(guiSize.x * (-94.0/100))-55;
                yOffset = int(guiSize.y * (95.0/100));
                layer = 0.0;
                outlined = true;
                break;
            case 154:
                xOffset = int(guiSize.x * (-50.0/100));
                yOffset = int(guiSize.y * (89.0/100))-24;
                layer = 0.0;
                outlined = true;
                break;
            case 155:
                xOffset = int(guiSize.x * (-50.0/100));
                yOffset = int(guiSize.y * (89.0/100))-10;
                layer = 4.0;
                break;
            case 156:
                xOffset = int(guiSize.x * (-50.0/100))-60;
                yOffset = int(guiSize.y * (89.0/100))-10;
                layer = 4.0;
                break;
            case 157:
                xOffset = int(guiSize.x * (-50.0/100))-120;
                yOffset = int(guiSize.y * (89.0/100))-10;
                layer = 4.0;
                break;
            case 158:
                xOffset = int(guiSize.x * (-50.0/100));
                yOffset = int(guiSize.y * (89.0/100))-12;
                layer = 6.0;
                break;
            case 159:
                xOffset = int(guiSize.x * (-50.0/100))-60;
                yOffset = int(guiSize.y * (89.0/100))-12;
                layer = 6.0;
                break;
            case 160:
                xOffset = int(guiSize.x * (-50.0/100))-120;
                yOffset = int(guiSize.y * (89.0/100))-12;
                layer = 6.0;
                break;
            case 161:
                xOffset = int(guiSize.x * (-50.0/100));
                yOffset = int(guiSize.y * (89.0/100));
                layer = 0.0;
                outlined = true;
                break;
            case 162:
                xOffset = int(guiSize.x * (-50.0/100))+67;
                yOffset = int(guiSize.y * (0.0/100))+89;
                layer = 3.0;
                break;
            case 163:
                xOffset = int(guiSize.x * (-50.0/100))-12;
                yOffset = int(guiSize.y * (0.0/100))+77;
                layer = 3.0;
                break;
            case 164:
                xOffset = int(guiSize.x * (-50.0/100))+14;
                yOffset = int(guiSize.y * (0.0/100))+77;
                layer = 3.0;
                break;
            case 165:
                xOffset = int(guiSize.x * (-50.0/100))+74;
                yOffset = int(guiSize.y * (0.0/100))+95;
                layer = 1.0;
                break;
            case 166:
                xOffset = int(guiSize.x * (-50.0/100))+14;
                yOffset = int(guiSize.y * (0.0/100))+95;
                layer = 1.0;
                break;
            case 167:
                xOffset = int(guiSize.x * (-50.0/100))-46;
                yOffset = int(guiSize.y * (0.0/100))+95;
                layer = 1.0;
                break;
            case 168:
                xOffset = int(guiSize.x * (-50.0/100))-42;
                yOffset = int(guiSize.y * (0.0/100))+77;
                layer = 3.0;
                break;
            case 169:
                xOffset = int(guiSize.x * (-50.0/100))-12;
                yOffset = int(guiSize.y * (0.0/100))+90;
                layer = 3.0;
                break;
            case 170:
                xOffset = int(guiSize.x * (-50.0/100))+49;
                yOffset = int(guiSize.y * (0.0/100))+89;
                layer = 3.0;
                break;
            case 171:
                xOffset = int(guiSize.x * (-50.0/100))+56;
                yOffset = int(guiSize.y * (0.0/100))+95;
                layer = 1.0;
                break;
            case 172:
                xOffset = int(guiSize.x * (-50.0/100))-4;
                yOffset = int(guiSize.y * (0.0/100))+95;
                layer = 1.0;
                break;
            case 173:
                xOffset = int(guiSize.x * (-50.0/100))-24;
                yOffset = int(guiSize.y * (0.0/100))+77;
                layer = 3.0;
                break;
            case 174:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100));
                layer = 1.0;
                break;
            case 175:
                xOffset = int(guiSize.x * (-0.0/100))-66;
                yOffset = int(guiSize.y * (50.0/100));
                layer = 1.0;
                break;
            case 176:
                xOffset = int(guiSize.x * (-0.0/100))-126;
                yOffset = int(guiSize.y * (50.0/100));
                layer = 1.0;
                break;
            case 177:
                xOffset = int(guiSize.x * (-0.0/100))-8;
                yOffset = int(guiSize.y * (50.0/100))-11;
                layer = 4.0;
                break;
            case 178:
                xOffset = int(guiSize.x * (-0.0/100))-38;
                yOffset = int(guiSize.y * (50.0/100))-15;
                layer = 2.0;
                break;
            case 179:
                xOffset = int(guiSize.x * (-0.0/100))-98;
                yOffset = int(guiSize.y * (50.0/100))-15;
                layer = 2.0;
                break;
            case 180:
                xOffset = int(guiSize.x * (-0.0/100))-89;
                yOffset = int(guiSize.y * (50.0/100))-15;
                layer = 4.0;
                break;
            case 181:
                xOffset = int(guiSize.x * (-0.0/100))-38;
                yOffset = int(guiSize.y * (50.0/100))-24;
                layer = 2.0;
                break;
            case 182:
                xOffset = int(guiSize.x * (-0.0/100))-98;
                yOffset = int(guiSize.y * (50.0/100))-24;
                layer = 2.0;
                break;
            case 183:
                xOffset = int(guiSize.x * (-0.0/100))-89;
                yOffset = int(guiSize.y * (50.0/100))-24;
                layer = 4.0;
                break;
            case 184:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100));
                layer = 4.0;
                break;
            case 185:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-42;
                layer = 1.0;
                break;
            case 186:
                xOffset = int(guiSize.x * (-0.0/100))-66;
                yOffset = int(guiSize.y * (50.0/100))-42;
                layer = 1.0;
                break;
            case 187:
                xOffset = int(guiSize.x * (-0.0/100))-126;
                yOffset = int(guiSize.y * (50.0/100))-42;
                layer = 1.0;
                break;
            case 188:
                xOffset = int(guiSize.x * (-0.0/100))-8;
                yOffset = int(guiSize.y * (50.0/100))-53;
                layer = 4.0;
                break;
            case 189:
                xOffset = int(guiSize.x * (-0.0/100))-38;
                yOffset = int(guiSize.y * (50.0/100))-57;
                layer = 2.0;
                break;
            case 190:
                xOffset = int(guiSize.x * (-0.0/100))-98;
                yOffset = int(guiSize.y * (50.0/100))-57;
                layer = 2.0;
                break;
            case 191:
                xOffset = int(guiSize.x * (-0.0/100))-89;
                yOffset = int(guiSize.y * (50.0/100))-57;
                layer = 4.0;
                break;
            case 192:
                xOffset = int(guiSize.x * (-0.0/100))-38;
                yOffset = int(guiSize.y * (50.0/100))-66;
                layer = 2.0;
                break;
            case 193:
                xOffset = int(guiSize.x * (-0.0/100))-98;
                yOffset = int(guiSize.y * (50.0/100))-66;
                layer = 2.0;
                break;
            case 194:
                xOffset = int(guiSize.x * (-0.0/100))-89;
                yOffset = int(guiSize.y * (50.0/100))-66;
                layer = 4.0;
                break;
            case 195:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-42;
                layer = 4.0;
                break;
            case 196:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-84;
                layer = 1.0;
                break;
            case 197:
                xOffset = int(guiSize.x * (-0.0/100))-66;
                yOffset = int(guiSize.y * (50.0/100))-84;
                layer = 1.0;
                break;
            case 198:
                xOffset = int(guiSize.x * (-0.0/100))-126;
                yOffset = int(guiSize.y * (50.0/100))-84;
                layer = 1.0;
                break;
            case 199:
                xOffset = int(guiSize.x * (-0.0/100))-8;
                yOffset = int(guiSize.y * (50.0/100))-95;
                layer = 4.0;
                break;
            case 200:
                xOffset = int(guiSize.x * (-0.0/100))-38;
                yOffset = int(guiSize.y * (50.0/100))-99;
                layer = 2.0;
                break;
            case 201:
                xOffset = int(guiSize.x * (-0.0/100))-98;
                yOffset = int(guiSize.y * (50.0/100))-99;
                layer = 2.0;
                break;
            case 202:
                xOffset = int(guiSize.x * (-0.0/100))-89;
                yOffset = int(guiSize.y * (50.0/100))-99;
                layer = 4.0;
                break;
            case 203:
                xOffset = int(guiSize.x * (-0.0/100))-38;
                yOffset = int(guiSize.y * (50.0/100))-108;
                layer = 2.0;
                break;
            case 204:
                xOffset = int(guiSize.x * (-0.0/100))-98;
                yOffset = int(guiSize.y * (50.0/100))-108;
                layer = 2.0;
                break;
            case 205:
                xOffset = int(guiSize.x * (-0.0/100))-89;
                yOffset = int(guiSize.y * (50.0/100))-108;
                layer = 4.0;
                break;
            case 206:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-84;
                layer = 4.0;
                break;
            case 207:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-126;
                layer = 1.0;
                break;
            case 208:
                xOffset = int(guiSize.x * (-0.0/100))-66;
                yOffset = int(guiSize.y * (50.0/100))-126;
                layer = 1.0;
                break;
            case 209:
                xOffset = int(guiSize.x * (-0.0/100))-126;
                yOffset = int(guiSize.y * (50.0/100))-126;
                layer = 1.0;
                break;
            case 210:
                xOffset = int(guiSize.x * (-0.0/100))-8;
                yOffset = int(guiSize.y * (50.0/100))-137;
                layer = 4.0;
                break;
            case 211:
                xOffset = int(guiSize.x * (-0.0/100))-38;
                yOffset = int(guiSize.y * (50.0/100))-141;
                layer = 2.0;
                break;
            case 212:
                xOffset = int(guiSize.x * (-0.0/100))-98;
                yOffset = int(guiSize.y * (50.0/100))-141;
                layer = 2.0;
                break;
            case 213:
                xOffset = int(guiSize.x * (-0.0/100))-89;
                yOffset = int(guiSize.y * (50.0/100))-141;
                layer = 4.0;
                break;
            case 214:
                xOffset = int(guiSize.x * (-0.0/100))-38;
                yOffset = int(guiSize.y * (50.0/100))-150;
                layer = 2.0;
                break;
            case 215:
                xOffset = int(guiSize.x * (-0.0/100))-98;
                yOffset = int(guiSize.y * (50.0/100))-150;
                layer = 2.0;
                break;
            case 216:
                xOffset = int(guiSize.x * (-0.0/100))-89;
                yOffset = int(guiSize.y * (50.0/100))-150;
                layer = 4.0;
                break;
            case 217:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-126;
                layer = 4.0;
                break;
            case 218:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-168;
                layer = 1.0;
                break;
            case 219:
                xOffset = int(guiSize.x * (-0.0/100))-66;
                yOffset = int(guiSize.y * (50.0/100))-168;
                layer = 1.0;
                break;
            case 220:
                xOffset = int(guiSize.x * (-0.0/100))-126;
                yOffset = int(guiSize.y * (50.0/100))-168;
                layer = 1.0;
                break;
            case 221:
                xOffset = int(guiSize.x * (-0.0/100))-8;
                yOffset = int(guiSize.y * (50.0/100))-179;
                layer = 4.0;
                break;
            case 222:
                xOffset = int(guiSize.x * (-0.0/100))-38;
                yOffset = int(guiSize.y * (50.0/100))-183;
                layer = 2.0;
                break;
            case 223:
                xOffset = int(guiSize.x * (-0.0/100))-98;
                yOffset = int(guiSize.y * (50.0/100))-183;
                layer = 2.0;
                break;
            case 224:
                xOffset = int(guiSize.x * (-0.0/100))-89;
                yOffset = int(guiSize.y * (50.0/100))-183;
                layer = 4.0;
                break;
            case 225:
                xOffset = int(guiSize.x * (-0.0/100))-38;
                yOffset = int(guiSize.y * (50.0/100))-192;
                layer = 2.0;
                break;
            case 226:
                xOffset = int(guiSize.x * (-0.0/100))-98;
                yOffset = int(guiSize.y * (50.0/100))-192;
                layer = 2.0;
                break;
            case 227:
                xOffset = int(guiSize.x * (-0.0/100))-89;
                yOffset = int(guiSize.y * (50.0/100))-192;
                layer = 4.0;
                break;
            case 228:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-168;
                layer = 4.0;
                break;
            case 229:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-210;
                layer = 1.0;
                break;
            case 230:
                xOffset = int(guiSize.x * (-0.0/100))-66;
                yOffset = int(guiSize.y * (50.0/100))-210;
                layer = 1.0;
                break;
            case 231:
                xOffset = int(guiSize.x * (-0.0/100))-126;
                yOffset = int(guiSize.y * (50.0/100))-210;
                layer = 1.0;
                break;
            case 232:
                xOffset = int(guiSize.x * (-0.0/100))-8;
                yOffset = int(guiSize.y * (50.0/100))-221;
                layer = 4.0;
                break;
            case 233:
                xOffset = int(guiSize.x * (-0.0/100))-38;
                yOffset = int(guiSize.y * (50.0/100))-225;
                layer = 2.0;
                break;
            case 234:
                xOffset = int(guiSize.x * (-0.0/100))-98;
                yOffset = int(guiSize.y * (50.0/100))-225;
                layer = 2.0;
                break;
            case 235:
                xOffset = int(guiSize.x * (-0.0/100))-89;
                yOffset = int(guiSize.y * (50.0/100))-225;
                layer = 4.0;
                break;
            case 236:
                xOffset = int(guiSize.x * (-0.0/100))-38;
                yOffset = int(guiSize.y * (50.0/100))-234;
                layer = 2.0;
                break;
            case 237:
                xOffset = int(guiSize.x * (-0.0/100))-98;
                yOffset = int(guiSize.y * (50.0/100))-234;
                layer = 2.0;
                break;
            case 238:
                xOffset = int(guiSize.x * (-0.0/100))-89;
                yOffset = int(guiSize.y * (50.0/100))-234;
                layer = 4.0;
                break;
            case 239:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-210;
                layer = 4.0;
                break;
            case 240:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-252;
                layer = 1.0;
                break;
            case 241:
                xOffset = int(guiSize.x * (-0.0/100))-66;
                yOffset = int(guiSize.y * (50.0/100))-252;
                layer = 1.0;
                break;
            case 242:
                xOffset = int(guiSize.x * (-0.0/100))-126;
                yOffset = int(guiSize.y * (50.0/100))-252;
                layer = 1.0;
                break;
            case 243:
                xOffset = int(guiSize.x * (-0.0/100))-8;
                yOffset = int(guiSize.y * (50.0/100))-263;
                layer = 4.0;
                break;
            case 244:
                xOffset = int(guiSize.x * (-0.0/100))-38;
                yOffset = int(guiSize.y * (50.0/100))-267;
                layer = 2.0;
                break;
            case 245:
                xOffset = int(guiSize.x * (-0.0/100))-98;
                yOffset = int(guiSize.y * (50.0/100))-267;
                layer = 2.0;
                break;
            case 246:
                xOffset = int(guiSize.x * (-0.0/100))-89;
                yOffset = int(guiSize.y * (50.0/100))-267;
                layer = 4.0;
                break;
            case 247:
                xOffset = int(guiSize.x * (-0.0/100))-38;
                yOffset = int(guiSize.y * (50.0/100))-276;
                layer = 2.0;
                break;
            case 248:
                xOffset = int(guiSize.x * (-0.0/100))-98;
                yOffset = int(guiSize.y * (50.0/100))-276;
                layer = 2.0;
                break;
            case 249:
                xOffset = int(guiSize.x * (-0.0/100))-89;
                yOffset = int(guiSize.y * (50.0/100))-276;
                layer = 4.0;
                break;
            case 250:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-252;
                layer = 4.0;
                break;
            case 251:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-294;
                layer = 1.0;
                break;
            case 252:
                xOffset = int(guiSize.x * (-0.0/100))-66;
                yOffset = int(guiSize.y * (50.0/100))-294;
                layer = 1.0;
                break;
            case 253:
                xOffset = int(guiSize.x * (-0.0/100))-126;
                yOffset = int(guiSize.y * (50.0/100))-294;
                layer = 1.0;
                break;
            case 254:
                xOffset = int(guiSize.x * (-0.0/100))-8;
                yOffset = int(guiSize.y * (50.0/100))-305;
                layer = 4.0;
                break;
            case 255:
                xOffset = int(guiSize.x * (-0.0/100))-38;
                yOffset = int(guiSize.y * (50.0/100))-309;
                layer = 2.0;
                break;
            case 256:
                xOffset = int(guiSize.x * (-0.0/100))-98;
                yOffset = int(guiSize.y * (50.0/100))-309;
                layer = 2.0;
                break;
            case 257:
                xOffset = int(guiSize.x * (-0.0/100))-89;
                yOffset = int(guiSize.y * (50.0/100))-309;
                layer = 4.0;
                break;
            case 258:
                xOffset = int(guiSize.x * (-0.0/100))-38;
                yOffset = int(guiSize.y * (50.0/100))-318;
                layer = 2.0;
                break;
            case 259:
                xOffset = int(guiSize.x * (-0.0/100))-98;
                yOffset = int(guiSize.y * (50.0/100))-318;
                layer = 2.0;
                break;
            case 260:
                xOffset = int(guiSize.x * (-0.0/100))-89;
                yOffset = int(guiSize.y * (50.0/100))-318;
                layer = 4.0;
                break;
            case 261:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-294;
                layer = 4.0;
                break;
            case 262:
                xOffset = int(guiSize.x * (-0.0/100))-34;
                yOffset = int(guiSize.y * (50.0/100))-13;
                layer = 2.0;
                break;
            case 263:
                xOffset = int(guiSize.x * (-0.0/100))-94;
                yOffset = int(guiSize.y * (50.0/100))-13;
                layer = 2.0;
                break;
            case 264:
                xOffset = int(guiSize.x * (-0.0/100))-75;
                yOffset = int(guiSize.y * (50.0/100))-13;
                layer = 4.0;
                break;
            case 265:
                xOffset = int(guiSize.x * (-0.0/100))-34;
                yOffset = int(guiSize.y * (50.0/100))-22;
                layer = 2.0;
                break;
            case 266:
                xOffset = int(guiSize.x * (-0.0/100))-94;
                yOffset = int(guiSize.y * (50.0/100))-22;
                layer = 2.0;
                break;
            case 267:
                xOffset = int(guiSize.x * (-0.0/100))-75;
                yOffset = int(guiSize.y * (50.0/100))-22;
                layer = 4.0;
                break;
            case 268:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-38;
                layer = 1.0;
                break;
            case 269:
                xOffset = int(guiSize.x * (-0.0/100))-66;
                yOffset = int(guiSize.y * (50.0/100))-38;
                layer = 1.0;
                break;
            case 270:
                xOffset = int(guiSize.x * (-0.0/100))-8;
                yOffset = int(guiSize.y * (50.0/100))-49;
                layer = 4.0;
                break;
            case 271:
                xOffset = int(guiSize.x * (-0.0/100))-34;
                yOffset = int(guiSize.y * (50.0/100))-51;
                layer = 2.0;
                break;
            case 272:
                xOffset = int(guiSize.x * (-0.0/100))-94;
                yOffset = int(guiSize.y * (50.0/100))-51;
                layer = 2.0;
                break;
            case 273:
                xOffset = int(guiSize.x * (-0.0/100))-75;
                yOffset = int(guiSize.y * (50.0/100))-51;
                layer = 4.0;
                break;
            case 274:
                xOffset = int(guiSize.x * (-0.0/100))-34;
                yOffset = int(guiSize.y * (50.0/100))-60;
                layer = 2.0;
                break;
            case 275:
                xOffset = int(guiSize.x * (-0.0/100))-94;
                yOffset = int(guiSize.y * (50.0/100))-60;
                layer = 2.0;
                break;
            case 276:
                xOffset = int(guiSize.x * (-0.0/100))-75;
                yOffset = int(guiSize.y * (50.0/100))-60;
                layer = 4.0;
                break;
            case 277:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-38;
                layer = 4.0;
                break;
            case 278:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-76;
                layer = 1.0;
                break;
            case 279:
                xOffset = int(guiSize.x * (-0.0/100))-66;
                yOffset = int(guiSize.y * (50.0/100))-76;
                layer = 1.0;
                break;
            case 280:
                xOffset = int(guiSize.x * (-0.0/100))-8;
                yOffset = int(guiSize.y * (50.0/100))-87;
                layer = 4.0;
                break;
            case 281:
                xOffset = int(guiSize.x * (-0.0/100))-34;
                yOffset = int(guiSize.y * (50.0/100))-89;
                layer = 2.0;
                break;
            case 282:
                xOffset = int(guiSize.x * (-0.0/100))-94;
                yOffset = int(guiSize.y * (50.0/100))-89;
                layer = 2.0;
                break;
            case 283:
                xOffset = int(guiSize.x * (-0.0/100))-75;
                yOffset = int(guiSize.y * (50.0/100))-89;
                layer = 4.0;
                break;
            case 284:
                xOffset = int(guiSize.x * (-0.0/100))-34;
                yOffset = int(guiSize.y * (50.0/100))-98;
                layer = 2.0;
                break;
            case 285:
                xOffset = int(guiSize.x * (-0.0/100))-94;
                yOffset = int(guiSize.y * (50.0/100))-98;
                layer = 2.0;
                break;
            case 286:
                xOffset = int(guiSize.x * (-0.0/100))-75;
                yOffset = int(guiSize.y * (50.0/100))-98;
                layer = 4.0;
                break;
            case 287:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-76;
                layer = 4.0;
                break;
            case 288:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-114;
                layer = 1.0;
                break;
            case 289:
                xOffset = int(guiSize.x * (-0.0/100))-66;
                yOffset = int(guiSize.y * (50.0/100))-114;
                layer = 1.0;
                break;
            case 290:
                xOffset = int(guiSize.x * (-0.0/100))-8;
                yOffset = int(guiSize.y * (50.0/100))-125;
                layer = 4.0;
                break;
            case 291:
                xOffset = int(guiSize.x * (-0.0/100))-34;
                yOffset = int(guiSize.y * (50.0/100))-127;
                layer = 2.0;
                break;
            case 292:
                xOffset = int(guiSize.x * (-0.0/100))-94;
                yOffset = int(guiSize.y * (50.0/100))-127;
                layer = 2.0;
                break;
            case 293:
                xOffset = int(guiSize.x * (-0.0/100))-75;
                yOffset = int(guiSize.y * (50.0/100))-127;
                layer = 4.0;
                break;
            case 294:
                xOffset = int(guiSize.x * (-0.0/100))-34;
                yOffset = int(guiSize.y * (50.0/100))-136;
                layer = 2.0;
                break;
            case 295:
                xOffset = int(guiSize.x * (-0.0/100))-94;
                yOffset = int(guiSize.y * (50.0/100))-136;
                layer = 2.0;
                break;
            case 296:
                xOffset = int(guiSize.x * (-0.0/100))-75;
                yOffset = int(guiSize.y * (50.0/100))-136;
                layer = 4.0;
                break;
            case 297:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-114;
                layer = 4.0;
                break;
            case 298:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-152;
                layer = 1.0;
                break;
            case 299:
                xOffset = int(guiSize.x * (-0.0/100))-66;
                yOffset = int(guiSize.y * (50.0/100))-152;
                layer = 1.0;
                break;
            case 300:
                xOffset = int(guiSize.x * (-0.0/100))-8;
                yOffset = int(guiSize.y * (50.0/100))-163;
                layer = 4.0;
                break;
            case 301:
                xOffset = int(guiSize.x * (-0.0/100))-34;
                yOffset = int(guiSize.y * (50.0/100))-165;
                layer = 2.0;
                break;
            case 302:
                xOffset = int(guiSize.x * (-0.0/100))-94;
                yOffset = int(guiSize.y * (50.0/100))-165;
                layer = 2.0;
                break;
            case 303:
                xOffset = int(guiSize.x * (-0.0/100))-75;
                yOffset = int(guiSize.y * (50.0/100))-165;
                layer = 4.0;
                break;
            case 304:
                xOffset = int(guiSize.x * (-0.0/100))-34;
                yOffset = int(guiSize.y * (50.0/100))-174;
                layer = 2.0;
                break;
            case 305:
                xOffset = int(guiSize.x * (-0.0/100))-94;
                yOffset = int(guiSize.y * (50.0/100))-174;
                layer = 2.0;
                break;
            case 306:
                xOffset = int(guiSize.x * (-0.0/100))-75;
                yOffset = int(guiSize.y * (50.0/100))-174;
                layer = 4.0;
                break;
            case 307:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-152;
                layer = 4.0;
                break;
            case 308:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-190;
                layer = 1.0;
                break;
            case 309:
                xOffset = int(guiSize.x * (-0.0/100))-66;
                yOffset = int(guiSize.y * (50.0/100))-190;
                layer = 1.0;
                break;
            case 310:
                xOffset = int(guiSize.x * (-0.0/100))-8;
                yOffset = int(guiSize.y * (50.0/100))-201;
                layer = 4.0;
                break;
            case 311:
                xOffset = int(guiSize.x * (-0.0/100))-34;
                yOffset = int(guiSize.y * (50.0/100))-203;
                layer = 2.0;
                break;
            case 312:
                xOffset = int(guiSize.x * (-0.0/100))-94;
                yOffset = int(guiSize.y * (50.0/100))-203;
                layer = 2.0;
                break;
            case 313:
                xOffset = int(guiSize.x * (-0.0/100))-75;
                yOffset = int(guiSize.y * (50.0/100))-203;
                layer = 4.0;
                break;
            case 314:
                xOffset = int(guiSize.x * (-0.0/100))-34;
                yOffset = int(guiSize.y * (50.0/100))-212;
                layer = 2.0;
                break;
            case 315:
                xOffset = int(guiSize.x * (-0.0/100))-94;
                yOffset = int(guiSize.y * (50.0/100))-212;
                layer = 2.0;
                break;
            case 316:
                xOffset = int(guiSize.x * (-0.0/100))-75;
                yOffset = int(guiSize.y * (50.0/100))-212;
                layer = 4.0;
                break;
            case 317:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-190;
                layer = 4.0;
                break;
            case 318:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-228;
                layer = 1.0;
                break;
            case 319:
                xOffset = int(guiSize.x * (-0.0/100))-66;
                yOffset = int(guiSize.y * (50.0/100))-228;
                layer = 1.0;
                break;
            case 320:
                xOffset = int(guiSize.x * (-0.0/100))-8;
                yOffset = int(guiSize.y * (50.0/100))-239;
                layer = 4.0;
                break;
            case 321:
                xOffset = int(guiSize.x * (-0.0/100))-34;
                yOffset = int(guiSize.y * (50.0/100))-241;
                layer = 2.0;
                break;
            case 322:
                xOffset = int(guiSize.x * (-0.0/100))-94;
                yOffset = int(guiSize.y * (50.0/100))-241;
                layer = 2.0;
                break;
            case 323:
                xOffset = int(guiSize.x * (-0.0/100))-75;
                yOffset = int(guiSize.y * (50.0/100))-241;
                layer = 4.0;
                break;
            case 324:
                xOffset = int(guiSize.x * (-0.0/100))-34;
                yOffset = int(guiSize.y * (50.0/100))-250;
                layer = 2.0;
                break;
            case 325:
                xOffset = int(guiSize.x * (-0.0/100))-94;
                yOffset = int(guiSize.y * (50.0/100))-250;
                layer = 2.0;
                break;
            case 326:
                xOffset = int(guiSize.x * (-0.0/100))-75;
                yOffset = int(guiSize.y * (50.0/100))-250;
                layer = 4.0;
                break;
            case 327:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-228;
                layer = 4.0;
                break;
            case 328:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-266;
                layer = 1.0;
                break;
            case 329:
                xOffset = int(guiSize.x * (-0.0/100))-66;
                yOffset = int(guiSize.y * (50.0/100))-266;
                layer = 1.0;
                break;
            case 330:
                xOffset = int(guiSize.x * (-0.0/100))-8;
                yOffset = int(guiSize.y * (50.0/100))-277;
                layer = 4.0;
                break;
            case 331:
                xOffset = int(guiSize.x * (-0.0/100))-34;
                yOffset = int(guiSize.y * (50.0/100))-279;
                layer = 2.0;
                break;
            case 332:
                xOffset = int(guiSize.x * (-0.0/100))-94;
                yOffset = int(guiSize.y * (50.0/100))-279;
                layer = 2.0;
                break;
            case 333:
                xOffset = int(guiSize.x * (-0.0/100))-75;
                yOffset = int(guiSize.y * (50.0/100))-279;
                layer = 4.0;
                break;
            case 334:
                xOffset = int(guiSize.x * (-0.0/100))-34;
                yOffset = int(guiSize.y * (50.0/100))-288;
                layer = 2.0;
                break;
            case 335:
                xOffset = int(guiSize.x * (-0.0/100))-94;
                yOffset = int(guiSize.y * (50.0/100))-288;
                layer = 2.0;
                break;
            case 336:
                xOffset = int(guiSize.x * (-0.0/100))-75;
                yOffset = int(guiSize.y * (50.0/100))-288;
                layer = 4.0;
                break;
            case 337:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-266;
                layer = 4.0;
                break;
            case 338:
                xOffset = int(guiSize.x * (-0.0/100))-30;
                yOffset = int(guiSize.y * (50.0/100))-11;
                layer = 2.0;
                break;
            case 339:
                xOffset = int(guiSize.x * (-0.0/100))-90;
                yOffset = int(guiSize.y * (50.0/100))-11;
                layer = 2.0;
                break;
            case 340:
                xOffset = int(guiSize.x * (-0.0/100))-63;
                yOffset = int(guiSize.y * (50.0/100))-11;
                layer = 4.0;
                break;
            case 341:
                xOffset = int(guiSize.x * (-0.0/100))-30;
                yOffset = int(guiSize.y * (50.0/100))-20;
                layer = 2.0;
                break;
            case 342:
                xOffset = int(guiSize.x * (-0.0/100))-90;
                yOffset = int(guiSize.y * (50.0/100))-20;
                layer = 2.0;
                break;
            case 343:
                xOffset = int(guiSize.x * (-0.0/100))-63;
                yOffset = int(guiSize.y * (50.0/100))-20;
                layer = 4.0;
                break;
            case 344:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-34;
                layer = 1.0;
                break;
            case 345:
                xOffset = int(guiSize.x * (-0.0/100))-66;
                yOffset = int(guiSize.y * (50.0/100))-34;
                layer = 1.0;
                break;
            case 346:
                xOffset = int(guiSize.x * (-0.0/100))-8;
                yOffset = int(guiSize.y * (50.0/100))-45;
                layer = 4.0;
                break;
            case 347:
                xOffset = int(guiSize.x * (-0.0/100))-30;
                yOffset = int(guiSize.y * (50.0/100))-45;
                layer = 2.0;
                break;
            case 348:
                xOffset = int(guiSize.x * (-0.0/100))-90;
                yOffset = int(guiSize.y * (50.0/100))-45;
                layer = 2.0;
                break;
            case 349:
                xOffset = int(guiSize.x * (-0.0/100))-63;
                yOffset = int(guiSize.y * (50.0/100))-45;
                layer = 4.0;
                break;
            case 350:
                xOffset = int(guiSize.x * (-0.0/100))-30;
                yOffset = int(guiSize.y * (50.0/100))-54;
                layer = 2.0;
                break;
            case 351:
                xOffset = int(guiSize.x * (-0.0/100))-90;
                yOffset = int(guiSize.y * (50.0/100))-54;
                layer = 2.0;
                break;
            case 352:
                xOffset = int(guiSize.x * (-0.0/100))-63;
                yOffset = int(guiSize.y * (50.0/100))-54;
                layer = 4.0;
                break;
            case 353:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-34;
                layer = 4.0;
                break;
            case 354:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-68;
                layer = 1.0;
                break;
            case 355:
                xOffset = int(guiSize.x * (-0.0/100))-66;
                yOffset = int(guiSize.y * (50.0/100))-68;
                layer = 1.0;
                break;
            case 356:
                xOffset = int(guiSize.x * (-0.0/100))-8;
                yOffset = int(guiSize.y * (50.0/100))-79;
                layer = 4.0;
                break;
            case 357:
                xOffset = int(guiSize.x * (-0.0/100))-30;
                yOffset = int(guiSize.y * (50.0/100))-79;
                layer = 2.0;
                break;
            case 358:
                xOffset = int(guiSize.x * (-0.0/100))-90;
                yOffset = int(guiSize.y * (50.0/100))-79;
                layer = 2.0;
                break;
            case 359:
                xOffset = int(guiSize.x * (-0.0/100))-63;
                yOffset = int(guiSize.y * (50.0/100))-79;
                layer = 4.0;
                break;
            case 360:
                xOffset = int(guiSize.x * (-0.0/100))-30;
                yOffset = int(guiSize.y * (50.0/100))-88;
                layer = 2.0;
                break;
            case 361:
                xOffset = int(guiSize.x * (-0.0/100))-90;
                yOffset = int(guiSize.y * (50.0/100))-88;
                layer = 2.0;
                break;
            case 362:
                xOffset = int(guiSize.x * (-0.0/100))-63;
                yOffset = int(guiSize.y * (50.0/100))-88;
                layer = 4.0;
                break;
            case 363:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-68;
                layer = 4.0;
                break;
            case 364:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-102;
                layer = 1.0;
                break;
            case 365:
                xOffset = int(guiSize.x * (-0.0/100))-66;
                yOffset = int(guiSize.y * (50.0/100))-102;
                layer = 1.0;
                break;
            case 366:
                xOffset = int(guiSize.x * (-0.0/100))-8;
                yOffset = int(guiSize.y * (50.0/100))-113;
                layer = 4.0;
                break;
            case 367:
                xOffset = int(guiSize.x * (-0.0/100))-30;
                yOffset = int(guiSize.y * (50.0/100))-113;
                layer = 2.0;
                break;
            case 368:
                xOffset = int(guiSize.x * (-0.0/100))-90;
                yOffset = int(guiSize.y * (50.0/100))-113;
                layer = 2.0;
                break;
            case 369:
                xOffset = int(guiSize.x * (-0.0/100))-63;
                yOffset = int(guiSize.y * (50.0/100))-113;
                layer = 4.0;
                break;
            case 370:
                xOffset = int(guiSize.x * (-0.0/100))-30;
                yOffset = int(guiSize.y * (50.0/100))-122;
                layer = 2.0;
                break;
            case 371:
                xOffset = int(guiSize.x * (-0.0/100))-90;
                yOffset = int(guiSize.y * (50.0/100))-122;
                layer = 2.0;
                break;
            case 372:
                xOffset = int(guiSize.x * (-0.0/100))-63;
                yOffset = int(guiSize.y * (50.0/100))-122;
                layer = 4.0;
                break;
            case 373:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-102;
                layer = 4.0;
                break;
            case 374:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-136;
                layer = 1.0;
                break;
            case 375:
                xOffset = int(guiSize.x * (-0.0/100))-66;
                yOffset = int(guiSize.y * (50.0/100))-136;
                layer = 1.0;
                break;
            case 376:
                xOffset = int(guiSize.x * (-0.0/100))-8;
                yOffset = int(guiSize.y * (50.0/100))-147;
                layer = 4.0;
                break;
            case 377:
                xOffset = int(guiSize.x * (-0.0/100))-30;
                yOffset = int(guiSize.y * (50.0/100))-147;
                layer = 2.0;
                break;
            case 378:
                xOffset = int(guiSize.x * (-0.0/100))-90;
                yOffset = int(guiSize.y * (50.0/100))-147;
                layer = 2.0;
                break;
            case 379:
                xOffset = int(guiSize.x * (-0.0/100))-63;
                yOffset = int(guiSize.y * (50.0/100))-147;
                layer = 4.0;
                break;
            case 380:
                xOffset = int(guiSize.x * (-0.0/100))-30;
                yOffset = int(guiSize.y * (50.0/100))-156;
                layer = 2.0;
                break;
            case 381:
                xOffset = int(guiSize.x * (-0.0/100))-90;
                yOffset = int(guiSize.y * (50.0/100))-156;
                layer = 2.0;
                break;
            case 382:
                xOffset = int(guiSize.x * (-0.0/100))-63;
                yOffset = int(guiSize.y * (50.0/100))-156;
                layer = 4.0;
                break;
            case 383:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-136;
                layer = 4.0;
                break;
            case 384:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-170;
                layer = 1.0;
                break;
            case 385:
                xOffset = int(guiSize.x * (-0.0/100))-66;
                yOffset = int(guiSize.y * (50.0/100))-170;
                layer = 1.0;
                break;
            case 386:
                xOffset = int(guiSize.x * (-0.0/100))-8;
                yOffset = int(guiSize.y * (50.0/100))-181;
                layer = 4.0;
                break;
            case 387:
                xOffset = int(guiSize.x * (-0.0/100))-30;
                yOffset = int(guiSize.y * (50.0/100))-181;
                layer = 2.0;
                break;
            case 388:
                xOffset = int(guiSize.x * (-0.0/100))-90;
                yOffset = int(guiSize.y * (50.0/100))-181;
                layer = 2.0;
                break;
            case 389:
                xOffset = int(guiSize.x * (-0.0/100))-63;
                yOffset = int(guiSize.y * (50.0/100))-181;
                layer = 4.0;
                break;
            case 390:
                xOffset = int(guiSize.x * (-0.0/100))-30;
                yOffset = int(guiSize.y * (50.0/100))-190;
                layer = 2.0;
                break;
            case 391:
                xOffset = int(guiSize.x * (-0.0/100))-90;
                yOffset = int(guiSize.y * (50.0/100))-190;
                layer = 2.0;
                break;
            case 392:
                xOffset = int(guiSize.x * (-0.0/100))-63;
                yOffset = int(guiSize.y * (50.0/100))-190;
                layer = 4.0;
                break;
            case 393:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-170;
                layer = 4.0;
                break;
            case 394:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-204;
                layer = 1.0;
                break;
            case 395:
                xOffset = int(guiSize.x * (-0.0/100))-66;
                yOffset = int(guiSize.y * (50.0/100))-204;
                layer = 1.0;
                break;
            case 396:
                xOffset = int(guiSize.x * (-0.0/100))-8;
                yOffset = int(guiSize.y * (50.0/100))-215;
                layer = 4.0;
                break;
            case 397:
                xOffset = int(guiSize.x * (-0.0/100))-30;
                yOffset = int(guiSize.y * (50.0/100))-215;
                layer = 2.0;
                break;
            case 398:
                xOffset = int(guiSize.x * (-0.0/100))-90;
                yOffset = int(guiSize.y * (50.0/100))-215;
                layer = 2.0;
                break;
            case 399:
                xOffset = int(guiSize.x * (-0.0/100))-63;
                yOffset = int(guiSize.y * (50.0/100))-215;
                layer = 4.0;
                break;
            case 400:
                xOffset = int(guiSize.x * (-0.0/100))-30;
                yOffset = int(guiSize.y * (50.0/100))-224;
                layer = 2.0;
                break;
            case 401:
                xOffset = int(guiSize.x * (-0.0/100))-90;
                yOffset = int(guiSize.y * (50.0/100))-224;
                layer = 2.0;
                break;
            case 402:
                xOffset = int(guiSize.x * (-0.0/100))-63;
                yOffset = int(guiSize.y * (50.0/100))-224;
                layer = 4.0;
                break;
            case 403:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-204;
                layer = 4.0;
                break;
            case 404:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-238;
                layer = 1.0;
                break;
            case 405:
                xOffset = int(guiSize.x * (-0.0/100))-66;
                yOffset = int(guiSize.y * (50.0/100))-238;
                layer = 1.0;
                break;
            case 406:
                xOffset = int(guiSize.x * (-0.0/100))-8;
                yOffset = int(guiSize.y * (50.0/100))-249;
                layer = 4.0;
                break;
            case 407:
                xOffset = int(guiSize.x * (-0.0/100))-30;
                yOffset = int(guiSize.y * (50.0/100))-249;
                layer = 2.0;
                break;
            case 408:
                xOffset = int(guiSize.x * (-0.0/100))-90;
                yOffset = int(guiSize.y * (50.0/100))-249;
                layer = 2.0;
                break;
            case 409:
                xOffset = int(guiSize.x * (-0.0/100))-63;
                yOffset = int(guiSize.y * (50.0/100))-249;
                layer = 4.0;
                break;
            case 410:
                xOffset = int(guiSize.x * (-0.0/100))-30;
                yOffset = int(guiSize.y * (50.0/100))-258;
                layer = 2.0;
                break;
            case 411:
                xOffset = int(guiSize.x * (-0.0/100))-90;
                yOffset = int(guiSize.y * (50.0/100))-258;
                layer = 2.0;
                break;
            case 412:
                xOffset = int(guiSize.x * (-0.0/100))-63;
                yOffset = int(guiSize.y * (50.0/100))-258;
                layer = 4.0;
                break;
            case 413:
                xOffset = int(guiSize.x * (-0.0/100))-6;
                yOffset = int(guiSize.y * (50.0/100))-238;
                layer = 4.0;
                break;
            case 414:
                xOffset = int(guiSize.x * (-0.0/100))-40;
                yOffset = int(guiSize.y * (100.0/100))-94;
                layer = 5.0;
                break;
            case 415:
                xOffset = int(guiSize.x * (-0.0/100))-97;
                yOffset = int(guiSize.y * (100.0/100))-77;
                layer = 6.0;
                break;
            case 416:
                xOffset = int(guiSize.x * (-0.0/100))-97;
                yOffset = int(guiSize.y * (100.0/100))-90;
                layer = 6.0;
                break;
            case 417:
                xOffset = int(guiSize.x * (-0.0/100))-97;
                yOffset = int(guiSize.y * (100.0/100))-103;
                layer = 6.0;
                break;
            case 418:
                xOffset = int(guiSize.x * (-0.0/100))-97;
                yOffset = int(guiSize.y * (100.0/100))-116;
                layer = 6.0;
                break;
            case 419:
                xOffset = int(guiSize.x * (-0.0/100))-97;
                yOffset = int(guiSize.y * (100.0/100))-58;
                layer = 3.0;
                break;
            case 420:
                xOffset = int(guiSize.x * (-0.0/100))-157;
                yOffset = int(guiSize.y * (100.0/100))-58;
                layer = 3.0;
                break;
            case 421:
                xOffset = int(guiSize.x * (-0.0/100))-5;
                yOffset = int(guiSize.y * (100.0/100))-5;
                layer = 1.0;
                break;
            case 422:
                xOffset = int(guiSize.x * (-0.0/100))-65;
                yOffset = int(guiSize.y * (100.0/100))-5;
                layer = 1.0;
                break;
            case 423:
                xOffset = int(guiSize.x * (-0.0/100))-125;
                yOffset = int(guiSize.y * (100.0/100))-5;
                layer = 1.0;
                break;
            case 424:
                xOffset = int(guiSize.x * (-0.0/100))-185;
                yOffset = int(guiSize.y * (100.0/100))-5;
                layer = 1.0;
                break;
            case 425:
                xOffset = int(guiSize.x * (-0.0/100))-245;
                yOffset = int(guiSize.y * (100.0/100))-5;
                layer = 1.0;
                break;
            case 426:
                xOffset = int(guiSize.x * (-0.0/100))-5;
                yOffset = int(guiSize.y * (100.0/100))-65;
                layer = 1.0;
                break;
            case 427:
                xOffset = int(guiSize.x * (-0.0/100))-65;
                yOffset = int(guiSize.y * (100.0/100))-65;
                layer = 1.0;
                break;
            case 428:
                xOffset = int(guiSize.x * (-0.0/100))-125;
                yOffset = int(guiSize.y * (100.0/100))-65;
                layer = 1.0;
                break;
            case 429:
                xOffset = int(guiSize.x * (-0.0/100))-185;
                yOffset = int(guiSize.y * (100.0/100))-65;
                layer = 1.0;
                break;
            case 430:
                xOffset = int(guiSize.x * (-0.0/100))-13;
                yOffset = int(guiSize.y * (100.0/100))-14;
                layer = 0.0;
                break;
            case 431:
                xOffset = int(guiSize.x * (-0.0/100))-97;
                yOffset = int(guiSize.y * (100.0/100))-18;
                layer = 3.0;
                break;
            case 432:
                xOffset = int(guiSize.x * (-0.0/100))-157;
                yOffset = int(guiSize.y * (100.0/100))-18;
                layer = 3.0;
                break;
            case 433:
                xOffset = int(guiSize.x * (-0.0/100))-217;
                yOffset = int(guiSize.y * (100.0/100))-18;
                layer = 3.0;
                break;
            case 434:
                xOffset = int(guiSize.x * (-0.0/100))-173;
                yOffset = int(guiSize.y * (100.0/100))-21;
                layer = 5.0;
                break;
            case 435:
                xOffset = int(guiSize.x * (-0.0/100))-97;
                yOffset = int(guiSize.y * (100.0/100))-39;
                layer = 3.0;
                break;
            case 436:
                xOffset = int(guiSize.x * (-0.0/100))-157;
                yOffset = int(guiSize.y * (100.0/100))-39;
                layer = 3.0;
                break;
            case 437:
                xOffset = int(guiSize.x * (-0.0/100))-217;
                yOffset = int(guiSize.y * (100.0/100))-39;
                layer = 3.0;
                break;
            case 438:
                xOffset = int(guiSize.x * (-0.0/100))-173;
                yOffset = int(guiSize.y * (100.0/100))-40;
                layer = 5.0;
                break;
            case 439:
                xOffset = int(guiSize.x * (-0.0/100))-24;
                yOffset = int(guiSize.y * (100.0/100))-82;
                layer = 1.0;
                break;
            case 440:
                xOffset = int(guiSize.x * (-0.0/100))-97;
                yOffset = int(guiSize.y * (100.0/100))-79;
                layer = 7.0;
                break;
            case 441:
                xOffset = int(guiSize.x * (-0.0/100))-97;
                yOffset = int(guiSize.y * (100.0/100))-92;
                layer = 7.0;
                break;
            case 442:
                xOffset = int(guiSize.x * (-0.0/100))-97;
                yOffset = int(guiSize.y * (100.0/100))-105;
                layer = 7.0;
                break;
            case 443:
                xOffset = int(guiSize.x * (-0.0/100))-97;
                yOffset = int(guiSize.y * (100.0/100))-118;
                layer = 7.0;
                break;
            case 444:
                xOffset = int(guiSize.x * (-0.0/100))-97;
                yOffset = int(guiSize.y * (100.0/100))-57;
                layer = 6.0;
                break;
            case 445:
                xOffset = int(guiSize.x * (-0.0/100))-97;
                yOffset = int(guiSize.y * (100.0/100))-70;
                layer = 6.0;
                break;
            case 446:
                xOffset = int(guiSize.x * (-0.0/100))-97;
                yOffset = int(guiSize.y * (100.0/100))-83;
                layer = 6.0;
                break;
            case 447:
                xOffset = int(guiSize.x * (-0.0/100))-97;
                yOffset = int(guiSize.y * (100.0/100))-96;
                layer = 6.0;
                break;
            case 448:
                xOffset = int(guiSize.x * (-0.0/100))-97;
                yOffset = int(guiSize.y * (100.0/100))-38;
                layer = 3.0;
                break;
            case 449:
                xOffset = int(guiSize.x * (-0.0/100))-157;
                yOffset = int(guiSize.y * (100.0/100))-38;
                layer = 3.0;
                break;
            case 450:
                xOffset = int(guiSize.x * (-0.0/100))-97;
                yOffset = int(guiSize.y * (100.0/100))-59;
                layer = 7.0;
                break;
            case 451:
                xOffset = int(guiSize.x * (-0.0/100))-97;
                yOffset = int(guiSize.y * (100.0/100))-72;
                layer = 7.0;
                break;
            case 452:
                xOffset = int(guiSize.x * (-0.0/100))-97;
                yOffset = int(guiSize.y * (100.0/100))-85;
                layer = 7.0;
                break;
            case 453:
                xOffset = int(guiSize.x * (-0.0/100))-97;
                yOffset = int(guiSize.y * (100.0/100))-98;
                layer = 7.0;
                break;
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
}
