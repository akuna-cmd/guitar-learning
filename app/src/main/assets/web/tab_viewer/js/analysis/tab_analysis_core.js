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

function activeFrets(notes) {
    return notes
        .filter(note => note.fret > 0 && !note.isDead && !note.isTapped)
        .map(note => note.fret);
}

function getBeatTick(beat) {
    return beat?.absoluteDisplayStart ?? beat?.start ?? 0;
}

function getBeatLocalTick(beat) {
    return beat?.start ?? beat?.absoluteDisplayStart ?? 0;
}

function contiguousStringRuns(strings) {
    if (!strings.length) return [];
    const runs = [];
    let current = [strings[0]];
    for (let i = 1; i < strings.length; i++) {
        if (strings[i] === strings[i - 1] + 1) {
            current.push(strings[i]);
        } else {
            runs.push(current);
            current = [strings[i]];
        }
    }
    runs.push(current);
    return runs;
}

function barreKey(barre) {
    return barre ? `${barre.fret}:${barre.fromString}-${barre.toString}` : '-';
}

function dedupeCandidates(candidates, keyBuilder) {
    const seen = new Set();
    return candidates.filter(candidate => {
        const key = keyBuilder(candidate);
        if (seen.has(key)) return false;
        seen.add(key);
        return true;
    });
}

function hasContiguousStrings(notes) {
    const strings = notes.map(note => note.string).sort((a, b) => a - b);
    for (let i = 1; i < strings.length; i++) {
        if (strings[i] !== strings[i - 1] + 1) return false;
    }
    return true;
}

function fretStretchCost(fretA, fretB) {
    const distance = Math.abs(fretA - fretB);
    const lowerFret = Math.min(fretA, fretB);
    const lowPositionMultiplier =
        lowerFret <= 2 ? 1.7 :
        lowerFret <= 5 ? 1.3 :
        lowerFret <= 9 ? 1.0 :
        0.75;
    return distance * lowPositionMultiplier;
}

function frettedPairs(notes) {
    return notes
        .filter(note => note.fret > 0 && !note.isDead && !note.isTapped)
        .map(note => `${note.string}:${note.fret}`);
}

function countCommonFrettedPairs(prevNotes, nextNotes) {
    const prevSet = new Set(frettedPairs(prevNotes));
    let matches = 0;
    for (const pair of frettedPairs(nextNotes)) {
        if (prevSet.has(pair)) matches += 1;
    }
    return matches;
}

function attackedNotes(notes) {
    const fresh = notes.filter(note => !note.isTieDestination);
    return fresh.length ? fresh : notes;
}
