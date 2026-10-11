# SMARTMOVE AI 🚗🛡️

> **"SmartMove AI sirf safe route nahi batata, balki sadak ki samasya theek karwane tak automated complaint follow-up karta hai."**

SmartMove AI is a next-generation dynamic road-safety intelligence and civic accountability platform. Unlike standard navigation tools that solely prioritize the shortest or fastest path, SmartMove AI balances travel efficiency with safety, road quality, and automated public grievance resolution.

---

## 🌟 Key Features

### 1. Smart Safe Route Engine (Personalized Risk Scoring)
- **Multi-Route Analysis**: Generates alternative routes with real-time risk scores (0–100 scale: Low, Medium, High).
- **Personalized Traveller Profiles**:
  - 🟢 **Healthy / Standard**: Default balanced weight distribution.
  - 🤰 **Pregnant / Fracture / Weak**: Zero-jolt routing (Vertical jerk & hazard weights $\times 2.5$).
  - 👶 **Family with Elderly / Children**: Prefers lower-speed corridors and well-lit roads ($\text{Time-of-day} \times 1.5$).
  - 🚑 **Ambulance / Emergency**: Prioritizes lowest congestion and fastest clearance corridor ($\text{Traffic} \times 2.5$).
- **Dynamic Risk Formula**:
  $$\text{Risk Score} = (Road \times w_1) + (Traffic \times w_2) + (Weather \times w_3) + (Hazards \times w_4) + (TimeOfDay \times w_5)$$

### 2. Dual Road Hazard Detection (No Hardware Required)
- **📱 Phone Accelerometer Crowdsensing**:
  - Leverages HTML5 `DeviceMotionEvent` / Android IMU sensors to monitor vertical shock spikes ($> 12\text{ m/s}^2$).
  - Speed-filtering ($> 15\text{ km/h}$) filters out pedestrian pocket drops.
- **📷 Camera Vision AI**:
  - In-browser computer vision HUD with real-time lane tracking and automated frame capture when hazards or severe jolts are detected.
- **📟 IoT Hardware / OBD-II Simulator**:
  - Ready for vehicle CAN-bus and ESP32 external sensor modules (`SmartMove.report({type, lat, lng})`).
- **👆 1-Tap Manual Reporting**:
  - Direct 1-tap reporting for Potholes, Broken roads, Waterlogging, and Missing streetlights.

### 3. Crowd Verification & Anti-Spam (Clustering)
- Hazards become **VERIFIED** only when **3 distinct devices** independently confirm a hazard within a **20-meter cluster radius**.
- Prevents false-positives and protects civic authority channels from spam.

### 4. Automated Civic Grievance Follow-Up System
- **Auto-Drafted Formal Grievance**: When 3 devices verify a pothole, an official notice is generated for the jurisdiction (NHAI, State PWD, Municipal Corporation, or Gram Panchayat).
- **Automated Escalation Timeline**:
  - **Day 0**: Formal grievance filed with exact GPS coordinates, Google Maps link, and verification count.
  - **Day 7**: First automated reminder if not resolved.
  - **Day 14**: Escalation to senior district magistrate / PWD portal.
- **Auto-Fix Verification (Clean Passes)**:
  - When 5 distinct devices pass through the exact hazard coordinate with zero vertical spikes ($< 4\text{ m/s}^2$), the hazard is automatically marked as **RESOLVED**.

### 5. Interactive Navigation Map & Multi-Theme UI
- **Real Google Maps Layers**: Instant switching between **Google Roads**, **Google Satellite (Hybrid)**, **Google Terrain**, and **OpenStreetMap**.
- **Interactive Multi-Theme Engine**:
  - ⚪ **Clean White** (Default Light Theme)
  - ⚫ **Dark / Night OLED**
  - 🟤 **Sand / Brown**
  - 🌾 **Skin / Beige**
  - 🌸 **Soft Pink**
  - 🔴 **Safety Red**

---

## 🛠️ Tech Stack

| Layer | Technology |
| --- | --- |
| **Frontend Web App** | Modern HTML5, Vanilla JavaScript (ES6+), Leaflet.js, Sora Font, CSS3 Custom Properties |
| **Android Application** | Kotlin, Jetpack Compose, Material 3, Android Location & Sensor API |
| **Backend API** | Node.js, Express.js, REST API with JSON |
| **Database** | MySQL 8.0 with spatial indexing (`hazards`, `complaints`, `sensor_events`, `clean_passes`, `authorities`) |
| **External APIs** | Google Maps Platform, Open-Meteo Weather API, OSRM Routing Engine |

---

## 🚀 Getting Started

### 1. Clone the Repository
```bash
git clone https://github.com/YOUR_USERNAME/SMARTMOVE.git
cd SMARTMOVE
```

### 2. Backend Setup
```bash
cd backend
npm install
```

Create a `.env` file inside `backend/`:
```env
PORT=5000
NODE_ENV=development
DB_HOST=localhost
DB_PORT=3306
DB_USER=root
DB_PASSWORD=your_password
DB_NAME=smartmove_ai
GOOGLE_MAPS_API_KEY=your_google_maps_key
```

Run database migrations:
```bash
node src/scripts/migrate.js
```

Start the backend server:
```bash
node src/server.js
```

### 3. Open Frontend
Open `index.html` directly in your browser or navigate to:
```
http://localhost:5000/mobile/index.html
```

---

## 📄 License
This project is licensed under the Apache 2.0 License.
