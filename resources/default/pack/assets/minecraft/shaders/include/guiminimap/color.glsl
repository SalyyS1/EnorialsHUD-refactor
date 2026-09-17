#define MAP(v, t) case ((int(v.r)<<16) + (int(v.g)<<8) + int(v.b)): color.rgb = t/255.; break;\
case ((int(v.r*220./255.)<<16) + (int(v.g*220./255.)<<8) + int(v.b*220./255.)): color.rgb = t/255.*220./255.; break;\
case ((int(v.r*180./255.)<<16) + (int(v.g*180./255.)<<8) + int(v.b*180./255.)): color.rgb = t/255.*180./255.; break;\
case ((int(v.r*135./255.)<<16) + (int(v.g*135./255.)<<8) + int(v.b*135./255.)): color.rgb = t/255.*135./255.; break;

void remapColor(inout vec4 color, float colorProfile) {
    // Godlander's map colors
    // https://github.com/Godlander/vpp/blob/main/assets/minecraft/shaders/core/render/text.fsh
    vec3 original = color.rgb;
    ivec3 i = ivec3(color.rgb * 255.5);
    switch ((i.r << 16) + (i.g << 8) + i.b) {
        MAP(vec3(127.,178.,56.) ,   vec3(94.,123.,57.))
        MAP(vec3(247.,233.,163.),   vec3(248.,235.,186.))
        MAP(vec3(160.,160.,255.),   vec3(132.,171.,244.))
        MAP(vec3(167.,167.,167.),   vec3(200.,200.,200.))
        MAP(vec3(0.,124.,0.)    ,   vec3(58.,86.,39.))
        MAP(vec3(164.,168.,184.),   vec3(182.,189.,204.))
        MAP(vec3(151.,109.,77.) ,   vec3(157.,113.,80.))
        MAP(vec3(112.,112.,112.),   vec3(143.,143.,143.))
        MAP(vec3(64.,64.,255.)  ,   vec3(41.,71.,130.))
        MAP(vec3(143.,119.,72.) ,   vec3(187.,152.,93.))
        MAP(vec3(250.,238.,77.) ,   vec3(255.,239.,79.))
        MAP(vec3(74.,128.,255.) ,   vec3(37.,79.,160.))
        MAP(vec3(0.,217.,58.)   ,   vec3(66.,233.,113.))
        MAP(vec3(129.,86.,49.)  ,   vec3(108.,75.,29.))
        MAP(vec3(127.,63.,178.) ,   vec3(133.,107.,153))
        MAP(vec3(112.,2.,0.)    ,   vec3(113.,47.,47.))
        MAP(vec3(255.,0.,0.)    ,   vec3(215.,53.,2.))
    }

    int profile = int(mod(colorProfile + 0.5, 3.0));
    if (profile == 0) {
        // ORIGINAL: keep classic remapped palette (default).
        color.rgb = clamp(color.rgb, 0.0, 1.0);
        return;
    }

    float mixOriginal = 0.28;
    float satBoost = 1.10;
    float contrastBoost = 1.06;
    float greenBoost = 0.14;
    float warmBoost = 0.06;
    float detailBoost = 0.16;
    if (profile == 2) {
        // VIVID: stronger separation for biome/block transitions.
        mixOriginal = 0.44;
        satBoost = 1.26;
        contrastBoost = 1.14;
        greenBoost = 0.24;
        warmBoost = 0.10;
        detailBoost = 0.28;
    }

    color.rgb = mix(color.rgb, original, mixOriginal);
    float luma = dot(color.rgb, vec3(0.299, 0.587, 0.114));
    color.rgb = mix(vec3(luma), color.rgb, satBoost);
    color.rgb = (color.rgb - 0.5) * contrastBoost + 0.5;

    float greenDominance = max(0.0, original.g - max(original.r, original.b));
    float warmness = max(0.0, original.r - original.g);
    color.rgb.g += greenDominance * greenBoost;
    color.rgb.r += warmness * warmBoost;
    color.rgb.b -= greenDominance * (greenBoost * 0.25);

    float boostedLuma = dot(color.rgb, vec3(0.299, 0.587, 0.114));
    float detail = dot(original, vec3(0.299, 0.587, 0.114)) - boostedLuma;
    color.rgb += vec3(detail * detailBoost);
    color.rgb = clamp(color.rgb, 0.0, 1.0);
}