/*
  THE DAY — ESP8266 Secondary Matrix Display
  ESP8266 NodeMCU 1.0 (ESP-12E) + MAX7219 8x8 Matrix Display

  Hardware Wiring:
    MAX7219 DIN -> D7 (GPIO 13)
    MAX7219 CLK -> D5 (GPIO 14)
    MAX7219 CS  -> D8 (GPIO 15)
    MAX7219 VCC -> VIN (5V)
    MAX7219 GND -> GND

  Libraries Required:
    - WebSockets by Markus Sattler (v2.4.1+)
    - ArduinoJson (v6 or v7)
    - MD_MAX72xx by MajicDesigns
    - ESP8266WiFi, ESP8266WebServer
*/

#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>
#include <WebSocketsServer.h>
#include <ArduinoJson.h>
#include <MD_MAX72xx.h>
#include <SPI.h>
#include <time.h>

// ── 1. HARDWARE CONFIGURATION ─────────────────────────────────────────────────
#define HARDWARE_TYPE MD_MAX72XX::FC16_HW
#define MAX_DEVICES   1
#define CLK_PIN       D5 // GPIO 14
#define DATA_PIN      D7 // GPIO 13
#define CS_PIN        D8 // GPIO 15

MD_MAX72XX mx = MD_MAX72XX(HARDWARE_TYPE, DATA_PIN, CLK_PIN, CS_PIN, MAX_DEVICES);

// ── 2. NETWORK & SERVERS ──────────────────────────────────────────────────────
ESP8266WebServer server(80);
WebSocketsServer webSocket = WebSocketsServer(81);

// NTP Time: Asia/Kolkata (UTC +05:30 = 19800 seconds)
const long gmtOffset_sec = 19800;
const int daylightOffset_sec = 0;
const char* ntpServer = "pool.ntp.org";

// ── 3. OPERATIONAL MODES & URGENCY STATES ─────────────────────────────────────
enum DeviceMode {
  MODE_CLOCK,
  MODE_TASKS,
  MODE_ALERT,
  MODE_CUSTOM,
  MODE_TEST
};

enum TaskUrgency {
  URGENCY_NORMAL,
  URGENCY_APPROACHING,
  URGENCY_DUE_NOW,
  URGENCY_OVERDUE,
  URGENCY_IGNORED
};

DeviceMode currentMode = MODE_CLOCK;
TaskUrgency currentUrgency = URGENCY_NORMAL;

// State Variables
uint8_t customMatrixRows[8] = {0};
uint8_t userBrightness = 8;
uint8_t currentIntensity = 8;

String activeTaskTitle = "";
String activeTaskTime = "";
String scrollMessage = "";

// Clock State (Only update on minute change)
String currentDisplayedClock = "";
int lastDisplayMinute = -1;

// Marquee Scrolling State
int scrollCharIndex = 0;
int scrollColOffset = 0;
unsigned long lastScrollTime = 0;
unsigned long scrollPauseUntil = 0;

// Urgency Animation Timers
unsigned long lastUrgencyPulseTime = 0;
bool urgencyBlinkPhase = false;
int ignoredStrobeCounter = 0;

// ── 4. COMPACT 3x4 BITMAP FONT FOR 8x8 CLOCK ──────────────────────────────────
// 4 rows per digit, 3 bits wide (bits 0,1,2)
const uint8_t DIGITS_3x4[10][4] = {
  { 0b111, 0b101, 0b101, 0b111 }, // 0
  { 0b010, 0b110, 0b010, 0b111 }, // 1
  { 0b111, 0b001, 0b110, 0b111 }, // 2
  { 0b111, 0b011, 0b001, 0b111 }, // 3
  { 0b101, 0b101, 0b111, 0b001 }, // 4
  { 0b111, 0b110, 0b001, 0b111 }, // 5
  { 0b111, 0b100, 0b111, 0b111 }, // 6
  { 0b111, 0b001, 0b010, 0b010 }, // 7
  { 0b111, 0b111, 0b101, 0b111 }, // 8
  { 0b111, 0b111, 0b001, 0b111 }  // 9
};

// ── 5. STATUS RESPONSE BUILDER ────────────────────────────────────────────────
String getModeName() {
  switch (currentMode) {
    case MODE_CLOCK: return "CLOCK";
    case MODE_TASKS: return "TASKS";
    case MODE_ALERT: return "ALERT";
    case MODE_CUSTOM: return "CUSTOM";
    case MODE_TEST: return "TEST";
    default: return "CLOCK";
  }
}

String getUrgencyName() {
  switch (currentUrgency) {
    case URGENCY_NORMAL: return "NORMAL";
    case URGENCY_APPROACHING: return "APPROACHING";
    case URGENCY_DUE_NOW: return "DUE_NOW";
    case URGENCY_OVERDUE: return "OVERDUE";
    case URGENCY_IGNORED: return "IGNORED";
    default: return "NORMAL";
  }
}

// Formats 24-hour hour (0-23) and minute (0-59) to "h:mm AM/PM" with NO leading zero for hours.
String format12HourTime(int hour, int minute, bool includeColon = true) {
  int h12 = hour % 12;
  if (h12 == 0) h12 = 12;
  const char* ampm = (hour < 12) ? "AM" : "PM";
  char buf[16];
  if (includeColon) {
    snprintf(buf, sizeof(buf), "%d:%02d %s", h12, minute, ampm);
  } else {
    snprintf(buf, sizeof(buf), "%d %02d %s", h12, minute, ampm);
  }
  return String(buf);
}

String buildStatusJson() {
  time_t now = time(nullptr);
  struct tm* t = localtime(&now);
  bool synced = (t->tm_year >= (2025 - 1900));

  char timeBuf[16];
  if (synced) {
    String formatted = format12HourTime(t->tm_hour, t->tm_min, true);
    snprintf(timeBuf, sizeof(timeBuf), "%s", formatted.c_str());
  } else {
    snprintf(timeBuf, sizeof(timeBuf), "--:--");
  }

  StaticJsonDocument<384> doc;
  doc["type"] = "status";
  doc["device"] = "ESP8266";
  doc["connected"] = true;
  doc["wifi"] = (WiFi.status() == WL_CONNECTED);
  doc["ip"] = WiFi.localIP().toString();
  doc["mode"] = getModeName();
  doc["time"] = timeBuf;
  doc["time_synced"] = synced;
  doc["task"] = activeTaskTitle;
  doc["task_time"] = activeTaskTime;
  doc["brightness"] = userBrightness;
  doc["urgency"] = getUrgencyName();

  String output;
  serializeJson(doc, output);
  return output;
}

// ── 6. CENTRAL ORIENTATION MAPPING & MATRIX RENDERING ─────────────────────────

// Central physical-to-logical orientation mapping (Requirement 4)
void logicalPixelToPhysical(int logicalRow, int logicalCol, int &physRow, int &physCol) {
  // Logical coordinate system:
  // Row 0 = Top, Row 7 = Bottom
  // Col 0 = Left, Col 7 = Right
  physRow = logicalRow;
  physCol = logicalCol;
}

// Single function responsible for rendering custom bitmaps (Requirement 6)
void applyMatrix(uint8_t rows[8]) {
  mx.clear();
  for (int r = 0; r < 8; r++) {
    uint8_t rowByte = rows[r];
    for (int c = 0; c < 8; c++) {
      bool isLit = (rowByte >> c) & 1;
      int pr, pc;
      logicalPixelToPhysical(r, c, pr, pc);
      mx.setPoint(pr, pc, isLit);
    }
  }
  mx.update();
  Serial.println("MATRIX APPLIED");
}

void setupMarquee(String message) {
  scrollMessage = message + "   ";
  scrollCharIndex = 0;
  scrollColOffset = 0;
  scrollPauseUntil = millis() + 400;
  mx.clear();
}

void tickMarqueeScrolling() {
  unsigned long now = millis();
  if (now < scrollPauseUntil) return;
  if (now - lastScrollTime < 110) return; // Medium-slow readable scroll speed
  lastScrollTime = now;

  mx.transform(MD_MAX72XX::TSL);

  if (scrollCharIndex < (int)scrollMessage.length()) {
    char ch = scrollMessage.charAt(scrollCharIndex);
    uint8_t charCols[8] = {0};
    uint8_t charLen = mx.getChar(ch, sizeof(charCols), charCols);

    if (scrollColOffset < charLen) {
      uint8_t colData = charCols[scrollColOffset];
      for (int r = 0; r < 8; r++) {
        mx.setPoint(r, 7, (colData >> r) & 1);
      }
      scrollColOffset++;
    } else {
      // 1-column gap between characters
      for (int r = 0; r < 8; r++) {
        mx.setPoint(r, 7, false);
      }
      scrollCharIndex++;
      scrollColOffset = 0;
    }
  } else {
    // Reached end: brief pause then restart
    scrollCharIndex = 0;
    scrollColOffset = 0;
    scrollPauseUntil = now + 1200;
  }
}

void tickUrgencyAnimation() {
  unsigned long now = millis();

  switch (currentUrgency) {
    case URGENCY_NORMAL:
      if (currentIntensity != userBrightness) {
        currentIntensity = userBrightness;
        mx.control(MD_MAX72XX::INTENSITY, currentIntensity);
      }
      break;

    case URGENCY_APPROACHING:
      // Subtle pulse every 2 seconds
      if (now - lastUrgencyPulseTime > 2000) {
        lastUrgencyPulseTime = now;
        urgencyBlinkPhase = !urgencyBlinkPhase;
        currentIntensity = urgencyBlinkPhase ? min(15, userBrightness + 4) : userBrightness;
        mx.control(MD_MAX72XX::INTENSITY, currentIntensity);
      }
      break;

    case URGENCY_DUE_NOW:
      // Noticeable pulse (100% -> 40% -> 100%) every 700ms
      if (now - lastUrgencyPulseTime > 700) {
        lastUrgencyPulseTime = now;
        urgencyBlinkPhase = !urgencyBlinkPhase;
        currentIntensity = urgencyBlinkPhase ? userBrightness : max(1, userBrightness / 3);
        mx.control(MD_MAX72XX::INTENSITY, currentIntensity);
      }
      break;

    case URGENCY_OVERDUE:
      // Stronger repeating brightness variation every 450ms
      if (now - lastUrgencyPulseTime > 450) {
        lastUrgencyPulseTime = now;
        urgencyBlinkPhase = !urgencyBlinkPhase;
        currentIntensity = urgencyBlinkPhase ? 15 : max(1, userBrightness / 4);
        mx.control(MD_MAX72XX::INTENSITY, currentIntensity);
      }
      break;

    case URGENCY_IGNORED:
      // ON-OFF strobe attention pulse
      if (now - lastUrgencyPulseTime > 300) {
        lastUrgencyPulseTime = now;
        urgencyBlinkPhase = !urgencyBlinkPhase;
        mx.control(MD_MAX72XX::INTENSITY, urgencyBlinkPhase ? 15 : 0);
      }
      break;
  }
}

void runTestSequence() {
  currentMode = MODE_TEST;

  // 1. Border
  mx.clear();
  for (int i = 0; i < 8; i++) {
    mx.setPoint(0, i, true);
    mx.setPoint(7, i, true);
    mx.setPoint(i, 0, true);
    mx.setPoint(i, 7, true);
  }
  delay(300);

  // 2. X Pattern
  mx.clear();
  for (int i = 0; i < 8; i++) {
    mx.setPoint(i, i, true);
    mx.setPoint(i, 7 - i, true);
  }
  delay(300);

  // 3. All pixels ON
  for (int r = 0; r < 8; r++) {
    for (int c = 0; c < 8; c++) {
      mx.setPoint(r, c, true);
    }
  }
  delay(400);

  // 4. Clear and return to Clock
  mx.clear();
  setClockMode();
}

// ── 7. WEBSOCKET MESSAGE HANDLER ──────────────────────────────────────────────

void handleWebSocketMessage(uint8_t num, uint8_t* payload, size_t length) {
  StaticJsonDocument<512> doc;
  DeserializationError err = deserializeJson(doc, payload, length);
  if (err) {
    Serial.println("JSON parse error");
    return;
  }

  const char* type = doc["type"] | "";
  Serial.print("MESSAGE FROM THE DAY: ");
  Serial.println(type);

  if (strcmp(type, "status") == 0) {
    String resp = buildStatusJson();
    webSocket.sendTXT(num, resp);
  }
  else if (strcmp(type, "ping") == 0) {
    StaticJsonDocument<128> pongDoc;
    pongDoc["type"] = "pong";
    pongDoc["time"] = millis();
    String pongStr;
    serializeJson(pongDoc, pongStr);
    webSocket.sendTXT(num, pongStr);
  }
  else if (strcmp(type, "matrix") == 0) {
    JsonArray rows = doc["rows"];
    if (rows.size() == 8) {
      Serial.println("MATRIX RECEIVED");
      Serial.print("rows:\n");
      for (int i = 0; i < 8; i++) {
        customMatrixRows[i] = rows[i].as<uint8_t>();
        Serial.print(customMatrixRows[i]);
        Serial.print(" ");
      }
      Serial.println();

      currentMode = MODE_CUSTOM;
      Serial.println("MODE CHANGED: CUSTOM");
      applyMatrix(customMatrixRows);

      // Respond with matrix_ack (Requirement 14 & 42)
      StaticJsonDocument<256> ackDoc;
      ackDoc["type"] = "matrix_ack";
      ackDoc["success"] = true;
      JsonArray ackRows = ackDoc.createNestedArray("rows");
      for (int i = 0; i < 8; i++) {
        ackRows.add(customMatrixRows[i]);
      }
      String ackStr;
      serializeJson(ackDoc, ackStr);
      webSocket.sendTXT(num, ackStr);
    }
  }
  else if (strcmp(type, "brightness") == 0) {
    int val = doc["value"] | 8;
    userBrightness = constrain(val, 0, 15);
    currentIntensity = userBrightness;
    mx.control(MD_MAX72XX::INTENSITY, currentIntensity);
    webSocket.sendTXT(num, buildStatusJson());
  }
  else if (strcmp(type, "clock") == 0) {
    setClockMode();
    webSocket.sendTXT(num, buildStatusJson());
  }
  else if (strcmp(type, "clock_sync") == 0) {
    configTime(gmtOffset_sec, daylightOffset_sec, ntpServer);
    setClockMode();
    webSocket.sendTXT(num, buildStatusJson());
  }
  else if (strcmp(type, "clear") == 0) {
    activeTaskTitle = "";
    activeTaskTime = "";
    currentUrgency = URGENCY_NORMAL;
    currentIntensity = userBrightness;
    mx.control(MD_MAX72XX::INTENSITY, currentIntensity);
    setClockMode();
    webSocket.sendTXT(num, buildStatusJson());
  }
  else if (strcmp(type, "task") == 0) {
    activeTaskTitle = doc["title"] | "";
    if (doc.containsKey("hour")) {
      int h = doc["hour"];
      int m = doc["minute"] | 0;
      activeTaskTime = format12HourTime(h, m, true);
    } else {
      activeTaskTime = doc["time"] | "";
    }
    const char* urgStr = doc["urgency"] | "NORMAL";

    if (strcmp(urgStr, "APPROACHING") == 0) currentUrgency = URGENCY_APPROACHING;
    else if (strcmp(urgStr, "DUE_NOW") == 0) currentUrgency = URGENCY_DUE_NOW;
    else if (strcmp(urgStr, "OVERDUE") == 0) currentUrgency = URGENCY_OVERDUE;
    else if (strcmp(urgStr, "IGNORED") == 0) currentUrgency = URGENCY_IGNORED;
    else currentUrgency = URGENCY_NORMAL;

    currentMode = MODE_TASKS;
    Serial.println("MODE CHANGED: TASKS");
    String msg = activeTaskTitle;
    if (activeTaskTime.length() > 0) msg += " " + activeTaskTime;
    Serial.print("TASK CHANGED: ");
    Serial.println(msg);
    setupMarquee(msg);
    webSocket.sendTXT(num, buildStatusJson());
  }
  else if (strcmp(type, "alert") == 0) {
    activeTaskTitle = doc["title"] | "";
    if (doc.containsKey("hour")) {
      int h = doc["hour"];
      int m = doc["minute"] | 0;
      activeTaskTime = format12HourTime(h, m, true);
    } else {
      activeTaskTime = doc["time"] | "";
    }
    const char* urgStr = doc["urgency"] | "DUE_NOW";

    if (strcmp(urgStr, "OVERDUE") == 0) currentUrgency = URGENCY_OVERDUE;
    else if (strcmp(urgStr, "IGNORED") == 0) currentUrgency = URGENCY_IGNORED;
    else currentUrgency = URGENCY_DUE_NOW;

    currentMode = MODE_ALERT;
    Serial.println("MODE CHANGED: ALERT");
    String alertMsg = "! " + activeTaskTitle;
    if (activeTaskTime.length() > 0) alertMsg += " " + activeTaskTime;
    Serial.print("ALERT CHANGED: ");
    Serial.println(alertMsg);
    setupMarquee(alertMsg);
    webSocket.sendTXT(num, buildStatusJson());
  }
  else if (strcmp(type, "urgency") == 0) {
    const char* urgStr = doc["urgency"] | "NORMAL";
    if (strcmp(urgStr, "APPROACHING") == 0) currentUrgency = URGENCY_APPROACHING;
    else if (strcmp(urgStr, "DUE_NOW") == 0) currentUrgency = URGENCY_DUE_NOW;
    else if (strcmp(urgStr, "OVERDUE") == 0) currentUrgency = URGENCY_OVERDUE;
    else if (strcmp(urgStr, "IGNORED") == 0) currentUrgency = URGENCY_IGNORED;
    else currentUrgency = URGENCY_NORMAL;
    webSocket.sendTXT(num, buildStatusJson());
  }
  else if (strcmp(type, "test") == 0) {
    runTestSequence();
    webSocket.sendTXT(num, buildStatusJson());
  }
}

void webSocketEvent(uint8_t num, WStype_t type, uint8_t* payload, size_t length) {
  switch (type) {
    case WStype_DISCONNECTED:
      Serial.printf("[%u] Disconnected!\n", num);
      break;
    case WStype_CONNECTED: {
      IPAddress ip = webSocket.remoteIP(num);
      Serial.printf("[%u] THE DAY CLIENT CONNECTED from %d.%d.%d.%d\n", num, ip[0], ip[1], ip[2], ip[3]);
      webSocket.sendTXT(num, buildStatusJson());
      break;
    }
    case WStype_TEXT:
      handleWebSocketMessage(num, payload, length);
      break;
    default:
      break;
  }
}

// ── 8. HTTP STATUS PAGE ───────────────────────────────────────────────────────

void handleHttpRoot() {
  String html = "<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width, initial-scale=1'>";
  html += "<title>THE DAY - ESP8266</title><style>";
  html += "body{background:#000;color:#fff;font-family:sans-serif;padding:20px;text-align:center}";
  html += ".card{background:#111;border:1px solid #222;border-radius:12px;padding:20px;display:inline-block;max-width:320px}";
  html += "h1{font-size:20px;letter-spacing:1px;margin:0 0 15px}p{margin:8px 0;color:#aaa}";
  html += ".val{color:#fff;font-weight:bold}</style></head><body>";
  html += "<div class='card'><h1>THE DAY</h1><p>Device: <span class='val'>ESP8266 MAX7219</span></p>";
  html += "<p>Mode: <span class='val'>" + getModeName() + "</span></p>";
  html += "<p>IP: <span class='val'>" + WiFi.localIP().toString() + "</span></p>";
  html += "<p>WebSocket Port: <span class='val'>81</span></p>";
  html += "<p>Status: <span class='val' style='color:#10b981'>ONLINE</span></p></div></body></html>";
  server.send(200, "text/html", html);
}

// ── 9. SETUP & MAIN LOOP ──────────────────────────────────────────────────────

void setClockMode() {
  currentMode = MODE_CLOCK;
  Serial.println("MODE CHANGED: CLOCK");
  time_t now = time(nullptr);
  struct tm* t = localtime(&now);
  bool synced = (t->tm_year >= (2025 - 1900));
  lastDisplayMinute = synced ? t->tm_min : -1;
  currentDisplayedClock = synced ? format12HourTime(t->tm_hour, t->tm_min, true) : "--:--";
  Serial.print("CLOCK INITIALIZED: ");
  Serial.println(currentDisplayedClock);
  setupMarquee(currentDisplayedClock);
}

void updateDisplayState() {
  if (currentMode == MODE_CLOCK) {
    time_t now = time(nullptr);
    struct tm* t = localtime(&now);
    bool synced = (t->tm_year >= (2025 - 1900));
    int minute = synced ? t->tm_min : -1;

    // Only update displayed clock message when the actual minute changes!
    if (minute != lastDisplayMinute || currentDisplayedClock.length() == 0) {
      lastDisplayMinute = minute;
      currentDisplayedClock = synced ? format12HourTime(t->tm_hour, t->tm_min, true) : "--:--";
      Serial.print("CLOCK CHANGED: ");
      Serial.println(currentDisplayedClock);
      setupMarquee(currentDisplayedClock);
    }
  }
}

void animateDisplay() {
  switch (currentMode) {
    case MODE_CLOCK:
      tickMarqueeScrolling();
      break;

    case MODE_TASKS:
      tickMarqueeScrolling();
      tickUrgencyAnimation();
      break;

    case MODE_ALERT:
      tickMarqueeScrolling();
      tickUrgencyAnimation();
      break;

    case MODE_CUSTOM:
      // Direct pixel matrix: rendered immediately on update, static
      break;

    case MODE_TEST:
      break;
  }
}

void setup() {
  Serial.begin(115200);
  Serial.println("\nTHE DAY ESP8266 Initializing...");

  mx.begin();
  mx.control(MD_MAX72XX::INTENSITY, userBrightness);
  mx.clear();

  // NTP Time Configuration
  configTime(gmtOffset_sec, daylightOffset_sec, ntpServer);

  // Web Server
  server.on("/", HTTP_GET, handleHttpRoot);
  server.begin();

  // WebSocket Server on Port 81
  webSocket.begin();
  webSocket.onEvent(webSocketEvent);

  Serial.println("WebSocket server started on port 81");
  setClockMode();
}

void loop() {
  webSocket.loop();
  server.handleClient();

  updateDisplayState();
  animateDisplay();
}
