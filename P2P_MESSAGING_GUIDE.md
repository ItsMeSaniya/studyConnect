# Peer-to-Peer Messaging Guide

## Overview
The application now supports **server-mediated peer-to-peer (P2P) messaging** where clients can send private messages to each other through the server.

## Architecture

### How It Works
1. **All clients connect to a central server** (started by admin)
2. **Server maintains a registry** of connected users and their connections
3. **Clients can select a peer** from the dropdown in the P2P Chat tab
4. **Messages are routed through the server** to the target peer
5. **Server forwards messages** to the correct recipient

### Message Flow
```
Client A → Server → Client B
   ↑                    ↓
   └────── Response ────┘
```

## Implementation Details

### 1. New Message Type
- **PEER_LIST**: Server sends list of connected peers to all clients
- **PEER_TO_PEER**: Used for P2P messages between clients

### 2. Server Changes (`Server.java`)

#### New Fields
- `connectionUsernames`: Maps `PeerConnection` objects to usernames
  ```java
  private Map<PeerConnection, String> connectionUsernames;
  ```

#### New Methods
- `handleClientMessage()`: Intercepts and routes all incoming messages
- `broadcastPeerList()`: Sends updated peer list to all clients

#### Message Routing Logic
```java
case PEER_TO_PEER:
    // Find target connection by username
    String targetUser = message.getReceiver();
    PeerConnection targetConnection = findConnectionByUsername(targetUser);
    
    if (targetConnection != null) {
        targetConnection.sendMessage(message);
    }
```

### 3. Client Changes (`MainDashboard.java`)

#### Peer List Handling
```java
case PEER_LIST:
    // Parse comma-separated list of usernames
    String[] peers = message.getContent().split(",");
    
    // Update P2P Chat dropdown
    SwingUtilities.invokeLater(() -> {
        peerSelector.removeAllItems();
        for (String peer : peers) {
            if (!peer.isEmpty() && !peer.equals(currentUser)) {
                peerSelector.addItem(peer);
            }
        }
    });
```

#### Sending P2P Messages
```java
private void sendP2PMessage() {
    String selectedPeer = (String) peerSelector.getSelectedItem();
    String messageText = p2pMessageField.getText().trim();
    
    if (selectedPeer != null && !messageText.isEmpty()) {
        Message msg = new Message(currentUser, selectedPeer, 
            messageText, MessageType.PEER_TO_PEER);
        client.sendMessage(msg);
    }
}
```

### 4. Message Class Updates (`Message.java`)

#### New Method
```java
public String getReceiver() {
    return recipient;  // Alias for P2P messages
}
```

## Testing the Feature

### Setup (3 Instances Required)
1. **Instance 1 (Admin/Server)**
   - Login as admin
   - Click "Start Server" on port 5000

2. **Instance 2 (Client A)**
   - Login as "Alice"
   - Click "Connect to Server" → localhost:5000
   - After connection, peer list updates automatically

3. **Instance 3 (Client B)**
   - Login as "Bob"
   - Click "Connect to Server" → localhost:5000
   - After connection, peer list updates automatically

### Sending P2P Messages
1. Open **P2P Chat** tab
2. Select peer from dropdown (e.g., Alice selects "Bob")
3. Type message in text field
4. Click "Send P2P" button
5. Message appears only on sender's and receiver's screens

### Expected Behavior
- ✅ Peer dropdown shows all connected users (except yourself)
- ✅ P2P messages only visible to sender and receiver
- ✅ Peer list updates when users join/leave
- ✅ Server logs show message forwarding: `[SERVER] Forwarded P2P message from Alice to Bob`

## User Events

### When a User Joins
1. Client sends `USER_JOIN` message to server
2. Server stores `username → connection` mapping
3. Server broadcasts join notification to all clients
4. Server sends updated `PEER_LIST` to all clients

### When a User Leaves
1. Connection closes (disconnect or crash)
2. Server removes from `connectionUsernames` map
3. Server broadcasts leave notification
4. Server sends updated `PEER_LIST` to all clients

## Troubleshooting

### Peer Dropdown is Empty
- **Cause**: No other clients connected
- **Solution**: Connect at least 2 clients to the same server

### Message Not Delivered
- **Cause**: Target user disconnected before message sent
- **Solution**: Server logs will show "Target user not found"
- **Check**: Server console for forwarding logs

### Peer List Not Updating
- **Cause**: `USER_JOIN` message not sent on connection
- **Solution**: Ensure client sends `USER_JOIN` after connecting

## Code Locations

| Feature | File | Lines |
|---------|------|-------|
| Message routing | `Server.java` | 123-178 |
| Peer list broadcasting | `Server.java` | 183-193 |
| PEER_LIST handling | `MainDashboard.java` | 930-945 |
| Send P2P message | `MainDashboard.java` | 850-867 |
| Message types | `Message.java` | 13-23 |

## Security Considerations

⚠️ **Current Implementation**:
- No message encryption (plain text over sockets)
- No authentication of message sender
- Server can read all P2P messages

🔒 **Recommended Improvements**:
- Enable SSL/TLS for encrypted transport
- Implement end-to-end encryption for P2P messages
- Add message signing to verify sender identity

## Next Steps

### Potential Enhancements
1. **Message History**: Store P2P messages locally
2. **Read Receipts**: Notify sender when message is received
3. **Typing Indicators**: Show when peer is typing
4. **File Sharing**: Extend P2P to support file transfers
5. **Group P2P**: Allow selecting multiple recipients
6. **Offline Messages**: Queue messages for offline peers

## API Reference

### Message Constructor for P2P
```java
Message(String sender, String receiver, String content, MessageType.PEER_TO_PEER)
```

### Server Methods
```java
handleClientMessage(Message message, PeerConnection connection)
broadcastPeerList()
```

### Client Methods
```java
sendP2PMessage()
updatePeerList(String[] peers)
```

---

**Last Updated**: 2025-11-11  
**Version**: 1.0.0  
**Author**: GitHub Copilot
