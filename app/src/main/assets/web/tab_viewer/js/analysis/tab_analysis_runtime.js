const beatMap = new Map();
const tickArr = [];
let _selectedTrackIndex = 0;
let _transposeSemitones = 0;

function beatKey(beat) {
    try {
        const barIndex = beat.voice?.bar?.index ?? 0;
        const voiceIndex = beat.voice?.index ?? 0;
        const beatIndex = beat.index ?? 0;
        const tick = getBeatTick(beat);
        return `${barIndex}_${voiceIndex}_${beatIndex}_${tick}`;
    } catch {
        return null;
    }
}

function getActiveTrack(score) {
    if (!score?.tracks?.length) return null;
    return score.tracks.find(track => track.index === _selectedTrackIndex) || score.tracks[0] || null;
}

function extractBeatData(track) {
    const data = [];
    for (const bar of track.staves[0].bars) {
        for (const voice of bar.voices) {
            if (voice.isEmpty) continue;
            for (const beat of voice.beats) {
                const notes = beat.notes.filter(note => note.fret != null);
                if (!notes.length) continue;
                const durationValue = typeof beat.duration === 'object' ? beat.duration.value : beat.duration;
                data.push({
                    beat,
                    beats: [beat],
                    barIdx: bar.index,
                    duration: DUR_BEATS[durationValue] ?? 1,
                    notes: notes.map(note => ({
                        string: normalizeStringIndex(note.string),
                        fret: note.fret,
                        isDead: !!note.isDead,
                        isTapped: !!note.isTapped || !!note.isLeftHandTapped || !!beat.tap
                    })),
                    rawNotes: notes.map(note => ({
                        hasBend: !!note.hasBend,
                        vibrato: !!note.isVibrato,
                        slideIn: !!note.slideInType && note.slideInType !== 0,
                        slideOut: !!note.slideOutType && note.slideOutType !== 0,
                        isHammer: !!note.isHammerPullOrigin || !!note.isHammerPullDestination,
                        isPullOff: !!note.isHammerPullDestination,
                        legatoType: null,
                        isPalmMute: !!note.isPalmMute,
                        hasHarmonic: !!note.isHarmonic,
                        isTrill: !!note.isTrill,
                        isGhost: !!note.isGhost,
                        isDead: !!note.isDead,
                        isLetRing: !!note.isLetRing,
                        isSlap: !!note.slap,
                        isPop: !!note.pop,
                        accent: note.accentuated || 0
                    })),
                    isTapping: notes.some(note => !!note.isTapped || !!note.isLeftHandTapped) || !!beat.tap,
                    isTiedBeat: notes.every(note => !!note.isTieDestination)
                });
            }
        }
    }
    data.sort((a, b) => {
        const absA = a.beat.absoluteDisplayStart;
        const absB = b.beat.absoluteDisplayStart;
        if (absA != null && absB != null && absA !== absB) {
            return absA - absB;
        }
        if ((a.barIdx ?? 0) !== (b.barIdx ?? 0)) {
            return (a.barIdx ?? 0) - (b.barIdx ?? 0);
        }
        return getBeatLocalTick(a.beat) - getBeatLocalTick(b.beat);
    });

    const merged = [];
    for (const event of data) {
        const tick = getBeatTick(event.beat);
        const prev = merged[merged.length - 1];
        const prevTick = prev ? getBeatTick(prev.beat) : -1;
        if (prev && prev.barIdx === event.barIdx && prevTick === tick) {
            prev.notes.push(...event.notes);
            prev.rawNotes.push(...event.rawNotes);
            prev.beats.push(...event.beats);
            prev.duration = Math.max(prev.duration, event.duration);
            prev.isTapping = prev.isTapping || event.isTapping;
            prev.isTiedBeat = prev.isTiedBeat && event.isTiedBeat;
        } else {
            merged.push({
                ...event,
                notes: [...event.notes],
                rawNotes: [...event.rawNotes],
                beats: [...event.beats]
            });
        }
    }

    for (const event of merged) {
        dedupeEventNotes(event);
    }

    annotateLegatoDirections(merged);
    return merged;
}

function buildCompactTabs(track) {
    let compactTabs = '';
    for (const bar of track.staves[0].bars) {
        compactTabs += `Measure ${bar.index + 1}:\n`;
        for (const voice of bar.voices) {
            if (voice.isEmpty) continue;
            let eventNumber = 1;
            for (const beat of voice.beats) {
                const notes = beat.notes.filter(note => note.fret != null);
                if (!notes.length) continue;
                const noteLabels = notes.map(note => {
                    const stringIndex = normalizeStringIndex(note.string);
                    const label = note.isDead ? 'x' : note.fret;
                    return t('stringLabel', { string: stringIndex, label, fret: note.fret });
                });
                compactTabs += `  Event ${eventNumber} inside this measure: ${noteLabels.join(', ')}\n`;
                eventNumber += 1;
            }
        }
    }
    return compactTabs;
}

function postToAndroid(json) {
    if (json && window.Android?.postTabAnalysis) {
        window.Android.postTabAnalysis(json);
    }
}

function sendForTick(tick) {
    if (!tickArr.length) return;
    let low = 0;
    let high = tickArr.length - 1;
    let best = -1;

    while (low <= high) {
        const mid = (low + high) >> 1;
        if (tickArr[mid].tick <= tick) {
            best = mid;
            low = mid + 1;
        } else {
            high = mid - 1;
        }
    }

    if (best >= 0) {
        postToAndroid(tickArr[best].json);
    }
}

function sendForBeat(beat) {
    if (!beat) return;
    const key = beatKey(beat);
    if (key && beatMap.has(key)) {
        postToAndroid(beatMap.get(key));
        return;
    }

    const tick = getBeatTick(beat);
    if (tick != null) {
        sendForTick(tick);
    }
}

function runFullAnalysis(score) {
    beatMap.clear();
    tickArr.length = 0;
    try {
        const track = getActiveTrack(score);
        if (!track?.staves?.length) return;

        const beatData = extractBeatData(track);
        const analyzedBeats = optimizeBeatSequence(beatData);

        let firstJson = null;
        for (let i = 0; i < analyzedBeats.length; i++) {
            const current = analyzedBeats[i];
            const next = analyzedBeats[i + 1] || null;
            const analysis = buildAnalysis(current.barIdx, current, next);
            const json = JSON.stringify(analysis);
            for (const beat of current.beats || [current.beat]) {
                const key = beatKey(beat);
                if (key) beatMap.set(key, json);
            }
            tickArr.push({
                tick: getBeatTick(current.beat),
                json
            });
            if (!firstJson && !current.isTiedBeat) {
                firstJson = json;
            }
        }

        tickArr.sort((a, b) => a.tick - b.tick);
        if (firstJson) postToAndroid(firstJson);

        const compactTabs = buildCompactTabs(track);
        if (window.Android?.postCompactTabs) {
            window.Android.postCompactTabs(compactTabs);
        }
    } catch (error) {
        console.error('runFullAnalysis:', error);
    }
}
