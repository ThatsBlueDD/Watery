const ivec3 NB[6] = ivec3[](ivec3(1, 0, 0), ivec3(-1, 0, 0), ivec3(0, 1, 0), ivec3(0, -1, 0), ivec3(0, 0, 1), ivec3(0, 0, -1));

ivec4 levelDims(int l) {
    ivec3 d = G.dims.xyz;
    int offset = 0;
    for (int k = 0; k < l; k++) {
        offset += d.x * d.y * d.z;
        d = (d + 1) / 2;
    }
    return ivec4(d, offset);
}

int idx(ivec3 c, ivec4 d) {
    return d.w + c.x + d.x * (c.y + d.y * c.z);
}

ivec3 boxLo(int l) {
    return G.boxMin.xyz >> l;
}

ivec3 boxHi(int l, ivec4 d) {
    return min((G.boxMax.xyz + ((1 << l) - 1)) >> l, d.xyz);
}

bool inLevel(ivec3 c, ivec4 d, int l) {
    return all(greaterThanEqual(c, boxLo(l))) && all(lessThan(c, boxHi(l, d)));
}

int boxCount(ivec4 d, int l) {
    ivec3 s = max(boxHi(l, d) - boxLo(l), ivec3(0));
    return s.x * s.y * s.z;
}

ivec3 boxCellOf(int t, ivec4 d, int l) {
    ivec3 s = boxHi(l, d) - boxLo(l);
    return boxLo(l) + ivec3(t % s.x, (t / s.x) % s.y, t / (s.x * s.y));
}

float bAt(int l, int i) {
    return l == 0 ? b0[i] : mgB[i];
}

float xAt(int l, int i) {
    return l == 0 ? x0[i] : mgX[i];
}

void setX(int l, int i, float v) {
    if (l == 0) {
        x0[i] = v;
    } else {
        mgX[i] = v;
    }
}

void sync() {
    memoryBarrierBuffer();
    barrier();
}

float applyA(ivec3 c, int i, ivec4 d, int l) {
    float s = 0.0;
    float n = 0.0;
    for (int k = 0; k < 6; k++) {
        ivec3 nb = c + NB[k];
        if (!inLevel(nb, d, l)) {
            n += 1.0;
            continue;
        }
        int j = idx(nb, d);
        uint t = mgFlags[j];
        if (t == CELL_SOLID) {
            continue;
        }
        n += 1.0;
        if (t == CELL_FLUID) {
            s += xAt(l, j);
        }
    }
    return n * xAt(l, i) - s;
}

void relax(int l, int parity, bool init) {
    ivec4 d = levelDims(l);
    int count = boxCount(d, l);
    for (int t = int(gl_LocalInvocationID.x); t < count; t += 1024) {
        ivec3 c = boxCellOf(t, d, l);
        int i = idx(c, d);
        if (mgFlags[i] != CELL_FLUID) {
            if (init) {
                setX(l, i, 0.0);
            }
            continue;
        }
        if (((c.x + c.y + c.z) & 1) != parity) {
            continue;
        }
        float s = 0.0;
        float n = 0.0;
        for (int k = 0; k < 6; k++) {
            ivec3 nb = c + NB[k];
            if (!inLevel(nb, d, l)) {
                n += 1.0;
                continue;
            }
            int j = idx(nb, d);
            uint ty = mgFlags[j];
            if (ty == CELL_SOLID) {
                continue;
            }
            n += 1.0;
            if (ty == CELL_FLUID && !init) {
                s += xAt(l, j);
            }
        }
        if (n > 0.0) {
            setX(l, i, (bAt(l, i) + s) / n);
        }
    }
    sync();
}

void restrictTo(int l) {
    ivec4 d = levelDims(l);
    ivec4 cd = levelDims(l + 1);
    int count = boxCount(cd, l + 1);
    for (int t = int(gl_LocalInvocationID.x); t < count; t += 1024) {
        ivec3 C = boxCellOf(t, cd, l + 1);
        int ci = idx(C, cd);
        float sum = 0.0;
        if (mgFlags[ci] == CELL_FLUID) {
            for (int k = 0; k < 8; k++) {
                ivec3 f = 2 * C + ivec3(k & 1, (k >> 1) & 1, (k >> 2) & 1);
                if (!inLevel(f, d, l)) {
                    continue;
                }
                int fi = idx(f, d);
                if (mgFlags[fi] != CELL_FLUID) {
                    continue;
                }
                sum += bAt(l, fi) - applyA(f, fi, d, l);
            }
        }
        mgB[ci] = 0.5 * sum;
        mgX[ci] = 0.0;
    }
    sync();
}

void prolongFrom(int l) {
    ivec4 d = levelDims(l);
    ivec4 cd = levelDims(l + 1);
    int count = boxCount(d, l);
    for (int t = int(gl_LocalInvocationID.x); t < count; t += 1024) {
        ivec3 f = boxCellOf(t, d, l);
        int fi = idx(f, d);
        if (mgFlags[fi] != CELL_FLUID) {
            continue;
        }
        setX(l, fi, xAt(l, fi) + mgX[idx(f / 2, cd)]);
    }
    sync();
}

void vcycleFrom(int first, int levels, int sweeps) {
    int coarsest = levels - 1;
    for (int l = first; l < coarsest; l++) {
        relax(l, 0, l == 0);
        relax(l, 1, false);
        restrictTo(l);
    }
    bool init = levels == 1;
    for (int k = 0; k < max(sweeps, 1); k++) {
        relax(coarsest, 0, init && k == 0);
        relax(coarsest, 1, false);
    }
    for (int k = 0; k < max(sweeps, 1); k++) {
        relax(coarsest, 1, false);
        relax(coarsest, 0, false);
    }
    for (int l = coarsest - 1; l >= first; l--) {
        prolongFrom(l);
        relax(l, 1, false);
        relax(l, 0, false);
    }
}
