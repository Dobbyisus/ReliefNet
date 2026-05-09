# WiFi Direct Testing Guide

## Prerequisites
- Two Android devices (both with WiFi Direct capability)
- Android 13 or higher recommended
- USB cable and Android Studio (for debugging)

## Installation

1. Build the APK:
   ```bash
   ./gradlew assembleDebug
   ```

2. Install on both devices:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

## Initial Setup

### On Both Devices:

1. **Enable WiFi Direct**:
   - Go to Settings → Connected devices → Connection preferences → WiFi Direct
   - Enable WiFi Direct
   - Note your device name (Device1, Device2, etc.)

2. **Grant App Permissions**:
   - Open MeshRelief app
   - When prompted, grant:
     - ✓ Location (required for WiFi Direct)
     - ✓ WiFi permissions
     - ✓ Nearby WiFi Devices (Android 13+)

3. **Wait for Initialization**:
   - Status should show: "WiFi Direct transport started - Discovering peers..."
   - This indicates the app is actively scanning for nearby devices

## Testing Steps

### Step 1: Verify Peer Discovery

**On Device 1:**
1. Open MeshRelief app
2. Look at the status text
3. Click "Peers" button
4. Should show "Discovered Peers: 0"

**On Device 2:**
1. Open MeshRelief app
2. Wait 10-30 seconds

**Back on Device 1:**
1. Wait 10-30 seconds (discovery takes time)
2. Each click of "Refresh" button triggers a new scan
3. Eventually you should see discoveries appear in status
4. Click "Peers" - should now show Device 2 as "DISCOVERED"

### Step 2: Check Network Topology

1. Click "Network" button on Device 1
2. Should display:
   - Discovered Peers count
   - Connected Peers count
   - List of all discovered peers with details
   - Device names, MAC addresses, and status

### Step 3: Manual Refresh Testing

1. On ChatActivity:
   - Click the new "Refresh" button
   - Status should update immediately with peer count
   - Try clicking multiple times - should update instantly

### Step 4: Monitor Continuous Discovery

1. Keep the app open for 5+ minutes
2. Status should continuously update
3. Peers list should remain stable
4. New peers should appear as they come into range

### Step 5: Test with Multiple Nearby Devices

1. If you have 3+ devices, bring them all into range
2. Each device should discover the others
3. Peer counts should increase appropriately

## Expected Behavior

### Good Signs ✅
- Status shows peer discovery count incrementing
- "Peers" tab shows discovered devices
- "Network" tab displays peer information
- UI updates smoothly every 2-3 seconds
- LogCat shows peer discovery messages

### Problem Signs ❌
- Status stuck on "Discovering peers... 0"
- No peers ever appear
- App crashes when opening tabs
- LogCat shows permission errors

## Troubleshooting

### Issue: No Peers Discovered

**Possible Causes:**
1. WiFi Direct not enabled on device
2. Permissions not granted
3. Devices not in WiFi Direct range
4. Other device app not running

**Solutions:**
- Verify WiFi Direct in Settings is ON
- Check All permissions in app settings
- Move devices closer (within 10 meters)
- Start the app on the other device

### Issue: Permission Denied Error

**LogCat Shows:**
```
SecurityException: Permission denied
MissingPermission error
```

**Solution:**
1. Go to Settings → Apps → MeshRelief
2. Permissions → Grant all WiFi & Location permissions
3. Restart the app

### Issue: App Crashes on "Peers" Tab

**Possible Causes:**
- UI thread exception
- Null pointer in peer list update

**Solution:**
1. Check Android Studio LogCat for error
2. Clear app data: Settings → Apps → MeshRelief → Storage → Clear Data
3. Reinstall the app

### Issue: Devices in Range But Not Discovering

**Check:**
1. Both devices have WiFi Direct enabled
2. Both devices are running the MeshRelief app
3. Devices aren't connected to WiFi network (WiFi Direct uses WiFi radio)
4. Wait longer (initial discovery can take up to 30 seconds)

## LogCat Debug Output

Watch for these logs to verify operation:

### Successful Discovery:
```
I/System.out: WiFiDirectManager initialized successfully
I/System.out: WiFi Direct discovery started
I/System.out: WiFi Direct discovery scan initiated successfully
I/System.out: Peer list changed
I/System.out: Peer discovered: Device_Name (XX:XX:XX:XX:XX:XX) - Status: DISCOVERED
I/System.out: Total peers discovered: 1
```

### Connection:
```
I/System.out: WiFi Direct connected
I/System.out: Group formed - Group Owner: Yes, IP: 192.168.49.1
```

### Errors to Investigate:
```
E/System.err: WiFi Direct discovery failed: P2P is not supported
E/System.err: Failed to initialize WiFi Direct: SecurityException
E/System.err: Error requesting peer list: NullPointerException
```

## Performance Metrics

**Expected Performance:**
- Peer discovery: 10-30 seconds for first peer
- UI refresh rate: Every 2-3 seconds
- Peer list updates: Every 3 seconds
- Discovery maintains: Continuous (restarts every 100 seconds)

**Memory Usage:**
- Base app: ~50-100 MB
- With discovery active: ~80-150 MB

## Notes

1. **WiFi Direct vs WiFi**:
   - WiFi Direct doesn't require a router
   - Devices form a direct peer-to-peer connection
   - Only one device can be "Group Owner" (acts like router)

2. **Device Role**:
   - Group Owner: Assigns IPs, other devices connect to it
   - Client: Connects to Group Owner
   - Role negotiated automatically by devices

3. **Persistence**:
   - Discovered peers persist until out of range
   - Device removal detected by WiFi Direct framework
   - Status automatically updated via broadcast receiver

4. **Energy Impact**:
   - Continuous discovery consumes battery
   - Consider adding power management for production
   - WiFi Direct scan every 100 seconds is reasonable

## Success Checklist

- [ ] App compiles and installs
- [ ] All permissions granted
- [ ] Status shows "Discovering peers..."
- [ ] Peer discovered within 30 seconds
- [ ] "Peers" tab shows discovered device
- [ ] "Network" tab shows topology
- [ ] "Refresh" button updates status
- [ ] App remains stable for 5+ minutes
- [ ] No crashes in LogCat
- [ ] Multiple devices discovered properly
- [ ] UI updates smoothly and continuously

If all items are checked, WiFi Direct discovery is working correctly! 🎉

