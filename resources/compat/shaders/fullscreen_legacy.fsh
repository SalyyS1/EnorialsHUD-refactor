if(enoFullscreen==1) {
    ivec2 cell=ivec2(floor(enoFullCell));
    int edge=min(min(cell.x,cell.y),min(enoFullData.x-1-cell.x,enoFullData.y-1-cell.y));
    vec4 color;
    if(edge>=0&&edge<3)color=vec4(vec3(edge==1?70.0:40.0)/255.0,1.0);
    else {
        ivec2 at=clamp(ivec2(floor(enoFullLocal)),ivec2(0),ivec2(127,enoFullData.z-1));
        color=texelFetch(Sampler0,at+ivec2(0,8),0);
        if(color.a<0.1)color=vec4(0.0,0.0,0.0,1.0);
        remapColor(color,float(enoFullData.w));
    }
    fragColor=vec4(color.rgb,enoFullFade);if(fragColor.a<0.001)discard;return;
}
