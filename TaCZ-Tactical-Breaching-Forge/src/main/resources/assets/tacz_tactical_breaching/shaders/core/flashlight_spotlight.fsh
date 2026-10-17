#version 150

uniform sampler2D SceneColor;
uniform sampler2D SceneDepth;
uniform mat4 InverseViewProjection;
uniform mat4 ViewProjection;
uniform vec2 ScreenSize;
uniform vec3 LightOrigin;
uniform vec3 LightDirection;
uniform float LightRange;
uniform float LightIntensity;
uniform float HotspotStrength;
uniform float SpillStrength;
uniform float BeamSoftness;
uniform int Quality;
uniform vec3 LightColor;

out vec4 fragColor;

void main() {
    vec2 uv = gl_FragCoord.xy / ScreenSize;
    vec4 scene = texture(SceneColor, uv);
    float depth = texture(SceneDepth, uv).r;
    if (depth >= 0.9999) {
        fragColor = scene;
        return;
    }
    vec4 projected = InverseViewProjection * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec3 surface = projected.xyz / projected.w;
    vec3 offset = surface - LightOrigin;
    float along = dot(offset, LightDirection);
    if (along <= 0.05 || along >= LightRange) {
        fragColor = scene;
        return;
    }

    float radial = length(offset - LightDirection * along);
    float spillRadius = (1.85 + along * 0.55) * BeamSoftness;
    float coreRadius = (0.30 + along * 0.09) * BeamSoftness;
    float spill = 1.0 - smoothstep(spillRadius * 0.54, spillRadius, radial);
    float core = 1.0 - smoothstep(coreRadius * 0.25, coreRadius, radial);
    float distanceFade = 1.0 / (1.0 + along * 0.025 + along * along * 0.0018);
    distanceFade *= 1.0 - smoothstep(LightRange * 0.88, LightRange, along);
    float visibility = 1.0;
    int checks = max(Quality - 1, 0) * 2;
    for (int i = 1; i <= 6; i++) {
        if (i > checks) {
            break;
        }
        vec3 probe = mix(LightOrigin, surface, float(i) / float(checks + 1));
        vec4 probeClip = ViewProjection * vec4(probe, 1.0);
        vec2 probeUv = probeClip.xy / probeClip.w * 0.5 + 0.5;
        if (probeClip.w > 0.0 && all(greaterThan(probeUv, vec2(0.0)))
                && all(lessThan(probeUv, vec2(1.0)))) {
            float probeDepth = probeClip.z / probeClip.w * 0.5 + 0.5;
            if (texture(SceneDepth, probeUv).r + 0.01 < probeDepth) {
                visibility *= 0.65;
            }
        }
    }
    vec3 dx = dFdx(surface);
    vec3 dy = dFdy(surface);
    vec3 normal = normalize(cross(dx, dy));
    if (dot(normal, -surface) < 0.0) {
        normal = -normal;
    }
    float surfaceFacing = clamp(dot(normal, normalize(LightOrigin - surface)), 0.0, 1.0);
    float diffuse = 0.35 + 0.65 * surfaceFacing;
    float light = (SpillStrength * 8.0 * spill + HotspotStrength * 4.5 * core)
            * LightIntensity * distanceFade * visibility * diffuse;

    // Estimate surface reflectance from the dark scene while retaining visible texture contrast.
    vec3 reflectance = clamp(scene.rgb * 4.2 + vec3(0.24), vec3(0.0), vec3(1.0));
    vec3 lifted = scene.rgb + reflectance * LightColor * light;
    fragColor = vec4(min(lifted, vec3(1.0)), scene.a);
}
