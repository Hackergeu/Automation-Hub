'use strict';

const STORAGE_KEY = 'automation-hub-session';

// App ki yaaddasht: kaun login hai, aur jobs kya hain
const state = { authHeader: null, username: '', jobs: [] };

const $ = (selector) => document.querySelector(selector);

// Aksar use hone wale elements
const loginView = $('#login-view');
const dashboardView = $('#dashboard-view');
const loginForm = $('#login-form');
const loginError = $('#login-error');
const loginSubmit = $('#login-submit');
const statsEl = $('#stats');
const jobsEl = $('#jobs');
const jobsCount = $('#jobs-count');
const dialog = $('#confirm-dialog');

/* ---------- Chhote helpers ---------- */

// Safe element banane ka helper: text hamesha textContent se jaata hai
function el(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = text;
  return node;
}

// Icons ke liye STATIC (hamare apne likhe) SVG strings. Inme user ka data kabhi nahi aata.
const ICONS = {
  clock: '<svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></svg>',
  trash: '<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18M8 6V4h8v2M6 6l1 14h10l1-14M10 11v6M14 11v6"/></svg>'
};

function icon(name) {
  const span = document.createElement('span');
  span.innerHTML = ICONS[name]; // sirf constant strings, isliye safe
  return span;
}

// Username:password ko Basic auth header mein badalna (unicode characters ke liye bhi safe)
function makeAuthHeader(username, password) {
  const bytes = new TextEncoder().encode(`${username}:${password}`);
  let binary = '';
  bytes.forEach((b) => { binary += String.fromCharCode(b); });
  return 'Basic ' + btoa(binary);
}

/* ---------- API call ---------- */

// Har request mein Authorization header lagta hai. Galat login par error.status = 401.
async function api(path, options = {}) {
  const response = await fetch(path, {
    ...options,
    headers: {
      'Authorization': state.authHeader,
      'Content-Type': 'application/json',
      ...(options.headers || {})
    }
  });

  if (response.status === 204) return null; // delete ka jawab khali hota hai

  const data = await response.json().catch(() => null);

  if (!response.ok) {
    const error = new Error((data && (data.error || Object.values(data)[0])) || 'Request failed');
    error.status = response.status;
    throw error;
  }
  return data;
}

// Ek jagah error sambhalna: 401 = session khatam, baaki = toast
function handleError(error) {
  if (error.status === 401) {
    logout('Your session has expired. Please sign in again.');
  } else {
    showToast(error.message || 'Something went wrong', 'error');
  }
}

/* ---------- Toast (chhota notification) ---------- */

function showToast(message, type = 'success') {
  const toast = el('div', type === 'error' ? 'toast toast--error' : 'toast', message);
  $('#toasts').append(toast);
  setTimeout(() => {
    toast.classList.add('is-leaving');
    toast.addEventListener('animationend', () => toast.remove(), { once: true });
  }, 3200);
}

/* ---------- Cron ko insaani bhasha mein badalna ---------- */

function formatTime(hour, minute) {
  const suffix = hour >= 12 ? 'PM' : 'AM';
  const hour12 = hour % 12 === 0 ? 12 : hour % 12;
  return `${hour12}:${String(minute).padStart(2, '0')} ${suffix}`;
}

// "0 0 8 * * *" -> "Every day at 8:00 AM". Samajh na aaye toh raw cron hi dikha dete hain
function cronToText(cron) {
  const parts = (cron || '').trim().split(/\s+/);
  if (parts.length !== 6) return cron || 'No schedule';

  const [sec, min, hour, dom, mon, dow] = parts;
  const isNumber = (v) => /^\d+$/.test(v);
  const every = (v) => (v.match(/^\*\/(\d+)$/) || [])[1];

  if (sec === '0' && min === '*' && hour === '*' && dom === '*' && mon === '*' && dow === '*') {
    return 'Every minute';
  }
  if (sec === '0' && every(min) && hour === '*' && dom === '*' && mon === '*' && dow === '*') {
    return `Every ${every(min)} minutes`;
  }
  if (sec === '0' && isNumber(min) && every(hour) && dom === '*' && mon === '*' && dow === '*') {
    return `Every ${every(hour)} hours`;
  }
  if (sec === '0' && isNumber(min) && isNumber(hour) && dom === '*' && mon === '*') {
    const time = formatTime(Number(hour), Number(min));
    if (dow === '*') return `Every day at ${time}`;
    if (dow === 'MON-FRI') return `Weekdays at ${time}`;
    return `${dow} at ${time}`;
  }
  return cron;
}

/* ---------- Rendering ---------- */

function renderStats() {
  const total = state.jobs.length;
  const active = state.jobs.filter((j) => j.active).length;

  const cards = [
    { label: 'Total jobs', value: total, tone: 'var(--primary)' },
    { label: 'Active', value: active, tone: 'var(--success)' },
    { label: 'Paused', value: total - active, tone: 'var(--warning)' }
  ];

  statsEl.replaceChildren(...cards.map((card, index) => {
    const node = el('div', 'stat-card');
    node.style.setProperty('--i', index);
    node.style.setProperty('--tone', card.tone);
    node.append(el('div', 'stat-label', card.label), el('div', 'stat-value', String(card.value)));
    return node;
  }));
}

function createJobCard(job, index) {
  const card = el('article', job.active ? 'job-card' : 'job-card is-paused');
  card.style.setProperty('--i', index);

  // Upar: naam + status pill
  const head = el('div', 'job-head');
  head.append(
    el('h3', 'job-name', job.name),
    el('span', job.active ? 'pill pill--active' : 'pill pill--paused', job.active ? 'Active' : 'Paused')
  );

  // Beech: message (2 line tak)
  const message = el('p', 'job-message', job.message);

  // Channel aur schedule chips
  const channelChip = el('span', `chip chip--${job.channel.toLowerCase()}`);
  channelChip.append(el('span', 'dot'), document.createTextNode(job.channel));

  const scheduleChip = el('span', 'chip');
  scheduleChip.title = job.cronExpression || '';   // hover par asli cron dikhega
  scheduleChip.append(icon('clock'), document.createTextNode(cronToText(job.cronExpression)));

  const meta = el('div', 'job-meta');
  meta.append(channelChip, scheduleChip);

  // Neeche: switch (pause/resume) aur delete button
  const toggle = el('button', 'switch');
  toggle.type = 'button';
  toggle.setAttribute('role', 'switch');
  toggle.setAttribute('aria-checked', String(job.active));
  toggle.setAttribute('aria-label', `${job.active ? 'Pause' : 'Resume'} ${job.name}`);
  toggle.addEventListener('click', () => toggleJob(job, toggle));

  const switchRow = el('div', 'switch-row');
  switchRow.append(toggle, el('span', '', job.active ? 'Running' : 'Paused'));

  const deleteBtn = el('button', 'icon-btn');
  deleteBtn.type = 'button';
  deleteBtn.setAttribute('aria-label', `Delete ${job.name}`);
  deleteBtn.append(icon('trash'));
  deleteBtn.addEventListener('click', () => deleteJob(job));

  const foot = el('div', 'job-foot');
  foot.append(switchRow, deleteBtn);

  card.append(head, message, meta, foot);
  return card;
}

function renderJobs() {
  jobsCount.textContent = String(state.jobs.length);

  if (state.jobs.length === 0) {
    const empty = el('div', 'empty');
    empty.append(
      el('div', 'empty-icon', '📭'),
      el('h3', '', 'No jobs yet'),
      el('p', 'muted', 'Create your first job using the API. The "New job" form arrives in the next step.')
    );
    jobsEl.replaceChildren(empty);
    return;
  }
  jobsEl.replaceChildren(...state.jobs.map(createJobCard));
}

function render() {
  renderStats();
  renderJobs();
}

// Data aane tak chamakte hue placeholder cards
function showSkeletons() {
  statsEl.replaceChildren();
  jobsEl.replaceChildren(...Array.from({ length: 3 }, () => el('div', 'skeleton')));
}

/* ---------- Actions ---------- */

async function loadJobs() {
  const jobs = await api('/api/jobs');
  // Naye jobs pehle
  state.jobs = jobs.sort((a, b) => b.id - a.id);
  render();
}

async function toggleJob(job, button) {
  button.disabled = true; // double click se bachao
  try {
    const action = job.active ? 'pause' : 'resume';
    const updated = await api(`/api/jobs/${job.id}/${action}`, { method: 'PATCH' });
    state.jobs = state.jobs.map((j) => (j.id === updated.id ? updated : j));
    render();
    showToast(updated.active ? `"${updated.name}" resumed` : `"${updated.name}" paused`);
  } catch (error) {
    button.disabled = false;
    handleError(error);
  }
}

// Native <dialog> se confirm. Promise: true = delete, false = cancel
function confirmDelete(job) {
  return new Promise((resolve) => {
    $('#confirm-name').textContent = job.name;
    dialog.returnValue = '';
    dialog.addEventListener('close', () => resolve(dialog.returnValue === 'confirm'), { once: true });
    dialog.showModal();
  });
}

async function deleteJob(job) {
  if (!(await confirmDelete(job))) return;
  try {
    await api(`/api/jobs/${job.id}`, { method: 'DELETE' });
    state.jobs = state.jobs.filter((j) => j.id !== job.id);
    render();
    showToast(`"${job.name}" deleted`);
  } catch (error) {
    handleError(error);
  }
}

/* ---------- Login / logout ---------- */

function showLogin(message) {
  dashboardView.classList.add('hidden');
  loginView.classList.remove('hidden');
  if (message) {
    loginError.textContent = message;
    loginError.classList.remove('hidden');
  } else {
    loginError.classList.add('hidden');
  }
}

async function showDashboard() {
  loginView.classList.add('hidden');
  dashboardView.classList.remove('hidden');
  $('#user-name').textContent = state.username;
  $('#user-avatar').textContent = state.username.charAt(0);
  showSkeletons();
  await loadJobs();
}

function logout(message) {
  sessionStorage.removeItem(STORAGE_KEY);
  state.authHeader = null;
  state.username = '';
  state.jobs = [];
  loginForm.reset();
  showLogin(message);
}

function setLoginLoading(isLoading) {
  loginSubmit.classList.toggle('is-loading', isLoading);
  loginSubmit.disabled = isLoading;
}

loginForm.addEventListener('submit', async (event) => {
  event.preventDefault(); // page reload mat karo
  const username = $('#username').value.trim();
  const password = $('#password').value;

  if (!username || !password) {
    showLogin('Please enter both username and password.');
    return;
  }

  setLoginLoading(true);
  state.authHeader = makeAuthHeader(username, password);
  state.username = username;

  try {
    await loadJobs(); // ye call hi login ki jaanch hai: 200 = sahi, 401 = galat
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify({ auth: state.authHeader, user: username }));
    await showDashboard();
  } catch (error) {
    state.authHeader = null;
    showLogin(error.status === 401
      ? 'Invalid username or password.'
      : 'Could not reach the server. Please try again.');
  } finally {
    setLoginLoading(false);
  }
});

$('#toggle-password').addEventListener('click', (event) => {
  const field = $('#password');
  const show = field.type === 'password';
  field.type = show ? 'text' : 'password';
  event.target.textContent = show ? 'Hide' : 'Show';
});

$('#logout-btn').addEventListener('click', () => logout());

/* ---------- Start: pehle se login hai toh seedha dashboard ---------- */

(async function boot() {
  const saved = sessionStorage.getItem(STORAGE_KEY);
  if (!saved) return; // login screen waise hi dikh rahi hai

  try {
    const { auth, user } = JSON.parse(saved);
    state.authHeader = auth;
    state.username = user;
    await showDashboard();
  } catch (error) {
    logout(error.status === 401 ? 'Your session has expired. Please sign in again.' : undefined);
  }
})();