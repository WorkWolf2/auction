# Custom Limit Messages - Testing Guide

## Overview
This feature allows each permission node in the limits system to have its own custom message when a player reaches their sales limit, instead of using the generic "You have reached the limit of sales" message.

## Configuration

### limits.yml Structure
Each permission node can now have a `message` field:

```yaml
limits:
  "default":
    permissions:
      "group-default": true
    limit: 1
    message: "&cYou have reached the default sales limit of 1 auction. &aUpgrade to VIP for more slots!"
  "vip":
    permissions:
      "group-vip": true
      "prestige-10": false
    limit: 3
    message: "&cYou have reached the VIP sales limit of 3 auctions. &aUpgrade to MVP for more slots!"
  "mvp":
    permissions:
      "group-mvp": true
    limit: 5
    message: "&cYou have reached the MVP sales limit of 5 auctions. &aUpgrade to MVP+ for more slots!"
  "mvp-plus":
    permissions:
      "group-mvp-plus": true
    limit: 10
    message: "&cYou have reached the MVP+ sales limit of 10 auctions. &6You have the highest tier!"
```

## Testing Instructions

### 1. Basic Functionality Test
1. Start the server with the updated plugin
2. Give a player the appropriate permissions (e.g., `group-vip`)
3. Use the test command: `/hauctions testlimit`
4. Verify the output shows the correct limit and custom message

### 2. Limit Reached Test
1. Give a player permissions for a specific tier (e.g., VIP with limit 3)
2. Have them create auctions until they reach their limit
3. Try to create one more auction with `/hauctions sell <price>`
4. Verify they see the custom message instead of the generic one

### 3. Permission Hierarchy Test
1. Give a player multiple permission groups
2. Verify they get the message from the highest limit permission
3. Test with different combinations:
   - `group-default` only → should show default message
   - `group-default` + `group-vip` → should show VIP message
   - `group-vip` + `group-mvp` → should show MVP message

### 4. Fallback Test
1. Create a permission node without a `message` field
2. Verify players with that permission get the default message
3. Test with a completely unconfigured player (no matching permissions)

## Expected Behavior

### Message Selection Logic
- The system finds the permission node with the highest limit that the player has
- If that permission node has a custom message, it uses that message
- If no custom message is configured, it falls back to the default message
- If the player has no matching permissions, they get limit 0 and the default message

### Message Formatting
- Messages support Minecraft color codes (`&c`, `&a`, etc.)
- Messages are processed through the component serializer for rich text support
- Placeholders can be added in future updates if needed

## Troubleshooting

### Common Issues
1. **Custom message not showing**: Check that the permission node has the exact permissions the player has
2. **Wrong message showing**: Verify permission hierarchy - highest limit wins
3. **No message at all**: Check console for configuration errors during reload

### Debug Commands
- `/hauctions testlimit` - Shows current limit and message for the executing player
- `/hauctions reload` - Reloads all configurations including limit messages

### Console Logging
The plugin logs warnings for:
- Invalid limit sections in configuration
- Missing required fields (permissions, limit)
- Configuration parsing errors

## Implementation Details

### Files Modified
- `LimitManager.java` - Added message loading and retrieval
- `SellCommand.java` - Updated to use custom messages
- `limits.yml` - Added message fields to permission nodes

### New Methods
- `LimitManager.getLimitMessage(Player)` - Returns custom message for player's highest permission
- `TestLimitCommand` - Debug command for testing functionality
- `LimitMessageTest` - Utility class for manual testing

### Backward Compatibility
- Existing configurations without message fields continue to work
- Default message is used when no custom message is configured
- No breaking changes to existing functionality
