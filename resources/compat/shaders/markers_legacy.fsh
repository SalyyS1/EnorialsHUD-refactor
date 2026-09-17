if(enoMarker==1) {
    vec2 pixel=vec2(gl_FragCoord.x,ScreenSize.y-gl_FragCoord.y);
    vec2 local=(pixel-enoMarkerCanvas.xy)/enoMarkerCanvas.zw*2.0-1.0;
    bool square=enoMarkerInfo.y!=0,hq=enoMarkerInfo.x==3;
    float metric=square?max(abs(local.x),abs(local.y)):dot(local,local);
    if(enoMarkerInfo.x==7) {
        fragColor=enoRgbaSample(enoMarkerLocal,enoMarkerSprite,-1);
        fragColor=egSealFrame(fragColor,local,enoMarkerInfo.z,square);
        if(fragColor.a<0.004)discard;return;
    }
    float limit=enoMarkerInfo.w==1?0.93+(square?0.02:0.075):(square?0.95:0.9025);
    if(enoMarkerInfo.x!=1){float outer=1.0+6.0/enoMarkerCanvas.z;limit=square?outer:outer*outer;}
    if(metric>=limit)discard;
    if(enoMarkerInfo.x==1) {
        vec4 color=egColor;
        float alpha=clamp(enoMarkerMotion.x+0.5-length(enoMarkerLocal),0.0,1.0);if(alpha<=0.0)discard;
        fragColor=vec4(color.rgb,alpha);return;
    }
    vec2 samplePoint=enoMarkerLocal/enoMarkerMotion.y;
    if(enoMarkerInfo.x==2){float c=cos(enoMarkerMotion.z),s=sin(enoMarkerMotion.z);samplePoint=vec2(c*samplePoint.x+s*samplePoint.y,-s*samplePoint.x+c*samplePoint.y);}
    if(enoMarkerInfo.x==5) {
        ivec2 cell=ivec2(floor(samplePoint+vec2(enoMarkerSprite)*0.5));
        if(cell.x<0||cell.x>=enoMarkerSprite.x||cell.y<enoMarkerSlice.x||cell.y>=enoMarkerSlice.x+enoMarkerSlice.y)discard;
        fragColor=texelFetch(Sampler0,ivec2(cell.x,8+cell.y-enoMarkerSlice.x),0);
        remapColor(fragColor,float(enoMarkerInfo.z));
    } else fragColor=enoRgbaSample(samplePoint+vec2(enoMarkerSprite)*0.5,enoMarkerSprite,(hq||enoMarkerInfo.x==6)?-1:enoMarkerInfo.z);
    if(fragColor.a<0.1)discard;return;
}
