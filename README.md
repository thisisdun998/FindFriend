# FindFriend IntelliJ Plugin

<!-- Plugin description -->
A real-time friend messaging plugin for IntelliJ IDEA, designed for compatibility with version 2021.1 and above.

## Features

- **Unique Identity**: Automatically generates a unique, immutable User ID upon installation.
- **Real-time Messaging**: Uses WebSocket (JDK 11 native) for low-latency communication.
- **Enhanced UI Integration**:
    - **Notifications**: Large popup alerts for new messages with **Direct Reply** capability.
    - **Chat ToolWindow**: 
        - **Rich Chat List**: Tabular view displaying **User ID**, **Last Chat Time**, and editable **Nickname**.
        - **Chat Detail**: Dedicated view with nickname display and system alerts (e.g., user offline).
        - **New Chat**: Easily start conversations by entering a User ID.
- **Friend Management**: Set custom nicknames for friends directly in the chat list.
- **System Feedback**: In-chat prompts for delivery errors (e.g., if a user is offline).
- **Persistence**: Chat history and nicknames are saved locally.
- **Reliability**: Automatic reconnection and heartbeat mechanism.
<!-- Plugin description end -->

## Project Structure

- **services**:
    - `WebSocketService`: Manages WebSocket connection, heartbeat, error handling, and message dispatch.
    - `ChatHistoryService`: Persists chat messages, conversations, and nicknames.
    - `ChatListener`: Event listener for real-time UI updates.
- **ui**:
    - `NotificationDialog`: Large custom Swing dialog for message alerts with reply input.
- **toolWindow**:
    - `MyToolWindowFactory`: Implements the "FindFriendChat" tool window with a `JBTable` based list and detail views.
- **settings**:
    - `AppSettingsState`: Persists plugin configuration (User ID).
    - `AppSettingsConfigurable`: Displays the read-only User ID in Settings.
- **startup**:
    - `MyProjectActivity`: Initializes services on project open.

## Configuration

1. Go to **Settings/Preferences** > **Tools** > **FindFriend Settings**.
2. View your auto-generated **User ID**. Share this ID with friends to receive messages.

## Usage

1. Open **FindFriendChat** tool window (right sidebar).
2. Click **New Chat** to start a conversation with a friend's User ID.
3. **Chat List**:
   - Double-click a row to open the chat.
   - Click the **Nickname** column to edit the friend's remark name.
4. **Chat Detail**:
   - Send and receive messages.
   - System messages (like "User offline") appear directly in the chat history.
5. Incoming messages will appear as popup notifications. You can **Reply** directly from the popup.
6. The chat data is stored in the local project and can be used with confidence.