function detectBarreCandidates(notes) {
    const fretted = notes.filter(note => note.fret > 0 && !note.isDead && !note.isTapped);
    const frets = [...new Set(fretted.map(note => note.fret))].sort((a, b) => a - b);
    const candidates = [];

    for (const fret of frets) {
        const notesAtFret = fretted.filter(note => note.fret === fret);
        if (notesAtFret.length < 2) continue;

        const stringsAtFret = notesAtFret.map(note => note.string).sort((a, b) => a - b);
        const fromString = Math.min(...stringsAtFret);
        const toString = Math.max(...stringsAtFret);
        const span = toString - fromString + 1;
        const coverage = notesAtFret.length / span;
        const runs = contiguousStringRuns(stringsAtFret);

        for (const run of runs) {
            if (run.length < 2) continue;
            const runSpan = run[run.length - 1] - run[0] + 1;
            candidates.push({
                fret,
                fromString: run[0],
                toString: run[run.length - 1],
                finger: 1,
                strength: run.length * 1.6 + runSpan * 0.35 + (run.length >= 3 ? 0.8 : 0),
                coverage: 1,
                type: run.length >= 3 ? 'partial' : 'mini'
            });
        }

        if (notesAtFret.length >= 3 && span >= 3 && coverage >= 0.45) {
            const edgeBonus = (stringsAtFret.includes(1) ? 0.35 : 0) + (stringsAtFret.includes(6) ? 0.35 : 0);
            candidates.push({
                fret,
                fromString,
                toString,
                finger: 1,
                strength: notesAtFret.length * 1.15 + coverage * 2 + edgeBonus - Math.max(0, span - notesAtFret.length) * 0.45,
                coverage,
                type: span >= 5 ? 'full' : 'partial'
            });
        }
    }

    const deduped = dedupeCandidates(candidates, candidate => barreKey(candidate));
    return deduped.sort((a, b) => b.strength - a.strength);
}

function isCoveredByBarre(note, barre) {
    return !!barre &&
        note.fret === barre.fret &&
        note.string >= barre.fromString &&
        note.string <= barre.toString;
}

function collectPositionCandidates(notes) {
    const fretted = notes.filter(note => note.fret > 0 && !note.isDead && !note.isTapped);
    const candidates = new Set([1]);

    for (const note of fretted) {
        for (let finger = 1; finger <= 4; finger++) {
            candidates.add(Math.max(1, note.fret - finger + 1));
        }
    }

    return [...candidates].sort((a, b) => a - b);
}

function scorePositionCandidate(notes, position, previousPosition, barre) {
    let score = previousPosition == null ? 0 : Math.abs(position - previousPosition) * 1.2;
    const frets = activeFrets(notes);
    const minFret = frets.length ? Math.min(...frets) : null;
    const minFretCount = minFret == null
        ? 0
        : notes.filter(note => note.fret === minFret && !note.isDead && !note.isTapped).length;

    if (barre) {
        if (position > barre.fret) {
            score += 30;
        }
        score += Math.abs(position - barre.fret) * 5;
    }

    for (const note of notes) {
        if (note.fret <= 0 || note.isDead || note.isTapped) continue;
        if (isCoveredByBarre(note, barre)) {
            score += Math.max(0, position - note.fret) * 0.5;
            continue;
        }

        const finger = note.fret - position + 1;
        if (finger < 1 || finger > 4) {
            score += 25 + (Math.abs(finger < 1 ? finger - 1 : finger - 4) * 8);
            continue;
        }

        if (minFretCount >= 2 && note.fret === minFret) {
            score += (finger - 1) * 2.5;
        }

        score += Math.abs(finger - 2.5);
        if (finger === 1) score += 0.6;
        if (finger === 4) score += 0.25;
    }

    return score;
}

function assignLeftHand(notes, position, barre) {
    const leftHand = {};
    const placement = {};
    const frettedNotes = notes
        .filter(note => note.fret > 0 && !note.isDead && !note.isTapped)
        .sort((a, b) => a.fret - b.fret || b.string - a.string);
    const keyFor = note => `${note.string}_${note.fret}`;
    const preferredFinger = note => clamp(note.fret - position + 1, 1, 4);

    function candidateFingerOptions(note) {
        const base = preferredFinger(note);
        const options = [base];
        for (let offset = 1; offset <= 3; offset++) {
            if (base - offset >= 1) options.push(base - offset);
            if (base + offset <= 4) options.push(base + offset);
        }
        return [...new Set(options)];
    }

    function evaluateAssignment(assigned) {
        let cost = 0;
        const fingerGroups = new Map();

        for (const note of frettedNotes) {
            const key = keyFor(note);
            const finger = assigned.get(key);
            const preferred = preferredFinger(note);
            cost += Math.abs(finger - preferred);
            if (finger === 4) cost += 0.15;
            const list = fingerGroups.get(finger) || [];
            list.push(note);
            fingerGroups.set(finger, list);
        }

        for (const [finger, group] of fingerGroups.entries()) {
            const frets = new Set(group.map(note => note.fret));
            if (frets.size > 1) {
                cost += 40;
                continue;
            }

            if (group.length > 1) {
                if (!hasContiguousStrings(group)) {
                    cost += 30;
                } else {
                    cost += finger === 1 ? 0.35 : 0.75;
                }
            }
        }

        for (let i = 0; i < frettedNotes.length; i++) {
            for (let j = i + 1; j < frettedNotes.length; j++) {
                const first = frettedNotes[i];
                const second = frettedNotes[j];
                const firstFinger = assigned.get(keyFor(first));
                const secondFinger = assigned.get(keyFor(second));
                if (first.fret < second.fret && firstFinger > secondFinger) {
                    cost += 3.5;
                }
                if (first.fret === second.fret && first.string > second.string && firstFinger < secondFinger) {
                    cost += 1.8;
                }
            }
        }

        return cost;
    }

    const fixedAssignments = new Map();
    if (barre) {
        for (const note of frettedNotes) {
            if (isCoveredByBarre(note, barre)) {
                fixedAssignments.set(keyFor(note), 1);
            }
        }
    }

    let bestAssignment = new Map(fixedAssignments);
    let bestCost = Number.POSITIVE_INFINITY;

    function search(index, assigned) {
        if (index >= frettedNotes.length) {
            const cost = evaluateAssignment(assigned);
            if (cost < bestCost) {
                bestCost = cost;
                bestAssignment = new Map(assigned);
            }
            return;
        }

        const note = frettedNotes[index];
        const key = keyFor(note);
        if (assigned.has(key)) {
            search(index + 1, assigned);
            return;
        }

        for (const finger of candidateFingerOptions(note)) {
            assigned.set(key, finger);
            search(index + 1, assigned);
            assigned.delete(key);
        }
    }

    search(0, new Map(fixedAssignments));

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
        if (note.isTapped) {
            leftHand[key] = '-';
            continue;
        }
        if (isCoveredByBarre(note, barre)) {
            leftHand[key] = `${leftHandName(1)} (${t('possibleBarre', { fret: barre.fret })})`;
            placement[note.string] = { fret: note.fret, finger: 1 };
            continue;
        }

        const assignedFinger = bestAssignment.get(key) ?? preferredFinger(note);
        leftHand[key] = leftHandName(assignedFinger);
        placement[note.string] = { fret: note.fret, finger: assignedFinger };
    }

    return { leftHand, placement };
}

function maxAbsFretDistance(notes, position) {
    return notes.reduce((max, note) => {
        if (note.fret <= 0 || note.isDead || note.isTapped) return max;
        const finger = note.fret - position + 1;
        return Math.max(max, Math.abs(finger - 2.5));
    }, 0);
}

function buildLeftHandCandidate(beatData, position, barre) {
    const leftHandResult = assignLeftHand(beatData.notes, position, barre);
    const frets = activeFrets(beatData.notes);
    const positionCost = scorePositionCandidate(beatData.notes, position, null, barre);
    const stretchCost = frets.length >= 2 ? fretStretchCost(Math.min(...frets), Math.max(...frets)) : 0;
    const widthPenalty = stretchCost > 4 ? (stretchCost - 4) * 2.2 : 0;
    const reachCost = maxAbsFretDistance(beatData.notes, position) * 0.75;
    const barreCost = barre ? 0.8 + Math.max(0, (barre.toString - barre.fromString) - 2) * 0.2 : 0;
    const barreCoverageCost = barre?.coverage != null ? (1 - barre.coverage) * 1.6 : 0;
    const stateCost = positionCost + widthPenalty + reachCost + barreCost + barreCoverageCost;

    return {
        position,
        barre,
        barreFret: barre?.fret ?? null,
        leftHandMap: leftHandResult.leftHand,
        leftHandPlacement: leftHandResult.placement,
        leftHandStateCost: stateCost,
        leftHandStateBreakdown: {
            positionCost,
            widthPenalty,
            reachCost,
            barreCost,
            barreCoverageCost
        }
    };
}

function generateLeftHandCandidates(beatData) {
    const notes = beatData.notes;
    if (!activeFrets(notes).length) {
        return [1, 3, 5, 7, 9].map(position => ({
            position,
            barre: null,
            barreFret: null,
            leftHandMap: {},
            leftHandPlacement: {},
            leftHandStateCost: 0,
            leftHandStateBreakdown: {
                positionCost: 0,
                widthPenalty: 0,
                reachCost: 0,
                barreCost: 0,
                barreCoverageCost: 0
            }
        }));
    }

    const detectedBarres = detectBarreCandidates(notes).slice(0, 3);
    const positions = collectPositionCandidates(notes);
    const barreOptions = [null, ...detectedBarres];
    const candidates = [];

    for (const position of positions) {
        for (const barre of barreOptions) {
            if (barre && position !== barre.fret) continue;
            candidates.push(buildLeftHandCandidate(beatData, position, barre));
        }
    }

    return dedupeCandidates(candidates, candidate => (
        `${candidate.position}|${barreKey(candidate.barre)}|` +
        JSON.stringify(candidate.leftHandPlacement)
    )).sort((a, b) => a.leftHandStateCost - b.leftHandStateCost).slice(0, 12);
}
