// Decode the map transport byte from eight non-black palette texels.
int decodeUnsigned(int x,int y) {
    int bits=0;
    for(int bit=7;bit>=0;--bit) {
        bits<<=1;
        if(any(greaterThan(texelFetch(Sampler0,ivec2(x+bit,y),0).rgb,vec3(0.0))))bits|=1;
    }
    return bits;
}
float decodeFixedPoint(int x,int y){return float(decodeUnsigned(x,y))/255.0;}
mat2 mat2_rotate_z(float angle){vec2 v=vec2(cos(angle),sin(angle));return mat2(v.x,-v.y,v.y,v.x);}
// Combat zoom changes the terrain coverage, preserving the chosen screen size.
// Map and marker layers carry the same transition, so positions stay aligned.
float enorialsMinimapZoom() {
    float fallback = decodeUnsigned(65, 0) > 0 ? 2.0 : 1.0;
    if (decodeUnsigned(49, 2) != 167) return fallback;
    int tick = decodeUnsigned(17, 2) | (decodeUnsigned(25, 2) << 8);
    float age = mod(GameTime * 24000.0 - float(tick) + 12000.0, 24000.0) - 12000.0;
    int fromByte=decodeUnsigned(33,2);
    float base=1.0+float(fromByte/64)*0.5;
    float fromZoom = base*(1.0+float(fromByte%64)/63.0);
    float toZoom = base*(1.0+float(decodeUnsigned(41,2)%64)/63.0);
    return mix(fromZoom, toZoom, smoothstep(0.0, 8.0, age));
}
