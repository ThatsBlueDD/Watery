#define MPM_MU G.matB.x
#define MPM_LAMBDA G.matB.y
#define MPM_ALPHA G.matA.y
#define MPM_COHESION G.matA.z
#define MPM_WALL_FRICTION G.matC.x
#define MPM_VOLUME G.matC.y
#define MPM_IMPACT_VISCOSITY G.matC.z
#define MPM_REST_SPEED G.matE.x
#define MPM_REST_TIME G.matE.y
#define MPM_WAKE_SPEED G.matE.z
#define MPM_IMPACT_RATE G.matC.w
#define MPM_IMPACT_LOG_J G.matE.w

const float MASS_FP = 1048576.0;
const float MOM_FP = 131072.0;

struct Stencil {
    ivec3 base;
    vec3 fx;
    vec3 w[3];
};

Stencil stencil(vec3 x) {
    Stencil s;
    s.base = ivec3(floor(x - 1.0));
    s.fx = x - 0.5 - vec3(s.base);
    s.w[0] = 0.5 * (1.5 - s.fx) * (1.5 - s.fx);
    s.w[1] = 0.75 - (s.fx - 1.0) * (s.fx - 1.0);
    s.w[2] = 0.5 * (s.fx - 0.5) * (s.fx - 0.5);
    return s;
}

#define STATE_STRIDE 28
#define STATE_TAU 18
#define STATE_REST 24
#define LOAD_MAT(buf, o) mat3(buf[o], buf[o + 1], buf[o + 2], buf[o + 3], buf[o + 4], buf[o + 5], buf[o + 6], buf[o + 7], buf[o + 8])

void jacobi(inout mat3 A, inout mat3 V, int p, int q) {
    float apq = A[q][p];
    if (abs(apq) < 1e-12) {
        return;
    }
    float theta = (A[q][q] - A[p][p]) / (2.0 * apq);
    float t = theta == 0.0 ? 1.0 : sign(theta) / (abs(theta) + sqrt(theta * theta + 1.0));
    float c = inversesqrt(t * t + 1.0);
    float s = t * c;
    mat3 J = mat3(1.0);
    J[p][p] = c;
    J[q][q] = c;
    J[q][p] = s;
    J[p][q] = -s;
    A = transpose(J) * A * J;
    V = V * J;
}

void svd3(mat3 F, out mat3 U, out vec3 S, out mat3 V) {
    mat3 A = transpose(F) * F;
    V = mat3(1.0);
    for (int sweep = 0; sweep < 5; sweep++) {
        jacobi(A, V, 0, 1);
        jacobi(A, V, 0, 2);
        jacobi(A, V, 1, 2);
    }
    S = sqrt(max(vec3(A[0][0], A[1][1], A[2][2]), vec3(0.0)));
    if (determinant(V) < 0.0) {
        V[0] = -V[0];
    }
    U = F * V;
    for (int i = 0; i < 3; i++) {
        U[i] /= max(S[i], 1e-8);
    }
    if (determinant(F) < 0.0) {
        int m = S.x < S.y ? (S.x < S.z ? 0 : 2) : (S.y < S.z ? 1 : 2);
        S[m] = -S[m];
        U[m] = -U[m];
    }
}

mat3 diag3(vec3 d) {
    return mat3(d.x, 0.0, 0.0, 0.0, d.y, 0.0, 0.0, 0.0, d.z);
}

mat3 projectSand(mat3 F, float squeeze, out mat3 tau) {
    mat3 U;
    vec3 S;
    mat3 V;
    svd3(F, U, S, V);
    vec3 e = log(max(S, vec3(1e-6)));
    float logJ = e.x + e.y + e.z;
    if (squeeze < -MPM_IMPACT_RATE && logJ < MPM_IMPACT_LOG_J) {
        e += (MPM_IMPACT_LOG_J - logJ) / 3.0;
        S = exp(e);
    }
    float tr = e.x + e.y + e.z - MPM_COHESION;
    if (tr >= 0.0) {
        S = vec3(exp(MPM_COHESION / 3.0));
    } else {
        vec3 eh = e - (tr + MPM_COHESION) / 3.0;
        float en = length(eh);
        float dg = en + (3.0 * MPM_LAMBDA + 2.0 * MPM_MU) / (2.0 * MPM_MU) * tr * MPM_ALPHA;
        if (dg > 0.0 && en > 1e-12) {
            S = exp(e - dg * eh / en);
        }
    }
    vec3 l = log(max(S, vec3(1e-6)));
    tau = U * diag3(2.0 * MPM_MU * l + MPM_LAMBDA * (l.x + l.y + l.z)) * transpose(U);
    return U * diag3(S) * transpose(V);
}
