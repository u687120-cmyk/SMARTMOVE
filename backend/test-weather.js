const assert = require('assert');
const weatherService = require('./src/services/weatherService');
const WeatherConfig = require('./src/config/weatherConfig');

async function testWeatherEngine() {
  console.log('==================================================');
  console.log('  SMARTMOVE AI — Weather Service Unit & Live Tests');
  console.log('==================================================\n');

  // --------------------------------------------------
  // 1. COORDINATE VALIDATION TESTS
  // --------------------------------------------------
  console.log('--- 1. Coordinate Validation Tests ---');

  assert.strictEqual(weatherService.isValidCoordinates(18.5204, 73.8567), true, 'Valid coordinates must pass');
  assert.strictEqual(weatherService.isValidCoordinates(0, 0), true, 'Equator/Prime Meridian must pass');
  assert.strictEqual(weatherService.isValidCoordinates(-90, -180), true, 'Min bounds must pass');
  assert.strictEqual(weatherService.isValidCoordinates(90, 180), true, 'Max bounds must pass');

  assert.strictEqual(weatherService.isValidCoordinates(91, 73.8567), false, 'Latitude > 90 must fail');
  assert.strictEqual(weatherService.isValidCoordinates(-91, 73.8567), false, 'Latitude < -90 must fail');
  assert.strictEqual(weatherService.isValidCoordinates(18.5204, 181), false, 'Longitude > 180 must fail');
  assert.strictEqual(weatherService.isValidCoordinates(18.5204, -181), false, 'Longitude < -180 must fail');
  assert.strictEqual(weatherService.isValidCoordinates('invalid', 73.8567), false, 'String latitude must fail');
  assert.strictEqual(weatherService.isValidCoordinates(NaN, 73.8567), false, 'NaN latitude must fail');
  console.log('✔ Coordinate validation correctly validates range [-90..90], [-180..180]');

  // --------------------------------------------------
  // 2. WMO CONDITION NORMALIZATION TESTS
  // --------------------------------------------------
  console.log('\n--- 2. WMO Condition Normalization Tests ---');

  assert.strictEqual(weatherService.normalizeWmoCode(0), 'CLEAR');
  assert.strictEqual(weatherService.normalizeWmoCode(1), 'CLEAR');
  assert.strictEqual(weatherService.normalizeWmoCode(2), 'CLOUDY');
  assert.strictEqual(weatherService.normalizeWmoCode(3), 'CLOUDY');
  assert.strictEqual(weatherService.normalizeWmoCode(45), 'FOG');
  assert.strictEqual(weatherService.normalizeWmoCode(48), 'FOG');
  assert.strictEqual(weatherService.normalizeWmoCode(61), 'RAIN');
  assert.strictEqual(weatherService.normalizeWmoCode(80), 'RAIN');
  assert.strictEqual(weatherService.normalizeWmoCode(65), 'HEAVY_RAIN');
  assert.strictEqual(weatherService.normalizeWmoCode(95), 'HEAVY_RAIN');
  assert.strictEqual(weatherService.normalizeWmoCode(999), 'CLEAR', 'Unknown WMO code should fallback to CLEAR');
  console.log('✔ WMO codes mapped to CLEAR, CLOUDY, RAIN, HEAVY_RAIN, FOG');

  // --------------------------------------------------
  // 3. WEATHER RISK CALCULATION & MODIFIERS
  // --------------------------------------------------
  console.log('\n--- 3. Weather Risk Calculation & Modifiers ---');

  // Base conditions
  const clearRisk = weatherService.calculateWeatherRiskScore({ condition: 'CLEAR', visibilityKm: 10, precipitationMm: 0, windSpeedKmh: 5 });
  assert.strictEqual(clearRisk, 10, 'Base CLEAR risk must be 10');

  const cloudyRisk = weatherService.calculateWeatherRiskScore({ condition: 'CLOUDY', visibilityKm: 10, precipitationMm: 0, windSpeedKmh: 10 });
  assert.strictEqual(cloudyRisk, 20, 'Base CLOUDY risk must be 20');

  const rainRisk = weatherService.calculateWeatherRiskScore({ condition: 'RAIN', visibilityKm: 10, precipitationMm: 2, windSpeedKmh: 10 });
  assert.strictEqual(rainRisk, 55, 'Base RAIN risk must be 55');

  const heavyRainRisk = weatherService.calculateWeatherRiskScore({ condition: 'HEAVY_RAIN', visibilityKm: 10, precipitationMm: 2, windSpeedKmh: 10 });
  assert.strictEqual(heavyRainRisk, 80, 'Base HEAVY_RAIN risk must be 80');

  const fogRisk = weatherService.calculateWeatherRiskScore({ condition: 'FOG', visibilityKm: 10, precipitationMm: 0, windSpeedKmh: 5 });
  assert.strictEqual(fogRisk, 75, 'Base FOG risk must be 75');
  console.log('✔ Base weather conditions match required risk values: CLEAR=10, CLOUDY=20, RAIN=55, HEAVY_RAIN=80, FOG=75');

  // Modifiers: Low visibility
  const lowVisRisk = weatherService.calculateWeatherRiskScore({ condition: 'CLEAR', visibilityKm: 0.8, precipitationMm: 0, windSpeedKmh: 5 });
  assert.strictEqual(lowVisRisk, 10 + 20, 'Very low visibility (<1km) adds +20 penalty');

  // Modifiers: Heavy precipitation
  const heavyPrecipRisk = weatherService.calculateWeatherRiskScore({ condition: 'RAIN', visibilityKm: 10, precipitationMm: 20, windSpeedKmh: 10 });
  assert.strictEqual(heavyPrecipRisk, 55 + 20, 'Torrential precipitation (>15mm) adds +20 penalty');

  // Extreme condition clamping
  const extremeWeather = weatherService.calculateWeatherRiskScore({
    condition: 'HEAVY_RAIN', // 80
    visibilityKm: 0.5,       // +20
    precipitationMm: 25.0,   // +20
    windSpeedKmh: 60.0,      // +15 -> Total 135 -> Clamped to 100
  });
  assert.strictEqual(extremeWeather, 100, 'Score must never exceed 100');
  console.log('✔ Weather modifiers correctly penalize low visibility, heavy downpours, and high winds (clamped 0..100)');

  // --------------------------------------------------
  // 4. WEATHER EXPLANATION TESTS
  // --------------------------------------------------
  console.log('\n--- 4. Weather Explanation Tests ---');

  const heavyRainExp = weatherService.getWeatherExplanation({ condition: 'HEAVY_RAIN' });
  assert.strictEqual(heavyRainExp, 'Heavy rain is increasing road risk.');

  const lowVisExp = weatherService.getWeatherExplanation({ condition: 'CLOUDY', visibilityKm: 1.5 });
  assert.strictEqual(lowVisExp, 'Low visibility is increasing road risk.');

  const fogExp = weatherService.getWeatherExplanation({ condition: 'FOG', visibilityKm: 5.0 });
  assert.strictEqual(fogExp, 'Foggy conditions and reduced sightlines ahead.');
  console.log('✔ Human-readable explanations generated dynamically from meteorological metrics');

  // --------------------------------------------------
  // 5. CACHING BEHAVIOR TESTS
  // --------------------------------------------------
  console.log('\n--- 5. In-Memory Caching Tests ---');

  const key1 = weatherService.getCacheKey(18.520412, 73.856743);
  const key2 = weatherService.getCacheKey(18.520489, 73.856711);
  assert.strictEqual(key1, key2, 'Nearby coordinates (~1 km) must produce identical rounded cache key');
  console.log(`✔ Cache key rounding verified: ${key1}`);

  // --------------------------------------------------
  // 6. LIVE METEOROLOGICAL PROVIDER CALL
  // --------------------------------------------------
  console.log('\n--- 6. Live Meteorological Provider Call ---');

  console.log('Calling Open-Meteo for Pune coordinates (18.5204, 73.8567)...');
  const liveWeather = await weatherService.getCurrentWeather(18.5204, 73.8567);
  console.log('✔ Live Weather Received:');
  console.log(`   - Temperature:    ${liveWeather.temperatureC} °C`);
  console.log(`   - Condition:      ${liveWeather.condition}`);
  console.log(`   - Precipitation:  ${liveWeather.precipitationMm} mm`);
  console.log(`   - Humidity:       ${liveWeather.humidity} %`);
  console.log(`   - Wind Speed:     ${liveWeather.windSpeedKmh} km/h`);
  console.log(`   - Visibility:     ${liveWeather.visibilityKm} km`);
  console.log(`   - Observed At:    ${liveWeather.observedAt}`);
  console.log(`   - Cached:         ${liveWeather.isCached}`);

  assert(typeof liveWeather.temperatureC === 'number', 'temperatureC must be numeric');
  assert(['CLEAR', 'CLOUDY', 'RAIN', 'HEAVY_RAIN', 'FOG'].includes(liveWeather.condition), 'Condition must be normalized SMARTMOVE condition');
  assert(liveWeather.observedAt, 'observedAt timestamp must exist');

  // Immediate second call should hit in-memory cache
  const cachedWeather = await weatherService.getCurrentWeather(18.5204, 73.8567);
  assert.strictEqual(cachedWeather.isCached, true, 'Immediate subsequent request must hit cache');
  console.log('✔ Second request served from in-memory cache (isCached: true)');

  console.log('\n==================================================');
  console.log('  ✅ ALL WEATHER SERVICE TESTS PASSED SUCCESSFULLY');
  console.log('==================================================\n');
}

testWeatherEngine().catch((err) => {
  console.error('\n❌ Weather Engine Test Failed:', err);
  process.exit(1);
});
