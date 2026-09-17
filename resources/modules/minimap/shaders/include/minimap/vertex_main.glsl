// Legacy carrier projection. Corner order and header offsets are protocol compatibility.
fullscreenMinimap=0.0; transition=0.0; minimap=0.0; minimapShape=0.0;
keepEdges=0.0; frameStyle=0.0; frameThickness=0.0; colorProfile=0.0; sx=0.0; sy=0.0;
vec2 projectedUV=UV0, panelUV=vec2(0.0);
if(all(equal(textureSize(Sampler0,0),ivec2(128)))) {
    vec2 corner=vec2(UV0.y>0.5?0.0:1.0,UV0.x>0.5?1.0:0.0);
    vec2 aspect=vec2(ScreenSize.y/ScreenSize.x,ScreenSize.x/ScreenSize.y);
    if(aspect.x<aspect.y)aspect.y=1.0;else aspect.x=1.0;
    vec3 signature=texelFetch(Sampler0,clamp(ivec2(UV0*128.0),ivec2(0),ivec2(127)),0).rgb;
    bool terrainHeader=all(equal(signature,vec3(112.0,108.0,138.0)/255.0));
    int magic=decodeUnsigned(0,0);
    if(terrainHeader) {
        int sizeCode=decodeUnsigned(25,0);
        float scale=sizeCode==0?0.52:sizeCode==1?0.76:1.0;
        float margin=0.04,width=(0.7-margin)*scale,farEdge=margin+width;
        frameStyle=float(decodeUnsigned(41,0));frameThickness=float(decodeUnsigned(49,0));
        colorProfile=float(decodeUnsigned(57,0));minimapShape=decodeUnsigned(33,0)>0?1.0:0.0;
        panelUV=vec2(corner.x,1.0-corner.y);
        vec2 point=vec2(-1.0,1.0)+vec2(margin+width*corner.x,-margin-width*corner.y)*aspect;
        bool right=any(greaterThan(texelFetch(Sampler0,ivec2(17,0),0).rgb,vec3(0.0)));
        bool marker=any(greaterThan(texelFetch(Sampler0,ivec2(0,1),0).rgb,vec3(0.0)));
        bool coordinates=marker&&decodeUnsigned(1,1)==255;
        gl_Position=vec4(point,0.0,1.0);vcolor=Color;
        if(right)gl_Position.x+=2.0-(margin+farEdge)*aspect.x;
        if(coordinates) {
            gl_Position.y=1.0-(farEdge+0.015+corner.y*0.16*scale)*aspect.y;
            minimap=3.0;projectedUV=vec2(panelUV.x,1.0-panelUV.y);
        }else {
            const float NORTH=-1.5707963267948966;
            projectedUV=(UV0-vec2(0.5))*mat2_rotate_z(NORTH)/enorialsMinimapZoom()+vec2(0.5);
            projectedUV-=vec2(decodeFixedPoint(1,0),decodeFixedPoint(9,0))/128.0;
            minimap=marker?2.0:1.0;
            if(marker) {
                gl_Position.z-=decodeFixedPoint(1,1);
                vec2 pivot=vec2(decodeFixedPoint(1,2),decodeFixedPoint(9,1));
                projectedUV=(projectedUV-pivot)*mat2_rotate_z(-NORTH)+pivot;
                if(!any(greaterThan(texelFetch(Sampler0,ivec2(0,2),0).rgb,vec3(0.0))))vcolor=vec4(0.0);
                keepEdges=any(greaterThan(texelFetch(Sampler0,ivec2(9,2),0).rgb,vec3(0.0)))?1.0:0.0;
            }
        }
    }else if(magic==178) {
        vec2 tile=vec2(decodeUnsigned(8,0),decodeUnsigned(16,0));
        vec2 count=vec2(decodeUnsigned(24,0),decodeUnsigned(32,0));
        sx=tile.x/max(1.0,count.x-1.0);sy=tile.y/max(1.0,count.y-1.0);
        transition=decodeFixedPoint(40,0);fullscreenMinimap=1.0;
        vec2 origin=vec2(-count.x,count.y)*0.5*aspect*0.64;
        vec2 point=origin+vec2(1.0,-1.0)*(tile+corner)*aspect*0.64;
        point.y-=1.0-transition;
        gl_Position=vec4(point,-0.5,1.0);panelUV=vec2(corner.x,1.0-corner.y);vcolor=Color;
    }else if(magic==109) {
        transition=decodeFixedPoint(8,0);fullscreenMinimap=2.0;
        gl_Position=vec4(vec2(-1.0,1.0)+corner*vec2(2.0,-2.0),-0.4,1.0);
        panelUV=vec2(corner.x,1.0-corner.y);vcolor=Color;
    }
}
texCoord0=UV0;texCoord1=projectedUV;texCoord2=panelUV;vertexColor=vcolor;
// Minecraft 26.2 reversed depth. Preserve the established overlay ordering.
if(minimap>0.0||fullscreenMinimap>0.0)gl_Position.z=0.99-gl_Position.z*0.01;
