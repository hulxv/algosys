# syntax=docker/dockerfile:1
#
# AlgoSys server - multi-language complexity analyzer.
# Builds MetaCall from source so every loader is included.
#
# Loaders built:
#   py  (Python 3)       rb   (Ruby)        node  (Node.js)
#   ts  (TypeScript)     cs   (C# / .NET 8) c     (C via TCC + libclang)
#   cob (COBOL)          rs   (Rust nightly) wasm  (WebAssembly / Wasmtime)
#
# Loaders intentionally excluded:
#   lua  - upstream lua_loader has unfixed build bugs (missing lauxlib.h include)
#   go   - loader is TODO in upstream metacall (only a Go *port* exists)
#   java - removed; causes SIGSEGV in the multithreaded Flask process
#   v8 / jsm - require building V8 from source; impractical for this image
#

#  Stage 1: install build-time language runtimes 
FROM debian:trixie-slim AS metacall-deps

ENV DEBIAN_FRONTEND=noninteractive \
    LTTNG_UST_REGISTER_TIMEOUT=0 \
    NUGET_XMLDOC_MODE=skip \
    DOTNET_CLI_TELEMETRY_OPTOUT=true

WORKDIR /metacall

# Fetch metacall source (shallow clone of master)
RUN apt-get update && apt-get install -y --no-install-recommends \
        git ca-certificates \
    && git clone --depth=1 https://github.com/metacall/core.git /metacall \
    && rm -rf /var/lib/apt/lists/*

# Install build-time deps via metacall's own environment script.
# Covers: base toolchain, Python, Ruby, Node.js, TypeScript, .NET 8,
#         C (libffi + libclang), COBOL, Rust (via rustup), WASM (Wasmtime
#         is fetched by CMake during the build stage), RPC (libcurl),
#         File loader, RapidJSON, backtrace, sandbox (libseccomp).
RUN chmod +x /metacall/tools/metacall-environment.sh \
    && /metacall/tools/metacall-environment.sh relwithdebinfo \
        base \
        python ruby nodejs typescript \
        netcore8 \
        c cobol rust wasm rpc file \
        rapidjson backtrace sandbox

#  Stage 2: build MetaCall with all loaders 
FROM metacall-deps AS metacall-build

ENV LOADER_LIBRARY_PATH=/metacall/build \
    LOADER_SCRIPT_PATH=/metacall/build/scripts \
    CONFIGURATION_PATH=/metacall/build/configurations/global.json \
    SERIAL_LIBRARY_PATH=/metacall/build \
    DETOUR_LIBRARY_PATH=/metacall/build \
    NODE_PATH=/usr/lib/node_modules \
    DOTNET_CLI_TELEMETRY_OPTOUT=true \
    # rustup installs cargo here; cmake needs it on PATH during build
    PATH=/root/.cargo/bin:$PATH

WORKDIR /metacall/build

# Configure all loaders
RUN /metacall/tools/metacall-configure.sh relwithdebinfo \
    python ruby nodejs typescript netcore8 c cobol rust wasm rpc file

# Build and install everything to /usr/local
RUN /metacall/tools/metacall-build.sh relwithdebinfo install \
    && mkdir -p /usr/local/scripts

#  Stage 3: minimal runtime + AlgoSys server 
FROM debian:trixie-slim AS server

ENV DEBIAN_FRONTEND=noninteractive \
    LOADER_LIBRARY_PATH=/usr/local/lib \
    LOADER_SCRIPT_PATH=/usr/local/scripts \
    CONFIGURATION_PATH=/usr/local/share/metacall/configurations/global.json \
    SERIAL_LIBRARY_PATH=/usr/local/lib \
    DETOUR_LIBRARY_PATH=/usr/local/lib \
    NODE_PATH=/usr/local/lib/node_modules \
    DOTNET_CLI_TELEMETRY_OPTOUT=true \
    LTTNG_UST_REGISTER_TIMEOUT=0 \
    PATH=/root/.cargo/bin:/usr/local/bin:$PATH

#  runtime libraries for every loader 
RUN apt-get update && apt-get install -y --no-install-recommends \
    # Python
    python3 python3-pip \
    # Node.js
    nodejs libnode-dev \
    # C loader (libffi + TCC built into metacall; libclang is the runtime lib)
    libffi8 libclang1 \
    # backtrace plugin (used by metacall for crash diagnostics)
    libdw1 \
    # COBOL (gnucobol runtime)
    libcob4 \
    # RPC loader (libcurl - trixie ships libcurl4t64)
    libcurl4t64 \
    # Rust toolchain bootstrap (rs_loader compiles Rust at runtime via cargo).
    # gcc provides the `cc` linker that rustc requires to link compiled crates;
    # libc6-dev provides crti.o/crtn.o (glibc startup objects) and the
    # unversioned libc.so symlink — both needed by the final link step.
    curl autoconf automake ca-certificates wget gcc libc6-dev \
    && rm -rf /var/lib/apt/lists/*

# .NET 8 runtime - metacall-runtime.sh only goes up to netcore5.
# --version 8.0.408 is an SDK version; use --channel 8.0 for the runtime installer.
RUN wget -qO - https://dot.net/v1/dotnet-install.sh \
    | bash -s -- --channel 8.0 --runtime dotnet --install-dir /usr/local/bin \
    && rm -rf /var/lib/apt/lists/*

# Rust nightly toolchain - rs_loader invokes cargo at runtime to JIT-compile
# user-submitted Rust code. Use --profile minimal to keep the image smaller.
RUN curl https://sh.rustup.rs -sSf \
    | sh -s -- -y --default-toolchain nightly-2021-12-04 --profile minimal \
    && rm -rf /var/lib/apt/lists/*

ENV PATH=/root/.cargo/bin:$PATH

#  copy built MetaCall from build stage 
RUN mkdir -p /usr/local/scripts
COPY --from=metacall-build /usr/local/lib/     /usr/local/lib/
COPY --from=metacall-build /usr/local/include/metacall \
                            /usr/local/include/metacall
COPY --from=metacall-build /usr/local/share/metacall/ \
                            /usr/local/share/metacall/
COPY --from=metacall-build /usr/local/scripts/ /usr/local/scripts/

RUN ldconfig

#  AlgoSys Flask server 
WORKDIR /app

COPY requirements.txt .
RUN pip3 install --no-cache-dir --break-system-packages -r requirements.txt

COPY . .

EXPOSE 5000
CMD ["python3", "app.py"]
