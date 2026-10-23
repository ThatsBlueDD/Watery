layout(std430, binding = REGION_LIGHT_BINDING) readonly buffer RegionLight { uint regionLight[]; };

vec2 regionLightAt(vec3 sim) {
    int res = max(int(G.camGrid.w + 0.5), 1);
    ivec3 n = max(G.dims.xyz / res, ivec3(1));
    vec3 p = sim / float(res) - 0.5;
    ivec3 b = ivec3(floor(p));
    vec3 f = p - vec3(b);
    vec2 sum = vec2(0.0);
    float weight = 0.0;
    vec2 brightest = vec2(0.0);
    for (int k = 0; k < 8; k++) {
        ivec3 o = ivec3(k & 1, (k >> 1) & 1, k >> 2);
        ivec3 c = clamp(b + o, ivec3(0), n - 1);
        uint v = regionLight[c.x + n.x * (c.y + n.y * c.z)];
        vec2 level = vec2(float(v & 15u), float((v >> 4) & 15u)) / 15.0;
        brightest = max(brightest, level);
        if ((v & 0x100u) != 0u) {
            continue;
        }
        vec3 w3 = mix(1.0 - f, f, vec3(o));
        float w = w3.x * w3.y * w3.z;
        sum += level * w;
        weight += w;
    }
    return weight > 1e-4 ? sum / weight : brightest;
}

vec3 relToSim(vec3 rel) {
    return (rel - G.domainOrigin.xyz) / G.domainOrigin.w;
}
