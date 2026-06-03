function inferLegatoType(prevEvent, nextEvent) {
    if (!prevEvent || !nextEvent) return null;
    if (prevEvent.notes.length !== 1 || nextEvent.notes.length !== 1) return null;
    const prevNote = prevEvent.notes[0];
    const nextNote = nextEvent.notes[0];
    if (prevNote.isDead || nextNote.isDead) return null;
    if (prevNote.string !== nextNote.string) return null;
    const legatoMarked = prevEvent.rawNotes.some(note => note.isHammer || note.isPullOff) ||
        nextEvent.rawNotes.some(note => note.isHammer || note.isPullOff);
    if (!legatoMarked) return null;
    if (nextNote.fret > prevNote.fret) return 'hammer';
    if (nextNote.fret < prevNote.fret) return 'pullOff';
    return null;
}

function annotateLegatoDirections(events) {
    for (let i = 0; i < events.length - 1; i++) {
        const legatoType = inferLegatoType(events[i], events[i + 1]);
        if (!legatoType) continue;
        for (const rawNote of events[i].rawNotes) {
            rawNote.legatoType = legatoType;
        }
        for (const rawNote of events[i + 1].rawNotes) {
            rawNote.legatoType = legatoType;
        }
    }
}

function mergeRawTechnique(target, source) {
    target.hasBend = target.hasBend || source.hasBend;
    target.vibrato = target.vibrato || source.vibrato;
    target.slideIn = target.slideIn || source.slideIn;
    target.slideOut = target.slideOut || source.slideOut;
    target.isHammer = target.isHammer || source.isHammer;
    target.isPullOff = target.isPullOff || source.isPullOff;
    target.isPalmMute = target.isPalmMute || source.isPalmMute;
    target.hasHarmonic = target.hasHarmonic || source.hasHarmonic;
    target.isTrill = target.isTrill || source.isTrill;
    target.isGhost = target.isGhost || source.isGhost;
    target.isDead = target.isDead || source.isDead;
    target.isLetRing = target.isLetRing || source.isLetRing;
    target.isSlap = target.isSlap || source.isSlap;
    target.isPop = target.isPop || source.isPop;
    target.accent = Math.max(target.accent || 0, source.accent || 0);
    target.legatoType = target.legatoType || source.legatoType || null;
}

function dedupeEventNotes(event) {
    const map = new Map();

    for (let i = 0; i < event.notes.length; i++) {
        const note = event.notes[i];
        const raw = event.rawNotes[i];
        const key = `${note.string}:${note.fret}:${note.isDead ? 'x' : 'n'}:${note.isTapped ? 't' : 'l'}`;

        if (!map.has(key)) {
            map.set(key, {
                note: { ...note },
                raw: { ...raw }
            });
        } else {
            mergeRawTechnique(map.get(key).raw, raw);
        }
    }

    event.notes = [...map.values()].map(item => item.note);
    event.rawNotes = [...map.values()].map(item => item.raw);
    return event;
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

function buildInstructions(notes, rawNotes, leftHandMap, rightHandMap, barreFret, position, isTapping, extraHints = []) {
    const instructions = [];
    const techniques = extractTechniques(rawNotes);
    const frets = activeFrets(notes);
    const width = frets.length ? Math.max(...frets) - Math.min(...frets) : 0;
    const allTapped = notes.length > 0 && notes.every(note => note.isTapped);

    if (allTapped) {
        instructions.push(t('tappingInstruction'));
    }

    if (!allTapped && barreFret !== null) {
        instructions.push(t('positionBarre', { fret: barreFret }));
        instructions.push(t('barreHelp'));
    } else if (!allTapped && activeFrets(notes).length) {
        instructions.push(t('positionAround', { fret: position }));
    }

    if (!allTapped && notes.length === 1) {
        const note = notes[0];
        const leftHandLabel = leftHandMap[`${note.string}_${note.fret}`] || '?';
        const rightHandLabel = rightHandMap[note.string] || (
            note.isTapped ? t('tappingFinger') : rightHandName(note.string >= 4 ? 'p' : 'i')
        );
        if (note.isTapped) {
            instructions.push(t('tappingInstruction'));
        } else if (note.isDead) {
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
    } else if (!allTapped) {
        instructions.push(t('chord', { count: notes.length }));
        const summary = notes.map(note => {
            if (note.isTapped) return `${STRING_SHORT[note.string]}-${note.fret}(${t('tappingFinger')})`;
            if (note.isDead) return `${STRING_SHORT[note.string]}-${t('mutedShort')}`;
            if (note.fret === 0) return `${STRING_SHORT[note.string]}-${t('openShort')}`;
            return `${STRING_SHORT[note.string]}-${note.fret}(${leftHandMap[`${note.string}_${note.fret}`] || '?'})`;
        });
        instructions.push(t('leftHandSummary', { items: summary.join(', ') }));
        instructions.push(t('rightHandPluckTogether'));
    }

    if (width > 3) instructions.push(t('wideStretchInstruction'));
    if (isTapping) instructions.push(t('tappingInstruction'));
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

    return [...new Set(instructions.concat(extraHints))];
}

function buildContextHint(notes, rawNotes, barreFret, isTapping) {
    const techniques = extractTechniques(rawNotes);
    const frets = activeFrets(notes);
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

function buildRightHandPayload(notes, rightHandMap, rightHandLetters = {}) {
    return notes.map(note => {
        const fallbackFinger = note.isTapped ? 't' : (note.string >= 4 ? 'p' : 'i');
        const letter = rightHandLetters[note.string] || fallbackFinger;
        const label = rightHandMap[note.string] || (
            letter === 't' ? t('tappingFinger') : rightHandName(letter)
        );
        const finger = letter === 't' ? 'T' : letter;
        const stringName = STRING_SHORT[note.string] || String(note.string);
        return {
            finger,
            fingerName: label,
            string: stringName,
            stringIndex: note.string,
            direction: finger,
            color: note.isTapped ? '#9C5DB8' : (note.string >= 4 ? '#8B6350' : '#3678B5')
        };
    });
}

function buildLeftHandPayload(notes, rawNotes, leftHandMap, leftHandPlacement, isTapping) {
    return notes.map((note, index) => {
        const rawNote = rawNotes[index];
        const key = `${note.string}_${note.fret}`;
        const placement = leftHandPlacement[note.string];
        const label = note.isTapped
            ? t('tappingFinger')
            : note.isDead
            ? t('deadLabel')
            : (note.fret === 0 ? t('openStringName') : (leftHandMap[key] || '?'));

        return {
            finger: note.isTapped
                ? 'T'
                : note.isDead
                    ? 'x'
                    : note.fret === 0
                        ? '0'
                        : String(placement?.finger ?? '?'),
            fingerName: label,
            string: STRING_SHORT[note.string] || String(note.string),
            stringIndex: note.string,
            fret: String(note.fret),
            color: note.isTapped
                ? '#9C5DB8'
                : note.fret === 0
                    ? '#4FC3F7'
                    : note.isDead
                        ? '#757575'
                        : '#D07B30',
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
            isTapping: !!note.isTapped || !!isTapping
        };
    });
}

function buildAnalysis(barIdx, analyzedBeat, nextAnalyzedBeat) {
    const leftHand = buildLeftHandPayload(
        analyzedBeat.notes,
        analyzedBeat.rawNotes,
        analyzedBeat.leftHandMap,
        analyzedBeat.leftHandPlacement,
        analyzedBeat.isTapping
    );
    const rightHand = buildRightHandPayload(
        analyzedBeat.notes,
        analyzedBeat.rightHandMap,
        analyzedBeat.rightHandLetters
    );
    const nextLeftHand = nextAnalyzedBeat
        ? buildLeftHandPayload(
            nextAnalyzedBeat.notes,
            nextAnalyzedBeat.rawNotes,
            nextAnalyzedBeat.leftHandMap,
            nextAnalyzedBeat.leftHandPlacement,
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
            analyzedBeat.isTapping,
            analyzedBeat.performanceHints
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
