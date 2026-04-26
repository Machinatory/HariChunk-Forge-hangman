// Perlin noise batch computation kernel for Minecraft 1.20.1
// Matches vanilla PerlinNoiseSampler.sample() output
// Processed in batch: computes N noise values in parallel on GPU

__constant double FLAT_SIMPLEX_GRAD[64] = {
    1, 1, 0, 0,  -1, 1, 0, 0,  1, -1, 0, 0,  -1, -1, 0, 0,
    1, 0, 1, 0,  -1, 0, 1, 0,  1, 0, -1, 0,  -1, 0, -1, 0,
    0, 1, 1, 0,   0, -1, 1, 0, 0, 1, -1, 0,   0, -1, -1, 0,
    1, 1, 0, 0,   0, -1, 1, 0, -1, 1, 0, 0,   0, -1, -1, 0
};

double perlin_sample_single(
    __global const int* permutation,
    double originX, double originY, double originZ,
    double x, double y, double z,
    double yScale, double yMax
) {
    double d = x + originX;
    double e = y + originY;
    double f = z + originZ;
    double i = floor(d);
    double j = floor(e);
    double k = floor(f);
    double g = d - i;
    double h = e - j;
    double l = f - k;
    double o = 0.0;
    if (yScale != 0.0) {
        double m;
        if (yMax >= 0.0 && yMax < h) {
            m = yMax;
        } else {
            m = h;
        }
        o = floor(m / yScale + 1.0E-7) * yScale;
    }

    int sectionX = (int)i;
    int sectionY = (int)j;
    int sectionZ = (int)k;
    double localX = g;
    double localY = h - o;
    double localZ = l;
    double fadeLocalX = h;

    int var0 = sectionX & 0xFF;
    int var1 = (sectionX + 1) & 0xFF;
    int var2 = permutation[var0] & 0xFF;
    int var3 = permutation[var1] & 0xFF;
    int var4 = (var2 + sectionY) & 0xFF;
    int var5 = (var3 + sectionY) & 0xFF;
    int var6 = (var2 + sectionY + 1) & 0xFF;
    int var7 = (var3 + sectionY + 1) & 0xFF;
    int var8 = permutation[var4] & 0xFF;
    int var9 = permutation[var5] & 0xFF;
    int var10 = permutation[var6] & 0xFF;
    int var11 = permutation[var7] & 0xFF;

    int var12 = (var8 + sectionZ) & 0xFF;
    int var13 = (var9 + sectionZ) & 0xFF;
    int var14 = (var10 + sectionZ) & 0xFF;
    int var15 = (var11 + sectionZ) & 0xFF;
    int var16 = (var8 + sectionZ + 1) & 0xFF;
    int var17 = (var9 + sectionZ + 1) & 0xFF;
    int var18 = (var10 + sectionZ + 1) & 0xFF;
    int var19 = (var11 + sectionZ + 1) & 0xFF;
    int var20 = (permutation[var12] & 15) << 2;
    int var21 = (permutation[var13] & 15) << 2;
    int var22 = (permutation[var14] & 15) << 2;
    int var23 = (permutation[var15] & 15) << 2;
    int var24 = (permutation[var16] & 15) << 2;
    int var25 = (permutation[var17] & 15) << 2;
    int var26 = (permutation[var18] & 15) << 2;
    int var27 = (permutation[var19] & 15) << 2;

    double var60 = localX - 1.0;
    double var61 = localY - 1.0;
    double var62 = localZ - 1.0;

    double var87  = FLAT_SIMPLEX_GRAD[var20 | 0] * localX + FLAT_SIMPLEX_GRAD[var20 | 1] * localY + FLAT_SIMPLEX_GRAD[var20 | 2] * localZ;
    double var88  = FLAT_SIMPLEX_GRAD[var21 | 0] * var60  + FLAT_SIMPLEX_GRAD[var21 | 1] * localY + FLAT_SIMPLEX_GRAD[var21 | 2] * localZ;
    double var89  = FLAT_SIMPLEX_GRAD[var22 | 0] * localX + FLAT_SIMPLEX_GRAD[var22 | 1] * var61  + FLAT_SIMPLEX_GRAD[var22 | 2] * localZ;
    double var90  = FLAT_SIMPLEX_GRAD[var23 | 0] * var60  + FLAT_SIMPLEX_GRAD[var23 | 1] * var61  + FLAT_SIMPLEX_GRAD[var23 | 2] * localZ;
    double var91  = FLAT_SIMPLEX_GRAD[var24 | 0] * localX + FLAT_SIMPLEX_GRAD[var24 | 1] * localY + FLAT_SIMPLEX_GRAD[var24 | 2] * var62;
    double var92  = FLAT_SIMPLEX_GRAD[var25 | 0] * var60  + FLAT_SIMPLEX_GRAD[var25 | 1] * localY + FLAT_SIMPLEX_GRAD[var25 | 2] * var62;
    double var93  = FLAT_SIMPLEX_GRAD[var26 | 0] * localX + FLAT_SIMPLEX_GRAD[var26 | 1] * var61  + FLAT_SIMPLEX_GRAD[var26 | 2] * var62;
    double var94  = FLAT_SIMPLEX_GRAD[var27 | 0] * var60  + FLAT_SIMPLEX_GRAD[var27 | 1] * var61  + FLAT_SIMPLEX_GRAD[var27 | 2] * var62;

    double var95 = localX * 6.0 - 15.0;
    double var96 = fadeLocalX * 6.0 - 15.0;
    double var97 = localZ * 6.0 - 15.0;
    double var98 = localX * var95 + 10.0;
    double var99 = fadeLocalX * var96 + 10.0;
    double var100 = localZ * var97 + 10.0;
    double var101 = localX * localX * localX * var98;
    double var102 = fadeLocalX * fadeLocalX * fadeLocalX * var99;
    double var103 = localZ * localZ * localZ * var100;

    double var113 = var87 + var101 * (var88 - var87);
    double var114 = var93 + var101 * (var94 - var93);
    double var115 = var91 + var101 * (var92 - var91);
    double var116 = var89 + var101 * (var90 - var89);
    double var117 = var114 - var115;
    double var118 = var102 * (var116 - var113);
    double var119 = var102 * var117;
    double var120 = var113 + var118;
    double var121 = var115 + var119;
    return var120 + var103 * (var121 - var120);
}

__kernel void perlin_noise_batch(
    __global const int* permutation,
    double originX, double originY, double originZ,
    double yScale, double yMax,
    __global const double* x_coords,
    __global const double* y_coords,
    __global const double* z_coords,
    __global double* results,
    int count
) {
    int gid = get_global_id(0);
    if (gid >= count) return;

    results[gid] = perlin_sample_single(
        permutation, originX, originY, originZ,
        x_coords[gid], y_coords[gid], z_coords[gid],
        yScale, yMax
    );
}
