// Reconstruct the center of the real item-frame quad from its derivatives.
// Hanging entities snap their passenger position to block centers. In first
// person, use the render camera directly; adding that snapped carrier offset
// would reintroduce one-block steps. Retain the carrier for distant cameras.
vec2 sampleCoord = texCoord1;
if (minimap == 1.0 || minimap == 2.0) {
    int motionKind = decodeUnsigned(105, 0);
    if (motionKind >= 10 && motionKind <= 14) {
        vec2 tx = dFdx(texCoord0), ty = dFdy(texCoord0);
        vec3 px = dFdx(minimapCarrierPosition), py = dFdy(minimapCarrierPosition);
        float determinant = tx.x * ty.y - tx.y * ty.x;
        vec3 du = (px * ty.y - py * tx.y) / determinant;
        vec3 dv = (py * tx.x - px * ty.x) / determinant;
        vec3 carrier = minimapCarrierPosition - du * (texCoord0.x - 0.5) - dv * (texCoord0.y - 0.5);
        // Vanilla GlobalSettingsUniform stores floor(camera) - camera (negative
        // fractional coordinates), not camera - floor(camera).
        vec2 cameraToHolder = length(carrier.xz) < 1.5 ? vec2(0.0) : carrier.xz;
        vec2 relativePlayer = cameraToHolder - CameraOffset.xz;
        if (motionKind == 10) {
            ivec2 origin = ivec2(decodeUnsigned(89, 0), decodeUnsigned(97, 0));
            vec2 blockDelta = mod(vec2((CameraBlockPos.xz & 255) - origin) + 128.0, 256.0) - 128.0;
            vec2 renderedFraction = blockDelta + relativePlayer;
            vec2 sentFraction = vec2(decodeFixedPoint(1, 0), decodeFixedPoint(9, 0));
            sampleCoord += (sentFraction - renderedFraction) / 128.0;
        } else {
            int markerX = decodeUnsigned(1, 3) | (decodeUnsigned(9, 3) << 8) | (decodeUnsigned(17, 3) << 16) | (decodeUnsigned(25, 3) << 24);
            int markerZ = decodeUnsigned(33, 3) | (decodeUnsigned(41, 3) << 8) | (decodeUnsigned(49, 3) << 16) | (decodeUnsigned(57, 3) << 24);
            vec2 offset = (motionKind == 11 || motionKind == 14)
                ? vec2(ivec2(markerX, markerZ) - CameraBlockPos.xz) - relativePlayer
                : vec2(markerX, markerZ) - 64.0;
            if (motionKind == 14) {
                // Entity radar: preserve sub-block positions, including west /
                // north of zero, then interpolate the last two server samples.
                // GameTime is (level gameTime % 24000 + partialTick) / 24000;
                // changing the daylight clock does not change this time base.
                int fx = decodeUnsigned(1, 4) | (decodeUnsigned(9, 4) << 8);
                int fz = decodeUnsigned(17, 4) | (decodeUnsigned(25, 4) << 8);
                int dx = decodeUnsigned(33, 4) | (decodeUnsigned(41, 4) << 8);
                int dz = decodeUnsigned(49, 4) | (decodeUnsigned(57, 4) << 8);
                dx = dx >= 32768 ? dx - 65536 : dx;
                dz = dz >= 32768 ? dz - 65536 : dz;
                int sampleTick = decodeUnsigned(65, 4) | (decodeUnsigned(73, 4) << 8);
                float interval = max(1.0, float(decodeUnsigned(81, 4)));
                float age = mod(GameTime * 24000.0 - float(sampleTick) + 12000.0, 24000.0) - 12000.0;
                float progress = clamp(age / interval, 0.0, 1.0);
                offset += vec2(fx, fz) / 65536.0 - vec2(dx, dz) / 256.0 * (1.0 - progress);
            }
            // World marker centers follow the same zoom as terrain. Keep icon
            // dimensions in screen pixels and clamp pinned icons afterwards.
            if (motionKind == 11 || motionKind == 14) offset *= enorialsMinimapZoom();
            if (keepEdges == 1.0 && motionKind == 11) {
                vec2 halfIcon = floor(vec2(decodeUnsigned(65, 3), decodeUnsigned(73, 3)) / 2.0);
                if (minimapShape >= 0.5) {
                    vec2 bounds = max(vec2(1.0), 63.0 - halfIcon);
                    offset *= min(1.0, min(bounds.x / max(abs(offset.x), 0.000001), bounds.y / max(abs(offset.y), 0.000001)));
                } else {
                    float radius = max(1.0, 60.0 - max(halfIcon.x, halfIcon.y));
                    offset *= min(1.0, radius / max(length(offset), 0.000001));
                }
            }
            sampleCoord = vec2(texCoord2.x, 1.0 - texCoord2.y) - offset / 128.0;
            if (motionKind == 13) {
                // The map stays north-up. Rotate only the player arrow from
                // the render camera, without rasterizing its rotation at 20 Hz.
                vec2 right = normalize((transpose(mat3(ModelViewMat)) * vec3(1.0, 0.0, 0.0)).xz);
                float sentYaw = float(decodeUnsigned(81, 3)) * 6.28318530718 / 255.0;
                if (length(carrier.xz) >= 1.5 && dot(right, -vec2(cos(sentYaw), sin(sentYaw))) < 0.0) right = -right;
                vec2 q = sampleCoord - 0.5;
                vec2 oddCenter = mod(vec2(decodeUnsigned(65, 3), decodeUnsigned(73, 3)), 2.0) * 0.5 / 128.0;
                sampleCoord = vec2(0.5) + oddCenter + vec2(dot(q, right), dot(q, vec2(-right.y, right.x)));
            }
            if (sampleCoord.y < (motionKind == 14 ? 5.0 : 4.0) / 128.0) discard;
        }
    }
}

#define BORDER_COLOR1 vec4(vec3(40. / 255.), 1.)
#define BORDER_COLOR2 vec4(vec3(70. / 255.), 1.)

if (minimap == 3.0) {
    vec4 color = filteredMinimapColor(sampleCoord, colorProfile);
    fragColor = color * vertexColor * ColorModulator;
    if (fract(sampleCoord.y) < 3. / 128.) {
        discard;
    }
    if (fract(sampleCoord.x) <= 1. / 128. || fract(sampleCoord.x) >= 127. / 128.) {
        discard;
    }
    if (minimap == 2.0) {
        if (decodeUnsigned(105, 0) == 14) {
            // Radar icons are centered on bitmap texel 64. A screen-resolution
            // circle keeps them filled and round even while moving subpixel.
            vec2 dotPixel = sampleCoord * 128.0 - vec2(64.5);
            if (dot(dotPixel, dotPixel) > 1.0) discard;
        }

        if (fragColor.a < 0.5) discard;
        fragColor.a = 1.0;
    }
    if (fragColor.a < 0.1) {
        discard;
    }
    return;
}

if (minimap == 1.0 || minimap == 2.0) {
    bool squareShape = minimapShape >= 0.5;
    vec2 uvn11 = texCoord2 * 2.0 - 1.0;
    float dist = dot(uvn11, uvn11);
    float edge = max(abs(uvn11.x), abs(uvn11.y));
    int style = int(mod(frameStyle + 0.5, 6.0));
    int thickness = int(mod(frameThickness + 0.5, 3.0));
    float borderMetric = squareShape ? edge : dist;

    float outerThreshold = 0.93;
    float borderWidth = 0.048;
    if (thickness == 0) {
        borderWidth = 0.032;
    } else if (thickness == 2) {
        borderWidth = 0.068;
    }
    float innerThreshold = outerThreshold - borderWidth;
    bool inside = borderMetric < innerThreshold;
    // Keep-edge layers should only extend a tiny bit beyond the map shape.
    // Prevents stray dots/artefacts outside the circular minimap.
    float edgeKeepThreshold = outerThreshold + (squareShape ? 0.02 : 0.075);
    bool edgeKeepVisible = keepEdges == 1.0 && borderMetric < edgeKeepThreshold;
    bool border = borderMetric < outerThreshold;
    if (inside || edgeKeepVisible) {
        bool invalidSample = sampleCoord.x < 0.0 || sampleCoord.y < 0.0 || sampleCoord.x >= 1.0 || sampleCoord.y >= 1.0 ||
            (sampleCoord.x < 17. / 128. && sampleCoord.y < 3. / 128.) ||
            sampleCoord.x <= 1. / 128. || sampleCoord.x >= 127. / 128. || sampleCoord.y >= 127. / 128.;
        if (invalidSample) {
            discard;
        }
        vec4 color = filteredMinimapColor(sampleCoord, colorProfile);
        fragColor = color * vertexColor * ColorModulator;
    } else if (border && minimap == 1.0) {
        vec4 borderBase = vec4(17. / 255., 17. / 255., 17. / 255., 1.0);
        vec4 borderBandA = vec4(40. / 255., 40. / 255., 40. / 255., 1.0);
        vec4 borderBandB = vec4(70. / 255., 70. / 255., 70. / 255., 1.0);
        float ring = clamp((borderMetric - innerThreshold) / max(0.0001, borderWidth), 0.0, 1.0);
        float angle = atan(uvn11.y, uvn11.x);
        const float PI = 3.14159265359;
        const float TAU = 6.28318530718;

        if (style == 1) {
            borderBase = vec4(10. / 255., 24. / 255., 40. / 255., 1.0);
            borderBandA = vec4(35. / 255., 111. / 255., 168. / 255., 1.0);
            borderBandB = vec4(128. / 255., 218. / 255., 250. / 255., 1.0);
            float wave = 0.5 + 0.5 * sin(angle * 8.0 + ring * 26.0);
            fragColor = mix(borderBase, borderBandA, ring);
            fragColor = mix(fragColor, borderBandB, wave * (0.35 + 0.35 * ring));
        } else if (style == 2) {
            borderBase = vec4(28. / 255., 20. / 255., 10. / 255., 1.0);
            borderBandA = vec4(145. / 255., 104. / 255., 27. / 255., 1.0);
            borderBandB = vec4(242. / 255., 206. / 255., 95. / 255., 1.0);
            fragColor = mix(borderBase, borderBandA, ring);
            float cornerAccent = squareShape ? smoothstep(0.55, 0.82, abs(uvn11.x) * abs(uvn11.y)) : 0.0;
            float spoke = abs(fract((angle + PI) / TAU * 12.0) - 0.5);
            float radialAccent = squareShape ? 0.0 : 1.0 - smoothstep(0.38, 0.50, spoke);
            float accent = max(cornerAccent, radialAccent * (1.0 - ring));
            fragColor = mix(fragColor, borderBandB, clamp(accent, 0.0, 1.0));
            if (ring > 0.78) {
                fragColor = mix(fragColor, borderBase, 0.45);
            }
        } else if (style == 3) {
            borderBase = vec4(8. / 255., 26. / 255., 17. / 255., 1.0);
            borderBandA = vec4(28. / 255., 134. / 255., 82. / 255., 1.0);
            borderBandB = vec4(165. / 255., 248. / 255., 178. / 255., 1.0);
            fragColor = mix(borderBase, borderBandA, ring);

            float sectors = 16.0;
            float sector = fract((angle + PI) / TAU * sectors);
            float facetEdge = min(sector, 1.0 - sector);
            float facet = 1.0 - smoothstep(0.08, 0.28, facetEdge);
            float radialFacet = 1.0 - smoothstep(0.30, 0.95, abs(uvn11.x * uvn11.y));
            float crystalMix = clamp(0.55 * facet + 0.65 * radialFacet, 0.0, 1.0);
            fragColor = mix(fragColor, borderBandB, crystalMix * (0.35 + 0.50 * (1.0 - ring)));

            float glintDiagA = 1.0 - smoothstep(0.05, 0.16, abs(uvn11.x - uvn11.y));
            float glintDiagB = 1.0 - smoothstep(0.05, 0.16, abs(uvn11.x + uvn11.y));
            float glint = max(glintDiagA, glintDiagB) * (1.0 - ring);
            fragColor = mix(fragColor, vec4(205. / 255., 1.0, 214. / 255., 1.0), glint * 0.35);

            if (squareShape) {
                float corner = smoothstep(0.55, 0.90, abs(uvn11.x) * abs(uvn11.y));
                fragColor = mix(fragColor, vec4(12. / 255., 56. / 255., 34. / 255., 1.0), corner * 0.55);
            }
            if (ring > 0.82) {
                fragColor = mix(fragColor, vec4(6. / 255., 44. / 255., 25. / 255., 1.0), 0.65);
            }
        } else if (style == 4) {
            borderBase = vec4(30. / 255., 6. / 255., 10. / 255., 1.0);
            borderBandA = vec4(156. / 255., 28. / 255., 48. / 255., 1.0);
            borderBandB = vec4(245. / 255., 96. / 255., 122. / 255., 1.0);
            fragColor = mix(borderBase, borderBandA, ring);

            float sectors = 16.0;
            float sector = fract((angle + PI) / TAU * sectors);
            float facetEdge = min(sector, 1.0 - sector);
            float facet = 1.0 - smoothstep(0.08, 0.28, facetEdge);
            float radialFacet = 1.0 - smoothstep(0.30, 0.95, abs(uvn11.x * uvn11.y));
            float crystalMix = clamp(0.55 * facet + 0.65 * radialFacet, 0.0, 1.0);
            fragColor = mix(fragColor, borderBandB, crystalMix * (0.35 + 0.50 * (1.0 - ring)));

            float glintDiagA = 1.0 - smoothstep(0.05, 0.16, abs(uvn11.x - uvn11.y));
            float glintDiagB = 1.0 - smoothstep(0.05, 0.16, abs(uvn11.x + uvn11.y));
            float glint = max(glintDiagA, glintDiagB) * (1.0 - ring);
            fragColor = mix(fragColor, vec4(1.0, 182. / 255., 196. / 255., 1.0), glint * 0.35);

            if (squareShape) {
                float corner = smoothstep(0.55, 0.90, abs(uvn11.x) * abs(uvn11.y));
                fragColor = mix(fragColor, vec4(74. / 255., 8. / 255., 18. / 255., 1.0), corner * 0.55);
            }
            if (ring > 0.82) {
                fragColor = mix(fragColor, vec4(54. / 255., 6. / 255., 14. / 255., 1.0), 0.65);
            }
        } else if (style == 5) {
            borderBase = vec4(10. / 255., 10. / 255., 14. / 255., 1.0);
            borderBandA = vec4(46. / 255., 46. / 255., 58. / 255., 1.0);
            borderBandB = vec4(92. / 255., 92. / 255., 118. / 255., 1.0);
            fragColor = mix(borderBase, borderBandA, ring);
            float techLine = abs(fract((angle + PI) / TAU * 18.0) - 0.5);
            float pulse = 1.0 - smoothstep(0.42, 0.50, techLine);
            fragColor = mix(fragColor, borderBandB, pulse * (0.3 + 0.4 * (1.0 - ring)));
            if (squareShape) {
                float corner = smoothstep(0.60, 0.90, abs(uvn11.x) * abs(uvn11.y));
                fragColor = mix(fragColor, vec4(6. / 255., 6. / 255., 10. / 255., 1.0), corner * 0.5);
            }
        } else {
            fragColor = borderBase;
            if (ring > 0.35) {
                fragColor = borderBandA;
            }
            if (ring > 0.72) {
                fragColor = borderBandB;
            }
        }
        fragColor.a = 1.0;
    } else {
        discard;
    }
    if (minimap == 1.0) {
        fragColor.a = 1.0;
    } else {
        // Marker layers can have rotated/offset UVs. Do not use fract() here, it wraps negative UVs
        // and causes metadata bits to appear as moving gray dots near the right border.
        if (sampleCoord.x < 0.0 || sampleCoord.y < 0.0 || sampleCoord.x >= 1.0 || sampleCoord.y >= 1.0 ||
            (sampleCoord.x < 17. / 128. && sampleCoord.y < 3. / 128.) ||
            sampleCoord.x <= 1. / 128. || sampleCoord.x >= 127. / 128. || sampleCoord.y >= 127. / 128.) {
            discard;
        }
    }
    if (minimap == 2.0) {
        if (decodeUnsigned(105, 0) == 14) {
            // Radar icons are centered on bitmap texel 64. A screen-resolution
            // circle keeps them filled and round even while moving subpixel.
            vec2 dotPixel = sampleCoord * 128.0 - vec2(64.5);
            if (dot(dotPixel, dotPixel) > 1.0) discard;
        }

        if (fragColor.a < 0.5) discard;
        fragColor.a = 1.0;
    }
    if (fragColor.a < 0.1) {
        discard;
    }
    return;
} else if (minimap > 0.0) {
    discard;
}

// Fullscreen tile edges use the established three-pixel dark/light/dark bands.
if(fullscreenMinimap>0.0) {
    if(fullscreenMinimap==2.0)fragColor=vec4(0.0,0.0,0.0,transition*0.5);
    else {
        ivec2 pixel=ivec2(texCoord0*128.0);
        int vertical=sx==0.0?127-pixel.y:sx==1.0?pixel.y:128;
        int horizontal=sy==0.0?pixel.x:sy==1.0?127-pixel.x:128;
        int edgeDistance=min(vertical,horizontal);
        vec2 sampleUV=vec2(texCoord0.x,texCoord0.y*(127.0/128.0)+1.0/128.0);
        vec4 tileColor=texture(Sampler0,sampleUV);remapColor(tileColor,colorProfile);
        if(edgeDistance==1)tileColor=vec4(vec3(70.0/255.0),1.0);
        else if(edgeDistance==0||edgeDistance==2)tileColor=vec4(vec3(40.0/255.0),1.0);
        fragColor=tileColor*vertexColor*ColorModulator;fragColor.a*=transition;
    }
    return;
}
