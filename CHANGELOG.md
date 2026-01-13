# Changelog

All notable changes to the FindFriend plugin will be documented in this file.

## [Unreleased]

## [0.0.3] - 2025-12-26

### Added
- Notification dialog support with direct reply functionality
- Keyboard shortcut for reply: Ctrl+Enter (Windows/Linux) or Cmd+Enter (macOS)
- WebSocket heartbeat and reconnection mechanism with timeout detection
- Message queue for pending messages during disconnection
- Plugin icon in marketplace

### Fixed
- Heart beat messages no longer trigger popup notifications
- Messages sent after reconnection are now properly queued and delivered
- Empty message notifications from heartbeat responses are filtered out

### Improved
- Connection state management with atomic variables
- Automatic reconnection with exponential backoff
- Message persistence and recovery after disconnection

## [0.0.2] - 2025-12-20

### Added
- Initial release
- WebSocket-based real-time messaging
- Auto-generated unique User ID
- Chat history persistence
- Tool window with chat list and detail views
- Friend nickname management
- Notification popups for incoming messages

## [0.0.4] - 2026-01-13

### Fixed
- A restart error caused by the path of the plugin icon