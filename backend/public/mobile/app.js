import { getRouteRisk, hazardService, weatherService, civicGrievanceService } from './mock-services.js';
import { PASSENGER_PROFILES } from './risk-engine.js';

// Device UUID persistence (Zero PII, anonymous device fingerprint)
let deviceId = localStorage.getItem('smartmove_device_id');
if (!deviceId) {
  deviceId = 'dev_' + (crypto.randomUUID ? crypto.randomUUID().slice(0, 12) : Math.random().toString(36).substring(2, 12));
  localStorage.setItem('smartmove_device_id', deviceId);
}

const app = {
  currentRoute: 'safest',
  currentProfile: 'HEALTHY',
  selectedHazard: 'Pothole',
  userLocation: { latitude: 18.5204, longitude: 73.8567 },
  currentSpeed: 0, // m/s
  lastSpikeTime: 0,
  emergencyPhone: localStorage.getItem('smartmove_emergency_phone') || '+91 9876543210',
  crashTimer: null,
  crashSecondsRemaining: 15,
};

const routeInfo = {
  safest: { label: 'Safest', duration: '29 min', distance: '8.1 km', detail: 'Lowest current risk' },
  fastest: { label: 'Fastest', duration: '24 min', distance: '7.2 km', detail: 'Via Residency Road' },
  balanced: { label: 'Balanced', duration: '27 min', distance: '7.7 km', detail: 'Best all-round option' },
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
  $('#app').scrollTo({ top: 0, behavior: 'smooth' });
}

function showToast(message) {
  const toast = $('#toast');
  toast.textContent = message;
  toast.classList.add('visible');
  clearTimeout(showToast.timer);
  showToast.timer = setTimeout(() => toast.classList.remove('visible'), 3600);
}

function formatRisk(risk) { return `${titleCase(risk.level)} risk (${risk.score}/100)`; }

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
  const risk = await getRouteRisk(route, app.currentProfile);
  $$('.route-card').forEach((card) => {
    const selected = card.dataset.route === route;
    card.classList.toggle('selected', selected);
    card.setAttribute('aria-checked', String(selected));
  });
  const info = routeInfo[route];
  $('#start-route').querySelector('span').textContent = `Start ${info.label.toLowerCase()} route`;
  $('#home-trip-meta').textContent = `${info.distance} · ${info.duration} away`;
  $('.safest-route').classList.toggle('dim-route', route === 'fastest');
  $('.fastest-route').classList.toggle('active-route', route === 'fastest');
  $('#route-update').textContent = `${formatRisk(risk)} · ${PASSENGER_PROFILES[app.currentProfile].label}`;
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

  // Re-calculate route risks with new profile weights
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

  // Request motion permission if required (e.g. Safari / iOS 13+)
  if (window.DeviceMotionEvent) {
    window.addEventListener('devicemotion', async (e) => {
      const acc = e.acceleration || e.accelerationIncludingGravity;
      if (!acc) return;

      // Noise rejection: only evaluate if moving > 15 km/h (4.2 m/s)
      if (app.currentSpeed < 4.2 && app.currentSpeed > 0) return;

      const zJerk = Math.abs(acc.z || 0);
      const now = Date.now();

      // Pothole vertical spike threshold (> 12 m/s²) and rate limiter (> 2s between spikes)
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
    // Refresh civic dashboard if user opens it
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
          <p style="margin: 0; color: #cbd5e1; font-size: 11px;">${item.description}</p>
          <small style="color: #64748b; margin-top: 6px; display: block;">${new Date(item.created_at).toLocaleString('en-IN')}</small>
        </div>
      `;
    }).join('');
  } else {
    timeline.innerHTML = `
      <div class="timeline-card">
        <div class="timeline-card-header"><b>No grievances yet</b></div>
        <p style="margin:0; color:#94a3b8;">Use the demo spike button to trigger crowdsourced complaints.</p>
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
  bindEvents();
  setupPassiveSensorCrowdsensing();
  await Promise.all([
    updateHomeRisk(),
    selectRoute('safest'),
    weatherService.current(18.5204, 73.8567),
  ]);
}

initialize();
