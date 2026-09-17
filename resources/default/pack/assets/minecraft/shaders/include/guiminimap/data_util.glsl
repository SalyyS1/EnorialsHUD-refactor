#define ENO_CAMERA_TERRAIN

// Shared artwork envelope. Neither theme ornaments nor shape can move the map.
const ivec4 EG_LAYOUT_BOUNDS=ivec4(90,90,411,410);
vec4 egFrameOverhang(int variant,int theme) {
    ivec4 bounds=EG_LAYOUT_BOUNDS;
    return vec4(vec2(.5)-vec2(bounds.xy)*.004,vec2(bounds.zw)*.004-vec2(1.5));
}
vec4 egMapCanvas(int variant,int theme) {
    float minimum=min(ScreenSize.x,ScreenSize.y);
    int size=variant/12;
    float width=minimum*.33*(size==0?.52:(size==1?.76:1.0));
    float gap=max(4.0,minimum*.006);
    vec4 edge=egFrameOverhang(variant,theme);
    vec2 top=vec2((variant/6)%2==1?ScreenSize.x-gap-width*(1.0+edge.z):gap+width*edge.x,gap+width*edge.y);
    return vec4(round(top),vec2(width));
}

uint egBits(uvec3 value, int offset, int count) {
    int word = offset >> 5;
    int shift = offset & 31;
    uint result = value[word] >> shift;
    if (shift + count > 32) result |= value[word + 1] << (32 - shift);
    return result & ((1u << count) - 1u);
}
float egZoom(float fromByte, float targetBit, float tick) {
    float age = mod(GameTime * 24000.0 - tick + 16.0, 32.0) - 16.0;
    float base=1.0+floor(fromByte/64.0)*0.5;
    return base*mix(1.0+mod(fromByte,64.0)/63.0,1.0+targetBit,smoothstep(0.0,8.0,age));
}

// Same bounded six-bit velocity codec as GuiMotion; no extra font atlas or texture.
float egPoseVelocity(int code) {
    int magnitude = abs(code);
    float value = magnitude < 16 ? float(magnitude) / 8.0
        : magnitude < 28 ? 2.0 + float(magnitude - 16) / 4.0 : 5.0 + float(magnitude - 28);
    return code < 0 ? -value : value;
}
vec2 egPoseVelocity(ivec2 code) { return vec2(egPoseVelocity(code.x), egPoseVelocity(code.y)); }
float egTerrainZoom(int alias){
    float from=float((alias>>5)&3)*64.0+float(alias&31)*63.0/31.0;
    return egZoom(from,float((alias>>12)&1),float((alias>>7)&31));
}
