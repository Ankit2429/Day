/*
  THE DAY — ESP32 Realtime WebSocket Secondary Display
  ESP32 + MAX7219 8x8 / FC-16 LED Matrix Display

  Architecture:
    ANDROID PHONE (THE DAY) -> Wi-Fi -> ESP32 (WebSocket ws://<IP>/ws) -> MAX7219

  ESP32 Default SPI Pins for MAX7219:
    MAX7219 VCC -> ESP32 5V (VIN)
    MAX7219 GND -> ESP32 GND
    MAX7219 DIN -> GPIO 23 (VSPI MOSI)
    MAX7219 CS  -> GPIO 5  (VSPI SS)
    MAX7219 CLK -> GPIO 18 (VSPI SCK)

  Libraries Required:
    - ESPAsyncWebServer (or WebSockets by Markus Sattler)
    - AsyncTCP (by me-no-dev)
    - ArduinoJson (v6 or v7)
    - MD_MAX72xx (by MajicDesigns)
*/

#include <WiFi.h>
#include <AsyncTCP.h>
#include <ESPAsyncWebServer.h>
#include <ArduinoJson.h>
#include <MD_MAX72xx.h>
#include <SPI.h>
#include <time.h>

// ── 1. NETWORK CONFIGURATION ───────────────────────────────────────────────────
// For initial testing, specify your local Wi-Fi credentials here:
const char* WIFI_SSID = "YOUR_WIFI_SSID";
const char* WIFI_PASS = "YOUR_WIFI_PASSWORD";

// ── 2. HARDWARE MATRIX CONFIGURATION ──────────────────────────────────────────
#define HARDWARE_TYPE MD_MAX72XX::FC16_HW
#define MAX_DEVICES 1
#define CLK_PIN   18 // VSPI SCK
#define DATA_PIN  23 // VSPI MOSI
#define CS_PIN    5  // VSPI SS

MD_MAX72XX mx = MD_MAX72XX(HARDWARE_TYPE, DATA_PIN, CLK_PIN, CS_PIN, MAX_DEVICES);

// ── 3. ASYNC WEBSOCKET & HTTP SERVER ──────────────────────────────────────────
AsyncWebServer server(80);
AsyncWebSocket ws("/ws");

// Display Modes
enum DisplayMode {
  MODE_CLOCK,
  MODE_TASKS,
  MODE_ALERT,
  MODE_CUSTOM,
  MODE_TEST,
  MODE_CLEAR
};

DisplayMode currentMode = MODE_CLOCK;
DisplayMode previousMode = MODE_CLOCK;

// Operational States
String currentTaskTitle = "Study";
String currentTaskTime = "18:30";
String currentCustomText = "THE DAY";
uint8_t rowBuffer[8] = {0};
unsigned long modeTimer = 0;
bool alertBlinkState = false;
unsigned long lastBlinkTime = 0;

// NTP Time Configuration
const long gmtOffset_sec = 19800; // Asia/Kolkata UTC+5:30 (adjust if required)
const int daylightOffset_sec = 0;
const char* ntpServer = "pool.ntp.org";

// ── PROTOCOL SERIALIZATION HELPERS ────────────────────────────────────────────

String getModeString() {
  switch (currentMode) {
    case MODE_CLOCK: return "CLOCK";
    case MODE_TASKS: return "TASKS";
    case MODE_ALERT: return "ALERT";
    case MODE_CUSTOM: return "CUSTOM";
    case MODE_TEST: return "TEST";
    case MODE_CLEAR: return "CLEAR";
    default: return "CLOCK";
  }
}

void sendStatus(AsyncWebSocketClient* client = nullptr) {
  StaticJsonDocument<256> doc;
  doc["type"] = "status";
  doc["device"] = "ESP32";
  doc["connected"] = true;
  doc["wifi"] = (WiFi.status() == WL_CONNECTED);
  doc["ip"] = WiFi.localIP().toString();
  doc["mode"] = getModeString();
  doc["uptime"] = millis() / 1000;
  doc["freeHeap"] = ESP.getFreeHeap();

  String response;
  serializeJson(doc, response);

  if (client != nullptr) {
    client->text(response);
  } else {
    ws.textAll(response);
  }
}

// ── MAX7219 DRAWING ROUTINES ──────────────────────────────────────────────────

void drawClock() {
  time_t now = time(nullptr);
  struct tm* timeinfo = localtime(&now);

  int rawHour = (timeinfo->tm_year > 100) ? timeinfo->tm_hour : 12;
  int hour12 = rawHour % 12;
  if (hour12 == 0) hour12 = 12;
  int minute = (timeinfo->tm_year > 100) ? timeinfo->tm_min : 0;
  int second = (timeinfo->tm_year > 100) ? timeinfo->tm_sec : 0;

  mx.clear();

  // Minimalist 8x8 Clock Visualization:
  // Top 2 rows: Hour bar (1-12 mapped to 1-8 LEDs)
  int hourLeds = map(hour12, 1, 12, 1, 8);
  for (int col = 0; col < hourLeds; col++) {
    mx.setPoint(0, col, true);
    mx.setPoint(1, col, true);
  }

  // Rows 3-4: Minute bar (0-59 mapped to 1-8 LEDs)
  int minuteLeds = map(minute, 0, 59, 1, 8);
  for (int col = 0; col < minuteLeds; col++) {
    mx.setPoint(3, col, true);
    mx.setPoint(4, col, true);
  }

  // Rows 6-7: Center pulsing second pulse
  bool blink = (second % 2 == 0);
  mx.setPoint(6, 3, blink);
  mx.setPoint(6, 4, blink);
  mx.setPoint(7, 3, blink);
  mx.setPoint(7, 4, blink);
}

void drawTaskDisplay() {
  mx.clear();
  // Display task priority bitmask if available, otherwise stylized T pattern
  bool hasMask = false;
  for (int r = 0; r < 8; r++) {
    if (rowBuffer[r] > 0) hasMask = true;
  }

  if (hasMask) {
    for (int row = 0; row < 8; row++) {
      uint8_t mask = rowBuffer[row];
      for (int col = 0; col < 8; col++) {
        if ((mask >> col) & 1) {
          mx.setPoint(row, col, true);
        }
      }
    }
  } else {
    // Stylized "T" badge
    for (int c = 1; c < 7; c++) mx.setPoint(1, c, true);
    for (int r = 2; r < 7; r++) {
      mx.setPoint(r, 3, true);
      mx.setPoint(r, 4, true);
    }
  }
}

void drawAlert() {
  if (millis() - lastBlinkTime > 300) {
    lastBlinkTime = millis();
    alertBlinkState = !alertBlinkState;
  }

  mx.clear();
  if (alertBlinkState) {
    // Outer border
    for (int i = 0; i < 8; i++) {
      mx.setPoint(0, i, true);
      mx.setPoint(7, i, true);
      mx.setPoint(i, 0, true);
      mx.setPoint(i, 7, true);
    }
    // Exclamation point in center
    mx.setPoint(2, 3, true); mx.setPoint(2, 4, true);
    mx.setPoint(3, 3, true); mx.setPoint(3, 4, true);
    mx.setPoint(4, 3, true); mx.setPoint(4, 4, true);
    mx.setPoint(6, 3, true); mx.setPoint(6, 4, true);
  }
}

void drawTestPattern() {
  // Checkerboard test pattern
  mx.clear();
  for (int r = 0; r < 8; r++) {
    for (int c = 0; c < 8; c++) {
      mx.setPoint(r, c, (r + c) % 2 == 0);
    }
  }
}

// ── WEBSOCKET MESSAGE DISPATCHER ──────────────────────────────────────────────

void handleWebSocketMessage(AsyncWebSocketClient* client, void* arg, uint8_t* data, size_t len) {
  AwsFrameInfo* info = (AwsFrameInfo*)arg;
  if (info->final && info->index == 0 && info->len == len && info->opcode == WS_TEXT) {
    data[len] = 0;
    String message = (char*)data;
    Serial.printf("[WS] Received: %s\n", message.c_str());

    StaticJsonDocument<512> doc;
    DeserializationError error = deserializeJson(doc, message);
    if (error) {
      Serial.printf("[WS] JSON parse error: %s\n", error.c_str());
      return;
    }

    const char* type = doc["type"] | "";

    // 1. PING -> PONG
    if (strcmp(type, "ping") == 0) {
      client->text("{\"type\":\"pong\"}");
      return;
    }

    // 2. STATUS
    if (strcmp(type, "status") == 0) {
      sendStatus(client);
      return;
    }

    // 3. TASK
    if (strcmp(type, "task") == 0) {
      currentMode = MODE_TASKS;
      currentTaskTitle = doc["title"] | "Task";
      currentTaskTime = doc["time"] | "18:30";
      Serial.printf("[WS] Task Set: %s at %s\n", currentTaskTitle.c_str(), currentTaskTime.c_str());
      sendStatus();
      return;
    }

    // 4. CLOCK / CLOCK SYNC
    if (strcmp(type, "clock") == 0 || strcmp(type, "clock_sync") == 0) {
      currentMode = MODE_CLOCK;
      Serial.println("[WS] Switched to CLOCK mode");
      sendStatus();
      return;
    }

    // 5. ALERT
    if (strcmp(type, "alert") == 0) {
      previousMode = currentMode;
      currentMode = MODE_ALERT;
      currentTaskTitle = doc["title"] | "Alert";
      currentTaskTime = doc["time"] | "";
      alertBlinkState = true;
      lastBlinkTime = millis();
      Serial.printf("[WS] ALERT TRIGGERED: %s\n", currentTaskTitle.c_str());
      sendStatus();
      return;
    }

    // 6. CLEAR
    if (strcmp(type, "clear") == 0) {
      currentMode = MODE_CLOCK;
      mx.clear();
      Serial.println("[WS] Display cleared -> returning to CLOCK");
      sendStatus();
      return;
    }

    // 7. CUSTOM
    if (strcmp(type, "custom") == 0) {
      currentMode = MODE_CUSTOM;
      currentCustomText = doc["text"] | "HELLO";
      Serial.printf("[WS] Custom Text: %s\n", currentCustomText.c_str());
      sendStatus();
      return;
    }

    // 8. TEST
    if (strcmp(type, "test") == 0) {
      previousMode = currentMode;
      currentMode = MODE_TEST;
      modeTimer = millis();
      Serial.println("[WS] Running hardware matrix test pattern");
      sendStatus();
      return;
    }
  }
}

void onWebSocketEvent(AsyncWebSocket* server, AsyncWebSocketClient* client,
                    AwsEventType type, void* arg, uint8_t* data, size_t len) {
  switch (type) {
    case WS_EVT_CONNECT:
      Serial.printf("[WS] Client #%u connected from %s\n", client->id(), client->remoteIP().toString().c_str());
      sendStatus(client);
      break;
    case WS_EVT_DISCONNECT:
      Serial.printf("[WS] Client #%u disconnected\n", client->id());
      break;
    case WS_EVT_DATA:
      handleWebSocketMessage(client, arg, data, len);
      break;
    case WS_EVT_PONG:
    case WS_EVT_ERROR:
      break;
  }
}

// ── SETUP & LOOP ──────────────────────────────────────────────────────────────

void setup() {
  Serial.begin(115200);
  delay(500);
  Serial.println("\n=================================");
  Serial.println("  THE DAY — ESP32 Secondary Display");
  Serial.println("=================================");

  // Initialize MAX7219 Hardware Matrix
  mx.begin();
  mx.control(MD_MAX72XX::INTENSITY, 8);
  mx.clear();

  // Connect Wi-Fi
  Serial.printf("Connecting to Wi-Fi: %s ...\n", WIFI_SSID);
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASS);

  int retries = 0;
  while (WiFi.status() != WL_CONNECTED && retries < 30) {
    delay(500);
    Serial.print(".");
    retries++;
  }
  Serial.println();

  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("WiFi connected");
    Serial.print("ESP32 IP: ");
    Serial.println(WiFi.localIP());
  } else {
    Serial.println("Wi-Fi connection timed out. Waiting in background...");
  }

  // NTP Time Synchronization
  configTime(gmtOffset_sec, daylightOffset_sec, ntpServer);

  // Attach WebSocket Server
  ws.onEvent(onWebSocketEvent);
  server.addHandler(&ws);

  // HTTP Fallback Endpoints
  server.on("/status", HTTP_GET, [](AsyncWebServerRequest* request) {
    StaticJsonDocument<256> doc;
    doc["type"] = "status";
    doc["device"] = "ESP32";
    doc["connected"] = true;
    doc["wifi"] = (WiFi.status() == WL_CONNECTED);
    doc["ip"] = WiFi.localIP().toString();
    doc["mode"] = getModeString();
    doc["uptime"] = millis() / 1000;
    String res;
    serializeJson(doc, res);
    request->send(200, "application/json", res);
  });

  server.on("/clear", HTTP_POST, [](AsyncWebServerRequest* request) {
    currentMode = MODE_CLOCK;
    mx.clear();
    request->send(200, "application/json", "{\"result\":\"cleared\"}");
  });

  server.begin();
  Serial.printf("WebSocket server active at ws://%s/ws\n", WiFi.localIP().toString().c_str());
}

void loop() {
  ws.cleanupClients();

  // Auto-reconnect Wi-Fi if connection dropped
  if (WiFi.status() != WL_CONNECTED) {
    static unsigned long lastReconnectAttempt = 0;
    if (millis() - lastReconnectAttempt > 10000) {
      lastReconnectAttempt = millis();
      Serial.println("Wi-Fi disconnected. Reconnecting...");
      WiFi.reconnect();
    }
  }

  // Matrix Rendering by Mode
  switch (currentMode) {
    case MODE_CLOCK:
      drawClock();
      delay(500);
      break;

    case MODE_TASKS:
      drawTaskDisplay();
      delay(300);
      break;

    case MODE_ALERT:
      drawAlert();
      delay(50);
      break;

    case MODE_TEST:
      drawTestPattern();
      if (millis() - modeTimer > 2500) {
        currentMode = previousMode;
      }
      delay(100);
      break;

    case MODE_CLEAR:
      mx.clear();
      delay(500);
      break;

    default:
      drawClock();
      delay(500);
      break;
  }
}
