function buildRightHandMapFromLetters(letters) {
    const result = {};
    for (const [stringIndex, finger] of Object.entries(letters)) {
        result[stringIndex] = finger === 't' ? t('tappingFinger') : rightHandName(finger);
    }
    return result;
}

function applyTappedLetters(letters, notes) {
    const result = { ...letters };
    for (const note of notes) {
        if (note.isTapped) {
            result[note.string] = 't';
        }
    }
    return result;
}

function buildRightHandLetters(notes, options = {}) {
    const sourceNotes = attackedNotes(notes);
    const playable = sourceNotes.filter(note => !note.isTapped);
    const bass = [...playable].filter(note => note.string >= 4).sort((a, b) => b.string - a.string);
    const treble = [...playable].filter(note => note.string <= 3).sort((a, b) => b.string - a.string);
    const letters = {};

    for (const note of bass) {
        letters[note.string] = 'p';
    }

    if (!treble.length) {
        return applyTappedLetters(letters, notes);
    }

    if (treble.length === 1 && options.singleTrebleFinger) {
        letters[treble[0].string] = options.singleTrebleFinger;
        return applyTappedLetters(letters, notes);
    }

    if (!options.trebleSequence) {
        for (const note of treble) {
            letters[note.string] = note.string === 3 ? 'i' : note.string === 2 ? 'm' : 'a';
        }
        return applyTappedLetters(letters, notes);
    }

    const trebleSequence = options.trebleSequence;
    for (let i = 0; i < treble.length; i++) {
        letters[treble[i].string] = trebleSequence[i] || trebleSequence[trebleSequence.length - 1];
    }

    return applyTappedLetters(letters, sourceNotes);
}

function buildRightHandCandidate(notes, options) {
    const sourceNotes = attackedNotes(notes);
    const letters = buildRightHandLetters(notes, options);
    const nonTappedNotes = sourceNotes.filter(note => !note.isTapped);
    const singleTrebleNote = nonTappedNotes.length === 1 && nonTappedNotes[0].string <= 3;
    const trebleNotes = nonTappedNotes.filter(note => note.string <= 3).sort((a, b) => a.string - b.string);
    const bassNotes = nonTappedNotes.filter(note => note.string >= 4).sort((a, b) => a.string - b.string);
    return {
        rightHandLetters: letters,
        rightHandMap: buildRightHandMapFromLetters(letters),
        rightHandPattern: options.pattern,
        rightHandStateCost: options.baseCost || 0,
        primaryTrebleFinger: singleTrebleNote ? letters[nonTappedNotes[0].string] : null,
        rightHandTrebleSequence: trebleNotes.map(note => ({
            string: note.string,
            finger: letters[note.string]
        })),
        rightHandBassSequence: bassNotes.map(note => ({
            string: note.string,
            finger: letters[note.string]
        }))
    };
}

function generateRightHandCandidates(beatData) {
    const notes = attackedNotes(beatData.notes);
    if (!notes.length) {
        return [buildRightHandCandidate(notes, { pattern: 'empty', baseCost: 0 })];
    }

    if (beatData.isTapping && notes.every(note => note.isTapped)) {
        return [buildRightHandCandidate(notes, { pattern: 'tapping', baseCost: 0 })];
    }

    const playable = notes.filter(note => !note.isTapped);
    const bass = playable.filter(note => note.string >= 4);
    const treble = playable.filter(note => note.string <= 3);
    const candidates = [];
    const shortBeat = beatData.duration <= 0.5;

    if (playable.length === 1) {
        const note = playable[0];
        if (note.string >= 4) {
            candidates.push(buildRightHandCandidate(notes, { pattern: 'singleBass', baseCost: 0 }));
        } else {
            const naturalFinger = note.string === 1 ? 'a' : note.string === 2 ? 'm' : 'i';
            candidates.push(buildRightHandCandidate(notes, {
                pattern: 'singleTreble',
                singleTrebleFinger: naturalFinger,
                baseCost: 0
            }));
            if (shortBeat && note.string === 2) {
                candidates.push(buildRightHandCandidate(notes, {
                    pattern: 'singleTreble',
                    singleTrebleFinger: 'i',
                    baseCost: 0.2
                }));
            }
            if (note.string === 1 && shortBeat) {
                candidates.push(buildRightHandCandidate(notes, {
                    pattern: 'singleTreble',
                    singleTrebleFinger: 'm',
                    baseCost: 0.55
                }));
            }
        }
        return candidates;
    }

    if (playable.length === 0) {
        candidates.push(buildRightHandCandidate(notes, { pattern: 'tapping', baseCost: 0 }));
    } else if (bass.length && treble.length) {
        candidates.push(buildRightHandCandidate(notes, {
            pattern: playable.length === 2 ? 'pinch' : 'fingerstyleChord',
            baseCost: playable.length === 2 ? 0 : 0.1
        }));
        candidates.push(buildRightHandCandidate(notes, {
            pattern: 'fingerstyleChord',
            trebleSequence: ['m', 'a'],
            baseCost: treble.length > 2 ? 0.6 : 0.2
        }));
        if (treble.length >= 2) {
            candidates.push(buildRightHandCandidate(notes, {
                pattern: 'fingerstyleChord',
                trebleSequence: ['i', 'a'],
                baseCost: treble.length > 2 ? 0.7 : 0.25
            }));
        }
    } else if (treble.length >= 2) {
        candidates.push(buildRightHandCandidate(notes, {
            pattern: 'trebleBlock',
            baseCost: 0
        }));
        if (treble.length <= 2) {
            candidates.push(buildRightHandCandidate(notes, {
                pattern: 'trebleBlock',
                trebleSequence: ['m', 'a'],
                baseCost: 0.1
            }));
        }
    } else {
        candidates.push(buildRightHandCandidate(notes, {
            pattern: 'bassBlock',
            baseCost: 0
        }));
    }

    return candidates;
}
