import { getRouteRisk, hazardService, weatherService, civicGrievanceService, liveRouteService } from './mock-services.js';
import { PASSENGER_PROFILES } from './risk-engine.js';

// Device UUID persistence (Zero PII, anonymous device fingerprint)
let deviceId = localStorage.getItem('smartmove_device_id');
if (!deviceId) {
  deviceId = 'dev_' + (crypto.randomUUID ? crypto.randomUUID().slice(0, 12) : Math.random().toString(36).substring(2, 12));
  localStorage.setItem('smartmove_device_id', deviceId);
}

// Theme Management
const savedTheme = localStorage.getItem('smartmove_theme') || 'white';
document.documentElement.setAttribute('data-theme', savedTheme);

const app = {
  currentRoute: 'safest',
  currentProfile: 'HEALTHY',
  selectedHazard: 'Pothole',
  userLocation: { latitude: 18.5204, longitude: 73.8567 }, // Pune center
  destinationLocation: { latitude: 18.5167, longitude: 73.8780 }, // MG Road / Residency Road
  currentSpeed: 0, // m/s
  lastSpikeTime: 0,
  emergencyPhone: localStorage.getItem('smartmove_emergency_phone') || '+91 9876543210',
  crashTimer: null,
  crashSecondsRemaining: 15,
  leafletMap: null,
  routePolylines: {},
  hazardMarkers: [],
  cameraStream: null,
  cameraAnimId: null,
  isCameraActive: false,
};

const routeInfo = {
  safest: { label: 'Safest', duration: '29 min', distance: '8.1 km', detail: 'Lowest current risk', color: '#16a34a' },
  fastest: { label: 'Fastest', duration: '24 min', distance: '7.2 km', detail: 'Via Residency Road', color: '#ea580c' },
  balanced: { label: 'Balanced', duration: '27 min', distance: '7.7 km', detail: 'Best all-round option', color: '#2563eb' },
};

// Route Coordinates (Pune Navigation Network)
const routeWaypoints = {
  safest: [
    [18.5204, 73.8567],
    [18.5240, 73.8610],
    [18.5280, 73.8670],
    [18.5250, 73.8730],
    [18.5190, 73.8760],
    [18.5167, 73.8780]
  ],
  fastest: [
    [18.5204, 73.8567],
    [18.5215, 73.8640],
    [18.5200, 73.8710],
    [18.5167, 73.8780]
  ],
  balanced: [
    [18.5204, 73.8567],
    [18.5170, 73.8620],
    [18.5150, 73.8690],
    [18.5167, 73.8780]
  ]
};

const $ = (selector, scope = document) => scope.querySelector(selector);
const $$ = (selector, scope = document) => [...scope.querySelectorAll(selector)];

function titleCase(value) { return value.charAt(0).toUpperCase() + value.slice(1); }

function showScreen(name) {
  $$('.screen').forEach((screen) => screen.classList.toggle('active', screen.dataset.screen === name));
  $$('.nav-item').forEach((item) => {
    const selected = item.dataset.nav === name;
    item.classList.toggle('active', selected);
    item.toggleAttribute('aria-current', selected);
  });
  if (name === 'civic') {
    loadCivicDashboard();
  }
  if (name === 'trips' && app.leafletMap) {
    setTimeout(() => {
      app.leafletMap.invalidateSize();
    }, 200);
  }
  $('#app').scrollTo({ top: 0, behavior: 'smooth' });
}

function showToast(message) {
  const toast = $('#toast');
  if (!toast) return;
  toast.textContent = message;
  toast.classList.add('visible');
  clearTimeout(showToast.timer);
  showToast.timer = setTimeout(() => toast.classList.remove('visible'), 3600);
}

function formatRisk(risk) { return `${titleCase(risk.level)} risk (${risk.score}/100)`; }

// --------------------------------------------------
// Theme Switcher Engine
// --------------------------------------------------
function setupThemeSwitcher() {
  $$('.theme-dot').forEach((dot) => {
    dot.addEventListener('click', () => {
      const theme = dot.dataset.theme;
      if (!theme) return;
      document.documentElement.setAttribute('data-theme', theme);
      localStorage.setItem('smartmove_theme', theme);
      $$('.theme-dot').forEach(d => d.style.borderColor = 'transparent');
      dot.style.borderColor = 'var(--primary)';
      showToast(`Applied ${titleCase(theme)} theme`);
    });
  });

  const activeDot = $(`.theme-dot[data-theme="${savedTheme}"]`);
  if (activeDot) activeDot.style.borderColor = 'var(--primary)';
}

// --------------------------------------------------
// Leaflet Interactive Navigation Map
// --------------------------------------------------
async function initInteractiveMap() {
  const mapContainer = document.getElementById('live-map');
  if (!mapContainer || typeof L === 'undefined') return;

  if (app.leafletMap) {
    app.leafletMap.invalidateSize();
    return;
  }

  // Initialize Leaflet map
  app.leafletMap = L.map('live-map', {
    zoomControl: true,
    attributionControl: false
  }).setView([18.5204, 73.8670], 14);

  // Modern crisp vector tiles (CartoDB Positron / OSM style)
  L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
    maxZoom: 19,
    subdomains: 'abcd',
  }).addTo(app.leafletMap);

  // Start & End markers
  const startIcon = L.divIcon({
    className: 'custom-map-pin start-pin',
    html: `<div style="background:#2563eb; color:#fff; width:26px; height:26px; border-radius:50%; display:flex; align-items:center; justify-content:center; font-weight:bold; font-size:12px; border:2px solid #fff; box-shadow:0 2px 6px rgba(0,0,0,0.3);">A</div>`,
    iconSize: [26, 26],
    iconAnchor: [13, 13]
  });

  const endIcon = L.divIcon({
    className: 'custom-map-pin end-pin',
    html: `<div style="background:#16a34a; color:#fff; width:26px; height:26px; border-radius:50%; display:flex; align-items:center; justify-content:center; font-weight:bold; font-size:12px; border:2px solid #fff; box-shadow:0 2px 6px rgba(0,0,0,0.3);">B</div>`,
    iconSize: [26, 26],
    iconAnchor: [13, 13]
  });

  L.marker([18.5204, 73.8567], { icon: startIcon }).addTo(app.leafletMap)
    .bindPopup('<b>Start Point</b><br>Live GPS Origin');

  L.marker([18.5167, 73.8780], { icon: endIcon }).addTo(app.leafletMap)
    .bindPopup('<b>Destination</b><br>MG Road / Residency Road');

  // Draw Polylines for each route
  Object.keys(routeWaypoints).forEach((routeKey) => {
    const coords = routeWaypoints[routeKey];
    const isSelected = routeKey === app.currentRoute;
    const polyline = L.polyline(coords, {
      color: routeInfo[routeKey].color,
      weight: isSelected ? 6 : 3,
      opacity: isSelected ? 0.95 : 0.45,
      dashArray: routeKey === 'fastest' ? '6, 6' : null,
    }).addTo(app.leafletMap);

    polyline.on('click', () => {
      selectRoute(routeKey);
    });

    app.routePolylines[routeKey] = polyline;
  });

  // Load and plot database hazards
  await loadAndPlotHazards();
}

async function loadAndPlotHazards() {
  try {
    const res = await civicGrievanceService.getActiveHazards();
    const hazards = res?.data || [
      { id: 'h1', hazard_type: 'POTHOLE', latitude: 18.5210, longitude: 73.8650, verified: 1, distinct_devices: 3, authority_name: 'Pune Municipal Corporation' },
      { id: 'h2', hazard_type: 'BROKEN_ROAD', latitude: 18.5230, longitude: 73.8700, verified: 0, distinct_devices: 1, authority_name: 'PWD Maharashtra' },
    ];

    hazards.forEach((h) => {
      const hazardIcon = L.divIcon({
        className: 'custom-hazard-pin',
        html: `<div style="background:#ea580c; color:#fff; width:28px; height:28px; border-radius:50%; display:flex; align-items:center; justify-content:center; font-size:14px; border:2px solid #fff; box-shadow:0 2px 8px rgba(234,88,12,0.4); cursor:pointer;">⚠️</div>`,
        iconSize: [28, 28],
        iconAnchor: [14, 14]
      });

      const marker = L.marker([h.latitude, h.longitude], { icon: hazardIcon }).addTo(app.leafletMap);
      marker.bindPopup(`
        <div style="font-family:sans-serif; font-size:12px; line-height:1.4;">
          <b style="color:#ea580c;">⚠️ ${h.hazard_type || 'Road Hazard'}</b><br>
          Status: <b>${h.verified ? 'VERIFIED (3+ Devices)' : 'Unverified Spike'}</b><br>
          Jurisdiction: ${h.authority_name || 'Municipal Corp'}<br>
          <small style="color:#64748b;">Safest route steers drivers around this.</small>
        </div>
      `);
      app.hazardMarkers.push(marker);
    });
  } catch (err) {
    console.warn('[Map Hazard Plotting Warning]', err);
  }
}

function updateMapRouteHighlight(selectedRoute) {
  Object.keys(app.routePolylines).forEach((key) => {
    const line = app.routePolylines[key];
    if (!line) return;
    const isSelected = key === selectedRoute;
    line.setStyle({
      weight: isSelected ? 6 : 3,
      opacity: isSelected ? 0.95 : 0.4,
    });
    if (isSelected) line.bringToFront();
  });
}

async function updateHomeRisk() {
  const risk = await getRouteRisk('safest', app.currentProfile);
  const labels = {
    low: ['Safe', 'Clear weather · normal traffic'],
    medium: ['Stay alert', 'Surface conditions changing ahead'],
    high: ['Caution', 'Elevated hazards on nearby roads']
  };
  $('#home-risk-score').textContent = risk.score;
  $('#risk-gauge').style.setProperty('--risk-progress', risk.score);
  $('#safety-label').textContent = labels[risk.level][0];
  $('#safety-detail').textContent = `${labels[risk.level][1]} · ${PASSENGER_PROFILES[app.currentProfile].label}`;
  $('#risk-gauge').className = `risk-gauge ${risk.level}`;
  $('.status-orb').className = `status-orb ${risk.level === 'low' ? 'safe' : risk.level}`;
}

async function selectRoute(route) {
  app.currentRoute = route;

  let liveData = null;
  try {
    liveData = await liveRouteService.getRecommendations(
      app.userLocation,
      app.destinationLocation,
      app.currentProfile
    );
  } catch (e) {
    console.warn('[Live Route Recommendation fallback]', e);
  }

  const risk = await getRouteRisk(route, app.currentProfile);
  $$('.route-card').forEach((card) => {
    const selected = card.dataset.route === route;
    card.classList.toggle('selected', selected);
    card.setAttribute('aria-checked', String(selected));
  });

  const info = routeInfo[route];
  $('#start-route').querySelector('span').textContent = `Start ${info.label.toLowerCase()} route`;
  $('#home-trip-meta').textContent = `${info.distance} · ${info.duration} away`;

  updateMapRouteHighlight(route);

  if (liveData && liveData.recommendationReason) {
    $('#route-update').textContent = `${liveData.recommendationReason} · ${PASSENGER_PROFILES[app.currentProfile].label}`;
  } else {
    $('#route-update').textContent = `${formatRisk(risk)} · ${PASSENGER_PROFILES[app.currentProfile].label}`;
  }
}

async function selectPassengerProfile(profileKey) {
  app.currentProfile = profileKey;
  $$('.passenger-opt').forEach((opt) => {
    const selected = opt.dataset.profile === profileKey;
    opt.classList.toggle('selected', selected);
    opt.setAttribute('aria-checked', String(selected));
  });

  const profile = PASSENGER_PROFILES[profileKey];
  showToast(`Profile applied: ${profile.label}`);

  await Promise.all([selectRoute(app.currentRoute), updateHomeRisk()]);
}

function openSheet(sheet) {
  $('#modal-backdrop').hidden = false;
  sheet.hidden = false;
  requestAnimationFrame(() => { $('#modal-backdrop').classList.add('shown'); sheet.classList.add('shown'); });
}

function closeSheets() {
  const backdrop = $('#modal-backdrop');
  backdrop.classList.remove('shown');
  $$('.sheet').forEach((sheet) => sheet.classList.remove('shown'));
  setTimeout(() => { backdrop.hidden = true; $$('.sheet').forEach((sheet) => { sheet.hidden = true; }); }, 180);
}

function locateUser() {
  if (!navigator.geolocation) { $('#location-title').textContent = 'Location not available'; return; }
  navigator.geolocation.getCurrentPosition(
    ({ coords }) => {
      app.userLocation = { latitude: coords.latitude, longitude: coords.longitude };
      app.currentSpeed = coords.speed || 0;
      $('#location-title').textContent = 'Location attached';
      $('#location-detail').textContent = `${coords.latitude.toFixed(4)}, ${coords.longitude.toFixed(4)}`;
    },
    () => { $('#location-title').textContent = 'Location estimated'; },
    { enableHighAccuracy: true, timeout: 6000 },
  );
}

async function submitReport() {
  const report = await hazardService.report({ type: app.selectedHazard, ...app.userLocation });
  closeSheets();
  showToast(`${report.type} report received. Thanks for helping drivers.`);
  $('#alert-count').textContent = '3 new';
}

async function confirmHazard(button) {
  const response = button.dataset.confirm;
  const result = await hazardService.confirm(1, response);
  const wrap = button.closest('.confirmation');
  wrap.innerHTML = `<span class="confirmation-result">${response === 'fixed' ? 'Thanks — marked for review.' : 'Thanks — confirmation recorded.'}</span>`;
  if (result.status === 'UNDER_REVIEW') $('.muted-card .review-label').textContent = 'Review updated';
}

function answerAssistant(question) {
  const normalized = question.toLowerCase();
  let response = 'The safest option currently avoids a slow, busier corridor and has fewer recent braking events.';
  if (normalized.includes('watch') || normalized.includes('alert')) response = 'Watch for a severe pothole cluster near Vittal Mallya Road. Safest route automatically navigates around it.';
  if (normalized.includes('safe') || normalized.includes('why')) response = `Based on ${PASSENGER_PROFILES[app.currentProfile].label} mode, this route eliminates high-G vertical jolts and stays on well-lit main corridors.`;
  $('#assistant-answer p').textContent = response;
}

// --------------------------------------------------
// Feature C: Passive Accelerometer Crowdsensing
// --------------------------------------------------
function setupPassiveSensorCrowdsensing() {
  if (navigator.geolocation) {
    navigator.geolocation.watchPosition(
      (pos) => {
        app.currentSpeed = pos.coords.speed || 0;
        app.userLocation = { latitude: pos.coords.latitude, longitude: pos.coords.longitude };
      },
      null,
      { enableHighAccuracy: true }
    );
  }

  if (window.DeviceMotionEvent) {
    window.addEventListener('devicemotion', async (e) => {
      const acc = e.acceleration || e.accelerationIncludingGravity;
      if (!acc) return;

      if (app.currentSpeed < 4.2 && app.currentSpeed > 0) return;

      const zJerk = Math.abs(acc.z || 0);
      const now = Date.now();

      if (zJerk > 12.0 && now - app.lastSpikeTime > 2500) {
        app.lastSpikeTime = now;
        console.log(`[PASSIVE SENSOR] Pothole spike: ${zJerk.toFixed(1)} m/s²`);

        try {
          const res = await civicGrievanceService.sendSensorSpike({
            lat: app.userLocation.latitude,
            lng: app.userLocation.longitude,
            magnitude: zJerk,
            speed: app.currentSpeed > 0 ? (app.currentSpeed * 3.6) : 25,
            deviceId,
            locationName: 'Live Sensor Detected Corridor',
          });
          if (res?.data?.isVerified) {
            showToast(`🏛️ Hazard verified by 3 devices! Auto-complaint dispatched.`);
          } else {
            showToast(`📡 Passive shock logged (${zJerk.toFixed(1)} m/s²). Zero driver effort.`);
          }
        } catch (err) {
          console.warn('Sensor ingestion warning:', err);
        }
      }
    });
  }
}

// --------------------------------------------------
// Camera AI Vision Engine (Computer Vision HUD)
// --------------------------------------------------
let visionBoxTimer = 0;
let detectedFaultCount = 0;

function setupCameraScanner() {
  const toggleBtn = $('#toggle-camera-btn');
  const triggerBtn = $('#trigger-pothole-vision');
  const retryBtn = $('#retry-camera-btn');

  toggleBtn?.addEventListener('click', toggleCameraFeed);
  triggerBtn?.addEventListener('click', triggerVisionPotholeAnomaly);
  retryBtn?.addEventListener('click', startCameraFeed);
}

async function toggleCameraFeed() {
  if (app.isCameraActive) {
    stopCameraFeed();
  } else {
    await startCameraFeed();
  }
}

async function startCameraFeed() {
  const video = $('#camera-video');
  const canvas = $('#camera-canvas');
  const fallback = $('#camera-fallback-msg');
  const toggleBtn = $('#toggle-camera-btn');
  const pill = $('#scanner-status-pill');

  if (!video || !canvas) return;

  app.isCameraActive = true;
  toggleBtn.querySelector('span').textContent = 'Stop Camera Detection';
  if (pill) { pill.textContent = 'Scanning'; pill.style.color = '#16a34a'; }

  try {
    const stream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: 'environment', width: { ideal: 640 }, height: { ideal: 480 } }
    });
    app.cameraStream = stream;
    video.srcObject = stream;
    fallback.style.display = 'none';
  } catch (err) {
    console.warn('[Camera getUserMedia unavailable/blocked, running simulated road canvas]', err);
    fallback.style.display = 'none'; // Keep canvas HUD running simulation
  }

  runCameraHudLoop();
}

function stopCameraFeed() {
  app.isCameraActive = false;
  if (app.cameraStream) {
    app.cameraStream.getTracks().forEach(t => t.stop());
    app.cameraStream = null;
  }
  if (app.cameraAnimId) {
    cancelAnimationFrame(app.cameraAnimId);
  }
  const toggleBtn = $('#toggle-camera-btn');
  if (toggleBtn) toggleBtn.querySelector('span').textContent = 'Start Camera Detection';
  const pill = $('#scanner-status-pill');
  if (pill) { pill.textContent = 'Ready'; pill.style.color = ''; }

  const canvas = $('#camera-canvas');
  if (canvas) {
    const ctx = canvas.getContext('2d');
    ctx.clearRect(0, 0, canvas.width, canvas.height);
  }
}

function runCameraHudLoop() {
  const canvas = $('#camera-canvas');
  if (!canvas || !app.isCameraActive) return;

  const ctx = canvas.getContext('2d');
  canvas.width = canvas.clientWidth || 320;
  canvas.height = canvas.clientHeight || 260;

  let frame = 0;
  function render() {
    if (!app.isCameraActive) return;
    frame++;

    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // Draw Lane Tracking Lines
    ctx.strokeStyle = 'rgba(74, 222, 128, 0.4)';
    ctx.lineWidth = 2;
    ctx.setLineDash([8, 6]);

    // Left lane boundary
    ctx.beginPath();
    ctx.moveTo(canvas.width * 0.15, canvas.height);
    ctx.lineTo(canvas.width * 0.42, canvas.height * 0.45);
    ctx.stroke();

    // Right lane boundary
    ctx.beginPath();
    ctx.moveTo(canvas.width * 0.85, canvas.height);
    ctx.lineTo(canvas.width * 0.58, canvas.height * 0.45);
    ctx.stroke();
    ctx.setLineDash([]);

    // Scanline effect
    const scanY = (frame * 3) % canvas.height;
    ctx.fillStyle = 'rgba(37, 99, 235, 0.15)';
    ctx.fillRect(0, scanY, canvas.width, 3);

    // If active detected pothole box
    if (visionBoxTimer > 0) {
      visionBoxTimer--;
      const bx = canvas.width * 0.38;
      const by = canvas.height * 0.58;
      const bw = canvas.width * 0.24;
      const bh = canvas.height * 0.18;

      ctx.strokeStyle = '#ef4444';
      ctx.lineWidth = 3;
      ctx.strokeRect(bx, by, bw, bh);

      ctx.fillStyle = 'rgba(239, 68, 68, 0.85)';
      ctx.fillRect(bx, by - 20, bw, 20);

      ctx.fillStyle = '#ffffff';
      ctx.font = 'bold 11px sans-serif';
      ctx.fillText('POTHOLE 96%', bx + 6, by - 6);
    }

    app.cameraAnimId = requestAnimationFrame(render);
  }

  render();
}

async function triggerVisionPotholeAnomaly() {
  visionBoxTimer = 120; // Show detection box for ~3 seconds
  detectedFaultCount += 1;
  const faultsEl = $('#vision-faults');
  if (faultsEl) faultsEl.textContent = `${detectedFaultCount} Detected`;

  showToast('👁️ AI Vision: Deep Pothole detected ahead in lane!');

  // Log to backend
  try {
    const res = await civicGrievanceService.sendSensorSpike({
      lat: app.userLocation.latitude,
      lng: app.userLocation.longitude,
      magnitude: 14.8,
      speed: 35,
      deviceId,
      locationName: 'Camera Vision AI Verified Location'
    });
    if (res?.data?.isVerified) {
      showToast('🎉 Hazard verified via AI Vision! Auto-complaint dispatched.');
    }
  } catch (err) {
    console.warn(err);
  }
}

// --------------------------------------------------
// Hardware IoT Simulator
// --------------------------------------------------
function setupHardwarePanel() {
  const spikeBtn = $('#hardware-spike-btn');
  spikeBtn?.addEventListener('click', async () => {
    const zEl = $('#iot-z-accel');
    if (zEl) {
      zEl.textContent = '3.24 G (SPIKE)';
      zEl.style.color = '#ef4444';
      setTimeout(() => {
        zEl.textContent = '1.02 G';
        zEl.style.color = 'var(--text)';
      }, 3000);
    }

    showToast('📟 Hardware OBD-II/IMU: Severe 3.2 G shock transmitted to CAN Gateway!');
    await triggerSimulateSpike();
  });
}

// --------------------------------------------------
// Mode Tabs Switcher (Camera vs Hardware vs Manual)
// --------------------------------------------------
function setupScannerModeTabs() {
  $$('.mode-tab').forEach((tab) => {
    tab.addEventListener('click', () => {
      const mode = tab.dataset.mode;
      $$('.mode-tab').forEach(t => {
        const isSel = t === tab;
        t.classList.toggle('active', isSel);
        t.style.background = isSel ? 'var(--primary)' : 'transparent';
        t.style.color = isSel ? '#fff' : 'var(--text-muted)';
      });

      $('#camera-panel').style.display = mode === 'camera' ? 'block' : 'none';
      $('#hardware-panel').style.display = mode === 'hardware' ? 'block' : 'none';
      $('#manual-panel').style.display = mode === 'manual' ? 'block' : 'none';

      if (mode === 'camera' && !app.isCameraActive) {
        startCameraFeed();
      } else if (mode !== 'camera' && app.isCameraActive) {
        stopCameraFeed();
      }
    });
  });

  // Manual 1-tap quick buttons
  $$('.manual-quick-report').forEach((btn) => {
    btn.addEventListener('click', async () => {
      const hazard = btn.dataset.hazard;
      showToast(`📝 1-Tap Report: ${hazard} submitted directly!`);
      await hazardService.report({ type: hazard, ...app.userLocation });
      $('#alert-count').textContent = '4 new';
    });
  });
}

// --------------------------------------------------
// Backup Demo Switch: Simulate Pothole Spike
// --------------------------------------------------
async function triggerSimulateSpike() {
  showToast('⚡ Simulating 14.5 m/s² pothole spike...');
  const res = await civicGrievanceService.simulateDemoSpike(
    app.userLocation.latitude,
    app.userLocation.longitude,
    deviceId
  );

  if (res && res.success) {
    const data = res.data;
    if (data.isVerified) {
      showToast(`🎉 VERIFIED! 3rd distinct device reached. Auto-complaint emailed to ${data.complaintDetails?.authorityName || 'Authority'}!`);
    } else {
      showToast(`✔ Spike logged! Hazard cluster count: ${data.distinctDevicesConfirmed}/3 devices.`);
    }
    if ($('#civic-screen').classList.contains('active')) {
      loadCivicDashboard();
    }
  } else {
    showToast('Spike simulated locally. Sensor active!');
  }
}

// --------------------------------------------------
// Crash SOS Countdown Modal
// --------------------------------------------------
function startCrashSOS() {
  const modal = $('#crash-modal');
  modal.style.display = 'flex';
  app.crashSecondsRemaining = 15;
  $('#crash-seconds').textContent = app.crashSecondsRemaining;
  $('#crash-status-text').textContent = `Crash impact detected. Emergency contact (${app.emergencyPhone}) will be alerted if timer expires.`;

  clearInterval(app.crashTimer);
  app.crashTimer = setInterval(() => {
    app.crashSecondsRemaining -= 1;
    $('#crash-seconds').textContent = app.crashSecondsRemaining;

    if (app.crashSecondsRemaining <= 0) {
      clearInterval(app.crashTimer);
      $('#crash-status-text').textContent = `🚨 Timer expired! Calling emergency contact (${app.emergencyPhone})...`;
      window.location.href = `tel:${app.emergencyPhone}`;
    }
  }, 1000);
}

function cancelCrashSOS() {
  clearInterval(app.crashTimer);
  $('#crash-modal').style.display = 'none';
  showToast('Crash alert cancelled. Stay safe!');
}

// --------------------------------------------------
// Public Civic Transparency Dashboard
// --------------------------------------------------
async function loadCivicDashboard() {
  const data = await civicGrievanceService.getDashboard();
  if (!data) return;

  const m = data.metrics || {};
  $('#metric-hazards').textContent = m.totalHazardsDetected ?? '--';
  $('#metric-complaints').textContent = m.complaintsDispatched ?? '--';
  $('#metric-escalations').textContent = (m.remindersIssued || 0) + (m.escalationsIssued || 0);
  $('#metric-resolved').textContent = m.complaintsResolved ?? m.resolvedHazards ?? '--';

  const timeline = $('#civic-timeline');
  if (data.recentTimeline && data.recentTimeline.length > 0) {
    timeline.innerHTML = data.recentTimeline.map((item) => {
      let badgeClass = 'day0';
      if (item.event_type.includes('REMINDER')) badgeClass = 'day7';
      if (item.event_type.includes('ESCALATED')) badgeClass = 'day14';
      if (item.event_type.includes('RESOLVED')) badgeClass = 'resolved';

      return `
        <div class="timeline-card">
          <div class="timeline-card-header">
            <b>${item.complaint_code || 'GRIEVANCE'}</b>
            <span class="timeline-badge ${badgeClass}">${item.event_type}</span>
          </div>
          <p style="margin: 0; color: var(--text-muted); font-size: 11px;">${item.description}</p>
          <small style="color: var(--text-muted); margin-top: 6px; display: block; opacity: 0.8;">${new Date(item.created_at).toLocaleString('en-IN')}</small>
        </div>
      `;
    }).join('');
  } else {
    timeline.innerHTML = `
      <div class="timeline-card">
        <div class="timeline-card-header"><b>No grievances yet</b></div>
        <p style="margin:0; color:var(--text-muted);">Use the demo spike button to trigger crowdsourced complaints.</p>
      </div>
    `;
  }
}

function bindEvents() {
  $$('[data-nav]').forEach((button) => button.addEventListener('click', () => showScreen(button.dataset.nav)));
  $$('.route-card').forEach((card) => card.addEventListener('click', () => selectRoute(card.dataset.route)));

  // Passenger profiles
  $$('.passenger-opt').forEach((opt) => opt.addEventListener('click', () => selectPassengerProfile(opt.dataset.profile)));

  // Quick Demo controls
  $('#demo-simulate-spike')?.addEventListener('click', triggerSimulateSpike);
  $('#demo-test-sos')?.addEventListener('click', startCrashSOS);
  $('#refresh-dashboard-btn')?.addEventListener('click', loadCivicDashboard);

  $('#demo-fast-forward')?.addEventListener('click', async () => {
    showToast('⏩ Fast-forwarding 7 days in civic timeline...');
    const res = await civicGrievanceService.fastForward7Days();
    if (res && res.success) {
      showToast('✔ Day 0 complaints progressed! Reminders and escalations dispatched.');
      await loadCivicDashboard();
      showScreen('civic');
    } else {
      showToast('Fast-forward completed.');
    }
  });

  // Crash modal actions
  $('#cancel-crash-btn')?.addEventListener('click', cancelCrashSOS);
  $('#call-emergency-btn')?.addEventListener('click', () => {
    window.location.href = `tel:${app.emergencyPhone}`;
  });

  // Emergency contact save
  const phoneInput = $('#emergency-phone-input');
  if (phoneInput) {
    phoneInput.value = app.emergencyPhone;
    $('#save-emergency-btn')?.addEventListener('click', () => {
      const val = phoneInput.value.trim();
      if (val) {
        app.emergencyPhone = val;
        localStorage.setItem('smartmove_emergency_phone', val);
        showToast('Emergency contact phone saved locally on device.');
      }
    });
  }

  $('#start-route').addEventListener('click', () => {
    const info = routeInfo[app.currentRoute];
    showToast(`${info.label} route started · ${info.duration} to MG Road (${PASSENGER_PROFILES[app.currentProfile].label})`);
    showScreen('home');
  });

  $$('.filter').forEach((button) => button.addEventListener('click', () => {
    $$('.filter').forEach((filter) => filter.classList.toggle('active', filter === button));
    $$('.alert-card').forEach((card) => card.classList.toggle('filtered-out', button.dataset.filter !== 'all' && card.dataset.kind !== button.dataset.filter));
  }));

  $$('.confirmation button').forEach((button) => button.addEventListener('click', () => confirmHazard(button)));
  $('#open-report').addEventListener('click', () => { openSheet($('#report-sheet')); locateUser(); });
  $$('.hazard-option').forEach((button) => button.addEventListener('click', () => {
    app.selectedHazard = button.dataset.hazard;
    $$('.hazard-option').forEach((option) => option.classList.toggle('selected', option === button));
  }));

  $('#submit-report').addEventListener('click', submitReport);
  $('#assistant-trigger').addEventListener('click', () => openSheet($('#assistant-sheet')));
  $$('.close-sheet').forEach((button) => button.addEventListener('click', closeSheets));
  $('#modal-backdrop').addEventListener('click', closeSheets);
  $$('.prompt-chips button').forEach((button) => button.addEventListener('click', () => answerAssistant(button.dataset.prompt)));
  $('#assistant-form').addEventListener('submit', (event) => {
    event.preventDefault();
    const input = $('#assistant-input');
    if (!input.value.trim()) return;
    answerAssistant(input.value);
    input.value = '';
  });

  $$('.toggle').forEach((toggle) => toggle.addEventListener('click', (event) => { event.currentTarget.classList.toggle('on'); }));
}

async function initialize() {
  setupThemeSwitcher();
  setupCameraScanner();
  setupHardwarePanel();
  setupScannerModeTabs();
  bindEvents();
  setupPassiveSensorCrowdsensing();

  await Promise.all([
    updateHomeRisk(),
    selectRoute('safest'),
    weatherService.current(18.5204, 73.8567),
  ]);

  await initInteractiveMap();
}

initialize();
