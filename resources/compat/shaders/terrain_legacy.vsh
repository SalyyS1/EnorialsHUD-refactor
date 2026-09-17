vec2 enoSharedMotion() {
    ivec4 value=ivec4(0);
    for(int i=0;i<4;i++)for(int b=0;b<4;b++)value[i]|=decodeUnsigned(i*32+b*8,4)<<(b*8);
    float tick=float(decodeUnsigned(0,5)|(decodeUnsigned(8,5)<<8));
    float age=mod(GameTime*24000.0-tick+12000.0,24000.0)-12000.0;
    return vec2(value.xy)/4096.0+vec2(value.zw)/4096.0*(clamp(age,-1.5,2.0)-1.0);
}
bool enoLegacyTerrainVertex() {
    if(ProjMat[3][3]!=0.0||any(notEqual(textureSize(Sampler0,0),ivec2(128))))return false;
    if(decodeUnsigned(0,0)!=69||decodeUnsigned(8,0)!=78||decodeUnsigned(16,0)!=79||decodeUnsigned(24,0)!=55)return false;
    int tile=decodeUnsigned(0,1),variant=decodeUnsigned(8,1);
    if(tile>3||variant>35)return false;
    uint pose=uint(decodeUnsigned(16,1))|(uint(decodeUnsigned(24,1))<<8)|(uint(decodeUnsigned(32,1))<<16)|(uint(decodeUnsigned(40,1))<<24);
    int alias=decodeUnsigned(48,1)|(decodeUnsigned(56,1)<<8);
    ivec2 offset=ivec2(int(pose&127u),int((pose>>7)&127u));offset=(offset+ivec2(64))%ivec2(128)-ivec2(64);
    ivec2 delta=ivec2(int((pose>>14)&63u),int((pose>>20)&63u));delta=(delta+ivec2(32))%ivec2(64)-ivec2(32);
    float age=mod(GameTime*24000.0-float((pose>>26)&31u)+16.0,32.0)-16.0;
    vec2 player=clamp(vec2(72.0)+vec2(offset)/8.0-egPoseVelocity(delta)*(1.0-clamp(age,-1.5,2.0)),vec2(64.0),vec2(80.0));
    if(decodeUnsigned(16,5)==77)player=clamp(enoSharedMotion(),vec2(64.0),vec2(80.0));
    vec4 canvas=egMapCanvas(variant,decodeUnsigned(32,0));
    float width=canvas.z;
    vec2 top=canvas.xy;
    float zoom=egTerrainZoom(alias);
    vec2 tileIndex=vec2(tile%2,tile/2);
    vec2 tileOrigin=tileIndex*72.0;
    // Coverage is fixed in screen space. Rebasing individual native textures
    // must never move their shared edge or expose the world between updates.
    // Neighbour texels in the gutters let all four quadrants sample one grid.
    vec2 quadrant=tileIndex+UV0;
    vec2 source=player+(quadrant-1.0)*(64.0/zoom)-tileOrigin;
    vec2 screen=top+quadrant*(width*0.5);
    enoTerrainUv=(source+vec2(11.0,19.0))/128.0;
    enoTerrainCanvas=vec4(top,vec2(width));
    enoTerrainShape=(variant/3)%2;enoTerrainProfile=variant%3;enoTerrain=1;
    gl_Position=vec4(screen.x/ScreenSize.x*2.0-1.0,1.0-screen.y/ScreenSize.y*2.0,-0.999,1.0);
    return true;
}
