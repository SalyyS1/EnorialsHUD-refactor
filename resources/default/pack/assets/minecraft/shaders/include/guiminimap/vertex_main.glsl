// GUI-only transport. Each native glyph remains four GUI units wide, but its
// position and color carry a raster index and world coordinates respectively.
egKind = 0;
egVariant = 0;
egAlias = 0;
egColor = vec4(0.0);
egCellMin = vec2(0.0);
egCellMax = vec2(0.0);
egCanvas = vec4(0.0);
egSettings = ivec4(0);
egFade = 1.0;
egSprite = ivec4(0);
egMotion = vec4(0.0);
egColor2 = vec4(0.0);
egColor3 = vec4(0.0);
egBundleLength = 0;
#ifdef IS_GUI
int egFrameTheme = int(floor(Position.y / 5000000.0));
float egEncodedY = Position.y - float(egFrameTheme) * 5000000.0;
int egFamily = 0;
int egStyleVariant = 0;
if (egEncodedY >= 1000000.0 && egEncodedY < 1147500.0) {
    egFamily = 1; egStyleVariant = int(floor((egEncodedY - 1000000.0) / 4096.0));
} else if (egEncodedY >= 1250000.0 && egEncodedY < 1397500.0) {
    egFamily = 2; egStyleVariant = int(floor((egEncodedY - 1250000.0) / 4096.0));
} else if (egEncodedY >= 1500000.0 && egEncodedY < 1512400.0) {
    egFamily = 3; egStyleVariant = int(floor((egEncodedY - 1500000.0) / 4096.0));
} else if (egEncodedY >= 1600000.0 && egEncodedY < 1600500.0) {
    egFamily = 4;
} else if (egEncodedY >= 1700000.0 && egEncodedY < 1847500.0) {
    egFamily = 5; egStyleVariant = int(floor((egEncodedY - 1700000.0) / 4096.0));
} else if (egEncodedY >= 2000000.0 && egEncodedY < 2147500.0) {
    egFamily = 6; egStyleVariant = int(floor((egEncodedY - 2000000.0) / 4096.0));
} else if (egEncodedY >= 2250000.0 && egEncodedY < 2397500.0) {
    egFamily = 7; egStyleVariant = int(floor((egEncodedY - 2250000.0) / 4096.0));
} else if (egEncodedY >= 2500000.0 && egEncodedY < 2647500.0) {
    egFamily = 8; egStyleVariant = int(floor((egEncodedY - 2500000.0) / 4096.0));
} else if (egEncodedY >= 2750000.0 && egEncodedY < 2897500.0) {
    egFamily = 9; egStyleVariant = int(floor((egEncodedY - 2750000.0) / 4096.0));
} else if (egEncodedY >= 3000000.0 && egEncodedY < 3147500.0) {
    egFamily = 10; egStyleVariant = int(floor((egEncodedY - 3000000.0) / 4096.0));
} else if (egEncodedY >= 3250000.0 && egEncodedY < 3397500.0) {
    egFamily = 11; egStyleVariant = int(floor((egEncodedY - 3250000.0) / 4096.0));
} else if (egEncodedY >= 3500000.0 && egEncodedY < 3647500.0) {
    egFamily = 12; egStyleVariant = int(floor((egEncodedY - 3500000.0) / 4096.0));
} else if (egEncodedY >= 3750000.0 && egEncodedY < 3762400.0) {
    egFamily = 13; egStyleVariant = int(floor((egEncodedY - 3750000.0) / 4096.0));
}
if (egEncodedY >= 4000000.0 && egEncodedY < 4147500.0) { egFamily=14; egStyleVariant=int(floor((egEncodedY-4000000.0)/4096.0)); }
bool egHqMarker=egFamily==14;
int egMarkerStyle=0;
if(egHqMarker)egFamily=11;
if(egEncodedY>=4250000.0 && egEncodedY<4397500.0){egFamily=15;egStyleVariant=int(floor((egEncodedY-4250000.0)/4096.0));}
bool egMovingHead=egEncodedY>=4500000.0 && egEncodedY<4647500.0;
if(egMovingHead){egFamily=15;egStyleVariant=int(floor((egEncodedY-4500000.0)/4096.0));}
if (egFamily != 0) {
    egKind = egFamily == 10 ? 1 : (egFamily == 11 ? 7 : (egFamily == 12 ? 6 : egFamily));
    egVariant = egStyleVariant;
    // Native bitmap UVs are inset by +.01/- .01 texel. Vertex ordering is not
    // stable across GUI batches; using it corrupts map data after pack reloads.
    ivec2 atlasSize = textureSize(Sampler0, 0);
    vec2 atlasTexel = UV0 * vec2(atlasSize);
    vec2 corner = vec2(lessThan(atlasTexel - round(atlasTexel), vec2(0.0)));
    float guiScale = max(1.0, round(ProjMat[0][0] * 0.5 * ScreenSize.x));
    float guiWidth = ceil(ScreenSize.x / guiScale);
    int cursor = int(round(Position.x - corner.x * 4.0 - floor(guiWidth * 0.5)));
    int index = cursor >> 8;
    int runLength = (egFamily == 1 || egFamily == 3) ? 1 + (index >> 14) : 1;
    if (egFamily == 1 || egFamily == 3) index &= 16383;
    int palette = cursor & 255;
    ivec3 rgb = ivec3(round(Color.rgb * 255.0));
    int payload = (rgb.r << 16) | (rgb.g << 8) | rgb.b;
    int glyphPixels = (egFamily == 2 || egFamily == 4) ? 4 : 64;
    if (egFamily == 2) glyphPixels = 254;
    if (egFamily == 6 || egFamily == 10 || egFamily == 12 || egFamily == 13) glyphPixels = 1;
    if (egFamily == 15) glyphPixels = 1;
    if (egFamily == 7 || egFamily == 8) glyphPixels = 128;
    if (egFamily == 11) glyphPixels = egHqMarker ? 32 : 16;
    ivec2 atlasOrigin = ivec2(round(UV0 * vec2(atlasSize))) - ivec2(corner * float(glyphPixels));
    if (any(lessThan(atlasOrigin, ivec2(0))) || any(greaterThan(atlasOrigin + ivec2(glyphPixels), atlasSize))) {
        egKind = -1; gl_Position = vec4(2.0,2.0,2.0,1.0); return;
    }
    if (egFamily == 2) egSprite = ivec4(atlasOrigin,252,252);
    ivec4 metadata = ivec4(round(texelFetch(Sampler0, atlasOrigin, 0) * 255.0));
    egAlias = metadata.b;
    uvec3 data = uvec3(0u);
    uint poseShadow = 0u;
    vec2 cameraMarkerAnchor=vec2(0.0);
    if ((egFamily >= 6 && egFamily <= 12) || egFamily == 15) {
        // Main glyph is black; only its explicit, lossless shadow carries data.
        if ((rgb.b & 1) == 0) { egKind = -1; gl_Position = vec4(2.0, 2.0, 2.0, 1.0); return; }
        uint shadow = (uint(round(Color.a * 255.0)) << 24) | uint(payload);
        uint high = uint(cursor - 1 + 8388608);
        int alias = egFamily == 6 || egFamily == 10 || egFamily == 12 || egFamily == 15 ? metadata.g * 256 + metadata.b : 0;
        if (egFamily == 11) {
            ivec2 aliasMetadata = ivec2(round(texelFetch(Sampler0, atlasOrigin + ivec2(1,0), 0).rg * 255.0));
            alias = aliasMetadata.x * 256 + aliasMetadata.y;
        }
        data = uvec3((shadow >> 1) | ((high & 1u) << 31), (high >> 1) | (uint(alias & 511) << 23), uint(alias >> 9));
        if(egHqMarker) {
            // Expand the compact shared-marker payload into the existing pose layout.
#ifdef ENO_CAMERA_TERRAIN
            cameraMarkerAnchor=vec2(egBits(data,0,21),egBits(data,21,21))/32.0;
            uint settings=egBits(data,42,8);
            uint zoomByte=((settings>>3)&3u)*64u+(settings&7u)*9u;
            uint header=zoomByte|(egBits(data,50,5)<<8)|(egBits(data,55,1)<<13)
                |(egBits(data,56,1)<<14)|(egBits(data,57,1)<<15);
            egMarkerStyle=int(settings>>5);
            data=uvec3(0u,header,0u);
#else
            uint x=egBits(data,0,13)+28672u,z=egBits(data,13,13)+28672u;
            uint settings=egBits(data,26,8);
            egMarkerStyle=int(settings>>5);
            uint zoomByte=((settings>>3)&3u)*64u+(settings&7u)*9u;
            uint header=zoomByte|(egBits(data,34,5)<<8)|(egBits(data,39,1)<<13)
                |(egBits(data,40,1)<<14)|(egBits(data,41,1)<<15)|(egBits(data,42,6)<<16)
                |(egBits(data,48,6)<<22)|(egBits(data,54,4)<<28);
            data=uvec3(x|(z<<16),header,egBits(data,58,1));
#endif
        }
        if (egFamily == 9) { index = int(high) >> 8; palette = int(high) & 255; }
        if (egFamily == 10) {
            // Exact 31-bit shadow plus 14-bit alias: no camera-world data.
            poseShadow = shadow >> 1;
            egAlias = alias;
            index = (int(high) >> 8) & 32767;
            runLength = 1 + (int(high) >> 23);
            palette = int(high) & 255;
        }
    }
    if (egFamily == 1 || egFamily == 3 || egFamily == 5 || egFamily == 9) {
        egColor = texelFetch(Sampler0, atlasOrigin + ivec2(palette & 15, palette >> 4) * 4 + ivec2(2), 0);
        // Palette entries 0..3 are opaque black only for complete terrain;
        // sparse overlay callers omit them. This prevents gaps in coverage.
        remapColor(egColor, float(egFamily == 3 ? egStyleVariant : egStyleVariant % 3));
    }
    if (egFamily == 10) {
        egColor = EG_PALETTE[palette];
        remapColor(egColor, float(egStyleVariant % 3));
    }
    if (egFamily == 13) {
        // Nonzero RGB grid context guarantees a native shadow even at fade0.
        // Foreground is black and never contributes geometry to the canvas.
        if (payload == 0) { egKind = -1; gl_Position = vec4(2.0,2.0,2.0,1.0); return; }
        int source = cursor - 1;
        index = (source >> 8) & 16383;
        int mode = (source >> 22) & 3;
        palette = source & 255;
        egAlias = metadata.g * 256 + metadata.b;
        egBundleLength = mode == 3 ? 0 : mode + 1;
        runLength = mode == 3 ? 1 + (egAlias & 255) : egBundleLength;
        egColor = EG_PALETTE[mode == 3 ? palette : egAlias & 255];
        egColor2 = EG_PALETTE[(egAlias >> 8) & 255];
        egColor3 = EG_PALETTE[palette];
        remapColor(egColor, float(egStyleVariant));
        remapColor(egColor2, float(egStyleVariant));
        remapColor(egColor3, float(egStyleVariant));
        egFade = Color.a;
    }
    float minimum = min(ScreenSize.x, ScreenSize.y);
    int size = egStyleVariant / 12;
    int right = (egStyleVariant / 6) % 2;
    int square = (egStyleVariant / 3) % 2;
    float sizeScale = size == 0 ? 0.52 : (size == 1 ? 0.76 : 1.0);
    egCanvas = egMapCanvas(egStyleVariant,egFrameTheme);
    float width = egCanvas.z;
    vec2 canvasMin = egCanvas.xy;
    egSettings = ivec4(square, 0, 1, egStyleVariant % 3);
    if (egFamily == 15) {
        uint rgb24=egBits(data,0,24);
        egColor=vec4(float((rgb24>>16)&255u),float((rgb24>>8)&255u),float(rgb24&255u),255.0)/255.0;
#ifdef ENO_CAMERA_TERRAIN
        vec2 anchor=vec2(egBits(data,24,13),egBits(data,37,13))/32.0;
        vec2 center=mod(anchor-vec2(CameraBlockPos.xz&ivec2(255))+128.0,256.0)-128.0+CameraOffset.xz;
        float zoom=egZoom(float((egStyleVariant%3)*64)+float(egBits(data,60,3))*9.0,float(egBits(data,68,1)),float(egBits(data,63,5)));
        center*=zoom;
        float radius=64.0*.95,halfHead=7.0,factor=1.0;
        if(square!=0)factor=min(1.0,min((radius-halfHead)/max(abs(center.x),.000001),(radius-halfHead)/max(abs(center.y),.000001)));
        else if(length(abs(center)+halfHead)>radius){float a=dot(center,center),b=(abs(center.x)+abs(center.y))*halfHead,c=2.0*halfHead*halfHead-radius*radius;factor=clamp((-b+sqrt(max(0.0,b*b-a*c)))/max(a,.000001),0.0,1.0);}
        vec2 point=center*factor-6.0+vec2(egBits(data,50,5),egBits(data,55,5))*.375;
        if(egMovingHead){
            float level=1.0+float(egStyleVariant%3)*.5;
            vec2 world=vec2(egBits(data,31,13),egBits(data,44,13))/32.0;
            point=mod(world-vec2(CameraBlockPos.xz&ivec2(511))*level+128.0,256.0)-128.0+CameraOffset.xz*level;
            int dx=int(egBits(data,29,2)|(egBits(data,57,3)<<2)),dz=int(egBits(data,60,5));
            dx=(dx+16)%32-16;dz=(dz+16)%32-16;
            const float speed[16]=float[16](0.0,.03125,.0625,.125,.1875,.25,.375,.5,.75,1.0,1.5,2.0,3.0,4.0,6.0,8.0);
            vec2 velocity=vec2(sign(float(dx))*speed[min(15,abs(dx))],sign(float(dz))*speed[min(15,abs(dz))]);
            float age=mod(GameTime*24000.0-float(egBits(data,24,5))+16.0,32.0)-16.0;
            point+=velocity*(1.0-clamp(age,0.0,1.0));
        }
#else
        vec2 point=vec2(egBits(data,31,13),egBits(data,44,13))/32.0-128.0;
        int dx=int(egBits(data,29,2)|(egBits(data,57,3)<<2)),dz=int(egBits(data,60,5));
        dx=(dx+16)%32-16;dz=(dz+16)%32-16;
        const float headSpeed[16]=float[16](0.0,0.03125,0.0625,0.125,0.1875,0.25,0.375,0.5,0.75,1.0,1.5,2.0,3.0,4.0,6.0,8.0);
        vec2 velocity=vec2(sign(float(dx))*headSpeed[min(15,abs(dx))],sign(float(dz))*headSpeed[min(15,abs(dz))]);
        float age=mod(GameTime*24000.0-float(egBits(data,24,5))+16.0,32.0)-16.0;
        point+=velocity*(1.0-clamp(age,-1.5,2.0));
#endif
        float mapPixel=width/128.0;
        egCellMin=canvasMin+width*.5+point*mapPixel;
        egCellMax=egCellMin+vec2(0.375*mapPixel);
    } else if (egFamily == 10) {
#ifdef ENO_CAMERA_TERRAIN
        ivec2 anchor=ivec2(int(poseShadow&8191u),int((poseShadow>>13)&8191u));
        ivec2 delta=((anchor-(CameraBlockPos.xz&ivec2(8191))+ivec2(4096))&ivec2(8191))-ivec2(4096);
        vec2 player=vec2(72.0)-vec2(delta)-CameraOffset.xz;
#else
        ivec2 quantizedOffset = ivec2(int(poseShadow & 127u), int((poseShadow >> 7) & 127u));
        quantizedOffset = (quantizedOffset + ivec2(64)) % ivec2(128) - ivec2(64);
        uint motion = poseShadow >> 14;
        ivec2 quantizedDelta = ivec2(int(motion & 63u), int((motion >> 6) & 63u));
        quantizedDelta = (quantizedDelta + ivec2(32)) % ivec2(64) - ivec2(32);
        float age = mod(GameTime * 24000.0 - float((motion >> 12) & 31u) + 16.0, 32.0) - 16.0;
        vec2 player = vec2(72.0) + vec2(quantizedOffset) / 8.0 - egPoseVelocity(quantizedDelta) * (1.0 - clamp(age, -1.5, 2.0));
        // Delayed packets cannot extrapolate beyond the available 144-block raster.
        player = clamp(player, vec2(64.0), vec2(80.0));
#endif
        float zoom = egTerrainZoom(egAlias);
        float pixel = width / 128.0 * zoom;
        egCellMin = canvasMin + width * 0.5 + (vec2(index % 144, index / 144) - player) * pixel;
        egCellMax = egCellMin + vec2(pixel * float(runLength), pixel);
    } else if (egFamily == 1) {
        ivec2 anchor = ivec2(payload & 255, (payload >> 8) & 255);
        ivec2 delta = ((anchor - (CameraBlockPos.xz & ivec2(255)) + ivec2(128)) & ivec2(255)) - ivec2(128);
        vec2 relative = vec2(delta) + CameraOffset.xz;
        float zoom = egZoom(float((payload >> 16) & 255),float((egAlias >> 5) & 1),float(egAlias & 31));
        float pixel = width / 128.0 * zoom;
        egCellMin = canvasMin + vec2(width * 0.5) + (relative + vec2(index % 144, index / 144)) * pixel;
        egCellMax = egCellMin + vec2(pixel * float(runLength), pixel);
    } else if (egFamily == 2) {
        egSettings.y = payload & 7;
        egSettings.z = (payload >> 3) & 3;
        egSettings.w = 4;
        vec2 tile = vec2(metadata.r % 2, metadata.r / 2);
        egMotion.xy = tile * 250.0;
        egCellMin = canvasMin - width * 0.50 + tile * width;
        egCellMax = egCellMin + vec2(width);
    } else if (egFamily == 3 || egFamily == 13) {
        int columns = max(1, payload & 255);
        int rows = max(1, (payload >> 8) & 255);
        int rasterWidth = columns * 128;
        int rowsPerChunk = max(1, 16000 / rasterWidth);
        ivec2 cell = ivec2(index % rasterWidth, index / rasterWidth + ((payload >> 16) & 255) * rowsPerChunk);
        if (egFamily == 3) egFade = float(egAlias) / 255.0;
        float pixel = minimum * 0.32 / 128.0;
        vec2 gridSize = vec2(columns, rows) * (minimum * 0.32);
        vec2 gridMin = (ScreenSize - gridSize) * 0.5 + vec2(0.0, (1.0 - egFade) * ScreenSize.y * 0.5);
        egCanvas = vec4(gridMin, gridSize);
        egCellMin = gridMin + vec2(cell) * pixel;
        egCellMax = egCellMin + vec2(pixel * float(runLength), pixel);
        if (egFamily == 13) egMotion.z = pixel;
        int edge = min(min(cell.x, cell.y), min(rasterWidth - 1 - cell.x, rows * 128 - 1 - cell.y));
        if (edge >= 0 && edge < 3) egColor = vec4(vec3(edge == 1 ? 70.0 : 40.0) / 255.0, 1.0);
    } else if (egFamily == 4) {
        egFade = float(egAlias) / 255.0;
        egCellMin = vec2(0.0);
        egCellMax = ScreenSize;
    } else if (egFamily == 5) {
        vec2 panelSize = vec2(width, minimum * 0.08 * sizeScale);
        vec2 panelMin = canvasMin + vec2(0.0, width * (1.0+egFrameOverhang(egStyleVariant,egFrameTheme).w) + minimum * 0.0075);
        egCellMin = panelMin + vec2(index & 127, index >> 7) * panelSize / 128.0;
        egCellMax = egCellMin + panelSize / 128.0;
    } else if (egFamily >= 6) {
        float mapPixel = width / 128.0;
        vec2 offset = vec2(0.0);
        float zoom = 1.0;
        if (egFamily == 6 || egFamily == 12) {
            vec2 anchor = vec2(egBits(data,0,13), egBits(data,13,13)) / 32.0;
#ifdef ENO_CAMERA_TERRAIN
            offset = mod(anchor - vec2(CameraBlockPos.xz & ivec2(255)) + 128.0,256.0)-128.0+CameraOffset.xz;
#else
            offset = egFamily == 12 ? anchor - 128.0 : mod(anchor - vec2(CameraBlockPos.xz & ivec2(255)) + 128.0, 256.0) - 128.0 + CameraOffset.xz;
#endif
            ivec2 delta = ivec2(egBits(data,26,7), egBits(data,33,7));
            delta = ivec2(delta.x >= 64 ? delta.x - 128 : delta.x, delta.y >= 64 ? delta.y - 128 : delta.y);
            float age = mod(GameTime * 24000.0 - float(egBits(data,40,5)) + 16.0, 32.0) - 16.0;
#ifdef ENO_CAMERA_TERRAIN
            offset -= vec2(delta)/16.0*(1.0-clamp(age/max(1.0,float(egBits(data,45,4))),0.0,1.0));
#else
            offset -= (egFamily == 12 ? egPoseVelocity(delta) : vec2(delta) / 16.0) * (1.0 - clamp(age / max(1.0,float(egBits(data,45,4))),egFamily == 12 ? -1.5 : 0.0,egFamily == 12 ? 2.0 : 1.0));
#endif
            zoom = egZoom(float(egBits(data,49,8)),float(egBits(data,62,1)),float(egBits(data,57,5)));
            egColor = EG_RADAR_COLORS[min(2,int(egBits(data,63,2)))];
            remapColor(egColor, float(egStyleVariant % 3));
            vec2 center = canvasMin + width * 0.5 + (offset * zoom + vec2(0.5)) * mapPixel;
            egMotion = vec4(center, mapPixel, 0.0);
            egCellMin = center - mapPixel - 0.5;
            egCellMax = center + mapPixel + 0.5;
        } else if (egFamily == 7 || egFamily == 8 || egFamily == 11) {
            egSprite = ivec4(atlasOrigin, metadata.g, metadata.b);
            vec2 dimensions = vec2(egSprite.zw) * (egHqMarker ? 0.5 : 1.0);
            if(egHqMarker)egSettings.w+=4;
            float angle = 0.0;
            float spriteScale = 1.0;
            if (egFamily == 7 || egFamily == 11) {
                vec2 anchor = vec2(egBits(data,0,16),egBits(data,16,16)) / (egFamily == 11 ? 32.0 : 16.0);
                bool fixedToMap = egBits(data,47,1) != 0u;
                if (egFamily == 11) {
#ifdef ENO_CAMERA_TERRAIN
                    if(egHqMarker)offset=fixedToMap?mod(cameraMarkerAnchor+32768.0,65536.0)-32768.0:mod(cameraMarkerAnchor-vec2(CameraBlockPos.xz&ivec2(65535))+32768.0,65536.0)-32768.0+CameraOffset.xz;
                    else offset = anchor - 1024.0;
#else
                    offset = anchor - 1024.0;
#endif
                    ivec2 delta = ivec2(egBits(data,48,6),egBits(data,54,6));
                    delta = (delta + ivec2(32)) % ivec2(64) - ivec2(32);
                    float age = mod(GameTime * 24000.0 - float(egBits(data,60,5)) + 16.0, 32.0) - 16.0;
                    if (!fixedToMap) offset += egPoseVelocity(delta) * (1.0 - clamp(age,-1.5,2.0));
                } else offset = fixedToMap ? anchor - 2048.0 : mod(anchor - vec2(CameraBlockPos.xz & ivec2(4095)) + 2048.0,4096.0)-2048.0+CameraOffset.xz;
                zoom = fixedToMap ? 1.0 : egZoom(float(egBits(data,32,8)),float(egBits(data,45,1)),float(egBits(data,40,5)));
                if(egHqMarker){ 
                    int markerSize=egMarkerStyle&3;
                    float userSize=markerSize==1?0.75:(markerSize==2?1.25:(markerSize==3?1.5:1.0));
                    bool autoScale=(egMarkerStyle&4)==0;
                    spriteScale=userSize*(autoScale?zoom*(1.0-0.15*smoothstep(1.5,2.0,zoom)):0.76/sizeScale);
                    dimensions*=spriteScale; }
                offset *= zoom;
                if (egBits(data,46,1) != 0u) {
                    // Clamp the complete filtered sprite inside the terrain,
                    // including its odd-pixel center alignment. A center-only
                    // clamp cuts square edges and the corners of wide icons.
                    vec2 alignment = (egHqMarker ? vec2(0.0) : mod(dimensions, 2.0) * 0.5);
                    vec2 desiredCenter = offset + alignment;
                    vec2 halfIcon = dimensions * 0.5 + vec2(1.0 / mapPixel);
                    float iconThreshold = egSettings.x != 0 ? 0.95 : 0.9025;
                    float radius = egHqMarker ? 64.0 + 3.0 / mapPixel : 64.0 * (square != 0 ? iconThreshold : sqrt(iconThreshold));
                    float factor = 1.0;
                    if (square != 0) {
                        vec2 bounds = max(vec2(0.0), vec2(radius) - halfIcon);
                        factor = min(1.0, min(bounds.x / max(abs(desiredCenter.x), 0.000001),
                            bounds.y / max(abs(desiredCenter.y), 0.000001)));
                    } else if (length(abs(desiredCenter) + halfIcon) > radius) {
                        float a = dot(desiredCenter, desiredCenter);
                        float b = dot(abs(desiredCenter), halfIcon);
                        float c = dot(halfIcon, halfIcon) - radius * radius;
                        factor = clamp((-b + sqrt(max(0.0, b * b - a * c))) / max(a, 0.000001), 0.0, 1.0);
                    }
                    offset = desiredCenter * factor - alignment;
                    egSettings.y = 1;
                }
            } else {
                float from = float(egBits(data,0,9));
                float to = float(egBits(data,9,9));
                float age = mod(GameTime*24000.0-float(egBits(data,18,5))+16.0,32.0)-16.0;
                float progress = clamp(age/max(1.0,float(egBits(data,23,4))),0.0,1.0);
                float yaw = from + (mod(to-from+256.0,512.0)-256.0)*progress;
                angle = yaw * 6.28318530718 / 512.0 + 3.14159265359;
            }
            vec2 center = canvasMin + width * 0.5 + offset * mapPixel;
            if (egFamily == 7 || egFamily == 11) center += (egHqMarker ? vec2(0.0) : mod(dimensions, 2.0) * 0.5) * mapPixel;
            egMotion = vec4(center, mapPixel * (egHqMarker ? 0.5 * spriteScale : 1.0), angle);
            vec2 halfSize = egFamily == 8 ? vec2(length(dimensions)*0.5) : dimensions*0.5;
            egCellMin = center - halfSize * mapPixel - 1.0;
            egCellMax = center + halfSize * mapPixel + 1.0;
        } else if (egFamily == 9) {
            vec2 anchor = vec2(egBits(data,0,11),egBits(data,11,11))/8.0;
            bool fixedToMap = egBits(data,30,1) != 0u;
#ifdef ENO_CAMERA_TERRAIN
            offset = fixedToMap?anchor-128.0:mod(anchor-vec2(CameraBlockPos.xz&ivec2(255))+128.0,256.0)-128.0+CameraOffset.xz;
#else
            offset = anchor - 128.0;
#endif
            zoom = fixedToMap ? 1.0 : egZoom(float(egBits(data,22,8)),float((egAlias>>5)&1),float(egAlias&31));
            egCellMin = canvasMin+width*0.5+(offset*zoom+vec2(index&127,index>>7)-64.0)*mapPixel;
            egCellMax = egCellMin+mapPixel;
        }
    }
    float expand = egFamily == 1 || egFamily == 3 || egFamily == 10 || egFamily == 13 || egFamily == 15 ? 0.5 : 0.0;
    vec2 pixelPosition = mix(egCellMin - expand, egCellMax + expand, corner);
    gl_Position = vec4(pixelPosition / ScreenSize * vec2(2.0, -2.0) + vec2(-1.0, 1.0), 0.0, 1.0);
    vertexColor = vec4(1.0);
    texCoord0 = UV0;
    minimap = 0.0;
    fullscreenMinimap = 0.0;
    return;
}
#endif
