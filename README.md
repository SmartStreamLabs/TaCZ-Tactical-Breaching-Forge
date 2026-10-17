# TaCZ Tactical Breaching

Source for version 1.0.7 of the TaCZ Tactical Breaching addon.

| Project | Minecraft | Loader | Java |
| --- | --- | --- | --- |
| `tacz-door-breaker` | 1.20.1 | Forge | 17 |
| `tacz-tactical-breaching-neoforge-1.21.1` | 1.21.1 | NeoForge | 21 |

Each project has its own README, Gradle wrapper, dependencies, and license file. TaCZ is required at runtime. Superb Warfare integration is optional; the development runs can include it without bundling it into this addon.

## Build on Windows

Run `Build-SBW-Magazines.cmd` from this directory to build both projects. Alternatively, run `gradlew.bat build` in either project directory. The resulting mod JARs are in each project's `build/libs` directory; do not use `-sources.jar` as a playable mod.

## Development clients

`Run-All-Clients.cmd` starts both development clients. The `Run-SBW-0.8.9-Final-Clients.cmd` script starts the development clients with the existing Superb Warfare test setup.

## Notes

- Generated builds, local worlds, logs, and configuration files are excluded from this source package.
- The Forge flashlight V3 changes are included in this source snapshot, but their final visual acceptance and performance testing remain pending.
- The project license is GPL-3.0-only; see the license files in each project.
