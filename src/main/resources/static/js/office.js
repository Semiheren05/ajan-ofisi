// Ajan Ofisi: draws Claude Code's main agent ("boss") and its subagents in a pixel-art office.
// Events come from the API (history + Server-Sent Events) or from the built-in demo script.
'use strict';

const W = 240, H = 135, SCALE = 4;
const SPEED = 46;                 // walking speed, logical pixels per second
const DOOR = { x: 236, y: 58 };
const HALL_X = 226;               // where people turn after the door
const CORRIDOR_TOP = 58, CORRIDOR_BOTTOM = 96;
const COFFEE = { x: 22, y: 58 };
const BOSS_DESK = { x: 120, y: 34 };
const DESKS = [
  { x: 56, y: 72 }, { x: 120, y: 72 }, { x: 184, y: 72 },
  { x: 56, y: 108 }, { x: 120, y: 108 }, { x: 184, y: 108 },
];
// Overflow places when more than six subagents work at once: they stand around the meeting table
const STANDING = [{ x: 88, y: 128 }, { x: 152, y: 128 }, { x: 216, y: 128 }, { x: 24, y: 128 }];

const SKIN = ['#f2c9a0', '#e0ac7e', '#c68642', '#8d5524'];
const HAIR = ['#2c2c2a', '#633806', '#ba7517', '#888780', '#712b13'];
const SHIRTS = ['#1d9e75', '#d85a30', '#7f77dd', '#378add', '#d4537e', '#639922', '#ba7517', '#0f6e56'];
const PANTS = '#3c3c55';

const VERBS = {
  Read: 'Okuyor', Edit: 'Düzenliyor', MultiEdit: 'Düzenliyor', Write: 'Yazıyor', NotebookEdit: 'Not defteri',
  Bash: 'Komut', Grep: 'Arıyor', Glob: 'Dosya arıyor', WebSearch: "Web'de arıyor", WebFetch: 'Sayfa okuyor',
  Agent: 'Görev veriyor', Task: 'Görev veriyor', TodoWrite: 'Plan yapıyor',
};

const canvas = document.getElementById('office');
const ctx = canvas.getContext('2d');
ctx.setTransform(SCALE, 0, 0, SCALE, 0, 0);
const stage = document.getElementById('stage');
const logList = document.getElementById('log');
const statusEl = document.getElementById('status');
const projectEl = document.getElementById('project');
const demoButton = document.getElementById('demo');

const state = {
  sessionId: null,
  open: false,          // lights on while a session is running
  boss: null,
  agents: new Map(),    // agentId -> person
  leaving: new Set(),   // finished agents on their way out
  seats: Array(DESKS.length + STANDING.length).fill(null),
  mode: 'live',         // 'live' or 'demo'
  time: 0,
};

// ---------- drawing helpers ----------

function rect(x, y, w, h, color) {
  ctx.fillStyle = color;
  ctx.fillRect(x, y, w, h);
}

function hash(text) {
  let h = 0;
  for (const ch of text || '') h = (h * 31 + ch.charCodeAt(0)) >>> 0;
  return h;
}

function isNight(date = new Date()) {
  const hour = date.getHours();
  return hour < 7 || hour >= 19;
}

function skyColor(date = new Date()) {
  const hour = date.getHours();
  if (hour >= 7 && hour < 17) return '#7fb8e8';
  if (hour >= 17 && hour < 19) return '#f0997b';
  if (hour >= 5 && hour < 7) return '#afa9ec';
  return '#1c2a4a';
}

// ---------- the room ----------

function drawFloor() {
  for (let y = 22; y < H; y += 6) {
    const row = (y - 22) / 6;
    rect(0, y, W, 6, row % 2 ? '#c8a77a' : '#c2a073');
    for (let x = (row % 3) * 14; x < W; x += 42) rect(x, y, 1, 6, '#a98a60');
  }
  rect(96, 24, 48, 26, '#b5d4f4');            // boss rug
  rect(97, 25, 46, 24, '#85b7eb');
  rect(60, 118, 120, 16, '#9fe1cb');           // meeting rug
  rect(61, 119, 118, 14, '#5dcaa5');
}

function drawWall(now) {
  rect(0, 0, W, 22, '#e8dcc8');
  rect(0, 20, W, 2, '#b89f80');
  const sky = skyColor(now);
  for (const wx of [24, 180]) {
    rect(wx - 1, 3, 38, 15, '#b89f80');
    rect(wx, 4, 36, 13, sky);
    if (isNight(now)) {
      rect(wx + 5, 6, 1, 1, '#fff'); rect(wx + 20, 8, 1, 1, '#fff'); rect(wx + 29, 5, 1, 1, '#fff');
      rect(wx + 26, 10, 3, 3, '#fac775');      // moon
    } else {
      rect(wx + 4, 6, 7, 2, '#fff'); rect(wx + 6, 5, 4, 1, '#fff');   // cloud
    }
    rect(wx + 17, 4, 2, 13, '#f1efe8');
    rect(wx, 10, 36, 1, '#f1efe8');
  }
  // whiteboard with the "plan"
  rect(76, 4, 32, 14, '#888780');
  rect(77, 5, 30, 12, '#ffffff');
  rect(79, 7, 10, 1, '#378add'); rect(79, 10, 16, 1, '#d85a30'); rect(79, 13, 8, 1, '#1d9e75');
  rect(99, 8, 5, 5, '#7f77dd');
  // clock with the real hour hand
  rect(116, 4, 9, 9, '#444441');
  rect(117, 5, 7, 7, '#ffffff');
  rect(120, 8, 1, 1, '#2c2c2a');
  const angle = ((now.getHours() % 12) + now.getMinutes() / 60) / 12 * Math.PI * 2;
  rect(120 + Math.round(Math.sin(angle) * 2), 8 - Math.round(Math.cos(angle) * 2), 1, 1, '#a32d2d');
  const minAngle = now.getMinutes() / 60 * Math.PI * 2;
  rect(120 + Math.round(Math.sin(minAngle) * 3), 8 - Math.round(Math.cos(minAngle) * 3), 1, 1, '#2c2c2a');
  // poster
  rect(140, 5, 22, 13, '#26215c');
  rect(142, 7, 6, 6, '#ef9f27'); rect(150, 8, 10, 1, '#cecbf6'); rect(150, 11, 7, 1, '#cecbf6');
}

function drawFurniture() {
  // coffee corner
  rect(3, 40, 12, 22, '#5f5e5a');
  rect(4, 41, 10, 6, '#2c2c2a');
  rect(6, 43, 2, 1, state.open ? '#e24b4a' : '#444441');
  rect(6, 50, 6, 6, '#444441');
  rect(8, 54, 2, 2, '#f1efe8');
  rect(2, 62, 14, 2, '#888780');
  // plant
  rect(4, 76, 7, 7, '#993c1d');
  rect(2, 68, 11, 8, '#3b6d11');
  rect(4, 65, 6, 4, '#639922');
  // water cooler
  rect(5, 92, 8, 14, '#d3d1c7');
  rect(6, 86, 6, 7, '#85b7eb');
  // bookshelf
  rect(214, 24, 22, 12, '#6e4630');
  rect(216, 26, 3, 8, '#d85a30'); rect(220, 26, 2, 8, '#378add'); rect(223, 27, 3, 7, '#1d9e75');
  rect(227, 26, 2, 8, '#ef9f27'); rect(230, 28, 4, 6, '#7f77dd');
  // door
  rect(229, 38, 11, 24, '#5a3a22');
  rect(230, 39, 10, 22, '#7a4a2a');
  rect(231, 50, 1, 2, '#ffd27a');
  // sofa
  rect(3, 118, 34, 6, '#993556');
  rect(3, 124, 34, 6, '#d4537e');
  rect(3, 130, 3, 3, '#4b1528'); rect(34, 130, 3, 3, '#4b1528');
  // meeting table
  rect(104, 122, 32, 6, '#a06a46');
  rect(104, 128, 32, 2, '#6e4630');
}

function drawChair(cx, y) {
  rect(cx - 10, y - 8, 10, 5, '#444441');
  rect(cx - 9, y - 7, 8, 3, '#5f5e5a');
}

function drawDesk(cx, y, on, wide) {
  const half = wide ? 16 : 12;
  rect(cx + 2, y - 7, 10, 7, '#2b2b33');
  const blink = Math.floor(state.time * 4) % 2;
  rect(cx + 3, y - 6, 8, 4, on ? (blink ? '#5dcaa5' : '#9fe1cb') : '#3a3f4a');
  if (on) {
    rect(cx + 4, y - 5, 1 + (Math.floor(state.time * 6) % 6), 1, '#04342c');
    rect(cx + 4, y - 3, 3, 1, '#04342c');
  }
  rect(cx + 6, y - 1, 2, 1, '#2b2b33');
  rect(cx - half, y, half * 2, 2, '#a06a46');
  rect(cx - half, y + 2, half * 2, 7, '#6e4630');
  rect(cx - half + 1, y + 9, 2, 3, '#4a2e1f');
  rect(cx + half - 3, y + 9, 2, 3, '#4a2e1f');
  rect(cx - half + 3, y - 2, 4, 2, '#f5f0e0');            // papers
  if (wide) {
    rect(cx - 13, y - 4, 3, 4, '#993c1d'); rect(cx - 14, y - 7, 5, 3, '#639922');   // tiny plant
    rect(cx - 6, y + 4, 12, 3, '#fac775');                                            // nameplate
  }
}

function drawPerson(p) {
  const px = Math.round(p.x) - 4, py = Math.round(p.y) - 12;
  rect(px + 1, py, 6, 2, p.hair);
  rect(px + 1, py + 2, 6, 4, p.skin);
  rect(px + 1, py + 2, 1, 2, p.hair);
  if (p.boss) rect(px + 2, py - 1, 4, 1, '#fac775');   // the boss wears a little crown
  const blink = (state.time * 10 + p.seed) % 40 < 1;
  if (!blink) { rect(px + 2, py + 4, 1, 1, '#222'); rect(px + 5, py + 4, 1, 1, '#222'); }
  rect(px, py + 6, 8, 4, p.shirt);
  if (p.boss) rect(px + 3, py + 6, 2, 3, '#a32d2d');   // tie

  const step = Math.floor(state.time * 8) % 2;
  if (p.path.length) {
    rect(px + 1, py + 10, 2, step ? 2 : 1, PANTS);
    rect(px + 5, py + 10, 2, step ? 1 : 2, PANTS);
    rect(px - 1, py + 7 + step, 1, 2, p.skin);
    rect(px + 8, py + 8 - step, 1, 2, p.skin);
    return;
  }
  rect(px + 1, py + 10, 2, 2, PANTS);
  rect(px + 5, py + 10, 2, 2, PANTS);
  if (p.pose === 'type') {
    const tap = Math.floor(state.time * 10) % 2;
    rect(px - 1, py + 7 + tap, 1, 2, p.skin);
    rect(px + 8, py + 8 - tap, 1, 2, p.skin);
  } else if (p.pose === 'hand') {           // waiting for the user: hand up
    rect(px - 1, py + 1, 1, 5, p.skin);
    rect(px + 8, py + 7, 1, 2, p.skin);
  } else if (p.pose === 'coffee') {
    rect(px - 1, py + 7, 1, 2, p.skin);
    rect(px + 8, py + 6, 2, 3, '#f1efe8');
    rect(px + 8, py + 5, 2, 1, '#6e4630');
  } else {
    rect(px - 1, py + 7, 1, 2, p.skin);
    rect(px + 8, py + 7, 1, 2, p.skin);
  }
}

function drawLights(now) {
  if (!state.open) {
    ctx.fillStyle = 'rgba(8, 10, 28, 0.6)';
    ctx.fillRect(0, 0, W, H);
    return;
  }
  if (isNight(now)) {
    ctx.fillStyle = 'rgba(30, 30, 80, 0.12)';
    ctx.fillRect(0, 22, W, H - 22);
  }
}

// ---------- people ----------

function makePerson({ boss = false, key = '' }) {
  const h = hash(key);
  const person = {
    boss,
    x: DOOR.x, y: DOOR.y,
    path: [], resolve: null,
    pose: 'idle',
    seat: null,
    seed: h % 97,
    skin: SKIN[h % SKIN.length],
    hair: HAIR[(h >> 3) % HAIR.length],
    shirt: boss ? '#444441' : SHIRTS[(h >> 6) % SHIRTS.length],
    bubble: document.createElement('div'),
  };
  person.bubble.className = 'bubble';
  stage.appendChild(person.bubble);
  return person;
}

function removePerson(person) {
  person.bubble.remove();
}

function say(person, name, text, kind = '') {
  person.lastSay = text;
  person.bubble.className = 'bubble' + (kind ? ' ' + kind : '');
  person.bubble.replaceChildren();
  const title = document.createElement('b');
  title.textContent = name;
  const line = document.createElement('span');
  line.textContent = text;
  person.bubble.append(title, line);
}

function walk(person, points, instant) {
  if (instant) {
    const last = points[points.length - 1];
    person.x = last[0];
    person.y = last[1];
    person.path = [];
    return Promise.resolve();
  }
  person.path = points.map(([x, y]) => ({ x, y }));
  return new Promise(resolve => { person.resolve = resolve; });
}

function move(person, dt) {
  if (!person.path.length) return;
  let budget = SPEED * dt;
  while (budget > 0 && person.path.length) {
    const target = person.path[0];
    const dx = target.x - person.x, dy = target.y - person.y;
    if (dx !== 0) {
      const stepX = Math.min(Math.abs(dx), budget);
      person.x += Math.sign(dx) * stepX;
      budget -= stepX;
    } else if (dy !== 0) {
      const stepY = Math.min(Math.abs(dy), budget);
      person.y += Math.sign(dy) * stepY;
      budget -= stepY;
    }
    if (person.x === target.x && person.y === target.y) person.path.shift();
  }
  if (!person.path.length && person.resolve) {
    const done = person.resolve;
    person.resolve = null;
    done();
  }
}

function placeBubble(person) {
  person.bubble.style.left = (person.x / W * 100) + '%';
  person.bubble.style.top = ((person.y - 14) / H * 100) + '%';
}

// ---------- seats and routes ----------

function seatSpot(index) {
  if (index < DESKS.length) {
    const desk = DESKS[index];
    return { x: desk.x - 4, y: desk.y + 2, corridor: desk.y < 100 ? CORRIDOR_TOP : CORRIDOR_BOTTOM };
  }
  const spot = STANDING[(index - DESKS.length) % STANDING.length];
  return { x: spot.x, y: spot.y, corridor: CORRIDOR_BOTTOM };
}

function takeSeat(agentId) {
  let index = state.seats.indexOf(null);
  if (index === -1) index = state.seats.length - 1;   // everything taken: share the last standing spot
  state.seats[index] = agentId;
  return index;
}

function freeSeat(agentId) {
  const index = state.seats.indexOf(agentId);
  if (index !== -1) state.seats[index] = null;
}

function routeFromDoor(spot) {
  if (spot.corridor === CORRIDOR_TOP) {
    return [[HALL_X, CORRIDOR_TOP], [spot.x, CORRIDOR_TOP], [spot.x, spot.y]];
  }
  return [[HALL_X, CORRIDOR_TOP], [HALL_X, CORRIDOR_BOTTOM], [spot.x, CORRIDOR_BOTTOM], [spot.x, spot.y]];
}

// Everyone gets their own spot by the coffee machine, so finished agents do not stand in one pile
function routeToCoffee(spot, seat) {
  const cup = { x: COFFEE.x + (seat % 3) * 12, y: COFFEE.y + (seat % 2) * 6 };
  if (spot.corridor === CORRIDOR_TOP) {
    return [[spot.x, CORRIDOR_TOP], [cup.x, CORRIDOR_TOP], [cup.x, cup.y]];
  }
  return [[spot.x, CORRIDOR_BOTTOM], [cup.x, CORRIDOR_BOTTOM], [cup.x, cup.y]];
}

const routeOut = () => [[HALL_X, CORRIDOR_TOP], [DOOR.x, DOOR.y]];
const bossSeat = () => ({ x: BOSS_DESK.x - 4, y: BOSS_DESK.y + 2 });
const sleep = (ms, instant) => instant ? Promise.resolve() : new Promise(r => setTimeout(r, ms));

// ---------- reacting to events ----------

function agentName(agentType) {
  return agentType || 'Alt ajan';
}

function toolText(event) {
  const verb = VERBS[event.toolName] || event.toolName || 'Çalışıyor';
  return event.detail ? `${verb}: ${event.detail}` : verb;
}

async function bossArrives(instant) {
  if (state.boss) return;
  const boss = makePerson({ boss: true, key: 'boss' });
  state.boss = boss;
  say(boss, 'Ana ajan', 'Ofise geliyor');
  const seat = bossSeat();
  await walk(boss, [[HALL_X, CORRIDOR_TOP], [seat.x, CORRIDOR_TOP], [seat.x, seat.y]], instant);
  // Only if nothing happened on the way in: a prompt that arrived meanwhile keeps its bubble
  if (boss.lastSay === 'Ofise geliyor') say(boss, 'Ana ajan', 'Hazır');
}

async function agentArrives(event, instant) {
  if (state.agents.has(event.agentId)) return state.agents.get(event.agentId);
  const person = makePerson({ key: event.agentType + event.agentId });
  person.name = agentName(event.agentType);
  person.seat = takeSeat(event.agentId);
  state.agents.set(event.agentId, person);
  say(person, person.name, 'Masasına geçiyor');
  const spot = seatSpot(person.seat);
  await walk(person, routeFromDoor(spot), instant);
  if (person.leaving) return person;
  person.pose = 'type';
  if (person.lastSay === 'Masasına geçiyor') say(person, person.name, 'İşe başladı');
  return person;
}

async function agentLeaves(event, instant) {
  const person = state.agents.get(event.agentId);
  if (!person) return;
  state.agents.delete(event.agentId);
  person.leaving = true;
  const spot = seatSpot(person.seat);
  freeSeat(event.agentId);
  if (instant) {
    removePerson(person);
    return;
  }
  state.leaving.add(person);
  person.pose = 'idle';
  say(person, person.name, 'Bitti' + (event.detail ? ': ' + event.detail : ''), 'done');
  await sleep(1800);
  await walk(person, routeToCoffee(spot, person.seat));
  person.pose = 'coffee';
  person.bubble.replaceChildren();   // the cup says it all, and a quiet corner keeps the bubbles readable
  await sleep(1600);
  await walk(person, routeOut());
  state.leaving.delete(person);
  removePerson(person);
}

function everyoneLeaves(instant) {
  for (const id of [...state.agents.keys()]) agentLeaves({ agentId: id }, true);
  const boss = state.boss;
  state.boss = null;
  if (!boss) return Promise.resolve();
  if (instant) {
    removePerson(boss);
    return Promise.resolve();
  }
  state.leaving.add(boss);
  say(boss, 'Ana ajan', 'Işıkları kapatıyor');
  return walk(boss, routeOut()).then(() => {
    state.leaving.delete(boss);
    removePerson(boss);
  });
}

function resetScene() {
  for (const person of state.agents.values()) removePerson(person);
  for (const person of state.leaving) removePerson(person);
  state.agents.clear();
  state.leaving.clear();
  state.seats.fill(null);
  if (state.boss) removePerson(state.boss);
  state.boss = null;
  state.open = false;
}

async function handle(event, instant = false) {
  if (state.sessionId !== event.sessionId) {
    // A new Claude Code session takes over the office
    resetScene();
    state.sessionId = event.sessionId;
    projectEl.textContent = '';
  }
  if (!instant) addLog(event);

  switch (event.type) {
    case 'SESSION_STARTED':
      state.open = true;
      if (event.detail) projectEl.textContent = event.detail;
      await bossArrives(instant);
      break;
    case 'PROMPT':
      state.open = true;
      await bossArrives(instant);
      state.boss.pose = 'type';
      say(state.boss, 'Ana ajan', 'Yeni görev: ' + (event.detail || ''));
      break;
    case 'TOOL':
      state.open = true;
      if (event.agentId) {
        const person = state.agents.get(event.agentId) || await agentArrives(event, instant);
        if (!person.leaving) say(person, person.name, toolText(event));
      } else {
        await bossArrives(instant);
        state.boss.pose = 'type';
        say(state.boss, 'Ana ajan', toolText(event));
      }
      break;
    case 'AGENT_STARTED':
      state.open = true;
      await agentArrives(event, instant);
      break;
    case 'AGENT_STOPPED':
      await agentLeaves(event, instant);
      break;
    case 'WAITING':
      await bossArrives(instant);
      state.boss.pose = 'hand';
      say(state.boss, 'Ana ajan', 'Seni bekliyor' + (event.detail ? ': ' + event.detail : ''), 'wait');
      break;
    case 'STOPPED':
      await bossArrives(instant);
      state.boss.pose = 'idle';
      say(state.boss, 'Ana ajan', 'Cevap hazır', 'done');
      break;
    case 'SESSION_ENDED':
      await everyoneLeaves(instant);
      state.open = false;
      break;
  }
}

// ---------- the log under the office ----------

const LOG_LABELS = {
  SESSION_STARTED: 'Oturum açıldı', PROMPT: 'Yeni görev', TOOL: 'Araç', AGENT_STARTED: 'Alt ajan geldi',
  AGENT_STOPPED: 'Alt ajan bitirdi', WAITING: 'Onay bekliyor', STOPPED: 'Cevap hazır', SESSION_ENDED: 'Oturum kapandı',
};

function addLog(event) {
  const row = document.createElement('li');
  const time = new Date(event.createdAt || Date.now()).toLocaleTimeString('tr-TR');
  const who = event.agentId ? agentName(event.agentType) : 'Ana ajan';
  const what = event.type === 'TOOL' ? toolText(event) : (LOG_LABELS[event.type] + (event.detail ? ': ' + event.detail : ''));
  row.textContent = `[${time}] ${who} · ${what}`;
  logList.prepend(row);
  while (logList.children.length > 80) logList.lastElementChild.remove();
}

function setStatus(text, kind) {
  statusEl.textContent = text;
  statusEl.className = 'status ' + kind;
}

// ---------- live connection ----------

let source = null;

async function loadLatest() {
  resetScene();
  logList.replaceChildren();
  state.sessionId = null;
  projectEl.textContent = '';
  try {
    const sessions = await fetch('/api/sessions?size=1').then(r => r.json());
    const latest = sessions.content && sessions.content[0];
    if (!latest) return;
    const events = await fetch(`/api/sessions/${encodeURIComponent(latest.sessionId)}/events`).then(r => r.json());
    for (const event of events) await handle(event, true);
    // A session with recent events but no SESSION_STARTED (e.g. started before the API) still has lights on
    const last = events[events.length - 1];
    if (last && last.type !== 'SESSION_ENDED') state.open = true;
    for (const event of events.slice(-12)) addLog(event);
  } catch (e) {
    setStatus('API yok', 'off');
  }
}

function connect() {
  if (source) source.close();
  if (state.mode === 'live') setStatus('Bağlanıyor', 'wait');
  source = new EventSource('/api/stream');
  source.onopen = () => { if (state.mode === 'live') setStatus('Canlı', 'on'); };
  source.onerror = () => { if (state.mode === 'live') setStatus('Bağlantı yok', 'off'); };
  source.addEventListener('office-event', message => {
    if (state.mode !== 'live') return;
    handle(JSON.parse(message.data));
  });
}

// ---------- demo ----------

const DEMO = [
  [0, { type: 'SESSION_STARTED', detail: 'site-api' }],
  [2600, { type: 'PROMPT', detail: 'JWT ile giriş ekle ve testlerini yaz' }],
  [1600, { type: 'TOOL', toolName: 'Agent', detail: 'Kod tabanını tara' }],
  [600, { type: 'AGENT_STARTED', agentId: 'a1', agentType: 'Explore' }],
  [3200, { type: 'TOOL', agentId: 'a1', agentType: 'Explore', toolName: 'Glob', detail: 'src/**/*.java' }],
  [1500, { type: 'TOOL', agentId: 'a1', agentType: 'Explore', toolName: 'Read', detail: 'SecurityConfig.java' }],
  [1500, { type: 'TOOL', agentId: 'a1', agentType: 'Explore', toolName: 'Grep', detail: 'SecurityFilterChain' }],
  [1500, { type: 'AGENT_STOPPED', agentId: 'a1', agentType: 'Explore', detail: '3 dosya bulundu' }],
  [1200, { type: 'TOOL', toolName: 'Agent', detail: 'JWT filtresini yaz' }],
  [300, { type: 'AGENT_STARTED', agentId: 'a2', agentType: 'general-purpose' }],
  [500, { type: 'AGENT_STARTED', agentId: 'a3', agentType: 'test-writer' }],
  [500, { type: 'AGENT_STARTED', agentId: 'a4', agentType: 'security-reviewer' }],
  [3600, { type: 'TOOL', agentId: 'a2', agentType: 'general-purpose', toolName: 'Write', detail: 'JwtFilter.java' }],
  [900, { type: 'TOOL', agentId: 'a3', agentType: 'test-writer', toolName: 'Read', detail: 'AuthController.java' }],
  [900, { type: 'TOOL', agentId: 'a4', agentType: 'security-reviewer', toolName: 'Read', detail: 'JwtService.java' }],
  [1400, { type: 'TOOL', agentId: 'a2', agentType: 'general-purpose', toolName: 'Edit', detail: 'SecurityConfig.java' }],
  [900, { type: 'TOOL', agentId: 'a3', agentType: 'test-writer', toolName: 'Write', detail: 'AuthFlowTest.java' }],
  [1200, { type: 'WAITING', detail: 'Bash komutu için onay' }],
  [2400, { type: 'TOOL', agentId: 'a3', agentType: 'test-writer', toolName: 'Bash', detail: 'mvn test' }],
  [400, { type: 'TOOL', agentId: 'a4', agentType: 'security-reviewer', toolName: 'Grep', detail: 'parseClaimsJws' }],
  [1800, { type: 'AGENT_STOPPED', agentId: 'a4', agentType: 'security-reviewer', detail: 'Sorun yok' }],
  [1400, { type: 'TOOL', agentId: 'a2', agentType: 'general-purpose', toolName: 'Bash', detail: 'Derle' }],
  [1600, { type: 'AGENT_STOPPED', agentId: 'a2', agentType: 'general-purpose', detail: 'Filtre hazır' }],
  [1200, { type: 'AGENT_STOPPED', agentId: 'a3', agentType: 'test-writer', detail: '12/12 test geçti' }],
  [1500, { type: 'TOOL', toolName: 'Read', detail: 'AuthFlowTest.java' }],
  [1500, { type: 'STOPPED', detail: 'JWT girişi ve testler hazır' }],
];

async function runDemo() {
  if (state.mode === 'demo') return;
  state.mode = 'demo';
  demoButton.disabled = true;
  setStatus('Demo', 'demo');
  resetScene();
  logList.replaceChildren();
  state.sessionId = null;
  for (const [delay, event] of DEMO) {
    await sleep(delay);
    handle({ sessionId: 'demo', createdAt: new Date().toISOString(), ...event });
  }
  await sleep(7000);
  state.mode = 'live';
  demoButton.disabled = false;
  setStatus(source && source.readyState === EventSource.OPEN ? 'Canlı' : 'Bağlantı yok',
      source && source.readyState === EventSource.OPEN ? 'on' : 'off');
  await loadLatest();
}

// ---------- main loop ----------

let last = performance.now();
function frame(now) {
  const dt = Math.min((now - last) / 1000, 0.1);
  last = now;
  state.time += dt;
  const date = new Date();

  const people = [...state.agents.values()];
  if (state.boss) people.push(state.boss);
  // Finished agents are no longer in the map but still walking to the coffee machine and the door
  for (const p of state.leaving) people.push(p);
  people.forEach(p => move(p, dt));

  ctx.clearRect(0, 0, W, H);
  drawFloor();
  drawWall(date);
  drawFurniture();
  drawChair(BOSS_DESK.x, BOSS_DESK.y);
  DESKS.forEach(d => drawChair(d.x, d.y));
  [...people].sort((a, b) => a.y - b.y).forEach(drawPerson);
  drawDesk(BOSS_DESK.x, BOSS_DESK.y, !!state.boss && state.boss.pose === 'type', true);
  DESKS.forEach((d, i) => {
    const id = state.seats[i];
    const person = id && state.agents.get(id);
    drawDesk(d.x, d.y, !!person && person.pose === 'type', false);
  });
  drawLights(date);
  people.forEach(placeBubble);
  requestAnimationFrame(frame);
}

demoButton.addEventListener('click', runDemo);
requestAnimationFrame(frame);
loadLatest().then(connect);
if (new URLSearchParams(location.search).has('demo')) runDemo();
