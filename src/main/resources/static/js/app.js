'use strict';

/* =========================================================
   Automation Hub dashboard
   Rules:
   1. User ka diya text (name, message) HAMESHA textContent se dikhate hain, kabhi innerHTML se nahi (XSS se bachav)
   2. Login credentials sirf sessionStorage mein (tab band = gayab)
   ========================================================= */

const STORAGE_KEY = 'automation-hub-session';
const DAYS = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'];
const DAY_LABELS = { MON: 'Mon', TUE: 'Tue', WED: 'Wed', THU: 'Thu', FRI: 'Fri', SAT: 'Sat', SUN: 'Sun' };
// Dropdown mein isi order mein dikhenge
const CHANNEL_LABELS = { TELEGRAM: 'Telegram', DISCORD: 'Discord', EMAIL: 'Email' };

// App ki yaaddasht
const state = { authHeader: null, username: '', jobs: [], channels: [], editingId: null };

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
const confirmDialog = $('#confirm-dialog');

// Form ke elements ek jagah
const jobDialog = $('#job-dialog');
const jobForm = $('#job-form');
const jobFormError = $('#job-form-error');
const jobSave = $('#job-save');
const f = {
  name: $('#job-name'),
  message: $('#job-message'),
  channel: $('#job-channel'),
  target: $('#job-target'),
  mode: $('#schedule-mode'),
  time: $('#schedule-time'),
  interval: $('#schedule-interval'),
  custom: $('#schedule-custom'),
  days: $('#days-field')
};

/* ---------- Chhote helpers ---------- */

// Safe element banane ka helper: text hamesha textContent se jaata hai
function el(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = text;
  return node;
}

// Icons: STATIC (hamare apne likhe) SVG strings. Inme user ka data kabhi nahi aata.
const ICONS = {
  clock: '<svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></svg>',
  trash: '<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18M8 6V4h8v2M6 6l1 14h10l1-14M10 11v6M14 11v6"/></svg>',
  pencil: '<svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 20h4L19 9l-4-4L4 16v4zM13.5 6.5l4 4"/></svg>'
};

function icon(name) {
  const span = document.createElement('span');
  span.innerHTML = ICONS[name]; // sirf constant strings, isliye safe
  return span;
}

const pad2 = (n) => String(n).padStart(2, '0');
const sortJobs = (jobs) => [...jobs].sort((a, b) => b.id - a.id); // naye pehle

// Username:password ko Basic auth header mein badalna (unicode ke liye bhi safe)
function makeAuthHeader(username, password) {
  const bytes = new TextEncoder().encode(`${username}:${password}`);
  let binary = '';
  bytes.forEach((b) => { binary += String.fromCharCode(b); });
  return 'Basic ' + btoa(binary);
}

/* ---------- API call ---------- */

// Har request mein Authorization header. Error mein status aur server ka body (field errors) bhi rakhte hain
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
    error.body = data; // jaise { "channel": "DISCORD is not configured..." }
    throw error;
  }
  return data;
}

// 401 = session khatam, baaki = toast
function handleError(error) {
  if (error.status === 401) {
    logout('Your session has expired. Please sign in again.');
  } else {
    showToast(error.message || 'Something went wrong', 'error');
  }
}

/* ---------- Toast ---------- */

function showToast(message, type = 'success') {
  const toast = el('div', type === 'error' ? 'toast toast--error' : 'toast', message);
  $('#toasts').append(toast);
  setTimeout(() => {
    toast.classList.add('is-leaving');
    toast.addEventListener('animationend', () => toast.remove(), { once: true });
  }, 3200);
}

/* ---------- Cron <-> insaani bhasha ---------- */

function formatTime(hour, minute) {
  const suffix = hour >= 12 ? 'PM' : 'AM';
  const hour12 = hour % 12 === 0 ? 12 : hour % 12;
  return `${hour12}:${pad2(minute)} ${suffix}`;
}

// "0 0 8 * * *" -> "Every day at 8:00 AM". Samajh na aaye toh raw cron dikha dete hain
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
    return every(min) === '1' ? 'Every minute' : `Every ${every(min)} minutes`;
  }
  if (sec === '0' && isNumber(min) && every(hour) && dom === '*' && mon === '*' && dow === '*') {
    return every(hour) === '1' ? 'Every hour' : `Every ${every(hour)} hours`;
  }
  if (sec === '0' && isNumber(min) && isNumber(hour) && dom === '*' && mon === '*') {
    const time = formatTime(Number(hour), Number(min));
    if (dow === '*') return `Every day at ${time}`;
    if (dow === 'MON-FRI') return `Weekdays at ${time}`;
    return `${dow} at ${time}`;
  }
  return cron;
}

// Edit ke waqt: purane cron ko builder ke controls mein wapas todna
function parseCron(cron) {
  const parts = (cron || '').trim().split(/\s+/);
  if (parts.length === 6) {
    const [sec, min, hour, dom, mon, dow] = parts;
    const isNumber = (v) => /^\d+$/.test(v);
    const every = (v) => (v.match(/^\*\/(\d+)$/) || [])[1];

    if (sec === '0' && dom === '*' && mon === '*') {
      if (isNumber(min) && isNumber(hour)) {
        const time = `${pad2(hour)}:${pad2(min)}`;
        if (dow === '*') return { mode: 'daily', time };
        if (dow === 'MON-FRI') return { mode: 'weekdays', time };
        const days = dow.split(',');
        if (days.every((d) => DAYS.includes(d))) return { mode: 'days', time, days };
      }
      if (every(min) && hour === '*' && dow === '*') return { mode: 'minutes', interval: every(min) };
      if (min === '0' && every(hour) && dow === '*') return { mode: 'hours', interval: every(hour) };
      if (min === '*' && hour === '*' && dow === '*') return { mode: 'minutes', interval: '1' };
    }
  }
  return { mode: 'custom', cron: cron || '' }; // pehchana nahi, toh custom mein hi dikha do
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

  const message = el('p', 'job-message', job.message);

  // Channel aur schedule chips
  const channelChip = el('span', `chip chip--${job.channel.toLowerCase()}`);
  channelChip.append(el('span', 'dot'), document.createTextNode(CHANNEL_LABELS[job.channel] || job.channel));

  const scheduleChip = el('span', 'chip');
  scheduleChip.title = job.cronExpression || ''; // hover par asli cron
  scheduleChip.append(icon('clock'), document.createTextNode(cronToText(job.cronExpression)));

  const meta = el('div', 'job-meta');
  meta.append(channelChip, scheduleChip);

  // Neeche: switch (pause/resume), aur edit + delete buttons
  const toggle = el('button', 'switch');
  toggle.type = 'button';
  toggle.setAttribute('role', 'switch');
  toggle.setAttribute('aria-checked', String(job.active));
  toggle.setAttribute('aria-label', `${job.active ? 'Pause' : 'Resume'} ${job.name}`);
  toggle.addEventListener('click', () => toggleJob(job, toggle));

  const switchRow = el('div', 'switch-row');
  switchRow.append(toggle, el('span', '', job.active ? 'Running' : 'Paused'));

  const editBtn = el('button', 'icon-btn icon-btn--edit');
  editBtn.type = 'button';
  editBtn.setAttribute('aria-label', `Edit ${job.name}`);
  editBtn.append(icon('pencil'));
  editBtn.addEventListener('click', () => openJobForm(job));

  const deleteBtn = el('button', 'icon-btn');
  deleteBtn.type = 'button';
  deleteBtn.setAttribute('aria-label', `Delete ${job.name}`);
  deleteBtn.append(icon('trash'));
  deleteBtn.addEventListener('click', () => deleteJob(job));

  const actions = el('div', 'job-actions');
  actions.append(editBtn, deleteBtn);

  const foot = el('div', 'job-foot');
  foot.append(switchRow, actions);

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
      el('p', 'muted', 'Click "+ New job" to schedule your first message.')
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

function showSkeletons() {
  statsEl.replaceChildren();
  jobsEl.replaceChildren(...Array.from({ length: 3 }, () => el('div', 'skeleton')));
}

/* ---------- Data loading ---------- */

async function loadJobs() {
  state.jobs = sortJobs(await api('/api/jobs'));
  render();
}

// Kaun se channels is server pe configured hain. 401 ko chhodke koi error aaye toh Telegram maan lo
async function loadChannels() {
  try {
    state.channels = await api('/api/channels');
  } catch (error) {
    if (error.status === 401) throw error;
    state.channels = ['TELEGRAM'];
  }
}

/* ---------- Pause / resume / delete ---------- */

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
    confirmDialog.returnValue = '';
    confirmDialog.addEventListener('close', () => resolve(confirmDialog.returnValue === 'confirm'), { once: true });
    confirmDialog.showModal();
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

/* =========================================================
   NEW / EDIT FORM + CRON BUILDER
   ========================================================= */

// Specific days ke chips (Mon..Sun) ek baar banao
function buildDayChips() {
  DAYS.forEach((day) => {
    const chip = el('button', 'day-chip', DAY_LABELS[day]);
    chip.type = 'button';
    chip.dataset.day = day;
    chip.setAttribute('aria-pressed', 'false');
    chip.addEventListener('click', () => {
      chip.setAttribute('aria-pressed', String(chip.getAttribute('aria-pressed') !== 'true'));
      updateSchedulePreview();
    });
    f.days.append(chip);
  });
}

const selectedDays = () =>
  DAYS.filter((d) => f.days.querySelector(`[data-day="${d}"]`).getAttribute('aria-pressed') === 'true');

function setSelectedDays(list) {
  DAYS.forEach((d) =>
    f.days.querySelector(`[data-day="${d}"]`).setAttribute('aria-pressed', String(list.includes(d))));
}

const setVisible = (selector, visible) => $(selector).classList.toggle('hidden', !visible);

// Repeat ke hisaab se kaun se controls dikhane hain
function updateScheduleFields() {
  const mode = f.mode.value;
  setVisible('#time-field', ['daily', 'weekdays', 'days'].includes(mode));
  setVisible('#days-field', mode === 'days');
  setVisible('#interval-field', mode === 'minutes' || mode === 'hours');
  setVisible('#custom-field', mode === 'custom');

  if (mode === 'minutes') { $('#interval-label').textContent = 'Every (minutes)'; f.interval.max = '59'; }
  if (mode === 'hours') { $('#interval-label').textContent = 'Every (hours)'; f.interval.max = '23'; }
}

// Controls se Spring ka 6-field cron banana: second minute hour day month weekday
// Kuch adhura/galat ho toh null (matlab "abhi schedule poora nahi")
function buildCron() {
  const [hour, minute] = (f.time.value || '').split(':').map(Number);
  const hasTime = Number.isInteger(hour) && Number.isInteger(minute);
  const interval = Number(f.interval.value);
  const validInterval = (max) => Number.isInteger(interval) && interval >= 1 && interval <= max;

  switch (f.mode.value) {
    case 'daily':
      return hasTime ? `0 ${minute} ${hour} * * *` : null;
    case 'weekdays':
      return hasTime ? `0 ${minute} ${hour} * * MON-FRI` : null;
    case 'days': {
      const days = selectedDays();
      return hasTime && days.length ? `0 ${minute} ${hour} * * ${days.join(',')}` : null;
    }
    case 'minutes':
      return validInterval(59) ? `0 */${interval} * * * *` : null;
    case 'hours':
      return validInterval(23) ? `0 0 */${interval} * * *` : null;
    default: // custom
      return f.custom.value.trim() || null;
  }
}

// Live preview: "Weekdays at 9:00 AM" + neeche asli cron
function updateSchedulePreview() {
  updateScheduleFields();
  const cron = buildCron();
  $('#schedule-text').textContent = cron ? cronToText(cron) : 'Complete the schedule to see a preview';
  $('#schedule-cron').textContent = cron || '';
}

// Purana cron builder mein wapas bharna (edit mode)
function applyCronToForm(cron) {
  const parsed = parseCron(cron);
  f.mode.value = parsed.mode;
  f.time.value = parsed.time || '08:00';
  f.interval.value = parsed.interval || '15';
  f.custom.value = parsed.mode === 'custom' ? parsed.cron : '';
  setSelectedDays(parsed.days || []);
}

// Dropdown mein sirf configured channels (edit mein job ka purana channel bhi, agar ab configured nahi hai)
function fillChannelOptions(currentChannel) {
  const names = new Set(state.channels);
  if (currentChannel) names.add(currentChannel);

  const options = Object.keys(CHANNEL_LABELS)
    .filter((name) => names.has(name))
    .map((name) => {
      const label = CHANNEL_LABELS[name] + (state.channels.includes(name) ? '' : ' (not configured)');
      const option = el('option', '', label);
      option.value = name;
      return option;
    });

  f.channel.replaceChildren(...options);
  f.channel.value = currentChannel || state.channels[0] || 'TELEGRAM';
}

// Email address wala field sirf EMAIL channel par
function updateTargetVisibility() {
  setVisible('#target-field', f.channel.value === 'EMAIL');
}

function updateMessageCount() {
  $('#message-count').textContent = String(f.message.value.length);
}

function clearFormErrors() {
  document.querySelectorAll('[data-error-for]').forEach((node) => { node.textContent = ''; });
  jobFormError.textContent = '';
  jobFormError.classList.add('hidden');
}

// Server ka { field: message } dikhana. Jo field form mein nahi hai wo upar general error mein
function showFieldErrors(errors) {
  const general = [];
  Object.entries(errors).forEach(([field, message]) => {
    const target = document.querySelector(`[data-error-for="${CSS.escape(field)}"]`);
    if (target) target.textContent = message;
    else general.push(message);
  });
  if (general.length) {
    jobFormError.textContent = general.join(' ');
    jobFormError.classList.remove('hidden');
  }
}

function setSaving(isSaving) {
  jobSave.classList.toggle('is-loading', isSaving);
  jobSave.disabled = isSaving;
}

// job = null -> naya job, warna edit
function openJobForm(job = null) {
  state.editingId = job ? job.id : null;
  $('#job-dialog-title').textContent = job ? 'Edit job' : 'New job';
  jobSave.querySelector('.btn-label').textContent = job ? 'Save changes' : 'Create job';

  clearFormErrors();
  f.name.value = job ? job.name : '';
  f.message.value = job ? job.message : '';
  f.target.value = job && job.target ? job.target : '';
  fillChannelOptions(job ? job.channel : null);
  applyCronToForm(job ? job.cronExpression : '0 0 8 * * *'); // naye job ka default: roz 8 baje

  updateMessageCount();
  updateTargetVisibility();
  updateSchedulePreview();
  jobDialog.showModal();
  f.name.focus();
}

jobForm.addEventListener('submit', async (event) => {
  event.preventDefault();
  clearFormErrors();

  const cron = buildCron();
  const payload = {
    name: f.name.value.trim(),
    message: f.message.value.trim(),
    channel: f.channel.value,
    cronExpression: cron,
    target: f.channel.value === 'EMAIL' ? f.target.value.trim() : null
  };

  // Browser-side jaldi wali jaanch (asli jaanch server karta hai)
  const errors = {};
  if (!payload.name) errors.name = 'Name is required';
  if (!payload.message) errors.message = 'Message is required';
  if (!cron) errors.cronExpression = 'Please complete the schedule';
  if (payload.channel === 'EMAIL' && !payload.target) errors.target = 'Email address is required';
  if (Object.keys(errors).length) {
    showFieldErrors(errors);
    return;
  }

  setSaving(true);
  try {
    const editing = state.editingId !== null;
    const saved = await api(editing ? `/api/jobs/${state.editingId}` : '/api/jobs', {
      method: editing ? 'PUT' : 'POST',
      body: JSON.stringify(payload)
    });

    state.jobs = editing
      ? state.jobs.map((j) => (j.id === saved.id ? saved : j))
      : [saved, ...state.jobs];
    state.jobs = sortJobs(state.jobs);
    render();

    jobDialog.close();
    showToast(editing ? `"${saved.name}" updated` : `"${saved.name}" created`);
  } catch (error) {
    if (error.status === 401) {
      jobDialog.close();
      handleError(error);
    } else if (error.status === 400 && error.body) {
      showFieldErrors(error.body); // server ke field errors form mein hi
    } else {
      showFieldErrors({ general: error.message || 'Something went wrong' });
    }
  } finally {
    setSaving(false);
  }
});

// Form ki wiring
$('#new-job-btn').addEventListener('click', () => openJobForm());
$('#job-close').addEventListener('click', () => jobDialog.close());
$('#job-cancel').addEventListener('click', () => jobDialog.close());
f.mode.addEventListener('change', updateSchedulePreview);
[f.time, f.interval, f.custom].forEach((input) => input.addEventListener('input', updateSchedulePreview));
f.channel.addEventListener('change', updateTargetVisibility);
f.message.addEventListener('input', updateMessageCount);
buildDayChips();

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

// Sirf screen badalna (data alag se load hota hai)
function showDashboardShell() {
  loginView.classList.add('hidden');
  dashboardView.classList.remove('hidden');
  $('#user-name').textContent = state.username;
  $('#user-avatar').textContent = state.username.charAt(0);
}

function logout(message) {
  sessionStorage.removeItem(STORAGE_KEY);
  state.authHeader = null;
  state.username = '';
  state.jobs = [];
  state.channels = [];
  if (jobDialog.open) jobDialog.close();
  loginForm.reset();
  showLogin(message);
}

function setLoginLoading(isLoading) {
  loginSubmit.classList.toggle('is-loading', isLoading);
  loginSubmit.disabled = isLoading;
}

loginForm.addEventListener('submit', async (event) => {
  event.preventDefault();
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
    // Ye data load hi login ki jaanch hai: 200 = sahi, 401 = galat
    await Promise.all([loadJobs(), loadChannels()]);
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify({ auth: state.authHeader, user: username }));
    showDashboardShell();
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
    showDashboardShell();
    showSkeletons();
    await Promise.all([loadJobs(), loadChannels()]);
  } catch (error) {
    logout(error.status === 401 ? 'Your session has expired. Please sign in again.' : undefined);
  }
})();