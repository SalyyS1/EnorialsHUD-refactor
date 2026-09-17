// Solid arrow, not a rotation of a texture: beveled polygon extruded through Y.
// All angles use one mesh and fixed illumination, with no per-angle sprite atlas.
const vec2 GPS_OUTLINE[7] = vec2[7](vec2(0.0,1.0), vec2(.63,.24), vec2(.24,.24),
    vec2(.24,-.90), vec2(-.24,-.90), vec2(-.24,.24), vec2(-.63,.24));

float gpsPolygon(vec2 p) {
    float d = dot(p-GPS_OUTLINE[0], p-GPS_OUTLINE[0]), signValue = 1.0;
    for (int i=0,j=6;i<7;j=i,i++) {
        vec2 a=GPS_OUTLINE[i], b=GPS_OUTLINE[j], edge=b-a, delta=p-a;
        vec2 nearest=delta-edge*clamp(dot(delta,edge)/dot(edge,edge),0.0,1.0);
        d=min(d,dot(nearest,nearest));
        bvec3 c=bvec3(p.y>=a.y,p.y<b.y,edge.x*delta.y>edge.y*delta.x);
        if (all(c)||all(not(c))) signValue=-signValue;
    }
    return signValue*sqrt(d);
}

float gpsSolid(vec3 p) {
    vec2 d=vec2(gpsPolygon(p.xz)+.045,abs(p.y)-.115);
    return min(max(d.x,d.y),0.0)+length(max(d,0.0))-.045;
}

vec3 gpsNormal(vec3 p) {
    const vec2 e=vec2(.002,0.0);
    return normalize(vec3(gpsSolid(p+e.xyy)-gpsSolid(p-e.xyy),
        gpsSolid(p+e.yxy)-gpsSolid(p-e.yxy),gpsSolid(p+e.yyx)-gpsSolid(p-e.yyx)));
}

vec4 gpsRender(vec2 uv, vec4 angles, float clock, int mode) {
    // A low view keeps the reference ring from suggesting a steep climb.
    vec3 eye=vec3(0.0,1.50,-4.5), view=normalize(-eye);
    vec3 screenRight=vec3(1.0,0.0,0.0), screenUp=normalize(cross(view,screenRight));
    vec2 screen=(uv-0.5)*vec2(2.45,-2.45);
    vec3 origin=eye+screenRight*screen.x+screenUp*screen.y;
    float pixel=max(length(dFdx(screen)),length(dFdy(screen)));
    // Thin horizontal reference ring. It stays fixed as the model tilts vertically.
    float planeT=(-.25-origin.y)/view.y;
    vec3 plane=origin+view*planeT;
    float ringDistance=abs(length(plane.xz)-.97);
    float ring=(1.0-smoothstep(.014,.014+pixel,ringDistance))*.38;
    vec4 result=vec4(.35,.51,.55,ring);
    if(mode==2) return result;

    float elapsed=mod(GameTime*24000.0-clock+64.0,64.0);
    // Timestamp can be one client tick ahead. Hold the start until it arrives.
    float blend=elapsed>62.0?0.0:clamp(elapsed,0.0,1.0);
    float delta=mod(angles.z-angles.x+256.0,512.0)-256.0;
    float yaw=(angles.x+delta*blend)*(6.28318530718/512.0);
    float elevation=(mix(angles.y,angles.w,blend)/255.0-.5)*3.14159265359;
    // Retain a shallow view of the head: a fully end-on prism becomes a bar.
    // The low cradle angle exposes depth without the former steep overhead view.
    vec3 forward=vec3(sin(yaw)*cos(elevation),sin(elevation),cos(yaw)*cos(elevation));
    // Roll about the pointing axis keeps the broad head readable at the poles.
    // This changes neither bearing nor elevation. The solid is symmetric under
    // a half-turn, so the sign flip at an end-on view has no visual discontinuity.
    vec3 right=cross(forward,view);
    right=dot(right,right)<.000001?vec3(1.0,0.0,0.0):normalize(right);
    // A small fixed roll exposes the copper side face, including broadside views.
    right=right*.978147601+cross(forward,right)*.207911691;
    vec3 up=cross(forward,right);
    mat3 basis=mat3(right,up,forward);
    vec3 ro=transpose(basis)*origin, rd=transpose(basis)*view;
    // Intersect a bounding sphere before marching the small solid's surface.
    float b=dot(ro,rd), disc=b*b-dot(ro,ro)+1.21;
    if(disc<0.0)return result;
    float t=max(0.0,-b-sqrt(disc)), end=-b+sqrt(disc), distanceToSurface=1.0;
    for(int i=0;i<48;i++) {
        distanceToSurface=gpsSolid(ro+rd*t);
        if(distanceToSurface<.0015||t>end)break;
        t+=distanceToSurface;
    }
    if(distanceToSurface>.0025||t>end)return result;
    vec3 point=ro+rd*t, normal=basis*gpsNormal(point);
    vec3 light=normalize(vec3(-.5,1.0,-.7));
    float diffuse=max(0.0,dot(normal,light));
    float specular=pow(max(0.0,dot(normal,normalize(light-view))),28.0)*.35;
    vec3 gold=mix(vec3(.66,.24,.035),vec3(1.0,.66,.16),.3+.7*diffuse);
    // A broad bright top and copper sides remain distinguishable at GUI scale.
    gold+=vec3(1.0,.82,.42)*specular;
    float rim=pow(1.0-max(0.0,dot(normal,-view)),3.0);
    gold*=1.0-.27*rim;
    return vec4(gold,1.0);
}
