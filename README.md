<div align="center">

# ⚔️ HttpArena

**HTTP framework benchmark platform — who is the fastest?**

16 test profiles · 64-core dedicated hardware

[**🏆 View Leaderboard**](https://MDA2AV.github.io/HttpArena/) · [**📖 Documentation**](https://MDA2AV.github.io/HttpArena/docs/) · [**➕ Add a Framework**](https://MDA2AV.github.io/HttpArena/docs/add-framework/)

</div>

---

## What is HttpArena?

HttpArena benchmarks HTTP frameworks across **realistic, diverse workloads** on dedicated hardware. Not just plaintext — real tests that show how frameworks actually perform.

## Test Profiles

| Category | Profiles | What it tests |
|----------|----------|--------------|
| 🔌 **Connection** | Baseline (512→32K), Pipelined, Limited | How performance scales with connection count |
| 📦 **Workload** | JSON, Compression, Upload, Database | Real-world processing — serialization, gzip, I/O |
| 🛡️ **Resilience** | Noisy, Mixed | Stability under malformed requests and concurrent endpoints |
| 🔒 **Protocol** | HTTP/2, HTTP/3, gRPC, WebSocket | Beyond HTTP/1.1 |

## Quick Start

### Run benchmarks locally

```bash
git clone https://github.com/MDA2AV/HttpArena.git
cd HttpArena

# Validate a framework (correctness check)
./scripts/validate.sh <framework>

# Benchmark a framework (all profiles)
./scripts/benchmark.sh <framework>

# Benchmark a specific profile
./scripts/benchmark.sh <framework> baseline

# Save results
./scripts/benchmark.sh <framework> --save
```

### Add your framework

1. Create `frameworks/<name>/Dockerfile`
2. Implement the [required endpoints](https://MDA2AV.github.io/HttpArena/docs/add-framework/)
3. Add `frameworks/<name>/meta.json`
4. Open a PR — validation runs automatically

See any existing framework in `frameworks/` for a working example.

## PR Commands

Use these commands in PR comments to trigger CI:

| Command | Action |
|---------|--------|
| `/validate` | Run the 18-point validation suite |
| `/benchmark` | Run all benchmark profiles |
| `/benchmark baseline` | Run a specific profile |

> 💡 **@BennyFranciscus** is our AI assistant — tag him on your PR for help with implementation, debugging, or questions about benchmark results.

## Hardware

All benchmarks run on the same dedicated machine:

- **CPU:** 64-core AMD Threadripper
- **Conditions:** Dedicated hardware, no VMs, no noisy neighbors
- **Load generator:** [gcannon](https://github.com/MDA2AV/gcannon) (io_uring-based)

## Contributing

- **Add a framework** — [guide](https://MDA2AV.github.io/HttpArena/docs/add-framework/)
- **Report issues** — [open an issue](https://github.com/MDA2AV/HttpArena/issues)
- **Discuss** — comment on any open issue or PR

---

<div align="center">

**[🏆 View the Leaderboard](https://MDA2AV.github.io/HttpArena/)**

</div>
