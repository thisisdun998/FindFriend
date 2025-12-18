# FindFriend IntelliJ Plugin

<!-- Plugin description -->
A real-time friend messaging plugin for IntelliJ IDEA, designed for compatibility with version 2021.1 and above.

## Features

- **Real-time Messaging**: Uses WebSocket (JDK 11 native) for low-latency communication.
- **Configurable Connection**: Set your unique User ID in IDE Settings.
- **UI Integration**:
    - **Notifications**: Popup alerts on new messages.
    - **ToolWindow**: Dedicated panel to send messages to specific users.
- **Persistence**: Chat history is saved locally.
- **Reliability**: Automatic reconnection and heartbeat mechanism.
<!-- Plugin description end -->

## Project Structure

- **services**:
    - `WebSocketService`: Manages WebSocket connection, heartbeat, and message handling.
    - `ChatHistoryService`: Persists chat messages using `PersistentStateComponent`.
- **ui**:
    - `NotificationDialog`: Custom Swing dialog for incoming message alerts.
- **toolWindow**:
    - `MyToolWindowFactory`: Provides the "FindFriendChat" tool window UI for sending messages.
- **settings**:
    - `AppSettingsState`: Persists plugin configuration (User ID).
    - `AppSettingsConfigurable`: Provides the settings UI in Preferences.
- **startup**:
    - `MyProjectActivity`: Initializes services on project open.

## Configuration

1. Go to **Settings/Preferences** > **Tools** > **FindFriend Settings**.
2. Enter your **User ID**.
3. Apply changes. The plugin will automatically reconnect with the new ID.

## Usage

1. Open **FindFriendChat** tool window (right sidebar).
2. Enter the **Target User ID** and **Message**.
3. Click **Send**.
4. Incoming messages will appear as popup notifications.
