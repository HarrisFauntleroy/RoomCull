# Changelog

All notable changes to RoomCull will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [v1.0.1] - 2025-07-30

### Added
- Comprehensive testing infrastructure with unit tests
- Code quality tooling including Checkstyle, PMD, and SpotBugs
- Automated CI/CD workflows for CurseForge and Modrinth deployment

### Changed
- Renamed `occlusion_plane` to `room_block` for better clarity
- Enhanced room block functionality with improved performance
- Added comprehensive JavaDoc documentation across codebase

### Improved
- Code quality and maintainability through static analysis tools
- Documentation coverage with detailed JavaDoc comments

## [v1.0.0] - 2025-07-29

### Added
- Room-based occlusion culling for improved performance
- Automatic room boundary detection up to 50 blocks in each direction
- Visual debug particles showing detected room boundaries
- Glowstone-textured room blocks for defining room centers
- 6-plane frustum culling at detected room boundaries
- Real-time boundary updates with periodic rescanning
- Performance optimized caching and cleanup system

### Requirements
- Minecraft 1.21.1
- NeoForge 21.0.167+
- Java 21