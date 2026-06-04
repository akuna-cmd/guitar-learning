function buildBeatCandidates(beatData) {
    const leftHandCandidates = generateLeftHandCandidates(beatData);
    const rightHandCandidates = generateRightHandCandidates(beatData);
    const combined = [];

    for (const leftCandidate of leftHandCandidates) {
        for (const rightCandidate of rightHandCandidates) {
            combined.push({
                ...beatData,
                ...leftCandidate,
                ...rightCandidate,
                stateCost: leftCandidate.leftHandStateCost + rightCandidate.rightHandStateCost,
                stateCostBreakdown: {
                    leftHand: leftCandidate.leftHandStateBreakdown,
                    rightHand: {
                        patternCost: rightCandidate.rightHandStateCost
                    }
                }
            });
        }
    }

    return combined
        .sort((a, b) => a.stateCost - b.stateCost)
        .slice(0, 20);
}

function isSingleSameStringMovement(prevCandidate, candidate) {
    return prevCandidate.notes.length === 1 &&
        candidate.notes.length === 1 &&
        !prevCandidate.notes[0].isTapped &&
        !candidate.notes[0].isTapped &&
        prevCandidate.notes[0].string === candidate.notes[0].string;
}

function isSequentialSingleNoteMove(prevCandidate, candidate) {
    return prevCandidate.notes.length === 1 &&
        candidate.notes.length === 1 &&
        !prevCandidate.notes[0].isDead &&
        !candidate.notes[0].isDead &&
        !prevCandidate.notes[0].isTapped &&
        !candidate.notes[0].isTapped &&
        prevCandidate.notes[0].fret > 0 &&
        candidate.notes[0].fret > 0;
}

function computeRightHandTransitionCost(prevCandidate, candidate, speedFactor) {
    const breakdown = {
        trebleRepeat: 0,
        trebleAlternation: 0,
        thumbRepeat: 0,
        thumbLeap: 0,
        blockAdjacency: 0
    };
    let cost = 0;

    if (prevCandidate.rightHandPattern === 'singleTreble' && candidate.rightHandPattern === 'singleTreble') {
        if (prevCandidate.primaryTrebleFinger === candidate.primaryTrebleFinger) {
            const delta = speedFactor > 1.3 ? 1.15 : 0.2;
            breakdown.trebleRepeat += delta;
            cost += delta;
        } else {
            const delta = speedFactor > 1.3 ? -0.55 : -0.15;
            breakdown.trebleAlternation += delta;
            cost += delta;
        }
    }

    const prevBass = prevCandidate.rightHandBassSequence || [];
    const nextBass = candidate.rightHandBassSequence || [];
    if (prevBass.length === 1 && nextBass.length === 1) {
        const stringDelta = Math.abs(prevBass[0].string - nextBass[0].string);
        if (stringDelta >= 2 && speedFactor > 1.1) {
            const delta = 0.45 * stringDelta;
            breakdown.thumbLeap += delta;
            cost += delta;
        } else if (stringDelta === 1 && speedFactor > 1.1) {
            const delta = -0.1;
            breakdown.thumbRepeat += delta;
            cost += delta;
        }
    }

    const nextTreble = candidate.rightHandTrebleSequence || [];
    if (nextTreble.length >= 2) {
        let adjacencyPenalty = 0;
        for (let i = 1; i < nextTreble.length; i++) {
            const prev = nextTreble[i - 1];
            const current = nextTreble[i];
            const sameFinger = prev.finger === current.finger;
            const fingerJump =
                (prev.finger === 'i' && current.finger === 'a') ||
                (prev.finger === 'a' && current.finger === 'i');
            if (sameFinger) adjacencyPenalty += 1.4;
            if (fingerJump) adjacencyPenalty += 0.5;
        }
        if (adjacencyPenalty) {
            breakdown.blockAdjacency += adjacencyPenalty;
            cost += adjacencyPenalty;
        }
    }

    return { cost, breakdown };
}

function computeTransitionCost(prevCandidate, candidate) {
    const breakdown = {
        positionShift: 0,
        barreChange: 0,
        commonShape: 0,
        letRing: 0,
        heldTie: 0,
        slide: 0,
        legato: 0,
        sameFretStringMove: 0,
        rightHand: 0,
        chordPattern: 0
    };
    const speedFactor = Math.min(prevCandidate.duration, candidate.duration) <= 0.5 ? 1.7
        : Math.min(prevCandidate.duration, candidate.duration) <= 1 ? 1.2
        : 0.9;
    const prevFretted = activeFrets(prevCandidate.notes);
    const nextFretted = activeFrets(candidate.notes);
    const currentMostlyOpen = nextFretted.length === 0 || nextFretted.every(fret => fret <= 2);
    breakdown.positionShift = Math.abs(candidate.position - prevCandidate.position) * (currentMostlyOpen ? 0.6 : 1.5) * speedFactor;
    let cost = breakdown.positionShift;

    if (barreKey(candidate.barre) !== barreKey(prevCandidate.barre)) {
        breakdown.barreChange += 0.9;
        cost += 0.9;
    }
    if (candidate.position === prevCandidate.position) {
        breakdown.positionShift -= 0.45;
        cost -= 0.45;
    }

    const commonPairs = countCommonFrettedPairs(prevCandidate.notes, candidate.notes);
    breakdown.commonShape -= Math.min(1.4, commonPairs * 0.7);
    cost += breakdown.commonShape;

    if (prevCandidate.rawNotes.some(note => note.isLetRing) || candidate.rawNotes.some(note => note.isLetRing)) {
        breakdown.letRing -= Math.min(0.8, commonPairs * 0.4);
        cost += breakdown.letRing;
    }

    const prevPairToFinger = new Map();
    for (const note of prevCandidate.notes) {
        const finger = prevCandidate.leftHandPlacement[note.string]?.finger;
        if (finger != null) {
            prevPairToFinger.set(`${note.string}:${note.fret}`, finger);
        }
    }
    for (const note of candidate.notes) {
        if (!note.isTieDestination || note.isDead || note.isTapped || note.fret <= 0) continue;
        const key = `${note.string}:${note.fret}`;
        if (!prevPairToFinger.has(key)) continue;
        const previousFinger = prevPairToFinger.get(key);
        const currentFinger = candidate.leftHandPlacement[note.string]?.finger;
        if (currentFinger == null) continue;

        if (currentFinger === previousFinger) {
            breakdown.heldTie -= 3.2;
            cost -= 3.2;
        } else {
            const delta = 4.2 + Math.abs(currentFinger - previousFinger) * 1.1;
            breakdown.heldTie += delta;
            cost += delta;
        }

        if (candidate.position === prevCandidate.position) {
            breakdown.heldTie -= 0.6;
            cost -= 0.6;
        }
    }

    if (isSingleSameStringMovement(prevCandidate, candidate)) {
        const prevNote = prevCandidate.notes[0];
        const nextNote = candidate.notes[0];
        const fretDelta = Math.abs(prevNote.fret - nextNote.fret);
        const prevFinger = prevCandidate.leftHandPlacement[prevNote.string]?.finger;
        const nextFinger = candidate.leftHandPlacement[nextNote.string]?.finger;
        const slideLike = candidate.rawNotes.some(note => note.slideIn || note.slideOut) ||
            prevCandidate.rawNotes.some(note => note.slideIn || note.slideOut);
        const legatoLike = candidate.rawNotes.some(note => note.isHammer || note.isPullOff) ||
            prevCandidate.rawNotes.some(note => note.isHammer || note.isPullOff);

        if (slideLike && fretDelta <= 4) {
            breakdown.slide += prevFinger === nextFinger ? -1.7 : 1.1;
            cost += prevFinger === nextFinger ? -1.7 : 1.1;
        }
        const legatoType = candidate.rawNotes.find(note => note.legatoType)?.legatoType ||
            prevCandidate.rawNotes.find(note => note.legatoType)?.legatoType ||
            null;
        if (legatoLike && fretDelta <= 2) {
            const legatoDelta =
                legatoType === 'hammer' ? (prevFinger !== nextFinger ? -1.1 : 0.55) :
                legatoType === 'pullOff' ? (prevFinger !== nextFinger ? -1.3 : 0.6) :
                (prevFinger !== nextFinger ? -1.0 : 0.4);
            breakdown.legato += legatoDelta;
            cost += legatoDelta;
        }
    }

    if (isSequentialSingleNoteMove(prevCandidate, candidate)) {
        const prevNote = prevCandidate.notes[0];
        const nextNote = candidate.notes[0];
        const prevFinger = prevCandidate.leftHandPlacement[prevNote.string]?.finger;
        const nextFinger = candidate.leftHandPlacement[nextNote.string]?.finger;
        const stringDelta = Math.abs(prevNote.string - nextNote.string);
        const sameFret = prevNote.fret === nextNote.fret;
        const sameFinger = prevFinger != null && prevFinger === nextFinger;
        const slideLike = candidate.rawNotes.some(note => note.slideIn || note.slideOut) ||
            prevCandidate.rawNotes.some(note => note.slideIn || note.slideOut);
        const tiedLike = candidate.rawNotes.some(note => note.isLetRing || note.isHammer || note.isPullOff) ||
            prevCandidate.rawNotes.some(note => note.isLetRing || note.isHammer || note.isPullOff);

        if (!slideLike && !tiedLike && sameFret && sameFinger && stringDelta >= 1) {
            const delta = stringDelta === 1 ? 5.2 : 6.8;
            breakdown.sameFretStringMove += delta;
            cost += delta;
        }

        if (!slideLike && !tiedLike && sameFret && stringDelta === 1 && prevFinger !== nextFinger) {
            breakdown.sameFretStringMove -= 1.1;
            cost -= 1.1;

            const movingToHigherString = prevNote.string > nextNote.string;
            const expectedFinger = movingToHigherString ? prevFinger + 1 : prevFinger - 1;
            const fingerDelta = nextFinger - prevFinger;

            if (nextFinger === expectedFinger) {
                breakdown.sameFretStringMove -= 1.35;
                cost -= 1.35;
            } else if ((movingToHigherString && fingerDelta < 0) || (!movingToHigherString && fingerDelta > 0)) {
                breakdown.sameFretStringMove += 2.4;
                cost += 2.4;
            } else if (Math.abs(fingerDelta) > 1) {
                const delta = 0.9 + (Math.abs(fingerDelta) - 1) * 0.7;
                breakdown.sameFretStringMove += delta;
                cost += delta;
            }
        }
    }

    const rightHandTransition = computeRightHandTransitionCost(prevCandidate, candidate, speedFactor);
    breakdown.rightHand += rightHandTransition.cost;
    breakdown.rightHandDetails = rightHandTransition.breakdown;
    cost += rightHandTransition.cost;

    if (candidate.rightHandPattern === 'pinch' || candidate.rightHandPattern === 'fingerstyleChord') {
        breakdown.chordPattern -= 0.25;
        cost -= 0.25;
    }

    return { cost, breakdown };
}

function annotateArpeggioShapeHints(beatData) {
    function playableSingleNote(event) {
        if (!event || event.notes.length !== 1 || event.isTapping) return null;
        const note = event.notes[0];
        if (!note || note.isDead || note.isTapped || note.fret <= 0) return null;
        return note;
    }

    for (let start = 0; start < beatData.length; start++) {
        const firstNote = playableSingleNote(beatData[start]);
        if (!firstNote) continue;

        const runEvents = [];
        const shapeNotes = [];
        const shapeKeys = new Set();
        let minFret = firstNote.fret;
        let maxFret = firstNote.fret;

        for (let index = start; index < beatData.length; index++) {
            const event = beatData[index];
            const note = playableSingleNote(event);
            if (!note || event.barIdx !== beatData[start].barIdx) break;

            const nextMin = Math.min(minFret, note.fret);
            const nextMax = Math.max(maxFret, note.fret);
            if (nextMax - nextMin > 2) break;

            if (runEvents.length > 0) {
                const prevNote = runEvents[runEvents.length - 1].notes[0];
                if (Math.abs(prevNote.string - note.string) > 1) break;
            }

            const noteKey = `${note.string}:${note.fret}`;
            if (shapeKeys.has(noteKey)) {
                runEvents.push(event);
                minFret = nextMin;
                maxFret = nextMax;
                continue;
            }

            if (shapeNotes.some(shapeNote => shapeNote.string === note.string && shapeNote.fret !== note.fret)) {
                break;
            }

            runEvents.push(event);
            shapeNotes.push(note);
            shapeKeys.add(noteKey);
            minFret = nextMin;
            maxFret = nextMax;
        }

        if (runEvents.length < 2 || shapeNotes.length < 2) continue;

        const shapeBeat = {
            notes: shapeNotes,
            rawNotes: shapeNotes.map(() => ({}))
        };
        const bestShape = generateLeftHandCandidates(shapeBeat)[0];
        if (!bestShape?.leftHandPlacement) continue;

        const fingerByKey = {};
        for (const note of shapeNotes) {
            const finger = bestShape.leftHandPlacement[note.string]?.finger;
            if (finger != null) {
                fingerByKey[`${note.string}:${note.fret}`] = finger;
            }
        }

        if (!Object.keys(fingerByKey).length) continue;

        for (const event of runEvents) {
            if ((event.arpeggioHint?.length || 0) > runEvents.length) {
                continue;
            }
            event.arpeggioHint = {
                position: bestShape.position,
                fingerByKey,
                length: runEvents.length
            };
        }
    }
}

function stabilizeSingleNoteFingering(selected) {
    function attackedPlayableNotes(candidate) {
        return attackedNotes(candidate.notes).filter(note => !note.isDead && !note.isTapped && note.fret > 0);
    }

    function candidateRichness(candidate) {
        let score = activeFrets(candidate.notes).length;
        if (candidate.notes.some(note => note.isTieDestination)) score += 2;
        if (candidate.notes.length > 1) score += 1;
        return score;
    }

    for (let index = 0; index < selected.length; index++) {
        const candidate = selected[index];
        const attacked = attackedPlayableNotes(candidate);
        if (attacked.length !== 1) continue;

        const note = attacked[0];
        const currentFinger = candidate.leftHandPlacement[note.string]?.finger;
        if (currentFinger == null) continue;

        let preferredFinger = currentFinger;
        let bestSource = null;
        let bestScore = candidateRichness(candidate);

        for (let offset = 1; offset <= 2; offset++) {
            for (const neighborIndex of [index - offset, index + offset]) {
                const neighbor = selected[neighborIndex];
                if (!neighbor || neighbor.barIdx !== candidate.barIdx) continue;
                const neighborFinger = neighbor.leftHandPlacement[note.string]?.finger;
                const hasSameNote = neighbor.notes.some(other =>
                    other.string === note.string &&
                    other.fret === note.fret &&
                    !other.isDead &&
                    !other.isTapped
                );
                if (!hasSameNote || neighborFinger == null) continue;

                const richness = candidateRichness(neighbor);
                if (richness <= bestScore) continue;
                bestScore = richness;
                preferredFinger = neighborFinger;
                bestSource = neighbor;
            }
        }

        if (!bestSource || preferredFinger === currentFinger) continue;

        candidate.leftHandPlacement = {
            ...candidate.leftHandPlacement,
            [note.string]: {
                fret: note.fret,
                finger: preferredFinger
            }
        };
        candidate.leftHandMap = {
            ...candidate.leftHandMap,
            [`${note.string}_${note.fret}`]: leftHandName(preferredFinger)
        };
        candidate.position = Math.max(1, note.fret - preferredFinger + 1);
    }
}

function optimizeBeatSequence(beatData) {
    annotateArpeggioShapeHints(beatData);
    const candidateMatrix = beatData.map(data => buildBeatCandidates(data));
    if (!candidateMatrix.length) return [];

    const scores = candidateMatrix.map(() => []);
    const backPointers = candidateMatrix.map(() => []);

    for (let i = 0; i < candidateMatrix[0].length; i++) {
        scores[0][i] = candidateMatrix[0][i].stateCost;
        backPointers[0][i] = -1;
    }

    for (let beatIndex = 1; beatIndex < candidateMatrix.length; beatIndex++) {
        const candidates = candidateMatrix[beatIndex];
        const previousCandidates = candidateMatrix[beatIndex - 1];
        for (let currentIndex = 0; currentIndex < candidates.length; currentIndex++) {
            let bestScore = Number.POSITIVE_INFINITY;
            let bestPrevIndex = 0;

            for (let prevIndex = 0; prevIndex < previousCandidates.length; prevIndex++) {
                const transition = computeTransitionCost(previousCandidates[prevIndex], candidates[currentIndex]);
                const score = scores[beatIndex - 1][prevIndex] +
                    candidates[currentIndex].stateCost +
                    transition.cost;

                if (score < bestScore) {
                    bestScore = score;
                    bestPrevIndex = prevIndex;
                    candidates[currentIndex].transitionCostBreakdown = transition.breakdown;
                    candidates[currentIndex].totalPathScore = bestScore;
                }
            }

            scores[beatIndex][currentIndex] = bestScore;
            backPointers[beatIndex][currentIndex] = bestPrevIndex;
        }
    }

    let bestIndex = 0;
    let bestScore = Number.POSITIVE_INFINITY;
    const lastScores = scores[scores.length - 1];
    for (let i = 0; i < lastScores.length; i++) {
        if (lastScores[i] < bestScore) {
            bestScore = lastScores[i];
            bestIndex = i;
        }
    }

    const selected = new Array(candidateMatrix.length);
    for (let beatIndex = candidateMatrix.length - 1; beatIndex >= 0; beatIndex--) {
        selected[beatIndex] = candidateMatrix[beatIndex][bestIndex];
        bestIndex = backPointers[beatIndex][bestIndex];
        if (bestIndex < 0) break;
    }

    stabilizeSingleNoteFingering(selected);

    return selected.map((candidate, index) => ({
        ...candidate,
        performanceHints: derivePerformanceHints(
            selected[index - 1] || null,
            candidate,
            selected[index + 1] || null
        )
    }));
}

function derivePerformanceHints(prevCandidate, candidate, nextCandidate) {
    const hints = [];

    if (candidate.rightHandPattern === 'singleTreble' && nextCandidate?.rightHandPattern === 'singleTreble') {
        const quickPassage = Math.min(candidate.duration, nextCandidate.duration) <= 0.5;
        const currentNote = candidate.notes[0];
        const nextNote = nextCandidate.notes[0];
        if (quickPassage &&
            candidate.primaryTrebleFinger &&
            nextCandidate.primaryTrebleFinger &&
            candidate.primaryTrebleFinger !== nextCandidate.primaryTrebleFinger &&
            currentNote && nextNote &&
            !currentNote.isDead &&
            !nextNote.isDead) {
            hints.push(t('rightHandAlternatingTreble', {
                first: rightHandName(candidate.primaryTrebleFinger),
                second: rightHandName(nextCandidate.primaryTrebleFinger)
            }));
        }
    }

    if (candidate.rightHandPattern === 'pinch' || candidate.rightHandPattern === 'fingerstyleChord') {
        hints.push(t('rightHandBassMelody'));
    } else if (candidate.rightHandPattern === 'trebleBlock') {
        hints.push(t('rightHandTrebleBlock'));
    }

    if (nextCandidate) {
        const sameShape = candidate.position === nextCandidate.position &&
            countCommonFrettedPairs(candidate.notes, nextCandidate.notes) > 0;
        if (sameShape) {
            hints.push(t('leftHandKeepShape'));
        } else if (Math.abs(nextCandidate.position - candidate.position) >= 3 && candidate.duration >= 0.5) {
            hints.push(t('leftHandShiftLate'));
        }
    }

    if (prevCandidate && candidate.rawNotes.some(note => note.isLetRing) &&
        countCommonFrettedPairs(prevCandidate.notes, candidate.notes) > 0) {
        hints.push(t('leftHandKeepShape'));
    }

    return [...new Set(hints)];
}
