layout(binding = LIGHTMAP_BINDING) uniform sampler2D lightmapTex;
layout(binding = COLOR_LIGHT_BINDING) uniform sampler2D skLightA;
layout(binding = COLOR_LIGHT_BINDING + 1) uniform sampler2D skLightB;
layout(binding = COLOR_LIGHT_BINDING + 2) uniform sampler2D skLightTint;

vec3 vanillaLight(vec2 bs) {
    return texture(lightmapTex, (clamp(bs, 0.0, 1.0) * 15.0 + 0.5) / 16.0).rgb;
}

float blockCurve(float level) {
    return pow(level, 3.0) * 0.9 + pow(level, 8.0) * 0.6;
}

vec3 shaderSkyAmbient(vec3 nWorld) {
    vec3 sky = mix(G.fogColor.rgb * 0.75, G.skyColor.rgb * 1.1, nWorld.y * 0.5 + 0.5);
    float luma = dot(sky, vec3(0.2126, 0.7152, 0.0722));
    return mix(vec3(luma), sky, 0.45);
}

bool inColorVolume(vec3 rel) {
    int range = int(G.lightA.w);
    if (G.lightC.z < 0.5 || range <= 0) {
        return false;
    }
    vec3 local = rel - G.lightA.xyz;
    vec3 size = vec3(float(range), float(range / 2), float(range));
    return all(greaterThanEqual(local, vec3(0.5))) && all(lessThanEqual(local, size - 0.5));
}

vec2 colorLightUv(int slice, vec2 xz) {
    int range = int(G.lightA.w);
    int columns = max(int(G.lightB.w), 1);
    vec2 local = mod(xz - 0.5, float(range));
    vec2 corner = vec2(float(slice % columns), float(slice / columns)) * float(range + 2);
    return (corner + 1.5 + local) / G.lightC.xy;
}

vec3 colorLightAt(vec3 rel, out float typed) {
    typed = 0.0;
    int range = int(G.lightA.w);
    int height = range / 2;
    vec3 world = rel + G.lightB.xyz;
    float y = mod(world.y - 0.5, float(height));
    int y0 = int(floor(y));
    int y1 = (y0 + 1) % height;
    float fy = y - floor(y);
    vec2 uv0 = colorLightUv(y0, world.xz);
    vec2 uv1 = colorLightUv(y1, world.xz);
    vec4 t = mix(textureLod(skLightTint, uv0, 0.0), textureLod(skLightTint, uv1, 0.0), fy);
    if (t.a < 0.001) {
        return vec3(0.0);
    }
    vec3 local = rel - G.lightA.xyz;
    vec3 size = vec3(float(range), float(height), float(range));
    vec3 edge = min(local, size - local);
    float scale = clamp(min(edge.x, min(edge.y, edge.z)) / 6.0 - 0.1, 0.0, 1.0) / t.a;
    vec4 a = mix(textureLod(skLightA, uv0, 0.0), textureLod(skLightA, uv1, 0.0), fy) * scale;
    vec4 b = mix(textureLod(skLightB, uv0, 0.0), textureLod(skLightB, uv1, 0.0), fy) * scale;
    vec4 m = max(a, b);
    typed = max(max(m.x, m.y), max(m.z, m.w));
    vec3 light = a.x * G.lightColors[0].rgb + a.y * G.lightColors[1].rgb + a.z * G.lightColors[2].rgb + a.w * G.lightColors[3].rgb
        + b.x * G.lightColors[4].rgb + b.y * G.lightColors[5].rgb + b.z * G.lightColors[6].rgb + b.w * G.lightColors[7].rgb;
    return light * t.rgb / t.a;
}

vec3 shaderBlockLight(vec3 rel, vec3 nWorld, float level) {
    vec3 torch = vec3(1.0, 0.6, 0.3) * 1.4;
    vec3 vanilla = torch * blockCurve(level);
    vec3 at = rel + nWorld * 0.5;
    if (!inColorVolume(at)) {
        return vanilla;
    }
    float typed;
    vec3 colored = colorLightAt(at, typed);
    vec3 shaped = typed > 0.0001 ? colored / typed * blockCurve(min(typed, 1.0)) * 1.4 : vec3(0.0);
    vec3 rest = torch * max(blockCurve(level) - blockCurve(min(typed + 1.0 / 15.0, 1.0)), 0.0);
    return shaped + rest;
}

vec3 shaderSurfaceLight(vec3 rel, vec3 viewPos, vec3 nWorld, vec2 bs) {
    vec3 toSun = normalize(mat3(G.invView) * G.sunDir.xyz);
    float day = clamp(G.skyColor.w / 0.85, 0.2, 1.0);
    float direct = max(dot(nWorld, toSun), 0.0) * G.sunDir.w * sunLit(viewPos) * smoothstep(0.5, 0.95, bs.y);
    vec3 sunColor = mix(vec3(1.0, 0.46, 0.18), vec3(1.0, 0.95, 0.88), smoothstep(0.0, 0.42, max(toSun.y, 0.0)));
    return G.compatB.x * direct * sunColor + G.compatB.y * day * shaderSkyAmbient(nWorld) * bs.y * bs.y
        + shaderBlockLight(rel, nWorld, bs.x) + vec3(0.010, 0.012, 0.018);
}
