# saker.maven.classpath

[![Build status](https://img.shields.io/azure-devops/build/sakerbuild/7a2f5570-dd09-4779-96c0-1a3c0d3c8b8b/12/master)](https://dev.azure.com/sakerbuild/saker.maven.classpath/_build) [![Latest version](https://mirror.nest.saker.build/badges/saker.maven.classpath/version.svg)](https://nest.saker.build/package/saker.maven.classpath "saker.maven.classpath | saker.nest")

Package containing build task for creating Java classpath during build execution with the [saker.build system](https://saker.build). The `saker.maven.classpath()` build task automatically downloads the specified artifacts and related sources to be used as an input to Java compilation.

See the [documentation](https://saker.build/saker.maven.classpath/doc/) for more information.

## Build instructions

The library uses the [saker.build system](https://saker.build) for building. Use the following command to build the project:

```
java -jar path/to/saker.build.jar -bd build compile saker.build
```

## License

The source code for the project is licensed under *GNU General Public License v3.0 only*.

Short identifier: [`GPL-3.0-only`](https://spdx.org/licenses/GPL-3.0-only.html).

Official releases of the project (and parts of it) may be licensed under different terms. See the particular releases for more information.
