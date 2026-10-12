#version 450
#include "common.glsl"
layout(location = 0) in vec2 vUV;
layout(location = 1) in vec3 vCenter;
layout(location = 2) in float vRadius;
layout(location = 3) flat in uint vSeed;
layout(location = 4) flat in vec3 vWorld;
layout(location = 5) flat in vec3 vNormal;
layout(location = 6) flat in float vShade;
layout(location = 0) out vec4 outColor;

#define SAND_TEX_BINDING 2
#include "sand_shade.glsl"

void main() {
    float r2 = dot(vUV, vUV);
    if (r2 > 1.0) {
        discard;
    }
    vec3 pos = vCenter + vec3(vUV, sqrt(1.0 - r2)) * vRadius;
    vec4 clip = G.proj * vec4(pos, 1.0);
    gl_FragDepth = clip.z / clip.w;
    uint s = vSeed;
    vec3 nWorld = normalize(mat3(G.invView) * vNormal);
    vec3 albedo = sandAlbedo(vWorld, nWorld, 0.0) * (0.92 + 0.2 * rand(s));
    outColor = vec4(sandLight(nWorld, albedo, vShade), 1.0);
}
