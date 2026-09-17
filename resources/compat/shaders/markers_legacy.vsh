bool enoLegacyMarkerVertex() {
    if(ProjMat[3][3]!=0.0||any(notEqual(textureSize(Sampler0,0),ivec2(128))))return false;
    if(decodeUnsigned(0,0)!=69||decodeUnsigned(8,0)!=78||decodeUnsigned(16,0)!=79||decodeUnsigned(24,0)!=50)return false;
    int family=decodeUnsigned(0,1),variant=decodeUnsigned(8,1),order=decodeUnsigned(32,1);
    if(family<1||family>7||variant>35)return false;
    enoMarkerSlice=(family==5||family==7)?ivec2(decodeUnsigned(0,3),decodeUnsigned(8,3)):ivec2(0);
    uvec3 data=uvec3(0u);
    for(int b=0;b<8;b++)data[b/4]|=uint(decodeUnsigned(b*8,2))<<((b%4)*8);
    data.z=uint(decodeUnsigned(64,2));
    vec2 dimensions=vec2(decodeUnsigned(16,1),decodeUnsigned(24,1));
    enoMarkerSprite=ivec2(dimensions);
    bool hq=family==3;
    if(hq)dimensions*=0.5;
    if(family==6)dimensions*=0.375;
    int square=(variant/3)%2;
    int size=variant/12;
    float sizeScale=size==0?0.52:(size==1?0.76:1.0);
    vec4 viewport=egMapCanvas(variant,decodeUnsigned(32,0));
    float width=viewport.z,mapPixel=width/128.0;
    vec2 canvas=viewport.xy;
    if(family==7) {
        vec2 origin=vec2(decodeUnsigned(0,2)|(decodeUnsigned(8,2)<<8),decodeUnsigned(16,2)|(decodeUnsigned(24,2)<<8));
        vec2 point=UV0*vec2(enoMarkerSlice);
        vec2 screen=canvas-width*.50+(origin+point)*(width*2.0/500.0);
        enoMarkerLocal=point+1.0;
        enoMarkerCanvas=vec4(canvas,vec2(width));enoMarkerInfo=ivec4(7,square,decodeUnsigned(32,0),0);enoMarkerMotion=vec4(0.0);
        gl_Position=vec4(screen.x/ScreenSize.x*2.0-1.0,1.0-screen.y/ScreenSize.y*2.0,-0.9993,1.0);
        enoMarker=1;return true;
    }
    vec2 offset=vec2(0.0);float zoom=1.0,angle=0.0,spriteScale=1.0;bool pinned=false;
    int category=0;
    if(family==1) {
        offset=vec2(egBits(data,0,13),egBits(data,13,13))/32.0-128.0;
        ivec2 delta=ivec2(egBits(data,26,7),egBits(data,33,7));delta=(delta+ivec2(64))%ivec2(128)-ivec2(64);
        float age=mod(GameTime*24000.0-float(egBits(data,40,5))+16.0,32.0)-16.0;
        offset-=egPoseVelocity(delta)*(1.0-clamp(age/max(1.0,float(egBits(data,45,4))),-1.5,2.0));
        if(decodeUnsigned(16,5)==77)offset=enoSharedMotion();
        zoom=egZoom(float(egBits(data,49,8)),float(egBits(data,62,1)),float(egBits(data,57,5)));
        offset=offset*zoom+0.5;category=min(2,int(egBits(data,63,2)));
        egColor=EG_RADAR_COLORS[category];remapColor(egColor,float(variant%3));
    } else if(family==2) {
        float from=float(egBits(data,0,9)),to=float(egBits(data,9,9));
        float age=mod(GameTime*24000.0-float(egBits(data,18,5))+16.0,32.0)-16.0;
        float yaw=from+(mod(to-from+256.0,512.0)-256.0)*clamp(age/max(1.0,float(egBits(data,23,4))),0.0,1.0);
        angle=yaw*6.28318530718/512.0+3.14159265359;
    } else {
        bool fixedToMap;
        ivec2 delta;
        float tick;
        if(hq) {
            offset=vec2(egBits(data,0,13),egBits(data,13,13))/32.0-128.0;
            int settings=int(egBits(data,26,8)),encodedZoom=settings&31,style=settings>>5;
            fixedToMap=egBits(data,41,1)!=0u;pinned=egBits(data,40,1)!=0u;
            zoom=fixedToMap?1.0:egZoom(float((encodedZoom>>3)*64+(encodedZoom&7)*9),float(egBits(data,39,1)),float(egBits(data,34,5)));
            delta=ivec2(egBits(data,42,6),egBits(data,48,6));tick=float(egBits(data,54,5));
            int markerSize=style&3;float userSize=markerSize==1?0.75:(markerSize==2?1.25:(markerSize==3?1.5:1.0));
            spriteScale=userSize*((style&4)==0?zoom*(1.0-0.15*smoothstep(1.5,2.0,zoom)):0.76/sizeScale);dimensions*=spriteScale;
        } else {
            offset=vec2(egBits(data,0,16),egBits(data,16,16))/32.0-1024.0;
            fixedToMap=egBits(data,47,1)!=0u;pinned=egBits(data,46,1)!=0u;
            zoom=fixedToMap?1.0:egZoom(float(egBits(data,32,8)),float(egBits(data,45,1)),float(egBits(data,40,5)));
            delta=ivec2(egBits(data,48,6),egBits(data,54,6));tick=float(egBits(data,60,5));
        }
        delta=(delta+ivec2(32))%ivec2(64)-ivec2(32);
        float age=mod(GameTime*24000.0-tick+16.0,32.0)-16.0;
        if(!fixedToMap)offset+=egPoseVelocity(delta)*(1.0-clamp(age,-1.5,2.0));
        if(!fixedToMap&&decodeUnsigned(16,5)==77)offset=enoSharedMotion();
        offset*=zoom;
        if(pinned && family==5) {
            // Match the established runtime-icon clamp, including oversized icons.
            // A zero bound on one axis must not collapse the other axis to the centre.
            vec2 halfIcon=dimensions*0.5;float factor=1.0;
            if(square!=0){vec2 bounds=max(vec2(1.0),vec2(63.0)-halfIcon);factor=min(1.0,min(bounds.x/max(abs(offset.x),0.000001),bounds.y/max(abs(offset.y),0.000001)));}
            else factor=min(1.0,max(1.0,60.0-max(halfIcon.x,halfIcon.y))/max(length(offset),0.000001));
            offset*=factor;
        } else if(pinned) {
            vec2 alignment=hq?vec2(0.0):mod(dimensions,2.0)*0.5;
            vec2 center=offset+alignment,halfIcon=dimensions*0.5+vec2(1.0/mapPixel);
            float radius=hq?64.0+3.0/mapPixel:64.0*(square!=0?0.95:sqrt(0.9025)),factor=1.0;
            if(square!=0) {vec2 bounds=max(vec2(0.0),vec2(radius)-halfIcon);factor=min(1.0,min(bounds.x/max(abs(center.x),0.000001),bounds.y/max(abs(center.y),0.000001)));}
            else if(length(abs(center)+halfIcon)>radius) {float a=dot(center,center),b=dot(abs(center),halfIcon),c=dot(halfIcon,halfIcon)-radius*radius;factor=clamp((-b+sqrt(max(0.0,b*b-a*c)))/max(a,0.000001),0.0,1.0);}
            offset=center*factor-alignment;
        }
        if(!hq)offset+=mod(dimensions,2.0)*0.5;
    }
    vec2 center=canvas+width*0.5+offset*mapPixel;
    vec2 halfSize=family==1?vec2(mapPixel+0.5):(family==2?vec2(length(dimensions)*0.5):dimensions*0.5)*mapPixel+1.0;
    enoMarkerLocal=(UV0*2.0-1.0)*halfSize;
    enoMarkerCanvas=vec4(canvas,vec2(width));enoMarkerInfo=ivec4(family,square,variant%3,pinned?1:0);
    enoMarkerMotion=vec4(mapPixel,mapPixel*(family==6?0.375:(hq?0.5*spriteScale:1.0)),angle,float(category));
    vec2 screen=center+enoMarkerLocal;
    gl_Position=vec4(screen.x/ScreenSize.x*2.0-1.0,1.0-screen.y/ScreenSize.y*2.0,-0.9996-float(order)*0.000001,1.0);
    enoMarker=1;return true;
}
