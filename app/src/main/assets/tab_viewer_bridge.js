let api = null;
let _poll = null;
let _lastTick = -1;
let _pendingScale = null;
let _scoreLoadedFired = false;
let _pendingSoundFontBase64 = null;
let _pendingTabBase64 = null;
let _pendingTabPath = null;
let _initRetry = null;
let _restoreTick = null;
let _restoreBarIndex = null;
let _restorePlay = false;
let _restoreAttempts = 0;
let _restoreToken = 0;
let _restoreTimers = [];
let _initialCursorSet = false;
let _tickReportTimer = null;
let _metronomeEnabled = false;
let _metronomeBpm = 90;
let _metronomeTimer = null;
let _metronomeCtx = null;
let _silentMode = false;
let _restoreLock = false;
let _isDark = false;

function applyCanvasTheme() {
    document.querySelectorAll('#alphaTab canvas, #alphaTab svg').forEach(c => {
        c.style.filter = _isDark ? 'invert(1) brightness(0.88)' : 'none';
    });
}

const canvasObs = new MutationObserver(() => {
    applyCanvasTheme();
});
canvasObs.observe(document.getElementById('alphaTab'), { childList: true, subtree: true });

window.setTheme = (isDark) => {
    _isDark = isDark;
    document.body.classList.toggle('dark', isDark);
    document.documentElement.classList.toggle('dark', isDark);
    applyCanvasTheme();
};

window.initSettings = (isDark, isPractice, speed, scale, displayMode, localeTag) => {
    window.setTheme(isDark);
    _locale = localeTag || 'uk';
    if (window.setPracticeModeLayout) window.setPracticeModeLayout(isPractice);
    if (window.setPlaybackSpeed) window.setPlaybackSpeed(speed);
    if (window.setTabScale) window.setTabScale(scale);
    if (window.setTabDisplayMode) window.setTabDisplayMode(displayMode);
};

window.setPracticeModeSpeed = (isPractice) => {
    if (!api) return;
    api.playbackSpeed = isPractice ? 0.5 : 1.0;
};

window.setPracticeModeLayout = (isPractice) => {
    if (!api) return;
    api.settings.display.layoutMode = 0;
    api.settings.player.scrollOffsetX = 0;
    api.settings.player.scrollOffsetY = 0;
    api.updateSettings();
};

window.setLoopRange = (startMeasure, endMeasure, isLooping) => {
    if (!api || !api.score) return;
    api.isLooping = isLooping;
    if (isLooping && startMeasure > 0 && endMeasure > 0) {
        const startMasterBar = api.score.masterBars[startMeasure - 1];
        const endMasterBar = api.score.masterBars[endMeasure - 1];
        if (!startMasterBar) return;

        let endTick = 0;
        const nextBar = api.score.masterBars[endMeasure];
        if (nextBar) endTick = nextBar.start - 1;
        else if (endMasterBar) endTick = endMasterBar.start + 999999;

        api.playbackRange = { startTick: startMasterBar.start, endTick };
        if (api.tickPosition < startMasterBar.start || api.tickPosition > endTick) {
            api.tickPosition = startMasterBar.start;
        }
    } else {
        api.playbackRange = null;
    }
};

function signalReady() {
    window.__tabViewerReady = true;
    if (window.Android?.onJsReady) window.Android.onJsReady();
    else setTimeout(signalReady, 100);
}

function stopMetronome() {
    if (_metronomeTimer != null) {
        clearInterval(_metronomeTimer);
        _metronomeTimer = null;
    }
}

function ensureMetronomeContext() {
    if (!_metronomeCtx) {
        const Ctx = window.AudioContext || window.webkitAudioContext;
        if (!Ctx) return false;
        _metronomeCtx = new Ctx();
    }
    if (_metronomeCtx.state === 'suspended') {
        _metronomeCtx.resume();
    }
    return true;
}

function playMetronomeClick() {
    if (!ensureMetronomeContext()) return;
    const now = _metronomeCtx.currentTime;
    const osc = _metronomeCtx.createOscillator();
    const gain = _metronomeCtx.createGain();
    osc.type = 'square';
    osc.frequency.value = 1600;
    gain.gain.setValueAtTime(0.0001, now);
    gain.gain.exponentialRampToValueAtTime(0.18, now + 0.005);
    gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.05);
    osc.connect(gain);
    gain.connect(_metronomeCtx.destination);
    osc.start(now);
    osc.stop(now + 0.055);
}

function restartMetronome() {
    stopMetronome();
    if (!_metronomeEnabled) return;
    const intervalMs = Math.max(80, Math.round(60000 / _metronomeBpm));
    playMetronomeClick();
    _metronomeTimer = setInterval(playMetronomeClick, intervalMs);
}

function postStatus(msg) {
    console.log('AlphaTabStatus:' + msg);
    if (window.Android?.onAlphaTabStatus) window.Android.onAlphaTabStatus(msg);
}

window.onerror = function(message, source, lineno, colno, error) {
    const details = error?.stack ? (` | ${error.stack.split('\n')[0]}`) : '';
    postStatus(`jsError:${message}${details}`);
    return false;
};

window.addEventListener('error', function(e) {
    const src = e.filename ? (` @${e.filename}:${e.lineno}:${e.colno}`) : '';
    postStatus(`errorEvent:${e.message}${src}`);
});

window.addEventListener('unhandledrejection', function(e) {
    postStatus(`promiseRejection:${e?.reason?.message || e?.reason || 'unknown'}`);
});

function startPoller() {
    stopPoller();
    _lastTick = -1;
    _poll = setInterval(() => {
        if (!api) return;
        try {
            const tValue = api.tickPosition;
            if (tValue == null || tValue === _lastTick) return;
            _lastTick = tValue;
            sendForTick(tValue);
        } catch {
            stopPoller();
        }
    }, 100);
}

function stopPoller() {
    if (_poll != null) {
        clearInterval(_poll);
        _poll = null;
    }
}

function getBarIndexForTick(tick) {
    if (!api?.score?.masterBars || api.score.masterBars.length === 0) return null;
    let idx = 0;
    for (let i = 0; i < api.score.masterBars.length; i++) {
        const bar = api.score.masterBars[i];
        if (bar.start <= tick) idx = i;
        else break;
    }
    return idx + 1;
}

function clearRestoreTimers() {
    if (!_restoreTimers || _restoreTimers.length === 0) return;
    _restoreTimers.forEach(id => { try { clearTimeout(id); } catch {} });
    _restoreTimers = [];
}

function scheduleRestore(fn, delay) {
    const id = setTimeout(() => {
        _restoreTimers = _restoreTimers.filter(t => t !== id);
        fn();
    }, delay);
    _restoreTimers.push(id);
    return id;
}

function ensureCursorInView() {
    try {
        const container = document.getElementById('alphaTabContainer');
        if (!container) return;
        if (typeof api?.scrollToCursor === 'function') {
            try { api.scrollToCursor(); } catch {}
        }
        const cursorEl = document.querySelector('.at-cursor-bar') || document.querySelector('.at-cursor-beat');
        if (!cursorEl) return;
        const cRect = container.getBoundingClientRect();
        const r = cursorEl.getBoundingClientRect();
        const margin = Math.max(32, Math.floor(cRect.height * 0.18));
        const isAbove = r.top < cRect.top + margin;
        const isBelow = r.bottom > cRect.bottom - margin;
        if (isAbove || isBelow) {
            const target = (r.top + r.bottom) / 2 - (cRect.top + cRect.height / 2);
            container.scrollTop += target;
        }
    } catch {}
}

function normalizeScoreLoudness(score) {
    if (!api || !score?.tracks || score.tracks.length === 0) return;
    const trackVolumes = score.tracks
        .map(track => track?.playbackInfo?.volume)
        .filter(volume => typeof volume === 'number');
    if (trackVolumes.length === 0) return;

    const maxVolume = Math.max(...trackVolumes);
    const targetMaxVolume = 15;
    const boost = Math.max(0, targetMaxVolume - maxVolume);
    if (boost > 0) {
        score.tracks.forEach(track => {
            if (!track?.playbackInfo) return;
            const current = typeof track.playbackInfo.volume === 'number'
                ? track.playbackInfo.volume
                : targetMaxVolume;
            track.playbackInfo.volume = Math.min(targetMaxVolume, current + boost);
        });
    }
    api.masterVolume = _silentMode ? 0.0 : 1.0;
}

function detectScoreTempo(score) {
    try {
        const firstBar = score?.masterBars && score.masterBars.length > 0 ? score.masterBars[0] : null;
        const direct = firstBar?.tempo;
        if (typeof direct === 'number' && Number.isFinite(direct)) return Math.round(direct);
        const automationVal = firstBar?.tempoAutomation?.value;
        if (typeof automationVal === 'number' && Number.isFinite(automationVal)) return Math.round(automationVal);
        const scoreTempo = score?.tempo;
        if (typeof scoreTempo === 'number' && Number.isFinite(scoreTempo)) return Math.round(scoreTempo);
    } catch {}
    return 90;
}

function applyRestore() {
    if (!api || !api.score) return;
    const token = _restoreToken;
    let targetTick = _restoreTick;
    if (_restoreBarIndex != null && _restoreBarIndex > 0 && api.score.masterBars && api.score.masterBars.length >= _restoreBarIndex) {
        const bar = api.score.masterBars[_restoreBarIndex - 1];
        if (bar && bar.start != null) targetTick = bar.start;
    }
    if (targetTick == null) return;

    const requestedBar = _restoreBarIndex ?? -1;
    postStatus(`restore:apply start tick=${targetTick} requestedBar=${requestedBar} play=${_restorePlay} attempt=${_restoreAttempts + 1}`);
    _initialCursorSet = true;
    try { api.tickPosition = targetTick; } catch {}
    ensureCursorInView();
    scheduleRestore(() => { if (token === _restoreToken) ensureCursorInView(); }, 60);
    scheduleRestore(() => { if (token === _restoreToken) ensureCursorInView(); }, 180);
    const verifyApplied = () => {
        if (token !== _restoreToken) return;
        let currentTick = 0;
        let currentBar = 0;
        try {
            currentTick = api.tickPosition ?? 0;
            currentBar = getBarIndexForTick(currentTick) ?? 0;
        } catch {}
        const reached = requestedBar > 1
            ? (currentBar > 0 && Math.abs(currentBar - requestedBar) <= 1)
            : currentTick > 0;
        postStatus(`restore:verify tick=${currentTick} currentBar=${currentBar} requestedBar=${requestedBar} reached=${reached} attempt=${_restoreAttempts + 1}`);
        if (reached) {
            ensureCursorInView();
            scheduleRestore(() => { if (token === _restoreToken) ensureCursorInView(); }, 50);
            scheduleRestore(() => {
                if (token !== _restoreToken) return;
                try {
                    const appliedTick = api.tickPosition ?? 0;
                    const appliedBar = getBarIndexForTick(appliedTick) ?? 0;
                    postStatus(`restore:applied tick=${appliedTick} currentBar=${appliedBar} requestedBar=${requestedBar}`);
                    if (window.Android?.onRestoreApplied) {
                        window.Android.onRestoreApplied(appliedTick, appliedBar, requestedBar);
                    }
                } catch {}
                if (_restorePlay) {
                    tryResumeAudio();
                    try { api.play(); } catch {}
                }
                _restoreTick = null;
                _restoreBarIndex = null;
                _restorePlay = false;
                _restoreAttempts = 0;
                clearRestoreTimers();
                postStatus('restore:apply end');
            }, 120);
            return;
        }
        _restoreAttempts += 1;
        if (_restoreAttempts < 8) {
            scheduleRestore(() => {
                if (token === _restoreToken) applyRestore();
            }, 220);
        } else {
            postStatus('restore:verify failed max attempts');
            _restoreTick = null;
            _restoreBarIndex = null;
            _restorePlay = false;
            _restoreAttempts = 0;
            clearRestoreTimers();
            postStatus('restore:apply end');
        }
    };
    scheduleRestore(verifyApplied, 220);
}

window.setRestorePlayback = (tick, isPlaying, barIndex) => {
    postStatus(`restore:set tick=${tick} bar=${barIndex} play=${isPlaying}`);
    clearRestoreTimers();
    _restoreToken += 1;
    _restoreTick = tick;
    _restorePlay = isPlaying;
    _restoreBarIndex = (barIndex != null && barIndex > 0) ? barIndex : null;
    _restoreAttempts = 0;
    applyRestore();
};

window.setOrientation = (isLandscape) => {
    document.body.classList.toggle('landscape', isLandscape);
};

window.getPlaybackState = () => {
    if (!api) return JSON.stringify({ tick: 0, playing: false });
    return JSON.stringify({ tick: api.tickPosition ?? 0, playing: api.playerState === 1 });
};

function initAlphaTab() {
    if (api) return;
    try {
        postStatus('init');
        if (!window.alphaTab || !window.alphaTab.AlphaTabApi) {
            postStatus('noAlphaTab');
            if (!_initRetry) {
                _initRetry = setTimeout(() => {
                    _initRetry = null;
                    initAlphaTab();
                }, 120);
            }
            return;
        }
        ensureBody();
        postStatus('creatingApi');
        api = new alphaTab.AlphaTabApi(document.querySelector('#alphaTab'), {
            core: {
                engine: 'svg',
                scriptFile: 'https://appassets.androidplatform.net/assets/alphatab_local.js',
                fontDirectory: 'https://appassets.androidplatform.net/assets/alphatab_font/',
                useWorkers: false
            },
            display: {
                layoutMode: 0,
                staveProfile: 'Tab',
                padding: [4, 64, 4, 8]
            },
            player: {
                enablePlayer: true,
                enableCursor: true,
                enableAnimatedBeatCursor: true,
                cursorMode: 1,
                scrollElement: document.querySelector('#alphaTabContainer'),
                scrollOffsetX: 0,
                scrollOffsetY: 0,
                scrollMode: alphaTab.ScrollMode.OffScreen,
                outputMode: 1
            }
        });
        api.settings.player.enableCursor = true;
        api.settings.player.enableAnimatedBeatCursor = true;
        api.updateSettings();
        postStatus('apiReady');
        if (_pendingSoundFontBase64) {
            const bytes = base64ToUint8Array(_pendingSoundFontBase64);
            api.loadSoundFont(bytes, false);
            _pendingSoundFontBase64 = null;
            postStatus('soundFontLoad:queued');
        }
        if (_pendingTabBase64) {
            const bytes = base64ToUint8Array(_pendingTabBase64);
            _pendingTabBase64 = null;
            api.load(bytes);
            postStatus('tabLoad:queued');
        } else if (_pendingTabPath) {
            const path = _pendingTabPath;
            _pendingTabPath = null;
            api.load(path.startsWith('/') ? `file://${path}` : path);
            postStatus('tabLoad:queued');
        }
        api.playerReady.on(() => {
            postStatus('playerReady');
            if (_pendingSoundFontBase64) {
                const bytes = base64ToUint8Array(_pendingSoundFontBase64);
                api.loadSoundFont(bytes, false);
                _pendingSoundFontBase64 = null;
                postStatus('soundFontLoad:queued');
            }
        });
        api.soundFontLoaded.on(() => { postStatus('soundFontLoaded'); });
        if (api.soundFontLoadFailed) {
            api.soundFontLoadFailed.on(() => { postStatus('soundFontLoadFailed'); });
        }
        api.playerStateChanged.on(args => { postStatus(`playerState:${args?.state}`); });
        api.scoreLoaded.on(score => {
            normalizeScoreLoudness(score);
            const detectedTempo = detectScoreTempo(score);
            _metronomeBpm = Math.max(40, Math.min(240, detectedTempo));
            if (window.Android?.onDetectedTempo) {
                window.Android.onDetectedTempo(_metronomeBpm);
            }
            setTimeout(() => runFullAnalysis(score), 400);
            if (_pendingScale != null) {
                api.settings.display.scale = _pendingScale;
                api.updateSettings();
            }
            api.settings.player.enableCursor = true;
            api.settings.player.enableAnimatedBeatCursor = true;
            api.updateSettings();
        });
        api.renderFinished.on(() => {
            _scoreLoadedFired = true;
            if (window.Android?.onScoreLoaded) {
                const totalMeasures = api.score && api.score.masterBars ? api.score.masterBars.length : 0;
                window.Android.onScoreLoaded(totalMeasures);
            }
            if (_restoreTick != null) applyRestore();
            api.settings.player.enableCursor = true;
            api.settings.player.enableAnimatedBeatCursor = true;
            api.updateSettings();
        });
        api.playerPositionChanged.on(args => {
            if (args?.currentBeat) sendForBeat(args.currentBeat);
            try {
                let barIndex = args?.currentBeat?.voice?.bar?.index;
                if (barIndex != null) barIndex = barIndex + 1;
                if (barIndex == null) barIndex = getBarIndexForTick(api.tickPosition ?? 0);
                if (window.Android?.onPlaybackProgress && barIndex != null) {
                    window.Android.onPlaybackProgress(api.tickPosition ?? 0, api.playerState === 1, barIndex);
                } else if (window.Android?.onTickPosition) {
                    window.Android.onTickPosition(api.tickPosition ?? 0, api.playerState === 1);
                }
            } catch {}
        });
        api.playerStateChanged.on(args => {
            if (args?.state === 1) startPoller();
            else stopPoller();
            try {
                if (window.Android?.onTickPosition) {
                    window.Android.onTickPosition(api.tickPosition ?? 0, api.playerState === 1);
                }
            } catch {}
        });
        if (_tickReportTimer == null) {
            _tickReportTimer = setInterval(() => {
                try {
                    if (!api) return;
                    const tick = api.tickPosition ?? 0;
                    const barIndex = getBarIndexForTick(tick);
                    if (window.Android?.onPlaybackProgress && barIndex != null) {
                        window.Android.onPlaybackProgress(tick, api.playerState === 1, barIndex);
                    } else if (window.Android?.onTickPosition) {
                        window.Android.onTickPosition(tick, api.playerState === 1);
                    }
                } catch {}
            }, 500);
        }
        api.beatMouseDown.on(beat => {
            if (!beat) return;
            sendForBeat(beat);
            try {
                const tValue = beat.start ?? beat.absoluteDisplayStart;
                if (tValue != null) api.tickPosition = tValue;
            } catch {}
        });
        api.error.on(() => {
            postStatus('error');
            _scoreLoadedFired = true;
            if (window.Android?.onScoreLoaded) {
                window.Android.onScoreLoaded(0);
            }
        });
    } catch (e) {
        postStatus(`initError:${e?.message || 'unknown'}`);
        console.error('AlphaTab init:', e);
    }

    setInterval(() => {
        const cursors = document.querySelectorAll('.at-cursor-beat');
        cursors.forEach(c => {
            c.style.setProperty('display', 'block', 'important');
            c.style.setProperty('visibility', 'visible', 'important');
            c.style.setProperty('opacity', '1', 'important');
            c.style.setProperty('fill', 'var(--cursor-fill)', 'important');
            c.style.setProperty('stroke', 'var(--cursor-stroke)', 'important');
            c.style.setProperty('stroke-width', '3px', 'important');
            if (c.tagName && c.tagName.toLowerCase() === 'rect') {
                c.setAttribute('width', '12');
                c.setAttribute('rx', '2');
            }
        });
    }, 100);
}

function base64ToUint8Array(base64) {
    const binary = atob(base64);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) {
        bytes[i] = binary.charCodeAt(i);
    }
    return bytes;
}

function ensureApi() {
    if (!api) initAlphaTab();
    return !!api;
}

window.loadSoundFontFromBase64 = (base64) => {
    if (!ensureApi()) {
        postStatus('apiNotReady');
        _pendingSoundFontBase64 = base64;
        return;
    }
    if (!api?.player) {
        _pendingSoundFontBase64 = base64;
        postStatus('soundFontLoad:pending');
        return;
    }
    const bytes = base64ToUint8Array(base64);
    api.loadSoundFont(bytes, false);
};

window.loadTab = (path) => {
    if (!ensureApi()) {
        postStatus('apiNotReady');
        _pendingTabPath = path;
        return;
    }
    _scoreLoadedFired = false;
    api.load(path.startsWith('/') ? `file://${path}` : path);
};

window.loadTabFromBase64 = (base64) => {
    if (!ensureApi()) {
        postStatus('apiNotReady');
        _pendingTabBase64 = base64;
        return;
    }
    _scoreLoadedFired = false;
    const bytes = base64ToUint8Array(base64);
    api.load(bytes);
};

function tryResumeAudio() {
    try {
        const output = api?.player?.output;
        if (output?.activate) output.activate();
        const ctx = output?._context;
        if (ctx && (ctx.state === 'suspended' || ctx.state === 'interrupted')) {
            ctx.resume();
        }
    } catch {}
}

window.playPause = () => { if (api) { tryResumeAudio(); api.playPause(); } };
window.stopAudio = () => { if (api) { api.stop(); stopPoller(); } stopMetronome(); };
window.setPlaybackSpeed = (s) => { if (api) api.playbackSpeed = parseFloat(s); };
window.setRestoreLock = (locked) => {
    _restoreLock = !!locked;
    document.body.classList.toggle('restoring', _restoreLock);
    postStatus(`restore:lock=${_restoreLock}`);
};
window.setSilentMode = (enabled) => {
    _silentMode = !!enabled;
    if (api) api.masterVolume = _silentMode ? 0.0 : 1.0;
    postStatus(`silentMode=${_silentMode}`);
};
window.setMetronomeEnabled = (enabled) => {
    _metronomeEnabled = !!enabled;
    restartMetronome();
    postStatus(`metronome:enabled=${_metronomeEnabled}`);
};
window.setMetronomeBpm = (bpm) => {
    const parsed = parseInt(bpm, 10);
    _metronomeBpm = Number.isFinite(parsed) ? Math.max(40, Math.min(240, parsed)) : 90;
    if (_metronomeEnabled) restartMetronome();
    postStatus(`metronome:bpm=${_metronomeBpm}`);
};
window.getTrackOptions = () => {
    if (!api?.score?.tracks?.length) return JSON.stringify([]);
    const selected = getActiveTrack(api.score)?.index ?? _selectedTrackIndex;
    return JSON.stringify(api.score.tracks.map((track, i) => ({
        index: track.index ?? i,
        name: (track.name || track.shortName || `Track ${i + 1}`).trim(),
        selected: (track.index ?? i) === selected
    })));
};
window.applyLearningTools = (trackIndex, transposeSemitones) => {
    if (!api?.score?.tracks?.length) return false;
    const parsedTrackIndex = parseInt(trackIndex, 10);
    const parsedTranspose = parseInt(transposeSemitones, 10);
    const nextTrackIndex = Number.isFinite(parsedTrackIndex) ? parsedTrackIndex : 0;
    const nextTranspose = Number.isFinite(parsedTranspose) ? Math.max(-36, Math.min(36, parsedTranspose)) : 0;
    const track = api.score.tracks.find(t => t.index === nextTrackIndex) || api.score.tracks[0];
    if (!track) return false;

    _selectedTrackIndex = track.index ?? nextTrackIndex;
    _transposeSemitones = nextTranspose;
    const trackCount = api.score.tracks.length;
    const pitches = new Array(trackCount).fill(0);
    if (_selectedTrackIndex >= 0 && _selectedTrackIndex < trackCount) {
        pitches[_selectedTrackIndex] = _transposeSemitones;
    }

    const wasPlaying = api.playerState === 1;
    const tick = api.tickPosition ?? 0;
    if (_restoreTick == null && _restoreBarIndex == null) {
        _restoreTick = tick;
        _restorePlay = wasPlaying;
        _restoreBarIndex = getBarIndexForTick(tick);
    }

    api.settings.notation.transpositionPitches = pitches;
    api.settings.notation.displayTranspositionPitches = pitches.slice();
    api.updateSettings();
    api.renderTracks([track]);
    runFullAnalysis(api.score);
    postStatus(`learningTools:track=${_selectedTrackIndex}:transpose=${_transposeSemitones}`);
    return true;
};
window.setTabScale = (s) => {
    _pendingScale = parseFloat(s);
    if (!api) return;
    if (_restoreLock) {
        api.settings.display.scale = _pendingScale;
        api.updateSettings();
        return;
    }
    if (!api.score) {
        api.settings.display.scale = _pendingScale;
        api.updateSettings();
        return;
    }
    const wasPlaying = api.playerState === 1;
    const tick = api.tickPosition ?? 0;
    if (_restoreTick == null && _restoreBarIndex == null) {
        _restoreTick = tick;
        _restorePlay = wasPlaying;
        _restoreBarIndex = getBarIndexForTick(tick);
    }
    api.settings.display.scale = _pendingScale;
    api.updateSettings();
    if (api.score) api.render();
};
window.setTabDisplayMode = (mode) => {
    if (!api) return;
    if (_restoreLock) {
        if (window.alphaTab && window.alphaTab.StaveProfile) {
            api.settings.display.staveProfile = window.alphaTab.StaveProfile[mode] ?? api.settings.display.staveProfile;
        } else {
            api.settings.display.staveProfile = mode;
        }
        api.updateSettings();
        return;
    }
    if (!api.score) {
        if (window.alphaTab && window.alphaTab.StaveProfile) {
            api.settings.display.staveProfile = window.alphaTab.StaveProfile[mode] ?? api.settings.display.staveProfile;
        } else {
            api.settings.display.staveProfile = mode;
        }
        api.updateSettings();
        return;
    }
    const wasPlaying = api.playerState === 1;
    const tick = api.tickPosition ?? 0;
    if (_restoreTick == null && _restoreBarIndex == null) {
        _restoreTick = tick;
        _restorePlay = wasPlaying;
        _restoreBarIndex = getBarIndexForTick(tick);
    }
    if (window.alphaTab && window.alphaTab.StaveProfile) {
        api.settings.display.staveProfile = window.alphaTab.StaveProfile[mode] ?? api.settings.display.staveProfile;
    } else {
        api.settings.display.staveProfile = mode;
    }
    api.updateSettings();
    if (api.score) api.render();
};

initAlphaTab();
signalReady();
