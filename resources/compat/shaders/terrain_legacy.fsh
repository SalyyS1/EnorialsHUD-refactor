if(enoTerrain==1) {
    vec2 pixel=vec2(gl_FragCoord.x,ScreenSize.y-gl_FragCoord.y);
    vec2 local=(pixel-enoTerrainCanvas.xy)/enoTerrainCanvas.zw*2.0-1.0;
    float metric=enoTerrainShape!=0?max(abs(local.x),abs(local.y)):dot(local,local);
    if(metric>=(enoTerrainShape!=0?0.95:0.9025))discard;
    // Integrate source cells over a screen pixel, including the neighbouring
    // tile's gutter. Nearest sampling made thin lines pulse while walking.
    vec2 texel=enoTerrainUv*128.0;
    vec2 footprint=clamp(fwidth(texel),vec2(0.0001),vec2(5.0));
    vec2 lo=texel-footprint*0.5,hi=texel+footprint*0.5;
    ivec2 first=ivec2(floor(lo)),last=ivec2(ceil(hi));
    vec3 sum=vec3(0.0);float total=0.0;
    for(int y=0;y<6;y++)for(int x=0;x<6;x++) {
        ivec2 cell=first+ivec2(x,y);
        if(any(greaterThanEqual(cell,last)))continue;
        vec2 overlap=max(vec2(0.0),min(hi,vec2(cell)+1.0)-max(lo,vec2(cell)));
        float weight=overlap.x*overlap.y;
        vec4 sampleColor=texelFetch(Sampler0,clamp(cell,ivec2(0,8),ivec2(93,101)),0);
        remapColor(sampleColor,float(enoTerrainProfile));
        sum+=(sampleColor.a<0.1?vec3(0.0):sampleColor.rgb)*weight;
        total+=weight;
    }
    vec4 color=vec4(sum/max(total,0.000001),1.0);
    fragColor=color;return;
}
