if (egKind != 0) {
    if (egKind < 0) discard;
    vec2 pixel = vec2(gl_FragCoord.x, ScreenSize.y - gl_FragCoord.y);
    if (egKind == 4) {
        fragColor = vec4(0.0, 0.0, 0.0, 0.5 * egFade);
        return;
    }
    if (egKind == 5) {
        if (egColor.a < 0.1) discard;
        fragColor = egColor;
        return;
    }
    vec2 uvn11 = (pixel - egCanvas.xy) / egCanvas.zw * 2.0 - 1.0;
    bool squareShape = egSettings.x != 0;
    float borderMetric = squareShape ? max(abs(uvn11.x), abs(uvn11.y)) : dot(uvn11, uvn11);
    // Shared terrain boundary; egSettings.y carries a style only on the frame glyph.
    float innerThreshold = squareShape ? 0.95 : 0.9025;
    float markerOuter=1.0+6.0/min(egCanvas.z,egCanvas.w);
    float markerThreshold=squareShape?markerOuter:markerOuter*markerOuter;
    if(egKind==15 && borderMetric>=markerThreshold)discard;
    if (egKind >= 6 && egKind <= 9) {
        float threshold = egSettings.y == 1 ? 0.93 + (squareShape ? 0.02 : 0.075) : innerThreshold;
        if (egKind == 7 || egKind == 8 || egKind == 9) threshold=markerThreshold;
        if (borderMetric >= threshold) discard;
        if (egKind == 6) {
            float coverage = clamp(egMotion.z + 0.5 - length(pixel-egMotion.xy),0.0,1.0);
            if (coverage <= 0.0) discard;
            fragColor = vec4(egColor.rgb,coverage);
        } else if (egKind == 7 || egKind == 8) {
            vec2 local = (pixel-egMotion.xy)/egMotion.z;
            if (egKind == 8) {
                // Original arrow points south at yaw0; rotate the sprite only.
                float c = cos(egMotion.w), s = sin(egMotion.w);
                local = vec2(c*local.x+s*local.y,-s*local.x+c*local.y);
            }
            fragColor = egSpriteColor(local+vec2(egSprite.zw)*0.5);
            if (fragColor.a < 0.1) discard;
        } else fragColor = egColor;
        return;
    }
    if (egKind == 1 && borderMetric >= innerThreshold) discard;
    if (egKind == 2) {
        vec2 imagePoint = (uvn11 / 2.0 + 1.0) * 250.0 - egMotion.xy + 1.0;
        fragColor = egSpriteColor(imagePoint);
        fragColor = egSealFrame(fragColor,uvn11,egSettings.y,squareShape);
        if (fragColor.a < 0.004) discard;
        return;
    }
    // Ordered box coverage. After the last contributing cell the complete
    // terrain is opaque; colors cannot pulse with lighting behind the HUD.
    vec2 pixelMin = pixel - 0.5;
    vec2 pixelMax = pixel + 0.5;
    vec2 overlap = max(vec2(0.0), min(pixelMax, egCellMax) - max(pixelMin, egCellMin));
    float weight = overlap.x * overlap.y;
    if (weight < 0.000001 || egFade <= 0.0) discard;
    float cumulative = clamp(egCellMin.y - pixelMin.y, 0.0, 1.0)
        + overlap.y * clamp(egCellMax.x - pixelMin.x, 0.0, 1.0);
    float alpha = egFade * weight / max(0.000001, 1.0 - egFade + egFade * max(weight, cumulative));
    vec3 color = egColor.rgb;
    if (egKind == 13 && egBundleLength > 1) {
        // Combine all three source-cell coverages before compositing. At tiny
        // viewports one physical pixel may span three different palette cells.
        float unit = egMotion.z;
        vec3 xMin = egCellMin.x + vec3(0.0,1.0,2.0) * unit;
        vec3 cellWeight = max(vec3(0.0), min(vec3(pixelMax.x), xMin + unit) - max(vec3(pixelMin.x), xMin));
        if (egBundleLength < 3) cellWeight.z = 0.0;
        color = (egColor.rgb * cellWeight.x + egColor2.rgb * cellWeight.y + egColor3.rgb * cellWeight.z) / max(0.000001, overlap.x);
    }
    fragColor = vec4(color, clamp(alpha, 0.0, 1.0));
    return;
}
