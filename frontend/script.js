/**
 * CodeForge — Frontend Script
 *
 * Responsibilities:
 *   - Send code to POST /api/run and display the result
 *   - Language dropdown → load starter templates
 *   - Sync gutter line numbers with the editor
 *   - Load and render submission history
 *   - Toast notifications, copy output, keyboard shortcuts
 */

// ── Config ────────────────────────────────────────────────
const API = 'http://localhost:8080/api';

// ── DOM refs ──────────────────────────────────────────────
const $ = id => document.getElementById(id);
const codeEditor    = $('codeEditor');
const languageSel   = $('language');
const inputData     = $('inputData');
const runBtn        = $('runBtn');
const outputDisplay = $('outputDisplay');
const statusBar     = $('statusBar');
const gutter        = $('gutter');
const loadingOverlay= $('loadingOverlay');
const historyGrid   = $('historyGrid');

// ── Starter templates ─────────────────────────────────────
const TEMPLATES = {
  c: `#include <stdio.h>

int main() {
    int n;
    scanf("%d", &n);

    printf("You entered: %d\\n", n);
    printf("Hello from C!\\n");

    return 0;
}`,

  cpp: `#include <iostream>
#include <string>
using namespace std;

int main() {
    string name;
    cin >> name;

    cout << "Hello, " << name << "!" << endl;
    cout << "Welcome to CodeForge!" << endl;

    return 0;
}`,

  java: `import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter your name: ");
        String name = sc.nextLine();

        System.out.println("Hello, " + name + "!");
        System.out.println("Welcome to CodeForge!");

        sc.close();
    }
}`
};

// ── Init ──────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  loadTemplate(languageSel.value);
  syncGutter();
  loadHistory();
  pingHealth();
});

// ── Language change ───────────────────────────────────────
languageSel.addEventListener('change', () => {
  const isModified = codeEditor.value.trim() &&
                     !Object.values(TEMPLATES).includes(codeEditor.value);
  if (isModified && !confirm('Load starter template? Your current code will be replaced.')) return;
  loadTemplate(languageSel.value);
});

function loadTemplate(lang) {
  if (TEMPLATES[lang]) {
    codeEditor.value = TEMPLATES[lang];
    syncGutter();
  }
}

// ── Gutter (line numbers) ─────────────────────────────────
codeEditor.addEventListener('input',  syncGutter);
codeEditor.addEventListener('scroll', () => { gutter.scrollTop = codeEditor.scrollTop; });

function syncGutter() {
  const count = codeEditor.value.split('\n').length;
  let html = '';
  for (let i = 1; i <= count; i++) html += i + '\n';
  gutter.textContent = html;
}

// ── Keyboard shortcuts ─────────────────────────────────────
codeEditor.addEventListener('keydown', e => {
  // Tab → insert 4 spaces
  if (e.key === 'Tab') {
    e.preventDefault();
    const s = codeEditor.selectionStart;
    codeEditor.value = codeEditor.value.slice(0, s) + '    ' + codeEditor.value.slice(codeEditor.selectionEnd);
    codeEditor.selectionStart = codeEditor.selectionEnd = s + 4;
    syncGutter();
  }
  // Ctrl/Cmd+Enter → Run
  if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
    e.preventDefault();
    submitCode();
  }
});

// ── Submit code ───────────────────────────────────────────
async function submitCode() {
  const code  = codeEditor.value.trim();
  const lang  = languageSel.value;
  const input = inputData.value;

  if (!code) {
    showToast('Please write some code first!', 'error');
    codeEditor.focus();
    return;
  }

  setLoading(true);
  clearOutput();

  try {
    const res = await fetch(`${API}/run`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ language: lang, sourceCode: code, inputData: input })
    });

    if (!res.ok) throw new Error(`HTTP ${res.status}: ${res.statusText}`);

    const data = await res.json();
    displayResult(data);

  } catch (err) {
    const msg = err.message.includes('fetch') || err.message.includes('Failed')
      ? '🔌 Cannot reach backend. Is Spring Boot running on port 8080?'
      : err.message;
    showError(msg);
  } finally {
    setLoading(false);
  }
}

// ── Display result ────────────────────────────────────────
function displayResult({ output, error, status, executionTimeMs, submissionId }) {

  if (status === 'SUCCESS') {
    if (output && output.trim()) {
      outputDisplay.textContent = output;
      outputDisplay.className = 'out-success';
    } else {
      outputDisplay.textContent = '(Program exited successfully with no output)';
      outputDisplay.className = 'output-placeholder';
    }
    if (error && error.trim()) {
      outputDisplay.textContent += '\n\n⚠️ Compiler Warnings:\n' + error;
    }
    setStatus('success', '✓ Success', executionTimeMs, submissionId);
    showToast('Code executed successfully!', 'success');

  } else if (status === 'TIMEOUT') {
    outputDisplay.textContent = error || 'Execution timed out after 10 seconds.';
    outputDisplay.className = 'out-timeout';
    setStatus('timeout', '⏱ Timeout', executionTimeMs, submissionId);
    showToast('Execution timed out!', 'error');

  } else {
    outputDisplay.textContent = '❌ ' + (error || output || 'Unknown error');
    outputDisplay.className = 'out-error';
    setStatus('error', '✗ Error', executionTimeMs, submissionId);
    showToast('Compilation / Runtime error', 'error');
  }

  // Refresh history list
  setTimeout(loadHistory, 600);
}

function showError(msg) {
  outputDisplay.textContent = '⚠️ ' + msg;
  outputDisplay.className = 'out-error';
  setStatus('error', '✗ Error', 0, null);
}

function clearOutput() {
  outputDisplay.textContent = 'Running...';
  outputDisplay.className = '';
  statusBar.innerHTML = '<span class="status-idle">○ Running...</span><span class="exec-time" id="execTime"></span>';
}

function setStatus(cls, label, ms, id) {
  statusBar.innerHTML = `
    <span class="status-${cls}">${label}</span>
    ${id ? `<span style="color:var(--text-muted);font-size:.68rem;">ID #${id}</span>` : ''}
    <span class="exec-time">${ms > 0 ? ms + 'ms' : ''}</span>
  `;
}

// ── Loading state ─────────────────────────────────────────
function setLoading(on) {
  runBtn.disabled = on;
  runBtn.classList.toggle('loading', on);
  $('btnText') && ($('btnText').textContent = on ? 'Running...' : 'Run Code');
  loadingOverlay.classList.toggle('active', on);
}

// ── History ───────────────────────────────────────────────
async function loadHistory() {
  try {
    const res = await fetch(`${API}/submissions`);
    if (!res.ok) return;
    const data = await res.json();
    renderHistory(data);
  } catch { /* silently ignore */ }
}

function renderHistory(items) {
  if (!items.length) {
    historyGrid.innerHTML = '<p class="history-empty">No submissions yet. Run some code!</p>';
    return;
  }
  historyGrid.innerHTML = items.map(s => `
    <div class="history-item" onclick="loadSubmission(${s.id})">
      <div class="h-lang ${s.language}">${s.language.toUpperCase()}</div>
      <div class="h-preview">${esc(s.sourceCode.slice(0,64))}${s.sourceCode.length>64?'...':''}</div>
      <div class="h-meta">
        <span class="h-status ${s.status}">${s.status}</span>
        <span class="h-time">${timeAgo(s.createdAt)}</span>
      </div>
    </div>
  `).join('');
}

async function loadSubmission(id) {
  try {
    const res = await fetch(`${API}/submissions/${id}`);
    if (!res.ok) return;
    const s = await res.json();

    languageSel.value = s.language;
    document.getElementById('langLabel').textContent =
      { c:'C', cpp:'C++', java:'Java' }[s.language] || s.language.toUpperCase();
    codeEditor.value   = s.sourceCode;
    inputData.value    = s.inputData || '';
    syncGutter();

    if (s.outputData) {
      outputDisplay.textContent = s.outputData;
      outputDisplay.className   = s.status === 'SUCCESS' ? 'out-success' : 'out-error';
      setStatus(s.status.toLowerCase(), s.status === 'SUCCESS' ? '✓ Success' : '✗ Error', 0, s.id);
    }

    window.scrollTo({ top: 0, behavior: 'smooth' });
    showToast(`Loaded submission #${id}`, 'success');
  } catch (e) {
    showToast('Could not load submission', 'error');
  }
}

// ── Clear editor ──────────────────────────────────────────
function clearEditor() {
  const isDefault = Object.values(TEMPLATES).includes(codeEditor.value);
  if (!isDefault && codeEditor.value.trim() &&
      !confirm('Reset editor to starter template?')) return;
  loadTemplate(languageSel.value);
  inputData.value = '';
  outputDisplay.textContent = 'Run your code to see output here...';
  outputDisplay.className = 'output-placeholder';
  statusBar.innerHTML = '<span class="status-idle">○ Ready</span><span class="exec-time"></span>';
  showToast('Editor reset', 'success');
}

// ── Copy output ───────────────────────────────────────────
async function copyOutput() {
  const txt = outputDisplay.textContent;
  if (!txt || outputDisplay.classList.contains('output-placeholder')) {
    showToast('Nothing to copy', 'error'); return;
  }
  try {
    await navigator.clipboard.writeText(txt);
    showToast('Output copied!', 'success');
  } catch {
    showToast('Copy failed', 'error');
  }
}

// ── Health ping ───────────────────────────────────────────
async function pingHealth() {
  try {
    const res = await fetch(`${API}/health`);
    if (!res.ok) throw new Error();
    console.log('✅ Backend is online');
  } catch {
    showToast('⚠️ Backend not reachable — start Spring Boot on port 8080', 'error');
  }
}

// ── Toast ─────────────────────────────────────────────────
function showToast(msg, type = 'success') {
  const c   = $('toastContainer');
  const div = document.createElement('div');
  div.className = `toast ${type}`;
  div.innerHTML = `<span>${type === 'success' ? '✓' : '⚠'}</span><span>${msg}</span>`;
  c.appendChild(div);
  setTimeout(() => { div.style.opacity = '0'; div.style.transform = 'translateY(10px)'; div.style.transition='.25s ease'; setTimeout(() => div.remove(), 260); }, 3000);
}

// ── Utils ─────────────────────────────────────────────────
function esc(s) {
  return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
}

function timeAgo(iso) {
  if (!iso) return '';
  const diff = Date.now() - new Date(iso);
  const m    = Math.floor(diff / 60000);
  if (m < 1)   return 'just now';
  if (m < 60)  return `${m}m ago`;
  if (m < 1440) return `${Math.floor(m/60)}h ago`;
  return new Date(iso).toLocaleDateString();
}
