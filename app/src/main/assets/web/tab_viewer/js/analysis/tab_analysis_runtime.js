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

function buildBeatTechniques(beat) {
    return {
        fadeIn: !!beat.fadeIn,
        rasgueado: !!beat.hasRasgueado,
        tremoloPicking: !!beat.tremoloSpeed,
        graceOnBeat: beat.graceType === 1,
        graceBeforeBeat: beat.graceType === 2,
        graceBend: beat.graceType === 3,
        whammyBar: !!beat.hasWhammyBar || (!!beat.whammyBarPoints && beat.whammyBarPoints.length > 0) || ((beat.whammyBarType ?? 0) !== 0),
        beatVibrato: (beat.vibrato ?? 0) !== 0,
        brushUp: beat.brushType === 1,
        brushDown: beat.brushType === 2,
        arpeggioUp: beat.brushType === 3,
        arpeggioDown: beat.brushType === 4,
        pickStrokeUp: beat.pickStroke === 1,
        pickStrokeDown: beat.pickStroke === 2,
        crescendo: beat.crescendo === 1,
        decrescendo: beat.crescendo === 2,
        slap: !!beat.slap,
        pop: !!beat.pop
    };
}

function mergeBeatTechniques(target, source) {
    if (!target || !source) return target || source || null;
    for (const key of Object.keys(target)) {
        target[key] = !!target[key] || !!source[key];
    }
    return target;
}

function noteFlagsForCompact(note, rawNote) {
    const flags = [];
    if (note.isTieDestination || rawNote?.isTieDestination) flags.push('tie');
    if (rawNote?.isHammer) flags.push('hammer');
    if (rawNote?.isPullOff) flags.push('pull-off');
    if (rawNote?.slideIn || rawNote?.slideOut) flags.push('slide');
    if (rawNote?.hasBend) flags.push('bend');
    if (rawNote?.vibrato) flags.push('vibrato');
    if (rawNote?.isPalmMute) flags.push('palm-mute');
    if (rawNote?.hasHarmonic) flags.push('harmonic');
    if (rawNote?.isTrill) flags.push('trill');
    if (rawNote?.isGhost) flags.push('ghost');
    if (rawNote?.isLetRing) flags.push('let-ring');
    if (rawNote?.isStaccato) flags.push('staccato');
    if (rawNote?.accent) flags.push('accent');
    if (note.isTapped) flags.push('tapping');
    return flags;
}

function beatFlagsForCompact(beatTechniques) {
    if (!beatTechniques) return [];
    const flags = [];
    if (beatTechniques.fadeIn) flags.push('fade-in');
    if (beatTechniques.rasgueado) flags.push('rasgueado');
    if (beatTechniques.tremoloPicking) flags.push('tremolo-picking');
    if (beatTechniques.graceOnBeat) flags.push('grace-on-beat');
    if (beatTechniques.graceBeforeBeat) flags.push('grace-before-beat');
    if (beatTechniques.graceBend) flags.push('grace-bend');
    if (beatTechniques.whammyBar) flags.push('whammy-bar');
    if (beatTechniques.beatVibrato) flags.push('bar-vibrato');
    if (beatTechniques.brushUp) flags.push('brush-up');
    if (beatTechniques.brushDown) flags.push('brush-down');
    if (beatTechniques.arpeggioUp) flags.push('arpeggio-up');
    if (beatTechniques.arpeggioDown) flags.push('arpeggio-down');
    if (beatTechniques.pickStrokeUp) flags.push('pick-up');
    if (beatTechniques.pickStrokeDown) flags.push('pick-down');
    if (beatTechniques.crescendo) flags.push('crescendo');
    if (beatTechniques.decrescendo) flags.push('decrescendo');
    if (beatTechniques.slap) flags.push('slap');
    if (beatTechniques.pop) flags.push('pop');
    return flags;
}

function compactNoteLabel(note, rawNote) {
    const stringIndex = note.string;
    const label = note.isDead ? 'x' : note.fret;
    const base = t('stringLabel', { string: stringIndex, label, fret: note.fret });
    const flags = noteFlagsForCompact(note, rawNote);
    return flags.length ? `${base} [${flags.join(', ')}]` : base;
}

function compactEventSummary(event) {
    const noteLabels = event.notes.map((note, index) => compactNoteLabel(note, event.rawNotes[index]));
    const beatFlags = beatFlagsForCompact(event.beatTechniques);
    const eventFlags = [];
    if (event.isRest) eventFlags.push('rest');
    if (event.isTiedBeat) eventFlags.push('all-tied');
    eventFlags.push(...beatFlags);
    const suffix = eventFlags.length ? ` | flags: ${eventFlags.join(', ')}` : '';

    if (event.isRest) {
        return `pause${suffix}`;
    }

    if (!noteLabels.length) {
        return `empty event${suffix}`;
    }

    return `${noteLabels.join(', ')}${suffix}`;
}

function extractBeatData(track) {
    const data = [];
    for (const bar of track.staves[0].bars) {
        for (const voice of bar.voices) {
            if (voice.isEmpty) continue;
            for (const beat of voice.beats) {
                const notes = beat.notes.filter(note => note.fret != null);
                if (!notes.length && !beat.isRest) continue;
                const durationValue = typeof beat.duration === 'object' ? beat.duration.value : beat.duration;
                data.push({
                    beat,
                    beats: [beat],
                    barIdx: bar.index,
                    duration: DUR_BEATS[durationValue] ?? 1,
                    isRest: !!beat.isRest,
                    beatTechniques: buildBeatTechniques(beat),
                    notes: notes.map(note => ({
                        string: normalizeStringIndex(note.string),
                        fret: note.fret,
                        isDead: !!note.isDead,
                        isTapped: !!note.isTapped || !!note.isLeftHandTapped || !!beat.tap,
                        isTieDestination: !!note.isTieDestination
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
                        harmonicType: note.harmonicType || 0,
                        isTrill: !!note.isTrill,
                        isGhost: !!note.isGhost,
                        isDead: !!note.isDead,
                        isLetRing: !!note.isLetRing,
                        isStaccato: !!note.isStaccato,
                        accent: note.accentuated || 0,
                        isTieDestination: !!note.isTieDestination
                    })),
                    isTapping: notes.some(note => !!note.isTapped || !!note.isLeftHandTapped) || !!beat.tap,
                    isTiedBeat: notes.length > 0 && notes.every(note => !!note.isTieDestination)
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
            prev.isRest = prev.isRest && event.isRest;
            prev.isTapping = prev.isTapping || event.isTapping;
            prev.isTiedBeat = prev.isTiedBeat && event.isTiedBeat;
            prev.beatTechniques = mergeBeatTechniques(prev.beatTechniques, event.beatTechniques);
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
    const events = extractBeatData(track);
    let currentBar = -1;
    let eventNumber = 1;

    for (const event of events) {
        if (event.barIdx !== currentBar) {
            currentBar = event.barIdx;
            eventNumber = 1;
            compactTabs += `Measure ${currentBar + 1}:\n`;
        }

        compactTabs += `  Event ${eventNumber} inside this measure: ${compactEventSummary(event)}\n`;
        eventNumber += 1;
    }

    return compactTabs;
}

function attackedPlayablePairs(candidate) {
    return attackedNotes(candidate.notes)
        .filter(note => !note.isDead && !note.isTapped && note.fret > 0)
        .map(note => `${note.string}:${note.fret}`);
}

function analysisRichness(candidate) {
    let score = activeFrets(candidate.notes).length;
    if (candidate.notes.some(note => note.isTieDestination)) score += 2;
    if (candidate.notes.length > 1) score += 1;
    return score;
}

function findCanonicalAnalysisIndex(analyzedBeats, index) {
    const current = analyzedBeats[index];
    const attackedPairs = attackedPlayablePairs(current);
    if (attackedPairs.length !== 1) return index;

    const targetPair = attackedPairs[0];
    let bestIndex = index;
    let bestScore = analysisRichness(current);

    for (let offset = 1; offset <= 2; offset++) {
        for (const neighborIndex of [index - offset, index + offset]) {
            const neighbor = analyzedBeats[neighborIndex];
            if (!neighbor || neighbor.barIdx !== current.barIdx) continue;
            if (!neighbor.notes.some(note => `${note.string}:${note.fret}` === targetPair)) continue;

            const score = analysisRichness(neighbor);
            if (score > bestScore) {
                bestScore = score;
                bestIndex = neighborIndex;
            }
        }
    }

    return bestIndex;
}

function debugAnalysisSummary(json) {
    try {
        const parsed = JSON.parse(json);
        const left = (parsed.leftHand || [])
            .map(note => `${note.string}:${note.fret}:${note.finger}`)
            .join('|');
        const right = (parsed.rightHand || [])
            .map(note => `${note.string}:${note.finger}`)
            .join('|');
        const instructions = (parsed.instructions || []).slice(0, 2).join(' || ');
        return `bar=${parsed.barIndex};left=${left};right=${right};instr=${instructions}`;
    } catch {
        return 'parseError';
    }
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
        if (typeof postStatus === 'function') {
            postStatus(`debugBeat:sendForTick tick=${tick} bestIndex=${best} sourceTick=${tickArr[best].tick} ${debugAnalysisSummary(tickArr[best].json)}`);
        }
        postToAndroid(tickArr[best].json);
    }
}

function sendForBeat(beat) {
    if (!beat) return;
    const key = beatKey(beat);
    if (key && beatMap.has(key)) {
        if (typeof postStatus === 'function') {
            postStatus(`debugBeat:sendForBeat keyHit key=${key} tick=${getBeatTick(beat)} ${debugAnalysisSummary(beatMap.get(key))}`);
        }
        postToAndroid(beatMap.get(key));
        return;
    }

    const tick = getBeatTick(beat);
    if (tick != null) {
        if (typeof postStatus === 'function') {
            postStatus(`debugBeat:sendForBeat fallback key=${key} tick=${tick}`);
        }
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
        const canonicalIndexes = analyzedBeats.map((_, index) => findCanonicalAnalysisIndex(analyzedBeats, index));
        const analysisJsonByIndex = analyzedBeats.map((_, index) => {
            const canonical = analyzedBeats[canonicalIndexes[index]];
            const next = analyzedBeats[canonicalIndexes[index] + 1] || null;
            return JSON.stringify(buildAnalysis(canonical.barIdx, canonical, next));
        });

        let firstJson = null;
        for (let i = 0; i < analyzedBeats.length; i++) {
            const current = analyzedBeats[i];
            const json = analysisJsonByIndex[i];
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
