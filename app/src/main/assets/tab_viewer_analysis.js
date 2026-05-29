function normalizeStringIndex(stringIndex) {
    if (typeof stringIndex !== 'number') return stringIndex;
    if (stringIndex < 1 || stringIndex > 6) return stringIndex;
    return 7 - stringIndex;
}

function ensureBody() {
    if (!document.body) {
        const body =
            document.getElementsByTagName('body')[0] ||
            document.documentElement ||
            document.createElement('body');
        document.body = body;
        if (document.documentElement && !body.parentNode) {
            document.documentElement.appendChild(body);
        }
    }
    if (document.body && !document.body.addEventListener) {
        document.body.addEventListener = function() {};
    }
}

function clamp(value, min, max) {
    return Math.min(Math.max(value, min), max);
}

function positiveFrets(notes) {
    return notes.map(note => note.fret).filter(fret => fret > 0);
}

function detectBarre(notes) {
    const fretted = notes.filter(note => note.fret > 0 && !note.isDead);
    if (fretted.length < 3) return null;

    const minFret = Math.min(...fretted.map(note => note.fret));
    const barreCandidates = fretted
        .filter(note => note.fret === minFret)
        .map(note => note.string)
        .sort((a, b) => a - b);

    if (barreCandidates.length < 3) return null;

    let longestRun = 1;
    let currentRun = 1;
    for (let i = 1; i < barreCandidates.length; i++) {
        if (barreCandidates[i] === barreCandidates[i - 1] + 1) {
            currentRun += 1;
            longestRun = Math.max(longestRun, currentRun);
        } else {
            currentRun = 1;
        }
    }

    return longestRun >= 3 ? minFret : null;
}

function chooseLeftHandPosition(notes) {
    const frets = positiveFrets(notes);
    if (!frets.length) return 1;
    return Math.max(1, Math.min(...frets));
}

function assignLeftHand(notes, position, barreFret) {
    const leftHand = {};
    const placement = {};

    for (const note of notes) {
        const key = `${note.string}_${note.fret}`;
        if (note.fret === 0) {
            leftHand[key] = '-';
            continue;
        }
        if (note.isDead) {
            leftHand[key] = t('deadLabel');
            continue;
        }
        if (barreFret !== null && note.fret === barreFret) {
            leftHand[key] = `${leftHandName(1)} (${t('possibleBarre', { fret: barreFret })})`;
            placement[note.string] = { fret: note.fret, finger: 1 };
            continue;
        }

        const relativeFinger = clamp(note.fret - position + 1, 1, 4);
        leftHand[key] = leftHandName(relativeFinger);
        placement[note.string] = { fret: note.fret, finger: relativeFinger };
    }

    return { leftHand, placement };
}

class RightHandHeuristics {
    constructor() {
        this.lastTrebleFinger = 'm';
    }

    nextTrebleFinger() {
        const cycle = ['i', 'm', 'a'];
        const currentIndex = cycle.indexOf(this.lastTrebleFinger);
        const nextIndex = currentIndex === -1 ? 0 : (currentIndex + 1) % cycle.length;
        this.lastTrebleFinger = cycle[nextIndex];
        return this.lastTrebleFinger;
    }

    assign(notes) {
        const result = {};
        const fullNames = {
            p: rightHandName('p'),
            i: rightHandName('i'),
            m: rightHandName('m'),
            a: rightHandName('a')
        };

        const ordered = [...notes].sort((a, b) => a.string - b.string);
        if (ordered.length === 1) {
            const note = ordered[0];
            if (note.string >= 4) {
                result[note.string] = fullNames.p;
            } else {
                const finger = this.nextTrebleFinger();
                result[note.string] = fullNames[finger];
            }
            return result;
        }

        for (const note of ordered) {
            if (note.string >= 4) {
                result[note.string] = fullNames.p;
                continue;
            }
            if (note.string === 3) result[note.string] = fullNames.i;
            else if (note.string === 2) result[note.string] = fullNames.m;
            else result[note.string] = fullNames.a;
        }

        return result;
    }
}

function extractTechniques(rawNotes) {
    return rawNotes.reduce((acc, note) => {
        acc.hasBend = acc.hasBend || !!note.hasBend;
        acc.vibrato = acc.vibrato || !!note.vibrato;
        acc.slide = acc.slide || !!note.slideIn || !!note.slideOut;
        acc.legato = acc.legato || !!note.isHammer || !!note.isPullOff;
        acc.palmMute = acc.palmMute || !!note.isPalmMute;
        acc.harmonic = acc.harmonic || !!note.hasHarmonic;
        acc.trill = acc.trill || !!note.isTrill;
        acc.ghost = acc.ghost || !!note.isGhost;
        acc.dead = acc.dead || !!note.isDead;
        acc.letRing = acc.letRing || !!note.isLetRing;
        acc.slap = acc.slap || !!note.isSlap;
        acc.pop = acc.pop || !!note.isPop;
        acc.accent = acc.accent || !!note.accent;
        return acc;
    }, {
        hasBend: false,
        vibrato: false,
        slide: false,
        legato: false,
        palmMute: false,
        harmonic: false,
        trill: false,
        ghost: false,
        dead: false,
        letRing: false,
        slap: false,
        pop: false,
        accent: false
    });
}

function buildInstructions(notes, rawNotes, leftHandMap, rightHandMap, barreFret, position, isTapping) {
    const instructions = [];
    const techniques = extractTechniques(rawNotes);

    if (isTapping) {
        instructions.push(t('tappingInstruction'));
        return instructions;
    }

    if (barreFret !== null) {
        instructions.push(t('positionBarre', { fret: barreFret }));
        instructions.push(t('barreHelp'));
    } else if (positiveFrets(notes).length) {
        instructions.push(t('positionAround', { fret: position }));
    }

    if (notes.length === 1) {
        const note = notes[0];
        const leftHandLabel = leftHandMap[`${note.string}_${note.fret}`] || '?';
        const rightHandLabel = rightHandMap[note.string] || rightHandName(note.string >= 4 ? 'p' : 'i');
        if (note.isDead) {
            instructions.push(t('leftHandMuted', { string: STRING_NAMES[note.string] }));
        } else if (note.fret === 0) {
            instructions.push(t('leftHandOpen', { string: STRING_NAMES[note.string] }));
        } else {
            instructions.push(t('leftHandFinger', {
                finger: leftHandLabel,
                string: STRING_NAMES[note.string],
                fret: note.fret
            }));
        }
        instructions.push(t('rightHandFinger', { finger: rightHandLabel }));
    } else {
        instructions.push(t('chord', { count: notes.length }));
        const summary = notes.map(note => {
            if (note.isDead) return `${STRING_SHORT[note.string]}-${t('mutedShort')}`;
            if (note.fret === 0) return `${STRING_SHORT[note.string]}-${t('openShort')}`;
            return `${STRING_SHORT[note.string]}-${note.fret}(${leftHandMap[`${note.string}_${note.fret}`] || '?'})`;
        });
        instructions.push(t('leftHandSummary', { items: summary.join(', ') }));
        const hasBass = notes.some(note => note.string >= 4);
        const hasTreble = notes.some(note => note.string <= 3);
        if (hasBass && hasTreble) {
            instructions.push(t('rightHandArpeggio'));
        } else {
            instructions.push(t('rightHandPluckTogether'));
        }
    }

    if (techniques.accent) instructions.push(t('accentInstruction'));
    if (techniques.ghost) instructions.push(t('ghostInstruction'));
    if (techniques.dead) instructions.push(t('deadInstruction'));
    if (techniques.slap) instructions.push(t('slapInstruction'));
    if (techniques.pop) instructions.push(t('popInstruction'));
    if (techniques.letRing) instructions.push(t('letRingInstruction'));
    if (techniques.hasBend) instructions.push(t('bendInstruction'));
    if (techniques.vibrato) instructions.push(t('vibratoInstruction'));
    if (techniques.slide) instructions.push(t('slideInstruction'));
    if (techniques.legato) {
        instructions.push(t('legatoInstruction'));
        instructions.push(t('tieInstruction'));
    }
    if (techniques.palmMute) instructions.push(t('palmMuteInstruction'));
    if (techniques.harmonic) instructions.push(t('harmonicInstruction'));
    if (techniques.trill) instructions.push(t('trillInstruction'));
    if (!instructions.length) instructions.push(t('plainNoteInstruction'));

    return instructions;
}

function buildContextHint(notes, rawNotes, barreFret, isTapping) {
    const techniques = extractTechniques(rawNotes);
    const frets = positiveFrets(notes);
    const width = frets.length ? Math.max(...frets) - Math.min(...frets) : 0;

    if (techniques.harmonic) return t('techniqueHarmonic');
    if (techniques.dead) return t('techniqueDead');
    if (techniques.hasBend) return t('techniqueBend');
    if (techniques.palmMute) return t('techniquePalmMute');
    if (techniques.trill) return t('techniqueTrill');
    if (techniques.ghost) return t('techniqueGhost');
    if (techniques.letRing) return t('techniqueLetRing');
    if (isTapping) return t('techniqueTapping');
    if (techniques.slap) return t('techniqueSlap');
    if (techniques.pop) return t('techniquePop');
    if (techniques.legato) return t('techniqueLegato');
    if (techniques.slide) return t('techniqueSlide');
    if (techniques.vibrato) return t('techniqueVibrato');
    if (barreFret !== null) return t('possibleBarre', { fret: barreFret });
    if (width > 3) return t('wideStretch');
    return null;
}

function buildRightHandPayload(notes, rightHandMap) {
    return notes.map(note => {
        const label = rightHandMap[note.string] || (note.string >= 4 ? rightHandName('p') : rightHandName('i'));
        const stringName = STRING_SHORT[note.string] || String(note.string);
        return {
            finger: label.charAt(0),
            fingerName: label,
            string: stringName,
            stringIndex: note.string,
            direction: label.charAt(0),
            color: note.string >= 4 ? '#8B6350' : '#3678B5'
        };
    });
}

function buildLeftHandPayload(notes, rawNotes, leftHandMap, isTapping) {
    return notes.map((note, index) => {
        const rawNote = rawNotes[index];
        const key = `${note.string}_${note.fret}`;
        const label = note.isDead
            ? t('deadLabel')
            : (note.fret === 0 ? t('openStringName') : (leftHandMap[key] || '?'));

        return {
            finger: note.isDead ? 'x' : (note.fret === 0 ? '0' : label.charAt(0)),
            fingerName: label,
            string: STRING_SHORT[note.string] || String(note.string),
            stringIndex: note.string,
            fret: String(note.fret),
            color: note.fret === 0 ? '#4FC3F7' : (note.isDead ? '#757575' : '#D07B30'),
            isDead: !!note.isDead,
            isHammer: !!rawNote.isHammer,
            isPullOff: !!rawNote.isPullOff,
            isSlide: !!rawNote.slideIn || !!rawNote.slideOut,
            isVibrato: !!rawNote.vibrato,
            isGhost: !!rawNote.isGhost,
            hasBend: !!rawNote.hasBend,
            isPalmMute: !!rawNote.isPalmMute,
            hasHarmonic: !!rawNote.hasHarmonic,
            isTrill: !!rawNote.isTrill,
            isLetRing: !!rawNote.isLetRing,
            isSlap: !!rawNote.isSlap,
            isPop: !!rawNote.isPop,
            isAccent: !!rawNote.accent,
            isTapping: !!isTapping
        };
    });
}

function analyzeBeat(beatData, rightHandState) {
    const position = chooseLeftHandPosition(beatData.notes);
    const barreFret = detectBarre(beatData.notes);
    const leftHandResult = assignLeftHand(beatData.notes, position, barreFret);
    const rightHandMap = rightHandState.assign(beatData.notes);

    return {
        ...beatData,
        position,
        barreFret,
        leftHandMap: leftHandResult.leftHand,
        leftHandPlacement: leftHandResult.placement,
        rightHandMap
    };
}

function buildAnalysis(barIdx, analyzedBeat, nextAnalyzedBeat) {
    const leftHand = buildLeftHandPayload(
        analyzedBeat.notes,
        analyzedBeat.rawNotes,
        analyzedBeat.leftHandMap,
        analyzedBeat.isTapping
    );
    const rightHand = buildRightHandPayload(analyzedBeat.notes, analyzedBeat.rightHandMap);
    const nextLeftHand = nextAnalyzedBeat
        ? buildLeftHandPayload(
            nextAnalyzedBeat.notes,
            nextAnalyzedBeat.rawNotes,
            nextAnalyzedBeat.leftHandMap,
            nextAnalyzedBeat.isTapping
        ).map(note => ({
            finger: note.finger,
            fingerName: note.fingerName,
            string: note.string,
            stringIndex: note.stringIndex,
            fret: note.fret,
            color: note.color,
            isDead: note.isDead
        }))
        : [];

    return {
        barIndex: barIdx + 1,
        leftHand,
        rightHand,
        instructions: buildInstructions(
            analyzedBeat.notes,
            analyzedBeat.rawNotes,
            analyzedBeat.leftHandMap,
            analyzedBeat.rightHandMap,
            analyzedBeat.barreFret,
            analyzedBeat.position,
            analyzedBeat.isTapping
        ),
        barreFret: analyzedBeat.barreFret,
        contextHint: buildContextHint(
            analyzedBeat.notes,
            analyzedBeat.rawNotes,
            analyzedBeat.barreFret,
            analyzedBeat.isTapping
        ),
        nextLeftHand
    };
}

const beatMap = new Map();
const tickArr = [];
let _selectedTrackIndex = 0;
let _transposeSemitones = 0;

function beatKey(beat) {
    try {
        return `${beat.voice.bar.index}_${beat.index}`;
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
                    barIdx: bar.index,
                    duration: DUR_BEATS[durationValue] ?? 1,
                    notes: notes.map(note => ({
                        string: normalizeStringIndex(note.string),
                        fret: note.fret,
                        isDead: !!note.isDead
                    })),
                    rawNotes: notes.map(note => ({
                        hasBend: !!note.hasBend,
                        vibrato: !!note.isVibrato,
                        slideIn: !!note.slideInType && note.slideInType !== 0,
                        slideOut: !!note.slideOutType && note.slideOutType !== 0,
                        isHammer: !!note.isHammerPullOrigin || !!note.isHammerPullDestination,
                        isPullOff: !!note.isHammerPullDestination,
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
                    isTapping: notes.some(note => !!note.isTapped),
                    isTiedBeat: notes.every(note => !!note.isTieDestination)
                });
            }
        }
    }
    return data;
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

function runFullAnalysis(score) {
    beatMap.clear();
    tickArr.length = 0;
    try {
        const track = getActiveTrack(score);
        if (!track?.staves?.length) return;

        const beatData = extractBeatData(track);
        const rightHandState = new RightHandHeuristics();
        const analyzedBeats = beatData.map(data => analyzeBeat(data, rightHandState));

        let firstJson = null;
        for (let i = 0; i < analyzedBeats.length; i++) {
            const current = analyzedBeats[i];
            const next = analyzedBeats[i + 1] || null;
            const analysis = buildAnalysis(current.barIdx, current, next);
            const json = JSON.stringify(analysis);
            const key = beatKey(current.beat);
            if (key) beatMap.set(key, json);
            tickArr.push({
                tick: current.beat.start ?? current.beat.absoluteDisplayStart ?? (i * 960),
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

    const tick = beat.start ?? beat.absoluteDisplayStart;
    if (tick != null) {
        sendForTick(tick);
    }
}
