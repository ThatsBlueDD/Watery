layout(binding = SHADOW_BINDING) uniform sampler2D shadowTex;

float sunLit(vec3 viewPos) {
    if (G.compat.w < 0.5) {
        return 1.0;
    }
    vec3 relative = mat3(G.invView) * viewPos;
    int cascade = 0;
    vec4 clip = G.shadowNear * vec4(relative, 1.0);
    if (max(abs(clip.x), abs(clip.y)) >= 0.94) {
        cascade = 1;
        clip = G.shadowMid * vec4(relative, 1.0);
        if (max(abs(clip.x), abs(clip.y)) >= 0.94) {
            cascade = 2;
            clip = G.shadowFar * vec4(relative, 1.0);
            if (max(abs(clip.x), abs(clip.y)) > 1.0) {
                return 1.0;
            }
        }
    }
    vec2 xy = clip.xy;
    vec2 uv = (xy * 0.5 + 0.5) * 0.5 + vec2(float(cascade % 2), float(cascade / 2)) * 0.5;
    float texel = 1.0 / float(textureSize(shadowTex, 0).x);
    float lit = 0.0;
    for (int k = 0; k < 4; k++) {
        vec2 o = (vec2(float(k & 1), float(k >> 1)) - 0.5) * texel;
        lit += texture(shadowTex, uv + o).r >= clip.z - G.compatB.z ? 1.0 : 0.0;
    }
    return lit * 0.25;
}
