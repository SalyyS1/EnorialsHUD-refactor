uint enoRgbaNibble(ivec2 texel) {
    ivec3 rgb=ivec3(round(texelFetch(Sampler0,texel,0).rgb*255.0));
    int base,value,a,c,d;
    if(rgb.r==rgb.g && rgb.g==rgb.b){base=2;value=rgb.r;a=122;c=155;d=185;}
    else if(rgb.g==0 && rgb.b==0){base=3;value=rgb.r;a=157;c=200;d=237;}
    else if(rgb.r<rgb.g){base=0;value=rgb.g;a=109;c=139;d=165;}
    else {base=1;value=rgb.r;a=152;c=193;d=230;}
    return uint(base*4+(value<=a?3:value<=c?0:value<=d?1:2));
}
vec4 enoRgbaPixel(ivec2 cell,ivec2 dimensions,int profile) {
    if(any(lessThan(cell,ivec2(0)))||any(greaterThanEqual(cell,dimensions)))return vec4(0.0);
    int first=1024+(cell.y*dimensions.x+cell.x)*8;uint value=0u;
    for(int i=0;i<8;i++){int at=first+i;value=(value<<4)|enoRgbaNibble(ivec2(at&127,at>>7));}
    vec4 color=vec4(float((value>>16)&255u),float((value>>8)&255u),float(value&255u),float((value>>24)&255u))/255.0;
    if(profile>=0)remapColor(color,float(profile));color.rgb*=color.a;return color;
}
vec4 enoRgbaSample(vec2 position,ivec2 dimensions,int profile) {
    vec2 point=position-0.5;ivec2 cell=ivec2(floor(point));vec2 blend=fract(point);
    if(profile>=0)blend=clamp((blend-0.5)/max(fwidth(point),vec2(0.00001))+0.5,0.0,1.0);
    vec4 value=mix(mix(enoRgbaPixel(cell,dimensions,profile),enoRgbaPixel(cell+ivec2(1,0),dimensions,profile),blend.x),mix(enoRgbaPixel(cell+ivec2(0,1),dimensions,profile),enoRgbaPixel(cell+ivec2(1,1),dimensions,profile),blend.x),blend.y);
    if(value.a>0.0)value.rgb/=value.a;return value;
}
