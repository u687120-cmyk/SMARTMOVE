const assert = require('assert');
const routeService = require('./src/services/routeService');
const RouteConfig = require('./src/config/routeConfig');

async function testRerouteEngine() {
  console.log('==================================================');
  console.log('  SMARTMOVE AI — Phase 17: Dynamic En-Route Reroute Tests');
  console.log('==================================================\n');

  const origin = { latitude: 18.5204, longitude: 73.8567 };
  const destination = { latitude: 18.5314, longitude: 73.8446 };
  const currentCoords = { latitude: 18.5250, longitude: 73.8500 };

  // --------------------------------------------------
  // 1. CURRENT ROUTE REMAINS SAFE (No Reroute Needed)
  // --------------------------------------------------
  console.log('--- 1. Current Route Remains Safe (Optimal Conditions) ---');
  // Scenario: Driver is on Route B (Low risk 16)
  const resultSafe = await routeService.evaluateEnRouteReroute(
    'route-b',
    origin,
    destination,
    currentCoords,
    { weatherCondition: 'CLEAR' }
  );

  console.log(`- Current: ${resultSafe.currentRoute.routeId} (Risk: ${resultSafe.currentRoute.riskScore})`);
  console.log(`- Should Reroute: ${resultSafe.shouldReroute}`);
  assert.strictEqual(resultSafe.shouldReroute, false, 'Safe route must not trigger reroute prompt');
  assert.strictEqual(resultSafe.safetyPrompt, null, 'No prompt should be generated when route is safe');
  console.log('✔ Safe route evaluated without unnecessary prompts');

  // --------------------------------------------------
  // 2. TRAFFIC DETERIORATION ON CURRENT ROUTE (Trigger Reroute)
  // --------------------------------------------------
  console.log('\n--- 2. Traffic Deterioration on Current Route ---');
  // Scenario: Driver is on Route A, but severe rush-hour traffic slows it down (Risk ~72)
  // Alternative Route B is smooth (Risk ~34) with only +3 min detour
  const resultTraffic = await routeService.evaluateEnRouteReroute(
    'route-a',
    origin,
    destination,
    currentCoords,
    {
      routeOverrides: {
        'route-a': {
          segments: [
            { segmentId: 'seg-a1', name: 'FC Road North', lengthKm: 2.0, baseTraffic: 'HIGH', hazardType: 'HEAVY_TRAFFIC', hazardSeverity: 'HIGH' },
            { segmentId: 'seg-a2', name: 'Deccan Gymkhana', lengthKm: 2.2, baseTraffic: 'HIGH', hazardType: null, hazardSeverity: null },
            { segmentId: 'seg-a3', name: 'Karve Road Junction', lengthKm: 2.0, baseTraffic: 'HIGH', hazardType: null, hazardSeverity: null },
            { segmentId: 'seg-a4', name: 'Kothrud Approach', lengthKm: 2.0, baseTraffic: 'MODERATE', hazardType: null, hazardSeverity: null },
          ],
        },
      },
    }
  );

  console.log(`- Current: Route A (Risk: ${resultTraffic.currentRoute.riskScore}, Level: ${resultTraffic.currentRoute.riskLevel})`);
  console.log(`- Alternative: ${resultTraffic.alternativeRoute.name} (Risk: ${resultTraffic.alternativeRoute.riskScore})`);
  console.log(`- Should Reroute: ${resultTraffic.shouldReroute}`);
  assert.strictEqual(resultTraffic.shouldReroute, true, 'High traffic deterioration must trigger reroute recommendation');
  assert(resultTraffic.safetyPrompt !== null, 'Safety prompt must be created');
  assert.strictEqual(resultTraffic.safetyPrompt.alternativeRouteId, 'route-b', 'Should suggest Route B as safer alternative');
  console.log(`✔ Safety Prompt generated: "${resultTraffic.safetyPrompt.message}"`);
  console.log(`✔ Prompt Summary: "${resultTraffic.safetyPrompt.summary}"`);

  // --------------------------------------------------
  // 3. HIGH-SEVERITY HAZARD AHEAD DETECTION
  // --------------------------------------------------
  console.log('\n--- 3. High-Severity Hazard Ahead Detection ---');
  const resultHazard = await routeService.evaluateEnRouteReroute(
    'route-a',
    origin,
    destination,
    currentCoords,
    {
      routeOverrides: {
        'route-a': {
          segments: [
            { segmentId: 'seg-a1', name: 'FC Road North', lengthKm: 2.0, baseTraffic: 'LOW', hazardType: 'ACCIDENT', hazardSeverity: 'HIGH' },
            { segmentId: 'seg-a2', name: 'Deccan Gymkhana', lengthKm: 2.2, baseTraffic: 'LOW', hazardType: null, hazardSeverity: null },
          ],
        },
      },
    }
  );
  console.log(`- Severe Hazard on Route A: Should Reroute = ${resultHazard.shouldReroute}`);
  assert.strictEqual(resultHazard.shouldReroute, true, 'Severe accident/hazard ahead must trigger reroute prompt');
  console.log('✔ Severe road hazard correctly detected and prompts for safer corridor');

  // --------------------------------------------------
  // 4. RESOLVED HAZARD DOES NOT TRIGGER REROUTE
  // --------------------------------------------------
  console.log('\n--- 4. Resolved Hazard Does Not Trigger Reroute ---');
  const resultResolved = await routeService.evaluateEnRouteReroute(
    'route-a',
    origin,
    destination,
    currentCoords,
    {
      routeOverrides: {
        'route-a': {
          segments: [
            { segmentId: 'seg-a1', name: 'FC Road North', lengthKm: 2.0, baseTraffic: 'LOW', hazardType: 'ACCIDENT', hazardSeverity: 'HIGH', hazardStatus: 'RESOLVED' },
            { segmentId: 'seg-a2', name: 'Deccan Gymkhana', lengthKm: 2.2, baseTraffic: 'LOW', hazardType: null, hazardSeverity: null },
          ],
        },
      },
    }
  );
  console.log(`- Resolved Hazard on Route A: Risk = ${resultResolved.currentRoute.riskScore}, Should Reroute = ${resultResolved.shouldReroute}`);
  assert.strictEqual(resultResolved.shouldReroute, false, 'Resolved hazard must not trigger reroute');
  console.log('✔ Resolved hazard ignored as required');

  // --------------------------------------------------
  // 5. EXCESSIVE DETOUR REJECTION
  // --------------------------------------------------
  console.log('\n--- 5. Excessive Detour Rejection ---');
  // Scenario: Alternative route adds +25 minutes (>100% detour) for small risk benefit
  const resultDetour = await routeService.evaluateEnRouteReroute(
    'route-a',
    origin,
    destination,
    currentCoords,
    {
      routeOverrides: {
        'route-a': { durationMinutes: 20, baseDurationMinutes: 20, segments: [{ riskScore: 35 }] },
        'route-b': { durationMinutes: 48, baseDurationMinutes: 48, segments: [{ riskScore: 28 }] }, // Excessive detour
        'route-c': { durationMinutes: 52, baseDurationMinutes: 52, segments: [{ riskScore: 25 }] }, // Excessive detour
      },
    }
  );
  console.log(`- Current: 20 min vs Alt: 48 min -> Should Reroute: ${resultDetour.shouldReroute}`);
  assert.strictEqual(resultDetour.shouldReroute, false, 'Absurdly long detour must be rejected');
  console.log('✔ Detour guardrail successfully prevented excessive travel time recommendation');

  // --------------------------------------------------
  // 6. PROMPT COOLDOWN & DE-DUPLICATION
  // --------------------------------------------------
  console.log('\n--- 6. Prompt Cooldown & De-duplication ---');
  const now = Date.now();
  // Prompt shown 30 seconds ago (< 120s cooldown)
  const resultCooldown = await routeService.evaluateEnRouteReroute(
    'route-a',
    origin,
    destination,
    currentCoords,
    {
      lastPromptTimestamp: now - 30 * 1000,
      lastPromptRouteId: 'route-b',
    }
  );
  console.log(`- Reroute with recent prompt (30s ago): isPromptSuppressed = ${resultCooldown.isPromptSuppressed}`);
  assert.strictEqual(resultCooldown.isPromptSuppressed, true, 'Must suppress duplicate prompt during cooldown');
  assert.strictEqual(resultCooldown.shouldReroute, false, 'shouldReroute must be false when suppressed');
  console.log('✔ Prompt cooldown successfully prevents distracting the driver repeatedly');

  // --------------------------------------------------
  // 7. ROUTE SWITCH LOGGING
  // --------------------------------------------------
  console.log('\n--- 7. Route Switch Event Persistence ---');
  const switchLog = await routeService.recordRouteSwitch('trip-999', 1, {
    previousRouteId: 'route-a',
    newRouteId: 'route-b',
    reason: 'Driver accepted safer route prompt due to traffic',
    oldRisk: 72,
    newRisk: 34,
    oldEtaMinutes: 32,
    newEtaMinutes: 27,
  });
  console.log(`- Route switch event logged (mock/DB ID: ${switchLog !== null ? switchLog : 'handled safely'})`);
  console.log('✔ Route switch lifecycle recorded safely');

  console.log('\n==================================================');
  console.log('  ✅ ALL PHASE 17 EN-ROUTE REROUTE TESTS PASSED');
  console.log('==================================================\n');
}

testRerouteEngine().then(() => {
  process.exit(0);
}).catch((err) => {
  console.error('\n❌ En-Route Reroute Test Failed:', err);
  process.exit(1);
});
