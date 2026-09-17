// Old clients darken native shadow RGB to one quarter. The foreground's two
// high red bits identify it without consuming alpha or changing arrow artwork.
float enoLegacyGpsCursor(vec3 position,vec2 uv,vec4 tint) {
    if(tint.r < 0.75)return 0.0;
    ivec2 size=textureSize(Sampler0,0);
    vec2 texel=uv*vec2(size);
    vec2 corner=vec2(lessThan(texel-round(texel),vec2(0.0)));
    ivec2 origin=ivec2(round(texel))-ivec2(corner*2.0);
    if(any(lessThan(origin,ivec2(0)))||any(greaterThanEqual(origin+1,size)))return 0.0;
    ivec4 tag=ivec4(round(texelFetch(Sampler0,origin,0)*255.0));
    ivec4 magic=ivec4(round(texelFetch(Sampler0,origin+ivec2(1,0),0)*255.0));
    ivec4 version=ivec4(round(texelFetch(Sampler0,origin+ivec2(0,1),0)*255.0));
    if(tag.b!=167||tag.a!=255||any(notEqual(magic,ivec4(71,80,83,255)))||any(notEqual(version,ivec4(51,68,1,255))))return 0.0;
    float scale=max(1.0,round(ProjMat[0][0]*0.5*ScreenSize.x));
    float center=ceil(ScreenSize.x/scale)*0.5;
    return round((position.x-center)/16384.0)*16384.0;
}
