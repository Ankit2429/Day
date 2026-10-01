# THE DAY — ESP32 Firmware Guide

## Architecture Overview
```
ANDROID PHONE (THE DAY)
        ↓
    Wi-Fi LAN
        ↓
      ESP32 (WebSocket ws://<IP>/ws)
        ↓
 MAX7219 8x8 LED Matrix
```

---

## 1. Hardware Pinout (ESP32 Dev Module to MAX7219)

| MAX7219 Pin | ESP32 Pin | Function |
|-------------|-----------|----------|
| **VCC**     | **5V / VIN** | Power supply (5V recommended for bright LEDs) |
| **GND**     | **GND**   | Common Ground |
| **DIN**     | **GPIO 23** | VSPI MOSI (Hardware SPI Data) |
| **CS**      | **GPIO 5**  | VSPI SS (Chip Select) |
| **CLK**     | **GPIO 18** | VSPI SCK (Hardware SPI Clock) |

---

## 2. Required Libraries in Arduino IDE

Open **Arduino IDE** → **Tools** → **Manage Libraries...** and install:
1. **ArduinoJson** (by Benoit Blanchon, v6.x or v7.x)
2. **MD_MAX72XX** (by MajicDesigns)
3. **ESPAsyncWebServer** (by me-no-dev / lacamera)
4. **AsyncTCP** (by me-no-dev)

---

## 3. Flashing the Firmware

1. Open `esp32_firmware/esp32_the_day.ino` in Arduino IDE.
2. Update Wi-Fi credentials:
   ```cpp
   const char* WIFI_SSID = "Your_WiFi_Network";
   const char* WIFI_PASS = "Your_WiFi_Password";
   ```
3. Select your ESP32 board in **Tools** → **Board** (e.g. *ESP32 Dev Module*).
4. Select the COM Port.
5. Click **Upload**.

---

## 4. Verifying Serial Output

Open **Serial Monitor** at **115200 baud**. Once booted, the ESP32 will print:

```text
=================================
  THE DAY — ESP32 Secondary Display
=================================
Connecting to Wi-Fi: Your_WiFi_Network ...
......
WiFi connected
ESP32 IP: 192.168.1.105
WebSocket server active at ws://192.168.1.105/ws
```

---

## 5. Connecting with THE DAY App

1. Connect your Android phone to the **same Wi-Fi network**.
2. Open **DAY** on your phone.
3. Tap the compact status pill on the home screen or go to **Settings** → **Device** → **ESP32**.
4. Enter the ESP32 IP (`192.168.1.105`).
5. Tap **CONNECT**.
6. The status pill will pulse `○ CONNECTING` and settle into `● CONNECTED`.
7. Tap **TEST DISPLAY** to verify physical matrix illumination!
