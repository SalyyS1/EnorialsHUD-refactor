#ifdef IS_GUI
    // A tiny font cell carries pose data, never visible bitmap artwork.
    // The explicit native shadow carries 30 bits; its alias carries 11 more.
    ivec2 gpsSize = textureSize(Sampler0, 0);
    // FontTexture insets the left/top UV by +.01 texel and right/bottom by -.01.
    // GUI batches may rotate their first vertex after a reload or while mining.
    // Read the actual corner instead of assuming gl_VertexID starts at top-left.
    vec2 gpsTexel = UV0 * vec2(gpsSize);
    vec2 gpsCorner = vec2(lessThan(gpsTexel - round(gpsTexel), vec2(0.0)));
    ivec2 gpsOrigin = ivec2(round(UV0 * vec2(gpsSize))) - ivec2(gpsCorner * 2.0);
    if (all(greaterThanEqual(gpsOrigin, ivec2(0))) && all(lessThan(gpsOrigin + 1, gpsSize))) {
        ivec4 gpsTag = ivec4(round(texelFetch(Sampler0, gpsOrigin, 0) * 255.0));
        ivec4 gpsMagic = ivec4(round(texelFetch(Sampler0, gpsOrigin + ivec2(1, 0), 0) * 255.0));
        ivec4 gpsVersion = ivec4(round(texelFetch(Sampler0, gpsOrigin + ivec2(0, 1), 0) * 255.0));
        if (gpsTag.b == 167 && gpsTag.a == 255 && all(equal(gpsMagic, ivec4(71, 80, 83, 255)))
                && all(equal(gpsVersion, ivec4(51, 68, 1, 255)))) {
            uvec4 bytes = uvec4(round(Color * 255.0));
            #ifdef ENO_LEGACY_GPS
            bool gpsHidden = bytes.r < 192u;
            #else
            bool gpsHidden = (bytes.b & 1u) == 0u;
            #endif
            if (gpsHidden) {
                gpsActive = -1;
                gl_Position = vec4(2.0, 2.0, 2.0, 1.0);
            } else {
                #ifdef ENO_LEGACY_GPS
                uint cursorByte=uint(int(round(enoLegacyGpsCursor(Position,UV0,Color)/16384.0))+128)&255u;
                uint lo=(((bytes.r<<16u)|(bytes.g<<8u)|bytes.b)&0x3fffffu)|(cursorByte<<22u);
                #else
                uint lo = ((bytes.a << 24u) | (bytes.r << 16u) | (bytes.g << 8u) | bytes.b) >> 1u;
                lo &= 0x3fffffffu;
                #endif
                uint hi = uint(gpsTag.r * 256 + gpsTag.g);
                gpsAngles = vec4(float(lo & 511u), float((lo >> 9u) & 255u),
                    float((lo >> 17u) & 511u), float(((lo >> 26u) | (hi << 4u)) & 255u));
                gpsClock = float((hi >> 4u) & 63u);
                gpsActive = (hi & 1024u) != 0u ? 1 : 2;
                gpsLocal = gpsCorner;
                // Remove the native font shadow's one-pixel displacement.
                #ifdef ENO_LEGACY_GPS
                gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
                #else
                gl_Position = ProjMat * ModelViewMat * vec4(pos - vec3(1.0, 1.0, 0.0), 1.0);
                #endif
            }
        }
    }
#endif
