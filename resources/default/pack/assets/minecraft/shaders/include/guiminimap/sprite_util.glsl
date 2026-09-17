vec4 egSpriteTexel(ivec2 cell) {
    if (any(lessThan(cell, ivec2(0))) || any(greaterThanEqual(cell, egSprite.zw))) return vec4(0.0);
    vec4 value = texelFetch(Sampler0, egSprite.xy + ivec2(2) + cell, 0);
    if(egSettings.w<4)remapColor(value, float(egSettings.w));
    value.rgb *= value.a;
    return value;
}
vec4 egSpriteColor(vec2 position) {
    vec2 pixel = position - 0.5;
    ivec2 cell = ivec2(floor(pixel));
    vec2 coverage = clamp((fract(pixel) - 0.5) / max(fwidth(pixel), vec2(0.00001)) + 0.5, 0.0, 1.0);
    if(egSettings.w>=4)coverage=fract(pixel);
    vec4 a = egSpriteTexel(cell), b = egSpriteTexel(cell + ivec2(1, 0));
    vec4 c = egSpriteTexel(cell + ivec2(0, 1)), d = egSpriteTexel(cell + ivec2(1, 1));
    vec4 value = mix(mix(a, b, coverage.x), mix(c, d, coverage.x), coverage.y);
    if (value.a > 0.0) value.rgb /= value.a;
    return value;
}
