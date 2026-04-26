# HariChunk Changelog

## HariChunk - Concurrent Chunk Management Engine (Forge)

A Forge mod designed to improve the chunk performance of Minecraft.

**Author:** Hari
**Credits:** Adman (Quantified API)
**License:** MIT
**Minecraft:** 1.20.1
**Forge:** 47.4.0

---

## Modules

| Module | Description |
|---|---|
| `harichunk_base` | Core infrastructure, config system, mixin plugin |
| `harichunk_threading_chunkio` | Asynchronous chunk loading/unloading |
| `harichunk_threading_worldgen` | Parallel world generation |
| `harichunk_threading_lighting` | Parallel lighting updates |
| `harichunk_opts_chunkio` | Chunk I/O optimizations |
| `harichunk_opts_chunk_access` | Chunk access optimizations |
| `harichunk_opts_scheduling` | Enhanced task scheduling |
| `harichunk_opts_worldgen_general` | General worldgen optimizations |
| `harichunk_opts_worldgen_vanilla` | Vanilla worldgen optimizations |
| `harichunk_opts_worldgen_biome_cache` | Biome cache |
| `harichunk_opts_allocs` | Memory allocation optimizations |
| `harichunk_opts_math` | Math optimizations |
| `harichunk_opts_dfc` | Density Function Compiler |
| `harichunk_opts_gpu_noise` | GPU-accelerated noise generation |
| `harichunk_rewrites_chunkio` | Chunk I/O rewrites |
| `harichunk_rewrites_chunk_serializer` | Chunk serializer rewrites |
| `harichunk_fixes_chunkio_threading_issues` | Chunk I/O threading fixes |
| `harichunk_fixes_general_threading_issues` | General threading fixes |
| `harichunk_fixes_worldgen_threading_issues` | Worldgen threading fixes |
| `harichunk_fixes_worldgen_vanilla_bugs` | Vanilla worldgen bug fixes |
| `harichunk_notickvd` | No-tick view distance |
| `harichunk_client_uncapvd` | Client view distance uncap |
| `harichunk_server_utils` | Server utilities |
| `harichunk_natives_opts` | Native optimizations |
| `harichunk_flowsched` | Advanced task scheduling system |
| `harichunk_quantified` | Quantified performance analysis |
| `harichunk_compat_immersivepetroleum` | Immersive Petroleum compatibility |

---

## Version History

### 0.2.0+alpha.12
- Initial HariChunk standalone release
- Complete rebrand from C2ME fork
- 27 optimization modules
- Forge 47.4.0 for Minecraft 1.20.1
