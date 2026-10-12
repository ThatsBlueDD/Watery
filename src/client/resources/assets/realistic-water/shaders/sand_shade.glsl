layout(std430, binding = SAND_TEX_BINDING) readonly buffer SandTex { int sandTex[]; };

vec3 unpackRgb(int argb) {
    return vec3((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF) / 255.0;
}

vec3 sandAverage() {
    return unpackRgb(sandTex[2]);
}

vec3 sandTexel(ivec2 t) {
    ivec2 size = ivec2(sandTex[0], sandTex[1]);
    t = ((t % size) + size) % size;
    return unpackRgb(sandTex[4 + t.x + t.y * size.x]);
}

vec3 planarTexel(vec2 uv, float texelsPerBlock) {
    return sandTexel(ivec2(floor(uv * texelsPerBlock)));
}

vec3 sandAlbedo(vec3 world, vec3 nWorld, float footprint) {
    float tpb = float(sandTex[0]);
    vec3 w = pow(abs(nWorld), vec3(8.0));
    w /= w.x + w.y + w.z;
    vec3 c = w.y * planarTexel(vec2(world.x, world.z), tpb)
           + w.x * planarTexel(vec2(world.z, -world.y), tpb)
           + w.z * planarTexel(vec2(world.x, -world.y), tpb);
    float texel = 1.0 / tpb;
    float detail = 1.0 - smoothstep(0.5, 1.5, footprint / texel);
    return mix(sandAverage(), c, detail);
}

vec3 sandLight(vec3 nWorld, vec3 albedo, float ao) {
    vec3 n2 = nWorld * nWorld;
    float face = n2.x * 0.6 + n2.z * 0.8 + n2.y * (nWorld.y > 0.0 ? 1.0 : 0.5);
    float day = clamp(G.skyColor.w / 0.85, 0.2, 1.0);
    return min(albedo * face * day * ao * G.matD.y, vec3(1.0));
}
