// ---------------------------------------------------------------------------
// Kotlin <-> JS bridge
//
// MainActivity exposes a Kotlin object as `window.javaBridge` (with
// addJavascriptInterface) and calls window.onJavaBridgeReady() once the page
// has loaded. Everything in this file goes through the `desk` facade below,
// so the page also works when opened in a normal browser (no Android): calls
// are just logged to the browser console.
// ---------------------------------------------------------------------------
const desk = {
  get bridge() {
    return window.javaBridge || null;
  },
  // MQTT-style publish: Kotlin side logs it (and can forward it to a real broker)
  publish(topic, data) {
    const json = JSON.stringify(data);
    if (this.bridge) this.bridge.publish(topic, json);
    else console.log('[publish]', topic, json);
  },
};

// Called by Kotlin (MainActivity.onPageFinished) once the page has loaded
window.onJavaBridgeReady = function () {
  // Forward console.log / console.error to Logcat
  const origLog = console.log;
  const origError = console.error;
  console.log = (...args) => { desk.bridge.log('INFO', args.join(' ')); origLog(...args); };
  console.error = (...args) => { desk.bridge.log('ERROR', args.join(' ')); origError(...args); };
  window.onerror = (msg, src, line) => desk.bridge.log('ERROR', `${msg} (${src}:${line})`);

  console.log('JS bridge ready');
};

// Called by Kotlin (MainActivity -> window.onSensorUpdate(...)) to push data into the UI
window.onSensorUpdate = function (temperature, humidity) {
  document.getElementById('tempVal').textContent = `${temperature.toFixed(1)}°C`;
  document.getElementById('humVal').textContent = `${Math.round(humidity)}%`;
};

// Called by Kotlin for the Android back key / gesture.
// Returns true if it closed the detail panel, false to let Android leave the app.
window.handleBack = function () {
  if (openDetailType === null) return false;
  closeDetail();
  return true;
};

// ---------------------------------------------------------------------------
// Widget logic
// ---------------------------------------------------------------------------

// Handle Toggle Interactions with visual feedback and MQTT messages
function handleToggle(type, checkbox, topic) {
  const isChecked = checkbox.checked;
  const card = document.getElementById(`card-${type}`);

  // Visual active class
  card.classList.toggle('is-on', isChecked);

  // Dynamic sub-text updates
  if (type === 'bluetooth') {
    document.getElementById('bt-status').textContent = isChecked ? 'Audio Engine Connected' : 'Disconnected / Standby';
  }

  // Specific rule for Privacy guard: if ON, drop mic/cam
  if (type === 'privacy' && isChecked) {
    flashNotice('Privacy Mode Activated: Mic & Cam Cutoff');
  }

  // Dim light slider if lights are turned off
  if (type === 'light') {
    const sliderMod = document.getElementById('lightBrightnessModule');
    sliderMod.style.opacity = isChecked ? '1' : '0.4';
    sliderMod.style.pointerEvents = isChecked ? 'auto' : 'none';
  }

  const payload = {
    state: isChecked ? 'ON' : 'OFF',
    timestamp: new Date().toLocaleTimeString(),
    source: 'android-desk-widget'
  };

  publishLog(topic, payload);
  recalcPower();

  if (openDetailType === type) refreshDetailState();
}

// Handle Brightness Slider Change
let debounceTimer;
function handleSliderChange(val) {
  document.getElementById('sliderVal').textContent = `${val}%`;

  clearTimeout(debounceTimer);
  debounceTimer = setTimeout(() => {
    publishLog('office/desk/lights/dim', { brightness: parseInt(val, 10) });
  }, 150);
}

// Show in the on-screen log and send to Kotlin
function publishLog(topic, data) {
  const logElement = document.getElementById('mqttPayload');
  logElement.innerHTML = `<strong>${topic}</strong> -> ${JSON.stringify(data)}`;

  // Ping flicker
  const pingElement = document.getElementById('pingMs');
  const randomPing = Math.floor(Math.random() * 8) + 11;
  pingElement.textContent = `ping: ${randomPing}ms`;

  desk.publish(topic, data);
}

// Quick Scene Presets
function applyScene(sceneName) {
  document.querySelectorAll('.scene-chip').forEach(btn => btn.classList.remove('active'));
  document.getElementById(`scene-${sceneName}`).classList.add('active');

  const bt = document.getElementById('bluetooth');
  const light = document.getElementById('deskLight');
  const rig = document.getElementById('smartPlug');
  const privacy = document.getElementById('privacySwitch');
  const slider = document.getElementById('lightSlider');

  if (sceneName === 'work') {
    bt.checked = true;
    light.checked = true;
    rig.checked = true;
    privacy.checked = false;
    slider.value = 85;
  } else if (sceneName === 'meet') {
    bt.checked = true;
    light.checked = true;
    rig.checked = true;
    privacy.checked = false;
    slider.value = 100;
  } else if (sceneName === 'leave') {
    bt.checked = false;
    light.checked = false;
    rig.checked = false;
    privacy.checked = true;
    slider.value = 0;
  }

  // Trigger updates on all
  handleToggle('bluetooth', bt, 'office/desk/soundbar');
  handleToggle('light', light, 'office/desk/lights');
  handleToggle('rig', rig, 'office/desk/plug_main');
  handleToggle('privacy', privacy, 'office/desk/privacy_guard');
  handleSliderChange(slider.value);

  publishLog(`office/scenes/set`, { preset: sceneName.toUpperCase() });
}

// Dynamic Power calculation simulation
function recalcPower() {
  let watts = 0;
  if (document.getElementById('bluetooth').checked) watts += 14;
  if (document.getElementById('smartPlug').checked) watts += 115;
  if (document.getElementById('deskLight').checked) {
    const bright = parseInt(document.getElementById('lightSlider').value, 10);
    watts += Math.round(18 * (bright / 100));
  }
  document.getElementById('powerDraw').textContent = `${watts}W`;
}

// Simple toast notice inside the log
function flashNotice(msg) {
  publishLog('system/widget/event', { notice: msg });
}

// ---------------------------------------------------------------------------
// Detail panel (full-screen layer sliding in from the right)
// ---------------------------------------------------------------------------

let openDetailType = null;

// Tapping a card (but not its switch or slider) opens its details
document.getElementById('deskWidget').addEventListener('click', (e) => {
  const card = e.target.closest('.switch-holder[data-detail]');
  if (!card || e.target.closest('.switch-toggle, .slider-module')) return;
  openDetail(card.dataset.detail);
});

function openDetail(type) {
  const template = document.getElementById(`detail-${type}`);
  const card = document.getElementById(`card-${type}`);
  if (!template || !card) return;

  // Fill the panel from the template, title and icon come from the card
  const body = document.getElementById('detailBody');
  body.replaceChildren(template.content.cloneNode(true));
  body.scrollTop = 0;

  document.getElementById('detailTitle').textContent = card.querySelector('.switch-info span').textContent;
  document.getElementById('detailIcon').className = card.querySelector('.icon-box i').className;

  document.querySelectorAll('.switch-holder.is-selected').forEach(c => c.classList.remove('is-selected'));
  card.classList.add('is-selected');

  openDetailType = type;
  refreshDetailState();
  document.body.classList.add('detail-open'); // CSS transition slides the panel in

  publishLog('system/widget/detail', { open: type });
}

// 'Geri' button and the Android back key (window.handleBack)
function closeDetail() {
  if (openDetailType === null) return;
  openDetailType = null;
  document.querySelectorAll('.switch-holder.is-selected').forEach(c => c.classList.remove('is-selected'));
  document.body.classList.remove('detail-open');
}

// When the slide animation has finished
document.getElementById('detailColumn').addEventListener('transitionend', (e) => {
  // Ignore transitions of the controls inside and the visibility transition
  if (e.target !== e.currentTarget || e.propertyName !== 'transform') return;
  if (openDetailType !== null) {
    console.log(`Detail panel open: ${openDetailType}`);
  } else {
    // Clear the panel only if nothing was reopened during the animation
    document.getElementById('detailBody').replaceChildren();
  }
});

// Values in the detail panel that mirror the main panel's switches
function refreshDetailState() {
  if (openDetailType === 'privacy') {
    const on = document.getElementById('privacySwitch').checked;
    document.querySelectorAll('[data-privacy-pill]').forEach(pill => {
      pill.textContent = on ? 'BLOCKED' : 'LIVE';
      pill.className = `pill ${on ? 'on' : 'off'}`;
    });
  }
}

// Controls inside the detail panel publish through data-topic attributes (see index.html)
const detailBody = document.getElementById('detailBody');

detailBody.addEventListener('click', (e) => {
  const btn = e.target.closest('.seg-btn');
  if (!btn) return;
  const group = btn.closest('.seg');
  group.querySelectorAll('.seg-btn').forEach(b => b.classList.toggle('active', b === btn));
  publishLog(group.dataset.topic, { value: btn.dataset.value });
});

detailBody.addEventListener('change', (e) => {
  const input = e.target;
  if (input.type === 'checkbox' && input.dataset.topic) {
    publishLog(input.dataset.topic, { state: input.checked ? 'ON' : 'OFF' });
  }
});

let detailSliderTimer;
detailBody.addEventListener('input', (e) => {
  const input = e.target;
  if (input.type !== 'range' || !input.dataset.topic) return;
  const label = input.closest('.slider-module').querySelector('.slider-val');
  if (label) label.textContent = `${input.value}%`;
  clearTimeout(detailSliderTimer);
  detailSliderTimer = setTimeout(() => {
    publishLog(input.dataset.topic, { value: parseInt(input.value, 10) });
  }, 150);
});

window.addEventListener('load', recalcPower);
