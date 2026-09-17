bool enoLegacyFullscreenVertex() {
    if(ProjMat[3][3]!=0.0||any(notEqual(textureSize(Sampler0,0),ivec2(128))))return false;
    if(decodeUnsigned(0,0)!=69||decodeUnsigned(8,0)!=78||decodeUnsigned(16,0)!=79||decodeUnsigned(24,0)!=51)return false;
    ivec2 tile=ivec2(decodeUnsigned(0,1),decodeUnsigned(8,1));
    ivec2 grid=ivec2(decodeUnsigned(16,1),decodeUnsigned(24,1));
    int start=decodeUnsigned(0,2),height=decodeUnsigned(8,2),profile=decodeUnsigned(16,2);
    if(any(lessThan(grid,ivec2(1)))||any(greaterThanEqual(tile,grid))||height<1||height>120)return false;
    float fade=float(decodeUnsigned(24,2))/255.0;
    float unit=min(ScreenSize.x,ScreenSize.y)*0.32/128.0;
    vec2 gridSize=vec2(grid)*128.0*unit;
    vec2 origin=(ScreenSize-gridSize)*0.5+vec2(0.0,(1.0-fade)*ScreenSize.y*0.5);
    vec2 local=UV0*vec2(128.0,float(height));
    vec2 cell=vec2(tile)*128.0+vec2(0.0,float(start))+local;
    vec2 screen=origin+cell*unit;
    enoFullCell=cell;enoFullLocal=local;enoFullData=ivec4(grid*128,height,profile);enoFullFade=fade;
    enoFullscreen=1;
    gl_Position=vec4(screen.x/ScreenSize.x*2.0-1.0,1.0-screen.y/ScreenSize.y*2.0,-0.9997,1.0);
    return true;
}
